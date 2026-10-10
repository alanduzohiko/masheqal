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
 * Loads the published MP3Quran.net reciter catalog.
 * That catalog supplies full-surah streams; it does not promise verse-by-verse media.
 * Raw API metadata is cached so reciters remain selectable after the first successful load.
 */
data class FullSurahReciter(
    val id: String,
    val name: String,
    val moshafName: String,
    val server: String,
    val availableSurahs: Set<Int>
) {
    fun audioUrl(surah: Int): String? {
        if (surah !in 1..114 || surah !in availableSurahs) return null
        return server.trimEnd('/') + "/" + surah.toString().padStart(3, '0') + ".mp3"
    }
}

class Mp3QuranReciterRepository(private val context: Context) {
    suspend fun loadArabicReciters(): List<FullSurahReciter> = withContext(Dispatchers.IO) {
        val cache = File(context.filesDir, "mp3quran_reciters_ar.json")
        val cached = if (cache.isFile && cache.length() > 2) {
            runCatching { parse(cache.readText(Charsets.UTF_8)) }.getOrNull()
        } else null

        // Refresh once per day; fall back to the last verified response while offline.
        val shouldRefresh = !cache.isFile || System.currentTimeMillis() - cache.lastModified() > 24L * 60L * 60L * 1000L
        if (!shouldRefresh && !cached.isNullOrEmpty()) return@withContext cached

        val fresh = runCatching {
            val endpoint = URL("https:" + "/" + "/www.mp3quran.net/api/v3/reciters?language=ar")
            val connection = (endpoint.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 8000
                readTimeout = 15000
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", "Masheqal-Android")
            }
            try {
                val code = connection.responseCode
                check(code in 200..299) { "Reciter server returned HTTP $code" }
                val body = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                val parsed = parse(body)
                check(parsed.isNotEmpty()) { "Reciter catalogue was empty" }
                runCatching {
                    val temp = File(cache.parentFile, cache.name + ".tmp")
                    temp.writeText(body, Charsets.UTF_8)
                    if (!temp.renameTo(cache)) {
                        cache.writeText(body, Charsets.UTF_8)
                        temp.delete()
                    }
                }
                parsed
            } finally {
                connection.disconnect()
            }
        }.getOrNull()

        fresh ?: cached ?: emptyList()
    }

    private fun parse(json: String): List<FullSurahReciter> {
        val root = JSONObject(json)
        val list = root.optJSONArray("reciters") ?: JSONArray()
        val result = mutableListOf<FullSurahReciter>()
        for (i in 0 until list.length()) {
            val reciter = list.optJSONObject(i) ?: continue
            val reciterId = reciter.optInt("id", -1)
            val name = reciter.optString("name", "").trim()
            val moshafs = reciter.optJSONArray("moshaf") ?: continue
            if (reciterId < 0 || name.isBlank()) continue

            for (j in 0 until moshafs.length()) {
                val moshaf = moshafs.optJSONObject(j) ?: continue
                val server = moshaf.optString("server", "").trim()
                if (!server.startsWith("https://")) continue
                val moshafId = moshaf.optInt("id", j)
                val moshafName = moshaf.optString("name", "").trim().ifBlank { "Recitation ${j + 1}" }
                val surahs = moshaf.optString("surah_list", "")
                    .split(',')
                    .mapNotNull { it.trim().toIntOrNull() }
                    .filter { it in 1..114 }
                    .toSet()
                result += FullSurahReciter(
                    id = "mp3quran:${reciterId}:${moshafId}",
                    name = name,
                    moshafName = moshafName,
                    server = server,
                    availableSurahs = surahs
                )
            }
        }
        return result.distinctBy { it.id }.sortedWith(compareBy({ it.name }, { it.moshafName }))
    }
}
