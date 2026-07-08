package io.radar.example.map

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.radar.example.store.TripBuilderStore

/** Selected-destinations list + Start Trip CTA, shown while building a trip (no active trip). */
@Composable
fun BuilderTray(store: TripBuilderStore, modifier: Modifier = Modifier) {
    val destinations = store.selectedDestinations
    if (destinations.isEmpty()) return

    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Trip builder — ${destinations.size} destination${if (destinations.size == 1) "" else "s"}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            destinations.forEachIndexed { index, destination ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "${index + 1}. ${destination.title}",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(
                        onClick = { store.move(index, index - 1) },
                        enabled = index > 0,
                    ) {
                        Icon(Icons.Filled.ArrowUpward, contentDescription = "Move up", modifier = Modifier.size(18.dp))
                    }
                    IconButton(
                        onClick = { store.move(index, index + 1) },
                        enabled = index < destinations.lastIndex,
                    ) {
                        Icon(Icons.Filled.ArrowDownward, contentDescription = "Move down", modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = { store.removeAt(index) }) {
                        Icon(Icons.Filled.Close, contentDescription = "Remove", modifier = Modifier.size(18.dp))
                    }
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = { store.startTrip() },
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        if (destinations.size == 1) "Start trip" else "Start multi-leg trip (${destinations.size} legs)",
                    )
                }
                TextButton(onClick = { store.clearSelection() }) {
                    Text("Clear")
                }
            }
        }
    }
}
