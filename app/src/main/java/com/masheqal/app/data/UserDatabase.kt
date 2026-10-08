package com.masheqal.app.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class UserDatabase(context: Context) : SQLiteOpenHelper(context, "masheqal_user.db", null, 2) {
    data class BookmarkRecord(val type:String,val reference:String,val title:String,val createdAt:Long)
    data class NoteRecord(val reference:String,val body:String,val tags:String,val createdAt:Long,val updatedAt:Long)
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE bookmarks(id INTEGER PRIMARY KEY AUTOINCREMENT, type TEXT NOT NULL, reference TEXT NOT NULL UNIQUE, title TEXT NOT NULL, createdAt INTEGER NOT NULL)")
        db.execSQL("CREATE TABLE notes(id INTEGER PRIMARY KEY AUTOINCREMENT, reference TEXT NOT NULL, body TEXT NOT NULL, tags TEXT NOT NULL DEFAULT '', createdAt INTEGER NOT NULL, updatedAt INTEGER NOT NULL)")
        db.execSQL("CREATE INDEX idx_notes_reference ON notes(reference)")
    }
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        var v = oldVersion
        if (v < 2) {
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_bookmarks_type ON bookmarks(type)")
            v = 2
        }
    }

    fun addBookmark(type: String, reference: String, title: String): Boolean {
        val values = ContentValues().apply { put("type", type); put("reference", reference); put("title", title); put("createdAt", System.currentTimeMillis()) }
        return writableDatabase.insertWithOnConflict("bookmarks", null, values, SQLiteDatabase.CONFLICT_IGNORE) != -1L
    }
    fun removeBookmark(reference: String) = writableDatabase.delete("bookmarks", "reference=?", arrayOf(reference)) > 0
    fun isBookmarked(reference: String): Boolean = readableDatabase.query("bookmarks", arrayOf("id"), "reference=?", arrayOf(reference), null, null, null, "1").use { it.moveToFirst() }
    fun listBookmarks(): List<Pair<String,String>> = readableDatabase.query("bookmarks", arrayOf("reference","title"), null, null, null, null, "createdAt DESC").use { c -> buildList { while (c.moveToNext()) add(c.getString(0) to c.getString(1)) } }
    fun listBookmarksDetailed(): List<BookmarkRecord> = readableDatabase.query("bookmarks", arrayOf("type","reference","title","createdAt"), null, null, null, null, "createdAt DESC").use { c -> buildList { while (c.moveToNext()) add(BookmarkRecord(c.getString(0),c.getString(1),c.getString(2),c.getLong(3))) } }
    fun searchBookmarks(query:String): List<BookmarkRecord> = readableDatabase.query("bookmarks", arrayOf("type","reference","title","createdAt"), "reference LIKE ? OR title LIKE ?", arrayOf("%$query%","%$query%"), null, null, "createdAt DESC").use { c -> buildList { while(c.moveToNext()) add(BookmarkRecord(c.getString(0),c.getString(1),c.getString(2),c.getLong(3))) } }

    fun addNote(reference: String, body: String, tags: String = "") {
        val now = System.currentTimeMillis()
        val v = ContentValues().apply { put("reference", reference); put("body", body); put("tags", tags); put("createdAt", now); put("updatedAt", now) }
        writableDatabase.insertOrThrow("notes", null, v)
    }
    fun listNotes(): List<Triple<String,String,String>> = readableDatabase.query("notes", arrayOf("reference","body","tags"), null, null, null, null, "updatedAt DESC").use { c -> buildList { while (c.moveToNext()) add(Triple(c.getString(0), c.getString(1), c.getString(2))) } }
    fun listNotesDetailed(): List<NoteRecord> = readableDatabase.query("notes", arrayOf("reference","body","tags","createdAt","updatedAt"), null, null, null, null, "updatedAt DESC").use { c -> buildList { while (c.moveToNext()) add(NoteRecord(c.getString(0),c.getString(1),c.getString(2),c.getLong(3),c.getLong(4))) } }
    fun searchNotes(query:String): List<NoteRecord> = readableDatabase.query("notes", arrayOf("reference","body","tags","createdAt","updatedAt"), "reference LIKE ? OR body LIKE ? OR tags LIKE ?", arrayOf("%$query%","%$query%","%$query%"), null, null, "updatedAt DESC").use { c -> buildList { while(c.moveToNext()) add(NoteRecord(c.getString(0),c.getString(1),c.getString(2),c.getLong(3),c.getLong(4))) } }
}
