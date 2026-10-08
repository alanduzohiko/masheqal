
package com.masheqal.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val Emerald = Color(0xFF12614E)
private val EmeraldDark = Color(0xFF083F34)
private val Gold = Color(0xFFB18A45)
private val Ivory = Color(0xFFF8F5ED)
private val Ink = Color(0xFF14201D)
private val DarkSurface = Color(0xFF17231F)

private val MasheqalTypography = Typography().copy(
    headlineLarge = Typography().headlineLarge.copy(
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.4).sp
    ),
    headlineMedium = Typography().headlineMedium.copy(
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.2).sp
    ),
    headlineSmall = Typography().headlineSmall.copy(fontWeight = FontWeight.Bold),
    titleLarge = Typography().titleLarge.copy(fontWeight = FontWeight.Bold),
    titleMedium = Typography().titleMedium.copy(fontWeight = FontWeight.SemiBold),
    labelLarge = Typography().labelLarge.copy(fontWeight = FontWeight.SemiBold)
)

@Composable
fun MasheqalTheme(theme: String, content: @Composable () -> Unit) {
    val systemDark = isSystemInDarkTheme()
    val dark = when (theme) {
        "dark", "amoled" -> true
        "light", "high_contrast" -> false
        else -> systemDark
    }

    val colors = when (theme) {
        "amoled" -> darkColorScheme(
            primary = Color(0xFF63D1B0),
            onPrimary = Color(0xFF00372D),
            secondary = Color(0xFFE1C477),
            onSecondary = Color(0xFF392B00),
            background = Color.Black,
            onBackground = Color.White,
            surface = Color.Black,
            onSurface = Color.White,
            surfaceVariant = Color(0xFF101714),
            onSurfaceVariant = Color(0xFFC4D5CE)
        )
        "dark" -> darkColorScheme(
            primary = Color(0xFF63D1B0),
            onPrimary = Color(0xFF00372D),
            secondary = Color(0xFFE1C477),
            onSecondary = Color(0xFF392B00),
            background = Color(0xFF0E1512),
            surface = DarkSurface,
            surfaceVariant = Color(0xFF202E29),
            onSurfaceVariant = Color(0xFFC4D5CE)
        )
        "high_contrast" -> lightColorScheme(
            primary = Color(0xFF004C3B),
            onPrimary = Color.White,
            secondary = Color(0xFF704E00),
            background = Color.White,
            surface = Color.White,
            surfaceVariant = Color(0xFFF0F0F0),
            onSurface = Color.Black,
            onBackground = Color.Black,
            onSurfaceVariant = Color(0xFF202020)
        )
        else -> lightColorScheme(
            primary = Emerald,
            onPrimary = Color.White,
            primaryContainer = Color(0xFFD2EFE5),
            onPrimaryContainer = EmeraldDark,
            secondary = Gold,
            onSecondary = Color(0xFF2B2100),
            secondaryContainer = Color(0xFFF2E5BF),
            onSecondaryContainer = Color(0xFF3B2D06),
            background = Ivory,
            onBackground = Ink,
            surface = Color.White,
            onSurface = Ink,
            surfaceVariant = Color(0xFFEEF2EE),
            onSurfaceVariant = Color(0xFF53615C)
        )
    }

    MaterialTheme(
        colorScheme = colors,
        typography = MasheqalTypography,
        content = content
    )
}
