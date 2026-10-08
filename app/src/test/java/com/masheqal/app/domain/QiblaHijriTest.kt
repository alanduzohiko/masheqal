package com.masheqal.app.domain

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QiblaHijriTest {
    @Test
    fun qiblaBearing_isNormalized() {
        val bearing = QiblaCalculator.bearingFrom(35.56, 45.43)
        assertTrue(bearing >= 0.0 && bearing < 360.0)
        assertTrue("Sulaymaniyah should point generally south-west", bearing in 190.0..210.0)
    }

    @Test
    fun hijriDate_isStructurallyValid() {
        val h = HijriCalculator.fromGregorian(LocalDate.of(2026, 10, 8))
        assertTrue(h.year >= 1300)
        assertTrue(h.month in 1..12)
        assertTrue(h.day in 1..30)
        assertEquals(24, h.day)
        assertEquals(4, h.month)
        assertEquals(1448, h.year)
    }
}
