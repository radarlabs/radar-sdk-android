package io.radar.sdk.model

import org.json.JSONObject

/**
 * TODO(coryp): RadarContext description and documentation reference
 */
class RadarContext(
    /**
     * A Boolean indicating whether the location context was generated with your live API key.
     */
    val live: Boolean,

    /**
     * An array of the geofences the location is in. May be empty if the location is not in any geofences. See [](https://radar.io/documentation/geofences).
     */
    val geofences: Array<RadarGeofence>,

    /**
     * The place a location is at. May be `null` if the location is not at a place, or if Places is not enabled. See [](https://radar.io/documentation/places).
     */
    val place: RadarPlace?,

    /**
     * The location's country. May be `null` if country is not available or if Regions is not enabled. See [](https://radar.io/documentation/regions).
     */
    val country: RadarRegion?,

    /**
     * The location's state. May be `null` if state is not available or if Regions is not enabled. See [](https://radar.io/documentation/regions).
     */
    val state: RadarRegion?,

    /**
     * The location's designated market area (DMA). May be `null` if DMA is not available or if Regions is not enabled. See [](https://radar.io/documentation/regions).
     */
    val dma: RadarRegion?,

    /**
     * The location's postal code. May be `null` if postal code is not available or if Regions is not enabled. See [](https://radar.io/documentation/regions).
     */
    val postalCode: RadarRegion?
) {

    internal companion object {
        private const val FIELD_LIVE = "live"
        private const val FIELD_GEOFENCES = "geofences"
        private const val FIELD_PLACE = "place"
        private const val FIELD_COUNTRY = "country"
        private const val FIELD_STATE = "state"
        private const val FIELD_DMA = "dma"
        private const val FIELD_POSTAL_CODE = "postalCode"

        internal fun fromJson(obj: JSONObject): RadarContext {
            val live = obj.optBoolean(FIELD_LIVE)
            val geofences = RadarGeofence.fromJSONArray(obj.getJSONArray(FIELD_GEOFENCES))!!
            val place = obj.optJSONObject(FIELD_PLACE)?.let(RadarPlace.Companion::fromJsonNullable)
            val country = obj.optJSONObject(FIELD_COUNTRY)?.let(RadarRegion.Companion::fromJson)
            val state = obj.optJSONObject(FIELD_STATE)?.let(RadarRegion.Companion::fromJson)
            val dma = obj.optJSONObject(FIELD_DMA)?.let(RadarRegion.Companion::fromJson)
            val postalCode = obj.optJSONObject(FIELD_POSTAL_CODE)?.let(RadarRegion.Companion::fromJson)

            return RadarContext(
                live,
                geofences,
                place,
                country,
                state,
                dma,
                postalCode
            )
        }

    }

}
