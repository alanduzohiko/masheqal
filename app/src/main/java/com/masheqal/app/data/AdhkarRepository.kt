package com.masheqal.app.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Offline-first reader for source-attributed adhkar and dua packages.
 * The bundled texts and their licence/source records live under assets/content.
 */
data class AdhkarItem(
    val id: String,
    val category: String,
    val title: String,
    val arabic: String,
    val translation: String = "",
    val transliteration: String = "",
    val count: Int = 1,
    val countDescription: String = "",
    val benefit: String = "",
    val source: String = ""
)

class AdhkarRepository(private val context: Context) {
    suspend fun loadAll(): List<AdhkarItem> = withContext(Dispatchers.IO) {
        val arabic = readArray("content/adhkar_ar.json")
        val english = readArray("content/adhkar_en.json")
        val englishByOrder = buildMap<Int, JSONObject> {
            for (i in 0 until english.length()) {
                val item = english.getJSONObject(i)
                put(item.optInt("order", i + 1), item)
            }
        }

        val result = mutableListOf<AdhkarItem>()
        for (i in 0 until arabic.length()) {
            val item = arabic.getJSONObject(i)
            val order = item.optInt("order", i + 1)
            val translation = englishByOrder[order]
            val type = item.optInt("type", 0)
            val count = item.optInt("count", 1).coerceAtLeast(1)
            val arabicText = item.optString("content", "").trim()
            if (arabicText.isBlank()) continue

            if (type == 0 || type == 1) {
                result += AdhkarItem(
                    id = "morning:$order",
                    category = "morning",
                    title = "ذكر $order",
                    arabic = arabicText,
                    translation = translation?.optString("translation", "").orEmpty(),
                    transliteration = translation?.optString("transliteration", "").orEmpty(),
                    count = count,
                    countDescription = translation?.optString("count_description", item.optString("count_description", "")) ?: item.optString("count_description", ""),
                    benefit = translation?.optString("fadl", item.optString("fadl", "")) ?: item.optString("fadl", ""),
                    source = translation?.optString("source", item.optString("source", "")) ?: item.optString("source", "")
                )
            }
            if (type == 0 || type == 2) {
                result += AdhkarItem(
                    id = "evening:$order",
                    category = "evening",
                    title = "ذكر $order",
                    arabic = arabicText,
                    translation = translation?.optString("translation", "").orEmpty(),
                    transliteration = translation?.optString("transliteration", "").orEmpty(),
                    count = count,
                    countDescription = translation?.optString("count_description", item.optString("count_description", "")) ?: item.optString("count_description", ""),
                    benefit = translation?.optString("fadl", item.optString("fadl", "")) ?: item.optString("fadl", ""),
                    source = translation?.optString("source", item.optString("source", "")) ?: item.optString("source", "")
                )
            }
        }

        appendGenericEntries(
            result,
            readArray("content/morning_duas_en.json"),
            category = "morning",
            prefix = "morning-dua"
        )
        appendGenericEntries(
            result,
            readArray("content/evening_duas_en.json"),
            category = "evening",
            prefix = "evening-dua"
        )
        appendGenericEntries(
            result,
            readArray("content/after_salah_en.json"),
            category = "after_prayer",
            prefix = "after-salah"
        )
        appendGenericEntries(
            result,
            readArray("content/daily_duas_en.json"),
            category = "daily_dua",
            prefix = "daily-dua"
        )
        appendGenericEntries(
            result,
            readArray("content/selected_duas_en.json"),
            category = "daily_dua",
            prefix = "selected-dua"
        )

        result
    }

    private fun appendGenericEntries(
        target: MutableList<AdhkarItem>,
        array: JSONArray,
        category: String,
        prefix: String
    ) {
        for (i in 0 until array.length()) {
            val o = array.optJSONObject(i) ?: continue
            val arabic = o.optString("arabic", "").trim()
            if (arabic.isBlank()) continue
            val note = o.optString("notes", "")
            val repeat = Regex("""(?i)(?:read|recite)\s*(\d+)\s*x""")
                .find(note)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 1
            target += AdhkarItem(
                id = "$prefix:${i + 1}",
                category = category,
                title = o.optString("title", "Supplication ${i + 1}"),
                arabic = arabic,
                translation = o.optString("translation", ""),
                transliteration = o.optString("latin", ""),
                count = repeat.coerceAtLeast(1),
                countDescription = note.ifBlank { o.optString("count_description", "") },
                benefit = o.optString("benefits", o.optString("fawaid", "")),
                source = o.optString("source", "")
            )
        }
    }

    private fun readArray(path: String): JSONArray {
        val text = context.assets.open(path).bufferedReader().use { it.readText() }
        return JSONArray(text)
    }
}
