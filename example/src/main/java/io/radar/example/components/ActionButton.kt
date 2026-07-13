package io.radar.example.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.radar.example.store.LocalLogStore
import io.radar.example.theme.RadarColors

enum class ActionButtonStyle { PRIMARY, SECONDARY, DESTRUCTIVE }

/**
 * A full-width action button that fires haptic feedback and **auto-logs its own tap** to
 * the console before running [action] — making the Tests tab self-documenting.
 */
@Composable
fun ActionButton(
    label: String,
    modifier: Modifier = Modifier,
    style: ActionButtonStyle = ActionButtonStyle.SECONDARY,
    action: () -> Unit,
) {
    val logStore = LocalLogStore.current
    val haptics = LocalHapticFeedback.current

    val background = when (style) {
        ActionButtonStyle.PRIMARY -> RadarColors.Twilight
        ActionButtonStyle.SECONDARY -> MaterialTheme.colorScheme.surfaceVariant
        ActionButtonStyle.DESTRUCTIVE -> RadarColors.Red.copy(alpha = 0.12f)
    }
    val foreground = when (style) {
        ActionButtonStyle.PRIMARY -> Color.White
        ActionButtonStyle.SECONDARY -> MaterialTheme.colorScheme.onSurface
        ActionButtonStyle.DESTRUCTIVE -> RadarColors.Red
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(background)
            .clickable {
                haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                logStore.writeAction(label)
                action()
            }
            .padding(vertical = 12.dp, horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = foreground,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
        )
    }
}
