
package com.masheqal.app.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.masheqal.app.MasheqalApp
import com.masheqal.app.data.AdhanRecording
import com.masheqal.app.data.AdhanRepository
import com.masheqal.app.data.LocationTimeZoneRepository
import com.masheqal.app.R
import kotlinx.coroutines.launch
import com.masheqal.app.domain.*
import com.masheqal.app.services.PrayerNotificationScheduler
import com.masheqal.app.util.LocationUtils
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import java.time.ZonedDateTime
import java.time.ZoneId
import kotlinx.coroutines.delay

@Composable
fun PrayerScreen(app: MasheqalApp, nav: NavHostController, onRequestLocation: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val deviceLocale = LocalConfiguration.current.locales[0] ?: Locale.ROOT
    var location by remember { mutableStateOf(LocationUtils.lastKnown(context)) }
    var locationZone by remember { mutableStateOf(ZoneId.systemDefault()) }
    val timeZoneRepository = remember(context) { LocationTimeZoneRepository(context) }
    var times by remember { mutableStateOf<PrayerTimes?>(null) }
    val settings by app.settings.state.collectAsState(initial = com.masheqal.app.data.SettingsState())
    val scope = rememberCoroutineScope()
    var adhanRecordings by remember { mutableStateOf(emptyList<AdhanRecording>()) }

    LaunchedEffect(Unit) {
        adhanRecordings = runCatching { AdhanRepository(context).loadCatalog() }.getOrDefault(emptyList())
    }
    val selectedAdhan = adhanRecordings.firstOrNull { it.id == settings.adhanRecordingId }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        scope.launch {
            location = LocationUtils.current(context) ?: LocationUtils.lastKnown(context)
        }
    }

    LaunchedEffect(location, settings.prayerMethod, settings.madhhab, settings.prayerRemindersEnabled) {
        val c = location
        if (c == null) {
            times = null
        } else {
            val method = PrayerMethod.valueOf(settings.prayerMethod)
            val madhhab = AsrMadhhab.valueOf(settings.madhhab)
            val resolvedZone = timeZoneRepository.resolve(c.latitude, c.longitude)
            locationZone = resolvedZone
            val localDate = LocalDate.now(resolvedZone)
            val offset = localDate.atTime(12, 0).atZone(resolvedZone).offset.totalSeconds / 3600.0
            val calculated = PrayerCalculator.calculate(
                localDate,
                Coordinates(c.latitude, c.longitude, offset),
                method,
                madhhab
            )
            times = calculated
            PrayerNotificationScheduler.storeConfig(
                context,
                c.latitude,
                c.longitude,
                method,
                madhhab,
                resolvedZone.id
            )
            if (settings.prayerRemindersEnabled) {
                PrayerNotificationScheduler.scheduleToday(context, calculated, resolvedZone.id)
            }
        }
    }

    LaunchedEffect(settings.adhanRecordingId, settings.playFullAdhan, adhanRecordings) {
        selectedAdhan?.let { recording ->
            PrayerNotificationScheduler.configureAdhan(
                context,
                recording.fileName,
                recording.displayTitle,
                settings.playFullAdhan
            )
        }
    }

    val allRows = times?.let {
        listOf(
            stringResource(R.string.fajr) to it.fajr,
            stringResource(R.string.sunrise) to it.sunrise,
            stringResource(R.string.dhuhr) to it.dhuhr,
            stringResource(R.string.asr) to it.asr,
            stringResource(R.string.maghrib) to it.maghrib,
            stringResource(R.string.isha) to it.isha
        )
    }.orEmpty()

    val next = times?.let { t ->
        val prayerRows = listOf(
            stringResource(R.string.fajr) to t.fajr,
            stringResource(R.string.dhuhr) to t.dhuhr,
            stringResource(R.string.asr) to t.asr,
            stringResource(R.string.maghrib) to t.maghrib,
            stringResource(R.string.isha) to t.isha
        )
        val now = ZonedDateTime.now(locationZone).let {
            it.hour * 60.0 + it.minute + it.second / 60.0
        }
        prayerRows.firstOrNull { it.second >= now } ?: prayerRows.firstOrNull()
    }

    val enableRemindersNow: () -> Unit = {
        val coordinates = location
        val prayerTimes = times
        if (coordinates != null && prayerTimes != null) {
            PrayerNotificationScheduler.storeConfig(
                context,
                coordinates.latitude,
                coordinates.longitude,
                PrayerMethod.valueOf(settings.prayerMethod),
                AsrMadhhab.valueOf(settings.madhhab),
                locationZone.id
            )
            selectedAdhan?.let { recording ->
                PrayerNotificationScheduler.configureAdhan(
                    context,
                    recording.fileName,
                    recording.displayTitle,
                    settings.playFullAdhan
                )
            }
            PrayerNotificationScheduler.scheduleToday(context, prayerTimes, locationZone.id)
            scope.launch { app.settings.setPrayerRemindersEnabled(true) }
        }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) enableRemindersNow()
    }
    val toggleReminders: () -> Unit = {
        if (settings.prayerRemindersEnabled) {
            PrayerNotificationScheduler.cancelReminders(context)
            scope.launch { app.settings.setPrayerRemindersEnabled(false) }
        } else if (
            android.os.Build.VERSION.SDK_INT >= 33 &&
            androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            enableRemindersNow()
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 16.dp, bottom = 28.dp)
    ) {
        item {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(stringResource(R.string.prayer), style = MaterialTheme.typography.headlineMedium)
                    Text(
                        LocalDate.now(locationZone).format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(deviceLocale)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                FilledTonalIconButton(onClick = { nav.navigate("qibla") }) {
                    Icon(Icons.Default.Explore, stringResource(R.string.qibla))
                }
            }
        }

        if (location == null || times == null) {
            item {
                Spacer(Modifier.height(18.dp))
                Card(
                    Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
                    shape = RoundedCornerShape(26.dp)
                ) {
                    Column(Modifier.padding(22.dp)) {
                        IconBadge(Icons.Default.LocationOn, emphasized = true)
                        Spacer(Modifier.height(14.dp))
                        Text(
                            stringResource(R.string.location_needed),
                            style = MaterialTheme.typography.titleLarge
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            stringResource(R.string.location_needed_details),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(14.dp))
                        Button(onClick = {
                            permissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_COARSE_LOCATION,
                                    Manifest.permission.ACCESS_FINE_LOCATION
                                )
                            )
                        }) {
                            Text(stringResource(R.string.set_location))
                        }
                    }
                }
            }
        } else {
            item {
                Spacer(Modifier.height(16.dp))
                Card(
                    Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
                    shape = RoundedCornerShape(26.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(Modifier.padding(20.dp)) {
                        Text(
                            stringResource(R.string.next_prayer),
                            style = MaterialTheme.typography.labelLarge
                        )
                        Spacer(Modifier.height(5.dp))
                        Text(
                            next?.first.orEmpty(),
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                next?.second?.let { formatMinutes(it, deviceLocale) }.orEmpty(),
                                style = MaterialTheme.typography.displaySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            if (next != null) {
                                Spacer(Modifier.width(12.dp))
                                var countdown by remember(next.first, next.second) {
                                    mutableStateOf(countdownText(next.second, locationZone))
                                }
                                LaunchedEffect(next.first, next.second, locationZone) {
                                    while (true) {
                                        countdown = countdownText(next.second, locationZone)
                                        delay(1000)
                                    }
                                }
                                Text(
                                    countdown,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.70f)
                                )
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { nav.navigate("qibla") }) {
                                Icon(Icons.Default.Explore, null)
                                Spacer(Modifier.width(6.dp))
                                Text(stringResource(R.string.open_qibla))
                            }
                            FilledTonalButton(onClick = toggleReminders) {
                                Icon(
                                    if (settings.prayerRemindersEnabled) Icons.Default.NotificationsOff else Icons.Default.NotificationsActive,
                                    null
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(stringResource(if (settings.prayerRemindersEnabled) R.string.cancel_reminders else R.string.schedule_reminders))
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        OutlinedButton(
                            onClick = { nav.navigate("adhan") },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.GraphicEq, null)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                selectedAdhan?.let { stringResource(R.string.adhan_open_library) + ": " + it.displayTitle }
                                    ?: stringResource(R.string.adhan_open_library)
                            )
                        }
                    }
                }
            }

            item {
                SectionTitle(stringResource(R.string.prayer))
            }

            items(allRows, key = { it.first }) { row ->
                val isNext = next?.first == row.first
                Card(
                    Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isNext) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                        } else {
                            MaterialTheme.colorScheme.surface
                        }
                    )
                ) {
                    Row(
                        Modifier.padding(horizontal = 18.dp, vertical = 15.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconBadge(
                                if (isNext) Icons.Default.Schedule
                                else Icons.Default.NotificationsActive,
                                emphasized = isNext
                            )
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(
                                    row.first,
                                    fontWeight = if (isNext) FontWeight.Bold else FontWeight.SemiBold
                                )
                                if (isNext) {
                                    Text(
                                        stringResource(R.string.next_prayer),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                        Text(
                            formatMinutes(row.second, deviceLocale),
                            style = MaterialTheme.typography.titleLarge,
                            color = if (isNext) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

private fun formatMinutes(v: Double, locale: Locale = Locale.getDefault()): String {
    val total = kotlin.math.round(v).toInt().mod(1440)
    return java.time.LocalTime.of((total / 60) % 24, total % 60).format(java.time.format.DateTimeFormatter.ofPattern("h:mm a", locale))
}

private fun countdownText(target: Double, locationZone: ZoneId = ZoneId.systemDefault()): String {
    val now = ZonedDateTime.now(locationZone)
    val current = now.hour * 60.0 + now.minute + now.second / 60.0
    var diff = target - current
    if (diff <= 0) diff += 1440.0
    val minutes = kotlin.math.floor(diff / 60.0).toInt()
    val seconds = (diff % 60).toInt()
    return "%02d:%02d".format(minutes, seconds)
}
