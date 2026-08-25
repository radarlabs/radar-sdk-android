package io.radar.example.console

import io.radar.example.store.ConsoleEntry
import io.radar.example.store.ConsoleKind
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal enum class ConsoleFilter(val label: String, val kinds: Set<ConsoleKind>?) {
    ALL("All", null),
    ACTIONS("Actions", setOf(ConsoleKind.ACTION, ConsoleKind.RESULT)),
    EVENTS("Events", setOf(ConsoleKind.EVENT)),
    LOCATIONS("Locations", setOf(ConsoleKind.LOCATION)),
    LOGS("Logs", setOf(ConsoleKind.LOG)),
    ERRORS("Errors", setOf(ConsoleKind.ERROR)),
    ;

    fun matches(kind: ConsoleKind): Boolean = kinds?.contains(kind) ?: true
}

internal fun filterConsoleEntries(
    entries: List<ConsoleEntry>,
    filter: ConsoleFilter,
    query: String,
): List<ConsoleEntry> {
    val normalizedQuery = query.trim()
    return entries.filter { entry ->
        filter.matches(entry.kind) &&
            (normalizedQuery.isEmpty() ||
                entry.summary.contains(normalizedQuery, ignoreCase = true) ||
                entry.detail?.contains(normalizedQuery, ignoreCase = true) == true)
    }
}

private val exportTimeFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)

internal fun exportConsoleText(entries: List<ConsoleEntry>): String =
    entries.joinToString("\n") { entry ->
        val time = exportTimeFormat.format(Date(entry.timestamp))
        val head = "[$time] ${entry.kind.name} — ${entry.summary}"
        if (entry.detail.isNullOrBlank()) head else "$head\n${entry.detail}"
    }
