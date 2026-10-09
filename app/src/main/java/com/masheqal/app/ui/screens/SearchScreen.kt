package com.masheqal.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.masheqal.app.MasheqalApp
import com.masheqal.app.R
import com.masheqal.app.data.SettingsState
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
    val settings by app.settings.state.collectAsState(initial = SettingsState())
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
            items(quranHits) { hit ->
                Card(
                    onClick = {
                        nav.navigate("quran/ref/${hit.verse.surah}/${hit.verse.ayah}")
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            "${hit.verse.surah}:${hit.verse.ayah}",
                            color = MaterialTheme.colorScheme.secondary
                        )
                        QuranText(hit.verse.text, size = 22f)
                        Spacer(Modifier.height(8.dp))
                        val translation = hit.verse.translationFor(settings.language)
                        if (!translation.isNullOrBlank()) {
                            Text(translation, style = MaterialTheme.typography.bodyMedium)
                        } else {
                            Text(
                                stringResource(R.string.translation_unavailable),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            if(bookmarkHits.isNotEmpty()) item{Text(stringResource(R.string.bookmarks),style=MaterialTheme.typography.titleMedium)}
            items(bookmarkHits) { bookmark ->
                FeatureCard(
                    bookmark.title,
                    bookmark.reference,
                    Icons.Default.Bookmark,
                    onClick = {
                        val parts = bookmark.reference.trim().split(':')
                        val surah = parts.getOrNull(0)?.toIntOrNull()
                        val ayah = parts.getOrNull(1)?.toIntOrNull()
                        if (surah != null && surah in 1..114) {
                            if (ayah != null && ayah > 0) {
                                nav.navigate("quran/ref/$surah/$ayah")
                            } else {
                                nav.navigate("quran/surah/$surah")
                            }
                        }
                    }
                )
            }
            if(noteHits.isNotEmpty()) item{Text(stringResource(R.string.notes),style=MaterialTheme.typography.titleMedium)}
            items(noteHits) { note ->
                Card(
                    onClick = { nav.navigate("notes") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(note.reference, color = MaterialTheme.colorScheme.secondary)
                        Text(note.body)
                    }
                }
            }
        }
    }
}
