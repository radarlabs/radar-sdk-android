package io.radar.sdk

import org.json.JSONObject
import java.util.*
import io.radar.sdk.model.RadarTripLeg


/**
 * An options class used to configure trip options.
 *
 * @see [](https://radar.com/documentation/trip-tracking)
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

    var approachingThreshold: Int = 0,

    var startTracking: Boolean = true,

    /**
     * For multi-destination trips, an optional array of trip legs.
     */
    var legs: Array<RadarTripLeg>? = null
) {

    companion object {

        internal const val KEY_EXTERNAL_ID = "externalId"
        internal const val KEY_METADATA = "metadata"
        internal const val KEY_DESTINATION_GEOFENCE_TAG = "destinationGeofenceTag"
        internal const val KEY_DESTINATION_GEOFENCE_EXTERNAL_ID = "destinationGeofenceExternalId"
        internal const val KEY_MODE = "mode"
        internal const val KEY_SCHEDULED_ARRIVAL_AT = "scheduledArrivalAt"
        internal const val KEY_APPROACHING_THRESHOLD = "approachingThreshold"
        internal const val KEY_START_TRACKING = "startTracking"
        internal const val KEY_LEGS = "legs"

        @JvmStatic
        fun fromJson(obj: JSONObject): RadarTripOptions {
            val options = RadarTripOptions(
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
                scheduledArrivalAt = if (obj.has(KEY_SCHEDULED_ARRIVAL_AT)) {
                    var scheduledArrivalAtLong = obj.optLong(
                        KEY_SCHEDULED_ARRIVAL_AT
                    )
                    if (scheduledArrivalAtLong != 0L) {
                        Date(scheduledArrivalAtLong)
                    } else {
                        RadarUtils.isoStringToDate(obj.optString(KEY_SCHEDULED_ARRIVAL_AT))
                    }
                } else null,
                approachingThreshold = obj.optInt(KEY_APPROACHING_THRESHOLD),
                startTracking = obj.optBoolean(KEY_START_TRACKING, true)
            )
            options.legs = obj.optJSONArray(KEY_LEGS)?.let { RadarTripLeg.fromJson(it) }
            return options
        }
    }

    fun toJson(): JSONObject {
        val obj = JSONObject()
        obj.put(KEY_EXTERNAL_ID, externalId)
        obj.put(KEY_METADATA, metadata)
        obj.put(KEY_DESTINATION_GEOFENCE_TAG, destinationGeofenceTag)
        obj.put(KEY_DESTINATION_GEOFENCE_EXTERNAL_ID, destinationGeofenceExternalId)
        obj.put(KEY_MODE, Radar.stringForMode(mode))
        if (scheduledArrivalAt != null) {
            obj.put(KEY_SCHEDULED_ARRIVAL_AT, RadarUtils.dateToISOString(scheduledArrivalAt))
        }
        if (approachingThreshold > 0) {
            obj.put(KEY_APPROACHING_THRESHOLD, approachingThreshold)
        }
        obj.put("startTracking", startTracking)

        legs?.let {
            if (it.isNotEmpty()) {
                obj.put(KEY_LEGS, RadarTripLeg.toJson(it))
            }
        }
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
                this.scheduledArrivalAt?.time == other.scheduledArrivalAt?.time &&
                this.approachingThreshold == other.approachingThreshold &&
                this.startTracking == other.startTracking &&
                this.legs?.contentDeepEquals(other.legs ?: emptyArray()) ?: (other.legs == null)
    }

    // Array properties require manual equals/hashCode since data class
    // auto-generated versions use reference equality for arrays.
    override fun hashCode(): Int {
        var result = externalId.hashCode()
        result = 31 * result + (metadata?.toString()?.hashCode() ?: 0)
        result = 31 * result + (destinationGeofenceTag?.hashCode() ?: 0)
        result = 31 * result + (destinationGeofenceExternalId?.hashCode() ?: 0)
        result = 31 * result + mode.hashCode()
        result = 31 * result + (scheduledArrivalAt?.time?.hashCode() ?: 0)
        result = 31 * result + approachingThreshold
        result = 31 * result + startTracking.hashCode()
        result = 31 * result + (legs?.contentDeepHashCode() ?: 0)
        return result
    }
}