package com.masheqal.app

import android.app.Application
import com.masheqal.app.data.QuranRepository
import com.masheqal.app.data.SettingsRepository
import com.masheqal.app.data.UserDatabase
import com.masheqal.app.data.PersonalRepository
import com.masheqal.app.data.AdhkarRepository
import com.masheqal.app.data.NamesOfAllahRepository

class MasheqalApp : Application() {
    lateinit var quran: QuranRepository
    lateinit var userDb: UserDatabase
    lateinit var settings: SettingsRepository
    lateinit var personal: PersonalRepository
    lateinit var adhkar: AdhkarRepository
    lateinit var namesOfAllah: NamesOfAllahRepository
    override fun onCreate() {
        super.onCreate()
        quran = QuranRepository(this)
        userDb = UserDatabase(this)
        settings = SettingsRepository(this)
        personal = PersonalRepository(this)
        adhkar = AdhkarRepository(this)
        namesOfAllah = NamesOfAllahRepository(this)
    }
}
