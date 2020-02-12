package io.radar.sdk.model

import org.json.JSONObject

/**
 * The context for a location.
 *
 * @see [](https://radar.io/documentation/api#context)
 */
class RadarContext(
    /**
     * An array of the geofences for the location. May be empty if the location is not in any geofences. See [](https://radar.io/documentation/geofences).
     */
    val geofences: Array<RadarGeofence>,

    /**
     * The place for the location. May be `null` if the location is not at a place or if Places is not enabled. See [](https://radar.io/documentation/places).
     */
    val place: RadarPlace?,

    /**
     * The country of the location. May be `null` if country is not available or if Regions is not enabled. See [](https://radar.io/documentation/regions).
     */
    val country: RadarRegion?,

    /**
     * The state of the location. May be `null` if state is not available or if Regions is not enabled. See [](https://radar.io/documentation/regions).
     */
    val state: RadarRegion?,

    /**
     * The designated market area (DMA) of the location. May be `null` if DMA is not available or if Regions is not enabled. See [](https://radar.io/documentation/regions).
     */
    val dma: RadarRegion?,

    /**
     * The postal code of the location. May be `null` if postal code is not available or if Regions is not enabled. See [](https://radar.io/documentation/regions).
     */
    val postalCode: RadarRegion?
) {

    internal companion object {
        private const val FIELD_GEOFENCES = "geofences"
        private const val FIELD_PLACE = "place"
        private const val FIELD_COUNTRY = "country"
        private const val FIELD_STATE = "state"
        private const val FIELD_DMA = "dma"
        private const val FIELD_POSTAL_CODE = "postalCode"

        internal fun fromJson(obj: JSONObject): RadarContext {
            val geofences = RadarGeofence.fromJSONArray(obj.getJSONArray(FIELD_GEOFENCES))!!
            val place = obj.optJSONObject(FIELD_PLACE)?.let(RadarPlace.Companion::fromJsonNullable)
            val country = obj.optJSONObject(FIELD_COUNTRY)?.let(RadarRegion.Companion::fromJson)
            val state = obj.optJSONObject(FIELD_STATE)?.let(RadarRegion.Companion::fromJson)
            val dma = obj.optJSONObject(FIELD_DMA)?.let(RadarRegion.Companion::fromJson)
            val postalCode = obj.optJSONObject(FIELD_POSTAL_CODE)?.let(RadarRegion.Companion::fromJson)

            return RadarContext(
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
