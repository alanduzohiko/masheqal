package com.masheqal.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.masheqal.app.MasheqalApp
import com.masheqal.app.R

@Composable
fun NotesScreen(app: MasheqalApp, nav: NavHostController) {
    var notes by remember { mutableStateOf(app.userDb.listNotes()) }
    var showAdd by remember { mutableStateOf(false) }
    var body by remember { mutableStateOf("") }
    var reference by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().padding(horizontal = 12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Row { IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.Default.ArrowBack, null) }; Text(stringResource(R.string.notes), style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(top = 12.dp)) }
            IconButton(onClick = { showAdd = true }) { Icon(Icons.Default.Add, stringResource(R.string.note)) }
        }
        if (notes.isEmpty()) {
            MissingContentScreen(stringResource(R.string.notes), stringResource(R.string.no_notes), onBack = { nav.popBackStack() })
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
                items(notes) { (ref, text, tags) ->
                    Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp)) { Text(ref, color = MaterialTheme.colorScheme.secondary); Text(text); if (tags.isNotBlank()) Text(tags, style = MaterialTheme.typography.labelSmall) } }
                }
            }
        }
    }
    if (showAdd) {
        AlertDialog(
            onDismissRequest = { showAdd = false },
            title = { Text(stringResource(R.string.new_note)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(reference, { reference = it }, label = { Text(stringResource(R.string.reference_optional)) }, singleLine = true)
                    OutlinedTextField(body, { body = it }, label = { Text(stringResource(R.string.note)) }, minLines = 5)
                }
            },
            confirmButton = { TextButton(onClick = { if (body.isNotBlank()) { app.userDb.addNote(reference.ifBlank { "personal" }, body); notes = app.userDb.listNotes(); body = ""; reference = ""; showAdd = false } }) { Text(stringResource(R.string.save)) } },
            dismissButton = { TextButton(onClick = { showAdd = false }) { Text(stringResource(R.string.cancel)) } }
        )
    }
}
