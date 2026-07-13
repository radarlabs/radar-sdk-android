package io.radar.example.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = RadarColors.Twilight,
    onPrimary = Color.White,
    primaryContainer = RadarColors.TwilightLight,
    onPrimaryContainer = Color.White,
    secondary = RadarColors.Blue,
    onSecondary = Color.White,
    background = RadarColors.LightBackground,
    onBackground = Color(0xFF1A1A1A),
    surface = RadarColors.LightSurface,
    onSurface = Color(0xFF1A1A1A),
    surfaceVariant = RadarColors.LightSurfaceVariant,
    onSurfaceVariant = Color(0xFF4A4A55),
    error = RadarColors.Red,
    onError = Color.White,
)

private val DarkColors = darkColorScheme(
    primary = RadarColors.TwilightLight,
    onPrimary = Color.White,
    primaryContainer = RadarColors.TwilightDark,
    onPrimaryContainer = Color.White,
    secondary = RadarColors.Blue,
    onSecondary = Color.White,
    background = RadarColors.DarkBackground,
    onBackground = Color(0xFFECECEC),
    surface = RadarColors.DarkSurface,
    onSurface = Color(0xFFECECEC),
    surfaceVariant = RadarColors.DarkSurfaceVariant,
    onSurfaceVariant = Color(0xFFB4B4C0),
    error = RadarColors.Red,
    onError = Color.White,
)

/** Material3 theme wrapping the whole app with Radar branding + light/dark support. */
@Composable
fun RadarExampleTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
