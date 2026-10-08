package com.masheqal.app.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.SettingsBrightness
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.masheqal.app.MasheqalApp
import com.masheqal.app.R
import com.masheqal.app.data.BackupRepository
import com.masheqal.app.data.SettingsState
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
    val backupExported = stringResource(R.string.backup_exported)
    val backupRestored = stringResource(R.string.backup_restored)
    val createBackup = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) message = BackupRepository.export(context, uri, app.userDb, settings, reading, khatmah).fold({ backupExported }, { "Backup error: ${it.message}" })
    }
    val restoreBackup = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            BackupRepository.restore(context, uri, app.userDb).onSuccess {
                scope.launch { app.settings.setTheme(it.settings.theme); app.settings.setLanguage(it.settings.language); app.settings.setTasbih(it.settings.tasbihCount); app.settings.setPrayerMethod(it.settings.prayerMethod); app.settings.setMadhhab(it.settings.madhhab); app.settings.setAwake(it.settings.keepScreenAwake); app.personal.setReading(it.reading.surah,it.reading.ayah); app.personal.setKhatmah(it.khatmah.days,it.khatmah.targetPages,it.khatmah.readPages,it.khatmah.active) }; onLanguage(it.settings.language)
                message = backupRestored
            }.onFailure { message = "Backup error: ${it.message}" }
        }
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
        Row(Modifier.fillMaxWidth()) { IconButton(onClick={nav.popBackStack()}) { Icon(Icons.Default.ArrowBack,null) }; Text(stringResource(R.string.settings),style=MaterialTheme.typography.headlineSmall,modifier=Modifier.padding(top=12.dp)) }
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.language),style=MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement=Arrangement.spacedBy(8.dp), modifier=Modifier.horizontalScroll(rememberScrollState())) {
            listOf("ckb" to "کوردی", "ar" to "العربية", "en" to "English").forEach { (code,label) -> FilterChip(selected=settings.language==code,onClick={scope.launch{app.settings.setLanguage(code)};onLanguage(code)},label={Text(label)},leadingIcon={Icon(Icons.Default.Language,null)}) }
        }
        Spacer(Modifier.height(14.dp)); Text(stringResource(R.string.theme),style=MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement=Arrangement.spacedBy(8.dp), modifier=Modifier.horizontalScroll(rememberScrollState())) {
            listOf("system" to R.string.system,"light" to R.string.light,"dark" to R.string.dark,"amoled" to R.string.amoled,"high_contrast" to R.string.high_contrast).forEach { (code,label) -> FilterChip(selected=settings.theme==code,onClick={scope.launch{app.settings.setTheme(code)}},label={Text(stringResource(label))},leadingIcon={Icon(Icons.Default.SettingsBrightness,null)}) }
        }
        Spacer(Modifier.height(14.dp))
        Text(stringResource(R.string.prayer_method),style=MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement=Arrangement.spacedBy(8.dp),modifier=Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
            listOf("MWL","EGYPTIAN","UMM_AL_QURA","KARACHI","ISNA","TEHRAN","TURKEY").forEach { code -> FilterChip(selected=settings.prayerMethod==code,onClick={scope.launch{app.settings.setPrayerMethod(code)}},label={Text(code.replace('_',' '))}) }
        }
        Spacer(Modifier.height(8.dp)); Text(stringResource(R.string.madhhab),style=MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement=Arrangement.spacedBy(8.dp), modifier=Modifier.horizontalScroll(rememberScrollState())) { listOf("SHAFI","HANAFI").forEach { code -> FilterChip(selected=settings.madhhab==code,onClick={scope.launch{app.settings.setMadhhab(code)}},label={Text(code)}) } }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.SpaceBetween) { Text(stringResource(R.string.keep_screen_awake)); Switch(checked=settings.keepScreenAwake,onCheckedChange={scope.launch{app.settings.setAwake(it)}}) }
        Spacer(Modifier.height(12.dp)); HorizontalDivider(); Spacer(Modifier.height(12.dp))
        FeatureCard(stringResource(R.string.content_center), stringResource(R.string.source_required), onClick={nav.navigate("content")})
        Spacer(Modifier.height(8.dp)); FeatureCard(stringResource(R.string.export_backup), stringResource(R.string.local_only), Icons.Default.FileDownload, onClick={createBackup.launch("masheqal-backup.json")})
        Spacer(Modifier.height(8.dp)); FeatureCard(stringResource(R.string.restore_backup), stringResource(R.string.local_only), Icons.Default.FileUpload, onClick={restoreBackup.launch(arrayOf("application/json","text/*"))})
        Spacer(Modifier.height(8.dp)); FeatureCard(stringResource(R.string.privacy), stringResource(R.string.local_only), onClick={showPrivacy=true})
        if (message != null) { Spacer(Modifier.height(12.dp)); Text(message!!, style=MaterialTheme.typography.bodyMedium) }
    }
    if (showPrivacy) AlertDialog(onDismissRequest={showPrivacy=false}, title={Text(stringResource(R.string.privacy))}, text={Text(stringResource(R.string.privacy_explanation))}, confirmButton={TextButton(onClick={showPrivacy=false}){Text(stringResource(R.string.done))}})
}
