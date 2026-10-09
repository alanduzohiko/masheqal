package com.masheqal.app.util

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.CancellationSignal
import androidx.core.content.ContextCompat
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

/**
 * A location fix carries its measured quality through the UI and prayer/Qibla features.
 * Approximate fixes are allowed, but callers can warn users when their accuracy is too low.
 */
data class CurrentLocation(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float = Float.POSITIVE_INFINITY,
    val timestampMillis: Long = 0L,
    val provider: String? = null
) {
    val isPrecise: Boolean
        get() = accuracyMeters.isFinite() && accuracyMeters <= LocationFixPolicy.PRECISE_ACCURACY_METERS
}

internal data class LocationCandidate(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float,
    val timestampMillis: Long,
    val provider: String?
)

/** Pure location-quality rules, deliberately separated from Android APIs for unit testing. */
internal object LocationFixPolicy {
    const val MAX_LAST_KNOWN_AGE_MILLIS = 30 * 60 * 1000L
    const val MAX_CURRENT_FIX_AGE_MILLIS = 2 * 60 * 1000L
    const val FUTURE_CLOCK_TOLERANCE_MILLIS = 60 * 1000L
    const val MAX_ACCEPTABLE_ACCURACY_METERS = 20_000f
    const val PRECISE_ACCURACY_METERS = 150f

    fun isUsable(
        fix: LocationCandidate,
        nowMillis: Long,
        maxAgeMillis: Long
    ): Boolean {
        if (!fix.latitude.isFinite() || fix.latitude !in -90.0..90.0) return false
        if (!fix.longitude.isFinite() || fix.longitude !in -180.0..180.0) return false
        if (!fix.accuracyMeters.isFinite() || fix.accuracyMeters <= 0f ||
            fix.accuracyMeters > MAX_ACCEPTABLE_ACCURACY_METERS
        ) return false

        val age = nowMillis - fix.timestampMillis
        if (age < -FUTURE_CLOCK_TOLERANCE_MILLIS) return false
        return age.coerceAtLeast(0L) <= maxAgeMillis
    }

    /**
     * Prioritize good accuracy without allowing an old fix to win indefinitely.
     * The score combines age (minutes) and uncertainty (75 metres per score point).
     */
    fun selectBest(
        fixes: List<LocationCandidate>,
        nowMillis: Long,
        maxAgeMillis: Long = MAX_LAST_KNOWN_AGE_MILLIS
    ): LocationCandidate? = fixes
        .asSequence()
        .filter { isUsable(it, nowMillis, maxAgeMillis) }
        .minByOrNull { fix ->
            val ageMinutes = (nowMillis - fix.timestampMillis).coerceAtLeast(0L) / 60_000.0
            ageMinutes * 3.0 + fix.accuracyMeters / 75.0
        }
}

object LocationUtils {
    private const val CURRENT_FIX_TIMEOUT_MILLIS = 12_000L
    private const val EARLY_ACCEPT_ACCURACY_METERS = 75f

    /**
     * Runtime permission is checked before provider access; a permission revocation race is
     * handled by the guarded provider call, which returns no fix instead of crashing.
     */
    @SuppressLint("MissingPermission")
    fun lastKnown(context: Context): CurrentLocation? {
        if (!hasLocationPermission(context)) return null

        val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val now = System.currentTimeMillis()
        val candidates = listOf(
            LocationManager.GPS_PROVIDER,
            LocationManager.NETWORK_PROVIDER,
            LocationManager.PASSIVE_PROVIDER
        ).mapNotNull { provider ->
            runCatching { manager.getLastKnownLocation(provider) }
                .getOrNull()
                ?.toCandidate()
        }

        return LocationFixPolicy.selectBest(candidates, now)?.toCurrentLocation()
    }

    /**
     * Requests GPS and network fixes together. A slow GPS provider no longer blocks a usable
     * network fix, and a rough network fix does not immediately beat a more accurate GPS result.
     */
    suspend fun current(context: Context): CurrentLocation? {
        if (!hasLocationPermission(context)) return null

        val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val providers = listOf(
            LocationManager.GPS_PROVIDER,
            LocationManager.NETWORK_PROVIDER
        ).filter { provider ->
            runCatching { manager.isProviderEnabled(provider) }.getOrDefault(false)
        }
        if (providers.isEmpty()) return lastKnown(context)

        val lock = Any()
        var bestObserved: LocationCandidate? = null
        val completedFix = withTimeoutOrNull(CURRENT_FIX_TIMEOUT_MILLIS) {
            suspendCancellableCoroutine<LocationCandidate?> { continuation ->
                val signals = providers.map { CancellationSignal() }
                val finished = BooleanArray(providers.size)
                var pending = providers.size

                fun cancelRequests() {
                    signals.forEach { signal -> runCatching { signal.cancel() } }
                }

                fun onProviderResult(index: Int, location: Location?) {
                    synchronized(lock) {
                        if (finished[index]) return
                        finished[index] = true
                        pending--

                        val candidate = location?.toCandidate()
                        if (candidate != null && LocationFixPolicy.isUsable(
                                candidate,
                                System.currentTimeMillis(),
                                LocationFixPolicy.MAX_CURRENT_FIX_AGE_MILLIS
                            )
                        ) {
                            bestObserved = LocationFixPolicy.selectBest(
                                listOfNotNull(bestObserved, candidate),
                                System.currentTimeMillis(),
                                LocationFixPolicy.MAX_CURRENT_FIX_AGE_MILLIS
                            )
                        }

                        val accurateEnough = bestObserved?.accuracyMeters
                            ?.let { it <= EARLY_ACCEPT_ACCURACY_METERS } == true
                        if (continuation.isActive && (accurateEnough || pending == 0)) {
                            continuation.resume(bestObserved)
                            cancelRequests()
                        }
                    }
                }

                continuation.invokeOnCancellation { cancelRequests() }

                providers.forEachIndexed { index, provider ->
                    try {
                        manager.getCurrentLocation(
                            provider,
                            signals[index],
                            context.mainExecutor
                        ) { location ->
                            onProviderResult(index, location)
                        }
                    } catch (_: SecurityException) {
                        onProviderResult(index, null)
                    } catch (_: IllegalArgumentException) {
                        onProviderResult(index, null)
                    }
                }
            }
        }

        val selected = completedFix ?: synchronized(lock) { bestObserved }
        if (selected != null) return selected.toCurrentLocation()
        return lastKnown(context)
    }

    private fun hasLocationPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

    private fun Location.toCandidate(): LocationCandidate {
        val accuracy = if (
            hasAccuracy() && accuracy.isFinite() && accuracy > 0f
        ) accuracy else 5_000f
        return LocationCandidate(
            latitude = latitude,
            longitude = longitude,
            accuracyMeters = accuracy,
            timestampMillis = time,
            provider = provider
        )
    }

    private fun LocationCandidate.toCurrentLocation() = CurrentLocation(
        latitude = latitude,
        longitude = longitude,
        accuracyMeters = accuracyMeters,
        timestampMillis = timestampMillis,
        provider = provider
    )
}
