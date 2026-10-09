package com.masheqal.app.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class IraqOfflineCityCatalogTest {
    @Test
    fun findsSulaymaniyahByEnglishAliasAndReturnsCountryCode() {
        val place = IraqOfflineCityCatalog.search("Slemani", Locale.ENGLISH).single()
        assertEquals("Sulaymaniyah", place.cityName)
        assertEquals("IQ", place.countryCode)
        assertTrue(place.latitude in 35.0..36.0)
        assertTrue(place.longitude in 45.0..46.0)
    }

    @Test
    fun returnsArabicLabelsForArabicUi() {
        val place = IraqOfflineCityCatalog.search("السليمانية", Locale("ar")).single()
        assertEquals("السليمانية", place.cityName)
        assertEquals("العراق", place.countryName)
        assertEquals("IQ", place.countryCode)
    }

    @Test
    fun supportsPartialCityQueryAndIgnoresEmptyQuery() {
        assertTrue(IraqOfflineCityCatalog.search("Bagh").any { it.cityName == "Baghdad" })
        assertTrue(IraqOfflineCityCatalog.search(" ").isEmpty())
    }
}
