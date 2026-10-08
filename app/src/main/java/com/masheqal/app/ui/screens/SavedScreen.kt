package com.masheqal.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.masheqal.app.MasheqalApp
import com.masheqal.app.R

@Composable
fun SavedScreen(app: MasheqalApp, nav: NavHostController) {
    var bookmarks by remember { mutableStateOf(app.userDb.listBookmarks()) }
    Column(Modifier.fillMaxSize().padding(horizontal = 12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
            IconButton(onClick = { nav.popBackStack() }) { androidx.compose.material3.Icon(Icons.Default.ArrowBack, null) }
            Text(stringResource(R.string.saved), style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(top = 12.dp))
        }
        if (bookmarks.isEmpty()) {
            MissingContentScreen(stringResource(R.string.saved), stringResource(R.string.no_saved_items), onBack = { nav.popBackStack() })
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
                items(bookmarks) { (reference, title) ->
                    FeatureCard(title = title, subtitle = reference, icon = Icons.Default.Bookmark, onClick = {
                        reference.substringBefore(":").toIntOrNull()?.let { s -> nav.navigate("quran/surah/$s") }
                    })
                }
            }
        }
    }
}
