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
        translationEn = "English meaning"
    )

    @Test
    fun parsesQuranReferencesWithWesternArabicAndPersianDigits() {
        assertEquals(2 to 255, QuranRepository.parseReferenceText("2:255"))
        assertEquals(2 to 255, QuranRepository.parseReferenceText("٢:٢٥٥"))
        assertEquals(2 to 255, QuranRepository.parseReferenceText("۲ ۲۵۵"))
        assertEquals(2 to 255, QuranRepository.parseReferenceText("٢：٢٥٥"))
    }

    @Test
    fun rejectsMalformedOrOutOfRangeQuranReferences() {
        assertNull(QuranRepository.parseReferenceText("115:1"))
        assertNull(QuranRepository.parseReferenceText("2:0"))
        assertNull(QuranRepository.parseReferenceText("two:255"))
    }

    @Test
    fun englishInterfaceUsesEnglishTranslationCaseInsensitively() {
        assertEquals("English meaning", verse.translationFor("en"))
        assertEquals("English meaning", verse.translationFor("EN"))
    }

    @Test
    fun arabicInterfaceDoesNotSilentlySubstituteEnglishTranslation() {
        assertNull(verse.translationFor("ar"))
    }

    @Test
    fun deferredSoraniLocaleDoesNotExposeAnUnbundledTranslation() {
        assertNull(verse.translationFor("ckb"))
        assertNull(verse.translationFor("CKB"))
    }

    @Test
    fun unknownLanguageDoesNotSilentlySubstituteEnglishTranslation() {
        assertNull(verse.translationFor("unknown"))
    }
}
