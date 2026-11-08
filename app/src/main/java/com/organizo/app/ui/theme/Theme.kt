package com.organizo.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Navy = Color(0xFF0A2342)

private val LightColors = lightColorScheme(
    primary = Navy,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCE6F7),
    onPrimaryContainer = Navy,
    secondary = Color(0xFF2C5282),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE3EAF6),
    onSecondaryContainer = Navy,
    background = Color(0xFFF5F7FB),
    onBackground = Color(0xFF0F172A),
    surface = Color.White,
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFE6EBF4),
    onSurfaceVariant = Color(0xFF44506A),
    outline = Color(0xFF8A94A8),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF0F3F9),
    surfaceContainer = Color(0xFFEBEFF7),
    surfaceContainerHigh = Color(0xFFE6EBF4),
    surfaceContainerHighest = Color(0xFFE1E7F2)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF9DB9E8),
    onPrimary = Navy,
    primaryContainer = Color(0xFF1B3A6B),
    onPrimaryContainer = Color(0xFFDCE6F7),
    secondary = Color(0xFF9DB9E8),
    onSecondary = Navy,
    secondaryContainer = Color(0xFF1B3A6B),
    onSecondaryContainer = Color(0xFFDCE6F7),
    background = Color(0xFF0B1220),
    onBackground = Color(0xFFE6EAF2),
    surface = Color(0xFF111A2B),
    onSurface = Color(0xFFE6EAF2),
    surfaceVariant = Color(0xFF1E2A44),
    onSurfaceVariant = Color(0xFFB3BDD1),
    outline = Color(0xFF6B7690),
    surfaceContainerLowest = Color(0xFF0A101C),
    surfaceContainerLow = Color(0xFF0F1828),
    surfaceContainer = Color(0xFF131D30),
    surfaceContainerHigh = Color(0xFF1A2540),
    surfaceContainerHighest = Color(0xFF1E2A44)
)

@Composable
fun OrganizoTheme(dark: Boolean, content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (dark) DarkColors else LightColors, content = content)
}
