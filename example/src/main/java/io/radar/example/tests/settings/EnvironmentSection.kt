package io.radar.example.tests.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
 * NOTE(jasonl): Edit the environment, key, and hosts together. Changes apply after restart.
 */
@Composable
fun EnvironmentSection() {
    val settings = LocalSettingsStore.current
    val context = LocalContext.current

    var selected by remember { mutableStateOf(settings.environment) }
    var key by remember(selected) { mutableStateOf(settings.publishableKey(selected)) }
    var host by remember(selected) { mutableStateOf(settings.hostOverride(selected)) }
    var verifiedHost by remember(selected) { mutableStateOf(settings.verifiedHostOverride(selected)) }

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

        FieldEditor("${selected.label} publishable key", key, { key = it }, placeholder = "prj_test_pk_…")
        HostOverrideEditor(
            "API host override",
            host,
            { host = it },
            selected.host(settings.localUrl),
            "https://api.radar-staging.com",
        )
        HostOverrideEditor(
            "Verified host override",
            verifiedHost,
            { verifiedHost = it },
            selected.verifiedHost(settings.localUrl),
            "https://api-verified.radar-staging.com",
        )
        Text(
            "Empty fields use defaults. Custom hosts can use ngrok, " +
                "api-server-dev-<name>.use1.radar-staging.com, or " +
                "api-server-pr-<number>.use1.radar-staging.com. Use http:// for local HTTP servers.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Button(
            onClick = {
                settings.saveEnvironment(selected, key, host, verifiedHost)
                MainActivity.restart(context)
            },
            enabled = selected != RadarEnvironment.LOCAL || settings.localUrl.isNotBlank() || (host.isNotBlank() && verifiedHost.isNotBlank()),
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        ) { Text("Apply & restart") }
    }
}

@Composable
private fun HostOverrideEditor(label: String, value: String, onValueChange: (String) -> Unit, defaultHost: String, stagingHost: String) {
    var expanded by remember { mutableStateOf(false) }

    FieldEditor(label, value, onValueChange, placeholder = defaultHost.ifBlank { "http://10.0.2.2:8081" })
    Box {
        TextButton(onClick = { expanded = true }) { Text("Host options") }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            listOf("Default" to "", "Staging" to stagingHost, "Local emulator" to "http://10.0.2.2:8081").forEach { (name, host) ->
                DropdownMenuItem(
                    text = { Text(name) },
                    onClick = {
                        onValueChange(host)
                        expanded = false
                    },
                )
            }
        }
    }
}
