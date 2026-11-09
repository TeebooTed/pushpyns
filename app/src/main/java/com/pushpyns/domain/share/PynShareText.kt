package com.pushpyns.domain.share

import com.pushpyns.domain.geo.formatCoordinates
import com.pushpyns.domain.geo.formattedAddress
import com.pushpyns.domain.model.GeoPoint
import com.pushpyns.domain.model.Pyn

fun pynShareText(pyn: Pyn): String = buildString {
    append(pyn.name.trim())
    val address = pyn.formattedAddress()
    if (address.isNotEmpty()) {
        append('\n')
        append(address)
    }
    if (pyn.description.isNotBlank()) {
        append("\n\n")
        append(pyn.description.trim())
    }
    if (pyn.notes.isNotBlank()) {
        append("\n\n")
        append(pyn.notes.trim())
    }
    val lat = pyn.latitude
    val lng = pyn.longitude
    if (lat != null && lng != null) {
        append("\n\n")
        append(formatCoordinates(lat, lng))
    }
    if (pyn.categories.isNotEmpty()) {
        append('\n')
        append("Categories: ")
        append(pyn.categories.joinToString(", ") { it.name })
    }
    append("\n\nShared from Pushpyns")
}

fun locationShareText(address: String, point: GeoPoint?): String = buildString {
    append("My location")
    if (address.isNotBlank()) {
        append("\n")
        append(address.trim())
    }
    if (point != null) {
        append("\n")
        append(formatCoordinates(point.latitude, point.longitude))
    }
    append("\n\nShared from Pushpyns")
}
