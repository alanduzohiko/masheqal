package com.masheqal.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.masheqal.app.MasheqalApp
import com.masheqal.app.R
import com.masheqal.app.data.QuranVerse

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuranPageScreen(app: MasheqalApp, nav: NavHostController, page: Int) {
    var verses by remember { mutableStateOf(emptyList<QuranVerse>()) }
    var currentJuz by remember { mutableStateOf(1) }
    var showTranslation by remember { mutableStateOf(false) }
    LaunchedEffect(page) {
        verses = app.quran.versesOfPage(page.coerceIn(1,604))
        currentJuz = verses.firstOrNull()?.let { app.quran.juzForVerse(it.surah,it.ayah) } ?: 1
    }
    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title={Text("${stringResource(R.string.page)} $page")},
            navigationIcon={IconButton(onClick={nav.popBackStack()}){Icon(Icons.Default.ArrowBack,null)}},
            actions={
                IconButton(onClick={ { showTranslation=!showTranslation } }) { Icon(if(showTranslation) Icons.Default.Visibility else Icons.Default.VisibilityOff,stringResource(R.string.show_translation)) }
                IconButton(onClick={ { if(page>1) nav.navigate("quran/page/${page-1}") } }, enabled=page>1){Icon(Icons.Default.ChevronLeft,null)}
                IconButton(onClick={ { if(page<604) nav.navigate("quran/page/${page+1}") } }, enabled=page<604){Icon(Icons.Default.ChevronRight,null)}
            }
        )
        Row(Modifier.fillMaxWidth().padding(horizontal=16.dp,vertical=6.dp),horizontalArrangement=Arrangement.SpaceBetween){Text("${stringResource(R.string.juz)} $currentJuz");Text("604 ${stringResource(R.string.pages_total)}",style=MaterialTheme.typography.labelSmall)}
        LazyColumn(contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
            items(verses) { v ->
                Column(Modifier.fillMaxWidth().padding(vertical=6.dp)) {
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text("${v.surah}:${v.ayah}",color=MaterialTheme.colorScheme.secondary,fontWeight=FontWeight.SemiBold)}
                    QuranText(v.text,size=27f,modifier=Modifier.fillMaxWidth())
                    if(showTranslation) Text(v.translationEn.orEmpty(),style=MaterialTheme.typography.bodyMedium,modifier=Modifier.padding(top=8.dp))
                }
            }
        }
    }
}
