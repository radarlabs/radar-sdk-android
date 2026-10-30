package io.radar.sdk

import android.content.Context
import android.location.Location
import io.radar.sdk.Radar.RadarStatus
import io.radar.sdk.model.RadarEvent
import io.radar.sdk.model.RadarUser

// Track instrumentation: `radar.track.*` and other attributes of trackOnce(), trackVerified(), and tracking update
// root spans.

/** App state that changes how long getting a location takes. */
internal fun RadarTraceSpan.trackContext(context: Context) = apply {
    setAttribute("radar.app.foreground", RadarActivityLifecycleCallbacks.foreground)
    setAttribute("radar.location.accuracy_authorization", RadarUtils.getLocationAccuracyAuthorization(context))
}

/** Set on the root span when its track is sent as a replay flush instead. */
internal fun RadarTraceSpan.replayed() = apply {
    setAttribute("radar.track.replayed", true)
}

internal fun RadarTraceSpan.stopped(stopped: Boolean) = apply {
    setAttribute("radar.track.stopped", stopped)
}

/** Set on the root span when its track is added to a batch, so no request is sent yet. */
internal fun RadarTraceSpan.batched() = apply {
    setAttribute("radar.track.batched", true)
}

/**
 * Set on a tracking update's root span when it ends without sending a track. [reason] is `ranging_failed`,
 * `no_beacons`, `beacon_state_unchanged`, or `no_synced_beacons`.
 */
internal fun RadarTraceSpan.skipped(reason: String) = apply {
    setAttribute("radar.track.skip_reason", reason)
}

/** Set on the `POST /v1/track/replay` client span. */
internal fun RadarTraceSpan.replayCount(count: Int) = apply {
    setAttribute("radar.replay.count", count.toLong())
}

internal fun RadarTraceSpan.endingWith(callback: Radar.RadarTrackCallback?): Radar.RadarTrackCallback = object : Radar.RadarTrackCallback {
    override fun onComplete(status: RadarStatus, location: Location?, events: Array<RadarEvent>?, user: RadarUser?) {
        end(status)
        callback?.onComplete(status, location, events, user)
    }
}
