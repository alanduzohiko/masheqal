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
    fun sessionStorageKeysStayDistinctAndStable() {
        assertEquals("morning", AdhkarPeriod.MORNING.storageKey)
        assertEquals("evening", AdhkarPeriod.EVENING.storageKey)
        assertEquals(1, AdhkarPeriod.MORNING.sourceType)
        assertEquals(2, AdhkarPeriod.EVENING.sourceType)
    }
}
