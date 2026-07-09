package io.radar.example.console

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Warning
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import io.radar.example.store.ConsoleKind
import io.radar.example.theme.RadarColors

/** Icon per console kind. */
val ConsoleKind.icon: ImageVector
    get() = when (this) {
        ConsoleKind.ACTION -> Icons.Filled.PlayArrow
        ConsoleKind.RESULT -> Icons.Filled.CheckCircle
        ConsoleKind.EVENT -> Icons.Filled.Bolt
        ConsoleKind.LOCATION -> Icons.Filled.LocationOn
        ConsoleKind.LOG -> Icons.Filled.Notes
        ConsoleKind.ERROR -> Icons.Filled.Warning
    }

/** Tint per console kind. */
val ConsoleKind.tint: Color
    get() = when (this) {
        ConsoleKind.ACTION -> RadarColors.Blue
        ConsoleKind.RESULT -> RadarColors.Green
        ConsoleKind.EVENT -> RadarColors.Purple
        ConsoleKind.LOCATION -> RadarColors.Teal
        ConsoleKind.LOG -> RadarColors.Gray
        ConsoleKind.ERROR -> RadarColors.Red
    }
