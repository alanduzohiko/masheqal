package com.masheqal.app.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.masheqal.app.MasheqalApp
import com.masheqal.app.R
import com.masheqal.app.data.BackupRepository
import com.masheqal.app.data.SettingsState
import com.masheqal.app.services.QuranPlaybackService
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(app: MasheqalApp, nav: NavHostController, onLanguage: (String) -> Unit) {
    val settings by app.settings.state.collectAsState(initial = SettingsState())
    val reading by app.personal.reading.collectAsState(initial = com.masheqal.app.data.ReadingPosition())
    val khatmah by app.personal.khatmah.collectAsState(initial = com.masheqal.app.data.KhatmahState())
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current
    var message by remember { mutableStateOf<String?>(null) }
    var showPrivacy by remember { mutableStateOf(false) }
    var adhanVoice by remember {
        mutableStateOf(
            context.getSharedPreferences(QuranPlaybackService.ADHAN_PREFERENCES, android.content.Context.MODE_PRIVATE)
                .getString(QuranPlaybackService.ADHAN_VOICE_KEY, QuranPlaybackService.DEFAULT_ADHAN_VOICE)
                ?: QuranPlaybackService.DEFAULT_ADHAN_VOICE
        )
    }
    val adhanPreviewLabel = stringResource(R.string.adhan_preview)
    val backupExported = stringResource(R.string.backup_exported)
    val backupRestored = stringResource(R.string.backup_restored)
    val backupError = stringResource(R.string.backup_error)

    val createBackup = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            message = BackupRepository.export(
                context, uri, app.userDb, settings, reading, khatmah
            ).fold({ backupExported }, { backupError })
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
                .onFailure { message = backupError }
        }
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { nav.popBackStack() }) {
                Icon(Icons.Default.ArrowBack, null)
            }
            Text(
                stringResource(R.string.settings),
                style = MaterialTheme.typography.headlineMedium
            )
        }

        SettingsGroup(title = stringResource(R.string.language)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.horizontalScroll(rememberScrollState())
            ) {
                listOf("ar" to "العربية", "en" to "English").forEach { (code, label) ->
                    FilterChip(
                        selected = settings.language == code,
                        onClick = {
                            scope.launch { app.settings.setLanguage(code) }
                            onLanguage(code)
                        },
                        label = { Text(label) },
                        leadingIcon = { Icon(Icons.Default.Language, null) }
                    )
                }
            }
        }

        SettingsGroup(title = stringResource(R.string.theme)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.horizontalScroll(rememberScrollState())
            ) {
                listOf(
                    "system" to R.string.system,
                    "light" to R.string.light,
                    "dark" to R.string.dark,
                    "amoled" to R.string.amoled,
                    "high_contrast" to R.string.high_contrast
                ).forEach { (code, label) ->
                    FilterChip(
                        selected = settings.theme == code,
                        onClick = { scope.launch { app.settings.setTheme(code) } },
                        label = { Text(stringResource(label)) },
                        leadingIcon = { Icon(Icons.Default.SettingsBrightness, null) }
                    )
                }
            }
        }

        SettingsGroup(title = stringResource(R.string.prayer_method)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.horizontalScroll(rememberScrollState())
            ) {
                listOf("MWL", "EGYPTIAN", "UMM_AL_QURA", "KARACHI", "ISNA", "TEHRAN", "TURKEY").forEach { code ->
                    FilterChip(
                        selected = settings.prayerMethod == code,
                        onClick = { scope.launch { app.settings.setPrayerMethod(code) } },
                        label = { Text(code.replace("_", " ")) }
                    )
                }
            }
        }

        SettingsGroup(title = stringResource(R.string.madhhab)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("SHAFI", "HANAFI").forEach { code ->
                    FilterChip(
                        selected = settings.madhhab == code,
                        onClick = { scope.launch { app.settings.setMadhhab(code) } },
                        label = { Text(code) }
                    )
                }
            }
        }

        SettingsGroup(title = stringResource(R.string.adhan_voice)) {
            AdhanVoicePicker(
                selectedVoice = adhanVoice,
                onVoiceSelected = { voice ->
                    adhanVoice = voice
                    context.getSharedPreferences(
                        QuranPlaybackService.ADHAN_PREFERENCES,
                        android.content.Context.MODE_PRIVATE
                    ).edit().putString(QuranPlaybackService.ADHAN_VOICE_KEY, voice).apply()
                },
                onPreview = { voice -> previewAdhanVoice(context, voice, adhanPreviewLabel) }
            )
        }

        Card(
            Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(22.dp)
        ) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconBadge(Icons.Default.Visibility, emphasized = true)
                Spacer(Modifier.width(12.dp))
                Text(
                    stringResource(R.string.keep_screen_awake),
                    Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium
                )
                Switch(
                    checked = settings.keepScreenAwake,
                    onCheckedChange = { enabled ->
                        scope.launch { app.settings.setAwake(enabled) }
                    }
                )
            }
        }

        SettingsGroup(title = stringResource(R.string.content_center)) {
            FeatureCard(
                stringResource(R.string.content_center),
                stringResource(R.string.source_required),
                Icons.Default.Inventory2
            ) { nav.navigate("content") }
        }

        SettingsGroup(title = stringResource(R.string.export_backup)) {
            FeatureCard(
                stringResource(R.string.export_backup),
                stringResource(R.string.local_only),
                Icons.Default.FileDownload
            ) { createBackup.launch("masheqal-backup.json") }
            Spacer(Modifier.height(8.dp))
            FeatureCard(
                stringResource(R.string.restore_backup),
                stringResource(R.string.local_only),
                Icons.Default.FileUpload
            ) { restoreBackup.launch(arrayOf("application/json", "text/*")) }
        }

        FeatureCard(
            stringResource(R.string.privacy),
            stringResource(R.string.local_only),
            Icons.Default.Lock
        ) { showPrivacy = true }

        message?.let { msg ->
            Card(
                Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp)
            ) {
                Text(msg, Modifier.padding(16.dp))
            }
        }
        Spacer(Modifier.height(20.dp))
    }

    if (showPrivacy) {
        AlertDialog(
            onDismissRequest = { showPrivacy = false },
            title = { Text(stringResource(R.string.privacy)) },
            text = { Text(stringResource(R.string.privacy_explanation)) },
            confirmButton = {
                TextButton(onClick = { showPrivacy = false }) {
                    Text(stringResource(R.string.done))
                }
            }
        )
    }
}

@Composable
private fun SettingsGroup(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
        )
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            content()
        }
    }
}