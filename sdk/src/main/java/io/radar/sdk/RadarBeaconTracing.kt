package io.radar.sdk

import io.radar.sdk.Radar.RadarStatus
import io.radar.sdk.model.RadarBeacon

// Beacon instrumentation: `radar.beacon*` attributes and the `radar.beacon.range` span.

internal fun RadarTraceSpan.beacons(requested: Boolean) = apply {
    setAttribute("radar.beacons", requested)
}

/** `continuous` (continuous ranging supplied the beacons), `search` (search and one-shot ranging), or `skipped`. */
internal fun RadarTraceSpan.beaconSource(source: String) = apply {
    setAttribute("radar.beacon.source", source)
}

/** Traces one-shot ranging as a `radar.beacon.range` child of [parent]. [mode] is `uuids` (by UUID or UID) or `beacons`. */
internal fun Radar.RadarBeaconCallback.traced(parent: RadarTraceSpan, mode: String): Radar.RadarBeaconCallback {
    val callback = this
    val span = parent.child("radar.beacon.range")
    span.setAttribute("radar.beacon.mode", mode)
    return object : Radar.RadarBeaconCallback {
        override fun onComplete(status: RadarStatus, beacons: Array<RadarBeacon>?) {
            beacons?.let { span.setAttribute("radar.beacon.ranged_count", it.size.toLong()) }
            span.end(status)
            callback.onComplete(status, beacons)
        }
    }
}
