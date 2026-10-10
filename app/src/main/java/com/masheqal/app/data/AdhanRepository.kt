package com.masheqal.app.data

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

data class AdhanRecording(
    val id: String,
    val fileName: String,
    val title: String,
    val arabicTitle: String,
    val sizeBytes: Long,
    val category: String,
    val featured: Boolean
) {
    fun streamUri(): String = streamUriForFile(fileName)

    val displayTitle: String
        get() = if (arabicTitle.isBlank()) title else "$title — $arabicTitle"
}

class AdhanRepository(private val context: Context) {
    companion object {
        fun streamUriForFile(fileName: String): String = Uri.parse("https:" + "/" + "/raw.githubusercontent.com/Kiwifu/adhan-mp3/main/")
            .buildUpon()
            .appendPath(fileName)
            .build()
            .toString()
    }

    suspend fun loadCatalog(): List<AdhanRecording> = withContext(Dispatchers.IO) {
        val source = context.assets.open("content/adhan_catalog.json")
            .bufferedReader(Charsets.UTF_8).use { it.readText() }
        val root = JSONObject(source)
        val array = root.getJSONArray("recordings")
        buildList(array.length()) {
            for (i in 0 until array.length()) {
                val item = array.optJSONObject(i) ?: continue
                val fileName = item.optString("fileName", "")
                val title = item.optString("title", "").trim()
                if (fileName.isBlank() || title.isBlank()) continue
                add(
                    AdhanRecording(
                        id = item.optString("id", "adhan-${i + 1}"),
                        fileName = fileName,
                        title = title,
                        arabicTitle = item.optString("arabicTitle", "").trim(),
                        sizeBytes = item.optLong("sizeBytes", 0L),
                        category = item.optString("category", "muezzin"),
                        featured = item.optBoolean("featured", false)
                    )
                )
            }
        }
    }
}
