package com.masheqal.app.data

import android.content.Context
import org.json.JSONArray
import java.text.Normalizer


data class QuranVerse(
    val id: Int,
    val surah: Int,
    val ayah: Int,
    val text: String,
    val translationEn: String? = null,
    val translationCkb: String? = null,
    val translationCkbFootnotes: String? = null
)
data class QuranTranslationInfo(
    val translator: String,
    val publisher: String,
    val version: String,
    val lastUpdate: String,
    val attribution: String,
    val translatedAyahCount: Int,
    val missingAyahs: List<String>
)
data class SurahMeta(val number: Int, val nameAr: String, val nameEn: String, val ayahCount: Int, val revelation: String)
data class SearchHit(val verse: QuranVerse, val matchedField: String)
data class QuranRange(val number: Int, val firstGlobalAyah: Int, val lastGlobalAyah: Int)

class QuranRepository(private val context: Context) {
    @Volatile private var verses: List<QuranVerse>? = null
    @Volatile private var surahs: List<SurahMeta>? = null
    @Volatile private var pages: List<QuranRange>? = null
    @Volatile private var juzs: List<QuranRange>? = null
    @Volatile private var soraniInfo: QuranTranslationInfo? = null

    suspend fun loadSurahs(): List<SurahMeta> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        surahs ?: synchronized(this@QuranRepository) {
            surahs ?: readSurahs().also { surahs = it }
        }
    }

    suspend fun loadSoraniTranslationInfo(): QuranTranslationInfo =
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            soraniInfo ?: synchronized(this@QuranRepository) {
                soraniInfo ?: readSoraniTranslationInfo().also { soraniInfo = it }
            }
        }

    suspend fun loadVerses(): List<QuranVerse> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        verses ?: synchronized(this@QuranRepository) {
            verses ?: readVerses().also { verses = it }
        }
    }

    suspend fun versesOfSurah(surah: Int): List<QuranVerse> = loadVerses().filter { it.surah == surah }

    suspend fun getVerse(surah: Int, ayah: Int): QuranVerse? = loadVerses().firstOrNull { it.surah == surah && it.ayah == ayah }

    suspend fun pageForVerse(surah: Int, ayah: Int): Int = globalRangeFor(loadPages(), (loadVerses().firstOrNull { it.surah==surah && it.ayah==ayah }?.id ?: 1))
    suspend fun versesOfPage(page: Int): List<QuranVerse> = loadVerses().filter { it.id in (loadPages().getOrNull(page-1)?.let { it.firstGlobalAyah..it.lastGlobalAyah } ?: IntRange.EMPTY) }
    suspend fun juzForVerse(surah: Int, ayah: Int): Int = globalRangeFor(loadJuzs(), (loadVerses().firstOrNull { it.surah==surah && it.ayah==ayah }?.id ?: 1))
    suspend fun versesOfJuz(juz: Int): List<QuranVerse> = loadVerses().filter { it.id in (loadJuzs().getOrNull(juz-1)?.let { it.firstGlobalAyah..it.lastGlobalAyah } ?: IntRange.EMPTY) }
    suspend fun loadPages(): List<QuranRange> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        pages ?: synchronized(this@QuranRepository) { pages ?: readRanges("content/quran_page_ranges.json", "page").also { pages=it } }
    }
    suspend fun loadJuzs(): List<QuranRange> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        juzs ?: synchronized(this@QuranRepository) { juzs ?: readRanges("content/quran_juz_ranges.json", "juz").also { juzs=it } }
    }

    suspend fun search(query: String, limit: Int = 60): List<SearchHit> {
        val q = query.trim()
        if (q.isEmpty()) return emptyList()
        val reference = parseReference(q)
        if (reference != null) {
            val hit = getVerse(reference.first, reference.second)
            return if (hit != null) listOf(SearchHit(hit, "reference")) else emptyList()
        }
        val nq = normalize(q)
        return loadVerses().asSequence()
            .mapNotNull { verse ->
                val ar = normalize(verse.text)
                val ckb = normalize(verse.translationCkb.orEmpty())
                val en = normalize(verse.translationEn.orEmpty())
                when {
                    ar == nq -> SearchHit(verse, "exact Arabic")
                    ar.startsWith(nq) -> SearchHit(verse, "Arabic prefix")
                    ar.contains(nq) -> SearchHit(verse, "Arabic")
                    ckb == nq -> SearchHit(verse, "exact Sorani")
                    ckb.startsWith(nq) -> SearchHit(verse, "Sorani prefix")
                    ckb.contains(nq) -> SearchHit(verse, "Sorani")
                    en == nq -> SearchHit(verse, "exact English")
                    en.startsWith(nq) -> SearchHit(verse, "English prefix")
                    en.contains(nq) -> SearchHit(verse, "English")
                    else -> null
                }
            }
            .take(limit)
            .toList()
    }

    fun parseReference(raw: String): Pair<Int, Int>? {
        val q = raw.trim().replace('：', ':').replace('－', '-')
        val m = Regex("^(\\d{1,3})\\s*[:\\- ]\\s*(\\d{1,3})$").find(q) ?: return null
        val s = m.groupValues[1].toIntOrNull() ?: return null
        val a = m.groupValues[2].toIntOrNull() ?: return null
        if (s !in 1..114 || a < 1) return null
        return s to a
    }

    private fun readSurahs(): List<SurahMeta> {
        val text = context.assets.open("content/surahs.json").bufferedReader().use { it.readText() }
        val array = JSONArray(text)
        return buildList(array.length()) {
            for (i in 0 until array.length()) {
                val o = array.getJSONObject(i)
                add(SurahMeta(o.getInt("number"), o.getString("name_ar"), o.getString("name_en"), o.getInt("ayah_count"), o.getString("revelation")))
            }
        }
    }

    private fun readRanges(path: String, key: String): List<QuranRange> {
        val text = context.assets.open(path).bufferedReader().use { it.readText() }
        val array = JSONArray(text)
        return buildList(array.length()) {
            for (i in 0 until array.length()) {
                val o=array.getJSONObject(i)
                add(QuranRange(o.getInt(key),o.getInt("first_global_ayah"),o.getInt("last_global_ayah")))
            }
        }
    }

    private fun globalRangeFor(ranges: List<QuranRange>, globalAyah: Int): Int = ranges.firstOrNull { globalAyah in it.firstGlobalAyah..it.lastGlobalAyah }?.number ?: 1

    private fun readVerses(): List<QuranVerse> {
        val arText = context.assets.open("content/quran_ar_uthmani.json").bufferedReader().use { it.readText() }
        val enText = context.assets.open("content/quran_en_translation.json").bufferedReader().use { it.readText() }
        val ckbText = context.assets.open("content/quran_ckb_translation.json").bufferedReader().use { it.readText() }
        val ar = JSONArray(arText)
        val en = JSONArray(enText)
        val ckb = JSONArray(ckbText)
        require(ar.length() == 6236 && en.length() == 6236 && ckb.length() == 6236) {
            "Quran package integrity check failed"
        }
        return buildList(ar.length()) {
            for (i in 0 until ar.length()) {
                val a = ar.getJSONObject(i)
                val e = en.getJSONObject(i)
                val k = ckb.getJSONObject(i)
                require(
                    a.getInt("id") == e.getInt("id") && a.getInt("surah") == e.getInt("surah") &&
                        a.getInt("ayah") == e.getInt("ayah") &&
                        a.getInt("id") == k.getInt("id") && a.getInt("surah") == k.getInt("surah") &&
                        a.getInt("ayah") == k.getInt("ayah")
                ) { "Translation mapping integrity failed at global ayah $i" }
                val sorani = k.optString("text", "")
                require(sorani.isNotBlank() || k.optBoolean("missing", false)) {
                    "Empty Sorani translation must be explicitly marked missing at global ayah ${a.getInt("id")}"
                }
                add(
                    QuranVerse(
                        id = a.getInt("id"),
                        surah = a.getInt("surah"),
                        ayah = a.getInt("ayah"),
                        text = a.getString("text"),
                        translationEn = e.getString("text"),
                        translationCkb = sorani.takeIf { it.isNotBlank() },
                        translationCkbFootnotes = k.optString("footnotes", "").takeIf { it.isNotBlank() }
                    )
                )
            }
        }
    }

    private fun readSoraniTranslationInfo(): QuranTranslationInfo {
        val manifestText = context.assets.open("content/quran_ckb_manifest.json")
            .bufferedReader().use { it.readText() }
        val o = org.json.JSONObject(manifestText)
        val missingJson = o.optJSONArray("missingAyahs") ?: org.json.JSONArray()
        val missing = buildList(missingJson.length()) {
            for (i in 0 until missingJson.length()) add(missingJson.getString(i))
        }
        return QuranTranslationInfo(
            translator = o.optString("translator", ""),
            publisher = o.optString("publisher", "QuranEnc.com"),
            version = o.optString("version", ""),
            lastUpdate = o.optString("lastUpdate", ""),
            attribution = o.optString("attribution", "QuranEnc.com"),
            translatedAyahCount = o.optInt("translatedAyahCount", 0),
            missingAyahs = missing
        )
    }

    companion object {
        fun normalize(input: String): String {
            val folded = input
                .replace('أ', 'ا').replace('إ', 'ا').replace('آ', 'ا')
                .replace('ى', 'ي').replace('ئ', 'ي').replace('ؤ', 'و').replace('ة', 'ه')
                .replace("ٱ", "ا")
            return Normalizer.normalize(folded.lowercase(), Normalizer.Form.NFD)
                .replace("\\p{M}+".toRegex(), "")
                .replace("ـ", "")
                .replace("[\\s\\p{Punct}]+".toRegex(), " ")
                .trim()
        }
    }
}
