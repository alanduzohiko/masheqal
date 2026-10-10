package com.masheqal.app.data

import android.content.Context
import com.masheqal.app.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

data class QuranEdition(
    val identifier: String,
    val name: String,
    val englishName: String,
    val language: String,
    val type: String,
    val format: String
)

class QuranEditionRepository(private val context: Context) {
    suspend fun loadCatalog(): List<QuranEdition> = withContext(Dispatchers.IO) {
        val cacheFile = File(context.filesDir, "quran_editions_text.json")
        val cached = if (cacheFile.isFile && cacheFile.length() > 2) {
            runCatching { parse(cacheFile.readText(Charsets.UTF_8)) }.getOrDefault(emptyList())
        } else emptyList()

        val shouldRefresh = cached.isEmpty() ||
            System.currentTimeMillis() - cacheFile.lastModified() > 24L * 60L * 60L * 1000L
        if (!shouldRefresh) return@withContext cached

        val fresh = runCatching {
            val url = URL("https:" + "/" + "/api.alquran.cloud/v1/edition?format=text")
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
                    ?: error("Edition service returned no response")
                check(status in 200..299) { "Edition service returned HTTP $status" }
                val editions = parse(body)
                check(editions.isNotEmpty()) { "Edition catalogue was empty" }
                runCatching {
                    val temporary = File(cacheFile.parentFile, cacheFile.name + ".tmp")
                    temporary.writeText(body, Charsets.UTF_8)
                    if (!temporary.renameTo(cacheFile)) {
                        cacheFile.writeText(body, Charsets.UTF_8)
                        temporary.delete()
                    }
                }
                editions
            } finally {
                connection.disconnect()
            }
        }.getOrNull()

        fresh ?: cached
    }

    suspend fun loadAudioReciters(): List<QuranEdition> = withContext(Dispatchers.IO) {
        val cacheFile = File(context.filesDir, "quran_audio_editions_ar.json")
        val cached = if (cacheFile.isFile && cacheFile.length() > 2) {
            runCatching { parseAudio(cacheFile.readText(Charsets.UTF_8)) }.getOrDefault(emptyList())
        } else emptyList()
        val freshEnough = cacheFile.isFile &&
            System.currentTimeMillis() - cacheFile.lastModified() <= 24L * 60L * 60L * 1000L
        if (freshEnough && cached.isNotEmpty()) return@withContext cached

        val fresh = runCatching {
            val url = URL("https:" + "/" + "/api.alquran.cloud/v1/edition?format=audio&language=ar&type=versebyverse")
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
                    ?: error("Audio edition service returned no response")
                check(status in 200..299) { "Audio edition service returned HTTP $status" }
                val editions = parseAudio(body)
                check(editions.isNotEmpty()) { "No verse-by-verse Arabic audio editions were returned" }
                runCatching {
                    val temp = File(cacheFile.parentFile, cacheFile.name + ".tmp")
                    temp.writeText(body, Charsets.UTF_8)
                    if (!temp.renameTo(cacheFile)) {
                        cacheFile.writeText(body, Charsets.UTF_8)
                        temp.delete()
                    }
                }
                editions
            } finally {
                connection.disconnect()
            }
        }.getOrNull()

        fresh ?: cached
    }

    private fun parseAudio(json: String): List<QuranEdition> {
        val root = JSONObject(json)
        check(root.optInt("code", 0) == 200) { "Audio edition service did not return success" }
        val list = root.optJSONArray("data") ?: JSONArray()
        return buildList {
            for (i in 0 until list.length()) {
                val item = list.optJSONObject(i) ?: continue
                val identifier = item.optString("identifier", "").trim()
                val format = item.optString("format", "").trim().lowercase()
                val type = item.optString("type", "").trim().lowercase()
                val language = item.optString("language", "").trim()
                val name = item.optString("name", "").trim()
                val englishName = item.optString("englishName", name).trim()
                if (identifier.isBlank() || name.isBlank()) continue
                if (format != "audio" || language != "ar") continue
                if (type.isNotBlank() && type != "versebyverse") continue
                add(QuranEdition(identifier, name, englishName, language, "audio", format))
            }
        }.distinctBy { it.identifier }.sortedBy { it.englishName }
    }

    fun fallbackTranslations(): List<QuranEdition> = listOf(
        fallback("en.sahih", R.string.translation_sahih, "en", "translation"),
        fallback("en.pickthall", R.string.translation_pickthall, "en", "translation"),
        fallback("en.yusufali", R.string.translation_yusufali, "en", "translation"),
        fallback("en.asad", R.string.translation_asad, "en", "translation"),
        fallback("en.hilali", R.string.translation_hilali, "en", "translation"),
        fallback("en.itani", R.string.translation_itani, "en", "translation")
    )

    fun fallbackTafsirs(): List<QuranEdition> = listOf(
        fallback("ar.muyassar", R.string.tafsir_muyassar, "ar", "tafsir"),
        fallback("ar.jalalayn", R.string.tafsir_jalalayn, "ar", "tafsir")
    )

    private fun fallback(identifier: String, label: Int, language: String, type: String) =
        QuranEdition(
            identifier = identifier,
            name = context.getString(label),
            englishName = context.getString(label),
            language = language,
            type = type,
            format = "text"
        )

    private fun parse(json: String): List<QuranEdition> {
        val root = JSONObject(json)
        check(root.optInt("code", 0) == 200) { "Edition service did not return success" }
        val list: JSONArray = root.optJSONArray("data") ?: JSONArray()
        val known = mutableListOf<QuranEdition>()
        for (i in 0 until list.length()) {
            val item = list.optJSONObject(i) ?: continue
            val identifier = item.optString("identifier", "").trim()
            val language = item.optString("language", "").trim()
            val type = item.optString("type", "").trim().lowercase()
            val format = item.optString("format", "").trim().lowercase()
            val name = item.optString("name", "").trim()
            val englishName = item.optString("englishName", name).trim()
            if (identifier.isBlank() || name.isBlank()) continue
            if (format.isNotBlank() && format != "text") continue
            if (type != "translation" && type != "tafsir") continue
            known += QuranEdition(identifier, name, englishName, language, type, "text")
        }
        return known.distinctBy { it.identifier }.sortedWith(
            compareBy<QuranEdition> { it.language }.thenBy { it.englishName }
        )
    }
}
