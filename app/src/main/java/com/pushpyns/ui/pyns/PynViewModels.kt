package com.pushpyns.ui.pyns

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pushpyns.domain.geo.PynSorter
import com.pushpyns.domain.geo.distanceMeters
import com.pushpyns.domain.geo.matchesQuery
import com.pushpyns.domain.model.Category
import com.pushpyns.domain.model.GeoPoint
import com.pushpyns.domain.model.Pyn
import com.pushpyns.domain.model.PynDraft
import com.pushpyns.domain.model.PynSort
import com.pushpyns.domain.repository.AddressLookup
import com.pushpyns.domain.repository.CategoryRepository
import com.pushpyns.domain.repository.LocationSource
import com.pushpyns.domain.repository.PhotoStorage
import com.pushpyns.domain.repository.PynRepository
import com.pushpyns.domain.validation.PynFieldError
import com.pushpyns.domain.validation.coordinateOrNull
import com.pushpyns.domain.validation.validatePyn
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PynListUiState(
    val query: String = "",
    val sort: PynSort = PynSort.DATE,
    val pyns: List<Pyn> = emptyList(),
    val distances: Map<Long, Double> = emptyMap(),
    val locationAvailable: Boolean = false,
    val loading: Boolean = true,
    val expanded: Set<Long> = emptySet(),
)

sealed interface PynListRow {
    data class Header(val title: String) : PynListRow

    data class Item(val pyn: Pyn) : PynListRow
}

@HiltViewModel
class PynListViewModel
@Inject
constructor(private val repository: PynRepository, private val location: LocationSource) : ViewModel() {
    private val query = MutableStateFlow("")
    private val sort = MutableStateFlow(PynSort.DATE)
    private val origin = MutableStateFlow<GeoPoint?>(null)
    private val expanded = MutableStateFlow<Set<Long>>(emptySet())
    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val messages = _messages.asSharedFlow()

    val uiState =
        combine(repository.observeAll(), query, sort, origin, expanded) { list, q, mode, point, open ->
            val filtered = list.filter { it.matchesQuery(q) }
            val ordered = PynSorter.sort(filtered, mode, point)
            val distances =
                ordered
                    .mapNotNull { pyn ->
                        distanceMeters(pyn, point)?.let { pyn.id to it }
                    }.toMap()
            PynListUiState(
                query = q,
                sort = mode,
                pyns = ordered,
                distances = distances,
                locationAvailable = point != null,
                loading = false,
                expanded = open,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PynListUiState())

    init {
        refreshLocation()
    }

    fun onQuery(value: String) {
        query.value = value
    }

    fun onSort(value: PynSort) {
        sort.value = value
        if (value == PynSort.DISTANCE && origin.value == null) refreshLocation()
    }

    fun toggle(id: Long) {
        expanded.update { current -> if (id in current) current - id else current + id }
    }

    fun refreshLocation() {
        viewModelScope.launch {
            if (location.hasPermission()) origin.value = location.current()
        }
    }

    fun rows(): List<PynListRow> {
        val state = uiState.value
        if (state.sort != PynSort.CATEGORY) return state.pyns.map { PynListRow.Item(it) }
        val grouped =
            state.pyns.groupBy { pyn ->
                pyn.categories.minByOrNull { it.name.lowercase() }?.name ?: ""
            }
        return buildList {
            grouped.forEach { (name, items) ->
                add(PynListRow.Header(name.ifBlank { "Uncategorized" }))
                items.forEach { add(PynListRow.Item(it)) }
            }
        }
    }
}

data class PynEditorUiState(
    val loading: Boolean = false,
    val titleIsEdit: Boolean = false,
    val name: String = "",
    val description: String = "",
    val notes: String = "",
    val address: String = "",
    val city: String = "",
    val stateName: String = "",
    val zip: String = "",
    val latitude: String = "",
    val longitude: String = "",
    val categories: List<Category> = emptyList(),
    val selected: Set<Long> = emptySet(),
    val photoPaths: List<String> = emptyList(),
    val errors: Set<PynFieldError> = emptySet(),
    val saving: Boolean = false,
    val locating: Boolean = false,
)

sealed interface EditorEvent {
    data class Saved(val id: Long, val continueFlow: Boolean = false) : EditorEvent

    data class Message(val text: String) : EditorEvent
}

@HiltViewModel
class PynEditorViewModel
@Inject
constructor(
    private val pyns: PynRepository,
    private val categories: CategoryRepository,
    private val photos: PhotoStorage,
    private val location: LocationSource,
    private val addresses: AddressLookup,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val pynId: Long? = savedStateHandle.get<Long>("pynId")
    private val initialLat = savedStateHandle.get<String>("lat").orEmpty()
    private val initialLng = savedStateHandle.get<String>("lng").orEmpty()
    private var pendingCapture: String? = null

    private val _state = MutableStateFlow(PynEditorUiState(loading = pynId != null, titleIsEdit = pynId != null))
    val state = _state.asStateFlow()
    private val _events = MutableSharedFlow<EditorEvent>(extraBufferCapacity = 1)
    val events = _events.asSharedFlow()

    init {
        viewModelScope.launch {
            categories.observeAll().collect { list -> _state.update { it.copy(categories = list) } }
        }
        if (pynId == null) {
            _state.update { it.copy(latitude = initialLat, longitude = initialLng, loading = false) }
            val lat = initialLat.toDoubleOrNull()
            val lng = initialLng.toDoubleOrNull()
            if (lat != null && lng != null) {
                viewModelScope.launch { applyAddress(lat, lng, overwrite = false) }
            }
        } else {
            viewModelScope.launch {
                val pyn = pyns.get(pynId)
                if (pyn == null) {
                    _events.emit(EditorEvent.Message("That pyn is no longer on this phone."))
                    _state.update { it.copy(loading = false) }
                    return@launch
                }
                _state.update {
                    it.copy(
                        loading = false,
                        name = pyn.name,
                        description = pyn.description,
                        notes = pyn.notes,
                        address = pyn.address,
                        city = pyn.city,
                        stateName = pyn.state,
                        zip = pyn.zip,
                        latitude = pyn.latitude?.let { value -> String.format(Locale.US, "%.6f", value) }.orEmpty(),
                        longitude =
                        pyn.longitude
                            ?.let { value ->
                                String.format(Locale.US, "%.6f", value)
                            }.orEmpty(),
                        selected = pyn.categories.map { category -> category.id }.toSet(),
                        photoPaths = pyn.photos.map { photo -> photo.relativePath },
                    )
                }
            }
        }
    }

    fun onName(value: String) = _state.update { it.copy(name = value) }

    fun onDescription(value: String) = _state.update { it.copy(description = value) }

    fun onNotes(value: String) = _state.update { it.copy(notes = value) }

    fun onAddress(value: String) = _state.update { it.copy(address = value) }

    fun onCity(value: String) = _state.update { it.copy(city = value) }

    fun onState(value: String) = _state.update { it.copy(stateName = value) }

    fun onZip(value: String) = _state.update { it.copy(zip = value) }

    fun onLat(value: String) = _state.update { it.copy(latitude = value) }

    fun onLng(value: String) = _state.update { it.copy(longitude = value) }

    fun toggleCategory(id: Long) = _state.update { current ->
        val next = if (id in current.selected) current.selected - id else current.selected + id
        current.copy(selected = next)
    }

    fun prepareCapture(): String? {
        if (_state.value.photoPaths.size >= 8) {
            _events.tryEmit(EditorEvent.Message("You can attach up to 8 photos."))
            return null
        }
        val target = photos.prepareCapture()
        pendingCapture = target.relativePath
        return target.uri
    }

    fun onCaptureResult(success: Boolean) {
        val path = pendingCapture ?: return
        pendingCapture = null
        if (!success) {
            photos.delete(path)
            return
        }
        _state.update { it.copy(photoPaths = it.photoPaths + path) }
    }

    fun importPhoto(uri: String) {
        if (_state.value.photoPaths.size >= 8) {
            _events.tryEmit(EditorEvent.Message("You can attach up to 8 photos."))
            return
        }
        viewModelScope.launch {
            runCatching { photos.import(uri) }
                .onSuccess { path -> _state.update { it.copy(photoPaths = it.photoPaths + path) } }
                .onFailure { _events.tryEmit(EditorEvent.Message(it.message ?: "Couldn't add that photo.")) }
        }
    }

    fun removePhoto(path: String) {
        photos.delete(path)
        _state.update { it.copy(photoPaths = it.photoPaths - path) }
    }

    fun useCurrentLocation() {
        viewModelScope.launch {
            _state.update { it.copy(locating = true) }
            val point = if (location.hasPermission()) location.current() else null
            if (point == null) {
                _state.update { it.copy(locating = false) }
                _events.tryEmit(EditorEvent.Message("Couldn't get a location fix. You can still type an address."))
                return@launch
            }
            _state.update {
                it.copy(
                    locating = false,
                    latitude = String.format(Locale.US, "%.6f", point.latitude),
                    longitude = String.format(Locale.US, "%.6f", point.longitude),
                )
            }
            applyAddress(point.latitude, point.longitude, overwrite = true)
        }
    }

    fun save() {
        val current = _state.value
        val errors = validatePyn(current.name, current.latitude, current.longitude)
        if (errors.isNotEmpty()) {
            _state.update { it.copy(errors = errors) }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(saving = true, errors = emptySet()) }
            val id =
                pyns.upsert(
                    PynDraft(
                        id = pynId,
                        name = current.name,
                        description = current.description,
                        notes = current.notes,
                        address = current.address,
                        city = current.city,
                        state = current.stateName,
                        zip = current.zip,
                        latitude = coordinateOrNull(current.latitude),
                        longitude = coordinateOrNull(current.longitude),
                        categoryIds = current.selected.toList(),
                        photoPaths = current.photoPaths,
                    ),
                )
            _state.update { it.copy(saving = false) }
            _events.emit(EditorEvent.Saved(id))
        }
    }

    private suspend fun applyAddress(lat: Double, lng: Double, overwrite: Boolean) {
        val postal = addresses.reverse(lat, lng) ?: return
        _state.update { current ->
            current.copy(
                address =
                if (overwrite ||
                    current.address.isBlank()
                ) {
                    postal.street.ifBlank { postal.fullLine }
                } else {
                    current.address
                },
                city = if (overwrite || current.city.isBlank()) postal.city else current.city,
                stateName = if (overwrite || current.stateName.isBlank()) postal.state else current.stateName,
                zip = if (overwrite || current.zip.isBlank()) postal.zip else current.zip,
            )
        }
    }
}

data class PynDetailUiState(val pyn: Pyn? = null, val loading: Boolean = true, val missing: Boolean = false)

@HiltViewModel
class PynDetailViewModel
@Inject
constructor(private val pyns: PynRepository, savedStateHandle: SavedStateHandle) : ViewModel() {
    val pynId: Long = savedStateHandle.get<Long>("pynId") ?: -1L
    private val _state = MutableStateFlow(PynDetailUiState())
    val state = _state.asStateFlow()
    private val _deleted = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val deleted = _deleted.asSharedFlow()

    init {
        viewModelScope.launch {
            pyns.observe(pynId).collect { pyn ->
                _state.value = PynDetailUiState(pyn = pyn, loading = false, missing = pyn == null)
            }
        }
    }

    fun delete() {
        viewModelScope.launch {
            pyns.delete(pynId)
            _deleted.emit(Unit)
        }
    }
}
