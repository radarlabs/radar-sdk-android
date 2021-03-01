package io.radar.sdk.model

import io.radar.sdk.Radar
import org.json.JSONArray
import org.json.JSONObject

/**
 * Represents a trip.
 *
 * @see [](https://radar.io/documentation/trip-tracking)
 */
class RadarTrip(
    /**
     * The Radar ID of the trip.
     */
    val _id: String,

    /**
     * The external ID of the trip.
     */
    val externalId: String,

    /**
     * The optional set of custom key-value pairs for the trip.
     */
    val metadata: JSONObject?,

    /**
     * For trips with a destination, the tag of the destination geofence.
     */
    val destinationGeofenceTag: String?,

    /**
     * For trips with a destination, the external ID of the destination geofence.
     */
    val destinationGeofenceExternalId: String?,

    /**
     * For trips with a destination, the location of the destination geofence.
     */
    val destinationLocation: RadarCoordinate?,

    /**
     * The travel mode for the trip.
     */
    val mode: Radar.RadarRouteMode?,

    /**
     * For trips with a destination, the distance to the destination geofence in meters based on the travel mode for the trip.
     */
    val etaDistance: Double?,

    /**
     * For trips with a destination, the ETA to the destination geofence in minutes based on the travel mode for the trip.
     */
    val etaDuration: Double?,

    /**
     * The status of the trip.
     */
    val status: RadarTripStatus
) {

    enum class RadarTripStatus {
        /** Unknown */
        UNKNOWN,
        /** `started` */
        STARTED,
        /** `approaching` */
        APPROACHING,
        /** `arrived` */
        ARRIVED,
        /** `expired` */
        EXPIRED,
        /** `completed` */
        COMPLETED,
        /** `canceled` */
        CANCELED
    }

    internal companion object {
        private const val FIELD_ID = "_id"
        private const val FIELD_EXTERNAL_ID = "externalId"
        private const val FIELD_METADATA = "metadata"
        private const val FIELD_DESTINATION_GEOFENCE_TAG = "destinationGeofenceTag"
        private const val FIELD_DESTINATION_GEOFENCE_EXTERNAL_ID = "destinationGeofenceExternalId"
        private const val FIELD_DESTINATION_LOCATION = "destinationLocation"
        private const val FIELD_COORDINATES = "coordinates"
        private const val FIELD_MODE = "mode"
        private const val FIELD_ETA = "eta"
        private const val FIELD_DISTANCE = "distance"
        private const val FIELD_DURATION = "duration"
        private const val FIELD_STATUS = "status"

        @JvmStatic
        fun fromJson(obj: JSONObject?): RadarTrip? {
            if (obj == null) {
                return null
            }

            val id: String = obj.optString(FIELD_ID) ?: ""
            val externalId: String = obj.optString(FIELD_EXTERNAL_ID) ?: ""
            val metadata: JSONObject? = obj.optJSONObject(FIELD_METADATA) ?: null
            val destinationGeofenceTag: String? = obj.optString(
                FIELD_DESTINATION_GEOFENCE_TAG) ?: null
            val destinationGeofenceExternalId: String? = obj.optString(
                FIELD_DESTINATION_GEOFENCE_EXTERNAL_ID) ?: null
            val destinationLocation: RadarCoordinate? = obj.optJSONObject(
                FIELD_DESTINATION_LOCATION)?.let { location ->
                location.optJSONArray(FIELD_COORDINATES)?.let { coordinate ->
                    RadarCoordinate(
                        coordinate.optDouble(1),
                        coordinate.optDouble(0)
                    )
                }
            }
            val mode: Radar.RadarRouteMode? = when(obj.optString(FIELD_MODE)) {
                "foot" -> Radar.RadarRouteMode.FOOT
                "bike" -> Radar.RadarRouteMode.BIKE
                "car" -> Radar.RadarRouteMode.CAR
                "truck" -> Radar.RadarRouteMode.TRUCK
                "motorbike" -> Radar.RadarRouteMode.MOTORBIKE
                else -> null
            }
            val etaDistance = obj.optJSONObject(FIELD_ETA)?.optDouble(FIELD_DISTANCE)
            val etaDuration = obj.optJSONObject(FIELD_ETA)?.optDouble(FIELD_DURATION)
            val status: RadarTripStatus = when(obj.optString(FIELD_STATUS)) {
                "started" -> RadarTripStatus.STARTED
                "approaching" -> RadarTripStatus.APPROACHING
                "arrived" -> RadarTripStatus.ARRIVED
                "expired" -> RadarTripStatus.EXPIRED
                "completed" -> RadarTripStatus.COMPLETED
                "canceled" -> RadarTripStatus.CANCELED
                else -> RadarTripStatus.UNKNOWN
            }

            return RadarTrip(
                id,
                externalId,
                metadata,
                destinationGeofenceTag,
                destinationGeofenceExternalId,
                destinationLocation,
                mode,
                etaDistance,
                etaDuration,
                status
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
        obj.putOpt(FIELD_ID, this._id)
        obj.putOpt(FIELD_EXTERNAL_ID, this.externalId)
        obj.putOpt(FIELD_METADATA, this.metadata)
        obj.putOpt(FIELD_DESTINATION_GEOFENCE_TAG, this.destinationGeofenceTag)
        obj.putOpt(FIELD_DESTINATION_GEOFENCE_EXTERNAL_ID, this.destinationGeofenceExternalId)
        obj.putOpt(FIELD_DESTINATION_LOCATION, this.destinationLocation?.toJson())
        obj.putOpt(FIELD_MODE, this.mode?.let { Radar.stringForMode(it) })
        val etaObj = JSONObject()
        etaObj.putOpt(FIELD_DISTANCE, this.etaDistance)
        etaObj.putOpt(FIELD_DURATION, this.etaDuration)
        obj.putOpt(FIELD_ETA, etaObj)
        obj.putOpt(FIELD_STATUS, Radar.stringForTripStatus(status))
        return obj
    }

}
