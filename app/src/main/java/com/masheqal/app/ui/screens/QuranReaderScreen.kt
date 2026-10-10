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
import com.masheqal.app.data.QuranStudyRepository
import com.masheqal.app.data.QuranEdition
import com.masheqal.app.data.QuranEditionRepository
import com.masheqal.app.data.FullSurahReciter
import com.masheqal.app.data.Mp3QuranReciterRepository
import kotlinx.coroutines.launch

private data class ReciterChoice(
    val id: String,
    val name: String,
    val fullSurahServer: String? = null,
    val availableSurahs: Set<Int> = emptySet()
)

// Edition identifiers follow the published Al Quran Cloud audio catalog.
private val quranTranslations = listOf(
    "en.sahih" to R.string.translation_sahih,
    "en.pickthall" to R.string.translation_pickthall,
    "en.yusufali" to R.string.translation_yusufali,
    "en.asad" to R.string.translation_asad,
    "en.hilali" to R.string.translation_hilali,
    "en.itani" to R.string.translation_itani
)
private val quranTafsirs = listOf(
    "ar.muyassar" to R.string.tafsir_muyassar,
    "ar.jalalayn" to R.string.tafsir_jalalayn
)

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
    ReciterChoice("ar.parhizgar", "Shahriar Parhizgar")
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
    var surahMetadata by remember { mutableStateOf<com.masheqal.app.data.SurahMeta?>(null) }
    var selected by remember { mutableStateOf<com.masheqal.app.data.QuranVerse?>(null) }
    var noteReference by remember { mutableStateOf("") }
    var showNote by remember { mutableStateOf(false) }
    var noteText by remember { mutableStateOf("") }
    var page by remember { mutableStateOf(1) }
    var currentAyah by remember(surah, initialAyah) { mutableIntStateOf(initialAyah) }
    var showReaderSettings by remember { mutableStateOf(false) }
    var isPlaying by remember { mutableStateOf(false) }
    var externalReciters by remember { mutableStateOf(emptyList<FullSurahReciter>()) }
    var reciterSearch by remember { mutableStateOf("") }
    var translationTexts by remember { mutableStateOf(emptyList<String>()) }
    var translationUnavailable by remember { mutableStateOf(false) }
    var tafsirTexts by remember { mutableStateOf(emptyList<String>()) }
    var availableTranslations by remember { mutableStateOf(emptyList<QuranEdition>()) }
    var availableTafsirs by remember { mutableStateOf(emptyList<QuranEdition>()) }
    var translationSearch by remember { mutableStateOf("") }
    var tafsirSearch by remember { mutableStateOf("") }
    val settings by app.settings.state.collectAsState(initial = com.masheqal.app.data.SettingsState())
    val listState = rememberLazyListState()
    val snackbar = remember { SnackbarHostState() }
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val bookmarkLabel = stringResource(R.string.bookmark)
    val noteLabel = stringResource(R.string.note)
    val translationUnavailableMessage = stringResource(R.string.translation_unavailable_offline)
    val fullSurahAudioLabel = stringResource(R.string.full_surah_audio_label)
    val audioErrorMessage = stringResource(R.string.audio_error)
    val missingSurahMessage = stringResource(R.string.reciter_missing_surah)
    val fullSurahNotice = stringResource(R.string.full_surah_audio_notice)
    val player = remember(context) { ExoPlayer.Builder(context).build() }
    val studyRepository = remember(context) { QuranStudyRepository(context) }

    LaunchedEffect(context) {
        externalReciters = runCatching { Mp3QuranReciterRepository(context).loadArabicReciters() }
            .getOrDefault(emptyList())
        val editionRepository = QuranEditionRepository(context)
        val catalogue = runCatching { editionRepository.loadCatalog() }.getOrDefault(emptyList())
        availableTranslations = (catalogue.filter { it.type == "translation" } + editionRepository.fallbackTranslations())
            .distinctBy { it.identifier }
        availableTafsirs = (catalogue.filter { it.type == "tafsir" } + editionRepository.fallbackTafsirs())
            .distinctBy { it.identifier }
    }
    val selectableReciters = quranReciters + externalReciters.map { reciter ->
        ReciterChoice(
            id = reciter.id,
            name = reciter.name + " — " + reciter.moshafName + " · " + fullSurahAudioLabel,
            fullSurahServer = reciter.server,
            availableSurahs = reciter.availableSurahs
        )
    }
    val filteredReciters = remember(selectableReciters, reciterSearch) {
        selectableReciters.filter { it.name.contains(reciterSearch.trim(), ignoreCase = true) }
    }
    val filteredTranslations = remember(availableTranslations, translationSearch) {
        availableTranslations.filter { edition ->
            translationSearch.isBlank() || edition.name.contains(translationSearch.trim(), ignoreCase = true) ||
                edition.englishName.contains(translationSearch.trim(), ignoreCase = true) ||
                edition.language.contains(translationSearch.trim(), ignoreCase = true) ||
                edition.identifier.contains(translationSearch.trim(), ignoreCase = true)
        }
    }
    val filteredTafsirs = remember(availableTafsirs, tafsirSearch) {
        availableTafsirs.filter { edition ->
            tafsirSearch.isBlank() || edition.name.contains(tafsirSearch.trim(), ignoreCase = true) ||
                edition.englishName.contains(tafsirSearch.trim(), ignoreCase = true) ||
                edition.language.contains(tafsirSearch.trim(), ignoreCase = true) ||
                edition.identifier.contains(tafsirSearch.trim(), ignoreCase = true)
        }
    }

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
                scope.launch { snackbar.showSnackbar(audioErrorMessage) }
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            player.release()
        }
    }

    fun playFrom(verse: com.masheqal.app.data.QuranVerse) {
        val fullSurahReciter = externalReciters.firstOrNull { it.id == settings.reciter }
        if (fullSurahReciter != null) {
            val fullSurahUrl = fullSurahReciter.audioUrl(surah)
            if (fullSurahUrl == null) {
                scope.launch { snackbar.showSnackbar(missingSurahMessage) }
                return
            }
            currentAyah = 1
            scope.launch {
                app.personal.setReading(surah, 1)
                snackbar.showSnackbar(fullSurahNotice)
            }
            player.setMediaItem(
                MediaItem.Builder()
                    .setMediaId(surah.toString() + ":1")
                    .setUri(fullSurahUrl)
                    .build()
            )
            player.prepare()
            player.play()
            return
        }

        val queue = verses.filter { it.ayah >= verse.ayah }.map { item ->
            MediaItem.Builder()
                .setMediaId(item.surah.toString() + ":" + item.ayah.toString())
                .setUri("https:" + "/" + "/cdn.islamic.network/quran/audio/128/" + settings.reciter + "/" + item.id + ".mp3")
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
        surahMetadata = app.quran.loadSurahs().firstOrNull { it.number == surah }
    }
    LaunchedEffect(surah, initialAyah) {
        page = app.quran.pageForVerse(surah, initialAyah)
    }
    LaunchedEffect(surah, settings.translationEdition, verses) {
        if (verses.isEmpty()) return@LaunchedEffect
        val result = runCatching {
            studyRepository.loadSurah(surah, settings.translationEdition).also { values ->
                require(values.size == verses.size) { "Translation verse count mismatch" }
            }
        }
        translationUnavailable = result.isFailure && settings.translationEdition != "en.sahih"
        translationTexts = result.getOrElse {
            if (settings.translationEdition == "en.sahih") verses.map { verse -> verse.translationEn.orEmpty() }
            else emptyList()
        }
    }
    LaunchedEffect(surah, settings.tafsirEdition, settings.showTafsir, verses) {
        if (!settings.showTafsir || verses.isEmpty()) {
            tafsirTexts = emptyList()
            return@LaunchedEffect
        }
        tafsirTexts = runCatching {
            studyRepository.loadSurah(surah, settings.tafsirEdition).also { values ->
                require(values.size == verses.size) { "Tafsir verse count mismatch" }
            }
        }.getOrElse { emptyList() }
    }
    LaunchedEffect(verses, initialAyah) {
        if (verses.isNotEmpty()) {
            val index = (initialAyah - 1).coerceIn(0, verses.lastIndex)
            listState.scrollToItem(index)
        }
    }

    val surahDisplayName = when (settings.language) {
        "en" -> surahMetadata?.nameEn ?: stringResource(R.string.surah_label) + " " + surah
        "ar" -> surahMetadata?.nameAr ?: stringResource(R.string.surah_label) + " " + surah
        else -> (surahMetadata?.nameAr ?: stringResource(R.string.surah_label)) + " " + surah
    }
    val secondarySurahName = when (settings.language) {
        "en" -> surahMetadata?.nameAr.orEmpty()
        else -> surahMetadata?.nameEn.orEmpty()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(surahDisplayName)
                        if (secondarySurahName.isNotBlank()) {
                            Text(secondarySurahName, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
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
            if (settings.showEnglishTranslation && translationUnavailable) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer
                        )
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(14.dp),
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CloudOff, null, tint = MaterialTheme.colorScheme.onTertiaryContainer)
                            Spacer(Modifier.width(10.dp))
                            Text(
                                translationUnavailableMessage,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }
                }
            }
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
                        val displayedTranslation = translationTexts.getOrNull(verse.ayah - 1)
                            ?.takeIf { it.isNotBlank() }
                            ?: if (settings.translationEdition == "en.sahih") verse.translationEn.orEmpty() else ""
                        if (settings.showEnglishTranslation && displayedTranslation.isNotBlank()) {
                            Spacer(Modifier.height(12.dp))
                            Text(
                                displayedTranslation,
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
                val displayedTranslation = translationTexts.getOrNull(verse.ayah - 1)
                    ?.takeIf { it.isNotBlank() }
                    ?: if (settings.translationEdition == "en.sahih") verse.translationEn.orEmpty() else ""
                if (settings.showEnglishTranslation && displayedTranslation.isNotBlank()) {
                    Spacer(Modifier.height(10.dp))
                    Text(displayedTranslation, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (settings.showTafsir) {
                    Spacer(Modifier.height(12.dp))
                    HorizontalDivider()
                    Text(stringResource(R.string.tafsir), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                    val tafsirText = tafsirTexts.getOrNull(verse.ayah - 1)
                    Text(
                        tafsirText?.takeIf { it.isNotBlank() } ?: stringResource(R.string.tafsir_loading_or_offline),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
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
                    OutlinedTextField(
                        value = reciterSearch,
                        onValueChange = { reciterSearch = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        placeholder = { Text(stringResource(R.string.reciter_search_hint)) },
                        leadingIcon = { Icon(Icons.Default.Search, null) },
                        trailingIcon = {
                            if (reciterSearch.isNotEmpty()) IconButton(onClick = { reciterSearch = "" }) {
                                Icon(Icons.Default.Close, stringResource(R.string.clear))
                            }
                        }
                    )
                    Text(
                        stringResource(R.string.quran_reciter_sources_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    filteredReciters.forEach { reciter ->
                        Row(
                            Modifier.fillMaxWidth().clickable {
                                scope.launch { app.settings.setReciter(reciter.id) }
                                player.stop()
                                reciterSearch = ""
                            },
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = settings.reciter == reciter.id,
                                onClick = {
                                    scope.launch { app.settings.setReciter(reciter.id) }
                                    player.stop()
                                    reciterSearch = ""
                                }
                            )
                            Text(reciter.name, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    HorizontalDivider(Modifier.padding(vertical = 8.dp))
                    Text(stringResource(R.string.select_translation), style = MaterialTheme.typography.titleSmall)
                    OutlinedTextField(
                        value = translationSearch,
                        onValueChange = { translationSearch = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        placeholder = { Text(stringResource(R.string.translation_search_hint)) },
                        leadingIcon = { Icon(Icons.Default.Search, null) },
                        trailingIcon = {
                            if (translationSearch.isNotEmpty()) IconButton(onClick = { translationSearch = "" }) {
                                Icon(Icons.Default.Close, stringResource(R.string.clear))
                            }
                        }
                    )
                    filteredTranslations.forEach { edition ->
                        Row(Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                            RadioButton(
                                selected = settings.translationEdition == edition.identifier,
                                onClick = { scope.launch { app.settings.setTranslationEdition(edition.identifier) } }
                            )
                            Column(Modifier.weight(1f)) {
                                Text(edition.englishName, style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    "${edition.language.uppercase()} · ${edition.name}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Text(stringResource(R.string.english_translation), Modifier.weight(1f))
                        Switch(checked = settings.showEnglishTranslation, onCheckedChange = { enabled ->
                            scope.launch { app.settings.setShowEnglishTranslation(enabled) }
                        })
                    }
                    HorizontalDivider(Modifier.padding(vertical = 8.dp))
                    Text(stringResource(R.string.select_tafsir), style = MaterialTheme.typography.titleSmall)
                    OutlinedTextField(
                        value = tafsirSearch,
                        onValueChange = { tafsirSearch = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        placeholder = { Text(stringResource(R.string.tafsir_search_hint)) },
                        leadingIcon = { Icon(Icons.Default.Search, null) },
                        trailingIcon = {
                            if (tafsirSearch.isNotEmpty()) IconButton(onClick = { tafsirSearch = "" }) {
                                Icon(Icons.Default.Close, stringResource(R.string.clear))
                            }
                        }
                    )
                    filteredTafsirs.forEach { edition ->
                        Row(Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                            RadioButton(
                                selected = settings.tafsirEdition == edition.identifier,
                                onClick = { scope.launch { app.settings.setTafsirEdition(edition.identifier) } }
                            )
                            Column(Modifier.weight(1f)) {
                                Text(edition.englishName, style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    "${edition.language.uppercase()} · ${edition.name}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Text(stringResource(R.string.show_tafsir), Modifier.weight(1f))
                        Switch(checked = settings.showTafsir, onCheckedChange = { enabled ->
                            scope.launch { app.settings.setShowTafsir(enabled) }
                        })
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(stringResource(R.string.audio_source_notice), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    Text(stringResource(R.string.quran_study_source_note), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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