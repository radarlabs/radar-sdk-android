package io.radar.sdk

import io.radar.sdk.Radar.RadarStatus
import io.radar.sdk.model.RadarVerifiedLocationToken

// Verification instrumentation: `radar.verified.*` attributes, the trackVerified() callback wrapper, and the
// `radar.fraud.prepare` span.

internal fun RadarTraceSpan.verifiedFailover(failover: Boolean) = apply {
    setAttribute("radar.verified.failover", failover)
}

internal fun RadarTraceSpan.endingWith(callback: Radar.RadarTrackVerifiedCallback?): Radar.RadarTrackVerifiedCallback = object : Radar.RadarTrackVerifiedCallback {
    override fun onComplete(status: RadarStatus, token: RadarVerifiedLocationToken?) {
        end(status)
        callback?.onComplete(status, token)
    }
}

/**
 * Ends this `radar.fraud.prepare` span with the preparation's status, then calls [callback]. The span covers the
 * whole fraud SDK call as seen from core, including attestation when it runs. A success without a payload ends as
 * `ERROR_BAD_REQUEST`, as trackVerified() treats it.
 */
internal fun RadarTraceSpan.endingWith(callback: (RadarStatus, RadarPreparedFraudPayload?) -> Unit): (RadarStatus, RadarPreparedFraudPayload?) -> Unit = { status, payload ->
    end(if (status == RadarStatus.SUCCESS && payload == null) RadarStatus.ERROR_BAD_REQUEST else status)
    callback(status, payload)
}
