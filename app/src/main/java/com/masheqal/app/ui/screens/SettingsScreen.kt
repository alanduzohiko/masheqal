package com.masheqal.app.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
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
import com.masheqal.app.data.BackupRepository
import com.masheqal.app.data.SettingsState
import com.masheqal.app.data.FullSurahReciter
import com.masheqal.app.data.Mp3QuranReciterRepository
import com.masheqal.app.data.QuranEdition
import com.masheqal.app.data.QuranEditionRepository
import kotlinx.coroutines.launch

private data class ReaderVoice(val id: String, val name: String)
private val settingsVoices = listOf(
    ReaderVoice("ar.alafasy", "Mishary Rashid Alafasy"),
    ReaderVoice("ar.sudais", "Abdul Rahman Al-Sudais"),
    ReaderVoice("ar.shuraim", "Saud Al-Shuraim"),
    ReaderVoice("ar.husary", "Mahmoud Khalil Al-Husary"),
    ReaderVoice("ar.minshawi", "Mohamed Siddiq Al-Minshawi"),
    ReaderVoice("ar.minshawimujawwad", "Al-Minshawi — Mujawwad"),
    ReaderVoice("ar.abdulbasit", "Abdul Basit Abdul Samad"),
    ReaderVoice("ar.abdulbasitmujawwad", "Abdul Basit — Mujawwad"),
    ReaderVoice("ar.ajamy", "Ahmed Al-Ajamy"),
    ReaderVoice("ar.muhammadayoub", "Muhammad Ayyoub"),
    ReaderVoice("ar.hudhaify", "Ali Al-Hudhaify"),
    ReaderVoice("ar.muhammadjibreel", "Muhammad Jibreel"),
    ReaderVoice("ar.parhizgar", "Shahriar Parhizgar")
)

@Composable
fun SettingsScreen(
    app: MasheqalApp,
    nav: NavHostController,
    onLanguage: (String) -> Unit,
    onRequestLocation: () -> Unit
) {
    val settings by app.settings.state.collectAsState(initial = SettingsState())
    val reading by app.personal.reading.collectAsState(initial = com.masheqal.app.data.ReadingPosition())
    val khatmah by app.personal.khatmah.collectAsState(initial = com.masheqal.app.data.KhatmahState())
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current
    var message by remember { mutableStateOf<String?>(null) }
    var showPrivacy by remember { mutableStateOf(false) }
    var showPrayerMethods by remember { mutableStateOf(false) }
    var showReciters by remember { mutableStateOf(false) }
    var showTranslations by remember { mutableStateOf(false) }
    var showTafsirs by remember { mutableStateOf(false) }
    var reciterSearch by remember { mutableStateOf("") }
    var extraVoices by remember { mutableStateOf(emptyList<FullSurahReciter>()) }
    var translationSearch by remember { mutableStateOf("") }
    var tafsirSearch by remember { mutableStateOf("") }
    val editionRepository = remember(context) { QuranEditionRepository(context) }
    var availableTranslations by remember(context) { mutableStateOf(editionRepository.fallbackTranslations()) }
    var availableTafsirs by remember(context) { mutableStateOf(editionRepository.fallbackTafsirs()) }
    val backupExported = stringResource(R.string.backup_exported)
    val backupRestored = stringResource(R.string.backup_restored)
    val fullSurahAudioLabel = stringResource(R.string.full_surah_audio_label)

    LaunchedEffect(context) {
        extraVoices = runCatching { Mp3QuranReciterRepository(context).loadArabicReciters() }
            .getOrDefault(emptyList())
        val catalogue = runCatching { editionRepository.loadCatalog() }.getOrDefault(emptyList())
        availableTranslations = (catalogue.filter { it.type == "translation" } + editionRepository.fallbackTranslations())
            .distinctBy { it.identifier }
        availableTafsirs = (catalogue.filter { it.type == "tafsir" } + editionRepository.fallbackTafsirs())
            .distinctBy { it.identifier }
    }

    val createBackup = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            message = BackupRepository.export(context, uri, app.userDb, settings, reading, khatmah)
                .fold({ backupExported }, { "Backup error: ${it.message}" })
        }
    }
    val restoreBackup = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            BackupRepository.restore(context, uri, app.userDb)
                .onSuccess { backup ->
                    scope.launch {
                        app.settings.setTheme(backup.settings.theme)
                        app.settings.setLanguage(backup.settings.language)
                        app.settings.setTasbih(backup.settings.tasbihCount)
                        app.settings.setPrayerMethod(backup.settings.prayerMethod)
                        app.settings.setMadhhab(backup.settings.madhhab)
                        app.settings.setAwake(backup.settings.keepScreenAwake)
                        app.settings.setReciter(backup.settings.reciter)
                        app.settings.setShowEnglishTranslation(backup.settings.showEnglishTranslation)
                        app.settings.setTranslationEdition(backup.settings.translationEdition)
                        app.settings.setTafsirEdition(backup.settings.tafsirEdition)
                        app.settings.setShowTafsir(backup.settings.showTafsir)
                        app.settings.setAdhanRecordingId(backup.settings.adhanRecordingId)
                        app.settings.setPrayerRemindersEnabled(backup.settings.prayerRemindersEnabled)
                        app.settings.setPlayFullAdhan(backup.settings.playFullAdhan)
                        app.personal.setReading(backup.reading.surah, backup.reading.ayah)
                        app.personal.setKhatmah(
                            backup.khatmah.days,
                            backup.khatmah.targetPages,
                            backup.khatmah.readPages,
                            backup.khatmah.active
                        )
                    }
                    onLanguage(backup.settings.language)
                    message = backupRestored
                }
                .onFailure { message = "Backup error: ${it.message}" }
        }
    }

    val selectableVoices = settingsVoices + extraVoices.map { voice ->
        ReaderVoice(
            voice.id,
            voice.name + " — " + voice.moshafName + " · " + fullSurahAudioLabel
        )
    }
    val filteredVoices = remember(selectableVoices, reciterSearch) {
        selectableVoices.filter { it.name.contains(reciterSearch.trim(), ignoreCase = true) }
    }
    val selectedVoice = selectableVoices.firstOrNull { it.id == settings.reciter } ?: settingsVoices.first()
    val methodNames = listOf(
        "MWL" to R.string.method_mwl,
        "EGYPTIAN" to R.string.method_egyptian,
        "UMM_AL_QURA" to R.string.method_umm_al_qura,
        "KARACHI" to R.string.method_karachi,
        "ISNA" to R.string.method_isna,
        "TEHRAN" to R.string.method_tehran,
        "TURKEY" to R.string.method_turkey,
        "GULF" to R.string.method_gulf,
        "KUWAIT" to R.string.method_kuwait,
        "QATAR" to R.string.method_qatar,
        "SINGAPORE" to R.string.method_singapore,
        "FRANCE" to R.string.method_france,
        "RUSSIA" to R.string.method_russia,
        "DUBAI" to R.string.method_dubai
    )
    val selectedMethod = methodNames.firstOrNull { it.first == settings.prayerMethod } ?: methodNames.first()
    val filteredTranslations = remember(availableTranslations, translationSearch) {
        availableTranslations.filter { edition ->
            translationSearch.isBlank() || edition.name.contains(translationSearch.trim(), ignoreCase = true) ||
                edition.englishName.contains(translationSearch.trim(), ignoreCase = true) ||
                edition.language.contains(translationSearch.trim(), ignoreCase = true) ||
                edition.identifier.contains(translationSearch.trim(), ignoreCase = true)
        }
    }
    val filteredTafsirs = remember(availableTafsirs, tafsirSearch) {
        availableTafsirs.filter { edition ->
            tafsirSearch.isBlank() || edition.name.contains(tafsirSearch.trim(), ignoreCase = true) ||
                edition.englishName.contains(tafsirSearch.trim(), ignoreCase = true) ||
                edition.language.contains(tafsirSearch.trim(), ignoreCase = true) ||
                edition.identifier.contains(tafsirSearch.trim(), ignoreCase = true)
        }
    }
    val selectedTranslation = availableTranslations.firstOrNull { it.identifier == settings.translationEdition } ?: availableTranslations.first()
    val selectedTafsir = availableTafsirs.firstOrNull { it.identifier == settings.tafsirEdition } ?: availableTafsirs.first()

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Card(
            modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth().animateContentSize(),
            shape = RoundedCornerShape(30.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Box(
                Modifier.fillMaxWidth().background(
                    Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.84f),
                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.58f)
                        )
                    )
                )
            ) {
                Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color.White.copy(alpha = 0.16f)
                        ) {
                            Icon(Icons.Default.Tune, null, Modifier.padding(12.dp).size(28.dp), tint = Color.White)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                stringResource(R.string.settings),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                stringResource(R.string.settings_subtitle),
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.84f)
                            )
                        }
                        IconButton(onClick = { nav.popBackStack() }) {
                            Icon(Icons.Default.Close, stringResource(R.string.done), tint = Color.White)
                        }
                    }
                    Text(
                        stringResource(R.string.settings_personalize),
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White.copy(alpha = 0.92f)
                    )
                }
            }
        }

        SettingsSection(
            title = stringResource(R.string.language),
            subtitle = stringResource(R.string.settings_language_hint),
            icon = Icons.Default.Language
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("ckb" to "کوردی", "ar" to "العربية", "en" to "English").forEach { (code, label) ->
                    FilterChip(
                        selected = settings.language == code,
                        onClick = {
                            scope.launch { app.settings.setLanguage(code) }
                            onLanguage(code)
                        },
                        label = { Text(label) },
                        modifier = Modifier.weight(1f),
                        leadingIcon = if (settings.language == code) {
                            { Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp)) }
                        } else null
                    )
                }
            }
        }

        SettingsSection(
            title = stringResource(R.string.theme),
            subtitle = stringResource(R.string.settings_theme_hint),
            icon = Icons.Default.Palette
        ) {
            val themes = listOf(
                "system" to R.string.system,
                "light" to R.string.light,
                "dark" to R.string.dark,
                "amoled" to R.string.amoled,
                "high_contrast" to R.string.high_contrast
            )
            themes.chunked(3).forEach { rowThemes ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    rowThemes.forEach { (code, label) ->
                        FilterChip(
                            selected = settings.theme == code,
                            onClick = { scope.launch { app.settings.setTheme(code) } },
                            label = { Text(stringResource(label)) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    repeat(3 - rowThemes.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }

        SettingsSection(
            title = stringResource(R.string.prayer_preferences),
            subtitle = stringResource(R.string.settings_prayer_hint),
            icon = Icons.Default.Schedule
        ) {
            OutlinedCard(
                onClick = { showPrayerMethods = true },
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Calculate, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.prayer_method), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(stringResource(selectedMethod.second), style = MaterialTheme.typography.titleMedium)
                    }
                    Icon(Icons.Default.ExpandMore, null)
                }
            }
            Text(stringResource(R.string.madhhab), style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = settings.madhhab == "SHAFI",
                    onClick = { scope.launch { app.settings.setMadhhab("SHAFI") } },
                    label = { Text(stringResource(R.string.madhhab_shafi)) },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = settings.madhhab == "HANAFI",
                    onClick = { scope.launch { app.settings.setMadhhab("HANAFI") } },
                    label = { Text(stringResource(R.string.madhhab_hanafi)) },
                    modifier = Modifier.weight(1f)
                )
            }
            OutlinedCard(
                onClick = onRequestLocation,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.MyLocation, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.settings_location_title), style = MaterialTheme.typography.titleSmall)
                        Text(stringResource(R.string.settings_location_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Icon(Icons.Default.ChevronRight, null)
                }
            }
        }

        SettingsSection(
            title = stringResource(R.string.adhan_library),
            subtitle = stringResource(R.string.adhan_library_subtitle),
            icon = Icons.Default.GraphicEq
        ) {
            OutlinedCard(
                onClick = { nav.navigate("adhan") },
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.RecordVoiceOver, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.adhan_open_library), style = MaterialTheme.typography.titleSmall)
                        Text(stringResource(R.string.adhan_play_full), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Icon(Icons.Default.ChevronRight, null)
                }
            }
            ToggleRow(
                title = stringResource(R.string.adhan_play_full),
                subtitle = stringResource(R.string.settings_adhan_play_hint),
                checked = settings.playFullAdhan,
                icon = Icons.Default.NotificationsActive,
                onChange = { enabled -> scope.launch { app.settings.setPlayFullAdhan(enabled) } }
            )
            OutlinedCard(
                onClick = { nav.navigate("prayer") },
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Schedule, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.prayer), style = MaterialTheme.typography.titleSmall)
                        Text(
                            stringResource(if (settings.prayerRemindersEnabled) R.string.adhan_reminders_enabled else R.string.adhan_reminders_disabled),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(Icons.Default.ChevronRight, null)
                }
            }
        }

        SettingsSection(
            title = stringResource(R.string.quran_reading_preferences),
            subtitle = stringResource(R.string.settings_quran_hint),
            icon = Icons.Default.MenuBook
        ) {
            OutlinedCard(
                onClick = { showReciters = true },
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.RecordVoiceOver, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.select_reciter), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(selectedVoice.name, style = MaterialTheme.typography.titleSmall)
                    }
                    Icon(Icons.Default.ExpandMore, null)
                }
            }
            Row(
                Modifier.fillMaxWidth().padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Translate, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.english_translation), style = MaterialTheme.typography.titleSmall)
                    Text(stringResource(R.string.settings_translation_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(
                    checked = settings.showEnglishTranslation,
                    onCheckedChange = { enabled -> scope.launch { app.settings.setShowEnglishTranslation(enabled) } }
                )
            }
            OutlinedCard(
                onClick = { showTranslations = true },
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Translate, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.select_translation), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(selectedTranslation.englishName, style = MaterialTheme.typography.titleSmall)
                    }
                    Icon(Icons.Default.ExpandMore, null)
                }
            }
            OutlinedCard(
                onClick = { showTafsirs = true },
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AutoStories, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.select_tafsir), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(selectedTafsir.englishName, style = MaterialTheme.typography.titleSmall)
                    }
                    Icon(Icons.Default.ExpandMore, null)
                }
            }
            ToggleRow(
                title = stringResource(R.string.show_tafsir),
                subtitle = stringResource(R.string.settings_tafsir_hint),
                checked = settings.showTafsir,
                icon = Icons.Default.AutoStories,
                onChange = { enabled -> scope.launch { app.settings.setShowTafsir(enabled) } }
            )
            OutlinedCard(
                onClick = { nav.navigate("content") },
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LibraryBooks, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.content_center), style = MaterialTheme.typography.titleSmall)
                        Text(stringResource(R.string.settings_content_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Icon(Icons.Default.ChevronRight, null)
                }
            }
        }

        SettingsSection(
            title = stringResource(R.string.settings_reading_display),
            subtitle = stringResource(R.string.settings_display_hint),
            icon = Icons.Default.Visibility
        ) {
            ToggleRow(
                title = stringResource(R.string.keep_screen_awake),
                subtitle = stringResource(R.string.settings_keep_awake_hint),
                checked = settings.keepScreenAwake,
                icon = Icons.Default.ScreenLockPortrait,
                onChange = { enabled -> scope.launch { app.settings.setAwake(enabled) }
                }
            )
        }

        SettingsSection(
            title = stringResource(R.string.settings_data),
            subtitle = stringResource(R.string.settings_data_hint),
            icon = Icons.Default.Security
        ) {
            ActionSettingRow(
                title = stringResource(R.string.export_backup),
                subtitle = stringResource(R.string.local_only),
                icon = Icons.Default.FileDownload,
                onClick = { createBackup.launch("masheqal-backup.json") }
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
            ActionSettingRow(
                title = stringResource(R.string.restore_backup),
                subtitle = stringResource(R.string.settings_restore_hint),
                icon = Icons.Default.FileUpload,
                onClick = { restoreBackup.launch(arrayOf("application/json", "text/*")) }
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
            ActionSettingRow(
                title = stringResource(R.string.privacy),
                subtitle = stringResource(R.string.settings_privacy_hint),
                icon = Icons.Default.Lock,
                onClick = { showPrivacy = true }
            )
        }

        message?.let {
            Card(
                Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
                shape = RoundedCornerShape(18.dp)
            ) {
                Text(it, Modifier.padding(16.dp))
            }
        }
        Spacer(Modifier.height(18.dp))
    }

    if (showPrayerMethods) {
        AlertDialog(
            onDismissRequest = { showPrayerMethods = false },
            title = { Text(stringResource(R.string.prayer_method)) },
            text = {
                Column(Modifier.heightIn(max = 440.dp).verticalScroll(rememberScrollState())) {
                    methodNames.forEach { (code, label) ->
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = settings.prayerMethod == code,
                                onClick = {
                                    scope.launch { app.settings.setPrayerMethod(code) }
                                    showPrayerMethods = false
                                }
                            )
                            Text(stringResource(label), style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPrayerMethods = false }) { Text(stringResource(R.string.done)) }
            }
        )
    }

    if (showReciters) {
        AlertDialog(
            onDismissRequest = { showReciters = false },
            title = { Text(stringResource(R.string.select_reciter)) },
            text = {
                Column(Modifier.heightIn(max = 440.dp).verticalScroll(rememberScrollState())) {
                    OutlinedTextField(
                        value = reciterSearch,
                        onValueChange = { reciterSearch = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        placeholder = { Text(stringResource(R.string.reciter_search_hint)) },
                        leadingIcon = { Icon(Icons.Default.Search, null) },
                        trailingIcon = {
                            if (reciterSearch.isNotEmpty()) IconButton(onClick = { reciterSearch = "" }) {
                                Icon(Icons.Default.Close, stringResource(R.string.clear))
                            }
                        }
                    )
                    Text(stringResource(R.string.quran_reciter_sources_note), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    filteredVoices.forEach { voice ->
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = settings.reciter == voice.id,
                                onClick = {
                                    scope.launch { app.settings.setReciter(voice.id) }
                                    reciterSearch = ""
                                    showReciters = false
                                }
                            )
                            Text(voice.name, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showReciters = false }) { Text(stringResource(R.string.done)) }
            }
        )
    }

    if (showTranslations) {
        AlertDialog(
            onDismissRequest = { showTranslations = false },
            title = { Text(stringResource(R.string.select_translation)) },
            text = {
                Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
                    OutlinedTextField(
                        value = translationSearch,
                        onValueChange = { translationSearch = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        placeholder = { Text(stringResource(R.string.translation_search_hint)) },
                        leadingIcon = { Icon(Icons.Default.Search, null) },
                        trailingIcon = {
                            if (translationSearch.isNotEmpty()) IconButton(onClick = { translationSearch = "" }) {
                                Icon(Icons.Default.Close, stringResource(R.string.clear))
                            }
                        }
                    )
                    filteredTranslations.forEach { edition ->
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = settings.translationEdition == edition.identifier,
                                onClick = {
                                    scope.launch { app.settings.setTranslationEdition(edition.identifier) }
                                    showTranslations = false
                                    translationSearch = ""
                                }
                            )
                            Column(Modifier.weight(1f)) {
                                Text(edition.englishName, style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    "${edition.language.uppercase()} · ${edition.name}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showTranslations = false }) { Text(stringResource(R.string.done)) } }
        )
    }

    if (showTafsirs) {
        AlertDialog(
            onDismissRequest = { showTafsirs = false },
            title = { Text(stringResource(R.string.select_tafsir)) },
            text = {
                Column(Modifier.heightIn(max = 300.dp).verticalScroll(rememberScrollState())) {
                    OutlinedTextField(
                        value = tafsirSearch,
                        onValueChange = { tafsirSearch = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        placeholder = { Text(stringResource(R.string.tafsir_search_hint)) },
                        leadingIcon = { Icon(Icons.Default.Search, null) },
                        trailingIcon = {
                            if (tafsirSearch.isNotEmpty()) IconButton(onClick = { tafsirSearch = "" }) {
                                Icon(Icons.Default.Close, stringResource(R.string.clear))
                            }
                        }
                    )
                    filteredTafsirs.forEach { edition ->
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = settings.tafsirEdition == edition.identifier,
                                onClick = {
                                    scope.launch { app.settings.setTafsirEdition(edition.identifier) }
                                    showTafsirs = false
                                    tafsirSearch = ""
                                }
                            )
                            Column(Modifier.weight(1f)) {
                                Text(edition.englishName, style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    "${edition.language.uppercase()} · ${edition.name}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showTafsirs = false }) { Text(stringResource(R.string.done)) } }
        )
    }

    if (showPrivacy) {
        AlertDialog(
            onDismissRequest = { showPrivacy = false },
            title = { Text(stringResource(R.string.privacy)) },
            text = { Text(stringResource(R.string.privacy_explanation)) },
            confirmButton = {
                TextButton(onClick = { showPrivacy = false }) { Text(stringResource(R.string.done)) }
            }
        )
    }
}

@Composable
private fun SettingsSection(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth().animateContentSize(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                    Icon(icon, null, Modifier.padding(10.dp).size(22.dp), tint = MaterialTheme.colorScheme.primary)
                }
                Spacer(Modifier.width(11.dp))
                Column(Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            content()
        }
    }
}

@Composable
private fun ToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onChange: (Boolean) -> Unit
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun ActionSettingRow(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    OutlinedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(17.dp)
    ) {
        Row(Modifier.fillMaxWidth().padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
