package com.masheqal.app.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * Downloads source-tagged Uthmani Tajweed text from Quran.com's public API.
 * The app caches each complete surah after validating its ayah count; the original
 * bundled Quran text remains the offline fallback if this content has never been cached.
 */
class QuranTajweedRepository(private val context: Context) {
    suspend fun loadSurah(surah: Int, expectedAyahs: Int): List<String> = withContext(Dispatchers.IO) {
        require(surah in 1..114) { "Surah must be between 1 and 114" }
        require(expectedAyahs in 1..286) { "Invalid ayah count" }
        val folder = File(context.filesDir, "quran_tajweed_cache").apply { mkdirs() }
        val cache = File(folder, "surah_${surah}.json")
        if (cache.isFile && cache.length() > 2) {
            runCatching { parseCache(cache.readText(Charsets.UTF_8), expectedAyahs) }
                .getOrNull()
                ?.let { return@withContext it }
        }

        val taggedVerses = mutableListOf<String>()
        var page = 1
        val perPage = 50
        while (taggedVerses.size < expectedAyahs && page <= 8) {
            val url = URL(
                "https:" + "/" + "/api.quran.com/api/v4/verses/by_chapter/" +
                    surah.toString() +
                    "?fields=text_uthmani_tajweed&per_page=" + perPage + "&page=" + page
            )
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 8000
                readTimeout = 15000
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", "Masheqal-Android")
            }
            try {
                val status = connection.responseCode
                val stream = if (status in 200..299) connection.inputStream else connection.errorStream
                val body = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
                    ?: error("Tajweed service returned no response")
                check(status in 200..299) { "Tajweed service returned HTTP $status" }
                val root = JSONObject(body)
                val verses = root.optJSONArray("verses") ?: JSONArray()
                if (verses.length() == 0) break
                for (i in 0 until verses.length()) {
                    val item = verses.optJSONObject(i) ?: continue
                    val tagged = item.optString("text_uthmani_tajweed", "").trim()
                    if (tagged.isNotBlank()) taggedVerses += tagged
                }
            } finally {
                connection.disconnect()
            }
            page++
        }
        check(taggedVerses.size == expectedAyahs) {
            "Tajweed text count mismatch for surah $surah: got ${taggedVerses.size}, expected $expectedAyahs"
        }
        val encoded = JSONArray().apply { taggedVerses.forEach { put(it) } }.toString()
        runCatching {
            val temp = File(folder, cache.name + ".tmp")
            temp.writeText(encoded, Charsets.UTF_8)
            if (!temp.renameTo(cache)) {
                cache.writeText(encoded, Charsets.UTF_8)
                temp.delete()
            }
        }
        taggedVerses
    }

    private fun parseCache(json: String, expectedAyahs: Int): List<String> {
        val array = JSONArray(json)
        require(array.length() == expectedAyahs) { "Cached Tajweed count mismatch" }
        return buildList(array.length()) {
            for (i in 0 until array.length()) {
                val text = array.getString(i).trim()
                require(text.isNotBlank()) { "Empty Tajweed verse in cache" }
                add(text)
            }
        }
    }
}
