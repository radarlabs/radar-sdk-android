package io.radar.example.theme

import androidx.compose.ui.graphics.Color

/**
 * Radar-branded color palette for the example app.
 *
 * The brand accent is Radar "twilight" (#2A1688). Semantic colors mirror the iOS
 * example's console/overlay color language and are reused by [io.radar.example.store.ConsoleEntry]
 * and the map overlay sources.
 */
object RadarColors {
    // Brand
    val Twilight = Color(0xFF2A1688)
    val TwilightLight = Color(0xFF5B47C7)
    val TwilightDark = Color(0xFF1B0E5C)

    // Semantic status / console kinds
    val Blue = Color(0xFF2563EB) // action / active
    val Green = Color(0xFF16A34A) // result / success / synced
    val Purple = Color(0xFF7C3AED) // event
    val Teal = Color(0xFF0D9488) // location / places
    val Gray = Color(0xFF6B7280) // log / completed
    val Red = Color(0xFFDC2626) // error / destructive / cancel
    val Orange = Color(0xFFEA580C) // current trip leg / active geofence

    // Surfaces
    val LightBackground = Color(0xFFF7F7FB)
    val LightSurface = Color(0xFFFFFFFF)
    val LightSurfaceVariant = Color(0xFFECECF3)
    val DarkBackground = Color(0xFF121016)
    val DarkSurface = Color(0xFF1C1A22)
    val DarkSurfaceVariant = Color(0xFF2A2731)
}
