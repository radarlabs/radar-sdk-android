package io.radar.example

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import io.radar.sdk.Radar

@Composable
fun MainView() {
    val myRadarReceiver = MyRadarReceiver().apply {
        Radar.setReceiver(this)
    }
    val tabs = listOf("Map", "Debug", "Tests")
    var tabIndex by remember { mutableIntStateOf(0) }

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
            when (tabIndex) {
                0 -> Column {

                }
                1 -> DebugView(myRadarReceiver)
                2 -> TestView()
            }
        }
    }
}