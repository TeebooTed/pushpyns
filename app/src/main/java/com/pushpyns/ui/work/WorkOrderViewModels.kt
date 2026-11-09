package com.pushpyns.ui.work

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pushpyns.domain.model.LineItemDraft
import com.pushpyns.domain.model.Pyn
import com.pushpyns.domain.model.PynDraft
import com.pushpyns.domain.model.WorkOrder
import com.pushpyns.domain.model.WorkOrderDraft
import com.pushpyns.domain.model.WorkOrderStatus
import com.pushpyns.domain.repository.AddressLookup
import com.pushpyns.domain.repository.CategoryRepository
import com.pushpyns.domain.repository.LocationSource
import com.pushpyns.domain.repository.PhotoStorage
import com.pushpyns.domain.repository.PynRepository
import com.pushpyns.domain.repository.WorkOrderRepository
import com.pushpyns.domain.validation.LineItemInput
import com.pushpyns.domain.validation.WorkOrderFieldError
import com.pushpyns.domain.validation.coordinateOrNull
import com.pushpyns.domain.validation.parsedLine
import com.pushpyns.domain.validation.validateWorkOrder
import com.pushpyns.domain.work.CompleteWorkOrder
import com.pushpyns.ui.pyns.EditorEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class OrderFilter { OPEN, COMPLETE }

data class WorkListUiState(
    val query: String = "",
    val filter: OrderFilter = OrderFilter.OPEN,
    val orders: List<WorkOrder> = emptyList(),
    val loading: Boolean = true,
)

@HiltViewModel
class WorkOrderListViewModel
@Inject
constructor(repository: WorkOrderRepository) : ViewModel() {
    private val query = MutableStateFlow("")
    private val filter = MutableStateFlow(OrderFilter.OPEN)

    val uiState =
        combine(repository.observeAll(), query, filter) { orders, q, mode ->
            val needle = q.trim().lowercase()
            val status = if (mode == OrderFilter.OPEN) WorkOrderStatus.OPEN else WorkOrderStatus.COMPLETE
            WorkListUiState(
                query = q,
                filter = mode,
                loading = false,
                orders =
                orders.filter { order ->
                    order.status == status &&
                        (
                            needle.isEmpty() ||
                                listOf(
                                    order.name,
                                    order.customerName,
                                    order.address,
                                    order.city,
                                    order.description,
                                ).any { it.lowercase().contains(needle) }
                            )
                },
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WorkListUiState())

    fun onQuery(value: String) {
        query.value = value
    }

    fun onFilter(value: OrderFilter) {
        filter.value = value
    }
}

data class LineForm(val key: Long, val description: String, val quantity: String, val unitPrice: String)

data class WorkEditorUiState(
    val loading: Boolean = false,
    val editing: Boolean = false,
    val jobName: String = "",
    val customer: String = "",
    val description: String = "",
    val notes: String = "",
    val address: String = "",
    val city: String = "",
    val stateName: String = "",
    val zip: String = "",
    val latitude: String = "",
    val longitude: String = "",
    val scheduledAt: Long? = null,
    val pyns: List<Pyn> = emptyList(),
    val linkedPynId: Long? = null,
    val saveAsPyn: Boolean = false,
    val lines: List<LineForm> = emptyList(),
    val photoPaths: List<String> = emptyList(),
    val errors: Set<WorkOrderFieldError> = emptySet(),
    val saving: Boolean = false,
    val locating: Boolean = false,
)

@HiltViewModel
class WorkOrderEditorViewModel
@Inject
constructor(
    private val workOrders: WorkOrderRepository,
    private val pyns: PynRepository,
    private val categories: CategoryRepository,
    private val photos: PhotoStorage,
    private val location: LocationSource,
    private val addresses: AddressLookup,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val workOrderId: Long? = savedStateHandle.get<Long>("workOrderId")
    private val initialPynId: Long? = savedStateHandle.get<Long>("pynId")?.takeIf { it > 0 }
    private var pendingCapture: String? = null
    private var lineKey = 1L

    private val _state =
        MutableStateFlow(
            WorkEditorUiState(loading = workOrderId != null, editing = workOrderId != null),
        )
    val state = _state.asStateFlow()
    private val _events = MutableSharedFlow<EditorEvent>(extraBufferCapacity = 1)
    val events = _events.asSharedFlow()

    init {
        viewModelScope.launch {
            pyns.observeAll().collect { list -> _state.update { it.copy(pyns = list) } }
        }
        if (workOrderId != null) {
            viewModelScope.launch { loadExisting(workOrderId) }
        } else if (initialPynId != null) {
            viewModelScope.launch { prefillPyn(initialPynId) }
        }
    }

    private suspend fun loadExisting(id: Long) {
        val order = workOrders.get(id)
        if (order == null) {
            _events.emit(EditorEvent.Message("That work order is no longer on this phone."))
            _state.update { it.copy(loading = false) }
            return
        }
        _state.update {
            it.copy(
                loading = false,
                jobName = order.name,
                customer = order.customerName,
                description = order.description,
                notes = order.notes,
                address = order.address,
                city = order.city,
                stateName = order.state,
                zip = order.zip,
                latitude = order.latitude.formatCoord(),
                longitude = order.longitude.formatCoord(),
                scheduledAt = order.scheduledAt,
                linkedPynId = order.pynId,
                lines =
                order.lineItems.map { item ->
                    LineForm(
                        key = lineKey++,
                        description = item.description,
                        quantity = item.quantity,
                        unitPrice =
                        com.pushpyns.domain.money.Money
                            .formatCents(item.unitPriceCents)
                            .removePrefix("$"),
                    )
                },
                photoPaths = order.photos.map { photo -> photo.relativePath },
            )
        }
    }

    private suspend fun prefillPyn(id: Long) {
        val pyn = pyns.get(id) ?: return
        applyPyn(pyn)
    }

    fun onJob(value: String) = _state.update { it.copy(jobName = value) }

    fun onCustomer(value: String) = _state.update { it.copy(customer = value) }

    fun onDescription(value: String) = _state.update { it.copy(description = value) }

    fun onNotes(value: String) = _state.update { it.copy(notes = value) }

    fun onAddress(value: String) = _state.update { it.copy(address = value) }

    fun onCity(value: String) = _state.update { it.copy(city = value) }

    fun onState(value: String) = _state.update { it.copy(stateName = value) }

    fun onZip(value: String) = _state.update { it.copy(zip = value) }

    fun onLat(value: String) = _state.update { it.copy(latitude = value) }

    fun onLng(value: String) = _state.update { it.copy(longitude = value) }

    fun onSchedule(value: Long?) = _state.update { it.copy(scheduledAt = value) }

    fun onSaveAsPyn(value: Boolean) = _state.update { it.copy(saveAsPyn = value) }

    fun onLinkPyn(id: Long?) {
        _state.update { it.copy(linkedPynId = id, saveAsPyn = if (id != null) false else it.saveAsPyn) }
        val pyn = _state.value.pyns.firstOrNull { it.id == id } ?: return
        applyPyn(pyn)
    }

    fun addLine(description: String, quantity: String, price: String) {
        _state.update {
            it.copy(lines = it.lines + LineForm(lineKey++, description, quantity, price))
        }
    }

    fun removeLine(key: Long) = _state.update { it.copy(lines = it.lines.filterNot { line -> line.key == key }) }

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
        if (!success) photos.delete(path) else _state.update { it.copy(photoPaths = it.photoPaths + path) }
    }

    fun importPhoto(uri: String) {
        if (_state.value.photoPaths.size >= 8) {
            _events.tryEmit(EditorEvent.Message("You can attach up to 8 photos."))
            return
        }
        viewModelScope.launch {
            runCatching { photos.import(uri) }
                .onSuccess { path -> _state.update { it.copy(photoPaths = it.photoPaths + path) } }
                .onFailure { error ->
                    _events.tryEmit(EditorEvent.Message(error.message ?: "Couldn't add that photo."))
                }
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
            addresses.reverse(point.latitude, point.longitude)?.let { postal ->
                _state.update { current ->
                    current.copy(
                        address = postal.street.ifBlank { postal.fullLine },
                        city = postal.city,
                        stateName = postal.state,
                        zip = postal.zip,
                    )
                }
            }
        }
    }

    fun save(continueToTerms: Boolean) {
        val current = _state.value
        val inputs = current.lines.map { LineItemInput(it.description, it.quantity, it.unitPrice) }
        val errors =
            validateWorkOrder(current.jobName, current.customer, current.latitude, current.longitude, inputs)
        if (errors.isNotEmpty()) {
            _state.update { it.copy(errors = errors) }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(saving = true, errors = emptySet()) }
            var linked = current.linkedPynId
            if (current.saveAsPyn && linked == null) {
                val cats = categories.observeAll().first()
                val workSite = cats.firstOrNull { it.name.equals("Work site", ignoreCase = true) }
                val copies = current.photoPaths.map { photos.duplicate(it) }
                linked =
                    pyns.upsert(
                        PynDraft(
                            id = null,
                            name = current.jobName.trim(),
                            description = current.description,
                            notes = current.notes,
                            address = current.address,
                            city = current.city,
                            state = current.stateName,
                            zip = current.zip,
                            latitude = coordinateOrNull(current.latitude),
                            longitude = coordinateOrNull(current.longitude),
                            categoryIds = listOfNotNull(workSite?.id),
                            photoPaths = copies,
                        ),
                    )
            }
            val id =
                workOrders.upsert(
                    WorkOrderDraft(
                        id = workOrderId,
                        pynId = linked,
                        name = current.jobName,
                        customerName = current.customer,
                        description = current.description,
                        notes = current.notes,
                        address = current.address,
                        city = current.city,
                        state = current.stateName,
                        zip = current.zip,
                        latitude = coordinateOrNull(current.latitude),
                        longitude = coordinateOrNull(current.longitude),
                        scheduledAt = current.scheduledAt,
                        lineItems =
                        inputs.mapNotNull { parsedLine(it) }.map {
                            LineItemDraft(it.description, it.quantity, it.unitPriceCents)
                        },
                        photoPaths = current.photoPaths,
                    ),
                )
            _state.update { it.copy(saving = false) }
            _events.emit(EditorEvent.Saved(id, continueFlow = continueToTerms))
        }
    }

    private fun applyPyn(pyn: Pyn) {
        _state.update {
            it.copy(
                linkedPynId = pyn.id,
                address = pyn.address,
                city = pyn.city,
                stateName = pyn.state,
                zip = pyn.zip,
                latitude = pyn.latitude.formatCoord(),
                longitude = pyn.longitude.formatCoord(),
                description = if (it.description.isBlank()) pyn.description else it.description,
                notes = if (it.notes.isBlank()) pyn.notes else it.notes,
                jobName = if (it.jobName.isBlank()) pyn.name else it.jobName,
            )
        }
    }
}

private fun Double?.formatCoord(): String = this?.let { String.format(Locale.US, "%.6f", it) }.orEmpty()

data class WorkDetailUiState(val order: WorkOrder? = null, val loading: Boolean = true)

@HiltViewModel
class WorkOrderDetailViewModel
@Inject
constructor(
    private val workOrders: WorkOrderRepository,
    private val complete: CompleteWorkOrder,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    val workOrderId: Long = savedStateHandle.get<Long>("workOrderId") ?: -1L
    private val _state = MutableStateFlow(WorkDetailUiState())
    val state = _state.asStateFlow()
    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val events = _events.asSharedFlow()

    init {
        viewModelScope.launch {
            workOrders.observe(workOrderId).collect { order ->
                _state.value = WorkDetailUiState(order = order, loading = false)
            }
        }
    }

    fun delete() {
        viewModelScope.launch {
            workOrders.delete(workOrderId)
            _events.emit("deleted")
        }
    }

    fun recreatePdf() {
        viewModelScope.launch {
            runCatching { complete.ensurePdf(workOrderId) }
                .onFailure { _events.emit(it.message ?: "Couldn't recreate the PDF.") }
        }
    }
}

data class SignatureUiState(val customer: String = "", val job: String = "", val saving: Boolean = false, val error: String? = null)

@HiltViewModel
class WorkOrderCompleteViewModel
@Inject
constructor(
    private val workOrders: WorkOrderRepository,
    private val complete: CompleteWorkOrder,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    val workOrderId: Long = savedStateHandle.get<Long>("workOrderId") ?: -1L
    private val _signature = MutableStateFlow(SignatureUiState())
    val signature = _signature.asStateFlow()
    private val _done = MutableSharedFlow<Long>(extraBufferCapacity = 1)
    val done = _done.asSharedFlow()
    private val _order = MutableStateFlow<WorkOrder?>(null)
    val order = _order.asStateFlow()

    init {
        viewModelScope.launch {
            workOrders.observe(workOrderId).collect { _order.value = it }
        }
        viewModelScope.launch {
            val current = workOrders.get(workOrderId)
            _signature.update {
                it.copy(customer = current?.customerName.orEmpty(), job = current?.name.orEmpty())
            }
        }
    }

    fun acceptTerms(onAccepted: () -> Unit) {
        viewModelScope.launch {
            complete.acceptTerms(workOrderId)
            onAccepted()
        }
    }

    fun completeWith(png: ByteArray) {
        viewModelScope.launch {
            _signature.update { it.copy(saving = true, error = null) }
            runCatching { complete.complete(workOrderId, png) }
                .onSuccess {
                    _signature.update { state -> state.copy(saving = false) }
                    _done.emit(workOrderId)
                }.onFailure { error ->
                    _signature.update { state -> state.copy(saving = false, error = error.message) }
                }
        }
    }
}
