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
 * A red strip across the top of every tab, shown only when the environment is misconfigured: no
 * key is set, or the key check says the key isn't valid on the selected environment.
 */
@Composable
fun EnvironmentBadge() {
    val settings = LocalSettingsStore.current
    val env = settings.environment
    val key = settings.resolvedPublishableKey
    if (key.isNotBlank() && !settings.keyCheck.mismatches(env)) return

    val problem = if (key.isBlank()) "no key set" else "${SettingsStore.keyPrefix(key)} · ${settings.keyCheck.summary()}"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            // As the Scaffold's topBar, the badge has to clear the status and action bars itself.
            .statusBarsPadding()
            .background(Color(0xFFC62828))
            .padding(horizontal = 12.dp, vertical = 4.dp),
    ) {
        Text(
            text = "⚠ ${env.label} · $problem",
            color = Color.White,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
