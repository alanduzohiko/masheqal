package com.masheqal.app.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DuaScreen(
    app: MasheqalApp,
    nav: NavHostController,
    initialQuery: String = ""
) {
    val context = LocalContext.current
    var packageData by remember { mutableStateOf<AdhkarPackage?>(null) }
    var loading by remember { mutableStateOf(true) }
    var loadFailed by remember { mutableStateOf(false) }
    var retry by remember { mutableIntStateOf(0) }
    var query by rememberSaveable(initialQuery) { mutableStateOf(initialQuery) }
    var category by remember { mutableStateOf(AdhkarPeriod.ALL) }

    val categoryChoices = listOf(
        AdhkarPeriod.ALL,
        AdhkarPeriod.AFTER_PRAYER,
        AdhkarPeriod.SLEEP,
        AdhkarPeriod.WAKE_UP,
        AdhkarPeriod.BATHROOM,
        AdhkarPeriod.FOOD,
        AdhkarPeriod.MOSQUE,
        AdhkarPeriod.WUDU,
        AdhkarPeriod.FASTING,
        AdhkarPeriod.HOME,
        AdhkarPeriod.TRAVEL,
        AdhkarPeriod.CLOTHING,
        AdhkarPeriod.WEATHER,
        AdhkarPeriod.PROTECTION,
        AdhkarPeriod.GENERAL
    )

    LaunchedEffect(retry) {
        loading = true
        loadFailed = false
        runCatching { app.adhkar.loadPackage() }
            .onSuccess { packageData = it }
            .onFailure { loadFailed = true }
        loading = false
    }

    val sourceDuas = packageData?.items.orEmpty().filter { it.titleEn.isNotBlank() }
    val normalizedQuery = query.trim().lowercase()
    val visibleDuas = sourceDuas.filter { item ->
        val matchesCategory = category == AdhkarPeriod.ALL || item.categoryId == category.categoryId
        val matchesQuery = normalizedQuery.isBlank() || GlobalSearchMatcher.matches(
            query,
            listOf(item.titleEn, item.arabic, item.transliteration, item.translationEn, item.sourceEn, item.sourceAr)
        )
        matchesCategory && matchesQuery
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(stringResource(R.string.dua))
                        Text(
                            stringResource(R.string.adhkar_expanded_content_title),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)
        ) {
            Row(
                Modifier.fillMaxWidth().padding(top = 8.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categoryChoices.forEach { option ->
                    FilterChip(
                        selected = category == option,
                        onClick = { category = option },
                        label = { Text(duaCategoryLabel(option)) }
                    )
                }
            }

            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text(stringResource(R.string.dua_search_hint)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotBlank()) {
                        IconButton(onClick = { query = "" }) {
                            Icon(Icons.Default.Close, contentDescription = stringResource(R.string.clear))
                        }
                    }
                }
            )

            when {
                loading -> Box(
                    Modifier.fillMaxWidth().weight(1f),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator() }

                loadFailed || packageData == null -> Column(
                    Modifier.fillMaxWidth().weight(1f).padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(stringResource(R.string.adhkar_load_error), textAlign = TextAlign.Center)
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = { retry++ }) { Text(stringResource(R.string.adhkar_retry)) }
                }

                else -> LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item(key = "dua-source-notice") {
                        Card(
                            shape = RoundedCornerShape(22.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer
                            )
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                Text(
                                    stringResource(R.string.adhkar_expanded_content_title),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    stringResource(R.string.dua_source_notice),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                packageData?.source?.let { source ->
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        "${source.attribution} · ${source.license}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }
                        }
                    }

                    if (visibleDuas.isEmpty()) {
                        item(key = "dua-empty") {
                            Text(
                                stringResource(R.string.dua_no_results),
                                modifier = Modifier.fillMaxWidth().padding(24.dp),
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    items(visibleDuas, key = { it.order }) { item ->
                        DuaCard(
                            item = item,
                            onCopy = { copyDua(context, item) },
                            onShare = { shareDua(context, item) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DuaCard(
    item: AdhkarItem,
    onCopy: () -> Unit,
    onShare: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                item.titleEn,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(12.dp))
            Text(
                item.arabic,
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center
            )
            if (item.transliteration.isNotBlank()) {
                Spacer(Modifier.height(10.dp))
                Text(
                    item.transliteration,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (item.translationEn.isNotBlank()) {
                Spacer(Modifier.height(10.dp))
                Text(
                    stringResource(R.string.adhkar_translation_en_label),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary
                )
                Spacer(Modifier.height(3.dp))
                Text(item.translationEn, style = MaterialTheme.typography.bodyMedium)
            }
            if (item.meritEn.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(item.meritEn, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.height(10.dp))
            Text(
                "${stringResource(R.string.adhkar_source_label)}: ${item.sourceEn}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(onClick = onCopy) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.copy))
                }
                Spacer(Modifier.weight(1f))
                FilledTonalButton(onClick = onShare) {
                    Icon(Icons.Default.Share, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.share))
                }
            }
        }
    }
}

@Composable
private fun duaCategoryLabel(category: AdhkarPeriod): String {
    val resource = when (category) {
        AdhkarPeriod.ALL -> R.string.adhkar_all
        AdhkarPeriod.AFTER_PRAYER -> R.string.adhkar_after_prayer
        AdhkarPeriod.SLEEP -> R.string.adhkar_sleep
        AdhkarPeriod.WAKE_UP -> R.string.adhkar_wake_up
        AdhkarPeriod.BATHROOM -> R.string.adhkar_bathroom
        AdhkarPeriod.FOOD -> R.string.adhkar_food
        AdhkarPeriod.MOSQUE -> R.string.adhkar_mosque
        AdhkarPeriod.WUDU -> R.string.adhkar_wudu
        AdhkarPeriod.FASTING -> R.string.adhkar_fasting
        AdhkarPeriod.HOME -> R.string.adhkar_home
        AdhkarPeriod.TRAVEL -> R.string.adhkar_travel
        AdhkarPeriod.CLOTHING -> R.string.adhkar_clothing
        AdhkarPeriod.WEATHER -> R.string.adhkar_weather
        AdhkarPeriod.PROTECTION -> R.string.adhkar_protection
        AdhkarPeriod.GENERAL -> R.string.adhkar_general
        else -> R.string.adhkar_all
    }
    return stringResource(resource)
}

private fun copyDua(context: Context, item: AdhkarItem) {
    val text = buildString {
        append(item.arabic)
        if (item.transliteration.isNotBlank()) append("\n\n").append(item.transliteration)
        append("\n\n").append(item.translationEn)
        append("\n\n").append(item.sourceEn)
    }
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText(item.titleEn, text))
    Toast.makeText(context, context.getString(R.string.copied_to_clipboard), Toast.LENGTH_SHORT).show()
}

private fun shareDua(context: Context, item: AdhkarItem) {
    val text = buildString {
        append(item.titleEn).append("\n\n")
        append(item.arabic)
        if (item.transliteration.isNotBlank()) append("\n\n").append(item.transliteration)
        append("\n\n").append(item.translationEn)
        append("\n\n").append(item.sourceEn)
    }
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, context.getString(R.string.share)))
}
