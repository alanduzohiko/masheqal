package com.masheqal.app.ui.screens

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GlobalSearchMatcherTest {
    @Test
    fun matchesArabicWithoutDiacriticsAgainstVocalizedText() {
        assertTrue(GlobalSearchMatcher.matches("رحمن", listOf("الرَّحْمَٰن")))
    }

    @Test
    fun matchesEnglishTranslationAndTransliterationCaseInsensitively() {
        assertTrue(GlobalSearchMatcher.matches("merciful", listOf("Ar-Raheem", "The Most Merciful")))
    }

    @Test
    fun blankQueryNeverMatches() {
        assertFalse(GlobalSearchMatcher.matches("   ", listOf("Quran", "القرآن")))
    }

    @Test
    fun emptyFieldsDoNotCreateFalsePositive() {
        assertFalse(GlobalSearchMatcher.matches("unmatched", listOf(null, "", "   ")))
    }
}
