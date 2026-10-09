package com.masheqal.app.ui.screens

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
import androidx.navigation.NavHostController
import com.masheqal.app.MasheqalApp
import com.masheqal.app.R
import com.masheqal.app.data.QuranVerse
import kotlinx.coroutines.launch

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
