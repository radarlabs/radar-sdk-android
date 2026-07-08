package io.radar.example.store

import java.util.concurrent.atomic.AtomicLong

/** The type of a console row — drives its icon and tint in the Debug tab. */
enum class ConsoleKind {
    /** User tapped an action button (logged automatically by ActionButton). */
    ACTION,

    /** An SDK call returned to its completion handler. */
    RESULT,

    /** An SDK event fired (`onEventsReceived`). */
    EVENT,

    /** An SDK location update (`onLocationUpdated` / `onClientLocationUpdated`). */
    LOCATION,

    /** An SDK debug message (`onLog`). */
    LOG,

    /** An SDK failure (`onError` or a non-success status). */
    ERROR,
}

/**
 * One row in the unified console timeline.
 *
 * [summary] is the compact one-line representation; [detail] is the optional multi-line
 * payload revealed when the row is expanded (pretty-printed event JSON, formatted SDK
 * result, full location vector, etc.).
 */
data class ConsoleEntry(
    val kind: ConsoleKind,
    val summary: String,
    val detail: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val id: Long = nextId(),
) {
    companion object {
        private val counter = AtomicLong(0)
        private fun nextId(): Long = counter.getAndIncrement()
    }
}
