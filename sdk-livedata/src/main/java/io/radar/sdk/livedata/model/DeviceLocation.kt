package io.radar.sdk.livedata.model

import android.location.Location
import io.radar.sdk.Radar

/**
 * Contains information about a current location that has not necessarily synced to the server
 *
 * @param[location] The location.
 * @param[stopped] A boolean indicating whether the client is stopped.
 * @param[source] The source of the location.
 * @see [io.radar.sdk.RadarReceiver.onClientLocationUpdated]
 */
data class DeviceLocation(
    val location: Location,
    val stopped: Boolean,
    val source: Radar.RadarLocationSource
)
