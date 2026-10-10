package com.masheqal.app.ui.screens

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.masheqal.app.MasheqalApp
import com.masheqal.app.R
import com.masheqal.app.data.AllahName
import com.masheqal.app.data.NamesOfAllahPackage
import com.masheqal.app.data.NamesOfAllahProgressStore

private enum class NamesFilter { ALL, LEARNED, REMAINING, FAVORITES }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NamesOfAllahScreen(app: MasheqalApp, nav: NavHostController, initialQuery: String = "") {
    val context = androidx.compose.ui.platform.LocalContext.current
    val progressStore = remember(context) { NamesOfAllahProgressStore(context) }
    var packageData by remember { mutableStateOf<NamesOfAllahPackage?>(null) }
    var loading by remember { mutableStateOf(true) }
    var loadFailed by remember { mutableStateOf(false) }
    var retryNonce by remember { mutableIntStateOf(0) }
    var query by rememberSaveable(initialQuery) { mutableStateOf(initialQuery) }
    var filter by rememberSaveable { mutableStateOf(NamesFilter.ALL) }
    val learnedByNumber = remember { mutableStateMapOf<Int, Boolean>() }
    val favoriteByNumber = remember { mutableStateMapOf<Int, Boolean>() }

    LaunchedEffect(retryNonce) {
        loading = true
        loadFailed = false
        runCatching { app.namesOfAllah.load() }
            .onSuccess { loaded ->
                packageData = loaded
                learnedByNumber.clear()
                loaded.names.forEach { name ->
                    learnedByNumber[name.number] = progressStore.isLearned(name.number)
                    favoriteByNumber[name.number] = progressStore.isFavorite(name.number)
                }
            }
            .onFailure { loadFailed = true }
        loading = false
    }

    val allNames = packageData?.names.orEmpty()
    val learnedCount = allNames.count { learnedByNumber[it.number] == true }
    val normalizedQuery = query.trim().lowercase()
    val visibleNames = allNames.filter { name ->
        val learned = learnedByNumber[name.number] == true
        val matchesFilter = when (filter) {
            NamesFilter.ALL -> true
            NamesFilter.LEARNED -> learned
            NamesFilter.REMAINING -> !learned
            NamesFilter.FAVORITES -> favoriteByNumber[name.number] == true
        }
        val matchesQuery = normalizedQuery.isBlank() || GlobalSearchMatcher.matches(
            query,
            listOf(name.arabic, name.transliteration, name.meaning, name.description)
        )
        matchesFilter && matchesQuery
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(stringResource(R.string.names_of_allah))
                        Text(
                            stringResource(R.string.names_learned_count, learnedCount),
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
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text(stringResource(R.string.names_search_hint)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) }
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = filter == NamesFilter.ALL,
                    onClick = { filter = NamesFilter.ALL },
                    label = { Text(stringResource(R.string.names_filter_all)) }
                )
                FilterChip(
                    selected = filter == NamesFilter.LEARNED,
                    onClick = { filter = NamesFilter.LEARNED },
                    label = { Text(stringResource(R.string.names_filter_learned)) }
                )
                FilterChip(
                    selected = filter == NamesFilter.REMAINING,
                    onClick = { filter = NamesFilter.REMAINING },
                    label = { Text(stringResource(R.string.names_filter_remaining)) }
                )
                FilterChip(
                    selected = filter == NamesFilter.FAVORITES,
                    onClick = { filter = NamesFilter.FAVORITES },
                    label = { Text(stringResource(R.string.names_filter_favorites)) }
                )
            }
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { learnedCount / 99f },
                modifier = Modifier.fillMaxWidth()
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
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(42.dp))
                    Spacer(Modifier.height(12.dp))
                    Text(stringResource(R.string.adhkar_load_error), textAlign = TextAlign.Center)
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = { retryNonce++ }) {
                        Text(stringResource(R.string.adhkar_retry))
                    }
                }

                else -> LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item(key = "names-source-notice") {
                        Card(
                            shape = RoundedCornerShape(22.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer
                            )
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.School,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        stringResource(R.string.names_meaning_review_pending),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    stringResource(R.string.names_source_notice),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                packageData?.source?.let { source ->
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        "${source.license} · ${source.count} names · ${source.sourceCommit.take(7)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }
                        }
                    }

                    if (visibleNames.isEmpty()) {
                        item(key = "names-empty-state") {
                            Text(
                                stringResource(R.string.names_no_results),
                                modifier = Modifier.fillMaxWidth().padding(24.dp),
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    items(visibleNames, key = { it.number }) { name ->
                        val learned = learnedByNumber[name.number] == true
                        AllahNameCard(
                            name = name,
                            learned = learned,
                            favorite = favoriteByNumber[name.number] == true,
                            onFavoriteChange = { next ->
                                favoriteByNumber[name.number] = next
                                progressStore.setFavorite(name.number, next)
                            },
                            onShare = { shareAllahName(context, name) },
                            onLearnedChange = { next ->
                                learnedByNumber[name.number] = next
                                progressStore.setLearned(name.number, next)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AllahNameCard(
    name: AllahName,
    learned: Boolean,
    favorite: Boolean,
    onFavoriteChange: (Boolean) -> Unit,
    onShare: () -> Unit,
    onLearnedChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (learned) MaterialTheme.colorScheme.secondaryContainer
            else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(38.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            name.number.toString(),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    name.transliteration,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                IconButton(
                    onClick = { onFavoriteChange(!favorite) }
                ) {
                    Icon(
                        if (favorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = stringResource(
                            if (favorite) R.string.names_mark_unfavorite else R.string.names_mark_favorite
                        ),
                        tint = if (favorite) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Checkbox(
                    checked = learned,
                    onCheckedChange = onLearnedChange,
                    modifier = Modifier.size(42.dp)
                )
            }

            Spacer(Modifier.height(8.dp))
            Text(
                name.arabic,
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(10.dp))
            Text(
                name.meaning,
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )
            if (name.description.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    name.description,
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (name.references.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                Text(
                    "${stringResource(R.string.names_references)}: ${name.references.joinToString(" · ")}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                OutlinedButton(onClick = onShare) {
                    Icon(Icons.Default.Share, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.share))
                }
            }
        }
    }
}

private fun shareAllahName(context: Context, name: AllahName) {
    val text = buildString {
        append(name.arabic).append("\n")
        append(name.transliteration).append("\n\n")
        append(name.meaning).append("\n\n")
        append(context.getString(R.string.names_of_allah))
        append(" · Apache-2.0 dataset")
    }
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, context.getString(R.string.share)))
}
