package io.radar.example.tests

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.radar.example.tests.panels.MessagingPanel
import io.radar.example.tests.panels.NotificationsPanel
import io.radar.example.tests.panels.SearchPanel
import io.radar.example.tests.panels.TrackingPanel
import io.radar.example.tests.panels.TripsPanel
import io.radar.example.tests.panels.VerifiedPanel
import io.radar.example.tests.settings.SettingsSheet

/** The Tests tab: a gear (settings), a recent-activity card, and 6 collapsible test panels. */
@Composable
fun TestsView(onViewAllLogs: () -> Unit, modifier: Modifier = Modifier) {
    var showSettings by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text(
                "Tests",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = { showSettings = true }) {
                Icon(Icons.Filled.Settings, contentDescription = "Settings")
            }
        }

        RecentActivityCard(onViewAll = onViewAllLogs)

        TrackingPanel()
        TripsPanel()
        VerifiedPanel()
        SearchPanel()
        NotificationsPanel()
        MessagingPanel()
    }

    if (showSettings) {
        SettingsSheet(onDismiss = { showSettings = false })
    }
}
