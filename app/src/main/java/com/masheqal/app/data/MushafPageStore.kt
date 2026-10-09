package com.masheqal.app.data

import android.content.Context
import com.caverock.androidsvg.SVG
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
    val fromCache: Boolean
)

/**
 * Loads the official Madinah Mushaf page artwork from the pinned Quran SVG release.
 * Pages are stored as gzip files in app-private storage. A viewed page is available offline after
 * its first successful fetch. The source URL is fixed; arbitrary URLs are never accepted.
 */
object MushafPageStore {
    private const val VERSION = "v1.1.1"
    private const val MAX_TRANSFER_BYTES = 2 * 1024 * 1024
    private const val MAX_SVG_BYTES = 4 * 1024 * 1024
    private const val CONNECT_TIMEOUT_MS = 12_000
    private const val READ_TIMEOUT_MS = 20_000

    suspend fun load(context: Context, page: Int): LoadedMushafPage {
        require(page in 1..604) { "Mushaf page must be between 1 and 604" }
        return withContext(Dispatchers.IO) {
            val file = pageFile(context, page)
            if (file.isFile && file.length() in 1..MAX_TRANSFER_BYTES.toLong()) {
                val cached = runCatching { parseGzip(file) }.getOrNull()
                if (cached != null) return@withContext LoadedMushafPage(cached, true)
                file.delete()
            }

            file.parentFile?.mkdirs()
            val url = URL(
                "https://cdn.quran.ws/svg/pages/$VERSION/hafs-kfqc/%03d.svg".format(Locale.US, page)
            )
            val connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                instanceFollowRedirects = true
                setRequestProperty("Accept", "image/svg+xml")
                setRequestProperty("Accept-Encoding", "gzip")
                setRequestProperty("User-Agent", "Masheqal-Android")
            }

            try {
                val code = connection.responseCode
                if (code !in 200..299) throw IOException("Mushaf page request failed ($code)")
                val receivedGzip = connection.getHeaderField("Content-Encoding")
                    ?.equals("gzip", ignoreCase = true) == true
                val bytes = connection.inputStream.use { readBounded(it, MAX_TRANSFER_BYTES) }
                if (bytes.isEmpty()) throw IOException("Mushaf page response was empty")

                val svgBytes = if (receivedGzip) {
                    GZIPInputStream(ByteArrayInputStream(bytes)).use {
                        readBounded(it, MAX_SVG_BYTES)
                    }
                } else {
                    if (bytes.size > MAX_SVG_BYTES) throw IOException("Mushaf page is too large")
                    bytes
                }

                val svgText = svgBytes.toString(Charsets.UTF_8)
                if (!svgText.contains("<svg", ignoreCase = true)) {
                    throw IOException("Mushaf page did not contain SVG markup")
                }
                val parsed = SVG.getFromString(svgText)

                val temp = File(file.parentFile, file.name + ".tmp")
                GZIPOutputStream(temp.outputStream()).use { it.write(svgBytes) }
                if (!temp.renameTo(file)) {
                    temp.copyTo(file, overwrite = true)
                    temp.delete()
                }
                LoadedMushafPage(parsed, false)
            } finally {
                connection.disconnect()
            }
        }
    }

    fun cachedPageCount(context: Context): Int =
        pageDirectory(context).listFiles()?.count { it.isFile && it.name.endsWith(".svg.gz") } ?: 0

    private fun pageFile(context: Context, page: Int): File =
        File(pageDirectory(context), "%03d.svg.gz".format(Locale.US, page))

    private fun pageDirectory(context: Context): File =
        File(context.filesDir, "mushaf/$VERSION/hafs-kfqc")

    private fun parseGzip(file: File): SVG {
        val text = GZIPInputStream(file.inputStream()).use { stream ->
            readBounded(stream, MAX_SVG_BYTES).toString(Charsets.UTF_8)
        }
        if (!text.contains("<svg", ignoreCase = true)) throw IOException("Invalid cached SVG")
        return SVG.getFromString(text)
    }

    private fun readBounded(input: java.io.InputStream, limit: Int): ByteArray {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(8 * 1024)
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            if (output.size() + count > limit) throw IOException("Mushaf page exceeds size limit")
            output.write(buffer, 0, count)
        }
        return output.toByteArray()
    }
}
