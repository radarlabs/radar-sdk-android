package io.radar.sdk.model

import android.location.Location
import org.json.JSONObject

/**
 * Represents the current user state. For more information, see [](https://radar.io/documentation).
 *
 * @see [](https://radar.io/documentation)
 */
class RadarUser(
    /**
     * The Radar ID of the user.
     */
    val _id: String,

    /**
     * The unique ID of the user, provided when you identified the user. May be `null` if the user has not been identified.
     */
    val userId: String?,

    /**
     * The device ID of the user.
     */
    val deviceId: String?,

    /**
     * The optional description of the user.
     */
    val description: String?,

    /**
     * The optional set of custom key-value pairs for the user.
     */
    val metadata: JSONObject?,

    /**
     * The user's last known location.
     */
    val location: Location,

    /**
     * The user's last known geofences. May be `null` or empty if the user is not in any geofences. See [](https://radar.io/documentation/geofences).
     */
    val geofences: Array<RadarGeofence>?,

    /**
     * The user's last known place. May be `null` if the user is not at a place or if Places is not enabled. See [](https://radar.io/documentation/places).
     */
    val place: RadarPlace?,

    /**
     * Learned insights for the user. May be `null` if no insights are available or if Insights is not enabled. See [](https://radar.io/documentation/insights).
     */
    val insights: RadarUserInsights?,

    /**
     * A boolean indicating whether the user is stopped.
     */
    val stopped: Boolean,

    /**
     * A boolean indicating whether the user was last updated in the foreground.
     */
    val foreground: Boolean,

    /**
     * The user's last known country. May be `null` if country is not available or if Regions is not enabled. See [](https://radar.io/documentation/regions).
     */
    val country: RadarRegion?,

    /**
     * The user's last known state. May be `null` if state is not available or if Regions is not enabled. See [](https://radar.io/documentation/regions).
     */
    val state: RadarRegion?,

    /**
     * The user's last known designated market area (DMA). May be `null` if DMA is not available or if Regions is not enabled. See [](https://radar.io/documentation/regions).
     */
    val dma: RadarRegion?,

    /**
     * The user's last known postal code. May be `null` if postal code is not available or if Regions is not enabled. See [](https://radar.io/documentation/regions).
     */
    val postalCode: RadarRegion?,

    /**
     * An array of nearby chains. May be `null` if no chains are nearby or if nearby chains are not enabled.
     */
    val nearbyPlaceChains: Array<RadarChain>?,

    /**
     * The user's segments. May be `null` if segments are not enabled.
     */
    val segments: Array<RadarSegment>?,

    /**
     * The user's top chains. May be `null` if segments are not enabled.
     */
    val topChains: Array<RadarChain>?
) {

    internal companion object {
        internal const val FIELD_ID = "_id"
        internal const val FIELD_USER_ID = "userId"
        internal const val FIELD_DEVICE_ID = "deviceId"
        internal const val FIELD_DESCRIPTION = "description"
        internal const val FIELD_METADATA = "metadata"
        internal const val FIELD_LOCATION = "location"
        internal const val FIELD_COORDINATES = "coordinates"
        internal const val FIELD_LOCATION_ACCURACY = "locationAccuracy"
        internal const val FIELD_GEOFENCES = "geofences"
        internal const val FIELD_PLACE = "place"
        internal const val FIELD_INSIGHTS = "insights"
        internal const val FIELD_STOPPED = "stopped"
        internal const val FIELD_FOREGROUND = "foreground"
        private const val FIELD_COUNTRY = "country"
        private const val FIELD_STATE = "state"
        private const val FIELD_DMA = "dma"
        private const val FIELD_POSTAL_CODE = "postalCode"
        private const val FIELD_NEARBY_PLACE_CHAINS = "nearbyPlaceChains"
        private const val FIELD_SEGMENTS = "segments"
        private const val FIELD_TOP_CHAINS = "topChains"

        internal fun deserialize(obj: JSONObject?): RadarUser? {
            if (obj == null) {
                return null
            }

            val id = obj.optString(FIELD_ID)
            val userId: String? = obj.optString(FIELD_USER_ID)
            val deviceId: String? = obj.optString(FIELD_DEVICE_ID)
            val description: String? = obj.optString(FIELD_DESCRIPTION)
            val metadata: JSONObject? = obj.optJSONObject(FIELD_METADATA)
            val stopped: Boolean = obj.optBoolean(FIELD_STOPPED)
            val foreground: Boolean = obj.optBoolean(FIELD_FOREGROUND)
            val locationObj = obj.optJSONObject(FIELD_LOCATION)
            val locationCoordinatesObj = locationObj?.optJSONArray(FIELD_COORDINATES)
            val location = Location("RadarSDK").apply {
                longitude = locationCoordinatesObj?.optDouble(0) ?: 0.0
                latitude = locationCoordinatesObj?.optDouble(1) ?: 0.0
                if (obj.has(FIELD_LOCATION_ACCURACY)) {
                    accuracy = obj.optDouble(FIELD_LOCATION_ACCURACY).toFloat()
                }
            }
            val geofences = RadarGeofence.deserializeArray(obj.optJSONArray(FIELD_GEOFENCES))
            val place = RadarPlace.deserialize(obj.optJSONObject(FIELD_PLACE))
            val insights = RadarUserInsights.deserialize(obj.optJSONObject(FIELD_INSIGHTS))
            val country = obj.optJSONObject(FIELD_COUNTRY)?.let(RadarRegion.Companion::deserialize)
            val state = obj.optJSONObject(FIELD_STATE)?.let(RadarRegion.Companion::deserialize)
            val dma = obj.optJSONObject(FIELD_DMA)?.let(RadarRegion.Companion::deserialize)
            val postalCode = obj.optJSONObject(FIELD_POSTAL_CODE)?.let(RadarRegion.Companion::deserialize)
            val nearbyPlaceChains = RadarChain.deserializeArray(obj.optJSONArray(FIELD_NEARBY_PLACE_CHAINS))
            val segments = RadarSegment.deserializeArray(obj.optJSONArray(FIELD_SEGMENTS))
            val topChains = RadarChain.deserializeArray(obj.optJSONArray(FIELD_TOP_CHAINS))

            return RadarUser(
                id,
                userId,
                deviceId,
                description,
                metadata,
                location,
                geofences,
                place,
                insights,
                stopped,
                foreground,
                country,
                state,
                dma,
                postalCode,
                nearbyPlaceChains,
                segments,
                topChains
            )
        }

    }

    fun serialize(): JSONObject {
        val obj = JSONObject()
        obj.putOpt(FIELD_ID, this._id)
        obj.putOpt(FIELD_USER_ID, this.userId)
        obj.putOpt(FIELD_DEVICE_ID, this.deviceId)
        obj.putOpt(FIELD_DESCRIPTION, this.description)
        obj.putOpt(FIELD_METADATA, this.metadata)
        obj.putOpt(FIELD_GEOFENCES, RadarGeofence.serializeArray(this.geofences))
        obj.putOpt(FIELD_PLACE, this.place?.serialize())
        obj.putOpt(FIELD_INSIGHTS, this.insights?.serialize())
        obj.putOpt(FIELD_STOPPED, this.stopped)
        obj.putOpt(FIELD_FOREGROUND, this.foreground)
        obj.putOpt(FIELD_COUNTRY, this.country?.serialize())
        obj.putOpt(FIELD_STATE, this.state?.serialize())
        obj.putOpt(FIELD_DMA, this.dma?.serialize())
        obj.putOpt(FIELD_POSTAL_CODE, this.postalCode?.serialize())
        obj.putOpt(FIELD_NEARBY_PLACE_CHAINS, RadarChain.serializeArray(this.nearbyPlaceChains))
        obj.putOpt(FIELD_SEGMENTS, RadarSegment.serializeArray(this.segments))
        obj.putOpt(FIELD_TOP_CHAINS, RadarChain.serializeArray(this.topChains))
        return obj
    }

}
