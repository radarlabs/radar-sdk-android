package io.radar.sdk

import android.content.Context
import android.location.Location
import android.os.SystemClock
import io.radar.sdk.Radar.RadarStatus
import java.util.Locale

// Location instrumentation: `radar.location.*` attributes and the `radar.location.get` span.

internal fun RadarTraceSpan.desiredAccuracy(desiredAccuracy: RadarTrackingOptions.RadarTrackingOptionsDesiredAccuracy) = apply {
    setAttribute("radar.location.desired_accuracy", desiredAccuracy.name.lowercase(Locale.ROOT))
}

/** `foreground_location`, `background_location`, `geofence_exit`, and so on. */
internal fun RadarTraceSpan.locationSource(source: Radar.RadarLocationSource) = apply {
    setAttribute("radar.location.source", source.name.lowercase(Locale.ROOT))
}

/** Age and accuracy of [location], and the provider that supplied it. A small age means a cached or fresh fix. */
internal fun RadarTraceSpan.locationQuality(location: Location, context: Context) = recording {
    locationAgeMs(location)?.let { setAttribute("radar.location.age_ms", it) }
    setAttribute("radar.location.accuracy_m", location.accuracy.toDouble())
    RadarSettings.getLocationServicesProvider(context)?.let { setAttribute("radar.location.services_provider", it) }
}

/**
 * Uses the monotonic clock when the location has it. Locations the SDK rebuilds from saved fields (job extras,
 * RadarState) have no elapsedRealtimeNanos, so their age comes from the wall clock instead. Null when the location
 * has neither timestamp.
 */
private fun locationAgeMs(location: Location): Long? {
    val ageMs = when {
        location.elapsedRealtimeNanos > 0 -> (SystemClock.elapsedRealtimeNanos() - location.elapsedRealtimeNanos) / 1_000_000
        location.time > 0 -> System.currentTimeMillis() - location.time
        else -> return null
    }
    return ageMs.coerceAtLeast(0)
}

/** Traces getting a location as a `radar.location.get` child of [parent]. A small age means a cached location. */
internal fun Radar.RadarLocationCallback.traced(parent: RadarTraceSpan, context: Context): Radar.RadarLocationCallback {
    val callback = this
    val span = parent.child("radar.location.get")
    return object : Radar.RadarLocationCallback {
        override fun onComplete(status: RadarStatus, location: Location?, stopped: Boolean) {
            if (location != null) {
                span.locationQuality(location, context)
            }
            span.end(status)
            callback.onComplete(status, location, stopped)
        }
    }
}
