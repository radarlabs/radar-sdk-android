package io.radar.example.console

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import io.radar.example.store.ConsoleEntry
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.US)

/** One row in the console timeline. Tap to expand the monospaced [ConsoleEntry.detail]. */
@Composable
fun ConsoleEntryRow(entry: ConsoleEntry, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    val hasDetail = !entry.detail.isNullOrBlank()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = hasDetail) { expanded = !expanded }
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Icon(
                imageVector = entry.kind.icon,
                contentDescription = entry.kind.name,
                tint = entry.kind.tint,
                modifier = Modifier.size(18.dp).padding(top = 2.dp),
            )
            Column(modifier = Modifier.padding(start = 10.dp)) {
                Text(
                    text = entry.summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = timeFormat.format(Date(entry.timestamp)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (hasDetail) {
            AnimatedVisibility(visible = expanded) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp, start = 28.dp),
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                ) {
                    Text(
                        text = entry.detail!!,
                        modifier = Modifier
                            .horizontalScroll(rememberScrollState())
                            .padding(10.dp),
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}
