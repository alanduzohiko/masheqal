package com.masheqal.app.ui.screens

import com.masheqal.app.data.QuranRepository

/** Shared Arabic/English matching for global search results. */
object GlobalSearchMatcher {
    fun matches(query: String, fields: Iterable<String?>): Boolean {
        val needle = QuranRepository.normalize(query)
        if (needle.isBlank()) return false
        return fields.any { value ->
            !value.isNullOrBlank() && QuranRepository.normalize(value).contains(needle)
        }
    }
}
