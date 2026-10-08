package com.masheqal.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.masheqal.app.MasheqalApp
import com.masheqal.app.R
import kotlinx.coroutines.launch

@Composable
fun QuranReaderScreen(app: MasheqalApp, nav: NavHostController, surah: Int, initialAyah: Int) {
    var verses by remember { mutableStateOf(emptyList<com.masheqal.app.data.QuranVerse>()) }
    var selected by remember { mutableStateOf<com.masheqal.app.data.QuranVerse?>(null) }
    var showNote by remember { mutableStateOf(false) }
    var noteText by remember { mutableStateOf("") }
    var page by remember { mutableStateOf(1) }
    val listState = rememberLazyListState()
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    LaunchedEffect(surah) { verses = app.quran.versesOfSurah(surah) }
    LaunchedEffect(surah, initialAyah) { page = app.quran.pageForVerse(surah,initialAyah) }
    LaunchedEffect(verses, initialAyah) {
        val index = (initialAyah - 1).coerceIn(0, (verses.size - 1).coerceAtLeast(0))
        if (verses.isNotEmpty()) listState.scrollToItem(index)
    }
    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title={Text("سورە $surah")},
            navigationIcon={IconButton(onClick={nav.popBackStack()}){Icon(Icons.Default.ArrowBack,null)}},
            actions={Row { IconButton(onClick={ { nav.navigate("quran/page/$page") } }){Icon(Icons.Default.MenuBook,stringResource(R.string.page_view))}; IconButton(onClick={ { nav.navigate("search") } }){Icon(Icons.Default.Search,stringResource(R.string.search))} }}
        )
        LazyColumn(
            state=listState,
            contentPadding=PaddingValues(horizontal=16.dp, vertical=8.dp),
            verticalArrangement=Arrangement.spacedBy(12.dp)
        ) {
            items(verses) { verse ->
                Card(Modifier.fillMaxWidth(), onClick={ selected=verse; scope.launch { app.personal.setReading(surah,verse.ayah) } }) {
                    Column(Modifier.padding(18.dp)) {
                        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){
                            Text("$surah:${verse.ayah}",color=MaterialTheme.colorScheme.secondary,fontWeight=FontWeight.SemiBold)
                            Icon(Icons.Default.MoreHoriz,null)
                        }
                        Spacer(Modifier.height(10.dp)); QuranText(verse.text,size=27f); Spacer(Modifier.height(12.dp))
                        Text(verse.translationEn.orEmpty(),style=MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }
    }
    selected?.let { verse ->
        ModalBottomSheet(onDismissRequest={selected=null}) {
            Column(Modifier.fillMaxWidth().padding(20.dp)) {
                Text("$surah:${verse.ayah}",style=MaterialTheme.typography.titleMedium,color=MaterialTheme.colorScheme.secondary)
                QuranText(verse.text,size=24f)
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceEvenly){
                    IconButton(onClick={app.userDb.addBookmark("ayah","$surah:${verse.ayah}","$surah:${verse.ayah}");selected=null}){Icon(Icons.Default.BookmarkBorder,stringResource(R.string.bookmark))}
                    IconButton(onClick={showNote=true}){Icon(Icons.Default.Notes,stringResource(R.string.note))}
                    IconButton(onClick={shareText("${verse.text}\n\n${verse.translationEn}\n$surah:${verse.ayah}");selected=null}){Icon(Icons.Default.Share,stringResource(R.string.share))}
                    IconButton(onClick={
                        val uri=ShareCardUtils.createVerseCard(context,verse.text,verse.translationEn.orEmpty(),"$surah:${verse.ayah}")
                        ShareCardUtils.shareImage(context,uri); selected=null
                    }){Icon(Icons.Default.Image,stringResource(R.string.share_image))}
                }
                Spacer(Modifier.height(8.dp))
                Text(stringResource(R.string.source_modules_gated),style=MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(20.dp))
            }
        }
    }
    if (showNote && selected != null) {
        AlertDialog(
            onDismissRequest={showNote=false},
            title={Text(stringResource(R.string.note))},
            text={OutlinedTextField(noteText,{noteText=it},modifier=Modifier.fillMaxWidth(),minLines=4)},
            confirmButton={TextButton(onClick={app.userDb.addNote("$surah:${selected!!.ayah}",noteText);noteText="";showNote=false;selected=null}){Text(stringResource(R.string.done))}},
            dismissButton={TextButton(onClick={showNote=false}){Text(stringResource(R.string.cancel))}}
        )
    }
}
