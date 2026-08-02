package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = PurplePrimary,
    onPrimary = Color.White,
    primaryContainer = LavenderCardBg,
    onPrimaryContainer = TextPrimary,
    secondary = PurpleSecondary,
    onSecondary = Color.White,
    background = BackgroundStart,
    onBackground = TextPrimary,
    surface = Color.White,
    onSurface = TextPrimary,
    surfaceVariant = GlassWhite,
    onSurfaceVariant = TextSecondary,
    outline = GlassBorder
)

private val DarkColorScheme = darkColorScheme(
    primary = PurpleSecondary,
    onPrimary = TextPrimary,
    primaryContainer = PurpleDark,
    onPrimaryContainer = Color.White,
    secondary = PurplePrimary,
    onSecondary = Color.White,
    background = Color(0xFF0F0C29),
    onBackground = Color.White,
    surface = Color(0xFF1E1B4B),
    onSurface = Color.White,
    surfaceVariant = Color(0x33FFFFFF),
    onSurfaceVariant = Color(0xFFD1D5DB),
    outline = Color(0x40A78BFA)
)

@Composable
fun MONEYTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

// Alias for backwards compatibility if needed
@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MONEYTheme(darkTheme = darkTheme, content = content)
}
