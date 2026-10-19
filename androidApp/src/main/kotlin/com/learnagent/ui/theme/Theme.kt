package com.learnagent.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF7A4FD0),
    onPrimary = White,
    secondary = Color(0xFF9A7B2E),
    onSecondary = White,
    background = Color(0xFFF6F2FA),
    surface = Color(0xFFFDFBFF),
    onBackground = Color(0xFF241C30),
    onSurface = Color(0xFF241C30),
    surfaceVariant = Color(0xFFEDE6F5),
    onSurfaceVariant = Color(0xFF6B6178),
    outline = Color(0xFFD8CFE4),
    error = Coral,
    onError = White
)

private val DarkColorScheme = darkColorScheme(
    primary = Phantom,
    onPrimary = Void,
    primaryContainer = Color(0xFF2A1B45),
    onPrimaryContainer = Color(0xFFE8DDFF),
    secondary = Aureate,
    onSecondary = Void,
    secondaryContainer = Color(0xFF2E2410),
    onSecondaryContainer = Color(0xFFF2E3B8),
    tertiary = MoonSilver,
    onTertiary = Void,
    background = Void,
    onBackground = TextHigh,
    surface = Veil,
    onSurface = TextHigh,
    surfaceVariant = Glimmer,
    onSurfaceVariant = TextMid,
    outline = Mist,
    outlineVariant = Mist,
    error = Coral,
    onError = Void
)

@Composable
fun LearnAgentTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}
