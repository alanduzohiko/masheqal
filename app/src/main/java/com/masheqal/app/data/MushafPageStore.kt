package com.masheqal.app.data

import android.content.Context
import com.caverock.androidsvg.SVG
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
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
    private const val EDITION = "hafs-kfqc"
    private const val MANIFEST_URL = "https://cdn.quran.ws/svg/pages/$VERSION/manifest.json"
    private const val MAX_MANIFEST_TRANSFER_BYTES = 2 * 1024 * 1024
    private const val MAX_MANIFEST_BYTES = 4 * 1024 * 1024
    private const val MAX_TRANSFER_BYTES = 2 * 1024 * 1024
    private const val MAX_SVG_BYTES = 4 * 1024 * 1024
    private const val MAX_POLYGON_TRANSFER_BYTES = 512 * 1024
    private const val MAX_POLYGON_BYTES = 1024 * 1024
    private const val CONNECT_TIMEOUT_MS = 12_000
    private const val READ_TIMEOUT_MS = 20_000

    private data class ExpectedResource(val path: String, val size: Long, val sha256: String)

    @Volatile
    private var manifestInMemory: Map<String, ExpectedResource>? = null

    suspend fun load(context: Context, page: Int): LoadedMushafPage {
        require(page in 1..604) { "Mushaf page must be between 1 and 604" }
        return withContext(Dispatchers.IO) {
            val svgFile = svgFile(context, page)
            var fromCache = false
            var svgText: String? = null
            var parsedSvg: SVG? = null
            // Cached pages can remain readable without a network round-trip. If the pinned
            // manifest is available locally, their integrity is checked before reuse.
            val localManifest = readCachedManifest(context)

            if (svgFile.isFile && svgFile.length() in 1..MAX_TRANSFER_BYTES.toLong()) {
                val cached = runCatching { readGzipText(svgFile, MAX_SVG_BYTES) }.getOrNull()
                val expected = localManifest?.get(manifestPath(page, "svg"))
                val integrityOk = cached != null &&
                    (expected == null || matchesExpectedResource(cached.toByteArray(Charsets.UTF_8), expected))
                if (integrityOk && cached!!.contains("<svg", ignoreCase = true)) {
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
                val expected = manifestEntries(context)[manifestPath(page, "svg")]
                    ?: throw IOException("Pinned Mushaf manifest lacks page $page SVG metadata")
                val fetched = downloadText(url, "image/svg+xml", MAX_TRANSFER_BYTES, MAX_SVG_BYTES)
                verifyExpectedResource(fetched.toByteArray(Charsets.UTF_8), expected, url)
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

    /** Number of pages with both validly-written SVG and ayah-region cache files present. */
    fun cachedOfflinePageCount(context: Context): Int = (1..604).count { page ->
        val svg = svgFile(context, page)
        val polygons = polygonFile(context, page)
        svg.isFile && svg.length() in 1..MAX_TRANSFER_BYTES.toLong() &&
            polygons.isFile && polygons.length() in 1..MAX_POLYGON_TRANSFER_BYTES.toLong()
    }

    /**
     * Downloads and validates the complete Hafs/KFQC Mushaf plus the ayah tap maps.
     * Pages are processed sequentially to avoid loading many large SVGs into memory at once.
     * Cancellation is checked between page requests; completed pages remain cached if cancelled.
     */
    suspend fun downloadAll(
        context: Context,
        onProgress: (completedPages: Int, totalPages: Int) -> Unit
    ): Int {
        // The offline downloader must validate the inventory of all 604 pages and their sidecars.
        manifestEntries(context)
        var completed = 0
        for (page in 1..604) {
            currentCoroutineContext().ensureActive()
            val loaded = load(context, page)
            if (loaded.ayahRegions.isEmpty()) {
                throw IOException("Ayah-region metadata is unavailable for Mushaf page $page")
            }
            currentCoroutineContext().ensureActive()
            completed = page
            onProgress(completed, 604)
        }
        return completed
    }

    /**
     * Loads the checksum inventory from the immutable release. Downloaded resources are checked
     * against the published byte length and SHA-256 before the app writes them to its cache.
     */
    @Synchronized
    private fun manifestEntries(context: Context): Map<String, ExpectedResource> {
        manifestInMemory?.let { return it }
        readCachedManifest(context)?.let { manifest ->
            manifestInMemory = manifest
            return manifest
        }
        val text = downloadText(
            MANIFEST_URL,
            "application/json",
            MAX_MANIFEST_TRANSFER_BYTES,
            MAX_MANIFEST_BYTES
        )
        val entries = parseManifestEntries(text)
        validateCompleteManifest(entries)
        val file = manifestFile(context)
        val temp = File(file.parentFile, file.name + ".tmp")
        writeGzipText(temp, text)
        replaceFile(temp, file)
        manifestInMemory = entries
        return entries
    }

    private fun readCachedManifest(context: Context): Map<String, ExpectedResource>? {
        val file = manifestFile(context)
        if (!file.isFile || file.length() !in 1..MAX_MANIFEST_TRANSFER_BYTES.toLong()) return null
        val parsed = runCatching { parseManifestEntries(readGzipText(file, MAX_MANIFEST_BYTES)) }.getOrNull()
        val valid = parsed?.let { runCatching { validateCompleteManifest(it) }.isSuccess } == true
        if (!valid) {
            file.delete()
            return null
        }
        return parsed
    }

    private fun parseManifestEntries(jsonText: String): Map<String, ExpectedResource> {
        val root = JSONObject(jsonText)
        val result = linkedMapOf<String, ExpectedResource>()

        fun joinPath(parent: String, child: String): String {
            val cleanParent = parent.replace('\\\\', '/').trim('/')
            val cleanChild = child.replace('\\\\', '/').trim('/')
            return when {
                cleanParent.isBlank() -> cleanChild
                cleanChild.isBlank() -> cleanParent
                cleanChild.startsWith("$cleanParent/") -> cleanChild
                else -> "$cleanParent/$cleanChild"
            }
        }

        fun visit(value: Any?, parentPath: String) {
            when (value) {
                is JSONArray -> {
                    for (index in 0 until value.length()) visit(value.opt(index), parentPath)
                }
                is JSONObject -> {
                    val explicitPath = sequenceOf("path", "file", "relativePath")
                        .map { key -> value.optString(key, "").trim() }
                        .firstOrNull { it.isNotEmpty() }
                    val name = value.optString("name", "").trim()
                    val currentPath = when {
                        !explicitPath.isNullOrBlank() -> explicitPath.replace('\\\\', '/').trim('/')
                        name.isNotBlank() -> joinPath(parentPath, name)
                        else -> parentPath
                    }
                    val sha = sequenceOf("sha256", "sha256sum", "sha-256")
                        .map { key -> value.optString(key, "").trim().lowercase(Locale.ROOT) }
                        .firstOrNull { it.matches(Regex("[0-9a-f]{64}")) }
                    val size = sequenceOf("size", "bytes", "byteSize", "fileSize")
                        .map { key -> value.optLong(key, -1L) }
                        .firstOrNull { it > 0L } ?: -1L

                    if (sha != null && size > 0L) {
                        val canonical = canonicalManifestPath(currentPath) ?: canonicalManifestPath(parentPath)
                        if (canonical != null) result[canonical] = ExpectedResource(canonical, size, sha)
                    }

                    val iterator = value.keys()
                    while (iterator.hasNext()) {
                        val key = iterator.next()
                        if (key in setOf("path", "file", "relativePath", "name", "size", "bytes", "byteSize", "fileSize", "sha256", "sha256sum", "sha-256")) continue
                        visit(value.opt(key), joinPath(currentPath, key))
                    }
                }
            }
        }

        visit(root, "")
        if (result.isEmpty()) throw IOException("Pinned Mushaf manifest contained no SHA-256 file entries")
        return result
    }

    private fun canonicalManifestPath(path: String): String? {
        val normalized = path.replace('\\\\', '/').trim('/')
        val marker = "$EDITION/"
        val markerIndex = normalized.lastIndexOf(marker)
        val suffix = if (markerIndex >= 0) normalized.substring(markerIndex + marker.length) else normalized
        if (!Regex("\\\\d{3}\\\\.(svg|json)").matches(suffix)) return null
        return "$EDITION/$suffix"
    }

    private fun validateCompleteManifest(entries: Map<String, ExpectedResource>) {
        for (page in 1..604) {
            for (extension in listOf("svg", "json")) {
                val key = manifestPath(page, extension)
                val expected = entries[key] ?: throw IOException("Pinned manifest is missing $key")
                if (expected.size <= 0L || !expected.sha256.matches(Regex("[0-9a-f]{64}"))) {
                    throw IOException("Pinned manifest has invalid integrity metadata for ${expected.path}")
                }
            }
        }
    }

    private fun manifestPath(page: Int, extension: String): String =
        "$EDITION/${String.format(Locale.US, "%03d", page)}.$extension"

    private fun manifestFile(context: Context): File =
        File(pageDirectory(context), "manifest-$VERSION.json.gz")

    private fun matchesExpectedResource(bytes: ByteArray, expected: ExpectedResource): Boolean =
        bytes.size.toLong() == expected.size && sha256Hex(bytes).equals(expected.sha256, ignoreCase = true)

    private fun verifyExpectedResource(bytes: ByteArray, expected: ExpectedResource, url: String) {
        if (!matchesExpectedResource(bytes, expected)) {
            throw IOException("Mushaf integrity check failed for $url")
        }
    }

    internal fun sha256Hex(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
        return digest.joinToString("") { byte -> "%02x".format(Locale.ROOT, byte.toInt() and 0xff) }
    }

    internal fun matchesSha256(bytes: ByteArray, expectedSha256: String): Boolean =
        expectedSha256.matches(Regex("[0-9a-fA-F]{64}")) &&
            sha256Hex(bytes).equals(expectedSha256, ignoreCase = true)

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
        val localManifest = readCachedManifest(context)
        if (file.isFile && file.length() in 1..MAX_POLYGON_TRANSFER_BYTES.toLong()) {
            val cached = runCatching { readGzipText(file, MAX_POLYGON_BYTES) }.getOrNull()
            val expected = localManifest?.get(manifestPath(page, "json"))
            val integrityOk = cached != null &&
                (expected == null || matchesExpectedResource(cached.toByteArray(Charsets.UTF_8), expected))
            val regions = if (integrityOk) cached?.let { runCatching { parseAyahRegions(it) }.getOrNull() } else null
            if (!regions.isNullOrEmpty()) return regions
            file.delete()
        }

        return try {
            val url = pageUrl(page, "json")
            val expected = manifestEntries(context)[manifestPath(page, "json")]
                ?: throw IOException("Pinned Mushaf manifest lacks page $page ayah-region metadata")
            val json = downloadText(
                url,
                "application/json",
                MAX_POLYGON_TRANSFER_BYTES,
                MAX_POLYGON_BYTES
            )
            verifyExpectedResource(json.toByteArray(Charsets.UTF_8), expected, url)
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
            val polygons = MushafGeometry.parsePolygons(item.optString("polygon", ""))
            if (polygons.isEmpty()) continue
            regions += MushafAyahRegion(surahNumber = surah, ayahNumber = ayah, polygons = polygons)
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
