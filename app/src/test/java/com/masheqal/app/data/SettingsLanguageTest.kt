package com.masheqal.app.data

import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsLanguageTest {
    @Test
    fun arabicAndEnglishAreTheOnlySupportedUiLanguages() {
        assertEquals("ar", normalizeAppLanguage("ar"))
        assertEquals("en", normalizeAppLanguage("en"))
        assertEquals("en", normalizeAppLanguage(" EN "))
        assertEquals("ar", normalizeAppLanguage("AR"))
    }

    @Test
    fun deferredOrUnknownLanguagesSafelyFallBackToArabic() {
        assertEquals("ar", normalizeAppLanguage("ckb"))
        assertEquals("ar", normalizeAppLanguage("fr"))
        assertEquals("ar", normalizeAppLanguage(null))
        assertEquals("ar", normalizeAppLanguage(""))
    }
}
