package io.radar.example.tests.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AssistChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.radar.example.store.LocalLogStore
import io.radar.example.store.LocalSettingsStore
import io.radar.example.store.TestPreset

/** Preset chips that apply identity + tracking in one tap. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PresetSection() {
    val settings = LocalSettingsStore.current
    val log = LocalLogStore.current

    Column(modifier = Modifier.fillMaxWidth()) {
        Text("Presets", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TestPreset.all.forEach { preset ->
                AssistChip(
                    onClick = { settings.apply(preset, log) },
                    label = { Text(preset.name) },
                )
            }
        }
    }
}
