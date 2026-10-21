package com.masheqal.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.masheqal.app.MasheqalApp
import com.masheqal.app.R
import com.masheqal.app.data.SettingsState
import dev.kosrat.muslimdata.models.AzkarCategory
import dev.kosrat.muslimdata.models.AzkarChapter
import dev.kosrat.muslimdata.models.AzkarItem
import dev.kosrat.muslimdata.models.Language
import dev.kosrat.muslimdata.repository.MuslimRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun HisnScreen(app: MasheqalApp, nav: NavHostController) {
    val settings by app.settings.state.collectAsState(initial = SettingsState())
    val context = LocalContext.current
    val language = remember(settings.language) {
        when (settings.language) {
            "ar" -> Language.AR
            "ckb" -> Language.CKB
            else -> Language.EN
        }
    }

    var selectedCategory by remember { mutableStateOf<AzkarCategory?>(null) }
    var selectedChapter by remember { mutableStateOf<AzkarChapter?>(null) }
    var categories by remember { mutableStateOf<List<AzkarCategory>>(emptyList()) }
    var chapters by remember { mutableStateOf<List<AzkarChapter>>(emptyList()) }
    var entries by remember { mutableStateOf<List<AzkarItem>>(emptyList()) }
    var query by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var retryKey by remember { mutableStateOf(0) }

    LaunchedEffect(language, selectedCategory?.categoryId, selectedChapter?.chapterId, retryKey) {
        isLoading = true
        errorMessage = null
        try {
            val repository = MuslimRepository(context.applicationContext)
            when {
                selectedChapter != null -> {
                    entries = withContext(Dispatchers.IO) {
                        repository.getAzkarItems(selectedChapter!!.chapterId, language).orEmpty()
                    }
                }
                selectedCategory != null -> {
                    chapters = withContext(Dispatchers.IO) {
                        repository.getAzkarChapters(language, selectedCategory!!.categoryId).orEmpty()
                    }
                }
                else -> {
                    categories = withContext(Dispatchers.IO) {
                        repository.getAzkarCategories(language)
                    }
                }
            }
        } catch (exception: Exception) {
            errorMessage = when (settings.language) {
                "ar" -> "تعذّر تحميل المحتوى. أعد المحاولة."
                "ckb" -> "نەتوانرا ناوەڕۆکەکە باربکرێت. دووبارە هەوڵ بدەوە."
                else -> "Could not load this content. Please try again."
            }
        } finally {
            isLoading = false
        }
    }

    val term = query.trim()
    val visibleCategories = remember(categories, term) {
        if (term.isBlank()) categories else categories.filter { it.categoryName.contains(term, true) }
    }
    val visibleChapters = remember(chapters, term) {
        if (term.isBlank()) chapters else chapters.filter { it.chapterName.contains(term, true) }
    }
    val visibleEntries = remember(entries, term) {
        if (term.isBlank()) entries else entries.filter {
            it.item.contains(term, true) ||
                it.translation.contains(term, true) ||
                it.reference.contains(term, true) ||
                it.itemId.toString() == term
        }
    }

    val screenTitle = selectedChapter?.chapterName
        ?: selectedCategory?.categoryName
        ?: stringResource(R.string.hisn)
    val searchHint = when (settings.language) {
        "ar" -> "ابحث في العنوان أو الذكر أو المعنى"
        "ckb" -> "بگەڕێ لە ناونیشان، زیکر یان واتا"
        else -> "Search title, remembrance, or meaning"
    }
    val retryLabel = when (settings.language) {
        "ar" -> "إعادة المحاولة"
        "ckb" -> "دووبارە هەوڵ بدەوە"
        else -> "Try again"
    }

    fun goBackOneLevel() {
        when {
            selectedChapter != null -> selectedChapter = null
            selectedCategory != null -> selectedCategory = null
            else -> nav.popBackStack()
        }
        query = ""
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { goBackOneLevel() }) {
                Icon(Icons.Default.ArrowBack, contentDescription = null)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = screenTitle,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = when (settings.language) {
                        "ar" -> "نصوص الأذكار مع الترجمة والمراجع"
                        "ckb" -> "دەقی زیکرەکان لەگەڵ وەرگێڕان و سەرچاوە"
                        else -> "Remembrances with translations and references"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            placeholder = { Text(searchHint) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            shape = RoundedCornerShape(18.dp)
        )

        when {
            isLoading -> Box(
                modifier = Modifier.fillMaxSize().padding(bottom = 64.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }

            errorMessage != null -> Column(
                modifier = Modifier.fillMaxWidth().padding(top = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = errorMessage.orEmpty(),
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
                Button(onClick = { retryKey += 1 }) { Text(retryLabel) }
            }

            selectedChapter != null -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(visibleEntries, key = { it.itemId }) { entry ->
                    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = entry.item,
                                modifier = Modifier.fillMaxWidth(),
                                fontSize = 23.sp,
                                lineHeight = 38.sp,
                                textAlign = TextAlign.End,
                                color = MaterialTheme.colorScheme.primary
                            )
                            if (entry.translation.isNotBlank()) {
                                Text(
                                    text = entry.translation,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            if (entry.reference.isNotBlank()) {
                                Text(
                                    text = entry.reference,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            selectedCategory != null -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(visibleChapters, key = { it.chapterId }) { chapter ->
                    HisnNavigationCard(
                        title = chapter.chapterName,
                        subtitle = chapter.chapterId.toString(),
                        number = chapter.chapterId,
                        onClick = {
                            selectedChapter = chapter
                            query = ""
                        }
                    )
                }
            }

            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(visibleCategories, key = { it.categoryId }) { category ->
                    HisnNavigationCard(
                        title = category.categoryName,
                        subtitle = category.categoryId.toString(),
                        number = category.categoryId,
                        onClick = {
                            selectedCategory = category
                            query = ""
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun HisnNavigationCard(
    title: String,
    subtitle: String,
    number: Int,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(42.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = number.toString(),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null)
        }
    }
}
