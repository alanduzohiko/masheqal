package com.masheqal.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.masheqal.app.MasheqalApp
import com.masheqal.app.R
import com.masheqal.app.data.QuranRepository
import kotlinx.coroutines.delay

private sealed class SearchItem {
    data class Quran(val hit: com.masheqal.app.data.SearchHit):SearchItem()
    data class Bookmark(val ref:String,val title:String):SearchItem()
    data class Note(val ref:String,val text:String):SearchItem()
}

@Composable
fun SearchScreen(app: MasheqalApp, nav: NavHostController, initialQuery: String = "") {
    var query by rememberSaveable(initialQuery) { mutableStateOf(initialQuery) }
    var quranHits by remember { mutableStateOf(emptyList<com.masheqal.app.data.SearchHit>()) }
    var bookmarkHits by remember { mutableStateOf(emptyList<com.masheqal.app.data.UserDatabase.BookmarkRecord>()) }
    var noteHits by remember { mutableStateOf(emptyList<com.masheqal.app.data.UserDatabase.NoteRecord>()) }
    LaunchedEffect(query) {
        if (query.isNotBlank()) {
            delay(180)
            quranHits=app.quran.search(query)
            bookmarkHits=app.userDb.searchBookmarks(query)
            noteHits=app.userDb.searchNotes(query)
        } else { quranHits=emptyList(); bookmarkHits=emptyList(); noteHits=emptyList() }
    }
    val total=quranHits.size+bookmarkHits.size+noteHits.size
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(12.dp),horizontalArrangement=Arrangement.spacedBy(8.dp),verticalAlignment=Alignment.CenterVertically){
            IconButton(onClick={nav.popBackStack()}){Icon(Icons.Default.ArrowBack,null)}
            OutlinedTextField(query,{query=it},Modifier.weight(1f),singleLine=true,placeholder={Text(stringResource(R.string.search_across_content))},leadingIcon={Icon(Icons.Default.Search,null)})
        }
        if(query.isBlank()) Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center){Text(stringResource(R.string.search_across_content))}
        else if(total==0) Box(Modifier.fillMaxSize().padding(24.dp),contentAlignment=Alignment.Center){Text(stringResource(R.string.no_results))}
        else LazyColumn(contentPadding=PaddingValues(12.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
            if(quranHits.isNotEmpty()) item{Text(stringResource(R.string.quran),style=MaterialTheme.typography.titleMedium)}
            items(quranHits){h->Card(onClick={nav.navigate("quran/ref/${h.verse.surah}/${h.verse.ayah}")},Modifier.fillMaxWidth()){Column(Modifier.padding(16.dp)){Text("${h.verse.surah}:${h.verse.ayah}",color=MaterialTheme.colorScheme.secondary);QuranText(h.verse.text,size=22f);Spacer(Modifier.height(8.dp));Text(h.verse.translationEn.orEmpty(),style=MaterialTheme.typography.bodyMedium)}}}
            if(bookmarkHits.isNotEmpty()) item{Text(stringResource(R.string.bookmarks),style=MaterialTheme.typography.titleMedium)}
            items(bookmarkHits){b->FeatureCard(b.title,b.reference,Icons.Default.Bookmark,onClick={b.reference.substringBefore(":").toIntOrNull()?.let{nav.navigate("quran/surah/$it")}})}
            if(noteHits.isNotEmpty()) item{Text(stringResource(R.string.notes),style=MaterialTheme.typography.titleMedium)}
            items(noteHits){n->Card(Modifier.fillMaxWidth()){Column(Modifier.padding(16.dp)){Text(n.reference,color=MaterialTheme.colorScheme.secondary);Text(n.body)}}}
        }
    }
}
