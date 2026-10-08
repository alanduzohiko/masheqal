package com.masheqal.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.masheqal.app.MasheqalApp
import com.masheqal.app.R
import kotlinx.coroutines.launch

@Composable
fun TasbihScreen(app: MasheqalApp, nav: NavHostController) {
    val settings by app.settings.state.collectAsState(initial = com.masheqal.app.data.SettingsState())
    val scope = rememberCoroutineScope()
    val target = 33
    Column(Modifier.fillMaxSize().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.Default.ArrowBack, null) }
            Text(stringResource(R.string.tasbih), style = MaterialTheme.typography.headlineSmall)
        }
        Spacer(Modifier.height(40.dp))
        Text("${settings.tasbihCount}", style = MaterialTheme.typography.displayLarge, color = MaterialTheme.colorScheme.primary)
        Text("/ $target", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(25.dp))
        Button(
            onClick = { scope.launch { app.settings.setTasbih(settings.tasbihCount + 1) } },
            modifier = Modifier.size(190.dp),
            shape = CircleShape
        ) { Text("ذکر\n+1", style = MaterialTheme.typography.headlineMedium) }
        Spacer(Modifier.height(18.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            IconButton(onClick = { scope.launch { app.settings.setTasbih(settings.tasbihCount - 1) } }) { Icon(Icons.Default.Undo, stringResource(R.string.undo)) }
            IconButton(onClick = { scope.launch { app.settings.setTasbih(0) } }) { Icon(Icons.Default.RestartAlt, stringResource(R.string.reset)) }
        }
        Spacer(Modifier.height(20.dp))
        Text(if (settings.tasbihCount >= target) stringResource(R.string.complete) else stringResource(R.string.in_progress))
    }
}
