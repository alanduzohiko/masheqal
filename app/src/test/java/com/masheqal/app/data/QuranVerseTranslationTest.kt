package com.masheqal.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class QuranVerseTranslationTest {
    private val verse = QuranVerse(
        id = 1,
        surah = 1,
        ayah = 1,
        text = "Arabic Quran text",
        translationEn = "English meaning",
        translationCkb = "واتای سۆرانی"
    )

    @Test
    fun soraniInterfaceSelectsSoraniTranslation() {
        assertEquals("واتای سۆرانی", verse.translationFor("ckb"))
        assertEquals("واتای سۆرانی", verse.translationFor("CKB"))
    }

    @Test
    fun nonSoraniInterfaceUsesEnglishTranslation() {
        assertEquals("English meaning", verse.translationFor("en"))
        assertEquals("English meaning", verse.translationFor("ar"))
    }

    @Test
    fun missingSoraniTranslationIsNotSilentlyReplacedWithEnglish() {
        val verseWithoutSorani = verse.copy(translationCkb = null)
        assertNull(verseWithoutSorani.translationFor("ckb"))
    }
}