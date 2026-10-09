
package com.masheqal.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import android.content.ComponentName
import android.content.Context
import android.net.Uri
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.masheqal.app.data.SurahMeta
import com.masheqal.app.data.OfflineAudioDownloads
import com.masheqal.app.data.OfflineAudioStatus
import com.masheqal.app.services.QuranPlaybackService
import kotlinx.coroutines.delay
import java.util.Locale
import androidx.navigation.NavHostController
import com.masheqal.app.MasheqalApp
import com.masheqal.app.R
import kotlinx.coroutines.launch

@Composable
fun QuranScreen(app: MasheqalApp, nav: NavHostController) {
    var surahs by remember { mutableStateOf(emptyList<com.masheqal.app.data.SurahMeta>()) }
    var query by rememberSaveable { mutableStateOf("") }
    var pageDialog by remember { mutableStateOf(false) }
    var juzDialog by remember { mutableStateOf(false) }
    var pageText by rememberSaveable { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        surahs = app.quran.loadSurahs()
    }

    val filtered = remember(query, surahs) {
        val q = query.trim()
        if (q.isBlank()) {
            surahs
        } else {
            surahs.filter {
                it.nameAr.contains(q) ||
                    it.nameEn.contains(q, true) ||
                    it.number.toString() == q
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 16.dp, bottom = 28.dp)
    ) {
        item {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.quran), style = MaterialTheme.typography.headlineMedium)
                    Spacer(Modifier.height(3.dp))
                    Text(
                        "114 • 6236",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    FilledTonalIconButton(onClick = { nav.navigate("audio") }) {
                        Icon(Icons.Default.Headphones, stringResource(R.string.audio_player_title))
                    }
                    FilledTonalIconButton(onClick = { nav.navigate("search") }) {
                        Icon(Icons.Default.Search, stringResource(R.string.search))
                    }
                }
            }
        }

        item {
            Spacer(Modifier.height(14.dp))
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                shape = RoundedCornerShape(18.dp),
                singleLine = true,
                placeholder = { Text(stringResource(R.string.search_hint)) },
                leadingIcon = { Icon(Icons.Default.Search, null) },
            )
        }

        item {
            Spacer(Modifier.height(12.dp))
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { pageDialog = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.AutoStories, null)
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.page_view))
                }
                OutlinedButton(
                    onClick = { juzDialog = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Bookmark, null)
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.juz))
                }
            }
        }

        item {
            SectionTitle(
                stringResource(R.string.reading),
                stringResource(R.string.saved)
            ) {
                nav.navigate("saved")
            }
        }

        items(filtered, key = { it.number }) { s ->
            Card(
                onClick = { nav.navigate("quran/surah/${s.number}") },
                modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
                shape = RoundedCornerShape(22.dp)
            ) {
                Row(
                    Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        Modifier.size(48.dp),
                        shape = RoundedCornerShape(15.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                s.number.toString(),
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(s.nameAr, style = MaterialTheme.typography.titleLarge)
                        Text(
                            s.nameEn,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "${s.ayahCount} • ${s.revelation}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                    Icon(
                        Icons.Default.AutoStories,
                        null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }

    if (pageDialog) {
        AlertDialog(
            onDismissRequest = { pageDialog = false },
            title = { Text(stringResource(R.string.page_view)) },
            text = {
                OutlinedTextField(
                    value = pageText,
                    onValueChange = { pageText = it.filter(Char::isDigit) },
                    label = { Text(stringResource(R.string.page)) },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val page = pageText.toIntOrNull()
                        if (page != null && page in 1..604) {
                            pageDialog = false
                            pageText = ""
                            nav.navigate("quran/page/$page")
                        }
                    }
                ) {
                    Text(stringResource(R.string.open_quran))
                }
            },
            dismissButton = {
                TextButton(onClick = { pageDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (juzDialog) {
        AlertDialog(
            onDismissRequest = { juzDialog = false },
            title = { Text(stringResource(R.string.juz)) },
            text = {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.heightIn(max = 360.dp)
                ) {
                    items((1..30).toList()) { juz ->
                        ListItem(
                            headlineContent = {
                                Text(
                                    "${stringResource(R.string.juz)} $juz",
                                    fontWeight = FontWeight.SemiBold
                                )
                            },
                            leadingContent = {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Text(
                                        juz.toString(),
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    scope.launch {
                                        val range = app.quran.loadJuzs().firstOrNull { it.number == juz }
                                        val firstVerse = range?.let { target ->
                                            app.quran.loadVerses().firstOrNull { it.id == target.firstGlobalAyah }
                                        }
                                        val targetPage = firstVerse?.let {
                                            app.quran.pageForVerse(it.surah, it.ayah)
                                        } ?: 1
                                        juzDialog = false
                                        nav.navigate("quran/page/$targetPage")
                                    }
                                },
                            colors = ListItemDefaults.colors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        )
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { juzDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}


private data class AudioEdition(val id: String, val nameResource: Int)

/** Uses Al Quran Cloud's documented surah-audio CDN; this app streams and does not redistribute files. */
object QuranAudioCatalog {
    private val editions = mapOf(
        "ar.alafasy" to 128,
        "ar.husary" to 128,
        "ar.minshawi" to 128,
        "ar.sudais" to 192,
        "ar.shuraim" to 128,
        "ar.abdulbasit" to 192,
        "ar.ajamy" to 128,
        "ar.hudhaify" to 128
    )

    fun surahUrl(surah: Int, edition: String): String {
        require(surah in 1..114) { "Surah number must be between 1 and 114" }
        val bitrate = editions[edition] ?: throw IllegalArgumentException("Unsupported recitation edition")
        return "https://cdn.islamic.network/quran/audio-surah/$bitrate/$edition/$surah.mp3"
    }
}

@UnstableApi
private fun makeAudioQueue(
    context: Context,
    surahs: List<SurahMeta>,
    edition: String,
    reciter: String
): List<MediaItem> = surahs.map { surah ->
        val local = OfflineAudioDownloads.readyFileOrNull(context, edition, surah.number)
        val uri = local?.let(Uri::fromFile) ?: Uri.parse(QuranAudioCatalog.surahUrl(surah.number, edition))
        MediaItem.Builder()
            .setMediaId("surah-${surah.number}")
            .setUri(uri)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(surah.nameAr)
                    .setDisplayTitle(surah.nameEn)
                    .setArtist(reciter)
                    .setAlbumTitle("مەشخەڵ · Al Quran Cloud / Islamic Network")
                    .build()
            )
            .build()
    }

private fun audioTime(ms: Long): String {
    val seconds = ms.coerceAtLeast(0L) / 1000L
    return "%02d:%02d".format(Locale.ROOT, seconds / 60L, seconds % 60L)
}

@UnstableApi
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuranAudioScreen(app: MasheqalApp, nav: NavHostController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var downloadRefresh by remember { mutableIntStateOf(0) }
    var queueRefresh by remember { mutableIntStateOf(0) }
    var selectedDownload by remember { mutableStateOf<Pair<String, Int>?>(null) }
    var showReciterMenu by remember { mutableStateOf(false) }
    var downloadError by remember { mutableStateOf(false) }
    val editions = listOf(
        AudioEdition("ar.alafasy", R.string.reciter_alafasy),
        AudioEdition("ar.husary", R.string.reciter_husary),
        AudioEdition("ar.minshawi", R.string.reciter_minshawi),
        AudioEdition("ar.sudais", R.string.reciter_sudais),
        AudioEdition("ar.shuraim", R.string.reciter_shuraim),
        AudioEdition("ar.abdulbasit", R.string.reciter_abdulbasit),
        AudioEdition("ar.ajamy", R.string.reciter_ajamy),
        AudioEdition("ar.hudhaify", R.string.reciter_hudhaify)
    )
    var selectedEdition by rememberSaveable {
        mutableStateOf(
            context.getSharedPreferences("masheqal_audio_preferences", Context.MODE_PRIVATE)
                .getString("reciter", "ar.alafasy") ?: "ar.alafasy"
        )
    }
    val activeEdition = editions.firstOrNull { it.id == selectedEdition } ?: editions.first()
    val activeReciterName = stringResource(activeEdition.nameResource)
    var surahs by remember { mutableStateOf(emptyList<SurahMeta>()) }
    var reload by remember { mutableIntStateOf(0) }
    var loadFailed by remember { mutableStateOf(false) }
    var controller by remember { mutableStateOf<MediaController?>(null) }
    var connectionFailed by remember { mutableStateOf(false) }
    var reconnect by remember { mutableIntStateOf(0) }
    var playbackFailed by remember { mutableStateOf(false) }
    var currentSurah by remember { mutableIntStateOf(0) }
    var isPlaying by remember { mutableStateOf(false) }
    var positionMs by remember { mutableStateOf(0L) }
    var durationMs by remember { mutableStateOf(0L) }

    LaunchedEffect(app, reload) {
        runCatching { app.quran.loadSurahs() }
            .onSuccess { surahs = it; loadFailed = false }
            .onFailure { loadFailed = true }
    }

    DisposableEffect(context, reconnect) {
        var active = true
        val token = SessionToken(context, ComponentName(context, QuranPlaybackService::class.java))
        val future = MediaController.Builder(context, token).buildAsync()
        future.addListener({
            runCatching { future.get() }
                .onSuccess { if (active) { controller = it; connectionFailed = false } }
                .onFailure { if (active) connectionFailed = true }
        }, ContextCompat.getMainExecutor(context))
        onDispose {
            active = false
            MediaController.releaseFuture(future)
            controller = null
        }
    }

    val queue = remember(surahs, activeEdition, activeReciterName, queueRefresh) {
        makeAudioQueue(context, surahs, activeEdition.id, activeReciterName)
    }

    fun playSurah(number: Int) {
        val player = controller
        val index = surahs.indexOfFirst { it.number == number }
        if (player == null || index < 0 || queue.isEmpty()) {
            playbackFailed = true
            return
        }
        player.setMediaItems(queue, index, 0L)
        player.prepare()
        player.play()
        currentSurah = number
        playbackFailed = false
    }

    fun selectEdition(edition: AudioEdition, reciterName: String) {
        if (edition.id == selectedEdition) return
        val player = controller
        val oldNumber = currentSurah
        val oldPosition = player?.currentPosition?.coerceAtLeast(0L) ?: 0L
        val wasPlaying = player?.isPlaying == true
        selectedEdition = edition.id
        context.getSharedPreferences("masheqal_audio_preferences", Context.MODE_PRIVATE)
            .edit()
            .putString("reciter", edition.id)
            .apply()
        if (player != null && oldNumber in 1..114 && surahs.isNotEmpty()) {
            val newQueue = makeAudioQueue(context, surahs, edition.id, reciterName)
            val index = surahs.indexOfFirst { it.number == oldNumber }.coerceAtLeast(0)
            player.setMediaItems(newQueue, index, oldPosition)
            player.prepare()
            if (wasPlaying) player.play()
        }
    }

    DisposableEffect(controller) {
        val player = controller
        if (player == null) {
            onDispose { }
        } else {
            val listener = object : Player.Listener {
                override fun onMediaItemTransition(item: MediaItem?, reason: Int) {
                    currentSurah = item?.mediaId?.removePrefix("surah-")?.toIntOrNull() ?: 0
                    positionMs = player.currentPosition.coerceAtLeast(0L)
                    durationMs = player.duration.coerceAtLeast(0L)
                    playbackFailed = false
                }
                override fun onIsPlayingChanged(playing: Boolean) {
                    isPlaying = playing
                    positionMs = player.currentPosition.coerceAtLeast(0L)
                }
                override fun onPlaybackStateChanged(state: Int) {
                    durationMs = player.duration.coerceAtLeast(0L)
                }
                override fun onPlayerError(error: PlaybackException) {
                    playbackFailed = true
                    isPlaying = false
                }
            }
            player.addListener(listener)
            currentSurah = player.currentMediaItem?.mediaId?.removePrefix("surah-")?.toIntOrNull() ?: currentSurah
            isPlaying = player.isPlaying
            onDispose { player.removeListener(listener) }
        }
    }

    LaunchedEffect(controller, isPlaying) {
        val player = controller ?: return@LaunchedEffect
        while (isPlaying) {
            positionMs = player.currentPosition.coerceAtLeast(0L)
            durationMs = player.duration.coerceAtLeast(0L)
            delay(500L)
        }
    }

    LaunchedEffect(selectedDownload) {
        val target = selectedDownload ?: return@LaunchedEffect
        var missingChecks = 0
        while (true) {
            delay(1_000L)
            downloadRefresh++
            when (OfflineAudioDownloads.status(context, target.first, target.second)) {
                OfflineAudioStatus.DOWNLOADING -> missingChecks = 0
                OfflineAudioStatus.READY -> {
                    if (selectedEdition == target.first) queueRefresh++
                    selectedDownload = null
                    break
                }
                OfflineAudioStatus.NOT_DOWNLOADED, OfflineAudioStatus.FAILED -> {
                    // DownloadManager can take a moment to publish the new row after enqueue.
                    missingChecks++
                    if (missingChecks >= 5) {
                        downloadError = true
                        selectedDownload = null
                        break
                    }
                }
            }
        }
    }

    val currentMeta = surahs.firstOrNull { it.number == currentSurah }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.audio_player_title)) },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(Modifier.fillMaxWidth().padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                modifier = Modifier.size(54.dp),
                                shape = RoundedCornerShape(18.dp),
                                color = MaterialTheme.colorScheme.primary
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Headphones, null,
                                        tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(28.dp))
                                }
                            }
                            Spacer(Modifier.width(14.dp))
                            Column(Modifier.weight(1f)) {
                                Text(currentMeta?.nameAr ?: stringResource(R.string.quran),
                                    style = MaterialTheme.typography.titleLarge)
                                Text(currentMeta?.nameEn ?: stringResource(R.string.audio_choose_surah),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    if (currentSurah in 1..114) "$currentSurah / 114" else stringResource(R.string.audio_ready),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        Spacer(Modifier.height(14.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                stringResource(R.string.audio_reciter),
                                style = MaterialTheme.typography.labelLarge
                            )
                            Box {
                                OutlinedButton(onClick = { showReciterMenu = true }) {
                                    Text(activeReciterName, maxLines = 1)
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                }
                                DropdownMenu(
                                    expanded = showReciterMenu,
                                    onDismissRequest = { showReciterMenu = false }
                                ) {
                                    editions.forEach { edition ->
                                        val editionName = stringResource(edition.nameResource)
                                        DropdownMenuItem(
                                            text = { Text(editionName) },
                                            leadingIcon = {
                                                if (edition.id == selectedEdition) {
                                                    Icon(Icons.Default.CheckCircle, contentDescription = null)
                                                }
                                            },
                                            onClick = {
                                                selectEdition(edition, editionName)
                                                showReciterMenu = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                        Slider(
                            value = if (durationMs > 0L) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f,
                            onValueChange = { if (durationMs > 0L) positionMs = (it * durationMs).toLong() },
                            onValueChangeFinished = { if (durationMs > 0L) controller?.seekTo(positionMs) },
                            enabled = durationMs > 0L
                        )
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(audioTime(positionMs), style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(audioTime(durationMs), style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Spacer(Modifier.height(6.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                enabled = controller != null && currentSurah > 0,
                                onClick = {
                                    val player = controller
                                    if (player != null && player.currentMediaItemIndex >= 0) {
                                        if (player.currentPosition > 5000L) player.seekTo(0L)
                                        else if (player.hasPreviousMediaItem()) player.seekToPreviousMediaItem()
                                    }
                                }
                            ) { Icon(Icons.Default.SkipPrevious, stringResource(R.string.audio_previous_surah)) }
                            Spacer(Modifier.width(18.dp))
                            FilledIconButton(
                                modifier = Modifier.size(62.dp),
                                enabled = controller != null && surahs.isNotEmpty(),
                                onClick = {
                                    val player = controller
                                    if (player == null) connectionFailed = true
                                    else if (currentSurah == 0) playSurah(1)
                                    else if (player.isPlaying) player.pause() else player.play()
                                }
                            ) {
                                Icon(
                                    if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    stringResource(if (isPlaying) R.string.pause else R.string.audio_play),
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                            Spacer(Modifier.width(18.dp))
                            IconButton(
                                enabled = controller != null && surahs.isNotEmpty(),
                                onClick = {
                                    val player = controller
                                    if (player != null && player.hasNextMediaItem()) player.seekToNextMediaItem()
                                    else if (currentSurah == 0) playSurah(1)
                                }
                            ) { Icon(Icons.Default.SkipNext, stringResource(R.string.audio_next_surah)) }
                        }
                        Spacer(Modifier.height(10.dp))
                        Text(stringResource(R.string.audio_source_notice),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (downloadError) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                stringResource(R.string.audio_download_failed),
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                            TextButton(onClick = { downloadError = false }) {
                                Text(stringResource(R.string.done))
                            }
                        }
                        if (connectionFailed || playbackFailed || loadFailed) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                stringResource(if (loadFailed) R.string.audio_load_error else R.string.audio_connection_error),
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            TextButton(onClick = {
                                when {
                                    loadFailed -> reload++
                                    connectionFailed -> { connectionFailed = false; reconnect++ }
                                    currentSurah > 0 -> playSurah(currentSurah)
                                }
                            }) { Text(stringResource(R.string.audio_retry)) }
                        } else if (controller == null) {
                            Spacer(Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                                Spacer(Modifier.width(8.dp))
                                Text(stringResource(R.string.audio_connecting), style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
            item {
                Text(stringResource(R.string.audio_reciter), style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    editions.forEach { edition ->
                        val editionName = stringResource(edition.nameResource)
                        FilterChip(
                            selected = edition.id == selectedEdition,
                            onClick = { selectEdition(edition, editionName) },
                            label = { Text(editionName) },
                            leadingIcon = { if (edition.id == selectedEdition) Icon(Icons.Default.GraphicEq, null) }
                        )
                    }
                }
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(stringResource(R.string.audio_choose_surah), style = MaterialTheme.typography.titleMedium)
                    Text("${surahs.size} / 114", style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (loadFailed) {
                item { Text(stringResource(R.string.audio_load_error), color = MaterialTheme.colorScheme.error) }
            } else {
                items(surahs, key = { it.number }) { surah ->
                    val playingThis = currentSurah == surah.number && isPlaying
                    val offlineStatus = remember(downloadRefresh, activeEdition.id, surah.number) {
                        OfflineAudioDownloads.status(context, activeEdition.id, surah.number)
                    }
                    val downloadDescription = when (offlineStatus) {
                        OfflineAudioStatus.READY -> stringResource(R.string.audio_downloaded)
                        OfflineAudioStatus.DOWNLOADING -> stringResource(R.string.audio_downloading)
                        OfflineAudioStatus.FAILED -> stringResource(R.string.audio_download_failed)
                        OfflineAudioStatus.NOT_DOWNLOADED -> stringResource(R.string.audio_download)
                    }
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (currentSurah == surah.number)
                                MaterialTheme.colorScheme.secondaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Row(
                            Modifier.fillMaxWidth().clickable { playSurah(surah.number) }
                                .padding(start = 14.dp, top = 8.dp, bottom = 8.dp, end = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primaryContainer) {
                                Text(surah.number.toString(),
                                    modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    style = MaterialTheme.typography.labelLarge)
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(surah.nameAr, style = MaterialTheme.typography.titleMedium)
                                Text(surah.nameEn, style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            IconButton(
                                enabled = offlineStatus != OfflineAudioStatus.DOWNLOADING &&
                                    offlineStatus != OfflineAudioStatus.READY,
                                onClick = {
                                    downloadError = false
                                    runCatching {
                                        OfflineAudioDownloads.enqueue(
                                            context, activeEdition.id, surah.number,
                                            surah.nameAr, activeReciterName
                                        )
                                    }.onSuccess {
                                        downloadRefresh++
                                        selectedDownload = activeEdition.id to surah.number
                                    }.onFailure {
                                        downloadError = true
                                    }
                                }
                            ) {
                                when (offlineStatus) {
                                    OfflineAudioStatus.READY ->
                                        Icon(Icons.Default.CheckCircle, contentDescription = downloadDescription,
                                            tint = MaterialTheme.colorScheme.primary)
                                    OfflineAudioStatus.DOWNLOADING ->
                                        CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                                    else ->
                                        Icon(Icons.Default.Download, contentDescription = downloadDescription)
                                }
                            }
                            Icon(
                                if (playingThis) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = stringResource(R.string.audio_play_surah),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}
