package com.masheqal.app.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.core.content.ContextCompat
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

data class CurrentLocation(val latitude: Double, val longitude: Double)

object LocationUtils {
    fun lastKnown(context: Context): CurrentLocation? {
        if (
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) return null

        val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
        val locations = providers.mapNotNull {
            runCatching { manager.getLastKnownLocation(it) }.getOrNull()
        }
        val best = locations.maxByOrNull { it.time } ?: return null
        return best.toCurrentLocation()
    }

    suspend fun current(context: Context): CurrentLocation? {
        if (
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) return null

        val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val provider = listOf(
            LocationManager.GPS_PROVIDER,
            LocationManager.NETWORK_PROVIDER
        ).firstOrNull { runCatching { manager.isProviderEnabled(it) }.getOrDefault(false) }
            ?: return lastKnown(context)

        return suspendCancellableCoroutine { continuation ->
            runCatching {
                manager.getCurrentLocation(
                    provider,
                    null,
                    context.mainExecutor
                ) { location ->
                    if (continuation.isActive) {
                        continuation.resume(location?.toCurrentLocation())
                    }
                }
            }.onFailure {
                if (continuation.isActive) {
                    continuation.resume(lastKnown(context))
                }
            }
        }
    }

    private fun Location.toCurrentLocation(): CurrentLocation =
        CurrentLocation(latitude = latitude, longitude = longitude)
}
