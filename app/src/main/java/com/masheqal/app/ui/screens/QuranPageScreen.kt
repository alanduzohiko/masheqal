package com.masheqal.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.masheqal.app.MasheqalApp
import com.masheqal.app.R
import com.masheqal.app.data.QuranVerse
import com.masheqal.app.data.SettingsState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuranPageScreen(app: MasheqalApp, nav: NavHostController, page: Int) {
    val currentPage = page.coerceIn(1, 604)
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val settings by app.settings.state.collectAsState(initial = SettingsState())
    var verses by remember { mutableStateOf(emptyList<QuranVerse>()) }
    var currentJuz by remember { mutableStateOf(1) }
    var showTranslation by remember { mutableStateOf(false) }
    var showMushaf by remember(currentPage) { mutableStateOf(true) }

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
                        contentDescription = null
                    )
                }
            },
            actions = {
                IconButton(
                    onClick = { showMushaf = !showMushaf }
                ) {
                    Icon(
                        if (showMushaf) Icons.Default.TextFields else Icons.Default.MenuBook,
                        contentDescription = stringResource(
                            if (showMushaf) R.string.text_mode else R.string.mushaf_mode
                        )
                    )
                }
                if (!showMushaf) {
                    IconButton(onClick = { showTranslation = !showTranslation }) {
                        Icon(
                            if (showTranslation) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = stringResource(R.string.show_translation)
                        )
                    }
                }
                IconButton(
                    onClick = { if (currentPage > 1) nav.navigate("quran/page/${currentPage - 1}") },
                    enabled = currentPage > 1
                ) {
                    Icon(
                        if (isRtl) Icons.Default.ChevronRight else Icons.Default.ChevronLeft,
                        contentDescription = null
                    )
                }
                IconButton(
                    onClick = { if (currentPage < 604) nav.navigate("quran/page/${currentPage + 1}") },
                    enabled = currentPage < 604
                ) {
                    Icon(
                        if (isRtl) Icons.Default.ChevronLeft else Icons.Default.ChevronRight,
                        contentDescription = null
                    )
                }
            }
        )
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("${stringResource(R.string.juz)} $currentJuz")
            Text(
                "${stringResource(R.string.mushaf_edition)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (showMushaf) {
            MushafPageImage(
                page = currentPage,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(verses) { verse ->
                    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                        Text(
                            "${verse.surah}:${verse.ayah}",
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.SemiBold
                        )
                        QuranText(
                            verse.text,
                            size = 27f,
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (showTranslation) {
                            val visibleTranslation = verse.translationFor(settings.language)
                            if (!visibleTranslation.isNullOrBlank()) {
                                Text(
                                    visibleTranslation,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            } else {
                                Text(
                                    stringResource(R.string.translation_unavailable),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
