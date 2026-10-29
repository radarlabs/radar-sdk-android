package io.radar.sdk

import io.radar.sdk.Radar.RadarStatus
import io.radar.sdk.model.RadarVerifiedLocationToken

// Verification instrumentation: `radar.verified.*` attributes and the trackVerified() callback wrapper.

internal fun RadarTraceSpan.verifiedFailover(failover: Boolean) = apply {
    setAttribute("radar.verified.failover", failover)
}

internal fun RadarTraceSpan.endingWith(callback: Radar.RadarTrackVerifiedCallback?): Radar.RadarTrackVerifiedCallback = object : Radar.RadarTrackVerifiedCallback {
    override fun onComplete(status: RadarStatus, token: RadarVerifiedLocationToken?) {
        end(status)
        callback?.onComplete(status, token)
    }
}
