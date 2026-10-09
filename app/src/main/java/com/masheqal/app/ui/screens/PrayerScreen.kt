
package com.masheqal.app.ui.screens

import android.content.Intent
import androidx.core.content.ContextCompat
import android.Manifest
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Schedule
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
import kotlinx.coroutines.launch
import com.masheqal.app.domain.*
import com.masheqal.app.services.PrayerNotificationScheduler
import com.masheqal.app.services.QuranPlaybackService
import com.masheqal.app.util.LocationUtils
import com.masheqal.app.util.LocationChoiceStore
import com.masheqal.app.util.PlaceLookup
import com.masheqal.app.util.PrayerTimeZoneResolver
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import java.time.ZonedDateTime
import kotlinx.coroutines.delay

@Composable
fun PrayerScreen(app: MasheqalApp, nav: NavHostController, onRequestLocation: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var location by remember { mutableStateOf(LocationChoiceStore.loadManual(context) ?: LocationUtils.lastKnown(context)) }
    var placeLabel by remember { mutableStateOf(location?.placeName) }
    var showCityPicker by remember { mutableStateOf(false) }
    var prayerZone by remember { mutableStateOf(ZoneId.systemDefault()) }
    var timezoneLookupFailed by remember { mutableStateOf(false) }
    var today by remember { mutableStateOf(LocalDate.now()) }
    var times by remember { mutableStateOf<PrayerTimes?>(null) }
    var tomorrowFajr by remember { mutableStateOf<Double?>(null) }
    var isRefreshingLocation by remember { mutableStateOf(false) }
    var locationRefreshFailed by remember { mutableStateOf(false) }
    val settings by app.settings.state.collectAsState(initial = com.masheqal.app.data.SettingsState())
    val scope = rememberCoroutineScope()
    var adhanEnabled by remember { mutableStateOf(PrayerNotificationScheduler.isAdhanEnabled(context)) }
    val adhanPreviewLabel = stringResource(R.string.adhan_preview)

    val refreshLocation: (Boolean) -> Unit = remember(context, scope) {
        { useDeviceLocation ->
            scope.launch {
                isRefreshingLocation = true
                try {
                    if (useDeviceLocation) {
                        val live = runCatching { LocationUtils.current(context) }.getOrNull()
                        val cached = live ?: LocationUtils.lastKnown(context)
                        if (cached != null) {
                            LocationChoiceStore.clearManual(context)
                            location = cached
                            locationRefreshFailed = false
                        } else {
                            location = LocationChoiceStore.loadManual(context)
                            locationRefreshFailed = true
                        }
                    } else {
                        location = LocationChoiceStore.loadManual(context) ?: LocationUtils.lastKnown(context)
                        locationRefreshFailed = location == null
                    }
                } finally {
                    isRefreshingLocation = false
                }
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        if (
            grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true ||
            grants[Manifest.permission.ACCESS_FINE_LOCATION] == true
        ) {
            refreshLocation(true)
        } else {
            location = LocationChoiceStore.loadManual(context) ?: LocationUtils.lastKnown(context)
            locationRefreshFailed = location == null
        }
    }

    fun requestDeviceLocation() {
        val hasPermission =
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (hasPermission) {
            refreshLocation(true)
        } else {
            permissionLauncher.launch(
                arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION)
            )
        }
    }

    LaunchedEffect(Unit) {
        // Preserve a selected city; only get a GPS fix when no manual place has been saved.
        val manual = LocationChoiceStore.loadManual(context)
        if (manual != null) location = manual else refreshLocation(true)
    }

    LaunchedEffect(location?.latitude, location?.longitude, location?.placeName) {
        val current = location
        placeLabel = current?.placeName
        if (current != null && current.placeName.isNullOrBlank()) {
            placeLabel = runCatching {
                PlaceLookup.reverseGeocode(context, current.latitude, current.longitude)?.let { match ->
                    location = current.copy(placeName = match.displayName, countryName = match.countryName, countryCode = match.countryCode)
                    match.displayName
                }
            }.getOrNull()
        }
    }

    LaunchedEffect(location?.latitude, location?.longitude, location?.countryCode) {
        val current = location
        if (current == null) {
            prayerZone = ZoneId.systemDefault()
            timezoneLookupFailed = false
        } else {
            val resolvedZone = runCatching {
                PrayerTimeZoneResolver.resolve(current.latitude, current.longitude, current.countryCode)
            }.getOrNull()
            if (resolvedZone != null) {
                prayerZone = resolvedZone
                timezoneLookupFailed = false
                val localDate = LocalDate.now(resolvedZone)
                if (today != localDate) today = localDate
            } else {
                prayerZone = ZoneId.systemDefault()
                timezoneLookupFailed = true
                val fallbackDate = LocalDate.now(prayerZone)
                if (today != fallbackDate) today = fallbackDate
            }
        }
    }

    // Keep the displayed schedule current when the app remains open across midnight.
    LaunchedEffect(prayerZone) {
        while (true) {
            delay(30_000L)
            val currentDate = LocalDate.now(prayerZone)
            if (currentDate != today) today = currentDate
        }
    }

    LaunchedEffect(location, settings.prayerMethod, settings.madhhab, today, prayerZone) {
        location?.let { c ->
            val zone = prayerZone
            val offset = today.atStartOfDay(zone).offset.totalSeconds / 3600.0
            val method = PrayerMethod.valueOf(settings.prayerMethod)
            val madhhab = AsrMadhhab.valueOf(settings.madhhab)
            val coordinates = Coordinates(c.latitude, c.longitude, offset)
            val currentSchedule = PrayerCalculator.calculate(
                today,
                coordinates,
                method,
                madhhab,
                zoneId = zone
            )
            val tomorrow = today.plusDays(1)
            val tomorrowOffset = tomorrow.atStartOfDay(zone).offset.totalSeconds / 3600.0
            val nextFajr = runCatching {
                PrayerCalculator.calculate(
                    tomorrow,
                    coordinates.copy(timezoneOffsetHours = tomorrowOffset),
                    method,
                    madhhab,
                    zoneId = zone
                ).fajr
            }.getOrNull()
            times = currentSchedule
            tomorrowFajr = nextFajr
            PrayerNotificationScheduler.storeConfig(
                context,
                c.latitude,
                c.longitude,
                method,
                madhhab,
                zone
            )
        }
    }

    val formattedDate = remember(today) {
        DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
            .withLocale(Locale.getDefault())
            .format(today)
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
        val now = ZonedDateTime.now().let {
            it.hour * 60.0 + it.minute + it.second / 60.0
        }
        PrayerCalculator.selectNextPrayer(now, prayerRows, tomorrowFajr)
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
                        formattedDate,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { requestDeviceLocation() },
                        enabled = !isRefreshingLocation
                    ) {
                        if (isRefreshingLocation) {
                            CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(
                                Icons.Default.MyLocation,
                                contentDescription = stringResource(R.string.location_refresh)
                            )
                        }
                    }
                    FilledTonalIconButton(onClick = { nav.navigate("qibla") }) {
                        Icon(Icons.Default.Explore, stringResource(R.string.qibla))
                    }
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
                            stringResource(R.string.location_refresh_failed),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(14.dp))
                        Button(
                            onClick = { requestDeviceLocation() },
                            enabled = !isRefreshingLocation
                        ) {
                            if (isRefreshingLocation) {
                                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                                Spacer(Modifier.width(8.dp))
                                Text(stringResource(R.string.location_refreshing))
                            } else {
                                Text(stringResource(R.string.set_location))
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(onClick = { showCityPicker = true }) {
                                Text(stringResource(R.string.choose_city))
                            }
                            TextButton(
                                onClick = { runCatching { context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)) } }
                            ) {
                                Text(stringResource(R.string.open_location_settings))
                            }
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
                                next?.second?.let(::formatMinutes).orEmpty(),
                                style = MaterialTheme.typography.displaySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            if (next != null) {
                                Spacer(Modifier.width(12.dp))
                                var countdown by remember(next.first, next.second) {
                                    mutableStateOf(countdownText(next.second, prayerZone))
                                }
                                LaunchedEffect(next.first, next.second) {
                                    while (true) {
                                        countdown = countdownText(next.second, prayerZone)
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
                        Spacer(Modifier.height(6.dp))
                        Text(
                            placeLabel ?: stringResource(R.string.location_city_unavailable),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(Modifier.height(4.dp))
                        if (location!!.isManual) {
                            Text(
                                stringResource(R.string.location_manual_accuracy),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Text(
                                stringResource(
                                    if (location!!.isPrecise) R.string.location_accuracy
                                    else R.string.location_accuracy_approximate,
                                    location!!.accuracyMeters.toInt().coerceAtLeast(1)
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = if (location!!.isPrecise) MaterialTheme.colorScheme.onSurfaceVariant
                                else MaterialTheme.colorScheme.error
                            )
                        }
                        if (timezoneLookupFailed) {
                            Text(
                                stringResource(R.string.location_timezone_fallback),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        if (locationRefreshFailed) {
                            Text(
                                stringResource(R.string.location_refresh_failed),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                            TextButton(
                                onClick = { runCatching { context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)) } }
                            ) {
                                Text(stringResource(R.string.open_location_settings))
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { nav.navigate("qibla") }) {
                                Icon(Icons.Default.Explore, null)
                                Spacer(Modifier.width(6.dp))
                                Text(stringResource(R.string.open_qibla))
                            }
                            FilledTonalButton(onClick = {
                                val c = location!!
                                PrayerNotificationScheduler.storeConfig(
                                    context,
                                    c.latitude,
                                    c.longitude,
                                    PrayerMethod.valueOf(settings.prayerMethod),
                                    AsrMadhhab.valueOf(settings.madhhab),
                                    prayerZone
                                )
                                PrayerNotificationScheduler.scheduleToday(context, times!!, prayerZone)
                            }) {
                                Icon(Icons.Default.NotificationsActive, null)
                                Spacer(Modifier.width(6.dp))
                                Text(stringResource(R.string.schedule_reminders))
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(onClick = { showCityPicker = true }) {
                                Text(stringResource(R.string.choose_city))
                            }
                            TextButton(onClick = { requestDeviceLocation() }) {
                                Text(stringResource(R.string.use_device_location))
                            }
                        }
                    }
                }
            }

            item {
                Card(
                    Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    stringResource(R.string.full_adhan_audio),
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    stringResource(R.string.full_adhan_description),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = adhanEnabled,
                                onCheckedChange = { enabled ->
                                    adhanEnabled = enabled
                                    PrayerNotificationScheduler.setAdhanEnabled(context, enabled)
                                }
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = {
                                val playbackIntent = Intent(context, QuranPlaybackService::class.java)
                                    .setAction(QuranPlaybackService.ACTION_PLAY_ADHAN)
                                    .putExtra(QuranPlaybackService.EXTRA_PRAYER_NAME, adhanPreviewLabel)
                                runCatching { ContextCompat.startForegroundService(context, playbackIntent) }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.NotificationsActive, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.adhan_preview))
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
                            formatMinutes(row.second),
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

    if (showCityPicker) {
        CityPickerDialog(
            onDismiss = { showCityPicker = false },
            onPlaceSelected = { selected ->
                location = LocationChoiceStore.saveManual(context, selected)
                placeLabel = selected.displayName
                locationRefreshFailed = false
                showCityPicker = false
            }
        )
    }
}

private fun formatMinutes(v: Double): String {
    val total = kotlin.math.round(v).toInt()
    return "%02d:%02d".format((total / 60) % 24, total % 60)
}

private fun countdownText(target: Double, zoneId: ZoneId = ZoneId.systemDefault()): String {
    val now = ZonedDateTime.now(zoneId)
    val current = now.hour * 60.0 + now.minute + now.second / 60.0
    var diff = target - current
    if (diff <= 0) diff += 1440.0
    val minutes = kotlin.math.floor(diff / 60.0).toInt()
    val seconds = (diff % 60).toInt()
    return "%02d:%02d".format(minutes, seconds)
}
