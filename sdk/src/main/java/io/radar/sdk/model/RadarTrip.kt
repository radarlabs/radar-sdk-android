package io.radar.sdk.model

import io.radar.sdk.Radar
import org.json.JSONArray
import org.json.JSONObject

/**
 * Represents a trip. For more information about trip tracking, see [](https://radar.io/documentation/trip-tracking).
 *
 * @see [](https://radar.io/documentation/trip-tracking
 */
class RadarTrip(
    /**
     * The external ID of the trip
     */
    val externalId: String,

    /**
     * The optional set of custom key-value pairs for the trip.
     */
    val metadata: JSONObject?,

    /**
     * The tag of the optional destination geofence.
     */
    val destinationGeofenceTag: String?,

    /**
     * The external ID of the optional destination geofence.
     */
    val destinationGeofenceExternalId: String?,

    /**
     * The location of the optional destination geofence.
     */
    val destinationLocation: RadarCoordinate?,

    /**
     * The travel mode of the trip.
     */
    val mode: Radar.RadarRouteMode?,

    /**
     * The ETA to the optional destination geofence.
     */
    val eta: RadarRoute?,

    /**
     * A boolean indicating whether the user has arrived (destination geofence entered).
     */
    val arrived: Boolean
) {

    internal companion object {
        private const val FIELD_EXTERNAL_ID = "externalId"
        private const val FIELD_METADATA = "metadata"
        private const val FIELD_DESTINATION_GEOFENCE_TAG = "destinationGeofenceTag"
        private const val FIELD_DESTINATION_GEOFENCE_EXTERNAL_ID = "destinationGeofenceExternalId"
        private const val FIELD_DESTINATION_LOCATION = "destinationLocation"
        private const val FIELD_COORDINATES = "coordinates"
        private const val FIELD_MODE = "mode"
        private const val FIELD_ETA = "eta"
        private const val FIELD_ARRIVED = "arrived"

        @JvmStatic
        fun fromJson(obj: JSONObject?): RadarTrip? {
            if (obj == null) {
                return null
            }

            val externalId = obj.optString(FIELD_EXTERNAL_ID, null)
            val metadata: JSONObject? = obj.optJSONObject(FIELD_METADATA)
            val destinationGeofenceTag: String? = obj.optString(
                FIELD_DESTINATION_GEOFENCE_TAG, null)
            val destinationGeofenceExternalId: String? = obj.optString(
                FIELD_DESTINATION_GEOFENCE_EXTERNAL_ID, null)
            val destinationLocation: RadarCoordinate? = obj.optJSONObject(
                FIELD_DESTINATION_LOCATION)?.let { location ->
                location.optJSONArray(FIELD_COORDINATES)?.let { coordinate ->
                    RadarCoordinate(
                        coordinate.optDouble(1),
                        coordinate.optDouble(0)
                    )
                }
            }
            val mode: Radar.RadarRouteMode? = Radar.RadarRouteMode.valueOf(obj.optString(FIELD_MODE))
            val eta: RadarRoute? = RadarRoute.fromJson(obj.optJSONObject(FIELD_ETA))
            val arrived = obj.optBoolean(FIELD_ARRIVED, false)

            return RadarTrip(
                externalId,
                metadata,
                destinationGeofenceTag,
                destinationGeofenceExternalId,
                destinationLocation,
                mode,
                eta,
                arrived
            )
        }

        @JvmStatic
        fun fromJson(arr: JSONArray?): Array<RadarTrip>? {
            if (arr == null) {
                return null
            }

            return Array(arr.length()) { index ->
                fromJson(arr.optJSONObject(index))
            }.filterNotNull().toTypedArray()
        }
    }

    fun toJson(): JSONObject {
        val obj = JSONObject()
        obj.putOpt(FIELD_EXTERNAL_ID, this.externalId)
        obj.putOpt(FIELD_METADATA, this.metadata)
        obj.putOpt(FIELD_DESTINATION_GEOFENCE_TAG, this.destinationGeofenceTag)
        obj.putOpt(FIELD_DESTINATION_GEOFENCE_EXTERNAL_ID, this.destinationGeofenceExternalId)
        obj.putOpt(FIELD_DESTINATION_LOCATION, this.destinationLocation?.toJson())
        obj.putOpt(FIELD_MODE, this.mode?.let { Radar.stringForMode(it) })
        obj.putOpt(FIELD_ETA, this.eta?.toJson())
        obj.putOpt(FIELD_ARRIVED, this.arrived)
        return obj
    }

}
