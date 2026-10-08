package com.masheqal.app.data

import android.content.Context
import org.json.JSONObject
import java.io.File

/**
 * Runtime gate for external licensed content packages. A package is never activated
 * unless it declares a supported type, version, source, and license metadata.
 */
class ContentPackageManager(private val context: Context) {
    data class PackageInfo(val type: String, val version: String, val title: String, val source: String, val license: String)
    private val root get() = File(context.filesDir, "content-packages")

    fun listInstalled(): List<PackageInfo> = root.listFiles()?.mapNotNull { dir ->
        runCatching {
            val o = JSONObject(File(dir, "manifest.json").readText())
            val type = o.getString("type")
            val version = o.getString("version")
            val title = o.optString("title", type)
            val source = o.getString("source")
            val license = o.getString("license")
            require(type in setOf("quran-sorani", "tafsir", "hadith", "adhkar", "hisn", "dua", "names", "audio"))
            PackageInfo(type, version, title, source, license)
        }.getOrNull()
    }?.sortedBy { it.type } ?: emptyList()

    fun validateManifest(json: String): Result<PackageInfo> = runCatching {
        val o = JSONObject(json)
        val type = o.getString("type")
        require(type in setOf("quran-sorani", "tafsir", "hadith", "adhkar", "hisn", "dua", "names", "audio"))
        PackageInfo(type, o.getString("version"), o.optString("title", type), o.getString("source"), o.getString("license"))
    }
}
