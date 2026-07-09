package io.radar.example.tests.settings

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.radar.example.components.ControlRow
import io.radar.example.store.LocalSettingsStore

/**
 * Tracking status + configured tracking-options breakdown. The SDK's server-driven
 * configuration is `internal`, so this shows the public `Radar.getTrackingOptions()` JSON.
 */
@Composable
fun TrackingSection() {
    val settings = LocalSettingsStore.current

    LaunchedEffect(Unit) { settings.refresh() }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text("Tracking", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        ControlRow("Status", value = if (settings.isTracking) "On" else "Off")
        ControlRow("Source", value = if (settings.usingRemoteTrackingOptions) "Remote (server)" else "Local")

        Text(
            "Configured options",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 8.dp),
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(6.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
        ) {
            Text(
                text = settings.trackingOptionsJson,
                modifier = Modifier
                    .heightIn(max = 220.dp)
                    .verticalScroll(rememberScrollState())
                    .horizontalScroll(rememberScrollState())
                    .padding(10.dp),
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
            )
        }
        TextButton(onClick = { settings.refresh() }) { Text("Refresh") }
    }
}
