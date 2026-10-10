package com.masheqal.app.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.masheqal.app.R
import com.masheqal.app.data.AdhkarItem
import com.masheqal.app.data.AdhkarRepository

private data class DhikrCategory(
    val key: String,
    val title: Int,
    val subtitle: Int,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

@Composable
fun AdhkarScreen(nav: NavHostController) {
    val context = LocalContext.current
    val preferences = remember { context.getSharedPreferences("adhkar_progress", android.content.Context.MODE_PRIVATE) }
    var allEntries by remember { mutableStateOf(emptyList<AdhkarItem>()) }
    var isLoading by remember { mutableStateOf(true) }
    var loadFailed by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf(if (java.time.LocalTime.now().hour in 4..11) "morning" else "evening") }
    var query by remember { mutableStateOf("") }
    val progress = remember { mutableStateMapOf<String, Int>() }

    LaunchedEffect(Unit) {
        runCatching { AdhkarRepository(context).loadAll() }
            .onSuccess {
                allEntries = it
                isLoading = false
                loadFailed = it.isEmpty()
            }
            .onFailure {
                isLoading = false
                loadFailed = true
            }
    }

    val categories = listOf(
        DhikrCategory("morning", R.string.morning, R.string.dhikr_morning_subtitle, Icons.Default.WbSunny),
        DhikrCategory("evening", R.string.evening, R.string.dhikr_evening_subtitle, Icons.Default.NightsStay),
        DhikrCategory("after_prayer", R.string.after_prayer_title, R.string.dhikr_after_prayer_subtitle, Icons.Default.Mosque),
        DhikrCategory("daily_dua", R.string.daily_duas_title, R.string.dhikr_daily_duas_subtitle, Icons.Default.Favorite)
    )
    val currentCategory = categories.first { it.key == selectedCategory }
    val filteredEntries = remember(allEntries, selectedCategory, query) {
        allEntries.filter { item ->
            item.category == selectedCategory && (
                query.isBlank() ||
                item.arabic.contains(query.trim(), ignoreCase = true) ||
                item.title.contains(query.trim(), ignoreCase = true) ||
                item.translation.contains(query.trim(), ignoreCase = true) ||
                item.source.contains(query.trim(), ignoreCase = true)
            )
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 12.dp, bottom = 30.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth().animateContentSize(),
                shape = RoundedCornerShape(30.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Box(
                    Modifier.fillMaxWidth().background(
                        Brush.linearGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.88f),
                                MaterialTheme.colorScheme.secondary.copy(alpha = 0.60f)
                            )
                        )
                    )
                ) {
                    Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = Color.White.copy(alpha = 0.16f),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Icon(
                                    Icons.Default.Spa, null,
                                    modifier = Modifier.padding(12.dp).size(28.dp),
                                    tint = Color.White
                                )
                            }
                            Surface(
                                color = Color.White.copy(alpha = 0.14f),
                                shape = RoundedCornerShape(50)
                            ) {
                                Row(
                                    Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.OfflinePin, null, tint = Color.White, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(5.dp))
                                    Text(stringResource(R.string.dhikr_offline_label), color = Color.White, style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                        Text(
                            stringResource(R.string.adhkar_dashboard_title),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            stringResource(R.string.adhkar_dashboard_subtitle),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.88f)
                        )
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                stringResource(R.string.dhikr_total, allEntries.size),
                                style = MaterialTheme.typography.labelLarge,
                                color = Color.White.copy(alpha = 0.94f)
                            )
                            FilledTonalButton(
                                onClick = { nav.navigate("content") },
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = Color.White.copy(alpha = 0.18f),
                                    contentColor = Color.White
                                )
                            ) {
                                Icon(Icons.Default.LibraryBooks, null)
                                Spacer(Modifier.width(7.dp))
                                Text(stringResource(R.string.content_center))
                            }
                        }
                    }
                }
            }
        }

        item {
            Text(
                stringResource(R.string.dhikr_sections),
                Modifier.padding(start = 20.dp, end = 20.dp, top = 4.dp),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Column(
                Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                categories.chunked(2).forEach { rowCategories ->
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        rowCategories.forEach { category ->
                            val selected = category.key == selectedCategory
                            Card(
                                onClick = {
                                    selectedCategory = category.key
                                    query = ""
                                },
                                modifier = Modifier.weight(1f).animateContentSize(),
                                shape = RoundedCornerShape(22.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surface
                                ),
                                border = if (selected) null else CardDefaults.outlinedCardBorder()
                            ) {
                                Column(
                                    Modifier.fillMaxWidth().padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(14.dp),
                                        color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                        else MaterialTheme.colorScheme.surfaceVariant,
                                        modifier = Modifier.size(42.dp)
                                    ) {
                                        Icon(
                                            category.icon, null,
                                            modifier = Modifier.padding(10.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    Text(
                                        stringResource(category.title),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 2
                                    )
                                    Text(
                                        stringResource(category.subtitle),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        minLines = 2
                                    )
                                    Text(
                                        allEntries.count { it.category == category.key }.toString(),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        stringResource(currentCategory.title),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        stringResource(R.string.dhikr_count, filteredEntries.size),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                FilledTonalIconButton(onClick = {
                    filteredEntries.forEach { item ->
                        progress.remove(item.id)
                        preferences.edit().remove(item.id).apply()
                    }
                }) {
                    Icon(Icons.Default.RestartAlt, stringResource(R.string.dhikr_reset_count))
                }
            }
        }

        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(18.dp),
                placeholder = { Text(stringResource(R.string.dhikr_search_hint)) },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }) { Icon(Icons.Default.Close, stringResource(R.string.clear)) }
                    }
                }
            )
        }

        if (isLoading) {
            item {
                Box(Modifier.fillMaxWidth().padding(28.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        } else if (loadFailed) {
            item {
                EmptyState(
                    title = stringResource(R.string.dhikr_load_error_title),
                    details = stringResource(R.string.dhikr_load_error_body),
                    icon = Icons.Default.CloudOff
                )
            }
        } else if (filteredEntries.isEmpty()) {
            item {
                EmptyState(
                    title = stringResource(R.string.dhikr_no_results),
                    details = stringResource(R.string.dhikr_no_results_hint),
                    icon = Icons.Default.SearchOff
                )
            }
        } else {
            items(filteredEntries, key = { it.id }) { entry ->
                val saved = progress[entry.id] ?: preferences.getInt(entry.id, 0)
                val target = entry.count.coerceAtLeast(1)
                Card(
                    modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth().animateContentSize(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (saved >= target) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(
                        Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                entry.title,
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    stringResource(R.string.dhikr_repetition, saved, target),
                                    modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }
                        Text(
                            entry.arabic,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                            fontSize = 26.sp,
                            lineHeight = 48.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (entry.translation.isNotBlank()) {
                            Text(
                                entry.translation,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (entry.transliteration.isNotBlank()) {
                            Text(
                                entry.transliteration,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        LinearProgressIndicator(
                            progress = { (saved.toFloat() / target).coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth(),
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                if (entry.countDescription.isNotBlank()) {
                                    Text(entry.countDescription, style = MaterialTheme.typography.labelMedium)
                                }
                                if (entry.source.isNotBlank()) {
                                    Text(
                                        stringResource(R.string.dhikr_source_reference, entry.source),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = {
                                    progress[entry.id] = 0
                                    preferences.edit().putInt(entry.id, 0).apply()
                                }) {
                                    Icon(Icons.Default.Refresh, stringResource(R.string.dhikr_reset_count))
                                }
                                FilledIconButton(
                                    onClick = {
                                        val nextCount = saved + 1
                                        progress[entry.id] = nextCount
                                        preferences.edit().putInt(entry.id, nextCount).apply()
                                    }
                                ) {
                                    Icon(
                                        if (saved >= target) Icons.Default.Check else Icons.Default.Add,
                                        stringResource(R.string.dhikr_count_action)
                                    )
                                }
                            }
                        }
                        if (entry.benefit.isNotBlank()) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))
                            Text(entry.benefit, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = {
                                val share = buildString {
                                    append(entry.arabic)
                                    if (entry.translation.isNotBlank()) append("\n\n").append(entry.translation)
                                    if (entry.source.isNotBlank()) append("\n\n").append(entry.source)
                                }
                                shareText(context, share)
                            }) {
                                Icon(Icons.Default.Share, null)
                                Spacer(Modifier.width(5.dp))
                                Text(stringResource(R.string.share))
                            }
                        }
                    }
                }
            }
        }

        item {
            Text(
                stringResource(R.string.dhikr_attribution),
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
