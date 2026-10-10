package com.masheqal.app.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId

class PrayerTimeZoneResolverTest {
    @Test
    fun prefersNamedRegionalZoneWhenTimezoneBoundariesOverlap() {
        val selected = PrayerTimeZoneResolver.chooseRegionalZone(
            listOf(ZoneId.of("Etc/GMT-3"), ZoneId.of("Asia/Baghdad"))
        )
        assertEquals(ZoneId.of("Asia/Baghdad"), selected)
    }

    @Test
    fun fallsBackToOnlyAvailableZoneWhenNoRegionalZoneExists() {
        assertEquals(
            ZoneId.of("Etc/GMT-3"),
            PrayerTimeZoneResolver.chooseRegionalZone(listOf(ZoneId.of("Etc/GMT-3")))
        )
        assertNull(PrayerTimeZoneResolver.chooseRegionalZone(emptyList()))
    }

    @Test
    fun validatesCoordinatesAndCountryCodeWithoutAndroidRuntime() {
        assertTrue(PrayerTimeZoneResolver.hasValidCoordinates(35.56, 45.43))
        assertFalse(PrayerTimeZoneResolver.hasValidCoordinates(91.0, 0.0))
        assertFalse(PrayerTimeZoneResolver.hasValidCoordinates(0.0, Double.NaN))
        assertEquals("IQ", PrayerTimeZoneResolver.normalizeCountryCode(" iq "))
        assertNull(PrayerTimeZoneResolver.normalizeCountryCode(null))
        assertNull(PrayerTimeZoneResolver.normalizeCountryCode("???"))
    }
}
