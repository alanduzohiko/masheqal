package com.masheqal.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
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
    val targetPages = state.targetPages.coerceAtLeast(1)
    val days = state.days.coerceAtLeast(1)
    val progress = (state.readPages.toFloat() / targetPages).coerceIn(0f, 1f)
    val dailyTarget = ceil(targetPages.toDouble() / days).toInt()

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { nav.popBackStack() }) {
                Icon(Icons.Default.ArrowBack, null)
            }
            Icon(Icons.Default.AutoStories, null)
            Spacer(Modifier.width(10.dp))
            Text(stringResource(R.string.khatmah), style = MaterialTheme.typography.headlineSmall)
        }

        Spacer(Modifier.height(14.dp))

        Card(
            Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(Modifier.padding(22.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        "${state.readPages} / ${targetPages}",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "${(progress * 100).toInt()}%",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Spacer(Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth(),
                    trackColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.55f)
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    "${stringResource(R.string.daily_target)}: $dailyTarget ${stringResource(R.string.page)}",
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        Spacer(Modifier.height(18.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = { scope.launch { app.personal.addKhatmahPages(1) } },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Add, null)
                Spacer(Modifier.width(6.dp))
                Text("+1 ${stringResource(R.string.page)}")
            }

            OutlinedButton(
                onClick = {
                    scope.launch {
                        if (state.active) {
                            app.personal.pauseKhatmah()
                        } else {
                            app.personal.setKhatmah(state.days, state.targetPages, state.readPages, true)
                        }
                    }
                },
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    if (state.active) Icons.Default.PauseCircle else Icons.Default.PlayArrow,
                    null
                )
                Spacer(Modifier.width(6.dp))
                Text(if (state.active) stringResource(R.string.pause) else stringResource(R.string.resume))
            }
        }

        Spacer(Modifier.height(14.dp))

        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp)) {
            Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                IconBadge(
                    if (state.active) Icons.Default.CheckCircle else Icons.Default.PauseCircle,
                    emphasized = state.active
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        if (state.active) stringResource(R.string.in_progress)
                        else stringResource(R.string.khatmah_paused),
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        stringResource(R.string.quran),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        TextButton(
            onClick = { scope.launch { app.personal.setKhatmah(30, 604, 0, true) } },
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text(stringResource(R.string.reset_khatmah))
        }
    }
}