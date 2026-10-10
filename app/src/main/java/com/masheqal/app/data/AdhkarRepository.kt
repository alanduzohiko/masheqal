package com.masheqal.app.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

enum class AdhkarPeriod(
    val categoryId: String,
    val storageKey: String,
    val sourceType: Int = -1
) {
    ALL("all", "all"),
    MORNING("morning", "morning", 1),
    EVENING("evening", "evening", 2),
    AFTER_PRAYER("after_prayer", "after_prayer"),
    SLEEP("sleep", "sleep"),
    WAKE_UP("wake_up", "wake_up"),
    BATHROOM("bathroom", "bathroom"),
    FOOD("food", "food"),
    MOSQUE("mosque", "mosque"),
    WUDU("wudu", "wudu"),
    FASTING("fasting", "fasting"),
    HOME("home", "home"),
    TRAVEL("travel", "travel"),
    CLOTHING("clothing", "clothing"),
    WEATHER("weather", "weather"),
    PROTECTION("protection", "protection"),
    GENERAL("general", "general");

    /** Legacy selector, retained to keep the original morning/evening rules independently tested. */
    fun includes(itemType: Int): Boolean = when (this) {
        MORNING -> itemType == 0 || itemType == 1
        EVENING -> itemType == 0 || itemType == 2
        else -> false
    }

    fun includes(item: AdhkarItem): Boolean = when (this) {
        ALL -> true
        MORNING -> item.categoryId == "morning" || item.categoryId == "morning_evening"
        EVENING -> item.categoryId == "evening" || item.categoryId == "morning_evening"
        else -> item.categoryId == categoryId
    }
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
    val vocabularyEn: String,
    val categoryId: String = "morning_evening",
    val titleEn: String = ""
) {
    fun appliesTo(period: AdhkarPeriod): Boolean = period.includes(this)
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
        val rowsText = context.assets.open("content/adhkar_all.json")
            .bufferedReader().use { it.readText() }
        val manifestText = context.assets.open("content/adhkar_all_manifest.json")
            .bufferedReader().use { it.readText() }
        val rows = JSONArray(rowsText)
        val manifest = JSONObject(manifestText)
        val expectedCount = manifest.getInt("recordCount")
        require(expectedCount == 82 && rows.length() == expectedCount) {
            "Combined adhkar data integrity check failed"
        }

        val validCategories = setOf(
            "morning_evening", "morning", "evening", "after_prayer", "sleep", "wake_up",
            "bathroom", "food", "mosque", "wudu", "fasting", "home", "travel", "clothing",
            "weather", "protection", "general"
        )
        val items = buildList(rows.length()) {
            for (index in 0 until rows.length()) {
                val row = rows.getJSONObject(index)
                val order = row.getInt("order")
                require(order == index + 1) { "Adhkar ordering mismatch at row $index" }
                val category = row.getString("categoryId")
                require(category in validCategories) { "Unknown adhkar category '$category' at order $order" }

                val arabic = row.getString("arabic")
                val translation = row.getString("translationEn")
                val sourceEn = row.getString("sourceEn")
                val sourceAr = row.optString("sourceAr", "")
                val repeats = row.getInt("repeatCount")
                val type = row.getInt("type")
                require(arabic.isNotBlank() && translation.isNotBlank()) {
                    "Missing Arabic or English meaning at adhkar order $order"
                }
                require(sourceEn.isNotBlank()) {
                    "Missing source citation at adhkar order $order"
                }
                require(repeats in 1..1000 && type in 0..3) {
                    "Invalid repetition count or type at adhkar order $order"
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
                        vocabularyEn = row.optString("vocabularyEn", ""),
                        categoryId = category,
                        titleEn = row.optString("titleEn", "")
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
        val sources = manifest.optJSONArray("sources") ?: JSONArray()
        require(source.source == "Combined licensed adhkar datasets")
        require(source.license == "MIT (both source datasets)")
        require(source.recordCount == items.size && !source.soraniTranslationIncluded) {
            "Combined adhkar manifest does not match bundled data"
        }
        require(sources.length() == 2) { "Both licensed adhkar source records must be present" }
        require(sources.getJSONObject(0).getString("source") == "Seen-Arabic/Morning-And-Evening-Adhkar-DB")
        require(sources.getJSONObject(1).getString("source") == "fitrahive/dua-dhikr")
        require(sources.getJSONObject(1).getString("sourceRef") == "f42f895f914319a844c3e3c2279483cae060ea19")
        require(manifest.optString("scholarReviewStatus") == "pending") {
            "The scholarly review status must not be hidden"
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
