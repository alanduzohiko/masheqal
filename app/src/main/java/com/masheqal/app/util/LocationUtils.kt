package com.masheqal.app.util

import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.core.content.ContextCompat
import android.Manifest

data class CurrentLocation(val latitude: Double, val longitude: Double)
object LocationUtils {
    fun lastKnown(context: Context): CurrentLocation? {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) return null
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
        val locations = providers.mapNotNull { runCatching { lm.getLastKnownLocation(it) }.getOrNull() }
        val best = locations.maxByOrNull { it.time } ?: return null
        return CurrentLocation(best.latitude, best.longitude)
    }
}
