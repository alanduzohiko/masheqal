
package com.masheqal.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
    val route: String
)

@Composable
fun LibraryScreen(app: MasheqalApp, nav: NavHostController) {
    val items = listOf(
        LibraryRow(R.string.audio, R.string.audio_source_notice, Icons.Default.Headphones, "audio"),
        LibraryRow(R.string.saved, R.string.bookmarks, Icons.Default.Bookmark, "saved"),
        LibraryRow(R.string.notes, R.string.notes, Icons.Default.Notes, "notes"),
        LibraryRow(R.string.khatmah, R.string.quran, Icons.Default.AutoStories, "khatmah"),
        LibraryRow(R.string.tasbih, R.string.tasbih, Icons.Default.TouchApp, "tasbih"),
        LibraryRow(R.string.calendar, R.string.calendar, Icons.Default.CalendarMonth, "calendar"),
        LibraryRow(R.string.qibla, R.string.qibla, Icons.Default.Explore, "qibla"),
        LibraryRow(R.string.tafsir, R.string.source_required, Icons.Default.MenuBook, "content"),
        LibraryRow(R.string.hadith, R.string.source_required, Icons.Default.LibraryBooks, "content"),
        LibraryRow(R.string.dua, R.string.source_required, Icons.Default.FavoriteBorder, "content"),
        LibraryRow(R.string.names_of_allah, R.string.source_required, Icons.Default.AutoAwesome, "content"),
        LibraryRow(R.string.content_center, R.string.download_center, Icons.Default.Inventory2, "content"),
        LibraryRow(R.string.settings, R.string.settings, Icons.Default.Settings, "settings")
    )

    Column(
        Modifier.fillMaxSize().padding(top = 16.dp)
    ) {
        Text(
            stringResource(R.string.library),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(horizontal = 20.dp)
        )
        Text(
            stringResource(R.string.download_center),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
        )
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(items) { item ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    onClick = { nav.navigate(item.route) }
                ) {
                    Column(Modifier.padding(16.dp)) {
                        IconBadge(
                            item.icon,
                            emphasized = item.route != "content"
                        )
                        Spacer(Modifier.height(14.dp))
                        Text(
                            stringResource(item.titleId),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(Modifier.height(3.dp))
                        Text(
                            stringResource(item.subtitleId),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2
                        )
                    }
                }
            }
        }
    }
}
