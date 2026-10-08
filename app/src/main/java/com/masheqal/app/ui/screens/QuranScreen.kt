package com.masheqal.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.masheqal.app.MasheqalApp
import com.masheqal.app.R
import kotlinx.coroutines.launch

@Composable
fun QuranScreen(app: MasheqalApp, nav: NavHostController) {
    var surahs by remember { mutableStateOf(emptyList<com.masheqal.app.data.SurahMeta>()) }
    var query by remember { mutableStateOf("") }
    val scope=rememberCoroutineScope()
    LaunchedEffect(Unit) { surahs=app.quran.loadSurahs() }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(query,{query=it},Modifier.weight(1f),singleLine=true,placeholder={Text(stringResource(R.string.search_hint))},leadingIcon={Icon(Icons.Default.Search,null)})
            IconButton(onClick={ { nav.navigate("search") } }) { Icon(Icons.Default.Search,null) }
        }
        LazyColumn(contentPadding=PaddingValues(12.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
            items(if(query.isBlank()) surahs else surahs.filter { it.nameAr.contains(query) || it.nameEn.contains(query, true) || it.number.toString()==query }) { s ->
                Card(onClick={nav.navigate("quran/surah/${s.number}")}, modifier=Modifier.fillMaxWidth()) { Row(Modifier.padding(16.dp), horizontalArrangement=Arrangement.SpaceBetween) { Column(Modifier.weight(1f)) { Text(s.nameAr,style=MaterialTheme.typography.titleMedium); Text(s.nameEn,style=MaterialTheme.typography.bodySmall); Text("${s.ayahCount} • ${s.revelation}",style=MaterialTheme.typography.labelSmall) }; Text(s.number.toString(),style=MaterialTheme.typography.titleLarge,color=MaterialTheme.colorScheme.primary) } }
            }
        }
    }
}
