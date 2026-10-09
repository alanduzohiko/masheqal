package com.masheqal.app.domain

import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import com.masheqal.app.ui.screens.QuranAudioCatalog

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
    fun nextPrayerAfterIshaUsesTomorrowFajrTime() {
        val prayers = listOf(
            "Fajr" to 300.0,
            "Dhuhr" to 730.0,
            "Asr" to 930.0,
            "Maghrib" to 1100.0,
            "Isha" to 1200.0
        )
        assertEquals("Dhuhr" to 730.0, PrayerCalculator.selectNextPrayer(700.0, prayers, 290.0))
        assertEquals("Isha" to 1200.0, PrayerCalculator.selectNextPrayer(1190.0, prayers, 290.0))
        assertEquals("Fajr" to 290.0, PrayerCalculator.selectNextPrayer(1300.0, prayers, 290.0))
        assertNull(PrayerCalculator.selectNextPrayer(1300.0, prayers, null))
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
    fun zoneIdUsesDaylightSavingRulesForThePrayerDate() {
        val date = LocalDate.of(2026, 7, 12)
        val coordinates = Coordinates(35.7750, -78.6336, -5.0)
        val daylightSaving = PrayerCalculator.calculate(
            date = date,
            c = coordinates,
            method = PrayerMethod.ISNA,
            madhhab = AsrMadhhab.HANAFI,
            highLatitudeRule = HighLatitudeRule.MIDDLE_OF_NIGHT,
            zoneId = ZoneId.of("America/New_York")
        )
        val fixedUtcMinusFive = PrayerCalculator.calculate(
            date = date,
            c = coordinates,
            method = PrayerMethod.ISNA,
            madhhab = AsrMadhhab.HANAFI,
            highLatitudeRule = HighLatitudeRule.MIDDLE_OF_NIGHT,
            zoneId = ZoneId.of("Etc/GMT+5")
        )

        assertEquals(60.0, daylightSaving.fajr - fixedUtcMinusFive.fajr, 0.0)
        assertEquals(60.0, daylightSaving.dhuhr - fixedUtcMinusFive.dhuhr, 0.0)
        assertEquals(60.0, daylightSaving.isha - fixedUtcMinusFive.isha, 0.0)
    }

    @Test
    fun alarmTargetIsResolvedAsLocalWallClockTimeAcrossDstTransition() {
        val date = LocalDate.of(2026, 3, 8)
        val zone = ZoneId.of("America/New_York")
        val instant = PrayerCalculator.instantForLocalPrayerMinute(date, 5 * 60.0, zone)
        val local = instant.atZone(zone)

        assertEquals(date, local.toLocalDate())
        assertEquals(5, local.hour)
        assertEquals(0, local.minute)
    }

    @Test
    fun rejectsInvalidCoordinatesAndOffsets() {
        val date = LocalDate.of(2026, 10, 8)
        runCatching { PrayerCalculator.calculate(date, Coordinates(95.0, 45.0, 3.0)) }
            .onSuccess { throw AssertionError("Latitude outside the valid range was accepted") }
        runCatching { PrayerCalculator.calculate(date, Coordinates(35.0, 45.0, 15.0)) }
            .onSuccess { throw AssertionError("An invalid UTC offset was accepted") }
    }
    @Test
    fun qiblaDirectionConvertsMagneticHeadingToTrueNorthAndWrapsAtNorth() {
        assertEquals(
            -10.0,
            QiblaCalculator.signedDeltaFromMagneticHeading(
                trueBearingDegrees = 90.0,
                magneticAzimuthDegrees = 90.0,
                magneticDeclinationDegrees = 10.0
            ),
            0.0001
        )
        assertEquals(
            5.0,
            QiblaCalculator.signedDeltaFromMagneticHeading(
                trueBearingDegrees = 5.0,
                magneticAzimuthDegrees = 350.0,
                magneticDeclinationDegrees = 10.0
            ),
            0.0001
        )
        assertEquals(
            -5.0,
            QiblaCalculator.signedDeltaFromMagneticHeading(
                trueBearingDegrees = 355.0,
                magneticAzimuthDegrees = 0.0,
                magneticDeclinationDegrees = 0.0
            ),
            0.0001
        )
    }

    @Test
    fun audioCatalogBuildsCanonicalSurahStreamingUrls() {
        assertEquals(
            "https://cdn.islamic.network/quran/audio-surah/128/ar.alafasy/1.mp3",
            QuranAudioCatalog.surahUrl(1, "ar.alafasy")
        )
        assertEquals(
            "https://cdn.islamic.network/quran/audio-surah/128/ar.minshawi/114.mp3",
            QuranAudioCatalog.surahUrl(114, "ar.minshawi")
        )
        assertEquals(
            "https://cdn.islamic.network/quran/audio-surah/192/ar.sudais/12.mp3",
            QuranAudioCatalog.surahUrl(12, "ar.sudais")
        )
        assertEquals(
            "https://cdn.islamic.network/quran/audio-surah/128/ar.hudhaify/36.mp3",
            QuranAudioCatalog.surahUrl(36, "ar.hudhaify")
        )
    }

    @Test
    fun audioCatalogRejectsInvalidSurahAndUnknownReciter() {
        runCatching { QuranAudioCatalog.surahUrl(0, "ar.alafasy") }
            .onSuccess { throw AssertionError("Surah 0 must be rejected") }
        runCatching { QuranAudioCatalog.surahUrl(115, "ar.alafasy") }
            .onSuccess { throw AssertionError("Surah 115 must be rejected") }
        runCatching { QuranAudioCatalog.surahUrl(1, "unknown.reciter") }
            .onSuccess { throw AssertionError("Unknown audio edition must be rejected") }
    }

}
