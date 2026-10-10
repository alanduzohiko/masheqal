package com.masheqal.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.masheqal.app.MasheqalApp
import com.masheqal.app.R
import com.masheqal.app.data.QuranStudyRepository
import com.masheqal.app.data.QuranTajweedRepository
import com.masheqal.app.data.QuranVerse
import com.masheqal.app.data.SettingsState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuranPageScreen(app: MasheqalApp, nav: NavHostController, page: Int) {
    val context = LocalContext.current
    val settings by app.settings.state.collectAsState(initial = SettingsState())
    val studyRepository = remember(context) { QuranStudyRepository(context) }
    val tajweedRepository = remember(context) { QuranTajweedRepository(context) }
    val scope = rememberCoroutineScope()

    var verses by remember { mutableStateOf(emptyList<QuranVerse>()) }
    var currentJuz by remember { mutableIntStateOf(1) }
    var showTranslation by remember(settings.showEnglishTranslation) {
        mutableStateOf(settings.showEnglishTranslation)
    }
    var translationTexts by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var translationUnavailable by remember { mutableStateOf(false) }
    var tajweedTexts by remember { mutableStateOf<Map<String, String>>(emptyMap()) }

    LaunchedEffect(page) {
        val loaded = app.quran.versesOfPage(page.coerceIn(1, 604))
        verses = loaded
        currentJuz = loaded.firstOrNull()?.let { app.quran.juzForVerse(it.surah, it.ayah) } ?: 1
        loaded.firstOrNull()?.let { app.personal.setReading(it.surah, it.ayah) }
    }

    LaunchedEffect(verses, showTranslation, settings.translationEdition) {
        translationTexts = emptyMap()
        translationUnavailable = false
        if (!showTranslation || verses.isEmpty()) return@LaunchedEffect

        val loadedTranslations = mutableMapOf<String, String>()
        var failed = false
        verses.groupBy { it.surah }.forEach { (surah, chapterVerses) ->
            val edition = runCatching {
                studyRepository.loadSurah(surah, settings.translationEdition)
            }.getOrNull()
            if (edition == null) failed = true
            chapterVerses.forEach { verse ->
                val translated = edition?.getOrNull(verse.ayah - 1)
                    ?.takeIf { it.isNotBlank() }
                    ?: if (settings.translationEdition == "en.sahih") {
                        verse.translationEn?.takeIf { it.isNotBlank() }
                    } else null
                if (translated != null) loadedTranslations["${verse.surah}:${verse.ayah}"] = translated
            }
        }
        translationTexts = loadedTranslations
        translationUnavailable = failed && loadedTranslations.isEmpty()
    }

    LaunchedEffect(verses, settings.showTajweedColors) {
        tajweedTexts = emptyMap()
        if (!settings.showTajweedColors || verses.isEmpty()) return@LaunchedEffect

        val loadedTagged = mutableMapOf<String, String>()
        verses.groupBy { it.surah }.forEach { (surah, chapterVerses) ->
            val completeChapter = runCatching { app.quran.versesOfSurah(surah) }.getOrNull()
            val tagged = completeChapter?.let {
                runCatching { tajweedRepository.loadSurah(surah, it.size) }.getOrNull()
            }
            if (tagged != null) {
                chapterVerses.forEach { verse ->
                    tagged.getOrNull(verse.ayah - 1)?.let {
                        loadedTagged["${verse.surah}:${verse.ayah}"] = it
                    }
                }
            }
        }
        tajweedTexts = loadedTagged
    }

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Column {
                    Text("${stringResource(R.string.page)} $page")
                    Text(
                        "${stringResource(R.string.juz)} $currentJuz",
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
                IconButton(onClick = { showTranslation = !showTranslation }) {
                    Icon(
                        if (showTranslation) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        stringResource(R.string.show_translation)
                    )
                }
                IconButton(
                    onClick = { if (page > 1) nav.navigate("quran/page/${page - 1}") },
                    enabled = page > 1
                ) {
                    Icon(Icons.Default.ChevronLeft, stringResource(R.string.previous))
                }
                IconButton(
                    onClick = { if (page < 604) nav.navigate("quran/page/${page + 1}") },
                    enabled = page < 604
                ) {
                    Icon(Icons.Default.ChevronRight, stringResource(R.string.next))
                }
            }
        )

        if (translationUnavailable) {
            Text(
                stringResource(R.string.translation_unavailable_offline),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(verses, key = { "${it.surah}:${it.ayah}" }) { verse ->
                val reference = "${verse.surah}:${verse.ayah}"
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        Modifier.fillMaxWidth()
                            .clickable {
                                scope.launch { app.personal.setReading(verse.surah, verse.ayah) }
                                nav.navigate("quran/ref/${verse.surah}/${verse.ayah}")
                            }
                            .padding(horizontal = 18.dp, vertical = 15.dp)
                    ) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                        ) {
                            Text(
                                reference,
                                color = MaterialTheme.colorScheme.secondary,
                                fontWeight = FontWeight.SemiBold
                            )
                            Icon(
                                Icons.Default.OpenInNew,
                                stringResource(R.string.open_quran),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                        val taggedText = tajweedTexts[reference]
                        if (settings.showTajweedColors && taggedText != null) {
                            QuranTajweedText(taggedText, size = 27f, modifier = Modifier.fillMaxWidth())
                        } else {
                            QuranText(verse.text, size = 27f, modifier = Modifier.fillMaxWidth())
                        }
                        if (showTranslation) {
                            val translation = translationTexts[reference]
                            if (!translation.isNullOrBlank()) {
                                Spacer(Modifier.height(10.dp))
                                Text(
                                    translation,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else if (settings.translationEdition != "en.sahih") {
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    stringResource(R.string.translation_unavailable_offline),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                verse.translationEn?.takeIf { it.isNotBlank() }?.let { fallback ->
                                    Spacer(Modifier.height(10.dp))
                                    Text(
                                        fallback,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
