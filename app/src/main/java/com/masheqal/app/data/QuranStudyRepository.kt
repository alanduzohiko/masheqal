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
 * Downloads optional Quran translation/tafsir editions from the Al Quran Cloud API.
 * Each requested surah is cached locally so a previously opened edition works offline.
 * Edition IDs are restricted to safe API identifiers and should be chosen from the UI catalog.
 */
class QuranStudyRepository(private val context: Context) {
    suspend fun loadSurah(surah: Int, edition: String): List<String> = withContext(Dispatchers.IO) {
        require(surah in 1..114) { "Surah must be between 1 and 114" }
        require(edition.matches(Regex("[a-z0-9.-]{2,80}"))) { "Invalid edition identifier" }

        val cacheFolder = File(context.filesDir, "quran_study_cache").apply { mkdirs() }
        val cacheFile = File(cacheFolder, "${edition.replace('.', '_')}_surah_${surah}.json")
        if (cacheFile.isFile && cacheFile.length() > 2) {
            runCatching { decodeVerses(cacheFile.readText(Charsets.UTF_8)) }
                .getOrNull()
                ?.takeIf { it.isNotEmpty() }
                ?.let { return@withContext it }
        }

        val url = URL("https:" + "/" + "/api.alquran.cloud/v1/surah/$surah/$edition")
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
            val response = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
                ?: error("Quran content server returned no response")
            check(status in 200..299) { "Quran content server returned HTTP $status" }

            val values = decodeVerses(response)
            check(values.isNotEmpty()) { "This content edition did not contain ayahs" }
            val encoded = JSONArray().apply { values.forEach { put(it) } }.toString()
            runCatching {
                val temp = File(cacheFolder, cacheFile.name + ".tmp")
                temp.writeText(encoded, Charsets.UTF_8)
                if (!temp.renameTo(cacheFile)) {
                    cacheFile.writeText(encoded, Charsets.UTF_8)
                    temp.delete()
                }
            }
            values
        } finally {
            connection.disconnect()
        }
    }

    private fun decodeVerses(json: String): List<String> {
        // Cache files hold a compact JSON array; API responses wrap the same texts in data.ayahs.
        val root = json.trim()
        if (root.startsWith("[")) {
            val array = JSONArray(root)
            return buildList(array.length()) {
                for (i in 0 until array.length()) add(array.getString(i))
            }
        }

        val response = JSONObject(root)
        check(response.optInt("code", 0) == 200) {
            response.optString("status", "Quran content was not available")
        }
        val data = response.getJSONObject("data")
        val ayahs = data.getJSONArray("ayahs")
        return buildList(ayahs.length()) {
            for (i in 0 until ayahs.length()) {
                add(ayahs.getJSONObject(i).optString("text", ""))
            }
        }.filter { it.isNotBlank() }
    }
}
