package com.masheqal.app.util

import java.text.Normalizer
import java.util.Locale

/**
 * Offline fallback for a small number of major Iraqi cities. These coordinates represent
 * approximate city centres, not a user's precise position. Other places are resolved through
 * Android Geocoder when available.
 *
 * Coordinate source: SimpleMaps free Iraq Cities Database (MIT-licensed subset).
 */
object IraqOfflineCityCatalog {
    private data class City(
        val englishName: String,
        val arabicName: String,
        val englishRegion: String,
        val arabicRegion: String,
        val latitude: Double,
        val longitude: Double,
        val aliases: List<String> = emptyList()
    )

    private val cities = listOf(
        City("Baghdad", "بغداد", "Baghdad Governorate", "محافظة بغداد", 33.3153, 44.3661,
            listOf("Bagdad", "Baghdad Iraq")),
        City("Mosul", "الموصل", "Nineveh", "نينوى", 36.3667, 43.1167,
            listOf("Al Mawsil", "Mousl")),
        City("Basra", "البصرة", "Basra Governorate", "محافظة البصرة", 30.5150, 47.8100,
            listOf("Basrah", "Al Basrah")),
        City("Kirkuk", "كركوك", "Kirkuk Governorate", "محافظة كركوك", 35.4667, 44.4000,
            listOf("Karkuk", "Kirkūk")),
        City("Sulaymaniyah", "السليمانية", "Sulaymaniyah Governorate", "محافظة السليمانية",
            35.5500, 45.4333, listOf("Sulaimani", "Slemani", "As Sulaymaniyah"))
    )

    fun search(query: String, locale: Locale = Locale.getDefault()): List<PlaceMatch> {
        val cleaned = normalize(query)
        if (cleaned.isBlank()) return emptyList()
        val matches = cities.filter { city ->
            (listOf(city.englishName, city.arabicName, city.englishRegion, city.arabicRegion) +
                city.aliases + "Iraq" + "العراق")
                .any { label ->
                    val normalized = normalize(label)
                    normalized.contains(cleaned) || cleaned.contains(normalized)
                }
        }
        val arabic = locale.language.equals("ar", ignoreCase = true)
        return matches.map { city ->
            PlaceMatch(
                latitude = city.latitude,
                longitude = city.longitude,
                cityName = if (arabic) city.arabicName else city.englishName,
                regionName = if (arabic) city.arabicRegion else city.englishRegion,
                countryName = if (arabic) "العراق" else "Iraq",
                countryCode = "IQ"
            )
        }
    }

    private fun normalize(value: String): String =
        Normalizer.normalize(value.trim(), Normalizer.Form.NFD)
            .replace("\\p{Mn}+".toRegex(), "")
            .lowercase(Locale.ROOT)
            .replace('’', '\'')
            .replace("ı", "i")
            .replace("ş", "s")
            .replace("ğ", "g")
}
