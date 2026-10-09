package com.masheqal.app.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.iakovlev.timeshape.TimeZoneEngine
import java.time.ZoneId

/** Offline coordinate-to-IANA-time-zone resolver; uses bundled timezone-boundary-builder data. */
object PrayerTimeZoneResolver {
    @Volatile private var engine: TimeZoneEngine? = null
    @Volatile private var initializationAttempted = false
    private val initializationLock = Any()

    suspend fun resolve(latitude: Double, longitude: Double): ZoneId? {
        if (!latitude.isFinite() || latitude !in -90.0..90.0) return null
        if (!longitude.isFinite() || longitude !in -180.0..180.0) return null
        return withContext(Dispatchers.IO) {
            val activeEngine = engineOrNull() ?: return@withContext null
            runCatching { activeEngine.query(latitude, longitude).orElse(null) }.getOrNull()
        }
    }

    private fun engineOrNull(): TimeZoneEngine? {
        engine?.let { return it }
        if (initializationAttempted) return null
        return synchronized(initializationLock) {
            engine?.let { return@synchronized it }
            if (!initializationAttempted) {
                engine = try {
                    TimeZoneEngine.initialize()
                } catch (_: Exception) {
                    null
                } catch (_: LinkageError) {
                    null
                } finally {
                    initializationAttempted = true
                }
            }
            engine
        }
    }
}
