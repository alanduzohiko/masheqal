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

    fun mapDocumentPoint(x: Float, y: Float, canvasWidth: Float, canvasHeight: Float): MushafPoint? {
        if (
            !x.isFinite() || !y.isFinite() || !canvasWidth.isFinite() || !canvasHeight.isFinite() ||
            canvasWidth <= 0f || canvasHeight <= 0f || width <= 0f || height <= 0f
        ) return null
        return MushafPoint(
            (x - minX) / width * canvasWidth,
            (y - minY) / height * canvasHeight
        )
    }
}

data class MushafAyahRegion(
    val surahNumber: Int,
    val ayahNumber: Int,
    val polygons: List<List<MushafPoint>>
) {
    fun contains(x: Float, y: Float): Boolean =
        polygons.any { MushafGeometry.contains(it, x, y) }
}

/**
 * Quran SVG page coordinates and JSON ayah-region coordinates share the same page coordinate
 * system. A region can be either a point list (opening pages 1–2) or SVG path data (later pages),
 * and a later-page ayah can contain several disjoint line polygons.
 */
object MushafGeometry {
    private val numberPattern = """[-+]?(?:\d+(?:\.\d*)?|\.\d+)(?:[eE][-+]?\d+)?"""
    private val viewBoxRegex = Regex("viewBox\\s*=\\s*[\"']\\s*($numberPattern)[,\\s]+($numberPattern)[,\\s]+($numberPattern)[,\\s]+($numberPattern)\\s*[\"']")
    private val pathTokenRegex = Regex("[A-Za-z]|$numberPattern")

    fun parseViewBox(svgText: String): MushafViewBox? {
        val match = viewBoxRegex.find(svgText) ?: return null
        val values = (1..4).map { match.groupValues[it].toFloatOrNull() ?: return null }
        val box = MushafViewBox(values[0], values[1], values[2], values[3])
        return box.takeIf {
            it.minX.isFinite() && it.minY.isFinite() && it.width.isFinite() && it.height.isFinite() &&
                it.width > 0f && it.height > 0f
        }
    }

    /** Parse the point-list format used on the decorative opening pages. */
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

    /**
     * Parses both official JSON polygon formats:
     * - opening pages: "x,y x,y ..."
     * - other pages: SVG path data with M/L/Z commands and potentially multiple subpaths.
     */
    fun parsePolygons(text: String): List<List<MushafPoint>> {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return emptyList()
        if (!trimmed.startsWith("M", ignoreCase = true)) {
            return listOf(parsePolygon(trimmed)).filter { it.size >= 3 }
        }

        val tokens = pathTokenRegex.findAll(trimmed).map { it.value }.toList()
        val polygons = mutableListOf<List<MushafPoint>>()
        var ring = mutableListOf<MushafPoint>()
        var command: Char? = null
        var currentX = 0f
        var currentY = 0f
        var index = 0

        fun flushRing() {
            if (ring.size >= 3) polygons += ring.toList()
            ring = mutableListOf()
        }

        fun numberAt(i: Int): Float? = tokens.getOrNull(i)?.toFloatOrNull()

        while (index < tokens.size) {
            val token = tokens[index]
            if (token.length == 1 && token[0].isLetter()) {
                command = token[0]
                index++
                if (command == 'Z' || command == 'z') {
                    flushRing()
                    command = null
                }
                continue
            }

            when (command) {
                'M', 'm', 'L', 'l' -> {
                    val xValue = numberAt(index)
                    val yValue = numberAt(index + 1)
                    if (xValue == null || yValue == null) {
                        index++
                        command = null
                        continue
                    }
                    index += 2
                    val relative = command == 'm' || command == 'l'
                    val nextX = if (relative) currentX + xValue else xValue
                    val nextY = if (relative) currentY + yValue else yValue
                    if (command == 'M' || command == 'm') {
                        flushRing()
                        ring.add(MushafPoint(nextX, nextY))
                        command = if (command == 'm') 'l' else 'L'
                    } else {
                        ring.add(MushafPoint(nextX, nextY))
                    }
                    currentX = nextX
                    currentY = nextY
                }
                else -> {
                    // The published polygon layer for Hafs/KFQC uses only M/L/Z. If a future
                    // release adds other commands, skip its numeric payload rather than crash.
                    index++
                }
            }
        }
        flushRing()
        return polygons
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
