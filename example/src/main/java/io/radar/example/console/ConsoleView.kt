package io.radar.example.console

import android.content.Intent
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.radar.example.store.ConsoleEntry
import io.radar.example.store.ConsoleKind
import io.radar.example.store.LocalLogStore
import android.content.ClipData
import android.content.ClipboardManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class ConsoleFilter(val label: String, val kinds: Set<ConsoleKind>?) {
    ALL("All", null),
    ACTIONS("Actions", setOf(ConsoleKind.ACTION, ConsoleKind.RESULT)),
    EVENTS("Events", setOf(ConsoleKind.EVENT)),
    LOCATIONS("Locations", setOf(ConsoleKind.LOCATION)),
    LOGS("Logs", setOf(ConsoleKind.LOG)),
    ERRORS("Errors", setOf(ConsoleKind.ERROR)),
    ;

    fun matches(kind: ConsoleKind): Boolean = kinds?.contains(kind) ?: true
}

private val exportTimeFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)

/** The Debug tab: a unified, filterable console timeline over [LogStore.entries]. */
@Composable
fun ConsoleView(modifier: Modifier = Modifier) {
    val logStore = LocalLogStore.current
    val context = LocalContext.current
    var filter by remember { mutableStateOf(ConsoleFilter.ALL) }

    val entries = logStore.entries
    val filtered = entries.filter { filter.matches(it.kind) }

    Column(modifier = modifier.fillMaxSize()) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Console",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "  ${entries.size}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = {
                val manager = context.getSystemService(ClipboardManager::class.java)
                manager?.setPrimaryClip(ClipData.newPlainText("Radar console", exportText(entries)))
            }) {
                Icon(Icons.Filled.ContentCopy, contentDescription = "Copy")
            }
            IconButton(onClick = {
                val send = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, exportText(entries))
                }
                context.startActivity(Intent.createChooser(send, "Share console"))
            }) {
                Icon(Icons.Filled.Share, contentDescription = "Share")
            }
            IconButton(onClick = { logStore.clear() }) {
                Icon(Icons.Filled.DeleteOutline, contentDescription = "Clear")
            }
        }

        // Filter chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ConsoleFilter.entries.forEach { f ->
                FilterChip(
                    selected = filter == f,
                    onClick = { filter = f },
                    label = { Text(f.label) },
                )
            }
        }

        HorizontalDivider()

        if (filtered.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Filled.Inbox,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "Nothing yet",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(filtered.asReversed(), key = { it.id }) { entry ->
                    ConsoleEntryRow(entry)
                    HorizontalDivider()
                }
            }
        }
    }
}

private fun exportText(entries: List<ConsoleEntry>): String =
    entries.joinToString("\n") { entry ->
        val time = exportTimeFormat.format(Date(entry.timestamp))
        val head = "[$time] ${entry.kind.name} — ${entry.summary}"
        if (entry.detail.isNullOrBlank()) head else "$head\n${entry.detail}"
    }
