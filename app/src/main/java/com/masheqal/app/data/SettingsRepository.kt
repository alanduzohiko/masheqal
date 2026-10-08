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

data class SettingsState(val theme: String = "system", val language: String = "ckb", val tasbihCount: Int = 0, val prayerMethod: String = "MWL", val madhhab: String = "SHAFI", val keepScreenAwake: Boolean = false)
class SettingsRepository(private val context: Context) {
    private object Keys { val theme=stringPreferencesKey("theme"); val language=stringPreferencesKey("language"); val tasbih=intPreferencesKey("tasbih"); val prayerMethod=stringPreferencesKey("prayerMethod"); val madhhab=stringPreferencesKey("madhhab"); val awake=booleanPreferencesKey("awake") }
    val state: Flow<SettingsState> = context.settingsDataStore.data.map { p -> SettingsState(p[Keys.theme] ?: "system", p[Keys.language] ?: "ckb", p[Keys.tasbih] ?: 0, p[Keys.prayerMethod] ?: "MWL", p[Keys.madhhab] ?: "SHAFI", p[Keys.awake] ?: false) }
    suspend fun setTheme(v:String)=context.settingsDataStore.edit{it[Keys.theme]=v}
    suspend fun setLanguage(v:String)=context.settingsDataStore.edit{it[Keys.language]=v}
    suspend fun setTasbih(v:Int)=context.settingsDataStore.edit{it[Keys.tasbih]=v.coerceAtLeast(0)}
    suspend fun setPrayerMethod(v:String)=context.settingsDataStore.edit{it[Keys.prayerMethod]=v}
    suspend fun setMadhhab(v:String)=context.settingsDataStore.edit{it[Keys.madhhab]=v}
    suspend fun setAwake(v:Boolean)=context.settingsDataStore.edit{it[Keys.awake]=v}
}
