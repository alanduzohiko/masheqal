package com.masheqal.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.masheqal.app.MasheqalApp
import com.masheqal.app.R
import kotlinx.coroutines.launch

private data class ReciterChoice(val id: String, val name: String)

// Edition identifiers follow the published Al Quran Cloud audio catalog.
private val quranReciters = listOf(
    ReciterChoice("ar.alafasy", "Mishary Rashid Alafasy"),
    ReciterChoice("ar.sudais", "Abdul Rahman Al-Sudais"),
    ReciterChoice("ar.shuraim", "Saud Al-Shuraim"),
    ReciterChoice("ar.husary", "Mahmoud Khalil Al-Husary"),
    ReciterChoice("ar.minshawi", "Mohamed Siddiq Al-Minshawi"),
    ReciterChoice("ar.minshawimujawwad", "Al-Minshawi — Mujawwad"),
    ReciterChoice("ar.abdulbasit", "Abdul Basit Abdul Samad"),
    ReciterChoice("ar.abdulbasitmujawwad", "Abdul Basit — Mujawwad"),
    ReciterChoice("ar.ajamy", "Ahmed Al-Ajamy"),
    ReciterChoice("ar.muhammadayoub", "Muhammad Ayyoub"),
    ReciterChoice("ar.hudhaify", "Ali Al-Hudhaify"),
    ReciterChoice("ar.muhammadjibreel", "Muhammad Jibreel"),
    ReciterChoice("ar.parhizgar", "Mahmoud Khalil Al-Husary — Muallim")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuranReaderScreen(
    app: MasheqalApp,
    nav: NavHostController,
    surah: Int,
    initialAyah: Int
) {
    var verses by remember { mutableStateOf(emptyList<com.masheqal.app.data.QuranVerse>()) }
    var selected by remember { mutableStateOf<com.masheqal.app.data.QuranVerse?>(null) }
    var noteReference by remember { mutableStateOf("") }
    var showNote by remember { mutableStateOf(false) }
    var noteText by remember { mutableStateOf("") }
    var page by remember { mutableStateOf(1) }
    var currentAyah by remember(surah, initialAyah) { mutableIntStateOf(initialAyah) }
    var showReaderSettings by remember { mutableStateOf(false) }
    var isPlaying by remember { mutableStateOf(false) }
    val settings by app.settings.state.collectAsState(initial = com.masheqal.app.data.SettingsState())
    val listState = rememberLazyListState()
    val snackbar = remember { SnackbarHostState() }
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val bookmarkLabel = stringResource(R.string.bookmark)
    val noteLabel = stringResource(R.string.note)
    val player = remember(context) { ExoPlayer.Builder(context).build() }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) { isPlaying = playing }
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                val nextAyah = mediaItem?.mediaId?.substringAfter(":")?.toIntOrNull()
                if (nextAyah != null) {
                    currentAyah = nextAyah
                    scope.launch { app.personal.setReading(surah, nextAyah) }
                }
            }
            override fun onPlayerError(error: PlaybackException) {
                scope.launch { snackbar.showSnackbar(context.getString(R.string.audio_error)) }
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            player.release()
        }
    }

    fun playFrom(verse: com.masheqal.app.data.QuranVerse) {
        val queue = verses.filter { it.ayah >= verse.ayah }.map { item ->
            MediaItem.Builder()
                .setMediaId(item.surah.toString() + ":" + item.ayah.toString())
                .setUri("https://cdn.islamic.network/quran/audio/128/" + settings.reciter + "/" + item.id + ".mp3")
                .build()
        }
        if (queue.isEmpty()) return
        currentAyah = verse.ayah
        scope.launch { app.personal.setReading(surah, verse.ayah) }
        player.setMediaItems(queue)
        player.prepare()
        player.play()
    }

    LaunchedEffect(surah) {
        verses = app.quran.versesOfSurah(surah)
    }
    LaunchedEffect(surah, initialAyah) {
        page = app.quran.pageForVerse(surah, initialAyah)
    }
    LaunchedEffect(verses, initialAyah) {
        if (verses.isNotEmpty()) {
            val index = (initialAyah - 1).coerceIn(0, verses.lastIndex)
            listState.scrollToItem(index)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("سورە $surah")
                        Text(
                            "${verses.size} ${stringResource(R.string.ayah)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, null)
                    }
                },
                actions = {
                    IconButton(onClick = { nav.navigate("quran/page/$page") }) {
                        Icon(Icons.Default.MenuBook, stringResource(R.string.page_view))
                    }
                    IconButton(onClick = {
                        if (isPlaying) player.pause() else {
                            val target = verses.firstOrNull { it.ayah == currentAyah } ?: verses.firstOrNull()
                            target?.let(::playFrom)
                        }
                    }) {
                        Icon(
                            if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            stringResource(if (isPlaying) R.string.pause_audio else R.string.play_current_ayah)
                        )
                    }
                    IconButton(onClick = { showReaderSettings = true }) {
                        Icon(Icons.Default.Tune, stringResource(R.string.reader_settings))
                    }
                    IconButton(onClick = { nav.navigate("search") }) {
                        Icon(Icons.Default.Search, stringResource(R.string.search))
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(verses, key = { it.id }) { verse ->
                Card(
                    onClick = {
                        selected = verse
                        currentAyah = verse.ayah
                        scope.launch { app.personal.setReading(surah, verse.ayah) }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(Modifier.padding(18.dp)) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "$surah:${verse.ayah}",
                                color = MaterialTheme.colorScheme.secondary,
                                fontWeight = FontWeight.SemiBold
                            )
                            Icon(Icons.Default.MoreHoriz, contentDescription = null)
                        }
                        Spacer(Modifier.height(10.dp))
                        QuranText(verse.text, size = 27f)
                        if (settings.showEnglishTranslation && !verse.translationEn.isNullOrBlank()) {
                            Spacer(Modifier.height(12.dp))
                            Text(
                                verse.translationEn.orEmpty(),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }

    selected?.let { verse ->
        ModalBottomSheet(onDismissRequest = { selected = null }) {
            Column(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp)
            ) {
                Text(
                    "$surah:${verse.ayah}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
                Spacer(Modifier.height(8.dp))
                QuranText(verse.text, size = 23f)
                if (settings.showEnglishTranslation && !verse.translationEn.isNullOrBlank()) {
                    Spacer(Modifier.height(10.dp))
                    Text(verse.translationEn.orEmpty(), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.height(14.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    IconButton(onClick = {
                        if (isPlaying) player.pause() else playFrom(verse)
                    }) {
                        Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, stringResource(if (isPlaying) R.string.pause_audio else R.string.play_current_ayah))
                    }
                    IconButton(onClick = {
                        app.userDb.addBookmark("ayah", "$surah:${verse.ayah}", "$surah:${verse.ayah}")
                        selected = null
                        scope.launch { snackbar.showSnackbar("$bookmarkLabel: $surah:${verse.ayah}") }
                    }) {
                        Icon(Icons.Default.BookmarkBorder, bookmarkLabel)
                    }
                    IconButton(onClick = {
                        noteReference = "$surah:${verse.ayah}"
                        noteText = ""
                        selected = null
                        showNote = true
                    }) {
                        Icon(Icons.Default.Notes, noteLabel)
                    }
                    IconButton(onClick = {
                        shareText(context, "${verse.text}\n\n${verse.translationEn.orEmpty()}\n$surah:${verse.ayah}")
                        selected = null
                    }) {
                        Icon(Icons.Default.Share, stringResource(R.string.share))
                    }
                    IconButton(onClick = {
                        val uri = ShareCardUtils.createVerseCard(
                            context,
                            verse.text,
                            verse.translationEn.orEmpty(),
                            "$surah:${verse.ayah}"
                        )
                        ShareCardUtils.shareImage(context, uri)
                        selected = null
                    }) {
                        Icon(Icons.Default.Image, stringResource(R.string.share_image))
                    }
                }
                Spacer(Modifier.height(14.dp))
            }
        }
    }

    if (showReaderSettings) {
        AlertDialog(
            onDismissRequest = { showReaderSettings = false },
            title = { Text(stringResource(R.string.reader_settings)) },
            text = {
                Column(Modifier.fillMaxWidth().heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
                    Text(stringResource(R.string.select_reciter), style = MaterialTheme.typography.titleSmall)
                    quranReciters.forEach { reciter ->
                        Row(
                            Modifier.fillMaxWidth().clickable {
                                scope.launch { app.settings.setReciter(reciter.id) }
                                player.stop()
                            },
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = settings.reciter == reciter.id,
                                onClick = {
                                    scope.launch { app.settings.setReciter(reciter.id) }
                                    player.stop()
                                }
                            )
                            Text(reciter.name, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    HorizontalDivider(Modifier.padding(vertical = 8.dp))
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Text(stringResource(R.string.english_translation), Modifier.weight(1f))
                        Switch(checked = settings.showEnglishTranslation, onCheckedChange = { enabled ->
                            scope.launch { app.settings.setShowEnglishTranslation(enabled) }
                        })
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(stringResource(R.string.audio_source_notice), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    Text(stringResource(R.string.tafsir_unavailable), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            confirmButton = { TextButton(onClick = { showReaderSettings = false }) { Text(stringResource(R.string.done)) } },
            dismissButton = { TextButton(onClick = { showReaderSettings = false; nav.navigate("content") }) { Text(stringResource(R.string.content_center)) } }
        )
    }

    if (showNote) {
        AlertDialog(
            onDismissRequest = { showNote = false },
            title = { Text(noteLabel) },
            text = {
                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 4
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (noteText.isNotBlank()) {
                        app.userDb.addNote(noteReference, noteText)
                        scope.launch { snackbar.showSnackbar("$noteLabel: $noteReference") }
                    }
                    noteText = ""
                    noteReference = ""
                    showNote = false
                }) {
                    Text(stringResource(R.string.done))
                }
            },
            dismissButton = {
                TextButton(onClick = { showNote = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}