package com.masheqal.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdhkarPeriodTest {
    @Test
    fun sharedDhikrAppearsInBothSessions() {
        assertTrue(AdhkarPeriod.MORNING.includes(0))
        assertTrue(AdhkarPeriod.EVENING.includes(0))
    }

    @Test
    fun morningAndEveningItemsDoNotLeakIntoTheOtherSession() {
        assertTrue(AdhkarPeriod.MORNING.includes(1))
        assertFalse(AdhkarPeriod.EVENING.includes(1))
        assertTrue(AdhkarPeriod.EVENING.includes(2))
        assertFalse(AdhkarPeriod.MORNING.includes(2))
    }

    @Test
    fun categoryFilterSeparatesDailyOccasionsAndKeepsSharedDhikrInBothSessions() {
        val shared = item(order = 1, categoryId = "morning_evening", type = 0)
        val afterPrayer = item(order = 35, categoryId = "after_prayer", type = 3)
        val sleep = item(order = 45, categoryId = "sleep", type = 3)

        assertTrue(AdhkarPeriod.ALL.includes(shared))
        assertTrue(AdhkarPeriod.MORNING.includes(shared))
        assertTrue(AdhkarPeriod.EVENING.includes(shared))
        assertTrue(AdhkarPeriod.AFTER_PRAYER.includes(afterPrayer))
        assertFalse(AdhkarPeriod.MORNING.includes(afterPrayer))
        assertTrue(AdhkarPeriod.SLEEP.includes(sleep))
        assertFalse(AdhkarPeriod.AFTER_PRAYER.includes(sleep))
    }

    private fun item(order: Int, categoryId: String, type: Int) = AdhkarItem(
        order = order,
        arabic = "نص عربي",
        translationEn = "English meaning",
        transliteration = "",
        repeatCount = 1,
        repeatDescriptionAr = "",
        repeatDescriptionEn = "",
        meritAr = "",
        meritEn = "",
        sourceAr = "",
        sourceEn = "Source reference",
        type = type,
        audioUrl = "",
        hadithAr = "",
        hadithEn = "",
        vocabularyAr = "",
        vocabularyEn = "",
        categoryId = categoryId
    )

    @Test
    fun sessionStorageKeysStayDistinctAndStable() {
        assertEquals("morning", AdhkarPeriod.MORNING.storageKey)
        assertEquals("evening", AdhkarPeriod.EVENING.storageKey)
        assertEquals(1, AdhkarPeriod.MORNING.sourceType)
        assertEquals(2, AdhkarPeriod.EVENING.sourceType)
    }
}
