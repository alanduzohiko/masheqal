
package com.masheqal.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
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
    val count = settings.tasbihCount.coerceAtLeast(0)

    Column(
        Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { nav.popBackStack() }) {
                Icon(Icons.Default.ArrowBack, contentDescription = null)
            }
            Text(
                stringResource(R.string.tasbih),
                style = MaterialTheme.typography.headlineSmall
            )
        }

        Spacer(Modifier.height(26.dp))

        Card(
            Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp)
        ) {
            Column(
                Modifier.fillMaxWidth().padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    stringResource(R.string.tasbih),
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "$count",
                    style = MaterialTheme.typography.displayLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "/ $target",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(20.dp))
                LinearProgressIndicator(
                    progress = { (count.toFloat() / target).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    if (count >= target) stringResource(R.string.complete)
                    else stringResource(R.string.in_progress),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.height(26.dp))

        Button(
            onClick = {
                scope.launch { app.settings.setTasbih(count + 1) }
            },
            modifier = Modifier.size(210.dp),
            shape = CircleShape,
            contentPadding = PaddingValues(0.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.TouchApp, null, Modifier.size(34.dp))
                Spacer(Modifier.height(5.dp))
                Text("ذکر", style = MaterialTheme.typography.titleLarge)
                Text("+1", style = MaterialTheme.typography.headlineMedium)
            }
        }

        Spacer(Modifier.height(20.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(
                onClick = {
                    scope.launch { app.settings.setTasbih((count - 1).coerceAtLeast(0)) }
                }
            ) {
                Icon(Icons.Default.Undo, stringResource(R.string.undo))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.undo))
            }
            OutlinedButton(
                onClick = {
                    scope.launch { app.settings.setTasbih(0) }
                }
            ) {
                Icon(Icons.Default.RestartAlt, stringResource(R.string.reset))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.reset))
            }
        }

        Spacer(Modifier.height(18.dp))

        Text(
            stringResource(R.string.local_only),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
