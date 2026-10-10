
package com.masheqal.app.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.masheqal.app.R

val QuranFont = FontFamily(Font(R.font.amiri_quran))

@Composable
fun SectionTitle(
    title: String,
    action: String? = null,
    onAction: (() -> Unit)? = null
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        if (action != null && onAction != null) {
            TextButton(
                onClick = onAction,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(action)
            }
        }
    }
}

@Composable
fun IconBadge(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    emphasized: Boolean = false
) {
    Surface(
        modifier = modifier.size(46.dp),
        shape = RoundedCornerShape(15.dp),
        color = if (emphasized) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
        }
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.padding(11.dp),
            tint = if (emphasized) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.primary
            }
        )
    }
}

@Composable
fun FeatureCard(
    title: String,
    subtitle: String? = null,
    icon: ImageVector = Icons.Default.ChevronRight,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    emphasized: Boolean = false,
    onClick: () -> Unit
) {
    val container = if (emphasized) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surface
    }
    val content = if (emphasized) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(
                1.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.10f),
                RoundedCornerShape(22.dp)
            )
            .clickable(enabled = enabled) { onClick() },
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = container),
        elevation = CardDefaults.cardElevation(defaultElevation = if (emphasized) 1.dp else 0.dp)
    ) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconBadge(icon, emphasized = emphasized)
            Spacer(Modifier.width(13.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = content, fontWeight = FontWeight.SemiBold)
                if (!subtitle.isNullOrBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (emphasized) {
                            content.copy(alpha = 0.75f)
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = content.copy(alpha = 0.55f)
            )
        }
    }
}

@Composable
fun StatPill(text: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.88f)
    ) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium
        )
    }
}

@Composable
fun QuranText(
    text: String,
    modifier: Modifier = Modifier,
    size: Float = 28f
) {
    Text(
        text = text,
        modifier = modifier.fillMaxWidth(),
        fontSize = size.sp,
        lineHeight = (size * 1.78f).sp,
        fontFamily = QuranFont,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurface
    )
}

private const val TAJWEED_CLOSE = "</tajweed>"
private val tajweedOpenPattern = Regex("""<tajweed\s+class=["']([^"']+)["']\s*>""")

private fun tajweedColor(rule: String, dark: Boolean): Color? = when (rule.lowercase()) {
    "madda_normal", "madda_permissible", "madda_necessary", "madda_obligatory" ->
        if (dark) Color(0xFF82B1FF) else Color(0xFF0D47A1)
    "ikhafa", "ikhafa_shafawi", "ghunnah" ->
        if (dark) Color(0xFF81C784) else Color(0xFF1B5E20)
    "idgham_ghunnah", "idgham_wo_ghunnah", "idgham_shafawi" ->
        if (dark) Color(0xFF80CBC4) else Color(0xFF00695C)
    "qalqalah" ->
        if (dark) Color(0xFFFFAB91) else Color(0xFFBF360C)
    "iqlab" ->
        if (dark) Color(0xFFCE93D8) else Color(0xFF6A1B9A)
    "ham_wasl", "laam_shamsiyah" ->
        if (dark) Color(0xFFFFD180) else Color(0xFF8D4C00)
    "silent" ->
        if (dark) Color(0xFFB0BEC5) else Color(0xFF546E7A)
    else -> null
}

private fun parseTajweedText(raw: String, dark: Boolean): AnnotatedString = buildAnnotatedString {
    var cursor = 0
    while (cursor < raw.length) {
        val opening = tajweedOpenPattern.find(raw, cursor)
        if (opening == null) {
            append(raw.substring(cursor).replace(TAJWEED_CLOSE, ""))
            break
        }
        append(raw.substring(cursor, opening.range.first))
        val closingIndex = raw.indexOf(TAJWEED_CLOSE, opening.range.last + 1)
        if (closingIndex < 0) {
            append(raw.substring(opening.range.first).replace(tajweedOpenPattern, "").replace(TAJWEED_CLOSE, ""))
            break
        }
        val segment = raw.substring(opening.range.last + 1, closingIndex)
        val color = tajweedColor(opening.groupValues[1], dark)
        if (color == null) append(segment) else withStyle(SpanStyle(color = color)) { append(segment) }
        cursor = closingIndex + TAJWEED_CLOSE.length
    }
}

@Composable
fun QuranTajweedText(
    taggedText: String,
    modifier: Modifier = Modifier,
    size: Float = 28f
) {
    val dark = isSystemInDarkTheme()
    val annotated = remember(taggedText, dark) { parseTajweedText(taggedText, dark) }
    Text(
        text = annotated,
        modifier = modifier.fillMaxWidth(),
        fontSize = size.sp,
        lineHeight = (size * 1.78f).sp,
        fontFamily = QuranFont,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurface
    )
}

@Composable
fun EmptyState(
    title: String,
    details: String,
    icon: ImageVector = Icons.Default.Inventory2,
    onBack: (() -> Unit)? = null
) {
    Box(
        Modifier.fillMaxSize().padding(28.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
            ) {
                Icon(
                    icon,
                    null,
                    Modifier.padding(18.dp).size(38.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(Modifier.height(18.dp))
            Text(
                title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Text(
                details,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (onBack != null) {
                Spacer(Modifier.height(18.dp))
                OutlinedButton(onClick = onBack) {
                    Text(stringResource(R.string.done))
                }
            }
        }
    }
}

@Composable
fun MissingContentScreen(title: String, details: String, onBack: () -> Unit) {
    EmptyState(title = title, details = details, onBack = onBack)
}
