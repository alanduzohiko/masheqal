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
import androidx.compose.material3.TextButton
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
import dev.kosrat.muslimdata.models.Language
import dev.kosrat.muslimdata.models.NameOfAllah
import dev.kosrat.muslimdata.repository.MuslimRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun NamesOfAllahScreen(app: MasheqalApp, nav: NavHostController) {
    val settings by app.settings.state.collectAsState(initial = SettingsState())
    val context = LocalContext.current
    val language = remember(settings.language) {
        when (settings.language) {
            "ar" -> Language.AR
            "ckb" -> Language.CKB
            else -> Language.EN
        }
    }

    var names by remember { mutableStateOf<List<NameOfAllah>>(emptyList()) }
    var query by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var retryKey by remember { mutableStateOf(0) }

    LaunchedEffect(language, retryKey) {
        isLoading = true
        errorMessage = null
        try {
            names = withContext(Dispatchers.IO) {
                MuslimRepository(context.applicationContext).getNamesOfAllah(language)
            }
            if (names.isEmpty()) {
                errorMessage = when (settings.language) {
                    "ar" -> "لم تُرجع قاعدة البيانات أسماءً. جرّب إعادة التحميل."
                    "ckb" -> "ناوەکان لە بنکەدراوەکە نەدۆزرایەوە. دووبارە هەوڵ بدەوە."
                    else -> "No names were returned. Please try again."
                }
            }
        } catch (exception: Exception) {
            names = emptyList()
            errorMessage = when (settings.language) {
                "ar" -> "تعذّر تحميل الأسماء. تحقّق من بيانات التطبيق ثم حاول مجددًا."
                "ckb" -> "نەتوانرا ناوەکان باربکرێن. تکایە دووبارە هەوڵ بدەوە."
                else -> "Could not load the names. Please try again."
            }
        } finally {
            isLoading = false
        }
    }

    val filteredNames = remember(names, query) {
        val term = query.trim()
        if (term.isEmpty()) names else names.filter { entry ->
            entry.number.toString() == term ||
                entry.name.contains(term, ignoreCase = true) ||
                entry.translation.contains(term, ignoreCase = true)
        }
    }

    val subtitle = when (settings.language) {
        "ar" -> "الأسماء الحسنى ومعانيها وترجماتها"
        "ckb" -> "ناوە جوانەکانی خوای گەورە و واتاکانیان"
        else -> "The beautiful names of Allah with their meanings"
    }
    val searchHint = when (settings.language) {
        "ar" -> "ابحث بالاسم أو المعنى أو الرقم"
        "ckb" -> "بگەڕێ بە ناو، واتا یان ژمارە"
        else -> "Search by name, meaning, or number"
    }
    val retryLabel = when (settings.language) {
        "ar" -> "إعادة المحاولة"
        "ckb" -> "دووبارە هەوڵ بدەوە"
        else -> "Try again"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { nav.popBackStack() }) {
                Icon(Icons.Default.ArrowBack, contentDescription = null)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.names_of_allah),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            placeholder = { Text(searchHint) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            shape = RoundedCornerShape(18.dp)
        )

        when {
            isLoading -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 64.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }

            errorMessage != null -> Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = errorMessage.orEmpty(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
                Button(onClick = { retryKey += 1 }) {
                    Text(retryLabel)
                }
            }

            filteredNames.isEmpty() -> Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 30.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.no_results),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredNames, key = { it.number }) { entry ->
                    NameOfAllahCard(entry)
                }
            }
        }
    }
}

@Composable
private fun NameOfAllahCard(entry: NameOfAllah) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(44.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = entry.number.toString(),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.name,
                    modifier = Modifier.fillMaxWidth(),
                    fontSize = 24.sp,
                    lineHeight = 34.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.End,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = entry.translation,
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Start,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
