package com.masheqal.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.masheqal.app.MasheqalApp
import com.masheqal.app.R
import com.masheqal.app.data.QuranVerse

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuranPageScreen(app: MasheqalApp, nav: NavHostController, page: Int) {
    val currentPage = page.coerceIn(1, 604)
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    var verses by remember { mutableStateOf(emptyList<QuranVerse>()) }
    var currentJuz by remember { mutableStateOf(1) }

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
                .padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}
