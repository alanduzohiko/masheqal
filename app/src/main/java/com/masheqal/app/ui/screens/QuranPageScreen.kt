package com.masheqal.app.ui.screens

import android.content.ComponentName
import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.navigation.NavHostController
import com.masheqal.app.MasheqalApp
import com.masheqal.app.R
import com.masheqal.app.data.QuranVerse
import com.masheqal.app.services.QuranPlaybackService
import kotlinx.coroutines.launch

@androidx.annotation.OptIn(markerClass = [UnstableApi::class])
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuranPageScreen(app: MasheqalApp, nav: NavHostController, page: Int) {
    val currentPage = page.coerceIn(1, 604)
    val context = LocalContext.current
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val scope = rememberCoroutineScope()
    var verses by remember { mutableStateOf(emptyList<QuranVerse>()) }
    var currentJuz by remember { mutableStateOf(1) }
    var selectedAyah by remember { mutableStateOf<QuranVerse?>(null) }
    var showEnglishMeaning by remember(selectedAyah?.id) { mutableStateOf(false) }
    var audioController by remember { mutableStateOf<MediaController?>(null) }
    var audioConnectionFailed by remember { mutableStateOf(false) }
    var audioPlaybackFailed by remember { mutableStateOf(false) }
    var playingMediaId by remember { mutableStateOf<String?>(null) }
    var isAudioPlaying by remember { mutableStateOf(false) }
    var repeatAyah by rememberSaveable { mutableStateOf(false) }

    DisposableEffect(context) {
        var active = true
        val token = SessionToken(context, ComponentName(context, QuranPlaybackService::class.java))
        val future = MediaController.Builder(context, token).buildAsync()
        future.addListener({
            runCatching { future.get() }
                .onSuccess {
                    if (active) {
                        audioController = it
                        audioConnectionFailed = false
                    } else {
                        MediaController.releaseFuture(future)
                    }
                }
                .onFailure {
                    if (active) audioConnectionFailed = true
                }
        }, ContextCompat.getMainExecutor(context))
        onDispose {
            active = false
            MediaController.releaseFuture(future)
            audioController = null
        }
    }

    DisposableEffect(audioController) {
        val controller = audioController
        if (controller == null) {
            onDispose { }
        } else {
            val listener = object : Player.Listener {
                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    playingMediaId = mediaItem?.mediaId
                    isAudioPlaying = controller.isPlaying
                }

                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    isAudioPlaying = isPlaying
                }

                override fun onPlayerError(error: PlaybackException) {
                    audioPlaybackFailed = true
                    isAudioPlaying = false
                }
            }
            controller.addListener(listener)
            playingMediaId = controller.currentMediaItem?.mediaId
            isAudioPlaying = controller.isPlaying
            onDispose { controller.removeListener(listener) }
        }
    }

    LaunchedEffect(currentPage) {
        verses = app.quran.versesOfPage(currentPage)
        currentJuz = verses.firstOrNull()?.let {
            app.quran.juzForVerse(it.surah, it.ayah)
        } ?: 1
    }

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("${stringResource(R.string.page)} $currentPage") },
            navigationIcon = {
                IconButton(onClick = { nav.popBackStack() }) {
                    Icon(
                        if (isRtl) Icons.Default.ArrowForward else Icons.Default.ArrowBack,
                        contentDescription = stringResource(R.string.back_step)
                    )
                }
            },
            actions = {
                IconButton(
                    onClick = { if (currentPage > 1) nav.navigate("quran/page/${currentPage - 1}") },
                    enabled = currentPage > 1
                ) {
                    Icon(
                        if (isRtl) Icons.Default.ChevronRight else Icons.Default.ChevronLeft,
                        contentDescription = stringResource(R.string.previous_page)
                    )
                }
                IconButton(
                    onClick = { if (currentPage < 604) nav.navigate("quran/page/${currentPage + 1}") },
                    enabled = currentPage < 604
                ) {
                    Icon(
                        if (isRtl) Icons.Default.ChevronLeft else Icons.Default.ChevronRight,
                        contentDescription = stringResource(R.string.next_page)
                    )
                }
            }
        )
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("${stringResource(R.string.juz)} $currentJuz")
            Text(
                stringResource(R.string.mushaf_edition),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        MushafPageImage(
            page = currentPage,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            onAyahSelected = { selectedSurah, selectedNumber ->
                scope.launch {
                    val selected = runCatching {
                        app.quran.loadVerses().firstOrNull {
                            it.surah == selectedSurah && it.ayah == selectedNumber
                        }
                    }.getOrNull()
                    if (selected != null) {
                        selectedAyah = selected
                        app.personal.setReading(selectedSurah, selectedNumber)
                    }
                }
            }
        )
    }

    selectedAyah?.let { verse ->
        ModalBottomSheet(onDismissRequest = { selectedAyah = null }) {
            Column(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(stringResource(R.string.ayah_options), style = MaterialTheme.typography.titleMedium)
                Text(
                    "${verse.surah}:${verse.ayah}",
                    color = MaterialTheme.colorScheme.secondary,
                    style = MaterialTheme.typography.labelLarge
                )
                QuranText(verse.text, size = 27f)

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val ayahMediaId = "ayah-${verse.id}"
                    val isThisAyahPlaying = playingMediaId == ayahMediaId && isAudioPlaying
                    FilledTonalButton(
                        onClick = {
                            val controller = audioController
                            if (controller == null) {
                                audioConnectionFailed = true
                            } else if (playingMediaId == ayahMediaId && controller.isPlaying) {
                                controller.pause()
                            } else {
                                val selectedEdition = context.getSharedPreferences(
                                    "masheqal_audio_preferences",
                                    Context.MODE_PRIVATE
                                ).getString("reciter", "ar.alafasy") ?: "ar.alafasy"
                                if (playingMediaId != ayahMediaId) {
                                    val item = MediaItem.Builder()
                                        .setMediaId(ayahMediaId)
                                        .setUri(QuranAudioCatalog.ayahUrl(verse.id, selectedEdition))
                                        .setMediaMetadata(
                                            MediaMetadata.Builder()
                                                .setTitle("${verse.surah}:${verse.ayah}")
                                                .setDisplayTitle("Quran ${verse.surah}:${verse.ayah}")
                                                .setArtist("Al Quran Cloud")
                                                .setAlbumTitle("مەشخەڵ")
                                                .build()
                                        )
                                        .build()
                                    controller.setMediaItem(item)
                                    controller.prepare()
                                } else if (controller.playbackState == Player.STATE_ENDED) {
                                    controller.seekTo(0L)
                                }
                                controller.repeatMode = if (repeatAyah) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
                                audioPlaybackFailed = false
                                controller.play()
                            }
                        },
                        enabled = audioController != null,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            if (isThisAyahPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = null
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(if (isThisAyahPlaying) R.string.pause_selected_ayah else R.string.play_selected_ayah))
                    }
                    FilterChip(
                        selected = repeatAyah,
                        onClick = {
                            repeatAyah = !repeatAyah
                            if (playingMediaId == ayahMediaId) {
                                audioController?.repeatMode = if (repeatAyah) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
                            }
                        },
                        label = { Text(stringResource(R.string.repeat_selected_ayah)) },
                        leadingIcon = { Icon(Icons.Default.Repeat, contentDescription = null) }
                    )
                }
                if (audioController == null && !audioConnectionFailed) {
                    Text(
                        stringResource(R.string.audio_connecting),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (audioConnectionFailed) {
                    Text(
                        stringResource(R.string.audio_connection_failed),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                if (audioPlaybackFailed) {
                    Text(
                        stringResource(R.string.audio_playback_failed),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                if (showEnglishMeaning) {
                    val meaning = verse.translationEn
                    if (!meaning.isNullOrBlank()) {
                        Text(
                            meaning,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text(
                            stringResource(R.string.translation_unavailable),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledTonalButton(
                        onClick = { showEnglishMeaning = !showEnglishMeaning },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Translate, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(if (showEnglishMeaning) R.string.hide_translation else R.string.english_translation))
                    }
                    FilledTonalButton(
                        onClick = {
                            app.userDb.addBookmark(
                                "ayah",
                                "${verse.surah}:${verse.ayah}",
                                "${verse.surah}:${verse.ayah}"
                            )
                            selectedAyah = null
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.BookmarkBorder, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.bookmark))
                    }
                }

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val textToCopy = buildString {
                                append(verse.text)
                                if (showEnglishMeaning && !verse.translationEn.isNullOrBlank()) {
                                    append("\n\n")
                                    append(verse.translationEn)
                                }
                                append("\n${verse.surah}:${verse.ayah}")
                            }
                            val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE)
                                as android.content.ClipboardManager
                            clipboard.setPrimaryClip(
                                android.content.ClipData.newPlainText(
                                    "Quran ${verse.surah}:${verse.ayah}",
                                    textToCopy
                                )
                            )
                            selectedAyah = null
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.copy_ayah))
                    }
                    OutlinedButton(
                        onClick = {
                            val textToShare = buildString {
                                append(verse.text)
                                if (showEnglishMeaning && !verse.translationEn.isNullOrBlank()) {
                                    append("\n\n")
                                    append(verse.translationEn)
                                }
                                append("\n${verse.surah}:${verse.ayah}")
                            }
                            shareText(context, textToShare)
                            selectedAyah = null
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.share))
                    }
                }
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}


/**
 * Compatibility entry point for saved references, external search results and older deep links.
 * It resolves the reference into the actual page of the pinned Mushaf rather than opening the
 * legacy verse-card reader.
 */
@Composable
fun QuranReferencePageRoute(
    app: MasheqalApp,
    nav: NavHostController,
    surah: Int,
    ayah: Int
) {
    var page by remember(surah, ayah) { mutableIntStateOf(0) }
    var failed by remember(surah, ayah) { mutableStateOf(false) }
    var retry by remember(surah, ayah) { mutableIntStateOf(0) }

    LaunchedEffect(surah, ayah, retry) {
        page = 0
        failed = false
        val result = runCatching {
            val safeSurah = surah.coerceIn(1, 114)
            val surahVerses = app.quran.versesOfSurah(safeSurah)
            val targetAyah = surahVerses.firstOrNull { it.ayah == ayah } ?: surahVerses.firstOrNull()
                ?: error("Quran surah data is unavailable")
            app.quran.pageForVerse(safeSurah, targetAyah.ayah).coerceIn(1, 604)
        }
        result.onSuccess { page = it }.onFailure { failed = true }
    }

    if (page in 1..604) {
        QuranPageScreen(app, nav, page)
    } else {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (failed) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(24.dp)
                ) {
                    Text(
                        stringResource(R.string.mushaf_load_error),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Button(onClick = { retry++ }) {
                        Text(stringResource(R.string.retry))
                    }
                }
            } else {
                CircularProgressIndicator()
            }
        }
    }
}
