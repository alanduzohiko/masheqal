package com.masheqal.app.data

import android.content.Context
import com.caverock.androidsvg.SVG
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

data class LoadedMushafPage(
    val svg: SVG,
    val fromCache: Boolean,
    val viewBox: MushafViewBox,
    val ayahRegions: List<MushafAyahRegion>
)

/**
 * Loads the official Madinah Mushaf (Hafs/KFQC) SVG artwork and its published ayah-region JSON
 * from the pinned Quran SVG release. Each file is cached in app-private storage after successful
 * validation. An unavailable polygon sidecar does not hide the page: reading stays available,
 * while tapping ayahs becomes available after the sidecar is fetched.
 */
object MushafPageStore {
    private const val VERSION = "v1.1.1"
    private const val MAX_TRANSFER_BYTES = 2 * 1024 * 1024
    private const val MAX_SVG_BYTES = 4 * 1024 * 1024
    private const val MAX_POLYGON_TRANSFER_BYTES = 512 * 1024
    private const val MAX_POLYGON_BYTES = 1024 * 1024
    private const val CONNECT_TIMEOUT_MS = 12_000
    private const val READ_TIMEOUT_MS = 20_000

    suspend fun load(context: Context, page: Int): LoadedMushafPage {
        require(page in 1..604) { "Mushaf page must be between 1 and 604" }
        return withContext(Dispatchers.IO) {
            val svgFile = svgFile(context, page)
            var fromCache = false
            var svgText: String? = null
            var parsedSvg: SVG? = null

            if (svgFile.isFile && svgFile.length() in 1..MAX_TRANSFER_BYTES.toLong()) {
                val cached = runCatching { readGzipText(svgFile, MAX_SVG_BYTES) }.getOrNull()
                if (cached != null && cached.contains("<svg", ignoreCase = true)) {
                    val parsed = runCatching { SVG.getFromString(cached) }.getOrNull()
                    if (parsed != null) {
                        svgText = cached
                        parsedSvg = parsed
                        fromCache = true
                    }
                }
                if (parsedSvg == null) svgFile.delete()
            }

            if (parsedSvg == null || svgText == null) {
                val url = pageUrl(page, "svg")
                val fetched = downloadText(url, "image/svg+xml", MAX_TRANSFER_BYTES, MAX_SVG_BYTES)
                if (!fetched.contains("<svg", ignoreCase = true)) {
                    throw IOException("Mushaf page did not contain SVG markup")
                }
                val parsed = SVG.getFromString(fetched)
                val temp = File(svgFile.parentFile, svgFile.name + ".tmp")
                temp.parentFile?.mkdirs()
                writeGzipText(temp, fetched)
                replaceFile(temp, svgFile)
                svgText = fetched
                parsedSvg = parsed
                fromCache = false
            }

            val viewBox = MushafGeometry.parseViewBox(svgText.orEmpty())
                ?: throw IOException("Mushaf page SVG has no valid viewBox")
            val regions = loadOrFetchAyahRegions(context, page)

            LoadedMushafPage(
                svg = parsedSvg ?: throw IOException("Mushaf page could not be parsed"),
                fromCache = fromCache,
                viewBox = viewBox,
                ayahRegions = regions
            )
        }
    }

    fun cachedPageCount(context: Context): Int =
        pageDirectory(context).listFiles()?.count { it.isFile && it.name.endsWith(".svg.gz") } ?: 0

    private fun pageUrl(page: Int, extension: String): String =
        "https://cdn.quran.ws/svg/pages/$VERSION/hafs-kfqc/%03d.%s".format(Locale.US, page, extension)

    private fun svgFile(context: Context, page: Int): File =
        File(pageDirectory(context), "%03d.svg.gz".format(Locale.US, page))

    private fun polygonFile(context: Context, page: Int): File =
        File(pageDirectory(context), "%03d.json.gz".format(Locale.US, page))

    private fun pageDirectory(context: Context): File =
        File(context.filesDir, "mushaf/$VERSION/hafs-kfqc")

    private fun loadOrFetchAyahRegions(context: Context, page: Int): List<MushafAyahRegion> {
        val file = polygonFile(context, page)
        if (file.isFile && file.length() in 1..MAX_POLYGON_TRANSFER_BYTES.toLong()) {
            val cached = runCatching { readGzipText(file, MAX_POLYGON_BYTES) }.getOrNull()
            val regions = cached?.let { runCatching { parseAyahRegions(it) }.getOrNull() }
            if (!regions.isNullOrEmpty()) return regions
            file.delete()
        }

        return try {
            val json = downloadText(
                pageUrl(page, "json"),
                "application/json",
                MAX_POLYGON_TRANSFER_BYTES,
                MAX_POLYGON_BYTES
            )
            val regions = parseAyahRegions(json)
            if (regions.isNotEmpty()) {
                file.parentFile?.mkdirs()
                val temp = File(file.parentFile, file.name + ".tmp")
                writeGzipText(temp, json)
                replaceFile(temp, file)
            }
            regions
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            // The published SVG remains readable even if the sidecar cannot be obtained.
            emptyList()
        }
    }

    private fun parseAyahRegions(jsonText: String): List<MushafAyahRegion> {
        val array = JSONArray(jsonText)
        val regions = ArrayList<MushafAyahRegion>(array.length())
        for (i in 0 until array.length()) {
            val item = array.optJSONObject(i) ?: continue
            val surah = item.optInt("surahNumber", -1)
            val ayah = item.optInt("ayahNumber", -1)
            if (surah !in 1..114 || ayah < 1) continue
            val points = MushafGeometry.parsePolygon(item.optString("polygon", ""))
            if (points.size < 3) continue
            regions += MushafAyahRegion(surahNumber = surah, ayahNumber = ayah, points = points)
        }
        return regions
    }

    private fun downloadText(
        url: String,
        accept: String,
        maxTransferBytes: Int,
        maxExpandedBytes: Int
    ): String {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            instanceFollowRedirects = true
            setRequestProperty("Accept", accept)
            setRequestProperty("Accept-Encoding", "gzip")
            setRequestProperty("User-Agent", "Masheqal-Android")
        }
        try {
            val code = connection.responseCode
            if (code !in 200..299) throw IOException("Mushaf resource request failed ($code)")
            val receivedGzip = connection.getHeaderField("Content-Encoding")
                ?.equals("gzip", ignoreCase = true) == true
            val transfer = connection.inputStream.use { readBounded(it, maxTransferBytes) }
            if (transfer.isEmpty()) throw IOException("Mushaf resource response was empty")
            val expanded = if (receivedGzip) {
                GZIPInputStream(ByteArrayInputStream(transfer)).use { readBounded(it, maxExpandedBytes) }
            } else {
                if (transfer.size > maxExpandedBytes) throw IOException("Mushaf resource is too large")
                transfer
            }
            return expanded.toString(Charsets.UTF_8)
        } finally {
            connection.disconnect()
        }
    }

    private fun readGzipText(file: File, limit: Int): String =
        GZIPInputStream(file.inputStream()).use { stream ->
            readBounded(stream, limit).toString(Charsets.UTF_8)
        }

    private fun writeGzipText(file: File, text: String) {
        file.parentFile?.mkdirs()
        GZIPOutputStream(file.outputStream()).use { it.write(text.toByteArray(Charsets.UTF_8)) }
    }

    private fun replaceFile(temp: File, target: File) {
        if (!temp.renameTo(target)) {
            temp.copyTo(target, overwrite = true)
            temp.delete()
        }
    }

    private fun readBounded(input: java.io.InputStream, limit: Int): ByteArray {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(8 * 1024)
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            if (output.size() + count > limit) throw IOException("Mushaf resource exceeds size limit")
            output.write(buffer, 0, count)
        }
        return output.toByteArray()
    }
}
