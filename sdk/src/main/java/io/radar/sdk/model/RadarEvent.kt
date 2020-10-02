package io.radar.sdk.model

import android.annotation.SuppressLint
import android.location.Location
import io.radar.sdk.model.RadarEvent.RadarEventType.*
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.TimeZone

/**
 * Represents a change in user state. For more information, see [](https://radar.io/documentation).
 *
 * @see [](https://radar.io/documentation)
 */
class RadarEvent(
    /**
     * The Radar ID of the event.
     */
    val _id: String,

    /**
     * The datetime when the event occurred on the device.
     */
    val createdAt: Date,

    /**
     * The datetime when the event was created on the server.
     */
    val actualCreatedAt: Date,

    /**
     * A boolean indicating whether the event was generated with your live API key.
     */
    val live: Boolean,

    /**
     * The type of the event.
     */
    val type: RadarEventType,

    /**
     * The geofence for which the event was generated. May be `null` for non-geofence events.
     */
    val geofence: RadarGeofence?,

    /**
     * The place for which the event was generated. May be `null` for non-place events.
     */
    val place: RadarPlace?,

    /**
     * The region for which the event was generated. May be `null` for non-region events.
     */
    val region: RadarRegion?,

    /**
     * For place entry events, alternate place candidates. May be `null` for non-place events.
     */
    val alternatePlaces: Array<RadarPlace>?,

    /**
     * For accepted place entry events, the verified place. May be `null` for non-place events or unverified events.
     */
    val verifiedPlace: RadarPlace?,

    /**
     * The verification of the event.
     */
    val verification: RadarEventVerification,

    /**
     * The confidence level of the event.
     */
    val confidence: RadarEventConfidence,

    /**
     * The duration between entry and exit events, in minutes, for exit events. 0 for entry events.
     */
    val duration: Float,

    /**
     * The location of the event.
     */
    val location: Location
) {

    /**
     * The types for events.
     */
    enum class RadarEventType {
        /** Unknown */
        UNKNOWN,
        /** `user.entered_geofence` */
        USER_ENTERED_GEOFENCE,
        /** `user.exited_geofence` */
        USER_EXITED_GEOFENCE,
        /** `user.entered_home` */
        USER_ENTERED_HOME,
        /** `user.exited_home` */
        USER_EXITED_HOME,
        /** `user.entered_office` */
        USER_ENTERED_OFFICE,
        /** `user.exited_office` */
        USER_EXITED_OFFICE,
        /** `user.started_traveling` */
        USER_STARTED_TRAVELING,
        /** `user.stopped_traveling` */
        USER_STOPPED_TRAVELING,
        /** `user.entered_place` */
        USER_ENTERED_PLACE,
        /** `user.exited_place` */
        USER_EXITED_PLACE,
        /** `user.nearby_place_chain` */
        USER_NEARBY_PLACE_CHAIN,
        /** `user.entered_region_country` */
        USER_ENTERED_REGION_COUNTRY,
        /** `user.exited_region_country` */
        USER_EXITED_REGION_COUNTRY,
        /** `user.entered_region_state` */
        USER_ENTERED_REGION_STATE,
        /** `user.exited_region_state` */
        USER_EXITED_REGION_STATE,
        /** `user.entered_region_dma` */
        USER_ENTERED_REGION_DMA,
        /** `user.exited_region_dma` */
        USER_EXITED_REGION_DMA,
        /** `user.started_commuting` */
        USER_STARTED_COMMUTING,
        /** `user.stopped_commuting` */
        USER_STOPPED_COMMUTING,
        /** `user.started_trip` */
        USER_STARTED_TRIP,
        /** `user.updated_trip` */
        USER_UPDATED_TRIP,
        /** `user.approaching_trip_destination` */
        USER_APPROACHING_TRIP_DESTINATION,
        /** `user.arrived_at_trip` */
        USER_ARRIVED_AT_TRIP_DESTINATION,
        /** `user.stopped_trip` */
        USER_STOPPED_TRIP
    }

    /**
     * The confidence levels for events.
     */
    enum class RadarEventConfidence {
        /** Unknown confidence */
        NONE,
        /** Low confidence */
        LOW,
        /** Medium confidence */
        MEDIUM,
        /** High confidence */
        HIGH
    }

    /**
     * The verification types for events.
     */
    enum class RadarEventVerification {
        /** Accept event */
        ACCEPT,
        /** Unverify event */
        UNVERIFY,
        /** Reject event */
        REJECT
    }

    internal companion object {
        private const val FIELD_ID = "_id"
        private const val FIELD_CREATED = "createdAt"
        private const val FIELD_ACTUAL_CREATED = "actualCreatedAt"
        private const val FIELD_LIVE = "live"
        private const val FIELD_TYPE = "type"
        private const val FIELD_GEOFENCE = "geofence"
        private const val FIELD_PLACE = "place"
        private const val FIELD_REGION = "region"
        private const val FIELD_ALTERNATE_PLACES = "alternatePlaces"
        private const val FIELD_VERIFIED_PLACE = "verifiedPlace"
        private const val FIELD_VERIFICATION = "verification"
        private const val FIELD_CONFIDENCE = "confidence"
        private const val FIELD_DURATION = "duration"
        private const val FIELD_LOCATION = "location"
        private const val FIELD_COORDINATES = "coordinates"
        private const val FIELD_LOCATION_ACCURACY = "locationAccuracy"

        @JvmStatic
        @SuppressLint("SimpleDateFormat")
        private fun fromJson(obj: JSONObject?): RadarEvent? {
            if (obj == null) {
                return null
            }

            val id = obj.optString(FIELD_ID, null)
            val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
            dateFormat.timeZone = TimeZone.getTimeZone("UTC")
            val createdAt = obj.optString(FIELD_CREATED).let { createdAtStr ->
                dateFormat.parse(createdAtStr)
            } ?: Date()
            val actualCreatedAt = obj.optString(FIELD_ACTUAL_CREATED).let { actualCreatedAtStr ->
                dateFormat.parse(actualCreatedAtStr)
            } ?: Date()
            val live = obj.optBoolean(FIELD_LIVE)
            val type = when (obj.optString(FIELD_TYPE)) {
                "user.entered_geofence" -> USER_ENTERED_GEOFENCE
                "user.exited_geofence" -> USER_EXITED_GEOFENCE
                "user.entered_home" -> USER_ENTERED_HOME
                "user.exited_home" -> USER_EXITED_HOME
                "user.entered_office" -> USER_ENTERED_OFFICE
                "user.exited_office" -> USER_EXITED_OFFICE
                "user.started_traveling" -> USER_STARTED_TRAVELING
                "user.stopped_traveling" -> USER_STOPPED_TRAVELING
                "user.entered_place" -> USER_ENTERED_PLACE
                "user.exited_place" -> USER_EXITED_PLACE
                "user.nearby_place_chain" -> USER_NEARBY_PLACE_CHAIN
                "user.entered_region_country" -> USER_ENTERED_REGION_COUNTRY
                "user.exited_region_country" -> USER_EXITED_REGION_COUNTRY
                "user.entered_region_state" -> USER_ENTERED_REGION_STATE
                "user.exited_region_state" -> USER_EXITED_REGION_STATE
                "user.entered_region_dma" -> USER_ENTERED_REGION_DMA
                "user.exited_region_dma" -> USER_EXITED_REGION_DMA
                "user.started_commuting" -> USER_STARTED_COMMUTING
                "user.stopped_commuting" -> USER_STOPPED_COMMUTING
                "user.started_trip" -> USER_STARTED_TRIP
                "user.updated_trip" -> USER_UPDATED_TRIP
                "user.approaching_trip_destination" -> USER_APPROACHING_TRIP_DESTINATION
                "user.arrived_at_trip_destination" -> USER_ARRIVED_AT_TRIP_DESTINATION
                "user.stopped_trip" -> USER_STOPPED_TRIP
                else -> UNKNOWN
            }
            val geofence = RadarGeofence.fromJson(obj.optJSONObject(FIELD_GEOFENCE))
            val place = RadarPlace.fromJson(obj.optJSONObject(FIELD_PLACE))
            val region = RadarRegion.fromJson(obj.optJSONObject(FIELD_REGION))
            val alternatePlaces = RadarPlace.fromJson(obj.optJSONArray(FIELD_ALTERNATE_PLACES))
            val verifiedPlace = RadarPlace.fromJson(obj.optJSONObject(FIELD_VERIFIED_PLACE))
            val verification = when (obj.optInt(FIELD_VERIFICATION)) {
                1 -> RadarEventVerification.ACCEPT
                -1 -> RadarEventVerification.REJECT
                else -> RadarEventVerification.UNVERIFY
            }
            val confidence = when (obj.optInt(FIELD_CONFIDENCE)) {
                3 -> RadarEventConfidence.HIGH
                2 -> RadarEventConfidence.MEDIUM
                1 -> RadarEventConfidence.LOW
                else -> RadarEventConfidence.NONE
            }
            val duration = obj.optDouble(FIELD_DURATION, 0.0).toFloat()
            val locationObj = obj.optJSONObject(FIELD_LOCATION)
            val locationCoordinatesObj = locationObj?.optJSONArray(FIELD_COORDINATES)
            val location = Location("RadarSDK").apply {
                longitude = locationCoordinatesObj?.optDouble(0) ?: 0.0
                latitude = locationCoordinatesObj?.optDouble(1) ?: 0.0
                if (obj.has(FIELD_LOCATION_ACCURACY)) {
                    accuracy = obj.optDouble(FIELD_LOCATION_ACCURACY).toFloat()
                }
                time = createdAt.time
            }

            return RadarEvent(
                id, createdAt, actualCreatedAt, live, type, geofence, place, region,
                alternatePlaces, verifiedPlace, verification, confidence, duration, location
            )
        }

        @JvmStatic
        fun fromJson(arr: JSONArray?): Array<RadarEvent>? {
            if (arr == null) {
                return null
            }

            return Array(arr.length()) { index ->
                fromJson(arr.optJSONObject(index))
            }.filterNotNull().toTypedArray()
        }

        @JvmStatic
        fun toJson(events: Array<RadarEvent> ?): JSONArray? {
            if (events == null) {
                return null
            }

            val arr = JSONArray()
            events.forEach { event ->
                arr.put(event.toJson())
            }
            return arr
        }

        @JvmStatic
        fun stringForType(type: RadarEventType): String? {
            return when (type) {
                USER_ENTERED_GEOFENCE -> "user.entered_geofence"
                USER_EXITED_GEOFENCE -> "user.exited_geofence"
                USER_ENTERED_HOME -> "user.entered_home"
                USER_EXITED_HOME -> "user.exited_home"
                USER_ENTERED_OFFICE -> "user.entered_office"
                USER_EXITED_OFFICE -> "user.exited_office"
                USER_STARTED_TRAVELING -> "user.started_traveling"
                USER_STOPPED_TRAVELING -> "user.stopped_traveling"
                USER_ENTERED_PLACE -> "user.entered_place"
                USER_EXITED_PLACE -> "user.exited_place"
                USER_NEARBY_PLACE_CHAIN -> "user.nearby_place_chain"
                USER_ENTERED_REGION_COUNTRY -> "user.entered_region_country"
                USER_EXITED_REGION_COUNTRY -> "user.exited_region_country"
                USER_ENTERED_REGION_STATE -> "user.entered_region_state"
                USER_EXITED_REGION_STATE -> "user.exited_region_state"
                USER_ENTERED_REGION_DMA -> "user.entered_region_dma"
                USER_EXITED_REGION_DMA -> "user.exited_region_dma"
                USER_STARTED_COMMUTING -> "user.started_commuting"
                USER_STOPPED_COMMUTING -> "user.stopped_commuting"
                USER_STARTED_TRIP -> "user.started_trip"
                USER_UPDATED_TRIP -> "user.updated_trip"
                USER_APPROACHING_TRIP_DESTINATION -> "user.approaching_trip_destination"
                USER_ARRIVED_AT_TRIP_DESTINATION -> "user.arrived_at_trip_destination"
                USER_STOPPED_TRIP -> "user.stopped_trip"
                else -> null
            }
        }
    }

    fun toJson(): JSONObject {
        val obj = JSONObject()
        obj.putOpt(FIELD_ID, this._id)
        obj.putOpt(FIELD_LIVE, this.live)
        obj.putOpt(FIELD_TYPE, stringForType(this.type))
        obj.putOpt(FIELD_GEOFENCE, this.geofence?.toJson())
        obj.putOpt(FIELD_PLACE, this.place?.toJson())
        obj.putOpt(FIELD_CONFIDENCE, this.confidence)
        obj.putOpt(FIELD_DURATION, this.duration)
        obj.putOpt(FIELD_ALTERNATE_PLACES, RadarPlace.toJson(this.alternatePlaces))
        obj.putOpt(FIELD_REGION, this.region?.toJson())
        val locationObj = JSONObject()
        locationObj.putOpt("type", "Point")
        val coordinatesArr = JSONArray()
        coordinatesArr.put(this.location.longitude)
        coordinatesArr.put(this.location.latitude)
        locationObj.putOpt("coordinates", coordinatesArr)
        obj.putOpt(FIELD_LOCATION, locationObj)
        return obj
    }

}
