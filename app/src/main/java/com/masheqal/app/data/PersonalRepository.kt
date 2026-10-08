package com.masheqal.app.data

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore

private val Context.personalDataStore by preferencesDataStore("masheqal_personal")

data class ReadingPosition(val surah: Int = 1, val ayah: Int = 1)
data class KhatmahState(val days: Int = 30, val targetPages: Int = 604, val readPages: Int = 0, val active: Boolean = false)

class PersonalRepository(private val context: Context) {
    private object Keys {
        val surah = intPreferencesKey("reading_surah")
        val ayah = intPreferencesKey("reading_ayah")
        val khatmahDays = intPreferencesKey("khatmah_days")
        val khatmahTarget = intPreferencesKey("khatmah_target_pages")
        val khatmahRead = intPreferencesKey("khatmah_read_pages")
        val khatmahActive = androidx.datastore.preferences.core.booleanPreferencesKey("khatmah_active")
        val homeOrder = stringPreferencesKey("home_order")
    }
    val reading: Flow<ReadingPosition> = context.personalDataStore.data.map { ReadingPosition(it[Keys.surah] ?: 1, it[Keys.ayah] ?: 1) }
    val khatmah: Flow<KhatmahState> = context.personalDataStore.data.map {
        KhatmahState(it[Keys.khatmahDays] ?: 30, it[Keys.khatmahTarget] ?: 604, it[Keys.khatmahRead] ?: 0, it[Keys.khatmahActive] ?: false)
    }
    val homeOrder: Flow<String?> = context.personalDataStore.data.map { it[Keys.homeOrder] }

    suspend fun setReading(surah: Int, ayah: Int) {
        context.personalDataStore.edit { it[Keys.surah] = surah; it[Keys.ayah] = ayah }
    }
    suspend fun setKhatmah(days: Int, targetPages: Int, readPages: Int = 0, active: Boolean = true) {
        context.personalDataStore.edit {
            it[Keys.khatmahDays] = days.coerceAtLeast(1)
            it[Keys.khatmahTarget] = targetPages.coerceAtLeast(1)
            it[Keys.khatmahRead] = readPages.coerceIn(0, targetPages.coerceAtLeast(1))
            it[Keys.khatmahActive] = active
        }
    }
    suspend fun addKhatmahPages(delta: Int) {
        context.personalDataStore.edit {
            val target = it[Keys.khatmahTarget] ?: 604
            it[Keys.khatmahRead] = ((it[Keys.khatmahRead] ?: 0) + delta).coerceIn(0, target)
            it[Keys.khatmahActive] = true
        }
    }
    suspend fun pauseKhatmah() { context.personalDataStore.edit { it[Keys.khatmahActive] = false } }
    suspend fun setHomeOrder(order: String) { context.personalDataStore.edit { it[Keys.homeOrder] = order } }
}
