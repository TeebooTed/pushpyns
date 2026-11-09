package com.pushpyns.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pushpyns.domain.geo.PynSorter
import com.pushpyns.domain.geo.distanceMeters
import com.pushpyns.domain.model.GeoPoint
import com.pushpyns.domain.model.Pyn
import com.pushpyns.domain.model.PynSort
import com.pushpyns.domain.repository.AddressLookup
import com.pushpyns.domain.repository.LocationSource
import com.pushpyns.domain.repository.PynRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class LocationAccess { Unknown, Granted, Denied }

data class HomeUiState(
    val pyns: List<Pyn> = emptyList(),
    val origin: GeoPoint? = null,
    val addressLine: String = "",
    val access: LocationAccess = LocationAccess.Unknown,
    val loading: Boolean = true,
)

@HiltViewModel
class HomeViewModel
@Inject
constructor(
    private val pyns: PynRepository,
    private val location: LocationSource,
    private val addresses: AddressLookup,
) : ViewModel() {
    private val origin = MutableStateFlow<GeoPoint?>(null)
    private val address = MutableStateFlow("")
    private val access = MutableStateFlow(LocationAccess.Unknown)
    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val messages = _messages.asSharedFlow()

    val uiState =
        combine(pyns.observeAll(), origin, address, access) { list, point, line, permission ->
            val ordered =
                if (point != null) {
                    PynSorter.sort(list, PynSort.DISTANCE, point)
                } else {
                    PynSorter.sort(list, PynSort.DATE, null)
                }
            HomeUiState(
                pyns = ordered,
                origin = point,
                addressLine = line,
                access = permission,
                loading = false,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    init {
        refresh()
    }

    fun onPermissionResult(granted: Boolean) {
        access.value = if (granted) LocationAccess.Granted else LocationAccess.Denied
        if (granted) refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            if (!location.hasPermission()) {
                access.value = LocationAccess.Denied
                return@launch
            }
            access.value = LocationAccess.Granted
            val point = location.current()
            origin.value = point
            if (point == null) {
                _messages.tryEmit("Couldn't get a location fix. You can still type an address.")
                return@launch
            }
            val postal = addresses.reverse(point.latitude, point.longitude)
            address.value = postal?.fullLine?.ifBlank { null }
                ?: "%.5f, %.5f".format(point.latitude, point.longitude)
        }
    }

    fun loadSamples() {
        viewModelScope.launch {
            val added = pyns.insertSamples()
            _messages.tryEmit(
                if (added > 0) "Sample places added." else "Those sample places are already on this phone.",
            )
        }
    }

    fun distanceFor(pyn: Pyn): Double? = distanceMeters(pyn, origin.value)

    fun updateAddress(value: String) = address.update { value }
}
