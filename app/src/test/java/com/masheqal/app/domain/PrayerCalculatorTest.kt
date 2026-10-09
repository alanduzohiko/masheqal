package com.masheqal.app.domain

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PrayerCalculatorTest {
    private val sulaymaniyah = Coordinates(35.56, 45.43, 3.0)

    @Test
    fun matchesUpstreamAdhanNorthAmericaHanafiReferenceVector() {
        // Reference vector from the upstream Adhan Java project's test suite:
        // 2015-07-12, Raleigh NC, North America method, Hanafi, America/New_York (UTC-04:00).
        val result = PrayerCalculator.calculate(
            date = LocalDate.of(2015, 7, 12),
            c = Coordinates(35.7750, -78.6336, -4.0),
            method = PrayerMethod.ISNA,
            madhhab = AsrMadhhab.HANAFI,
            highLatitudeRule = HighLatitudeRule.MIDDLE_OF_NIGHT
        )
        assertEquals(4 * 60 + 42, result.fajr.toInt())
        assertEquals(6 * 60 + 8, result.sunrise.toInt())
        assertEquals(13 * 60 + 21, result.dhuhr.toInt())
        assertEquals(18 * 60 + 22, result.asr.toInt())
        assertEquals(20 * 60 + 32, result.maghrib.toInt())
        assertEquals(21 * 60 + 57, result.isha.toInt())
    }

    @Test
    fun prayerOrderIsChronologicalForCommonMethodsAndMadhhabs() {
        val date = LocalDate.of(2026, 10, 8)
        for (method in PrayerMethod.entries) {
            for (madhhab in AsrMadhhab.entries) {
                val p = PrayerCalculator.calculate(date, sulaymaniyah, method, madhhab)
                val values = listOf(p.fajr, p.sunrise, p.dhuhr, p.asr, p.maghrib, p.isha)
                assertTrue("$method/$madhhab has non-finite value: $values", values.all(Double::isFinite))
                assertTrue("$method/$madhhab has impossible order: $values", values.zipWithNext().all { (a, b) -> a < b })
            }
        }
    }

    @Test
    fun asrChangesWithMadhhabButStaysBeforeMaghrib() {
        val date = LocalDate.of(2026, 10, 8)
        val shafi = PrayerCalculator.calculate(date, sulaymaniyah, PrayerMethod.MWL, AsrMadhhab.SHAFI)
        val hanafi = PrayerCalculator.calculate(date, sulaymaniyah, PrayerMethod.MWL, AsrMadhhab.HANAFI)
        assertTrue(shafi.asr < hanafi.asr)
        assertTrue(hanafi.asr < hanafi.maghrib)
    }

    @Test
    fun utcOffsetChangesLocalClockTimesByExpectedAmount() {
        val date = LocalDate.of(2026, 10, 8)
        val utc = PrayerCalculator.calculate(date, Coordinates(35.56, 45.43, 0.0))
        val baghdad = PrayerCalculator.calculate(date, Coordinates(35.56, 45.43, 3.0))
        assertEquals(180, (baghdad.dhuhr - utc.dhuhr).toInt())
        assertEquals(180, (baghdad.asr - utc.asr).toInt())
    }

    @Test
    fun rejectsInvalidCoordinatesAndOffsets() {
        val date = LocalDate.of(2026, 10, 8)
        runCatching { PrayerCalculator.calculate(date, Coordinates(95.0, 45.0, 3.0)) }
            .onSuccess { throw AssertionError("Latitude outside the valid range was accepted") }
        runCatching { PrayerCalculator.calculate(date, Coordinates(35.0, 45.0, 15.0)) }
            .onSuccess { throw AssertionError("An invalid UTC offset was accepted") }
    }
}
