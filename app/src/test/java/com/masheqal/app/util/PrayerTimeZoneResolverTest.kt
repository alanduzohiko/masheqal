package com.masheqal.app.util

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.ZoneId

class PrayerTimeZoneResolverTest {
    @Test
    fun resolvesKnownWorldCoordinatesWithinTheReportedCountry() = runBlocking {
        assertEquals(ZoneId.of("Asia/Baghdad"), PrayerTimeZoneResolver.resolve(35.56, 45.43, "IQ"))
        assertEquals(ZoneId.of("Europe/London"), PrayerTimeZoneResolver.resolve(51.5074, -0.1278, "GB"))
        assertEquals(ZoneId.of("America/New_York"), PrayerTimeZoneResolver.resolve(40.7128, -74.0060, "US"))
    }

    @Test
    fun rejectsInvalidCoordinatesAndUnknownCountry() = runBlocking {
        assertNull(PrayerTimeZoneResolver.resolve(91.0, 0.0, "IQ"))
        assertNull(PrayerTimeZoneResolver.resolve(0.0, 181.0, "IQ"))
        assertNull(PrayerTimeZoneResolver.resolve(35.56, 45.43, null))
        assertNull(PrayerTimeZoneResolver.resolve(35.56, 45.43, "???"))
    }
}
