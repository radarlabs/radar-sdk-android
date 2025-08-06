package io.radar.sdk.model

import android.location.Location
import io.radar.sdk.RadarUtils
import io.radar.sdk.model.RadarEvent.RadarEventType.CONVERSION
import io.radar.sdk.model.RadarEvent.RadarEventType.USER_APPROACHING_TRIP_DESTINATION
import io.radar.sdk.model.RadarEvent.RadarEventType.USER_ARRIVED_AT_TRIP_DESTINATION
import io.radar.sdk.model.RadarEvent.RadarEventType.USER_ARRIVED_AT_WRONG_TRIP_DESTINATION
import io.radar.sdk.model.RadarEvent.RadarEventType.USER_DWELLED_IN_GEOFENCE
import io.radar.sdk.model.RadarEvent.RadarEventType.USER_ENTERED_BEACON
import io.radar.sdk.model.RadarEvent.RadarEventType.USER_ENTERED_GEOFENCE
import io.radar.sdk.model.RadarEvent.RadarEventType.USER_ENTERED_PLACE
import io.radar.sdk.model.RadarEvent.RadarEventType.USER_ENTERED_REGION_COUNTRY
import io.radar.sdk.model.RadarEvent.RadarEventType.USER_ENTERED_REGION_DMA
import io.radar.sdk.model.RadarEvent.RadarEventType.USER_ENTERED_REGION_POSTAL_CODE
import io.radar.sdk.model.RadarEvent.RadarEventType.USER_ENTERED_REGION_STATE
import io.radar.sdk.model.RadarEvent.RadarEventType.USER_EXITED_BEACON
import io.radar.sdk.model.RadarEvent.RadarEventType.USER_EXITED_GEOFENCE
import io.radar.sdk.model.RadarEvent.RadarEventType.USER_EXITED_PLACE
import io.radar.sdk.model.RadarEvent.RadarEventType.USER_EXITED_REGION_COUNTRY
import io.radar.sdk.model.RadarEvent.RadarEventType.USER_EXITED_REGION_DMA
import io.radar.sdk.model.RadarEvent.RadarEventType.USER_EXITED_REGION_POSTAL_CODE
import io.radar.sdk.model.RadarEvent.RadarEventType.USER_EXITED_REGION_STATE
import io.radar.sdk.model.RadarEvent.RadarEventType.USER_FAILED_FRAUD
import io.radar.sdk.model.RadarEvent.RadarEventType.USER_NEARBY_PLACE_CHAIN
import io.radar.sdk.model.RadarEvent.RadarEventType.USER_STARTED_TRIP
import io.radar.sdk.model.RadarEvent.RadarEventType.USER_STOPPED_TRIP
import io.radar.sdk.model.RadarEvent.RadarEventType.USER_UPDATED_TRIP
import org.json.JSONArray
import org.json.JSONObject
import java.util.Date

/**
 * Represents a change in user state.
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
     * The name of the conversion event. This will only be set if the `type` is `CONVERSION`.
     */
    val conversionName: String?,

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
     * The beacon for which the event was generated. May be `null` for non-beacon events.
     */
    val beacon: RadarBeacon?,

    /**
     * The trip for which the event was generated. May be `null` for non-trip events.
     */
    val trip: RadarTrip?,

    /**
     * The fraud checks for which the event was generated. May be `null` for non-fraud events.
     */
    val fraud: RadarFraud?,

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
    val location: Location,

    /**
     * Whether the event was from a replayed location.
     */
    val replayed: Boolean,

    /**
     * The metadata of the event. Present on conversions only.
     */
    val metadata: JSONObject?
) {

    /**
     * The types for events.
     */
    enum class RadarEventType {
        /** Unknown */
        UNKNOWN,
        /** A conversion event, logged with Radar.logConversion() */
        CONVERSION,
        /** `user.entered_geofence` */
        USER_ENTERED_GEOFENCE,
        /** `user.exited_geofence` */
        USER_EXITED_GEOFENCE,
        /** `user.dwelled_in_geofence` */
        USER_DWELLED_IN_GEOFENCE,
        /** `user.entered_place` */
        USER_ENTERED_PLACE,
        /** `user.exited_place` */
        USER_EXITED_PLACE,
        /** `user.entered_region_country` */
        USER_ENTERED_REGION_COUNTRY,
        /** `user.exited_region_country` */
        USER_EXITED_REGION_COUNTRY,
        /** `user.entered_region_dma` */
        USER_ENTERED_REGION_DMA,
        /** `user.exited_region_dma` */
        USER_EXITED_REGION_DMA,
        /** `user.entered_region_state` */
        USER_ENTERED_REGION_STATE,
        /** `user.exited_region_state` */
        USER_EXITED_REGION_STATE,
        /** `user.entered_region_postal_code` */
        USER_ENTERED_REGION_POSTAL_CODE,
        /** `user.exited_region_postal_code` */
        USER_EXITED_REGION_POSTAL_CODE,
        /** `user.nearby_place_chain` */
        USER_NEARBY_PLACE_CHAIN,
        /** `user.entered_beacon` */
        USER_ENTERED_BEACON,
        /** `user.exited_beacon` */
        USER_EXITED_BEACON,
        /** `user.started_trip` */
        USER_STARTED_TRIP,
        /** `user.updated_trip` */
        USER_UPDATED_TRIP,
        /** `user.stopped_trip` */
        USER_STOPPED_TRIP,
        /** `user.approaching_trip_destination` */
        USER_APPROACHING_TRIP_DESTINATION,
        /** `user.arrived_at_trip_destination` */
        USER_ARRIVED_AT_TRIP_DESTINATION,
        /** user.arrived_at_wrong_trip_destination */
        USER_ARRIVED_AT_WRONG_TRIP_DESTINATION,
        /** `user.failed_fraud` */
        USER_FAILED_FRAUD,
    }

    /**
     * The confidence levels for events.
     */
    enum class RadarEventConfidence(val value: Int)  {
        /** Unknown confidence */
        NONE(0),
        /** Low confidence */
        LOW(1),
        /** Medium confidence */
        MEDIUM(2),
        /** High confidence */
        HIGH(3)
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
        private const val FIELD_CREATED_AT = "createdAt"
        private const val FIELD_ACTUAL_CREATED_AT = "actualCreatedAt"
        private const val FIELD_LIVE = "live"
        private const val FIELD_TYPE = "type"
        private const val FIELD_GEOFENCE = "geofence"
        private const val FIELD_PLACE = "place"
        private const val FIELD_REGION = "region"
        private const val FIELD_BEACON = "beacon"
        private const val FIELD_TRIP = "trip"
        private const val FIELD_FRAUD = "fraud"
        private const val FIELD_ALTERNATE_PLACES = "alternatePlaces"
        private const val FIELD_VERIFIED_PLACE = "verifiedPlace"
        private const val FIELD_VERIFICATION = "verification"
        private const val FIELD_CONFIDENCE = "confidence"
        private const val FIELD_DURATION = "duration"
        private const val FIELD_LOCATION = "location"
        private const val FIELD_COORDINATES = "coordinates"
        private const val FIELD_LOCATION_ACCURACY = "locationAccuracy"
        private const val FIELD_REPLAYED = "replayed"
        private const val FIELD_METADATA = "metadata"

        @JvmStatic
        fun fromJson(obj: JSONObject?): RadarEvent? {
            if (obj == null) {
                return null
            }

            val id = obj.optString(FIELD_ID) ?: ""
            val createdAt = RadarUtils.isoStringToDate(obj.optString(FIELD_CREATED_AT)) ?: Date()
            val actualCreatedAt = RadarUtils.isoStringToDate(obj.optString(FIELD_ACTUAL_CREATED_AT)) ?: Date()
            val live = obj.optBoolean(FIELD_LIVE)
            val type = when (obj.optString(FIELD_TYPE)) {
                "user.entered_geofence" -> USER_ENTERED_GEOFENCE
                "user.exited_geofence" -> USER_EXITED_GEOFENCE
                "user.dwelled_in_geofence" -> USER_DWELLED_IN_GEOFENCE
                "user.entered_place" -> USER_ENTERED_PLACE
                "user.exited_place" -> USER_EXITED_PLACE
                "user.entered_region_country" -> USER_ENTERED_REGION_COUNTRY
                "user.exited_region_country" -> USER_EXITED_REGION_COUNTRY
                "user.entered_region_dma" -> USER_ENTERED_REGION_DMA
                "user.exited_region_dma" -> USER_EXITED_REGION_DMA
                "user.entered_region_state" -> USER_ENTERED_REGION_STATE
                "user.exited_region_state" -> USER_EXITED_REGION_STATE
                "user.entered_region_postal_code" -> USER_ENTERED_REGION_POSTAL_CODE
                "user.exited_region_postal_code" -> USER_EXITED_REGION_POSTAL_CODE
                "user.nearby_place_chain" -> USER_NEARBY_PLACE_CHAIN
                "user.entered_beacon" -> USER_ENTERED_BEACON
                "user.exited_beacon" -> USER_EXITED_BEACON
                "user.started_trip" -> USER_STARTED_TRIP
                "user.updated_trip" -> USER_UPDATED_TRIP
                "user.stopped_trip" -> USER_STOPPED_TRIP
                "user.approaching_trip_destination" -> USER_APPROACHING_TRIP_DESTINATION
                "user.arrived_at_trip_destination" -> USER_ARRIVED_AT_TRIP_DESTINATION
                "user.arrived_at_wrong_trip_destination" -> USER_ARRIVED_AT_WRONG_TRIP_DESTINATION
                "user.failed_fraud" -> USER_FAILED_FRAUD
                else -> CONVERSION
            }
            var conversionName: String? = null

            if (type == CONVERSION) {
                conversionName = obj.optString(FIELD_TYPE)
            }
            val geofence = RadarGeofence.fromJson(obj.optJSONObject(FIELD_GEOFENCE))
            val place = RadarPlace.fromJson(obj.optJSONObject(FIELD_PLACE))
            val region = RadarRegion.fromJson(obj.optJSONObject(FIELD_REGION))
            val beacon = RadarBeacon.fromJson(obj.optJSONObject(FIELD_BEACON))
            val trip = RadarTrip.fromJson(obj.optJSONObject(FIELD_TRIP))
            val fraud = RadarFraud.fromJson(obj.optJSONObject(FIELD_FRAUD))
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

            val replayed = obj.optBoolean(FIELD_REPLAYED)
            
            val metadataStr = obj.optString(FIELD_METADATA)
            val metadataJson = if (!metadataStr.isNullOrEmpty()) {
                try {
                    JSONObject(metadataStr)
                } catch (e: Exception) {
                    null
                }
            } else {
                null
            }

            val event = RadarEvent(
                id, createdAt, actualCreatedAt, live, type, conversionName, geofence, place, region, beacon, trip, fraud,
                alternatePlaces, verifiedPlace, verification, confidence, duration, location, replayed, metadataJson
            )

            return event
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
                USER_DWELLED_IN_GEOFENCE -> "user.dwelled_in_geofence"
                USER_ENTERED_PLACE -> "user.entered_place"
                USER_EXITED_PLACE -> "user.exited_place"
                USER_ENTERED_REGION_COUNTRY -> "user.entered_region_country"
                USER_EXITED_REGION_COUNTRY -> "user.exited_region_country"
                USER_ENTERED_REGION_DMA -> "user.entered_region_dma"
                USER_EXITED_REGION_DMA -> "user.exited_region_dma"
                USER_ENTERED_REGION_STATE -> "user.entered_region_state"
                USER_EXITED_REGION_STATE -> "user.exited_region_state"
                USER_ENTERED_REGION_POSTAL_CODE -> "user.entered_region_postal_code"
                USER_EXITED_REGION_POSTAL_CODE -> "user.exited_region_postal_code"
                USER_NEARBY_PLACE_CHAIN -> "user.nearby_place_chain"
                USER_ENTERED_BEACON -> "user.entered_beacon"
                USER_EXITED_BEACON -> "user.exited_beacon"
                USER_STARTED_TRIP -> "user.started_trip"
                USER_UPDATED_TRIP -> "user.updated_trip"
                USER_STOPPED_TRIP -> "user.stopped_trip"
                USER_APPROACHING_TRIP_DESTINATION -> "user.approaching_trip_destination"
                USER_ARRIVED_AT_TRIP_DESTINATION -> "user.arrived_at_trip_destination"
                USER_ARRIVED_AT_WRONG_TRIP_DESTINATION -> "user.arrived_at_wrong_trip_destination"
                USER_FAILED_FRAUD -> "user.failed_fraud"
                else -> null
            }
        }
    }

    fun toJson(): JSONObject {
        val obj = JSONObject()
        obj.putOpt(FIELD_ID, this._id)
        obj.putOpt(FIELD_CREATED_AT, RadarUtils.dateToISOString(this.createdAt))
        obj.putOpt(FIELD_ACTUAL_CREATED_AT, RadarUtils.dateToISOString(this.actualCreatedAt))
        obj.putOpt(FIELD_LIVE, this.live)
        obj.putOpt(FIELD_TYPE, stringForType(this.type))
        obj.putOpt(FIELD_GEOFENCE, this.geofence?.toJson())
        obj.putOpt(FIELD_PLACE, this.place?.toJson())
        obj.putOpt(FIELD_CONFIDENCE, this.confidence?.value)
        obj.putOpt(FIELD_DURATION, this.duration)
        obj.putOpt(FIELD_REGION, this.region?.toJson())
        obj.putOpt(FIELD_BEACON, this.beacon?.toJson())
        obj.putOpt(FIELD_TRIP, this.trip?.toJson())
        obj.putOpt(FIELD_FRAUD, this.fraud?.toJson())
        obj.putOpt(FIELD_ALTERNATE_PLACES, RadarPlace.toJson(this.alternatePlaces))
        val locationObj = JSONObject()
        locationObj.putOpt("type", "Point")
        val coordinatesArr = JSONArray()
        coordinatesArr.put(this.location.longitude)
        coordinatesArr.put(this.location.latitude)
        locationObj.putOpt("coordinates", coordinatesArr)
        obj.putOpt(FIELD_LOCATION, locationObj)
        obj.putOpt(FIELD_REPLAYED, this.replayed)
        obj.putOpt(FIELD_METADATA, metadata)

        return obj
    }

}
