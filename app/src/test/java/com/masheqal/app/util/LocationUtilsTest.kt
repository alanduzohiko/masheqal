package com.masheqal.app.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LocationUtilsTest {
    private val now = 1_800_000_000_000L

    private fun fix(
        latitude: Double = 35.56,
        longitude: Double = 45.43,
        accuracy: Float = 25f,
        ageMillis: Long = 10_000L
    ) = LocationCandidate(
        latitude = latitude,
        longitude = longitude,
        accuracyMeters = accuracy,
        timestampMillis = now - ageMillis,
        provider = "test"
    )

    @Test
    fun rejectsStaleLastKnownFixRatherThanUsingYesterdayLocation() {
        val stale = fix(ageMillis = LocationFixPolicy.MAX_LAST_KNOWN_AGE_MILLIS + 1L)
        assertFalse(LocationFixPolicy.isUsable(stale, now, LocationFixPolicy.MAX_LAST_KNOWN_AGE_MILLIS))
        assertNull(LocationFixPolicy.selectBest(listOf(stale), now))
    }

    @Test
    fun rejectsInvalidCoordinatesAndUnboundedAccuracy() {
        assertFalse(LocationFixPolicy.isUsable(fix(latitude = 91.0), now, 60_000L))
        assertFalse(LocationFixPolicy.isUsable(fix(longitude = 181.0), now, 60_000L))
        assertFalse(LocationFixPolicy.isUsable(fix(accuracy = Float.POSITIVE_INFINITY), now, 60_000L))
        assertFalse(LocationFixPolicy.isUsable(fix(accuracy = 25_000f), now, 60_000L))
    }

    @Test
    fun prefersMoreAccurateFixesWhenBothAreRecent() {
        val coarse = fix(accuracy = 450f, ageMillis = 5_000L)
        val precise = fix(latitude = 35.561, longitude = 45.431, accuracy = 18f, ageMillis = 12_000L)

        val best = LocationFixPolicy.selectBest(listOf(coarse, precise), now)

        assertEquals(18f, best?.accuracyMeters)
        assertEquals(35.561, best?.latitude ?: Double.NaN, 0.0)
    }

    @Test
    fun flagsApproximateLocationWithoutDiscardingIt() {
        val approximate = CurrentLocation(
            latitude = 35.56,
            longitude = 45.43,
            accuracyMeters = 1_200f,
            timestampMillis = now,
            provider = "network"
        )
        val precise = approximate.copy(accuracyMeters = 30f)

        assertFalse(approximate.isPrecise)
        assertTrue(precise.isPrecise)
    }
}
