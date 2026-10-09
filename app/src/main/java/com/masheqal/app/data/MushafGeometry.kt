package com.masheqal.app.data

data class MushafPoint(val x: Float, val y: Float)

data class MushafViewBox(
    val minX: Float,
    val minY: Float,
    val width: Float,
    val height: Float
) {
    fun mapCanvasPoint(x: Float, y: Float, canvasWidth: Float, canvasHeight: Float): MushafPoint? {
        if (
            !x.isFinite() || !y.isFinite() || !canvasWidth.isFinite() || !canvasHeight.isFinite() ||
            canvasWidth <= 0f || canvasHeight <= 0f || width <= 0f || height <= 0f
        ) return null
        return MushafPoint(
            minX + x / canvasWidth * width,
            minY + y / canvasHeight * height
        )
    }
}

data class MushafAyahRegion(
    val surahNumber: Int,
    val ayahNumber: Int,
    val points: List<MushafPoint>
) {
    fun contains(x: Float, y: Float): Boolean = MushafGeometry.contains(points, x, y)
}

/** Small geometry helpers used to map taps on the rendered SVG to the pinned page metadata. */
object MushafGeometry {
    private val numberPattern = """[-+]?(?:\d+(?:\.\d*)?|\.\d+)(?:[eE][-+]?\d+)?"""
    private val viewBoxRegex = Regex("""viewBox\s*=\s*["']\s*($numberPattern)[,\s]+($numberPattern)[,\s]+($numberPattern)[,\s]+($numberPattern)\s*["']""")

    fun parseViewBox(svgText: String): MushafViewBox? {
        val match = viewBoxRegex.find(svgText) ?: return null
        val values = (1..4).map { match.groupValues[it].toFloatOrNull() ?: return null }
        val box = MushafViewBox(values[0], values[1], values[2], values[3])
        return box.takeIf {
            it.minX.isFinite() && it.minY.isFinite() && it.width.isFinite() && it.height.isFinite() &&
                it.width > 0f && it.height > 0f
        }
    }

    fun parsePolygon(text: String): List<MushafPoint> {
        if (text.isBlank()) return emptyList()
        return text.trim().split(Regex("\\s+")).mapNotNull { pair ->
            val parts = pair.split(',')
            if (parts.size != 2) return@mapNotNull null
            val x = parts[0].toFloatOrNull() ?: return@mapNotNull null
            val y = parts[1].toFloatOrNull() ?: return@mapNotNull null
            if (!x.isFinite() || !y.isFinite()) null else MushafPoint(x, y)
        }
    }

    fun contains(points: List<MushafPoint>, x: Float, y: Float): Boolean {
        if (points.size < 3 || !x.isFinite() || !y.isFinite()) return false
        var inside = false
        var j = points.lastIndex
        for (i in points.indices) {
            val a = points[i]
            val b = points[j]
            val crosses = (a.y > y) != (b.y > y)
            if (crosses) {
                val intersectionX = (b.x - a.x) * (y - a.y) / (b.y - a.y) + a.x
                if (x < intersectionX) inside = !inside
            }
            j = i
        }
        return inside
    }
}
