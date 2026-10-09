package com.masheqal.app.ui.screens

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.masheqal.app.R
import com.masheqal.app.services.QuranPlaybackService

private data class AdhanVoiceOption(
    val id: String,
    val titleRes: Int,
    val sourceRes: Int
)

private val adhanVoiceOptions = listOf(
    AdhanVoiceOption(
        QuranPlaybackService.DEFAULT_ADHAN_VOICE,
        R.string.adhan_voice_beautiful,
        R.string.adhan_voice_source_beautiful
    ),
    AdhanVoiceOption(
        QuranPlaybackService.ADHAN_VOICE_COMMUNITY,
        R.string.adhan_voice_community,
        R.string.adhan_voice_source_community
    )
)

@Composable
fun AdhanVoicePicker(
    selectedVoice: String,
    onVoiceSelected: (String) -> Unit,
    onPreview: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            stringResource(R.string.adhan_voice_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        adhanVoiceOptions.forEach { voice ->
            val selected = selectedVoice == voice.id
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (selected) MaterialTheme.colorScheme.secondaryContainer
                    else MaterialTheme.colorScheme.surface
                )
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(start = 8.dp, end = 10.dp, top = 8.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = selected,
                        onClick = { onVoiceSelected(voice.id) }
                    )
                    Column(
                        Modifier.weight(1f).padding(vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(
                            stringResource(voice.titleRes),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                        )
                        Text(
                            stringResource(voice.sourceRes),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { onPreview(voice.id) }) {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = stringResource(R.string.adhan_voice_preview),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

fun previewAdhanVoice(context: Context, voiceId: String, previewLabel: String) {
    val playbackIntent = Intent(context, QuranPlaybackService::class.java)
        .setAction(QuranPlaybackService.ACTION_PLAY_ADHAN)
        .putExtra(QuranPlaybackService.EXTRA_PRAYER_NAME, previewLabel)
        .putExtra(QuranPlaybackService.EXTRA_ADHAN_VOICE_ID, voiceId)
    ContextCompat.startForegroundService(context, playbackIntent)
}
