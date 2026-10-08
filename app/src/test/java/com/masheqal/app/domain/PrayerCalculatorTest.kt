package com.masheqal.app.domain

import java.time.LocalDate
import org.junit.Assert.assertTrue
import org.junit.Test

class PrayerCalculatorTest {
    private val sulaymaniyah = Coordinates(35.56, 45.43, 3.0)

    @Test
    fun prayerOrder_isChronological_forCommonMethodsAndMadhhabs() {
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
    fun asr_changes_withMadhhab_butStaysBeforeMaghrib() {
        val date = LocalDate.of(2026, 10, 8)
        val shafi = PrayerCalculator.calculate(date, sulaymaniyah, PrayerMethod.MWL, AsrMadhhab.SHAFI)
        val hanafi = PrayerCalculator.calculate(date, sulaymaniyah, PrayerMethod.MWL, AsrMadhhab.HANAFI)
        assertTrue(shafi.asr < hanafi.asr)
        assertTrue(hanafi.asr < hanafi.maghrib)
    }

    @Test
    fun methods_produceFiniteCommonDayResults() {
        val date = LocalDate.of(2026, 10, 8)
        for (method in PrayerMethod.entries) {
            val p = PrayerCalculator.calculate(date, sulaymaniyah, method, AsrMadhhab.SHAFI)
            assertTrue("$method", listOf(p.fajr,p.sunrise,p.dhuhr,p.asr,p.maghrib,p.isha).all(Double::isFinite))
        }
    }
}
