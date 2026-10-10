package com.masheqal.app.util

import org.junit.Assert.assertEquals
import org.junit.Test

class PlaceLabelFormatterTest {
    @Test
    fun formatsCityRegionAndCountryWithoutDuplicates() {
        assertEquals("Sulaymaniyah, Kurdistan Region, Iraq",
            PlaceLabelFormatter.format("Sulaymaniyah", "Kurdistan Region", "Iraq"))
    }
    @Test
    fun removesRepeatedRegionAndCountryLabels() {
        assertEquals("Baghdad, Iraq", PlaceLabelFormatter.format("Baghdad", "Baghdad", "Iraq"))
    }
    @Test
    fun ignoresBlankComponents() {
        assertEquals("Iraq", PlaceLabelFormatter.format("", " ", "Iraq"))
    }
}
