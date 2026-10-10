package com.masheqal.app.data

import android.content.Context
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject
import java.nio.charset.StandardCharsets

object BackupRepository {
    private const val SCHEMA = 1

    fun export(context: Context, uri: Uri, userDb: UserDatabase, settings: SettingsState, reading: ReadingPosition, khatmah: KhatmahState): Result<Unit> = runCatching {
        val root = JSONObject()
            .put("schema", SCHEMA)
            .put("product", "مەشخەڵ")
            .put("exportedAt", System.currentTimeMillis())
            .put("settings", JSONObject().put("theme", settings.theme).put("language", settings.language).put("tasbihCount", settings.tasbihCount).put("prayerMethod", settings.prayerMethod).put("madhhab", settings.madhhab).put("awake", settings.keepScreenAwake).put("reciter", settings.reciter).put("showEnglishTranslation", settings.showEnglishTranslation).put("translationEdition", settings.translationEdition).put("tafsirEdition", settings.tafsirEdition).put("showTafsir", settings.showTafsir).put("adhanRecordingId", settings.adhanRecordingId).put("prayerRemindersEnabled", settings.prayerRemindersEnabled).put("playFullAdhan", settings.playFullAdhan))
            .put("reading", JSONObject().put("surah", reading.surah).put("ayah", reading.ayah))
            .put("khatmah", JSONObject().put("days", khatmah.days).put("targetPages", khatmah.targetPages).put("readPages", khatmah.readPages).put("active", khatmah.active))
        val bookmarks = JSONArray()
        userDb.listBookmarksDetailed().forEach { b -> bookmarks.put(JSONObject().put("type", b.type).put("reference", b.reference).put("title", b.title).put("createdAt", b.createdAt)) }
        val notes = JSONArray()
        userDb.listNotesDetailed().forEach { n -> notes.put(JSONObject().put("reference", n.reference).put("body", n.body).put("tags", n.tags).put("createdAt", n.createdAt).put("updatedAt", n.updatedAt)) }
        root.put("bookmarks", bookmarks).put("notes", notes)
        context.contentResolver.openOutputStream(uri)?.use { it.write(root.toString(2).toByteArray(StandardCharsets.UTF_8)) }
            ?: error("Could not open backup destination")
    }

    fun restore(context: Context, uri: Uri, userDb: UserDatabase): Result<BackupPayload> = runCatching {
        val text = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } ?: error("Could not read backup")
        val o = JSONObject(text)
        require(o.optInt("schema", -1) == SCHEMA) { "Unsupported backup schema" }
        require(o.optString("product") == "مەشخەڵ") { "Not a مەشخەڵ backup" }
        val bookmarks = mutableListOf<BackupBookmark>()
        o.optJSONArray("bookmarks")?.let { a -> for (i in 0 until a.length()) { val b=a.getJSONObject(i); bookmarks += BackupBookmark(b.optString("type","ayah"), b.getString("reference"), b.optString("title",b.getString("reference"))) } }
        val notes = mutableListOf<BackupNote>()
        o.optJSONArray("notes")?.let { a -> for (i in 0 until a.length()) { val n=a.getJSONObject(i); notes += BackupNote(n.getString("reference"), n.getString("body"), n.optString("tags")) } }
        bookmarks.forEach { userDb.addBookmark(it.type,it.reference,it.title) }
        notes.forEach { if (it.body.isNotBlank()) userDb.addNote(it.reference,it.body,it.tags) }
        val s = o.optJSONObject("settings") ?: JSONObject()
        val r = o.optJSONObject("reading") ?: JSONObject()
        val k = o.optJSONObject("khatmah") ?: JSONObject()
        BackupPayload(
            SettingsState(s.optString("theme","system"), s.optString("language","ckb"), s.optInt("tasbihCount",0), s.optString("prayerMethod","MWL"), s.optString("madhhab","SHAFI"), s.optBoolean("awake",false), s.optString("reciter","ar.alafasy"), s.optBoolean("showEnglishTranslation",true), false, s.optString("translationEdition","en.sahih"), s.optString("tafsirEdition","ar.muyassar"), s.optBoolean("showTafsir",false), s.optString("adhanRecordingId","adhan-198"), s.optBoolean("prayerRemindersEnabled",false), s.optBoolean("playFullAdhan",true)),
            ReadingPosition(r.optInt("surah",1), r.optInt("ayah",1)),
            KhatmahState(k.optInt("days",30), k.optInt("targetPages",604), k.optInt("readPages",0), k.optBoolean("active",false))
        )
    }

    data class BackupPayload(val settings: SettingsState, val reading: ReadingPosition, val khatmah: KhatmahState)
    data class BackupBookmark(val type:String,val reference:String,val title:String)
    data class BackupNote(val reference:String,val body:String,val tags:String)
}
