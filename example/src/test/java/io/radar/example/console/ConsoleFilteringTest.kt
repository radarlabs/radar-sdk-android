package io.radar.example.console

import io.radar.example.store.ConsoleEntry
import io.radar.example.store.ConsoleKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ConsoleFilteringTest {
    private val entries = listOf(
        ConsoleEntry(ConsoleKind.ACTION, "Start trip"),
        ConsoleEntry(ConsoleKind.EVENT, "Geofence entered", "Venue detail payload"),
        ConsoleEntry(ConsoleKind.LOG, "Network request"),
        ConsoleEntry(ConsoleKind.ERROR, "Request failed"),
    )

    @Test
    fun filtersByKindAndReportsVisibleCount() {
        val filtered = filterConsoleEntries(entries, ConsoleFilter.ACTIONS, "")

        assertEquals(1, filtered.size)
        assertEquals(ConsoleKind.ACTION, filtered.single().kind)
    }

    @Test
    fun trimsWhitespaceAndMatchesSummaryWithoutCase() {
        val filtered = filterConsoleEntries(entries, ConsoleFilter.ALL, "  START TRIP  ")

        assertEquals(listOf(entries[0]), filtered)
    }

    @Test
    fun matchesDetailOnlyText() {
        val filtered = filterConsoleEntries(entries, ConsoleFilter.ALL, "DETAIL PAYLOAD")

        assertEquals(listOf(entries[1]), filtered)
    }

    @Test
    fun returnsNoMatchesWhenQueryDoesNotMatch() {
        val filtered = filterConsoleEntries(entries, ConsoleFilter.LOGS, "missing")

        assertTrue(filtered.isEmpty())
    }

    @Test
    fun exportsOnlyVisibleRows() {
        val visible = filterConsoleEntries(entries, ConsoleFilter.LOGS, "")
        val exported = exportConsoleText(visible)

        assertTrue(exported.contains("Network request"))
        assertFalse(exported.contains("Start trip"))
        assertFalse(exported.contains("Request failed"))
    }
}
