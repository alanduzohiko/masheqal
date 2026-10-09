package com.masheqal.app.data

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import com.masheqal.app.ui.screens.QuranAudioCatalog
import java.io.File

enum class OfflineAudioStatus {
    NOT_DOWNLOADED,
    DOWNLOADING,
    READY,
    FAILED
}

/**
 * App-private, persistent surah audio downloads managed by Android DownloadManager.
 * A partial file is never returned for playback: only DownloadManager STATUS_SUCCESSFUL
 * plus a non-empty local file is accepted as offline-ready.
 */
object OfflineAudioDownloads {
    private const val PREFS = "masheqal_offline_audio"
    private const val KEY_PREFIX = "download_"

    private fun key(edition: String, surah: Int) = "$"+"KEY_PREFIX$"+"edition:$"+"surah"

    fun enqueue(
        context: Context,
        edition: String,
        surah: Int,
        surahName: String,
        reciterName: String
    ): Long {
        val url = QuranAudioCatalog.surahUrl(surah, edition)
        val target = targetFile(context, edition, surah)
        val parent = target.parentFile ?: error("Offline audio directory is unavailable")
        check(parent.exists() || parent.mkdirs()) { "Could not create the offline audio folder" }

        val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val request = DownloadManager.Request(Uri.parse(url))
            .setTitle(surahName)
            .setDescription(reciterName)
            .setMimeType("audio/mpeg")
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(false)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationUri(Uri.fromFile(target))

        val id = manager.enqueue(request)
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putLong(key(edition, surah), id)
            .apply()
        return id
    }

    fun status(context: Context, edition: String, surah: Int): OfflineAudioStatus {
        val id = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getLong(key(edition, surah), -1L)
        if (id < 0L) return OfflineAudioStatus.NOT_DOWNLOADED
        val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val cursor = manager.query(DownloadManager.Query().setFilterById(id)) ?: return OfflineAudioStatus.FAILED
        cursor.use {
            if (!it.moveToFirst()) return OfflineAudioStatus.FAILED
            return when (it.getInt(it.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))) {
                DownloadManager.STATUS_SUCCESSFUL -> {
                    val file = targetFile(context, edition, surah)
                    if (file.isFile && file.length() > 0L) OfflineAudioStatus.READY
                    else OfflineAudioStatus.FAILED
                }
                DownloadManager.STATUS_PENDING,
                DownloadManager.STATUS_RUNNING,
                DownloadManager.STATUS_PAUSED -> OfflineAudioStatus.DOWNLOADING
                else -> OfflineAudioStatus.FAILED
            }
        }
    }

    fun readyFileOrNull(context: Context, edition: String, surah: Int): File? {
        if (status(context, edition, surah) != OfflineAudioStatus.READY) return null
        return targetFile(context, edition, surah).takeIf { it.isFile && it.length() > 0L }
    }

    private fun targetFile(context: Context, edition: String, surah: Int): File {
        require(surah in 1..114) { "Surah number must be between 1 and 114" }
        require(edition.matches(Regex("ar\\.[a-z]+"))) { "Invalid reciter edition" }
        val root = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC)
            ?: error("App-private music storage is unavailable")
        return File(File(root, "Masheqal/recitations"), edition.replace('.', '-') + "-" + surah.toString().padStart(3, '0') + ".mp3")
    }
}
