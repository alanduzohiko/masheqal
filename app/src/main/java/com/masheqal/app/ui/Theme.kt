package com.masheqal.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Emerald = Color(0xFF155E4B)
private val Gold = Color(0xFFA9823C)
private val Ivory = Color(0xFFFBF8F1)

@Composable
fun MasheqalTheme(theme: String, content: @Composable () -> Unit) {
    val dark = when (theme) { "dark", "amoled" -> true; "light", "high_contrast" -> false; else -> isSystemInDarkTheme() }
    val colors = when (theme) {
        "amoled" -> darkColorScheme(primary=Color(0xFF62C9AA), secondary=Color(0xFFE1C37A), background=Color.Black, surface=Color.Black, surfaceVariant=Color(0xFF101512))
        "high_contrast" -> lightColorScheme(primary=Color(0xFF004838), secondary=Color(0xFF6B4A00), background=Color.White, surface=Color.White, onSurface=Color.Black, onBackground=Color.Black)
        "dark" -> darkColorScheme(primary=Color(0xFF59B69A), secondary=Color(0xFFD2B46D), background=Color(0xFF111714), surface=Color(0xFF18201C))
        else -> lightColorScheme(primary=Emerald, secondary=Gold, background=Ivory, surface=Color.White)
    }
    MaterialTheme(colorScheme=colors, content=content)
}
