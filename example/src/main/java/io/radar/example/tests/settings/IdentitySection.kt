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
import io.radar.example.store.LocalSettingsStore

/** Identity editing. The publishable key lives in [EnvironmentSection]. */
@Composable
fun IdentitySection() {
    val settings = LocalSettingsStore.current

    var userId by remember(settings.userId) { mutableStateOf(settings.userId) }
    var description by remember(settings.description) { mutableStateOf(settings.description) }

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
    }
}
