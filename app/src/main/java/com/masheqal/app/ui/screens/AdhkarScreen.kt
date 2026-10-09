package com.masheqal.app.ui.screens

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.masheqal.app.MasheqalApp
import com.masheqal.app.R
import com.masheqal.app.data.AdhkarItem
import com.masheqal.app.data.AdhkarPackage
import com.masheqal.app.data.AdhkarPeriod
import com.masheqal.app.data.AdhkarProgressStore
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdhkarScreen(app: MasheqalApp, nav: NavHostController) {
    val context = LocalContext.current
    val progressStore = remember(context) { AdhkarProgressStore(context) }
    var sessionDate by remember { mutableStateOf(LocalDate.now().toString()) }
    var period by remember { mutableStateOf(AdhkarPeriod.MORNING) }
    var packageData by remember { mutableStateOf<AdhkarPackage?>(null) }
    var loading by remember { mutableStateOf(true) }
    var loadFailed by remember { mutableStateOf(false) }
    var retryNonce by remember { mutableStateOf(0) }
    val counts = remember { mutableStateMapOf<Int, Int>() }

    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(30_000L)
            val today = LocalDate.now().toString()
            if (today != sessionDate) sessionDate = today
        }
    }

    LaunchedEffect(period, retryNonce, sessionDate) {
        loading = true
        loadFailed = false
        runCatching { app.adhkar.loadPackage() }
            .onSuccess { loaded ->
                packageData = loaded
                counts.clear()
                loaded.items.filter { it.appliesTo(period) }.forEach { item ->
                    counts[item.order] = progressStore.getCount(sessionDate, period, item.order)
                }
            }
            .onFailure { loadFailed = true }
        loading = false
    }

    val visibleItems = packageData?.items.orEmpty().filter { it.appliesTo(period) }
    val totalTarget = visibleItems.sumOf { it.repeatCount }
    val completed = visibleItems.sumOf { counts[it.order] ?: 0 }
    val progress = if (totalTarget == 0) 0f else (completed.toFloat() / totalTarget).coerceIn(0f, 1f)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(stringResource(R.string.adhkar))
                        Text(
                            stringResource(R.string.adhkar_progress_label, completed, totalTarget),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { nav.navigate("content") }) {
                        Icon(Icons.Default.Inventory2, contentDescription = stringResource(R.string.content_center))
                    }
                }
            )
        }
    ) { insets ->
        Column(
            Modifier.fillMaxSize().padding(insets).padding(horizontal = 16.dp)
        ) {
            Row(
                Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FilterChip(
                    selected = period == AdhkarPeriod.MORNING,
                    onClick = { period = AdhkarPeriod.MORNING },
                    label = { Text(stringResource(R.string.morning)) },
                    leadingIcon = { Icon(Icons.Default.WbSunny, contentDescription = null) },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = period == AdhkarPeriod.EVENING,
                    onClick = { period = AdhkarPeriod.EVENING },
                    label = { Text(stringResource(R.string.evening)) },
                    leadingIcon = { Icon(Icons.Default.NightsStay, contentDescription = null) },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth()
            )

            if (loading) {
                Box(
                    Modifier.fillMaxWidth().weight(1f),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator() }
            } else if (loadFailed || packageData == null) {
                Column(
                    Modifier.fillMaxWidth().weight(1f).padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.CloudOff, contentDescription = null, modifier = Modifier.size(40.dp))
                    Spacer(Modifier.height(12.dp))
                    Text(stringResource(R.string.adhkar_load_error), textAlign = TextAlign.Center)
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = { retryNonce++ }) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.adhkar_retry))
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item(key = "source-notice") {
                        Card(
                            shape = RoundedCornerShape(22.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer
                            )
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                Text(
                                    stringResource(R.string.adhkar_bundled_title),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(Modifier.height(5.dp))
                                Text(
                                    stringResource(R.string.adhkar_bundled_notice),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                packageData?.source?.let { source ->
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        "${source.attribution} · ${source.release} · ${source.license}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }
                        }
                    }

                    items(visibleItems, key = { it.order }) { item ->
                        AdhkarCard(
                            item = item,
                            count = counts[item.order] ?: 0,
                            onIncrement = {
                                counts[item.order] = progressStore.setCount(
                                    sessionDate, period, item, (counts[item.order] ?: 0) + 1
                                )
                            },
                            onReset = {
                                progressStore.reset(sessionDate, period, item)
                                counts[item.order] = 0
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AdhkarCard(
    item: AdhkarItem,
    count: Int,
    onIncrement: () -> Unit,
    onReset: () -> Unit
) {
    val completed = count >= item.repeatCount
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "${item.order}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.secondary
                )
                Text(
                    "$count / ${item.repeatCount}",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(Modifier.height(10.dp))
            Text(
                item.arabic,
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center
            )

            if (item.translationEn.isNotBlank()) {
                Spacer(Modifier.height(12.dp))
                Text(
                    stringResource(R.string.adhkar_translation_en_label),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary
                )
                Spacer(Modifier.height(3.dp))
                Text(item.translationEn, style = MaterialTheme.typography.bodyMedium)
            }

            if (item.repeatDescriptionAr.isNotBlank() || item.repeatDescriptionEn.isNotBlank()) {
                Spacer(Modifier.height(10.dp))
                if (item.repeatDescriptionAr.isNotBlank()) {
                    Text(
                        "${stringResource(R.string.adhkar_repeat_note_ar)}: ${item.repeatDescriptionAr}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (item.repeatDescriptionEn.isNotBlank()) {
                    Text(
                        "${stringResource(R.string.adhkar_repeat_note_en)}: ${item.repeatDescriptionEn}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (item.meritAr.isNotBlank() || item.meritEn.isNotBlank()) {
                Spacer(Modifier.height(10.dp))
                if (item.meritAr.isNotBlank()) {
                    Text(
                        "${stringResource(R.string.adhkar_merit_ar_label)}: ${item.meritAr}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (item.meritEn.isNotBlank()) {
                    Text(
                        "${stringResource(R.string.adhkar_merit_en_label)}: ${item.meritEn}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (item.sourceAr.isNotBlank()) {
                Spacer(Modifier.height(10.dp))
                Text(
                    "${stringResource(R.string.adhkar_source_label)}: ${item.sourceAr}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(12.dp))
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(onClick = onReset, enabled = count > 0) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.reset))
                }
                Spacer(Modifier.weight(1f))
                FilledTonalButton(onClick = onIncrement, enabled = !completed) {
                    Icon(
                        if (completed) Icons.Default.CheckCircle else Icons.Default.Add,
                        contentDescription = null
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        if (completed) stringResource(R.string.complete)
                        else stringResource(R.string.adhkar_count_one)
                    )
                }
            }
        }
    }
}
