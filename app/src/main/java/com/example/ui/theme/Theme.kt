package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = GoldPrimary,
    onPrimary = Color(0xFF0A0F16),
    primaryContainer = GoldDark,
    onPrimaryContainer = Color(0xFF05070A),
    secondary = AccentCyan,
    onSecondary = Color(0xFF05070A),
    background = BgDark,
    onBackground = TextPrimary,
    surface = PanelDark,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceDark,
    onSurfaceVariant = TextDim,
    outline = LineBorder,
    outlineVariant = LineLight
)

@Composable
fun RefReelsTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
