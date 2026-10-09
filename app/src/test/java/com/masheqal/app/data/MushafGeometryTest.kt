package com.masheqal.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MushafGeometryTest {
    @Test
    fun readsNormalAndNegativeOriginViewBoxes() {
        assertEquals(MushafViewBox(0f, 0f, 345f, 550f),
            MushafGeometry.parseViewBox("<svg viewBox=\"0 0 345 550\">"))
        assertEquals(MushafViewBox(-53.3109f, -198.4777f, 345f, 550f),
            MushafGeometry.parseViewBox("<svg viewBox=\"-53.3109 -198.4777 345 550\">"))
        assertNull(MushafGeometry.parseViewBox("<svg viewBox=\"invalid\">"))
    }

    @Test
    fun parsesPolygonCoordinatePairsAndIgnoresMalformedPairs() {
        assertEquals(
            listOf(MushafPoint(0f, 0f), MushafPoint(10f, 0f), MushafPoint(10f, 8f)),
            MushafGeometry.parsePolygon("0,0 10,0 broken 10,8")
        )
    }

    @Test
    fun pointInPolygonDistinguishesInsideAndOutsidePoints() {
        val rectangle = MushafGeometry.parsePolygon("0,0 10,0 10,10 0,10")
        assertTrue(MushafGeometry.contains(rectangle, 5f, 5f))
        assertFalse(MushafGeometry.contains(rectangle, 15f, 5f))
        assertFalse(MushafGeometry.contains(emptyList(), 1f, 1f))
    }

    @Test
    fun mapsCanvasTapThroughNegativeOriginViewBox() {
        val box = MushafViewBox(-50f, -100f, 300f, 500f)
        assertEquals(MushafPoint(100f, 150f), box.mapCanvasPoint(150f, 250f, 300f, 500f))
    }
}
