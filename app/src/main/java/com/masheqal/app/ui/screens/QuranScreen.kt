
package com.masheqal.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
                FilledTonalIconButton(onClick = { nav.navigate("search") }) {
                    Icon(Icons.Default.Search, stringResource(R.string.search))
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
