package io.radar.example.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.radar.example.store.LocalSettingsStore
import io.radar.example.store.SettingsStore

/**
 * A strip across the top of every tab showing the environment, the key and whether the key is
 * valid there. Turns red when the key check says the key belongs somewhere else.
 */
@Composable
fun EnvironmentBadge() {
    val settings = LocalSettingsStore.current
    val env = settings.environment
    val key = settings.resolvedPublishableKey
    val mismatch = settings.keyCheck.mismatches(env) || key.isBlank()

    val keyText = if (key.isBlank()) "no key set" else SettingsStore.keyPrefix(key)
    val checkText = if (key.isBlank()) "" else " · ${settings.keyCheck.summary()}"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            // As the Scaffold's topBar, the badge has to clear the status and action bars itself.
            .statusBarsPadding()
            .background(if (mismatch) Color(0xFFC62828) else env.color)
            .padding(horizontal = 12.dp, vertical = 4.dp),
    ) {
        Text(
            text = (if (mismatch) "⚠ " else "") + "${env.label} · $keyText$checkText",
            color = Color.White,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
