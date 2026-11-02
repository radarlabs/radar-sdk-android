package io.radar.example

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
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
import io.radar.example.console.ConsoleView
import io.radar.example.map.MapScreen
import io.radar.example.tests.TestsView

/**
 * App shell: a 3-tab bottom bar (Map / Debug / Tests). The map stays composed under the other
 * tabs (alpha 0, gestures off) so switching tabs doesn't reload it.
 */
@Composable
fun MainView() {
    val tabs = listOf("Map", "Debug", "Tests")
    var tabIndex by remember { mutableIntStateOf(2) }

    Scaffold(bottomBar = {
        PrimaryTabRow(selectedTabIndex = tabIndex, modifier = Modifier.navigationBarsPadding()) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = index == tabIndex,
                    onClick = { tabIndex = index },
                    text = { Text(title) },
                )
            }
        }
    }) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            MapScreen(active = tabIndex == 0)
            when (tabIndex) {
                1 -> ConsoleView()
                2 -> TestsView(onViewAllLogs = { tabIndex = 1 })
            }
        }
    }
}
