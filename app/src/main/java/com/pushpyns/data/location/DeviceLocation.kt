package com.pushpyns.data.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Build
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.pushpyns.di.IoDispatcher
import com.pushpyns.domain.model.GeoPoint
import com.pushpyns.domain.model.PostalAddress
import com.pushpyns.domain.repository.AddressLookup
import com.pushpyns.domain.repository.LocationSource
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

@Singleton
class DeviceLocationSource
@Inject
constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val io: CoroutineDispatcher,
) : LocationSource {
    override fun hasPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
        return fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("MissingPermission")
    override suspend fun current(): GeoPoint? = withContext(io) {
        if (!hasPermission()) return@withContext null
        try {
            val client = LocationServices.getFusedLocationProviderClient(context)
            val last = client.lastLocation.awaitOrNull()
            val fix =
                last ?: client
                    .getCurrentLocation(
                        Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                        CancellationTokenSource().token,
                    ).awaitOrNull()
            fix?.let { GeoPoint(it.latitude, it.longitude) }
        } catch (_: Exception) {
            null
        }
    }
}

@Singleton
class AndroidAddressLookup
@Inject
constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val io: CoroutineDispatcher,
) : AddressLookup {
    override suspend fun reverse(latitude: Double, longitude: Double): PostalAddress? = withContext(io) {
        try {
            if (!Geocoder.isPresent()) return@withContext null
            val geocoder = Geocoder(context)
            val results =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    suspendCancellableCoroutine { cont ->
                        geocoder.getFromLocation(latitude, longitude, 1) { addresses ->
                            if (cont.isActive) cont.resume(addresses)
                        }
                    }
                } else {
                    @Suppress("DEPRECATION")
                    geocoder.getFromLocation(latitude, longitude, 1).orEmpty()
                }
            results.firstOrNull()?.toPostal()
        } catch (_: Exception) {
            null
        }
    }

    private fun Address.toPostal(): PostalAddress {
        val street =
            listOfNotNull(subThoroughfare, thoroughfare).joinToString(" ").ifBlank {
                getAddressLine(0).orEmpty().substringBefore(",")
            }
        return PostalAddress(
            fullLine = getAddressLine(0).orEmpty(),
            street = street,
            city = locality.orEmpty(),
            state = adminArea.orEmpty(),
            zip = postalCode.orEmpty(),
        )
    }
}

private suspend fun com.google.android.gms.tasks.Task<Location>.awaitOrNull(): Location? = suspendCancellableCoroutine { cont ->
    addOnSuccessListener { if (cont.isActive) cont.resume(it) }
    addOnFailureListener { if (cont.isActive) cont.resume(null) }
    addOnCanceledListener { if (cont.isActive) cont.resume(null) }
}
