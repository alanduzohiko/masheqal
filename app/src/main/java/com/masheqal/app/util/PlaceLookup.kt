package com.masheqal.app.util

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.coroutines.resume

data class PlaceMatch(
    val latitude: Double,
    val longitude: Double,
    val cityName: String,
    val regionName: String? = null,
    val countryName: String? = null,
    val countryCode: String? = null
) {
    val displayName: String
        get() = PlaceLabelFormatter.format(cityName, regionName, countryName)
}

object PlaceLookup {
    private const val MAX_RESULTS = 8

    suspend fun reverseGeocode(context: Context, latitude: Double, longitude: Double): PlaceMatch? {
        if (!latitude.isFinite() || latitude !in -90.0..90.0) return null
        if (!longitude.isFinite() || longitude !in -180.0..180.0) return null
        return getAddresses(context) { geocoder ->
            reverseAddresses(geocoder, latitude, longitude)
        }.asSequence().mapNotNull(::toPlaceMatch).firstOrNull { it.displayName.isNotBlank() }
    }

    suspend fun searchByName(context: Context, query: String): List<PlaceMatch> {
        val cleanQuery = query.trim()
        if (cleanQuery.isEmpty()) return emptyList()
        // Keep a small, transparent offline fallback for common Iraqi cities. If there is no
        // local match, use Android Geocoder for a broader world-wide search.
        IraqOfflineCityCatalog.search(cleanQuery).takeIf { it.isNotEmpty() }?.let { return it }
        return getAddresses(context) { geocoder ->
            searchAddresses(geocoder, cleanQuery)
        }.mapNotNull(::toPlaceMatch)
            .distinctBy { it.displayName.lowercase(Locale.ROOT) }
            .take(MAX_RESULTS)
    }

    private suspend fun getAddresses(
        context: Context,
        request: suspend (Geocoder) -> List<Address>
    ): List<Address> = withContext(Dispatchers.IO) {
        if (!Geocoder.isPresent()) return@withContext emptyList()
        try {
            request(Geocoder(context, Locale.getDefault()))
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            emptyList()
        }
    }

    @Suppress("DEPRECATION")
    private suspend fun reverseAddresses(geocoder: Geocoder, latitude: Double, longitude: Double): List<Address> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            callbackGeocode { listener -> geocoder.getFromLocation(latitude, longitude, MAX_RESULTS, listener) }
        } else {
            runCatching { geocoder.getFromLocation(latitude, longitude, MAX_RESULTS) ?: emptyList() }.getOrDefault(emptyList())
        }

    @Suppress("DEPRECATION")
    private suspend fun searchAddresses(geocoder: Geocoder, query: String): List<Address> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            callbackGeocode { listener -> geocoder.getFromLocationName(query, MAX_RESULTS, listener) }
        } else {
            runCatching { geocoder.getFromLocationName(query, MAX_RESULTS) ?: emptyList() }.getOrDefault(emptyList())
        }

    @androidx.annotation.RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private suspend fun callbackGeocode(start: (Geocoder.GeocodeListener) -> Unit): List<Address> =
        suspendCancellableCoroutine { continuation ->
            try {
                start(object : Geocoder.GeocodeListener {
                    override fun onGeocode(addresses: MutableList<Address>) {
                        if (continuation.isActive) continuation.resume(addresses)
                    }
                    override fun onError(errorMessage: String?) {
                        if (continuation.isActive) continuation.resume(emptyList())
                    }
                })
            } catch (_: Exception) {
                if (continuation.isActive) continuation.resume(emptyList())
            }
        }

    private fun toPlaceMatch(address: Address): PlaceMatch? {
        if (!address.hasLatitude() || !address.hasLongitude()) return null
        val country = address.countryName?.trim()?.takeIf(String::isNotEmpty)
        val city = sequenceOf(address.locality, address.subAdminArea, address.adminArea, address.featureName)
            .mapNotNull { it?.trim()?.takeIf(String::isNotEmpty) }
            .firstOrNull() ?: country ?: return null
        val region = sequenceOf(address.adminArea, address.subAdminArea)
            .mapNotNull { it?.trim()?.takeIf(String::isNotEmpty) }
            .firstOrNull { !it.equals(city, ignoreCase = true) }
        return PlaceMatch(address.latitude, address.longitude, city, region, country, address.countryCode?.uppercase(Locale.ROOT))
    }
}

object LocationChoiceStore {
    private const val PREFS = "masheqal_location_choice"
    private const val KEY_MANUAL = "manual"
    private const val KEY_LATITUDE = "latitude"
    private const val KEY_LONGITUDE = "longitude"
    private const val KEY_LABEL = "label"
    private const val KEY_COUNTRY = "country"
    private const val KEY_COUNTRY_CODE = "countryCode"

    fun loadManual(context: Context): CurrentLocation? {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (!prefs.getBoolean(KEY_MANUAL, false)) return null
        val latitude = prefs.getString(KEY_LATITUDE, null)?.toDoubleOrNull() ?: return null
        val longitude = prefs.getString(KEY_LONGITUDE, null)?.toDoubleOrNull() ?: return null
        val label = prefs.getString(KEY_LABEL, null)?.trim()?.takeIf(String::isNotEmpty) ?: return null
        if (!latitude.isFinite() || latitude !in -90.0..90.0) return null
        if (!longitude.isFinite() || longitude !in -180.0..180.0) return null
        return CurrentLocation(
            latitude = latitude,
            longitude = longitude,
            accuracyMeters = 20_000f,
            timestampMillis = System.currentTimeMillis(),
            provider = "manual",
            placeName = label,
            countryName = prefs.getString(KEY_COUNTRY, null),
            countryCode = prefs.getString(KEY_COUNTRY_CODE, null),
            isManual = true
        )
    }

    fun saveManual(context: Context, place: PlaceMatch): CurrentLocation {
        require(place.latitude.isFinite() && place.latitude in -90.0..90.0)
        require(place.longitude.isFinite() && place.longitude in -180.0..180.0)
        require(place.displayName.isNotBlank())
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putBoolean(KEY_MANUAL, true)
            .putString(KEY_LATITUDE, place.latitude.toString())
            .putString(KEY_LONGITUDE, place.longitude.toString())
            .putString(KEY_LABEL, place.displayName)
            .putString(KEY_COUNTRY, place.countryName)
            .putString(KEY_COUNTRY_CODE, place.countryCode?.uppercase(Locale.ROOT))
            .apply()
        return loadManual(context) ?: error("Saved city could not be read back")
    }

    fun clearManual(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().apply()
    }
}
