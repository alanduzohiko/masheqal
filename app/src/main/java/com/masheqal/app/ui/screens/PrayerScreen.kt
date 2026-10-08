package com.masheqal.app.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.masheqal.app.MasheqalApp
import com.masheqal.app.R
import com.masheqal.app.domain.*
import com.masheqal.app.services.PrayerNotificationScheduler
import com.masheqal.app.util.LocationUtils
import java.time.LocalDate
import java.time.ZoneId

@Composable
fun PrayerScreen(app: MasheqalApp, nav: NavHostController, onRequestLocation: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var location by remember { mutableStateOf(LocationUtils.lastKnown(context)) }
    var times by remember { mutableStateOf<PrayerTimes?>(null) }
    val settings by app.settings.state.collectAsState(initial = com.masheqal.app.data.SettingsState())
    LaunchedEffect(location, settings.prayerMethod, settings.madhhab) {
        location?.let { c ->
            val method = PrayerMethod.valueOf(settings.prayerMethod)
            val madhhab = AsrMadhhab.valueOf(settings.madhhab)
            val zone = ZoneId.systemDefault()
            val offset = LocalDate.now().atStartOfDay(zone).offset.totalSeconds / 3600.0
            times = PrayerCalculator.calculate(
                LocalDate.now(),
                Coordinates(c.latitude, c.longitude, offset),
                method,
                madhhab
            )
            PrayerNotificationScheduler.storeConfig(context, c.latitude, c.longitude, method, madhhab)
        }
    }
    val request = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        location = LocationUtils.lastKnown(context)
    }
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text(stringResource(R.string.prayer), style = MaterialTheme.typography.headlineSmall)
                Text(LocalDate.now().toString(), style = MaterialTheme.typography.bodySmall)
            }
            IconButton(onClick = { nav.navigate("qibla") }) { Icon(Icons.Default.Explore, stringResource(R.string.qibla)) }
        }
        if (location == null) {
            Card(Modifier.fillMaxWidth().padding(top = 12.dp)) {
                Column(Modifier.padding(18.dp)) {
                    Icon(Icons.Default.LocationOn, null)
                    Text(stringResource(R.string.location_needed))
                    Spacer(Modifier.height(10.dp))
                    Button(onClick = { request.launch(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION)) }) {
                        Text(stringResource(R.string.set_location))
                    }
                }
            }
        } else times?.let { t ->
            val rows = listOf(
                stringResource(R.string.fajr) to t.fajr,
                stringResource(R.string.sunrise) to t.sunrise,
                stringResource(R.string.dhuhr) to t.dhuhr,
                stringResource(R.string.asr) to t.asr,
                stringResource(R.string.maghrib) to t.maghrib,
                stringResource(R.string.isha) to t.isha
            )
            LazyColumn(
                contentPadding = PaddingValues(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(rows) { r ->
                    Card(Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(18.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(r.first, style = MaterialTheme.typography.titleMedium)
                            Text(formatMinutes(r.second), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
            Button(
                onClick = {
                    PrayerNotificationScheduler.storeConfig(
                        context,
                        location!!.latitude,
                        location!!.longitude,
                        PrayerMethod.valueOf(settings.prayerMethod),
                        AsrMadhhab.valueOf(settings.madhhab)
                    )
                    PrayerNotificationScheduler.scheduleToday(context, t)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Notifications, null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.schedule_reminders))
            }
        }
    }
}

private fun formatMinutes(v: Double): String {
    val total = kotlin.math.round(v).toInt()
    val h = (total / 60) % 24
    val m = total % 60
    return "%02d:%02d".format(h, m)
}
