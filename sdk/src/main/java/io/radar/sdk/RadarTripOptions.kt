package io.radar.sdk

import org.json.JSONObject
import java.util.*

/**
 * An options class used to configure background tracking.
 *
 * @see [](https://radar.io/documentation/sdk/android)
 */
data class RadarTripOptions(
    /**
     * A stable unique ID for the trip.
     */
    var externalId: String,

    /**
     * An optional set of custom key-value pairs for the trip.
     */
    var metadata: JSONObject? = null,

    /**
     * For trips with a destination, the tag of the destination geofence.
     */
    var destinationGeofenceTag: String? = null,

    /**
     * For trips with a destination, the external ID of the destination geofence.
     */
    var destinationGeofenceExternalId: String? = null,

    /**
     * For trips with a destination, the travel mode.
     */
    var mode: Radar.RadarRouteMode = Radar.RadarRouteMode.CAR,

    /**
     * The scheduled arrival time for the trip.
     */
    var scheduledArrivalAt: Date? = null,

    var approachingThreshold: Int = 0
) {

    companion object {

        internal const val KEY_EXTERNAL_ID = "externalId"
        internal const val KEY_METADATA = "metadata"
        internal const val KEY_DESTINATION_GEOFENCE_TAG = "destinationGeofenceTag"
        internal const val KEY_DESTINATION_GEOFENCE_EXTERNAL_ID = "destinationGeofenceExternalId"
        internal const val KEY_MODE = "mode"
        internal const val KEY_SCHEDULED_ARRIVAL_AT = "scheduledArrivalAt"

        @JvmStatic
        fun fromJson(obj: JSONObject): RadarTripOptions {
            return RadarTripOptions(
                externalId = obj.optString(KEY_EXTERNAL_ID),
                metadata = obj.optJSONObject(KEY_METADATA),
                destinationGeofenceTag = obj.optString(KEY_DESTINATION_GEOFENCE_TAG),
                destinationGeofenceExternalId = obj.optString(KEY_DESTINATION_GEOFENCE_EXTERNAL_ID),
                mode = when(obj.optString(KEY_MODE)) {
                    "foot" -> Radar.RadarRouteMode.FOOT
                    "bike" -> Radar.RadarRouteMode.BIKE
                    "truck" -> Radar.RadarRouteMode.TRUCK
                    "motorbike" -> Radar.RadarRouteMode.MOTORBIKE
                    else -> Radar.RadarRouteMode.CAR
                },
                scheduledArrivalAt = if (obj.has(KEY_SCHEDULED_ARRIVAL_AT)) Date(obj.optLong(
                    KEY_SCHEDULED_ARRIVAL_AT
                )) else null,
            )
        }

    }

    fun toJson(): JSONObject {
        val obj = JSONObject()
        obj.put(KEY_EXTERNAL_ID, externalId)
        obj.put(KEY_METADATA, metadata)
        obj.put(KEY_DESTINATION_GEOFENCE_TAG, destinationGeofenceTag)
        obj.put(KEY_DESTINATION_GEOFENCE_EXTERNAL_ID, destinationGeofenceExternalId)
        obj.put(KEY_MODE, Radar.stringForMode(mode))
        obj.put(KEY_SCHEDULED_ARRIVAL_AT, scheduledArrivalAt?.time)
        return obj
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }

        if (this.javaClass != other?.javaClass) {
            return false
        }

        other as RadarTripOptions

        return this.externalId == other.externalId &&
                this.metadata?.toString() == other.metadata?.toString() &&
                this.destinationGeofenceTag == other.destinationGeofenceTag &&
                this.destinationGeofenceExternalId == other.destinationGeofenceExternalId &&
                this.mode == other.mode &&
                this.scheduledArrivalAt?.time == other.scheduledArrivalAt?.time
    }

}