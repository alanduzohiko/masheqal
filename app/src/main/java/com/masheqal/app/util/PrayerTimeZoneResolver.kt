package com.masheqal.app.util

import android.icu.util.TimeZone
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.iakovlev.timeshape.TimeZoneEngine
import java.time.ZoneId
import java.util.LinkedHashMap
import java.util.Locale

/**
 * Offline coordinate-to-IANA-zone resolver. It loads only the time-zone shapes associated with
 * the place's ISO country code, not every polygon on Earth, to keep memory use bounded on phones.
 * The country code comes from Android Geocoder (or a manually selected city); without one, callers
 * use the device zone and disclose the fallback instead of guessing from longitude.
 */
object PrayerTimeZoneResolver {
    private const val MAX_CACHED_COUNTRIES = 2
    private val engineLock = Any()
    private val engines = LinkedHashMap<String, TimeZoneEngine>(4, 0.75f, true)

    suspend fun resolve(latitude: Double, longitude: Double, countryCode: String?): ZoneId? {
        if (!latitude.isFinite() || latitude !in -90.0..90.0) return null
        if (!longitude.isFinite() || longitude !in -180.0..180.0) return null
        val country = countryCode?.trim()?.uppercase(Locale.ROOT)
            ?.takeIf { it.matches(Regex("[A-Z]{2}")) } ?: return null

        return withContext(Dispatchers.IO) {
            val engine = engineForCountry(country) ?: return@withContext null
            runCatching {
                // Some boundary sources overlap. Prefer a named regional IANA zone to Etc/GMT.
                engine.queryAll(latitude, longitude)
                    .firstOrNull { zone ->
                        !zone.id.startsWith("Etc/") && zone.id != "UTC" && zone.id != "GMT"
                    }
                    ?: engine.query(latitude, longitude).orElse(null)
            }.getOrNull()
        }
    }

    private fun engineForCountry(countryCode: String): TimeZoneEngine? = synchronized(engineLock) {
        engines[countryCode]?.let { return@synchronized it }
        val zones = runCatching {
            TimeZone.getAvailableIDs(countryCode)
                .mapNotNull { id -> runCatching { ZoneId.of(id) }.getOrNull() }
                .toSet()
        }.getOrDefault(emptySet())
        if (zones.isEmpty()) return@synchronized null
        val created = runCatching { TimeZoneEngine.initialize(zones, false) }.getOrNull()
            ?: return@synchronized null
        engines[countryCode] = created
        while (engines.size > MAX_CACHED_COUNTRIES) {
            val iterator = engines.entries.iterator()
            if (iterator.hasNext()) {
                iterator.next()
                iterator.remove()
            } else break
        }
        created
    }
}
