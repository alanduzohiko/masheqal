package com.masheqal.app.services

import android.content.Intent
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.masheqal.app.R

class QuranPlaybackService : MediaSessionService() {
    private var mediaSession: MediaSession? = null
    private lateinit var player: ExoPlayer

    override fun onCreate() {
        super.onCreate()
        player = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                true
            )
            .build()
        player.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                val isAdhan = player.currentMediaItem?.mediaId?.startsWith("adhan-") == true
                if (playbackState == Player.STATE_ENDED && isAdhan) {
                    player.clearMediaItems()
                    stopSelf()
                }
            }
        })
        mediaSession = MediaSession.Builder(this, player).build()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_PLAY_ADHAN) {
            val prayerName = intent.getStringExtra(EXTRA_PRAYER_NAME)
                ?: getString(R.string.prayer)
            val preferences = getSharedPreferences(ADHAN_PREFERENCES, MODE_PRIVATE)
            val voiceId = intent.getStringExtra(EXTRA_ADHAN_VOICE_ID)
                ?: preferences.getString(ADHAN_VOICE_KEY, DEFAULT_ADHAN_VOICE)
                ?: DEFAULT_ADHAN_VOICE
            val (audioResource, voiceLabel) = when (voiceId) {
                ADHAN_VOICE_COMMUNITY -> R.raw.adhan_community to R.string.adhan_voice_community
                else -> R.raw.adhan to R.string.adhan_voice_beautiful
            }
            val metadata = MediaMetadata.Builder()
                .setTitle(prayerName)
                .setDisplayTitle(prayerName)
                .setArtist(getString(voiceLabel))
                .setAlbumTitle(getString(R.string.app_name))
                .build()
            val adhan = MediaItem.Builder()
                .setMediaId("adhan-${System.currentTimeMillis()}")
                .setUri("android.resource://$packageName/$audioResource")
                .setMediaMetadata(metadata)
                .build()
            player.setMediaItem(adhan)
            player.prepare()
            player.play()
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onTaskRemoved(rootIntent: Intent?) {
        if (!player.playWhenReady) stopSelf()
    }

    override fun onDestroy() {
        mediaSession?.release()
        player.release()
        super.onDestroy()
    }

    companion object {
        const val ACTION_PLAY_ADHAN = "com.masheqal.app.action.PLAY_ADHAN"
        const val EXTRA_PRAYER_NAME = "com.masheqal.app.extra.PRAYER_NAME"
        const val EXTRA_ADHAN_VOICE_ID = "com.masheqal.app.extra.ADHAN_VOICE_ID"
        const ADHAN_PREFERENCES = "masheqal_adhan_preferences"
        const ADHAN_VOICE_KEY = "voice"
        const DEFAULT_ADHAN_VOICE = "beautiful_adhan"
        const ADHAN_VOICE_COMMUNITY = "community_adhan"
    }
}
