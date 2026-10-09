package com.masheqal.app.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

enum class AdhkarPeriod(val sourceType: Int, val storageKey: String) {
    MORNING(1, "morning"),
    EVENING(2, "evening");

    fun includes(itemType: Int): Boolean = itemType == 0 || itemType == sourceType
}

data class AdhkarItem(
    val order: Int,
    val arabic: String,
    val translationEn: String,
    val transliteration: String,
    val repeatCount: Int,
    val repeatDescriptionAr: String,
    val repeatDescriptionEn: String,
    val meritAr: String,
    val meritEn: String,
    val sourceAr: String,
    val sourceEn: String,
    val type: Int,
    val audioUrl: String,
    val hadithAr: String,
    val hadithEn: String,
    val vocabularyAr: String,
    val vocabularyEn: String
) {
    fun appliesTo(period: AdhkarPeriod): Boolean = period.includes(type)
}

data class AdhkarSourceInfo(
    val source: String,
    val sourceUrl: String,
    val release: String,
    val releaseUrl: String,
    val license: String,
    val attribution: String,
    val recordCount: Int,
    val languageCoverage: List<String>,
    val soraniTranslationIncluded: Boolean
)

data class AdhkarPackage(
    val items: List<AdhkarItem>,
    val source: AdhkarSourceInfo
)

class AdhkarRepository(private val context: Context) {
    @Volatile private var cached: AdhkarPackage? = null

    suspend fun loadPackage(): AdhkarPackage = withContext(Dispatchers.IO) {
        cached ?: synchronized(this@AdhkarRepository) {
            cached ?: readPackage().also { cached = it }
        }
    }

    private fun readPackage(): AdhkarPackage {
        val rowsText = context.assets.open("content/adhkar_morning_evening.json")
            .bufferedReader().use { it.readText() }
        val manifestText = context.assets.open("content/adhkar_morning_evening_manifest.json")
            .bufferedReader().use { it.readText() }
        val rows = JSONArray(rowsText)
        val manifest = JSONObject(manifestText)
        require(rows.length() == 34) { "Adhkar data integrity check failed" }

        val items = buildList(rows.length()) {
            for (index in 0 until rows.length()) {
                val row = rows.getJSONObject(index)
                val order = row.getInt("order")
                require(order == index + 1) { "Adhkar ordering mismatch at row $index" }
                val arabic = row.getString("arabic")
                val translation = row.getString("translationEn")
                val sourceAr = row.getString("sourceAr")
                val sourceEn = row.getString("sourceEn")
                val repeats = row.getInt("repeatCount")
                val type = row.getInt("type")
                require(arabic.isNotBlank() && translation.isNotBlank()) {
                    "Missing Arabic or English text at adhkar order $order"
                }
                require(sourceAr.isNotBlank() && sourceEn.isNotBlank()) {
                    "Missing source citation at adhkar order $order"
                }
                require(repeats in 1..1000 && type in 0..2) {
                    "Invalid repetition count or period at adhkar order $order"
                }
                add(
                    AdhkarItem(
                        order = order,
                        arabic = arabic,
                        translationEn = translation,
                        transliteration = row.optString("transliteration", ""),
                        repeatCount = repeats,
                        repeatDescriptionAr = row.optString("repeatDescriptionAr", ""),
                        repeatDescriptionEn = row.optString("repeatDescriptionEn", ""),
                        meritAr = row.optString("meritAr", ""),
                        meritEn = row.optString("meritEn", ""),
                        sourceAr = sourceAr,
                        sourceEn = sourceEn,
                        type = type,
                        audioUrl = row.optString("audioUrl", ""),
                        hadithAr = row.optString("hadithAr", ""),
                        hadithEn = row.optString("hadithEn", ""),
                        vocabularyAr = row.optString("vocabularyAr", ""),
                        vocabularyEn = row.optString("vocabularyEn", "")
                    )
                )
            }
        }

        val languageArray = manifest.optJSONArray("languageCoverage") ?: JSONArray()
        val languages = buildList(languageArray.length()) {
            for (index in 0 until languageArray.length()) add(languageArray.getString(index))
        }
        val source = AdhkarSourceInfo(
            source = manifest.getString("source"),
            sourceUrl = manifest.getString("sourceUrl"),
            release = manifest.getString("sourceRef"),
            releaseUrl = manifest.getString("sourceReleaseUrl"),
            license = manifest.getString("license"),
            attribution = manifest.getString("attribution"),
            recordCount = manifest.getInt("recordCount"),
            languageCoverage = languages,
            soraniTranslationIncluded = manifest.optBoolean("soraniTranslationIncluded", false)
        )
        require(source.source == "Seen-Arabic/Morning-And-Evening-Adhkar-DB")
        require(source.release == "v1.0.2" && source.license == "MIT")
        require(source.recordCount == items.size && !source.soraniTranslationIncluded) {
            "Adhkar source manifest does not match the bundled data"
        }
        return AdhkarPackage(items, source)
    }
}

/**
 * Day- and session-specific local counters. Morning and evening sessions are tracked separately,
 * including shared adhkar that occur in both. No account, analytics or network sync is used.
 */
class AdhkarProgressStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        "masheqal_adhkar_progress",
        Context.MODE_PRIVATE
    )

    fun getCount(date: String, period: AdhkarPeriod, order: Int): Int =
        preferences.getInt(storageKey(date, period, order), 0).coerceAtLeast(0)

    fun setCount(date: String, period: AdhkarPeriod, item: AdhkarItem, value: Int): Int {
        val safeCount = value.coerceIn(0, item.repeatCount)
        preferences.edit().putInt(storageKey(date, period, item.order), safeCount).apply()
        return safeCount
    }

    fun reset(date: String, period: AdhkarPeriod, item: AdhkarItem) {
        preferences.edit().remove(storageKey(date, period, item.order)).apply()
    }

    private fun storageKey(date: String, period: AdhkarPeriod, order: Int): String =
        date + "_" + period.storageKey + "_" + order
}
