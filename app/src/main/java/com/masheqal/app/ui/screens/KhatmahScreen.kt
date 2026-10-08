package com.masheqal.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.masheqal.app.MasheqalApp
import com.masheqal.app.R
import kotlin.math.ceil
import kotlinx.coroutines.launch

@Composable
fun KhatmahScreen(app: MasheqalApp, nav: NavHostController) {
    val state by app.personal.khatmah.collectAsState(initial = com.masheqal.app.data.KhatmahState())
    val scope = rememberCoroutineScope()
    Column(Modifier.fillMaxSize().padding(20.dp)) {
        Row(Modifier.fillMaxWidth()) { IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.Default.ArrowBack, null) }; Icon(Icons.Default.AutoStories, null, Modifier.padding(top = 14.dp)); Spacer(Modifier.width(10.dp)); Text(stringResource(R.string.khatmah), style = MaterialTheme.typography.headlineSmall) }
        Spacer(Modifier.height(20.dp))
        val progress = (state.readPages.toFloat() / state.targetPages.coerceAtLeast(1)).coerceIn(0f, 1f)
        LinearProgressIndicator(progress = { progress }, Modifier.fillMaxWidth())
        Spacer(Modifier.height(12.dp))
        Text("${state.readPages} / ${state.targetPages} ${stringResource(R.string.page)}", style = MaterialTheme.typography.titleLarge)
        Text("${(progress * 100).toInt()}%")
        Spacer(Modifier.height(16.dp))
        Text("${stringResource(R.string.daily_target)}: ${ceil(state.targetPages.toDouble() / state.days).toInt()} ${stringResource(R.string.page)}")
        Spacer(Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { scope.launch { app.personal.addKhatmahPages(1) } }) { Text("+1 ${stringResource(R.string.page)}") }
            OutlinedButton(onClick = { scope.launch { app.personal.pauseKhatmah() } }) { Icon(Icons.Default.PauseCircle, null); Spacer(Modifier.width(6.dp)); Text(stringResource(R.string.pause)) }
        }
        Spacer(Modifier.height(24.dp))
        if (!state.active) Text(stringResource(R.string.khatmah_paused))
        TextButton(onClick = { scope.launch { app.personal.setKhatmah(30, 604, 0, true) } }) { Text(stringResource(R.string.reset_khatmah)) }
    }
}
