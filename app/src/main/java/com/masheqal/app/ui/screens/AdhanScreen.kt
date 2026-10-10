package com.masheqal.app.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.masheqal.app.MasheqalApp
import com.masheqal.app.R
import com.masheqal.app.data.AdhanRecording
import com.masheqal.app.data.AdhanRepository
import com.masheqal.app.data.SettingsState
import com.masheqal.app.services.PrayerNotificationScheduler
import kotlinx.coroutines.launch

private data class AdhanFilter(val key: String, val label: Int)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdhanScreen(app: MasheqalApp, nav: NavHostController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settings by app.settings.state.collectAsState(initial = SettingsState())
    var recordings by remember { mutableStateOf(emptyList<AdhanRecording>()) }
    var isLoading by remember { mutableStateOf(true) }
    var failed by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf("all") }
    var playingId by remember { mutableStateOf<String?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    val player = remember(context) {
        ExoPlayer.Builder(context)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                true
            )
            .build()
    }
    val selectedId = settings.adhanRecordingId
    val previewErrorMessage = stringResource(R.string.adhan_preview_error)
    val selectionSuccessMessage = stringResource(R.string.adhan_select_success)

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlayingNow: Boolean) {
                isPlaying = isPlayingNow
                if (!isPlayingNow && player.playbackState == Player.STATE_IDLE) playingId = null
            }
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    player.stop()
                    playingId = null
                }
            }
            override fun onPlayerError(error: PlaybackException) {
                playingId = null
                scope.launch { snackbar.showSnackbar(previewErrorMessage) }
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            player.release()
        }
    }

    LaunchedEffect(Unit) {
        runCatching { AdhanRepository(context).loadCatalog() }
            .onSuccess {
                recordings = it
                isLoading = false
                failed = it.isEmpty()
            }
            .onFailure {
                isLoading = false
                failed = true
            }
    }

    val filters = listOf(
        AdhanFilter("all", R.string.adhan_all),
        AdhanFilter("featured", R.string.adhan_featured),
        AdhanFilter("muezzin", R.string.adhan_muezzins),
        AdhanFilter("mosque-region", R.string.adhan_mosques),
        AdhanFilter("fajr", R.string.adhan_fajr)
    )
    val filtered = remember(recordings, query, filter) {
        recordings.filter { recording ->
            val matchesFilter = when (filter) {
                "featured" -> recording.featured
                "all" -> true
                else -> recording.category == filter
            }
            matchesFilter && (
                query.isBlank() ||
                recording.title.contains(query.trim(), ignoreCase = true) ||
                recording.arabicTitle.contains(query.trim(), ignoreCase = true) ||
                recording.fileName.contains(query.trim(), ignoreCase = true)
            )
        }.sortedWith(compareByDescending<AdhanRecording> { it.featured }.thenBy { it.title })
    }
    val chosen = recordings.firstOrNull { it.id == selectedId }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.adhan_library)) },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, stringResource(R.string.done))
                    }
                },
                actions = {
                    IconButton(onClick = {
                        if (isPlaying) {
                            player.pause()
                            playingId = null
                        } else {
                            chosen?.let {
                                player.setMediaItem(MediaItem.fromUri(it.streamUri()))
                                player.prepare()
                                player.play()
                                playingId = it.id
                            }
                        }
                    }) {
                        Icon(
                            if (player.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            stringResource(R.string.adhan_preview)
                        )
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(top = 8.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth().animateContentSize(),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Box(
                        Modifier.fillMaxWidth().background(
                            Brush.linearGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                                    MaterialTheme.colorScheme.secondary.copy(alpha = 0.62f)
                                )
                            )
                        )
                    ) {
                        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = Color.White.copy(alpha = 0.16f)
                                ) {
                                    Icon(
                                        Icons.Default.GraphicEq,
                                        null,
                                        modifier = Modifier.padding(12.dp).size(30.dp),
                                        tint = Color.White
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = Color.White.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        stringResource(R.string.adhan_recording_count, recordings.size),
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        style = MaterialTheme.typography.labelLarge,
                                        color = Color.White
                                    )
                                }
                            }
                            Text(
                                stringResource(R.string.adhan_library_title),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                stringResource(R.string.adhan_library_subtitle),
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.88f)
                            )
                            chosen?.let { recording ->
                                HorizontalDivider(color = Color.White.copy(alpha = 0.3f))
                                Text(
                                    stringResource(R.string.adhan_selected) + ": " + recording.displayTitle,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.NotificationsActive, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(R.string.adhan_play_full), style = MaterialTheme.typography.titleSmall)
                            Text(
                                stringResource(if (settings.prayerRemindersEnabled) R.string.adhan_reminders_enabled else R.string.adhan_reminders_disabled),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = settings.playFullAdhan,
                            onCheckedChange = { enabled ->
                                scope.launch { app.settings.setPlayFullAdhan(enabled) }
                                chosen?.let {
                                    PrayerNotificationScheduler.configureAdhan(
                                        context,
                                        it.fileName,
                                        it.displayTitle,
                                        enabled
                                    )
                                }
                            }
                        )
                    }
                }
            }

            item {
                Text(
                    stringResource(R.string.adhan_source_note),
                    modifier = Modifier.padding(horizontal = 20.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    singleLine = true,
                    placeholder = { Text(stringResource(R.string.adhan_search_hint)) },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    trailingIcon = {
                        if (query.isNotEmpty()) IconButton(onClick = { query = "" }) {
                            Icon(Icons.Default.Close, stringResource(R.string.clear))
                        }
                    }
                )
            }

            item {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    filters.take(3).forEach { option ->
                        FilterChip(
                            selected = filter == option.key,
                            onClick = { filter = option.key },
                            label = { Text(stringResource(option.label)) }
                        )
                    }
                }
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    filters.drop(3).forEach { option ->
                        FilterChip(
                            selected = filter == option.key,
                            onClick = { filter = option.key },
                            label = { Text(stringResource(option.label)) }
                        )
                    }
                }
            }

            if (isLoading) {
                item {
                    Box(Modifier.fillMaxWidth().padding(28.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            } else if (failed) {
                item {
                    EmptyState(
                        title = stringResource(R.string.dhikr_load_error_title),
                        details = stringResource(R.string.adhan_preview_error),
                        icon = Icons.Default.CloudOff
                    )
                }
            } else if (filtered.isEmpty()) {
                item {
                    EmptyState(
                        title = stringResource(R.string.adhan_no_results),
                        details = stringResource(R.string.adhan_no_results_hint),
                        icon = Icons.Default.Search
                    )
                }
            } else {
                items(filtered, key = { it.id }) { recording ->
                    val isSelected = selectedId == recording.id
                    val isThisPlaying = playingId == recording.id && isPlaying
                    Card(
                        modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth().animateContentSize(),
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.Top
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(13.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Icon(
                                        when (recording.category) {
                                            "fajr" -> Icons.Default.WbSunny
                                            "mosque-region" -> Icons.Default.LocationCity
                                            else -> Icons.Default.RecordVoiceOver
                                        },
                                        null,
                                        modifier = Modifier.padding(10.dp).size(22.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Spacer(Modifier.width(11.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        recording.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (recording.arabicTitle.isNotBlank()) {
                                        Text(
                                            recording.arabicTitle,
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Text(
                                        String.format("%.1f MB", recording.sizeBytes / (1024.0 * 1024.0)),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (recording.featured) {
                                    Icon(Icons.Default.Star, stringResource(R.string.adhan_featured), tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                FilledTonalButton(
                                    onClick = {
                                        if (isThisPlaying) {
                                            player.pause()
                                            playingId = null
                                        } else {
                                            player.stop()
                                            player.setMediaItem(MediaItem.fromUri(recording.streamUri()))
                                            player.prepare()
                                            player.play()
                                            playingId = recording.id
                                        }
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        if (isThisPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        null
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(stringResource(if (isThisPlaying) R.string.adhan_stop_preview else R.string.adhan_preview))
                                }
                                if (isSelected) {
                                    OutlinedButton(
                                        onClick = {
                                            scope.launch { snackbar.showSnackbar(selectionSuccessMessage) }
                                        },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.CheckCircle, null)
                                        Spacer(Modifier.width(5.dp))
                                        Text(stringResource(R.string.adhan_selected))
                                    }
                                } else {
                                    Button(
                                        onClick = {
                                            scope.launch {
                                                app.settings.setAdhanRecordingId(recording.id)
                                                PrayerNotificationScheduler.configureAdhan(
                                                    context,
                                                    recording.fileName,
                                                    recording.displayTitle,
                                                    settings.playFullAdhan
                                                )
                                                snackbar.showSnackbar(selectionSuccessMessage)
                                            }
                                        },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.Check, null)
                                        Spacer(Modifier.width(5.dp))
                                        Text(stringResource(R.string.adhan_select))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
