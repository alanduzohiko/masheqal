package com.masheqal.app.util

import java.util.Locale

object PlaceLabelFormatter {
    fun format(cityName: String?, regionName: String?, countryName: String?): String =
        listOf(cityName, regionName, countryName)
            .mapNotNull { it?.trim()?.takeIf(String::isNotEmpty) }
            .distinctBy { it.lowercase(Locale.ROOT) }
            .joinToString(", ")
}
