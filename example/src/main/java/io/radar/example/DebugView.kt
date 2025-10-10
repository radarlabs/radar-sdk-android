package io.radar.example

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight

@Composable
fun DebugView(receiver: MyRadarReceiver) {
    val eventScrollState = rememberScrollState()
    val logScrollState = rememberScrollState()

    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceEvenly) {
        Column(modifier = Modifier.weight(1f)) {
            Row {
                Text("Logs:", style = TextStyle(fontWeight = FontWeight.Bold))
                Button(onClick = { receiver.logs.clear() }) { Text("Clear") }
            }
            Column(modifier = Modifier.verticalScroll(logScrollState)) {
                receiver.logs.map {
                    HorizontalDivider()
                    Text(it)
                }
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Row {
                Text("Events:", style = TextStyle(fontWeight = FontWeight.Bold))
                Button(onClick = { receiver.events.clear() }) { Text("Clear") }
            }
            Column(modifier = Modifier.verticalScroll(eventScrollState)) {
                receiver.events.map {
                    HorizontalDivider()
                    it.geofence?.let { geofence ->
                        Text("${it.type} ${geofence.externalId}")
                    }
                }
            }
        }
    }
}