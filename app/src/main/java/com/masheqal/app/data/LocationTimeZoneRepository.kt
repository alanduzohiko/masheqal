package com.masheqal.app.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.time.ZoneId
import kotlin.math.abs
import kotlin.math.round

/**
 * Resolves the IANA timezone for the selected location, caching the result on-device.
 * Only coordinates rounded to two decimal places are sent to the lookup service.
 * If offline or the service is unavailable, the app quietly uses the device timezone.
 */
class LocationTimeZoneRepository(private val context: Context) {
    suspend fun resolve(latitude: Double, longitude: Double): ZoneId = withContext(Dispatchers.IO) {
        require(latitude in -90.0..90.0 && longitude in -180.0..180.0)
        val prefs = context.getSharedPreferences("location_timezone", Context.MODE_PRIVATE)
        val cachedLat = prefs.getString("latitude", null)?.toDoubleOrNull()
        val cachedLon = prefs.getString("longitude", null)?.toDoubleOrNull()
        val cachedZone = prefs.getString("zoneId", null)
        val cacheAge = System.currentTimeMillis() - prefs.getLong("savedAt", 0L)

        if (cachedLat != null && cachedLon != null && !cachedZone.isNullOrBlank() &&
            abs(cachedLat - latitude) <= 0.02 && abs(cachedLon - longitude) <= 0.02 &&
            cacheAge in 0..CACHE_MAX_AGE_MS
        ) {
            runCatching { ZoneId.of(cachedZone) }.getOrNull()?.let { return@withContext it }
        }

        val roundedLat = round(latitude * COORDINATE_PRECISION) / COORDINATE_PRECISION
        val roundedLon = round(longitude * COORDINATE_PRECISION) / COORDINATE_PRECISION
        val resolved = runCatching {
            val url = URL(
                "https:" + "/" + "/timeapi.io/api/TimeZone/coordinate?latitude=" +
                    roundedLat + "&longitude=" + roundedLon
            )
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 5000
                readTimeout = 7000
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", "Masheqal-Android")
            }
            try {
                check(connection.responseCode in 200..299) { "Time-zone service unavailable" }
                val body = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                val root = JSONObject(body)
                val details = root.optJSONObject("timeZoneInfo")
                val id = root.optString("timeZone", "").takeIf { it.isNotBlank() }
                    ?: details?.optString("ianaTimeId", "").orEmpty()
                ZoneId.of(id).also { zone ->
                    prefs.edit()
                        .putString("latitude", roundedLat.toString())
                        .putString("longitude", roundedLon.toString())
                        .putString("zoneId", zone.id)
                        .putLong("savedAt", System.currentTimeMillis())
                        .apply()
                }
            } finally {
                connection.disconnect()
            }
        }.getOrNull()

        resolved ?: runCatching { cachedZone?.let { ZoneId.of(it) } }.getOrNull()
            ?: ZoneId.systemDefault()
    }

    companion object {
        private const val COORDINATE_PRECISION = 100.0
        private const val CACHE_MAX_AGE_MS = 30L * 24L * 60L * 60L * 1000L
    }
}
