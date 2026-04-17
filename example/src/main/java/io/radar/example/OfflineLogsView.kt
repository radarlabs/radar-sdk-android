package io.radar.example

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Dedicated QA tab showing only the offline-events / offline-RTO trace feed
 * populated by [MyRadarReceiver.offlineLogs]. Android counterpart to the iOS
 * `DemoLogView`. Newest entries render at the top so QA doesn't have to scroll
 * during a walk.
 */
@Composable
fun OfflineLogsView(receiver: MyRadarReceiver) {
    val scrollState = rememberScrollState()
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize().padding(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Offline trace (${receiver.offlineLogs.size}):",
                style = TextStyle(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(vertical = 8.dp),
            )
            Button(onClick = { receiver.offlineLogs.clear() }) { Text("Clear") }
            Button(onClick = { copyToClipboard(context, receiver.offlineLogs.joinToString("\n")) }) {
                Text("Copy")
            }
        }

        Column(modifier = Modifier.fillMaxSize().verticalScroll(scrollState)) {
            // Reverse for newest-first ordering.
            receiver.offlineLogs.asReversed().forEach { line ->
                HorizontalDivider()
                Text(
                    text = line,
                    style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp),
                    modifier = Modifier.padding(vertical = 4.dp, horizontal = 2.dp),
                )
            }
        }
    }
}

private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
    clipboard.setPrimaryClip(ClipData.newPlainText("Radar offline log", text))
}
