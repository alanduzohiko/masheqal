package com.masheqal.app.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

data class AllahName(
    val number: Int,
    val arabic: String,
    val transliteration: String,
    val meaning: String,
    val description: String,
    val references: List<String>
)

data class NamesOfAllahSourceInfo(
    val source: String,
    val sourceUrl: String,
    val sourceCommit: String,
    val license: String,
    val attribution: String,
    val count: Int,
    val soraniMeaningIncluded: Boolean,
    val englishMeaningScholarReviewStatus: String,
    val recordsWithReferences: Int,
    val recordsWithoutReferences: Int
)

data class NamesOfAllahPackage(
    val names: List<AllahName>,
    val source: NamesOfAllahSourceInfo
)

class NamesOfAllahRepository(private val context: Context) {
    @Volatile private var cached: NamesOfAllahPackage? = null

    suspend fun load(): NamesOfAllahPackage = withContext(Dispatchers.IO) {
        cached ?: synchronized(this@NamesOfAllahRepository) {
            cached ?: readPackage().also { cached = it }
        }
    }

    private fun readPackage(): NamesOfAllahPackage {
        val dataText = context.assets.open("content/names_of_allah.json")
            .bufferedReader().use { it.readText() }
        val manifestText = context.assets.open("content/names_of_allah_manifest.json")
            .bufferedReader().use { it.readText() }
        val root = JSONObject(dataText)
        val manifest = JSONObject(manifestText)
        val rows = root.optJSONArray("names") ?: JSONArray()
        require(rows.length() == 99 && manifest.getInt("recordCount") == 99) {
            "Names of Allah dataset must contain exactly 99 names"
        }

        val names = buildList(rows.length()) {
            for (index in 0 until rows.length()) {
                val row = rows.getJSONObject(index)
                val number = row.getInt("number")
                require(number == index + 1) {
                    "Names of Allah ordering mismatch at row $index"
                }
                val arabic = row.getString("arabic")
                val transliteration = row.getString("transliteration")
                val meaning = row.getString("meaning")
                require(arabic.isNotBlank() && transliteration.isNotBlank() && meaning.isNotBlank()) {
                    "Missing Arabic, transliteration or English meaning at name $number"
                }
                val referenceArray = row.optJSONArray("references") ?: JSONArray()
                val references = buildList(referenceArray.length()) {
                    for (refIndex in 0 until referenceArray.length()) {
                        val value = referenceArray.optString(refIndex, "").trim()
                        if (value.isNotEmpty()) add(value)
                    }
                }
                add(
                    AllahName(
                        number = number,
                        arabic = arabic,
                        transliteration = transliteration,
                        meaning = meaning,
                        description = row.optString("description", ""),
                        references = references
                    )
                )
            }
        }

        val source = NamesOfAllahSourceInfo(
            source = manifest.getString("source"),
            sourceUrl = manifest.getString("sourceUrl"),
            sourceCommit = manifest.getString("sourceCommit"),
            license = manifest.getString("license"),
            attribution = manifest.getString("attribution"),
            count = manifest.getInt("recordCount"),
            soraniMeaningIncluded = manifest.optBoolean("soraniMeaningIncluded", false),
            englishMeaningScholarReviewStatus = manifest.getString("englishMeaningScholarReviewStatus"),
            recordsWithReferences = manifest.getInt("recordsWithReferences"),
            recordsWithoutReferences = manifest.getInt("recordsWithoutReferences")
        )
        require(source.license == "Apache-2.0")
        require(source.count == names.size)
        require(!source.soraniMeaningIncluded)
        require(source.englishMeaningScholarReviewStatus == "pending") {
            "Scholar-review status must remain explicit"
        }
        require(source.sourceCommit == "094dc91e11316a0eb5150f84dd106f3ea1be60e5")
        require(source.recordsWithReferences + source.recordsWithoutReferences == names.size)
        return NamesOfAllahPackage(names, source)
    }
}

/** Local-only learning markers for the 99 Names; no account or network sync is required. */
class NamesOfAllahProgressStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        "masheqal_names_of_allah_progress",
        Context.MODE_PRIVATE
    )

    fun isLearned(number: Int): Boolean {
        require(number in 1..99)
        return preferences.getBoolean("learned_$number", false)
    }

    fun setLearned(number: Int, learned: Boolean) {
        require(number in 1..99)
        preferences.edit().putBoolean("learned_$number", learned).apply()
    }

    fun countLearned(): Int = (1..99).count { isLearned(it) }
}
