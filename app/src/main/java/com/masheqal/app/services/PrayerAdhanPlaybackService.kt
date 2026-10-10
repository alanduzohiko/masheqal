package com.masheqal.app.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.masheqal.app.MainActivity
import com.masheqal.app.R

class PrayerAdhanPlaybackService : Service() {
    private var player: ExoPlayer? = null
    private var currentTitle: String = ""

    override fun onCreate() {
        super.onCreate()
        ensureNotificationChannel()
        player = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_ALARM)
                    .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH)
                    .build(),
                true
            )
            .build()
            .also { instance ->
                instance.addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        if (playbackState == Player.STATE_ENDED) stopPlayback()
                    }
                    override fun onPlayerError(error: PlaybackException) {
                        stopPlayback()
                    }
                })
            }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopPlayback()
                return START_NOT_STICKY
            }
            ACTION_PLAY -> {
                val url = intent.getStringExtra(EXTRA_URL)
                currentTitle = intent.getStringExtra(EXTRA_TITLE).orEmpty()
                if (url.isNullOrBlank() || !url.startsWith("https:")) {
                    stopSelf(startId)
                    return START_NOT_STICKY
                }
                startForeground(NOTIFICATION_ID, buildNotification(currentTitle))
                player?.apply {
                    stop()
                    clearMediaItems()
                    setMediaItem(MediaItem.fromUri(url))
                    prepare()
                    playWhenReady = true
                } ?: stopSelf(startId)
            }
        }
        return START_NOT_STICKY
    }

    private fun buildNotification(title: String): Notification {
        val stopIntent = Intent(this, PrayerAdhanPlaybackService::class.java).setAction(ACTION_STOP)
        val stopPending = PendingIntent.getService(
            this,
            STOP_REQUEST_CODE,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val openApp = PendingIntent.getActivity(
            this,
            OPEN_REQUEST_CODE,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(getString(R.string.adhan_playing))
            .setContentText(title.ifBlank { getString(R.string.adhan_default_title) })
            .setContentIntent(openApp)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .addAction(android.R.drawable.ic_media_pause, getString(R.string.stop_adhan), stopPending)
            .build()
    }

    private fun ensureNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (manager.getNotificationChannel(CHANNEL_ID) == null) {
                manager.createNotificationChannel(
                    NotificationChannel(
                        CHANNEL_ID,
                        getString(R.string.adhan_playback_channel),
                        NotificationManager.IMPORTANCE_LOW
                    )
                )
            }
        }
    }

    private fun stopPlayback() {
        player?.run {
            stop()
            clearMediaItems()
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        stopSelf()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        player?.release()
        player = null
        super.onDestroy()
    }

    companion object {
        const val ACTION_PLAY = "com.masheqal.app.action.PLAY_ADHAN"
        const val ACTION_STOP = "com.masheqal.app.action.STOP_ADHAN"
        const val EXTRA_URL = "adhan_url"
        const val EXTRA_TITLE = "adhan_title"
        private const val CHANNEL_ID = "adhan_playback"
        private const val NOTIFICATION_ID = 4921
        private const val STOP_REQUEST_CODE = 4922
        private const val OPEN_REQUEST_CODE = 4923
    }
}
