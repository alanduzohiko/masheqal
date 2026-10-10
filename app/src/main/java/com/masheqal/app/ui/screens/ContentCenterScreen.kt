package com.masheqal.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.masheqal.app.R
import com.masheqal.app.data.ContentPackageManager

private data class ContentLine(
    val title: Int,
    val detail: Int,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val route: String? = null
)

@Composable
fun ContentCenterScreen(nav: NavHostController) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val installed = ContentPackageManager(context).listInstalled()

    val bundled = listOf(
        ContentLine(R.string.quran_arabic, R.string.bundled_sourced_content, Icons.Default.MenuBook, "quran"),
        ContentLine(R.string.quran_english, R.string.bundled_sourced_content, Icons.Default.Translate, "quran"),
        ContentLine(R.string.quran_font, R.string.installed, Icons.Default.TextFields, "quran"),
        ContentLine(R.string.adhkar, R.string.bundled_sourced_content, Icons.Default.Spa, "adhkar"),
        ContentLine(R.string.dua, R.string.bundled_sourced_content, Icons.Default.FavoriteBorder, "adhkar")
    )
    val connected = listOf(
        ContentLine(R.string.tafsir, R.string.online_cached_content, Icons.Default.AutoStories, "quran"),
        ContentLine(R.string.audio, R.string.streamed_audio_content, Icons.Default.Headphones, "quran"),
        ContentLine(R.string.adhan_library, R.string.streamed_audio_content, Icons.Default.RecordVoiceOver, "adhan"),
        ContentLine(R.string.tajweed, R.string.online_cached_content, Icons.Default.Palette, "quran")
    )
    val gated = listOf(
        ContentLine(R.string.quran_sorani, R.string.source_required, Icons.Default.Translate),
        ContentLine(R.string.hadith, R.string.source_required, Icons.Default.LibraryBooks),
        ContentLine(R.string.hisn, R.string.source_required, Icons.Default.MenuBook),
        ContentLine(R.string.names_of_allah, R.string.source_required, Icons.Default.AutoAwesome),
        ContentLine(R.string.word_analysis, R.string.source_required, Icons.Default.Search)
    )

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            IconButton(onClick = { nav.popBackStack() }) {
                Icon(Icons.Default.ArrowBack, null)
            }
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.content_center), style = MaterialTheme.typography.headlineSmall)
                Text(
                    stringResource(R.string.content_center_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Text(
            stringResource(R.string.content_policy),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(vertical = 10.dp)
        )
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(7.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            item {
                SectionTitle(stringResource(R.string.bundled))
            }
            items(bundled) { row -> ContentLineCard(row) { row.route?.let(nav::navigate) } }
            item {
                SectionTitle(stringResource(R.string.connected_content))
            }
            items(connected) { row -> ContentLineCard(row) { row.route?.let(nav::navigate) } }
            item {
                SectionTitle(stringResource(R.string.source_dependent_content))
            }
            items(gated) { row -> ContentLineCard(row, enabled = false) {} }
            if (installed.isNotEmpty()) {
                item { SectionTitle(stringResource(R.string.installed_packages)) }
                items(installed) { pack ->
                    ListItem(
                        headlineContent = { Text(pack.title) },
                        supportingContent = { Text("${pack.type} • ${pack.version} • ${pack.license}") },
                        leadingContent = { Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ContentLineCard(
    row: ContentLine,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().then(
            if (enabled) Modifier.clickable(onClick = onClick) else Modifier
        ),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (enabled) MaterialTheme.colorScheme.surface
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.48f)
        )
    ) {
        Row(
            Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(13.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Icon(row.icon, null, Modifier.padding(10.dp), tint = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(row.title), style = MaterialTheme.typography.titleSmall)
                Text(
                    stringResource(row.detail),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (enabled) Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            else Icon(Icons.Default.Lock, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
