package com.masheqal.app.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore("masheqal_settings")

data class SettingsState(val theme: String = "system", val language: String = "ckb", val tasbihCount: Int = 0, val prayerMethod: String = "MWL", val madhhab: String = "SHAFI", val keepScreenAwake: Boolean = false, val reciter: String = "ar.alafasy", val showEnglishTranslation: Boolean = true, val onboardingCompleted: Boolean = false, val translationEdition: String = "en.sahih", val tafsirEdition: String = "ar.muyassar", val showTafsir: Boolean = false, val adhanRecordingId: String = "adhan-198", val prayerRemindersEnabled: Boolean = false, val playFullAdhan: Boolean = true, val showTajweedColors: Boolean = false, val quranScriptEdition: String = "quran-uthmani")
class SettingsRepository(private val context: Context) {
    private object Keys { val theme=stringPreferencesKey("theme"); val language=stringPreferencesKey("language"); val tasbih=intPreferencesKey("tasbih"); val prayerMethod=stringPreferencesKey("prayerMethod"); val madhhab=stringPreferencesKey("madhhab"); val awake=booleanPreferencesKey("awake"); val reciter=stringPreferencesKey("reciter"); val showEnglishTranslation=booleanPreferencesKey("showEnglishTranslation"); val onboardingCompleted=booleanPreferencesKey("onboardingCompleted"); val translationEdition=stringPreferencesKey("translationEdition"); val tafsirEdition=stringPreferencesKey("tafsirEdition"); val showTafsir=booleanPreferencesKey("showTafsir"); val adhanRecordingId=stringPreferencesKey("adhanRecordingId"); val prayerRemindersEnabled=booleanPreferencesKey("prayerRemindersEnabled"); val playFullAdhan=booleanPreferencesKey("playFullAdhan"); val showTajweedColors=booleanPreferencesKey("showTajweedColors"); val quranScriptEdition=stringPreferencesKey("quranScriptEdition") }
    val state: Flow<SettingsState> = context.settingsDataStore.data.map { p -> SettingsState(p[Keys.theme] ?: "system", p[Keys.language] ?: "ckb", p[Keys.tasbih] ?: 0, p[Keys.prayerMethod] ?: "MWL", p[Keys.madhhab] ?: "SHAFI", p[Keys.awake] ?: false, p[Keys.reciter] ?: "ar.alafasy", p[Keys.showEnglishTranslation] ?: true, p[Keys.onboardingCompleted] ?: false, p[Keys.translationEdition] ?: "en.sahih", p[Keys.tafsirEdition] ?: "ar.muyassar", p[Keys.showTafsir] ?: false, p[Keys.adhanRecordingId] ?: "adhan-198", p[Keys.prayerRemindersEnabled] ?: false, p[Keys.playFullAdhan] ?: true, p[Keys.showTajweedColors] ?: false, p[Keys.quranScriptEdition] ?: QuranScriptRepository.DEFAULT_EDITION) }
    suspend fun setTheme(v:String)=context.settingsDataStore.edit{it[Keys.theme]=v}
    suspend fun setLanguage(v:String)=context.settingsDataStore.edit{it[Keys.language]=v}
    suspend fun setTasbih(v:Int)=context.settingsDataStore.edit{it[Keys.tasbih]=v.coerceAtLeast(0)}
    suspend fun setPrayerMethod(v:String)=context.settingsDataStore.edit{it[Keys.prayerMethod]=v}
    suspend fun setMadhhab(v:String)=context.settingsDataStore.edit{it[Keys.madhhab]=v}
    suspend fun setAwake(v:Boolean)=context.settingsDataStore.edit{it[Keys.awake]=v}
    suspend fun setReciter(v:String)=context.settingsDataStore.edit{it[Keys.reciter]=v}
    suspend fun setShowEnglishTranslation(v:Boolean)=context.settingsDataStore.edit{it[Keys.showEnglishTranslation]=v}
    suspend fun setOnboardingCompleted(v:Boolean)=context.settingsDataStore.edit{it[Keys.onboardingCompleted]=v}
    suspend fun setTranslationEdition(v:String)=context.settingsDataStore.edit{it[Keys.translationEdition]=v}
    suspend fun setTafsirEdition(v:String)=context.settingsDataStore.edit{it[Keys.tafsirEdition]=v}
    suspend fun setShowTafsir(v:Boolean)=context.settingsDataStore.edit{it[Keys.showTafsir]=v}
    suspend fun setAdhanRecordingId(v:String)=context.settingsDataStore.edit{it[Keys.adhanRecordingId]=v}
    suspend fun setPrayerRemindersEnabled(v:Boolean)=context.settingsDataStore.edit{it[Keys.prayerRemindersEnabled]=v}
    suspend fun setPlayFullAdhan(v:Boolean)=context.settingsDataStore.edit{it[Keys.playFullAdhan]=v}
    suspend fun setShowTajweedColors(v:Boolean)=context.settingsDataStore.edit{it[Keys.showTajweedColors]=v}
    suspend fun setQuranScriptEdition(v:String)=context.settingsDataStore.edit {
        it[Keys.quranScriptEdition] = v.takeIf { id -> id.matches(Regex("[a-z0-9.-]{2,80}")) }
            ?: QuranScriptRepository.DEFAULT_EDITION
    }
}
