package com.masheqal.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.masheqal.app.MasheqalApp
import com.masheqal.app.R

@Composable
fun LibraryScreen(app: MasheqalApp, nav: NavHostController) {
    val rows = listOf(
        Triple(stringResource(R.string.tafsir), stringResource(R.string.source_required), Icons.Default.MenuBook),
        Triple(stringResource(R.string.hadith), stringResource(R.string.source_required), Icons.Default.LibraryBooks),
        Triple(stringResource(R.string.dua), stringResource(R.string.source_required), Icons.Default.FavoriteBorder),
        Triple(stringResource(R.string.names_of_allah), stringResource(R.string.source_required), Icons.Default.AutoAwesome),
        Triple(stringResource(R.string.saved), stringResource(R.string.saved), Icons.Default.Bookmark),
        Triple(stringResource(R.string.notes), stringResource(R.string.notes), Icons.Default.Notes),
        Triple(stringResource(R.string.khatmah), stringResource(R.string.quran), Icons.Default.AutoStories),
        Triple(stringResource(R.string.tasbih), stringResource(R.string.tasbih), Icons.Default.TouchApp),
        Triple(stringResource(R.string.calendar), stringResource(R.string.calendar), Icons.Default.CalendarMonth),
        Triple(stringResource(R.string.content_center), stringResource(R.string.content_center), Icons.Default.Inventory2),
        Triple(stringResource(R.string.settings), stringResource(R.string.settings), Icons.Default.Settings)
    )
    Column(Modifier.fillMaxSize().padding(horizontal = 12.dp)) {
        Text(stringResource(R.string.library), style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(12.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
            items(rows) { (title, subtitle, icon) ->
                FeatureCard(title, subtitle, icon = icon, onClick = {
                    when (title) {
                        stringResource(R.string.saved) -> nav.navigate("saved")
                        stringResource(R.string.notes) -> nav.navigate("notes")
                        stringResource(R.string.khatmah) -> nav.navigate("khatmah")
                        stringResource(R.string.tasbih) -> nav.navigate("tasbih")
                        stringResource(R.string.calendar) -> nav.navigate("calendar")
                        stringResource(R.string.content_center) -> nav.navigate("content")
                        stringResource(R.string.settings) -> nav.navigate("settings")
                        else -> nav.navigate("content")
                    }
                })
            }
        }
    }
}
