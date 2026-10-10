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
 * Provides alternative, source-backed Arabic Quran text scripts (not page images).
 * The bundled Uthmani text remains the offline default. Successfully loaded surahs
 * are cached individually and validated against the bundled ayah count.
 */
class QuranScriptRepository(private val context: Context) {
    suspend fun loadCatalog(): List<QuranEdition> = withContext(Dispatchers.IO) {
        val cacheFile = File(context.filesDir, "quran_script_editions.json")
        val cached = if (cacheFile.isFile && cacheFile.length() > 2) {
            runCatching { parseCatalog(cacheFile.readText(Charsets.UTF_8)) }.getOrDefault(emptyList())
        } else emptyList()
        val freshEnough = cacheFile.isFile &&
            System.currentTimeMillis() - cacheFile.lastModified() <= CATALOG_TTL_MS
        if (freshEnough && cached.isNotEmpty()) {
            return@withContext (fallbackScripts() + cached).distinctBy { it.identifier }.sortedBy { it.englishName }
        }

        val fresh = runCatching {
            val url = URL("https:" + "/" + "/api.alquran.cloud/v1/edition?format=text&language=ar&type=quran")
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 8000
                readTimeout = 15000
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", "Masheqal-Android")
            }
            try {
                check(connection.responseCode in 200..299) { "Quran script catalogue request failed" }
                val body = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                val parsed = parseCatalog(body)
                check(parsed.isNotEmpty()) { "Quran script catalogue is empty" }
                runCatching {
                    val temp = File(cacheFile.parentFile, cacheFile.name + ".tmp")
                    temp.writeText(body, Charsets.UTF_8)
                    if (!temp.renameTo(cacheFile)) {
                        cacheFile.writeText(body, Charsets.UTF_8)
                        temp.delete()
                    }
                }
                parsed
            } finally {
                connection.disconnect()
            }
        }.getOrNull()

        (fallbackScripts() + (fresh ?: cached))
            .distinctBy { it.identifier }
            .sortedBy { it.englishName }
    }

    suspend fun loadSurah(surah: Int, edition: String, expectedAyahs: Int): List<String> =
        withContext(Dispatchers.IO) {
            require(surah in 1..114) { "Surah must be between 1 and 114" }
            require(expectedAyahs in 1..286) { "Invalid ayah count" }
            require(edition.matches(Regex("[a-z0-9.-]{2,80}"))) { "Invalid edition identifier" }
            if (edition == DEFAULT_EDITION) return@withContext emptyList()

            val folder = File(context.filesDir, "quran_script_cache").apply { mkdirs() }
            val filename = "${edition.replace('.', '_')}_surah_${surah}.json"
            val cacheFile = File(folder, filename)
            val cached = if (cacheFile.isFile && cacheFile.length() > 2) {
                runCatching { parseTextArray(cacheFile.readText(Charsets.UTF_8), expectedAyahs) }.getOrNull()
            } else null
            if (cached != null) return@withContext cached

            val fresh = runCatching {
                val url = URL("https:" + "/" + "/api.alquran.cloud/v1/surah/$surah/$edition")
                val connection = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 8000
                    readTimeout = 15000
                    setRequestProperty("Accept", "application/json")
                    setRequestProperty("User-Agent", "Masheqal-Android")
                }
                try {
                    check(connection.responseCode in 200..299) { "Quran script request failed" }
                    val body = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                    val root = JSONObject(body)
                    check(root.optInt("code", 0) == 200) { "Quran script service did not return success" }
                    val data = root.optJSONObject("data") ?: error("Missing surah data")
                    val verses = data.optJSONArray("ayahs") ?: error("Missing ayahs")
                    val values = buildList {
                        for (i in 0 until verses.length()) {
                            val text = verses.optJSONObject(i)?.optString("text", "").orEmpty().trim()
                            if (text.isBlank()) error("Blank ayah in Quran script response")
                            add(text)
                        }
                    }
                    check(values.size == expectedAyahs) { "Quran script ayah count did not match the bundled Quran" }
                    val encoded = JSONArray().apply { values.forEach { put(it) } }.toString()
                    runCatching {
                        val temp = File(folder, cacheFile.name + ".tmp")
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
            }.getOrNull()

            fresh ?: cached ?: error("This Quran script has not been downloaded on this device yet")
        }

    private fun parseCatalog(json: String): List<QuranEdition> {
        val root = JSONObject(json)
        check(root.optInt("code", 0) == 200) { "Quran script catalogue returned an error" }
        val data = root.optJSONArray("data") ?: JSONArray()
        return buildList {
            for (i in 0 until data.length()) {
                val item = data.optJSONObject(i) ?: continue
                val id = item.optString("identifier", "").trim()
                val name = item.optString("name", "").trim()
                val englishName = item.optString("englishName", name).trim()
                val language = item.optString("language", "").trim()
                val format = item.optString("format", "").trim().lowercase()
                val type = item.optString("type", "").trim().lowercase()
                if (id.isBlank() || name.isBlank() || language != "ar" || format != "text" || type != "quran") continue
                if (!id.matches(Regex("[a-z0-9.-]{2,80}"))) continue
                add(QuranEdition(id, name, englishName, language, type, format))
            }
        }.distinctBy { it.identifier }
    }

    private fun parseTextArray(json: String, expectedAyahs: Int): List<String> {
        val array = JSONArray(json)
        if (array.length() != expectedAyahs) error("Cached Quran script had an unexpected ayah count")
        return buildList {
            for (i in 0 until array.length()) {
                val text = array.optString(i, "").trim()
                if (text.isBlank()) error("Cached Quran script has a blank ayah")
                add(text)
            }
        }
    }

    private fun fallbackScripts(): List<QuranEdition> = listOf(
        option(DEFAULT_EDITION, "الرسم العثماني", "Uthmani script"),
        option("quran-simple", "الرسم العربي المبسط", "Simple Arabic script"),
        option("quran-uthmani-quran-academy", "الرسم العثماني — أكاديمية القرآن", "Uthmani script — Quran Academy")
    )

    private fun option(id: String, name: String, englishName: String) =
        QuranEdition(id, name, englishName, "ar", "quran", "text")

    companion object {
        const val DEFAULT_EDITION = "quran-uthmani"
        private const val CATALOG_TTL_MS = 24L * 60L * 60L * 1000L
    }
}
