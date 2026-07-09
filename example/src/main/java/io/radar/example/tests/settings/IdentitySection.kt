package io.radar.example.tests.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.radar.example.components.FieldEditor
import io.radar.example.store.LocalLogStore
import io.radar.example.store.LocalSettingsStore

/** Identity + publishable-key editing. */
@Composable
fun IdentitySection() {
    val settings = LocalSettingsStore.current
    val log = LocalLogStore.current

    var userId by remember(settings.userId) { mutableStateOf(settings.userId) }
    var description by remember(settings.description) { mutableStateOf(settings.description) }
    var key by remember(settings.publishableKeyOverride) { mutableStateOf(settings.publishableKeyOverride) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text("Identity", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)

        FieldEditor("User ID", userId, { userId = it })
        TextButton(onClick = { settings.applyUserId(userId) }) { Text("Apply user ID") }

        FieldEditor("Description", description, { description = it })
        TextButton(onClick = { settings.applyDescription(description) }) { Text("Apply description") }

        Text(
            "Metadata: ${settings.metadataText}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )

        Text(
            "Publishable key override",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 12.dp),
        )
        FieldEditor("Publishable key", key, { key = it }, placeholder = "prj_test_pk_…")
        TextButton(onClick = {
            settings.savePublishableKeyOverride(key)
            log.writeResult("publishable key override saved", "Restart the app to apply.")
        }) { Text("Save key (restart to apply)") }
    }
}
