package com.leantech.finder.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Navy = Color(0xFF1D2B53)
val NavyDeep = Color(0xFF131C38)
val Coral = Color(0xFFE07A5F)
val Jade = Color(0xFF3D8361)
val Sand = Color(0xFFF2CC8F)
val Cloud = Color(0xFFF7F8FA)

private val LightScheme = lightColorScheme(
    primary = Navy,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCE2F5),
    onPrimaryContainer = NavyDeep,
    secondary = Coral,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFBE3DC),
    onSecondaryContainer = Color(0xFF5A2A1E),
    tertiary = Jade,
    onTertiary = Color.White,
    background = Cloud,
    onBackground = Color(0xFF141A24),
    surface = Color.White,
    onSurface = Color(0xFF141A24),
    surfaceVariant = Color(0xFFE9ECF3),
    onSurfaceVariant = Color(0xFF454C5A),
    outline = Color(0xFFB6BCC9),
    error = Color(0xFFB3261E),
)

private val DarkScheme = darkColorScheme(
    primary = Color(0xFF9FB2E8),
    onPrimary = NavyDeep,
    primaryContainer = Color(0xFF2A3968),
    onPrimaryContainer = Color(0xFFDCE2F5),
    secondary = Color(0xFFF0A48D),
    onSecondary = Color(0xFF3A1A12),
    secondaryContainer = Color(0xFF57301F),
    onSecondaryContainer = Color(0xFFFBE3DC),
    tertiary = Color(0xFF89C7A8),
    onTertiary = Color(0xFF10291F),
    background = Color(0xFF10131A),
    onBackground = Color(0xFFE7EAF0),
    surface = Color(0xFF171C26),
    onSurface = Color(0xFFE7EAF0),
    surfaceVariant = Color(0xFF232A38),
    onSurfaceVariant = Color(0xFFC2C8D4),
    outline = Color(0xFF4A5262),
    error = Color(0xFFF2B8B5),
)

@Composable
fun ChinaSourcingTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkScheme else LightScheme,
        content = content,
    )
}
