package io.radar.example

import android.content.Context
import android.location.Location
import androidx.compose.runtime.mutableStateListOf
import io.radar.sdk.Radar
import io.radar.sdk.RadarReceiver
import io.radar.sdk.model.RadarEvent
import io.radar.sdk.model.RadarUser
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MyRadarReceiver : RadarReceiver() {
    // Full, unfiltered SDK log + event feeds used by the default "Logs" tab.
    var events = mutableStateListOf<RadarEvent>()
    var logs = mutableStateListOf<String>()

    // Filtered feed used by the dedicated "Offline" QA tab. Mirrors the iOS
    // `DemoLog` behavior: only offline-event generation, offline RTO ramps,
    // sync-manager decisions, event summaries, and tracking-option transitions.
    var offlineLogs = mutableStateListOf<String>()

    private var lastTrackingOptionsKey: String? = null

    private val timestampFmt = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)

    override fun onEventsReceived(context: Context, events: Array<RadarEvent>, user: RadarUser?) {
        this.events.addAll(events)

        val summary = events.joinToString(separator = ", ") { event ->
            val offline = event.metadata?.optBoolean("offline", false) == true
            val gf = event.geofence?.description
                ?: event.geofence?.externalId
                ?: event.geofence?._id
                ?: "-"
            "${event.type}:$gf:offline=$offline"
        }
        appendOfflineLog("didReceiveEvents [$summary]")

        for (event in events) {
            val isOffline = event.metadata?.optBoolean("offline", false) == true
            val prefix = if (isOffline) "Offline" else "Online"
            val action = when (event.type) {
                RadarEvent.RadarEventType.USER_ENTERED_GEOFENCE -> "entered"
                RadarEvent.RadarEventType.USER_EXITED_GEOFENCE -> "exited"
                else -> null
            } ?: continue

            val geofenceLabel = event.geofence?.description
                ?: event.geofence?.externalId
                ?: event.geofence?._id
                ?: "-"
            val tag = event.geofence?.tag
            val body = if (tag != null) "$geofenceLabel (tag: $tag)" else geofenceLabel

            QaNotifier.post(context, "$prefix $action geofence", body)
        }

        checkForTrackingOptionsChange(context)
    }

    override fun onLocationUpdated(context: Context, location: Location, user: RadarUser) {
        appendOfflineLog(
            "onLocationUpdated lat=${location.latitude} lng=${location.longitude} " +
                "acc=${location.accuracy}"
        )
        checkForTrackingOptionsChange(context)
    }

    override fun onClientLocationUpdated(
        context: Context,
        location: Location,
        stopped: Boolean,
        source: Radar.RadarLocationSource
    ) {
    }

    override fun onError(context: Context, status: Radar.RadarStatus) {
    }

    override fun onLog(context: Context, message: String) {
        this.logs.add(message)

        if (message.contains("OfflineEventManager:") || message.contains("SyncManager:")) {
            appendOfflineLog(message)
        }
    }

    private fun checkForTrackingOptionsChange(context: Context) {
        val current = Radar.getTrackingOptions()
        val key = "${current.desiredStoppedUpdateInterval}|" +
                "${current.desiredMovingUpdateInterval}|" +
                "${current.desiredAccuracy.name}"

        val previous = lastTrackingOptionsKey
        lastTrackingOptionsKey = key

        if (previous == null || previous == key) return

        val direction = if (current.desiredMovingUpdateInterval in 1..60) "RAMP UP" else "RAMP DOWN"
        val body = "stopped=${current.desiredStoppedUpdateInterval}s " +
                "moving=${current.desiredMovingUpdateInterval}s " +
                "accuracy=${current.desiredAccuracy.name}"

        appendOfflineLog("trackingOptions $direction | $body (was $previous)")
        QaNotifier.post(context, "Tracking options changed — $direction", body)
    }

    private fun appendOfflineLog(message: String) {
        val line = "[${timestampFmt.format(Date())}] $message"
        offlineLogs.add(line)
        // Cap growth so the UI doesn't turn into molasses on long QA walks.
        while (offlineLogs.size > 500) {
            offlineLogs.removeAt(0)
        }
    }
}
