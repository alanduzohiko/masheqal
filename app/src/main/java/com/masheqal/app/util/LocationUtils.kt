package com.masheqal.app.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Looper
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

        val saved = context.getSharedPreferences("masheqal_location", Context.MODE_PRIVATE)
        val savedLat = saved.getString("latitude", null)?.toDoubleOrNull()
        val savedLon = saved.getString("longitude", null)?.toDoubleOrNull()
        if (savedLat != null && savedLon != null && savedLat in -90.0..90.0 && savedLon in -180.0..180.0) {
            return CurrentLocation(savedLat, savedLon)
        }

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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                runCatching {
                    manager.getCurrentLocation(
                        provider,
                        null,
                        context.mainExecutor
                    ) { location ->
                        val result = location?.toCurrentLocation()
                        if (result != null) persist(context, result)
                        if (continuation.isActive) {
                            continuation.resume(result ?: lastKnown(context))
                        }
                    }
                }.onFailure {
                    if (continuation.isActive) {
                        continuation.resume(lastKnown(context))
                    }
                }
            } else {
                val listener = object : LocationListener {
                    override fun onLocationChanged(location: Location) {
                        manager.removeUpdates(this)
                        val result = location.toCurrentLocation()
                        persist(context, result)
                        if (continuation.isActive) continuation.resume(result)
                    }

                    @Deprecated("Deprecated in API 29")
                    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit

                    override fun onProviderEnabled(provider: String) = Unit

                    override fun onProviderDisabled(provider: String) {
                        manager.removeUpdates(this)
                        if (continuation.isActive) continuation.resume(lastKnown(context))
                    }
                }
                continuation.invokeOnCancellation {
                    runCatching { manager.removeUpdates(listener) }
                }
                runCatching {
                    @Suppress("DEPRECATION")
                    manager.requestSingleUpdate(provider, listener, Looper.getMainLooper())
                }.onFailure {
                    if (continuation.isActive) continuation.resume(lastKnown(context))
                }
            }
        }
    }

    private fun persist(context: Context, location: CurrentLocation) {
        context.getSharedPreferences("masheqal_location", Context.MODE_PRIVATE).edit()
            .putString("latitude", location.latitude.toString())
            .putString("longitude", location.longitude.toString())
            .apply()
    }

    private fun Location.toCurrentLocation(): CurrentLocation =
        CurrentLocation(latitude = latitude, longitude = longitude)
}
