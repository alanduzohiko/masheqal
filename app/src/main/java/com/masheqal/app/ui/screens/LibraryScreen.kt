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

private data class LibraryRow(
    val titleId: Int,
    val subtitleId: Int,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val route: String?
)

@Composable
fun LibraryScreen(app: MasheqalApp, nav: NavHostController) {
    val rows = listOf(
        LibraryRow(R.string.tafsir, R.string.source_required, Icons.Default.MenuBook, "content"),
        LibraryRow(R.string.hadith, R.string.source_required, Icons.Default.LibraryBooks, "content"),
        LibraryRow(R.string.dua, R.string.source_required, Icons.Default.FavoriteBorder, "content"),
        LibraryRow(R.string.names_of_allah, R.string.source_required, Icons.Default.AutoAwesome, "content"),
        LibraryRow(R.string.saved, R.string.saved, Icons.Default.Bookmark, "saved"),
        LibraryRow(R.string.notes, R.string.notes, Icons.Default.Notes, "notes"),
        LibraryRow(R.string.khatmah, R.string.quran, Icons.Default.AutoStories, "khatmah"),
        LibraryRow(R.string.tasbih, R.string.tasbih, Icons.Default.TouchApp, "tasbih"),
        LibraryRow(R.string.calendar, R.string.calendar, Icons.Default.CalendarMonth, "calendar"),
        LibraryRow(R.string.content_center, R.string.content_center, Icons.Default.Inventory2, "content"),
        LibraryRow(R.string.settings, R.string.settings, Icons.Default.Settings, "settings")
    )
    Column(Modifier.fillMaxSize().padding(horizontal = 12.dp)) {
        Text(stringResource(R.string.library), style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(12.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
            items(rows) { row ->
                FeatureCard(
                    title = stringResource(row.titleId),
                    subtitle = stringResource(row.subtitleId),
                    icon = row.icon,
                    onClick = { row.route?.let(nav::navigate) }
                )
            }
        }
    }
}
