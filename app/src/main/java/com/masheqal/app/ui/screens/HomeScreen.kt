
package com.masheqal.app.ui.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.masheqal.app.domain.*
import com.masheqal.app.util.LocationUtils
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import kotlinx.coroutines.delay

private data class PrayerCandidate(val name: String, val minutes: Double)

@Composable
fun HomeScreen(app: MasheqalApp, nav: NavHostController, onRequestLocation: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var daily by remember { mutableStateOf<com.masheqal.app.data.QuranVerse?>(null) }
    val reading by app.personal.reading.collectAsState(initial = com.masheqal.app.data.ReadingPosition())
    val khatmah by app.personal.khatmah.collectAsState(initial = com.masheqal.app.data.KhatmahState())
    val settings by app.settings.state.collectAsState(initial = com.masheqal.app.data.SettingsState())
    var location by remember { mutableStateOf(LocationUtils.lastKnown(context)) }
    var today by remember { mutableStateOf(LocalDate.now()) }
    var prayerTimes by remember { mutableStateOf<PrayerTimes?>(null) }

    LaunchedEffect(Unit) {
        location = LocationUtils.lastKnown(context)
    }

    // Refresh date-dependent dashboard data without requiring the user to reopen the app.
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000L)
            val currentDate = LocalDate.now()
            if (currentDate != today) today = currentDate
        }
    }

    LaunchedEffect(today) {
        val verses = app.quran.loadVerses()
        if (verses.isNotEmpty()) {
            daily = verses[(today.dayOfYear - 1) % verses.size]
        }
    }

    LaunchedEffect(location, settings.prayerMethod, settings.madhhab, today) {
        location?.let { c ->
            val zone = ZoneId.systemDefault()
            val offset = ZonedDateTime.now(zone).offset.totalSeconds / 3600.0
            prayerTimes = PrayerCalculator.calculate(
                today,
                Coordinates(c.latitude, c.longitude, offset),
                PrayerMethod.valueOf(settings.prayerMethod),
                AsrMadhhab.valueOf(settings.madhhab),
                zoneId = zone
            )
        }
    }

    val candidate = prayerTimes?.let { p ->
        val rows = listOf(
            PrayerCandidate(stringResource(R.string.fajr), p.fajr),
            PrayerCandidate(stringResource(R.string.dhuhr), p.dhuhr),
            PrayerCandidate(stringResource(R.string.asr), p.asr),
            PrayerCandidate(stringResource(R.string.maghrib), p.maghrib),
            PrayerCandidate(stringResource(R.string.isha), p.isha)
        )
        val now = ZonedDateTime.now().let { it.hour * 60.0 + it.minute + it.second / 60.0 }
        rows.firstOrNull { it.minutes >= now } ?: rows.firstOrNull()
    }

    val date = today
    val hijri = HijriCalculator.fromGregorian(date)
    val gregorianDate = remember(date) {
        DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
            .withLocale(Locale.getDefault())
            .format(date)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 16.dp, bottom = 26.dp)
    ) {
        item {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineMedium)
                    Spacer(Modifier.height(3.dp))
                    Text(
                        "$gregorianDate  •  ${hijri.day}/${hijri.month}/${hijri.year}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                FilledTonalIconButton(onClick = { nav.navigate("search") }) {
                    Icon(Icons.Default.Search, stringResource(R.string.search))
                }
            }
        }

        item {
            Spacer(Modifier.height(16.dp))
            Card(
                Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Box(
                    Modifier.fillMaxWidth().background(
                        Brush.linearGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.78f),
                                MaterialTheme.colorScheme.secondary.copy(alpha = 0.62f)
                            )
                        )
                    )
                ) {
                    Column(Modifier.padding(22.dp)) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    stringResource(R.string.next_prayer),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = Color.White.copy(alpha = 0.84f)
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    candidate?.name ?: stringResource(R.string.set_location),
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = Color.White
                                )
                            }
                            FilledTonalIconButton(onClick = { nav.navigate("prayer") }) {
                                Icon(Icons.Default.Schedule, stringResource(R.string.prayer))
                            }
                        }

                        if (candidate != null) {
                            val now = ZonedDateTime.now()
                            var countdown by remember(candidate.name, candidate.minutes) {
                                mutableStateOf(countdownText(candidate.minutes, now))
                            }
                            LaunchedEffect(candidate.name, candidate.minutes) {
                                while (true) {
                                    countdown = countdownText(candidate.minutes, ZonedDateTime.now())
                                    delay(1000)
                                }
                            }
                            Spacer(Modifier.height(10.dp))
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    formatMinutes(candidate.minutes),
                                    style = MaterialTheme.typography.displaySmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    countdown,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White.copy(alpha = 0.82f)
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(
                                stringResource(R.string.location_ready),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.72f)
                            )
                        } else {
                            Spacer(Modifier.height(10.dp))
                            Text(
                                stringResource(R.string.location_needed),
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.84f)
                            )
                            Spacer(Modifier.height(12.dp))
                            Button(
                                onClick = {
                                    onRequestLocation()
                                    nav.navigate("prayer")
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.White,
                                    contentColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Text(stringResource(R.string.set_location))
                            }
                        }
                    }
                }
            }
        }

        item {
            SectionTitle(
                stringResource(R.string.continue_quran),
                stringResource(R.string.open_quran)
            ) {
                nav.navigate("quran/ref/${reading.surah}/${reading.ayah}")
            }
        }

        item {
            Card(
                Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
                shape = RoundedCornerShape(24.dp)
            ) {
                Row(
                    Modifier.padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconBadge(Icons.Default.MenuBook, emphasized = true)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text("${reading.surah}", style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.height(3.dp))
                        Text(
                            "${reading.surah}:${reading.ayah}  •  ${stringResource(R.string.resume_exact_position)}",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    FilledTonalIconButton(
                        onClick = {
                            nav.navigate("quran/ref/${reading.surah}/${reading.ayah}")
                        }
                    ) {
                        Icon(Icons.Default.PlayArrow, stringResource(R.string.open_quran))
                    }
                }
            }
        }

        if (khatmah.active) {
            item {
                SectionTitle(
                    stringResource(R.string.khatmah),
                    stringResource(R.string.complete)
                ) {
                    nav.navigate("khatmah")
                }
                val progress = (
                    khatmah.readPages.toFloat() /
                        khatmah.targetPages.coerceAtLeast(1)
                    ).coerceIn(0f, 1f)
                Card(
                    Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(Modifier.padding(18.dp)) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "${khatmah.readPages}/${khatmah.targetPages} ${stringResource(R.string.page)}",
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                "${(progress * 100).toInt()}%",
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxWidth(),
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }
                }
            }
        }

        item { SectionTitle(stringResource(R.string.ayah_of_day)) }

        item {
            daily?.let { verse ->
                Card(
                    Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
                    shape = RoundedCornerShape(26.dp)
                ) {
                    Column(Modifier.padding(20.dp)) {
                        StatPill("${verse.surah}:${verse.ayah}")
                        Spacer(Modifier.height(12.dp))
                        QuranText(verse.text, size = 26f)
                        Spacer(Modifier.height(14.dp))
                        val visibleTranslation = verse.translationFor(settings.language)
                        if (!visibleTranslation.isNullOrBlank()) {
                            Text(
                                visibleTranslation.orEmpty(),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            IconButton(onClick = {
                                app.userDb.addBookmark(
                                    "ayah",
                                    "${verse.surah}:${verse.ayah}",
                                    "${verse.surah}:${verse.ayah}"
                                )
                            }) {
                                Icon(Icons.Default.BookmarkBorder, stringResource(R.string.bookmark))
                            }
                            IconButton(onClick = {
                                shareText(
                                    context,
                                    "${verse.text}\n\n${visibleTranslation.orEmpty()}\n${verse.surah}:${verse.ayah}"
                                )
                            }) {
                                Icon(Icons.Default.Share, stringResource(R.string.share))
                            }
                            IconButton(onClick = {
                                val uri = ShareCardUtils.createVerseCard(
                                    context,
                                    verse.text,
                                    visibleTranslation.orEmpty(),
                                    "${verse.surah}:${verse.ayah}"
                                )
                                ShareCardUtils.shareImage(context, uri)
                            }) {
                                Icon(Icons.Default.Image, stringResource(R.string.share_image))
                            }
                        }
                    }
                }
            }
        }

        item { SectionTitle(stringResource(R.string.quick_access)) }

        item {
            Column(
                Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FeatureCard(
                        stringResource(R.string.qibla),
                        icon = Icons.Default.Explore,
                        modifier = Modifier.weight(1f)
                    ) {
                        nav.navigate("qibla")
                    }
                    FeatureCard(
                        stringResource(R.string.tasbih),
                        icon = Icons.Default.TouchApp,
                        modifier = Modifier.weight(1f)
                    ) {
                        nav.navigate("tasbih")
                    }
                }
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FeatureCard(
                        stringResource(R.string.bookmarks),
                        icon = Icons.Default.Bookmark,
                        modifier = Modifier.weight(1f)
                    ) {
                        nav.navigate("saved")
                    }
                    FeatureCard(
                        stringResource(R.string.notes),
                        icon = Icons.Default.Notes,
                        modifier = Modifier.weight(1f)
                    ) {
                        nav.navigate("notes")
                    }
                }
            }
        }
    }
}

private fun formatMinutes(v: Double): String {
    val total = kotlin.math.round(v).toInt()
    return "%02d:%02d".format((total / 60) % 24, total % 60)
}

private fun countdownText(prayerMinutes: Double, now: ZonedDateTime): String {
    val current = now.hour * 60.0 + now.minute + now.second / 60.0
    var diff = prayerMinutes - current
    if (diff <= 0) diff += 1440.0
    val minutes = kotlin.math.floor(diff / 60.0).toInt()
    val seconds = (diff % 60).toInt()
    return "%02d:%02d".format(minutes, seconds)
}
