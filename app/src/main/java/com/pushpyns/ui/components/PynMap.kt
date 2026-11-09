package com.pushpyns.ui.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.pushpyns.BuildConfig
import com.pushpyns.domain.model.GeoPoint

private val DefaultCenter = LatLng(37.7955, -122.3934)

fun mapsKeyPresent(): Boolean {
    val key = BuildConfig.MAPS_API_KEY
    return key.isNotBlank() && !key.equals("MISSING", ignoreCase = true)
}

@Composable
fun PynMap(pins: List<MapPin>, origin: GeoPoint?, modifier: Modifier = Modifier, onPinClick: (Long) -> Unit = {}) {
    if (!mapsKeyPresent()) {
        PlaceholderMap(
            pins = pins,
            origin = origin?.let { MapPin(-1, it.latitude, it.longitude) },
            caption = "Add a Maps API key in local.properties to replace this preview.",
            modifier = modifier,
            onPinClick = onPinClick,
        )
        return
    }
    val first = pins.firstOrNull { it.latitude != null && it.longitude != null }
    val start =
        when {
            origin != null -> LatLng(origin.latitude, origin.longitude)
            first?.latitude != null && first.longitude != null -> LatLng(first.latitude, first.longitude)
            else -> DefaultCenter
        }
    val camera =
        rememberCameraPositionState {
            position = CameraPosition.fromLatLngZoom(start, 13f)
        }
    var centered by remember { mutableStateOf(false) }
    LaunchedEffect(origin) {
        if (!centered && origin != null) {
            camera.position = CameraPosition.fromLatLngZoom(LatLng(origin.latitude, origin.longitude), 13f)
            centered = true
        }
    }
    GoogleMap(
        modifier = modifier.fillMaxSize(),
        cameraPositionState = camera,
    ) {
        pins.forEach { pin ->
            val lat = pin.latitude ?: return@forEach
            val lng = pin.longitude ?: return@forEach
            key(pin.id) {
                Marker(
                    state = remember(pin.id, lat, lng) { MarkerState(LatLng(lat, lng)) },
                    onClick = {
                        onPinClick(pin.id)
                        true
                    },
                )
            }
        }
    }
}
