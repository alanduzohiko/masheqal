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

    @Test
    fun buildsSurahUrlForLiveVerifiedNasserAlQatamiEdition() {
        assertEquals(
            "https://cdn.islamic.network/quran/audio-surah/128/ar.nasseralqatami/1.mp3",
            QuranAudioCatalog.surahUrl(1, "ar.nasseralqatami")
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsSurahEditionNotInLiveVerifiedCatalogue() {
        QuranAudioCatalog.surahUrl(1, "ar.husary")
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsAyahNumbersOutsideQuranRange() {
        QuranAudioCatalog.ayahUrl(6237, "ar.alafasy")
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsUnknownAudioEdition() {
        QuranAudioCatalog.ayahUrl(1, "ar.unknown")
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsSurahOnlyEditionForVerseAudio() {
        QuranAudioCatalog.ayahUrl(1, "ar.nasseralqatami")
    }
}
