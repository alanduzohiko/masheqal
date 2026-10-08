package com.masheqal.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.masheqal.app.R
import com.masheqal.app.domain.HijriCalculator
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun CalendarScreen(nav: NavHostController) {
    var selected by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }
    val date = remember(selected) { LocalDate.parse(selected) }
    val days = remember(date) { (-15..15).map { date.plusDays(it.toLong()) } }
    val formatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
    Column(Modifier.fillMaxSize().padding(14.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.Default.ArrowBack, null) }
            Text(stringResource(R.string.calendar), style = MaterialTheme.typography.headlineSmall)
        }
        Card(Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
            Column(Modifier.padding(18.dp)) {
                Text(date.format(formatter), style = MaterialTheme.typography.titleLarge)
                val h = HijriCalculator.fromGregorian(date)
                Text("${h.year}-${h.month}-${h.day} AH", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                Text(stringResource(R.string.hijri_calculated_note), style = MaterialTheme.typography.bodySmall)
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            IconButton(onClick = { selected = date.minusDays(1).toString() }) { Icon(Icons.Default.ChevronLeft, null) }
            OutlinedButton(onClick = { selected = LocalDate.now().toString() }) { Text(stringResource(R.string.today)) }
            IconButton(onClick = { selected = date.plusDays(1).toString() }) { Icon(Icons.Default.ChevronRight, null) }
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(7.dp), contentPadding = PaddingValues(vertical = 8.dp)) {
            items(days) { d ->
                val h = HijriCalculator.fromGregorian(d)
                val active = d == date
                Card(onClick = { selected = d.toString() }, modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(d.format(formatter), color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                        Text("${h.day}/${h.month}/${h.year}", color = MaterialTheme.colorScheme.secondary)
                    }
                }
            }
        }
    }
}
