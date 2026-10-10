
package com.masheqal.app.ui.screens

import android.content.Context
import androidx.compose.animation.animateContentSize
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.masheqal.app.MasheqalApp
import com.masheqal.app.data.QuranStudyRepository
import com.masheqal.app.R
import com.masheqal.app.domain.*
import com.masheqal.app.util.LocationUtils
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.time.ZonedDateTime
import kotlinx.coroutines.delay

private data class PrayerCandidate(val name: String, val minutes: Double)

@Composable
fun HomeScreen(app: MasheqalApp, nav: NavHostController, onRequestLocation: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val deviceLocale = LocalConfiguration.current.locales[0] ?: Locale.ROOT
    var daily by remember { mutableStateOf<com.masheqal.app.data.QuranVerse?>(null) }
    var dailyTranslation by remember { mutableStateOf<String?>(null) }
    val studyRepository = remember(context) { QuranStudyRepository(context) }
    val reading by app.personal.reading.collectAsState(initial = com.masheqal.app.data.ReadingPosition())
    val settings by app.settings.state.collectAsState(initial = com.masheqal.app.data.SettingsState())
    var location by remember { mutableStateOf(LocationUtils.lastKnown(context)) }
    var prayerTimes by remember { mutableStateOf<PrayerTimes?>(null) }

    LaunchedEffect(Unit) {
        val verses = app.quran.loadVerses()
        if (verses.isNotEmpty()) {
            daily = verses[(LocalDate.now().dayOfYear - 1) % verses.size]
        }
        location = LocationUtils.lastKnown(context)
    }

    LaunchedEffect(daily?.surah, daily?.ayah, settings.translationEdition) {
        val verse = daily
        if (verse == null) {
            dailyTranslation = null
            return@LaunchedEffect
        }
        val result = runCatching {
            studyRepository.loadSurah(verse.surah, settings.translationEdition)
        }.getOrNull()
        dailyTranslation = result?.getOrNull(verse.ayah - 1)?.takeIf { it.isNotBlank() }
            ?: if (settings.translationEdition == "en.sahih") verse.translationEn?.takeIf { it.isNotBlank() } else null
    }

    LaunchedEffect(location, settings.prayerMethod, settings.madhhab) {
        location?.let { c ->
            val offset = ZonedDateTime.now().offset.totalSeconds / 3600.0
            prayerTimes = PrayerCalculator.calculate(
                LocalDate.now(),
                Coordinates(c.latitude, c.longitude, offset),
                PrayerMethod.valueOf(settings.prayerMethod),
                AsrMadhhab.valueOf(settings.madhhab)
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

    val date = LocalDate.now()
    val hijri = HijriCalculator.fromGregorian(date)

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
                        "${date.format(DateTimeFormatter.ofPattern("EEE, d MMM", deviceLocale))}  •  ${hijri.day}/${hijri.month}/${hijri.year}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalIconButton(onClick = { nav.navigate("search") }) {
                        Icon(Icons.Default.Search, stringResource(R.string.search))
                    }
                    FilledTonalIconButton(onClick = { nav.navigate("settings") }) {
                        Icon(Icons.Default.Settings, stringResource(R.string.settings))
                    }
                }
            }
        }

        item {
            Spacer(Modifier.height(16.dp))
            Card(
                Modifier.padding(horizontal = 16.dp).fillMaxWidth().animateContentSize(),
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
                                    formatMinutes(candidate.minutes, deviceLocale),
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
            Column(
                Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SectionTitle(
                    stringResource(R.string.today_prayer_times),
                    stringResource(R.string.view_all)
                ) {
                    nav.navigate("prayer")
                }
                if (prayerTimes != null) {
                    val schedule = listOf(
                        stringResource(R.string.fajr) to prayerTimes!!.fajr,
                        stringResource(R.string.dhuhr) to prayerTimes!!.dhuhr,
                        stringResource(R.string.asr) to prayerTimes!!.asr,
                        stringResource(R.string.maghrib) to prayerTimes!!.maghrib,
                        stringResource(R.string.isha) to prayerTimes!!.isha
                    )
                    schedule.chunked(3).forEach { rowItems ->
                        Row(
                            Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowItems.forEach { (name, time) ->
                                Card(
                                    onClick = { nav.navigate("prayer") },
                                    modifier = Modifier.weight(1f).animateContentSize(),
                                    shape = RoundedCornerShape(18.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surface
                                    )
                                ) {
                                    Column(
                                        Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 13.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(5.dp)
                                    ) {
                                        Text(
                                            name,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1
                                        )
                                        Text(
                                            formatMinutes(time, deviceLocale),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                            repeat(3 - rowItems.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                } else {
                    Card(
                        onClick = {
                            onRequestLocation()
                            nav.navigate("prayer")
                        },
                        modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.MyLocation, null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(stringResource(R.string.set_location), fontWeight = FontWeight.SemiBold)
                                Text(stringResource(R.string.prayer_grid_location_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Icon(Icons.Default.ChevronRight, null)
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
                        if (settings.showEnglishTranslation && !dailyTranslation.isNullOrBlank()) {
                            Text(
                                dailyTranslation.orEmpty(),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else if (settings.showEnglishTranslation && settings.translationEdition != "en.sahih") {
                            Text(
                                stringResource(R.string.translation_unavailable_offline),
                                style = MaterialTheme.typography.bodySmall,
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
                                    "${verse.text}\n\n${dailyTranslation.orEmpty()}\n${verse.surah}:${verse.ayah}"
                                )
                            }) {
                                Icon(Icons.Default.Share, stringResource(R.string.share))
                            }
                            IconButton(onClick = {
                                val uri = ShareCardUtils.createVerseCard(
                                    context,
                                    verse.text,
                                    dailyTranslation.orEmpty(),
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

private fun formatMinutes(v: Double, locale: Locale = Locale.getDefault()): String {
    val total = kotlin.math.round(v).toInt()
    return java.time.LocalTime.of((total / 60) % 24, total % 60).format(java.time.format.DateTimeFormatter.ofPattern("h:mm a", locale))
}

private fun countdownText(prayerMinutes: Double, now: ZonedDateTime): String {
    val current = now.hour * 60.0 + now.minute + now.second / 60.0
    var diff = prayerMinutes - current
    if (diff <= 0) diff += 1440.0
    val minutes = kotlin.math.floor(diff / 60.0).toInt()
    val seconds = (diff % 60).toInt()
    return "%02d:%02d".format(minutes, seconds)
}
