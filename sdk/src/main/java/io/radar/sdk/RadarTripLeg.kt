package io.radar.sdk

import org.json.JSONObject
import java.util.*

/**
 * @see [](https://radar.io/documentation/sdk/android)
 */

// TODO: check for 0 or 1 of destination
data class RadarTripLeg(
    // destination fields
    val destinationGeofenceId: String? = null,
    val destinationGeofenceTag: String? = null,
    val destinationGeofenceExternalId: String? = null,
    val address: String? = null,
    val coordinates: String? = null,

    val status: String? = null,
    val metadata: JSONObject? = null
    // approachingAt
    // scheduledArrivalAt
) {

}