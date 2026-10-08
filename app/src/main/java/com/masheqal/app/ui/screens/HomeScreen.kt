package com.masheqal.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
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
    var prayerTimes by remember { mutableStateOf<PrayerTimes?>(null) }

    LaunchedEffect(Unit) {
        val verses = app.quran.loadVerses()
        if (verses.isNotEmpty()) daily = verses[(LocalDate.now().dayOfYear - 1) % verses.size]
        location = LocationUtils.lastKnown(context)
    }
    LaunchedEffect(location, settings.prayerMethod, settings.madhhab) {
        location?.let { c ->
            val zone = ZoneId.systemDefault()
            val offset = ZonedDateTime.now(zone).offset.totalSeconds / 3600.0
            prayerTimes = PrayerCalculator.calculate(LocalDate.now(), Coordinates(c.latitude,c.longitude,offset), PrayerMethod.valueOf(settings.prayerMethod), AsrMadhhab.valueOf(settings.madhhab))
        }
    }

    val candidate = prayerTimes?.let { p ->
        val rows = listOf(
            PrayerCandidate(stringResource(R.string.fajr), p.fajr), PrayerCandidate(stringResource(R.string.dhuhr), p.dhuhr),
            PrayerCandidate(stringResource(R.string.asr), p.asr), PrayerCandidate(stringResource(R.string.maghrib), p.maghrib), PrayerCandidate(stringResource(R.string.isha), p.isha)
        )
        val now = ZonedDateTime.now().let { it.hour * 60.0 + it.minute + it.second / 60.0 }
        rows.firstOrNull { it.minutes >= now } ?: rows.firstOrNull()
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(vertical = 18.dp)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                val hijri = HijriCalculator.fromGregorian(LocalDate.now())
                Text("${LocalDate.now()}  •  ${hijri.day}/${hijri.month}/${hijri.year}", style = MaterialTheme.typography.bodySmall)
            }
            IconButton(onClick = { nav.navigate("search") }) { Icon(Icons.Default.Search, stringResource(R.string.search)) }
        }
        Spacer(Modifier.height(16.dp))

        Card(Modifier.padding(horizontal = 20.dp).fillMaxWidth(), shape = RoundedCornerShape(26.dp)) {
            Column(Modifier.padding(22.dp)) {
                Text(stringResource(R.string.next_prayer), style = MaterialTheme.typography.labelLarge)
                if (candidate != null && prayerTimes != null) {
                    val now = ZonedDateTime.now()
                    var countdown by remember(candidate.name, candidate.minutes) { mutableStateOf(countdownText(candidate.minutes, now)) }
                    LaunchedEffect(candidate.name, candidate.minutes) {
                        while (true) { countdown = countdownText(candidate.minutes, ZonedDateTime.now()); delay(1000) }
                    }
                    Text(candidate.name, style = MaterialTheme.typography.headlineSmall)
                    Text(formatMinutes(candidate.minutes), style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.primary)
                    Text(countdown, style = MaterialTheme.typography.titleMedium)
                } else {
                    Text(stringResource(R.string.location_needed), style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = { onRequestLocation(); nav.navigate("prayer") }) { Text(stringResource(R.string.set_location)) }
                }
                if (location != null) Text(stringResource(R.string.location_ready), style = MaterialTheme.typography.labelSmall)
            }
        }

        SectionTitle(stringResource(R.string.continue_quran))
        FeatureCard(
            "سورە ${reading.surah}",
            "${reading.surah}:${reading.ayah} — ${stringResource(R.string.resume_exact_position)}",
            Icons.Default.MenuBook
        ) { nav.navigate("quran/ref/${reading.surah}/${reading.ayah}") }

        if (khatmah.active) {
            SectionTitle(stringResource(R.string.khatmah))
            val progress = (khatmah.readPages.toFloat() / khatmah.targetPages.coerceAtLeast(1)).coerceIn(0f,1f)
            Card(onClick={nav.navigate("khatmah")}, modifier=Modifier.padding(horizontal = 20.dp).fillMaxWidth(), shape=RoundedCornerShape(22.dp)) {
                Column(Modifier.padding(18.dp)) {
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text("${khatmah.readPages}/${khatmah.targetPages} ${stringResource(R.string.page)}");Text("${(progress*100).toInt()}%",color=MaterialTheme.colorScheme.primary)}
                    Spacer(Modifier.height(8.dp)); LinearProgressIndicator(progress={progress}, modifier=Modifier.fillMaxWidth())
                }
            }
        }

        SectionTitle(stringResource(R.string.ayah_of_day))
        daily?.let { verse ->
            Card(Modifier.padding(horizontal = 20.dp).fillMaxWidth(), shape = RoundedCornerShape(24.dp)) {
                Column(Modifier.padding(20.dp)) {
                    QuranText(verse.text, size = 25f)
                    Spacer(Modifier.height(12.dp)); Text(verse.translationEn.orEmpty(), style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.height(8.dp)); Text("${verse.surah}:${verse.ayah}", color = MaterialTheme.colorScheme.secondary)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        IconButton(onClick = { app.userDb.addBookmark("ayah", "${verse.surah}:${verse.ayah}", "${verse.surah}:${verse.ayah}") }) { Icon(Icons.Default.BookmarkBorder, stringResource(R.string.bookmark)) }
                        IconButton(onClick = { shareText(context, "${verse.text}\n\n${verse.translationEn}\n${verse.surah}:${verse.ayah}") }) { Icon(Icons.Default.Share, stringResource(R.string.share)) }
                        IconButton(onClick = { val uri=ShareCardUtils.createVerseCard(context,verse.text,verse.translationEn.orEmpty(),"${verse.surah}:${verse.ayah}"); ShareCardUtils.shareImage(context,uri) }) { Icon(Icons.Default.Image, stringResource(R.string.share_image)) }
                    }
                }
            }
        }
        SectionTitle(stringResource(R.string.quick_access))
        Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                FeatureCard(stringResource(R.string.qibla), icon = Icons.Default.Explore, modifier = Modifier.weight(1f)) { nav.navigate("qibla") }
                FeatureCard(stringResource(R.string.tasbih), icon = Icons.Default.TouchApp, modifier = Modifier.weight(1f)) { nav.navigate("tasbih") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                FeatureCard(stringResource(R.string.bookmarks), icon = Icons.Default.Bookmark, modifier = Modifier.weight(1f)) { nav.navigate("saved") }
                FeatureCard(stringResource(R.string.notes), icon = Icons.Default.Notes, modifier = Modifier.weight(1f)) { nav.navigate("notes") }
            }
        }
        Spacer(Modifier.height(24.dp))
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
    return "%02d:%02d".format(minutes,seconds)
}
