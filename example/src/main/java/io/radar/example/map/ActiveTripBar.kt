package io.radar.example.map

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.radar.example.store.TripBuilderStore
import io.radar.example.theme.RadarColors
import io.radar.sdk.model.RadarTripLeg

/** Active-trip header with leg-advance/reorder controls and complete/cancel actions. */
@Composable
fun ActiveTripBar(store: TripBuilderStore, modifier: Modifier = Modifier) {
    val trip = store.activeTrip ?: return
    var expanded by remember { mutableStateOf(false) }
    val legs = trip.legs
    val hasLegs = !legs.isNullOrEmpty()

    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Trip ${trip.externalId}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "status: ${trip.status.name.lowercase()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (hasLegs) {
                    IconButton(onClick = { expanded = !expanded }) {
                        Icon(
                            Icons.Filled.ExpandMore,
                            contentDescription = "Legs",
                            modifier = Modifier.rotate(if (expanded) 180f else 0f),
                        )
                    }
                }
            }

            if (hasLegs) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedButton(onClick = { store.advanceCurrentLeg(RadarTripLeg.RadarTripLegStatus.APPROACHING) }) {
                        Text("→ approaching")
                    }
                    OutlinedButton(onClick = { store.advanceCurrentLeg(RadarTripLeg.RadarTripLegStatus.ARRIVED) }) {
                        Text("→ arrived")
                    }
                    OutlinedButton(onClick = { store.advanceCurrentLeg(RadarTripLeg.RadarTripLegStatus.COMPLETED) }) {
                        Text("→ completed")
                    }
                }

                AnimatedVisibility(visible = expanded) {
                    Column(modifier = Modifier.padding(top = 8.dp)) {
                        legs!!.forEachIndexed { index, leg ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = "${index + 1}. ${legLabel(leg)} · ${RadarTripLeg.stringForStatus(leg.status)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.weight(1f),
                                )
                                val pending = leg.status == RadarTripLeg.RadarTripLegStatus.PENDING
                                if (pending && leg._id != null) {
                                    IconButton(onClick = { store.moveLeg(leg._id!!, TripBuilderStore.LegMoveDirection.UP) }) {
                                        Icon(Icons.Filled.ArrowUpward, "Move leg up", modifier = Modifier.size(16.dp))
                                    }
                                    IconButton(onClick = { store.moveLeg(leg._id!!, TripBuilderStore.LegMoveDirection.DOWN) }) {
                                        Icon(Icons.Filled.ArrowDownward, "Move leg down", modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(onClick = { store.completeTrip() }, modifier = Modifier.weight(1f)) {
                    Text("Complete")
                }
                Button(
                    onClick = { store.cancelTrip() },
                    colors = ButtonDefaults.buttonColors(containerColor = RadarColors.Red),
                ) {
                    Text("Cancel")
                }
            }
        }
    }
}

private fun legLabel(leg: RadarTripLeg): String =
    leg.destinationGeofenceExternalId
        ?: leg.destinationGeofenceId
        ?: leg.address
        ?: leg.coordinates?.let { "%.4f, %.4f".format(it.latitude, it.longitude) }
        ?: "leg"
