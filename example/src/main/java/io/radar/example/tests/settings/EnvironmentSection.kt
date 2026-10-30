package io.radar.example.tests.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.radar.example.MainActivity
import io.radar.example.components.FieldEditor
import io.radar.example.store.LocalSettingsStore
import io.radar.example.store.RadarEnvironment

/**
 * Picks the Radar environment and its publishable key. Changes are saved and applied on relaunch,
 * so the SDK never runs with a host from one environment and a key from another.
 */
@Composable
fun EnvironmentSection() {
    val settings = LocalSettingsStore.current
    val context = LocalContext.current

    var selected by remember { mutableStateOf(settings.environment) }
    var key by remember(selected) { mutableStateOf(settings.publishableKey(selected)) }
    var localUrl by remember { mutableStateOf(settings.savedLocalUrl()) }

    val missingLocalUrl = selected == RadarEnvironment.LOCAL && localUrl.isBlank()
    val changed = selected != settings.environment ||
        key.trim() != settings.publishableKey(selected) ||
        (selected == RadarEnvironment.LOCAL && localUrl.trim() != settings.localUrl)

    Column(modifier = Modifier.fillMaxWidth()) {
        Text("Environment", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        Text(
            "Running: ${settings.environmentSummary()}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
        )

        val environments = RadarEnvironment.entries
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            environments.forEachIndexed { index, env ->
                SegmentedButton(
                    selected = selected == env,
                    onClick = { selected = env },
                    shape = SegmentedButtonDefaults.itemShape(index, environments.size),
                ) { Text(env.label) }
            }
        }

        if (selected == RadarEnvironment.LOCAL) {
            FieldEditor(
                "Local server URL",
                localUrl,
                { localUrl = it },
                placeholder = "http://192.168.X.X:8081 (10.0.2.2 on the emulator)",
            )
        } else {
            Text(
                "Host: ${selected.host("")}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
        }

        FieldEditor("${selected.label} publishable key", key, { key = it }, placeholder = "prj_test_pk_…")

        Button(
            onClick = {
                settings.saveEnvironment(selected, key, localUrl)
                MainActivity.restart(context)
            },
            enabled = changed && !missingLocalUrl,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        ) { Text(if (missingLocalUrl) "Set a local server URL" else "Apply & restart") }
    }
}
