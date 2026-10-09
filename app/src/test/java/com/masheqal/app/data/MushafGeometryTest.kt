package com.masheqal.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MushafGeometryTest {
    @Test
    fun readsNormalAndNegativeOriginViewBoxes() {
        assertEquals(
            MushafViewBox(0f, 0f, 345f, 550f),
            MushafGeometry.parseViewBox("<svg viewBox=\"0 0 345 550\">")
        )
        assertEquals(
            MushafViewBox(-53.3109f, -198.4777f, 345f, 550f),
            MushafGeometry.parseViewBox("<svg viewBox=\"-53.3109 -198.4777 345 550\">")
        )
        assertNull(MushafGeometry.parseViewBox("<svg viewBox=\"invalid\">"))
    }

    @Test
    fun parsesOpeningPagePointListFormat() {
        assertEquals(
            listOf(
                listOf(MushafPoint(0f, 0f), MushafPoint(10f, 0f), MushafPoint(10f, 8f))
            ),
            MushafGeometry.parsePolygons("0,0 10,0 broken 10,8")
        )
    }

    @Test
    fun parsesLaterPageSvgPathData() {
        val parsed = MushafGeometry.parsePolygons("M 0.0 0.75 L 10.0 0.75 L 10.0 10.0 L 0.0 10.0 Z")
        assertEquals(1, parsed.size)
        assertEquals(
            listOf(MushafPoint(0f, 0.75f), MushafPoint(10f, 0.75f), MushafPoint(10f, 10f), MushafPoint(0f, 10f)),
            parsed.single()
        )
    }

    @Test
    fun handlesMultipleSubpathsForAyahSpanningSeveralLines() {
        val region = MushafAyahRegion(
            surahNumber = 2,
            ayahNumber = 7,
            polygons = MushafGeometry.parsePolygons(
                "M 0 0 L 10 0 L 10 10 L 0 10 Z M 20 20 L 30 20 L 30 30 L 20 30 Z"
            )
        )
        assertEquals(2, region.polygons.size)
        assertTrue(region.contains(5f, 5f))
        assertTrue(region.contains(25f, 25f))
        assertFalse(region.contains(15f, 15f))
    }

    @Test
    fun mapsCanvasTapThroughNegativeOriginViewBox() {
        val box = MushafViewBox(-50f, -100f, 300f, 500f)
        assertEquals(MushafPoint(100f, 150f), box.mapCanvasPoint(150f, 250f, 300f, 500f))
    }

    @Test
    fun mapsAyahDocumentPointBackToCanvasWithNegativeViewBoxOrigin() {
        val box = MushafViewBox(-50f, -100f, 300f, 500f)
        assertEquals(MushafPoint(150f, 250f), box.mapDocumentPoint(100f, 150f, 300f, 500f))
    }

    @Test
    fun viewBoxMappingRoundTripsCanvasAndDocumentCoordinates() {
        val box = MushafViewBox(-53.31f, -198.48f, 345f, 550f)
        val document = box.mapCanvasPoint(123f, 321f, 345f, 550f)!!
        val canvas = box.mapDocumentPoint(document.x, document.y, 345f, 550f)!!
        assertEquals(123f, canvas.x, 0.001f)
        assertEquals(321f, canvas.y, 0.001f)
    }
}
