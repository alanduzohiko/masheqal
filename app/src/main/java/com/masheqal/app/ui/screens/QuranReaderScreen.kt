package com.masheqal.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.masheqal.app.MasheqalApp
import com.masheqal.app.R
import com.masheqal.app.data.SettingsState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuranReaderScreen(
    app: MasheqalApp,
    nav: NavHostController,
    surah: Int,
    initialAyah: Int
) {
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val settings by app.settings.state.collectAsState(initial = SettingsState())
    var verses by remember { mutableStateOf(emptyList<com.masheqal.app.data.QuranVerse>()) }
    var selected by remember { mutableStateOf<com.masheqal.app.data.QuranVerse?>(null) }
    var noteReference by remember { mutableStateOf("") }
    var showNote by remember { mutableStateOf(false) }
    var noteText by remember { mutableStateOf("") }
    var page by remember { mutableStateOf(1) }
    val listState = rememberLazyListState()
    val snackbar = remember { SnackbarHostState() }
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val bookmarkLabel = stringResource(R.string.bookmark)
    val noteLabel = stringResource(R.string.note)

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
                        Icon(
                            if (isRtl) Icons.Default.ArrowForward else Icons.Default.ArrowBack,
                            contentDescription = null
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { nav.navigate("quran/page/$page") }) {
                        Icon(Icons.Default.MenuBook, stringResource(R.string.page_view))
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
                        val visibleTranslation = verse.translationFor(settings.language)
                        if (!visibleTranslation.isNullOrBlank()) {
                            Spacer(Modifier.height(12.dp))
                            Text(
                                visibleTranslation,
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
                val selectedTranslation = verse.translationFor(settings.language)
                if (!selectedTranslation.isNullOrBlank()) {
                    Spacer(Modifier.height(12.dp))
                    Text(selectedTranslation, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.height(14.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
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