package io.radar.example

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import io.radar.sdk.Radar
import org.json.JSONObject
import java.io.File

@Composable
fun MainView() {
    val myRadarReceiver = MyRadarReceiver().apply {
        Radar.setReceiver(this)
    }
    val tabs = listOf("Map", "Logs", "Custom", "Tests")
    var tabIndex by remember { mutableIntStateOf(0) }

    val file = File(File(LocalContext.current.filesDir, "RadarSDK"), "offlineData.json")
    var fileData by remember { mutableStateOf("") }
    val fileScrollState = rememberScrollState()

    Scaffold(bottomBar = {
        PrimaryTabRow(selectedTabIndex = tabIndex) {
            tabs.forEachIndexed { idx, tab ->
                Tab(
                    selected = (idx == tabIndex),
                    onClick = { tabIndex = idx },
                    text = { Text(tab) }
                )
            }
        }
    }) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            MapView(disabled = tabIndex != 0)

            when (tabIndex) {
                1 -> LogsView(myRadarReceiver)
                2 -> Column {
                    Row {
                        Text("Logs:", style = TextStyle(fontWeight = FontWeight.Bold))
                        Button(onClick = {
                            try {
                                if (!file.exists()) throw Exception()
                                val json = JSONObject(file.readBytes().toString(Charsets.UTF_8))
                                fileData = json.toString(2)
                            } catch (e: Exception) {
                                fileData = "Error: $e"
                            }
                        }) { Text("Refresh") }
                    }
                    Text(fileData, Modifier.fillMaxWidth().verticalScroll(fileScrollState))
                }
                3 -> TestView()
            }
        }
    }
}