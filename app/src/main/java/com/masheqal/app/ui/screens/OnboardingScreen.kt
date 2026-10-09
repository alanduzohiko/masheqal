package com.masheqal.app.ui.screens

import android.Manifest
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.masheqal.app.MasheqalApp
import com.masheqal.app.R
import com.masheqal.app.data.SettingsState
import com.masheqal.app.util.CurrentLocation
import com.masheqal.app.util.LocationUtils
import kotlinx.coroutines.launch

private data class OnboardingReciter(val id: String, val label: Int)

@Composable
fun OnboardingScreen(
    app: MasheqalApp,
    nav: NavHostController,
    onLanguage: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settings by app.settings.state.collectAsState(initial = SettingsState())
    var step by rememberSaveable { mutableIntStateOf(0) }
    var language by rememberSaveable { mutableStateOf(settings.language) }
    var prayerMethod by rememberSaveable { mutableStateOf(settings.prayerMethod) }
    var madhhab by rememberSaveable { mutableStateOf(settings.madhhab) }
    var location by remember { mutableStateOf(LocationUtils.lastKnown(context)) }
    var locating by remember { mutableStateOf(false) }
    var khatmahEnabled by rememberSaveable { mutableStateOf(false) }
    var khatmahDays by rememberSaveable { mutableIntStateOf(30) }
    var mushafStyle by rememberSaveable {
        mutableStateOf(
            context.getSharedPreferences("masheqal_reader_preferences", Context.MODE_PRIVATE)
                .getString("reading_mode", "mushaf") ?: "mushaf"
        )
    }
    val reciters = listOf(
        OnboardingReciter("ar.alafasy", R.string.reciter_alafasy),
        OnboardingReciter("ar.husary", R.string.reciter_husary),
        OnboardingReciter("ar.minshawi", R.string.reciter_minshawi),
        OnboardingReciter("ar.sudais", R.string.reciter_sudais),
        OnboardingReciter("ar.shuraim", R.string.reciter_shuraim),
        OnboardingReciter("ar.abdulbasit", R.string.reciter_abdulbasit),
        OnboardingReciter("ar.ajamy", R.string.reciter_ajamy),
        OnboardingReciter("ar.hudhaify", R.string.reciter_hudhaify)
    )
    var reciter by rememberSaveable {
        mutableStateOf(
            context.getSharedPreferences("masheqal_audio_preferences", Context.MODE_PRIVATE)
                .getString("reciter", "ar.alafasy") ?: "ar.alafasy"
        )
    }

    val locationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        if (
            grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true ||
            grants[Manifest.permission.ACCESS_FINE_LOCATION] == true
        ) {
            scope.launch {
                locating = true
                try {
                    location = LocationUtils.current(context) ?: LocationUtils.lastKnown(context)
                } finally {
                    locating = false
                }
            }
        }
    }

    fun skipSetup() {
        context.getSharedPreferences("masheqal_onboarding", Context.MODE_PRIVATE)
            .edit().putBoolean("complete", true).apply()
        nav.navigate("home") {
            popUpTo("onboarding") { inclusive = true }
            launchSingleTop = true
        }
    }

    fun finishSetup() {
        scope.launch {
            app.settings.setLanguage(language)
            app.settings.setPrayerMethod(prayerMethod)
            app.settings.setMadhhab(madhhab)
            if (khatmahEnabled) {
                app.personal.setKhatmah(
                    days = khatmahDays,
                    targetPages = 604,
                    readPages = 0,
                    active = true
                )
            }
            context.getSharedPreferences("masheqal_audio_preferences", Context.MODE_PRIVATE)
                .edit().putString("reciter", reciter).apply()
            context.getSharedPreferences("masheqal_reader_preferences", Context.MODE_PRIVATE)
                .edit().putString("reading_mode", mushafStyle).apply()
            context.getSharedPreferences("masheqal_onboarding", Context.MODE_PRIVATE)
                .edit().putBoolean("complete", true).apply()
            onLanguage(language)
            nav.navigate("home") {
                popUpTo("onboarding") { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    val methodOptions = listOf(
        "MWL" to R.string.method_mwl,
        "EGYPTIAN" to R.string.method_egyptian,
        "UMM_AL_QURA" to R.string.method_umm_al_qura,
        "KARACHI" to R.string.method_karachi,
        "ISNA" to R.string.method_isna,
        "TEHRAN" to R.string.method_tehran,
        "TURKEY" to R.string.method_turkey
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp, vertical = 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(R.drawable.ic_masheqal),
                        contentDescription = null,
                        modifier = Modifier.size(42.dp),
                        tint = androidx.compose.ui.graphics.Color.Unspecified
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.app_name),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    stringResource(R.string.brand_tagline),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            TextButton(onClick = { skipSetup() }) {
                Text(stringResource(R.string.skip_setup))
            }
        }

        Spacer(Modifier.height(20.dp))
        LinearProgressIndicator(
            progress = { (step + 1) / 4f },
            modifier = Modifier.fillMaxWidth(),
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.onboarding_step, step + 1, 4),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(16.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            when (step) {
                0 -> {
                    OnboardingHero(
                        icon = { Icon(Icons.Default.Language, contentDescription = null) },
                        title = stringResource(R.string.onboarding_welcome),
                        description = stringResource(R.string.onboarding_intro)
                    )
                    Text(
                        stringResource(R.string.onboarding_language_description),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            "ckb" to "کوردی",
                            "ar" to "العربية",
                            "en" to "English"
                        ).forEach { (code, label) ->
                            Card(
                                onClick = {
                                    language = code
                                    scope.launch { app.settings.setLanguage(code) }
                                    onLanguage(code)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (language == code)
                                        MaterialTheme.colorScheme.secondaryContainer
                                    else MaterialTheme.colorScheme.surface
                                )
                            ) {
                                Row(
                                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                                    RadioButton(selected = language == code, onClick = {
                                        language = code
                                        scope.launch { app.settings.setLanguage(code) }
                                        onLanguage(code)
                                    })
                                }
                            }
                        }
                    }
                }

                1 -> {
                    OnboardingHero(
                        icon = { Icon(Icons.Default.Schedule, contentDescription = null) },
                        title = stringResource(R.string.prayer_method),
                        description = stringResource(R.string.onboarding_prayer_description)
                    )
                    Text(
                        stringResource(R.string.prayer_method),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        methodOptions.chunked(2).forEach { group ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                group.forEach { (code, labelRes) ->
                                    FilterChip(
                                        selected = prayerMethod == code,
                                        onClick = { prayerMethod = code },
                                        label = { Text(stringResource(labelRes)) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                if (group.size == 1) Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                    Text(stringResource(R.string.madhhab), style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("SHAFI" to R.string.madhhab_shafi, "HANAFI" to R.string.madhhab_hanafi)
                            .forEach { (code, labelRes) ->
                                FilterChip(
                                    selected = madhhab == code,
                                    onClick = { madhhab = code },
                                    label = { Text(stringResource(labelRes)) }
                                )
                            }
                    }
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(8.dp))
                                Text(stringResource(R.string.onboarding_location_title), style = MaterialTheme.typography.titleMedium)
                            }
                            val current = location
                            if (current != null) {
                                Text(
                                    stringResource(
                                        if (current.isPrecise) R.string.location_accuracy
                                        else R.string.location_accuracy_approximate,
                                        current.accuracyMeters.toInt().coerceAtLeast(1)
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (current.isPrecise) MaterialTheme.colorScheme.onSurfaceVariant
                                    else MaterialTheme.colorScheme.error
                                )
                            } else {
                                Text(
                                    stringResource(R.string.onboarding_location_unavailable),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Button(
                                onClick = {
                                    locationLauncher.launch(
                                        arrayOf(
                                            Manifest.permission.ACCESS_COARSE_LOCATION,
                                            Manifest.permission.ACCESS_FINE_LOCATION
                                        )
                                    )
                                },
                                enabled = !locating,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                if (locating) {
                                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                                    Spacer(Modifier.width(8.dp))
                                    Text(stringResource(R.string.location_refreshing))
                                } else {
                                    Text(stringResource(R.string.use_current_location))
                                }
                            }
                        }
                    }
                }

                2 -> {
                    OnboardingHero(
                        icon = { Icon(Icons.Default.MenuBook, contentDescription = null) },
                        title = stringResource(R.string.quran),
                        description = stringResource(R.string.onboarding_reading_description)
                    )
                    Text(stringResource(R.string.onboarding_mushaf_style), style = MaterialTheme.typography.titleMedium)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Card(
                            onClick = { mushafStyle = "mushaf" },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (mushafStyle == "mushaf")
                                    MaterialTheme.colorScheme.secondaryContainer
                                else MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.MenuBook, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(stringResource(R.string.reading_style_mushaf), fontWeight = FontWeight.SemiBold)
                                    Text(stringResource(R.string.onboarding_mushaf_description), style = MaterialTheme.typography.bodySmall)
                                }
                                RadioButton(selected = mushafStyle == "mushaf", onClick = { mushafStyle = "mushaf" })
                            }
                        }
                        Card(
                            onClick = { mushafStyle = "text" },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (mushafStyle == "text")
                                    MaterialTheme.colorScheme.secondaryContainer
                                else MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.MenuBook, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(stringResource(R.string.reading_style_text), fontWeight = FontWeight.SemiBold)
                                    Text(stringResource(R.string.onboarding_text_description), style = MaterialTheme.typography.bodySmall)
                                }
                                RadioButton(selected = mushafStyle == "text", onClick = { mushafStyle = "text" })
                            }
                        }
                    }

                    Text(stringResource(R.string.audio_reciter), style = MaterialTheme.typography.titleMedium)
                    Text(
                        stringResource(R.string.onboarding_reciter_description),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        reciters.forEach { item ->
                            Card(
                                onClick = { reciter = item.id },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (reciter == item.id)
                                        MaterialTheme.colorScheme.secondaryContainer
                                    else MaterialTheme.colorScheme.surface
                                )
                            ) {
                                Row(
                                    Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.MusicNote, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Spacer(Modifier.width(10.dp))
                                    Text(stringResource(item.label), modifier = Modifier.weight(1f))
                                    RadioButton(selected = reciter == item.id, onClick = { reciter = item.id })
                                }
                            }
                        }
                    }
                }

                else -> {
                    OnboardingHero(
                        icon = { Icon(Icons.Default.Spa, contentDescription = null) },
                        title = stringResource(R.string.khatmah),
                        description = stringResource(R.string.onboarding_finish_description)
                    )
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(stringResource(R.string.onboarding_enable_khatmah), style = MaterialTheme.typography.titleMedium)
                                    Text(stringResource(R.string.onboarding_khatmah_description), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Switch(checked = khatmahEnabled, onCheckedChange = { khatmahEnabled = it })
                            }
                            if (khatmahEnabled) {
                                Text(stringResource(R.string.daily_target), style = MaterialTheme.typography.titleSmall)
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    listOf(7, 14, 30, 60).forEach { days ->
                                        FilterChip(
                                            selected = khatmahDays == days,
                                            onClick = { khatmahDays = days },
                                            label = { Text(stringResource(R.string.khatmah_days, days)) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(14.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (step > 0) {
                OutlinedButton(
                    onClick = { step-- },
                    modifier = Modifier.weight(1f)
                ) { Text(stringResource(R.string.back_step)) }
            }
            Button(
                onClick = { if (step < 3) step++ else finishSetup() },
                modifier = Modifier.weight(1.5f),
                contentPadding = PaddingValues(vertical = 15.dp)
            ) {
                Text(
                    stringResource(if (step < 3) R.string.continue_setup else R.string.complete),
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun OnboardingHero(
    icon: @Composable () -> Unit,
    title: String,
    description: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.primaryContainer,
                            MaterialTheme.colorScheme.secondaryContainer
                        )
                    )
                )
                .padding(20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(
                    modifier = Modifier.size(48.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.13f)
                ) {
                    Box(contentAlignment = Alignment.Center) { icon() }
                }
                Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                Text(
                    description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
