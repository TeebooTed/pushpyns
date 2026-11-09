package com.pushpyns.domain.geo

import com.pushpyns.domain.model.GeoPoint
import com.pushpyns.domain.model.Pyn
import com.pushpyns.domain.model.PynSort
import java.util.Locale
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

private const val EARTH_RADIUS_METERS = 6_371_000.0

fun haversineMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val phi1 = Math.toRadians(lat1)
    val phi2 = Math.toRadians(lat2)
    val dPhi = Math.toRadians(lat2 - lat1)
    val dLambda = Math.toRadians(lon2 - lon1)
    val a = sin(dPhi / 2).pow(2) + cos(phi1) * cos(phi2) * sin(dLambda / 2).pow(2)
    return 2 * EARTH_RADIUS_METERS * asin(sqrt(a))
}

fun distanceMeters(pyn: Pyn, origin: GeoPoint?): Double? {
    val lat = pyn.latitude ?: return null
    val lng = pyn.longitude ?: return null
    val point = origin ?: return null
    return haversineMeters(point.latitude, point.longitude, lat, lng)
}

fun formatDistance(meters: Double): String {
    val miles = meters / 1609.344
    return when {
        miles < 0.1 -> "${(meters * 3.28084).roundToInt()} ft"
        miles < 10 -> String.format(Locale.US, "%.1f mi", miles)
        else -> "${miles.roundToInt()} mi"
    }
}

fun formatCoordinates(latitude: Double, longitude: Double): String = String.format(Locale.US, "%.6f, %.6f", latitude, longitude)

fun formatAddress(address: String, city: String, state: String, zip: String): String {
    val street = address.trim()
    val cityState = listOf(city.trim(), state.trim()).filter { it.isNotEmpty() }.joinToString(", ")
    val cityStateZip = listOf(cityState, zip.trim()).filter { it.isNotEmpty() }.joinToString(" ")
    return listOf(street, cityStateZip).filter { it.isNotEmpty() }.joinToString(", ")
}

fun Pyn.formattedAddress(): String = formatAddress(address, city, state, zip)

fun Pyn.primaryCategoryName(): String = categories.minOfOrNull { it.name.lowercase(Locale.US) }.orEmpty()

fun Pyn.matchesQuery(query: String): Boolean {
    val needle = query.trim().lowercase(Locale.US)
    if (needle.isEmpty()) return true
    val fields = listOf(name, description, notes, address, city, state, zip)
    return fields.any { it.lowercase(Locale.US).contains(needle) } ||
        categories.any { it.name.lowercase(Locale.US).contains(needle) }
}

object PynSorter {
    fun sort(pyns: List<Pyn>, sort: PynSort, origin: GeoPoint?): List<Pyn> = when (sort) {
        PynSort.NAME -> pyns.sortedBy { it.name.lowercase(Locale.US) }
        PynSort.DATE -> pyns.sortedByDescending { it.createdAt }
        PynSort.CATEGORY ->
            pyns.sortedWith(
                compareBy<Pyn> { if (it.categories.isEmpty()) "\uFFFF" else it.primaryCategoryName() }
                    .thenBy { it.name.lowercase(Locale.US) },
            )
        PynSort.DISTANCE ->
            pyns.sortedWith(
                compareBy<Pyn> { distanceMeters(it, origin) ?: Double.MAX_VALUE }
                    .thenBy { it.name.lowercase(Locale.US) },
            )
    }
}
