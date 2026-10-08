package com.masheqal.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.masheqal.app.MasheqalApp
import com.masheqal.app.R
import com.masheqal.app.data.ContentPackageManager

@Composable
fun ContentCenterScreen(nav: NavHostController) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val installed = ContentPackageManager(context).listInstalled()
    val builtIn = listOf(
        stringResource(R.string.quran_arabic) to stringResource(R.string.installed),
        stringResource(R.string.quran_english) to stringResource(R.string.installed),
        stringResource(R.string.quran_font) to stringResource(R.string.installed)
    )
    val gated = listOf(R.string.quran_sorani, R.string.tafsir, R.string.hadith, R.string.adhkar, R.string.hisn, R.string.dua, R.string.names_of_allah, R.string.audio)
    Column(Modifier.fillMaxSize().padding(horizontal=16.dp)) {
        Row(Modifier.fillMaxWidth()) { IconButton(onClick={nav.popBackStack()}){Icon(Icons.Default.ArrowBack,null)}; Text(stringResource(R.string.content_center),style=MaterialTheme.typography.headlineSmall,modifier=Modifier.padding(top=12.dp)) }
        Text(stringResource(R.string.content_policy), style=MaterialTheme.typography.bodyMedium, modifier=Modifier.padding(vertical=10.dp))
        LazyColumn(verticalArrangement=Arrangement.spacedBy(8.dp), contentPadding=PaddingValues(bottom=24.dp)) {
            item { Text(stringResource(R.string.bundled),style=MaterialTheme.typography.titleMedium) }
            items(builtIn) { (name,status) -> ListItem(headlineContent={Text(name)},supportingContent={Text(status)},leadingContent={Icon(Icons.Default.CheckCircle,null)}) }
            item { Spacer(Modifier.height(8.dp)); Text(stringResource(R.string.external_packages),style=MaterialTheme.typography.titleMedium) }
            items(gated) { id -> ListItem(headlineContent={Text(stringResource(id))},supportingContent={Text(stringResource(R.string.source_required))},leadingContent={Icon(Icons.Default.Inventory2,null)}) }
            if (installed.isNotEmpty()) {
                item { Spacer(Modifier.height(8.dp)); Text(stringResource(R.string.installed_packages),style=MaterialTheme.typography.titleMedium) }
                items(installed) { p -> ListItem(headlineContent={Text(p.title)}, supportingContent={Text("${p.type} • ${p.version} • ${p.license}")}) }
            }
        }
    }
}
