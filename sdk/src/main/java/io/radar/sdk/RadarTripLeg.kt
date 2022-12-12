package io.radar.sdk

import org.json.JSONArray
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

    companion object {
        internal const val KEY_DESTINATION = "destination"
        internal const val KEY_METADATA = "metadata"
        internal const val KEY_SCHEDULED_ARRIVAL_AT = "scheduledArrivalAt"

        // destination
        internal const val KEY_DESTINATION_GEOFENCE_TAG = "destinationGeofenceTag"
        internal const val KEY_DESTINATION_GEOFENCE_EXTERNAL_ID = "destinationGeofenceExternalId"
        internal const val KEY_DESTINATION_GEOFENCE_ID = "destinationGeofenceId"
        internal const val KEY_DESTINATION_ADDRESS = "address"
        internal const val KEY_DESTINATION_COORDINATES = "coordinates"

        @JvmStatic
        fun toJson(legs: Array<RadarTripLeg> ?): JSONArray? {
            if (legs == null) {
                return null
            }

            val arr = JSONArray()
            legs.forEach { legs ->
                arr.put(legs.toJson())
            }
            return arr
        }
    }

    fun toJson(): JSONObject {
        val obj = JSONObject()
        val destination = JSONObject()
        destination.put(RadarTripLeg.KEY_DESTINATION_GEOFENCE_TAG, destinationGeofenceTag)
        destination.put(RadarTripLeg.KEY_DESTINATION_GEOFENCE_EXTERNAL_ID, destinationGeofenceExternalId)
        destination.put(RadarTripLeg.KEY_DESTINATION_GEOFENCE_ID, destinationGeofenceId)
        destination.put(RadarTripLeg.KEY_DESTINATION_ADDRESS, address)
        destination.put(RadarTripLeg.KEY_DESTINATION_COORDINATES, coordinates)

        obj.put(RadarTripLeg.KEY_METADATA, metadata)
        obj.put(RadarTripLeg.KEY_DESTINATION, destination)
//        obj.put(RadarTripLeg.KEY_SCHEDULED_ARRIVAL_AT, scheduledArrivalAt?.time)
        return obj
    }

}