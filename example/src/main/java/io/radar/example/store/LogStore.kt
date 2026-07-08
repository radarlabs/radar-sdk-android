package io.radar.example.store

import android.content.Context
import android.location.Location
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.mutableStateListOf
import io.radar.example.Utils
import io.radar.sdk.Radar
import io.radar.sdk.RadarReceiver
import io.radar.sdk.model.RadarEvent
import io.radar.sdk.model.RadarUser

/**
 * Single source of truth for `RadarReceiver` callbacks and user-action logging.
 *
 * The example app registers exactly one [LogStore] as the SDK receiver (passed to
 * `Radar.initialize`). Every UI consumer (Debug tab, Tests-tab recent activity) reads
 * [entries]; non-UI consumers ([TripBuilderStore]) subscribe via [onEvents] / [onLocation].
 *
 * This replaces the old two-receiver setup (one passed to `initialize`, a different one
 * set in `MainView`) with a single funnel.
 */
class LogStore : RadarReceiver() {

    /** Cap on retained timeline entries. Older entries are dropped FIFO. */
    private val maxEntries = 2000

    val entries = mutableStateListOf<ConsoleEntry>()

    /** Non-UI subscribers (e.g. the trip builder mirrors trip state off these). */
    var onEvents: ((events: Array<RadarEvent>, user: RadarUser?) -> Unit)? = null
    var onLocation: ((location: Location) -> Unit)? = null

    private val mainHandler = Handler(Looper.getMainLooper())

    // MARK: - Public write API (used by ActionButton + the verified/IAM receivers)

    fun write(kind: ConsoleKind, summary: String, detail: String? = null) {
        append(ConsoleEntry(kind = kind, summary = summary, detail = detail))
    }

    fun writeAction(title: String, detail: String? = null) = write(ConsoleKind.ACTION, title, detail)

    fun writeResult(title: String, detail: String? = null) = write(ConsoleKind.RESULT, title, detail)

    fun writeError(title: String, detail: String? = null) = write(ConsoleKind.ERROR, title, detail)

    /** Write a RESULT on success, an ERROR otherwise — for SDK completion handlers. */
    fun writeStatus(status: Radar.RadarStatus, summary: String, detail: String? = null) {
        if (status == Radar.RadarStatus.SUCCESS) writeResult(summary, detail) else writeError(summary, detail)
    }

    fun clear() {
        entries.clear()
    }

    private fun append(entry: ConsoleEntry) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            appendOnMain(entry)
        } else {
            mainHandler.post { appendOnMain(entry) }
        }
    }

    private fun appendOnMain(entry: ConsoleEntry) {
        entries.add(entry)
        if (entries.size > maxEntries) {
            entries.removeRange(0, entries.size - maxEntries)
        }
    }

    // MARK: - RadarReceiver

    override fun onEventsReceived(context: Context, events: Array<RadarEvent>, user: RadarUser?) {
        onEvents?.invoke(events, user)
        for (event in events) {
            append(ConsoleEntry(kind = ConsoleKind.EVENT, summary = summarize(event), detail = detailJson(event)))
        }
    }

    override fun onLocationUpdated(context: Context, location: Location, user: RadarUser) {
        onLocation?.invoke(location)
        append(ConsoleEntry(kind = ConsoleKind.LOCATION, summary = "synced  " + summarize(location)))
    }

    override fun onClientLocationUpdated(
        context: Context,
        location: Location,
        stopped: Boolean,
        source: Radar.RadarLocationSource,
    ) {
        onLocation?.invoke(location)
        val tag = if (stopped) "stopped" else "moving"
        append(ConsoleEntry(kind = ConsoleKind.LOCATION, summary = "$tag  " + summarize(location)))
    }

    override fun onError(context: Context, status: Radar.RadarStatus) {
        append(ConsoleEntry(kind = ConsoleKind.ERROR, summary = "onError: ${Utils.stringForRadarStatus(status)}"))
    }

    override fun onLog(context: Context, message: String) {
        append(ConsoleEntry(kind = ConsoleKind.LOG, summary = message))
    }

    // MARK: - Formatting

    private fun summarize(event: RadarEvent): String {
        val type = Utils.stringForRadarEvent(event)
        val externalId = event.geofence?.externalId
        if (!externalId.isNullOrEmpty()) return "$type — $externalId"
        return type
    }

    private fun detailJson(event: RadarEvent): String? =
        try {
            event.toJson().toString(2)
        } catch (e: Exception) {
            null
        }

    private fun summarize(location: Location): String {
        val lat = String.format("%.5f", location.latitude)
        val lng = String.format("%.5f", location.longitude)
        val acc = location.accuracy.toInt()
        return "$lat, $lng  ±${acc}m"
    }
}
