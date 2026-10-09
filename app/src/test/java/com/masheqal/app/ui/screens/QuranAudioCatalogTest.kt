package com.masheqal.app.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Test

class QuranAudioCatalogTest {
    @Test
    fun buildsVerseAudioUrlFromGlobalAyahNumber() {
        assertEquals(
            "https://cdn.islamic.network/quran/audio/128/ar.alafasy/262.mp3",
            QuranAudioCatalog.ayahUrl(262, "ar.alafasy")
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsAyahNumbersOutsideQuranRange() {
        QuranAudioCatalog.ayahUrl(6237, "ar.alafasy")
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsUnknownAudioEdition() {
        QuranAudioCatalog.ayahUrl(1, "ar.unknown")
    }
}
