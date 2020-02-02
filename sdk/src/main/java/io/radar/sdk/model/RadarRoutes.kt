package io.radar.sdk.model

import org.json.JSONObject

/**
 * Represents routes from an origin to a destination. For more information, see [](https://radar.io/documentation/api#route).
 *
 * @see [](https://radar.io/documentation/api#route
 */
class RadarRoutes(
    /**
     * The geodesic distance between the origin and destination.
     */
    val geodesic: RadarRouteDistance?,

    /**
     * The route by foot between the origin and destination. May be `null` if mode not specified or route unavailable.
     */
    val foot: RadarRoute?,

    /**
     * The route by bike between the origin and destination. May be `null` if mode not specified or route unavailable.
     */
    val bike: RadarRoute?,

    /**
     * The route by car between the origin and destination. May be `null` if mode not specified or route unavailable.
     */
    val car: RadarRoute?,

    /**
     * The route by transit between the origin and destination. May be `null` if mode not specified or route unavailable.
     */
    val transit: RadarRoute?
) {

    internal companion object {
        private const val FIELD_GEODESIC = "geodesic"
        private const val FIELD_FOOT = "foot"
        private const val FIELD_BIKE = "bike"
        private const val FIELD_CAR = "car"
        private const val FIELD_TRANSIT = "transit"

        fun fromJson(obj: JSONObject): RadarRoutes {
            val geodesic = obj.optJSONObject(FIELD_GEODESIC)?.let(RadarRouteDistance.Companion::fromJson)
            val foot = obj.optJSONObject(FIELD_FOOT)?.let(RadarRoute.Companion::fromJson)
            val bike = obj.optJSONObject(FIELD_BIKE)?.let(RadarRoute.Companion::fromJson)
            val car = obj.optJSONObject(FIELD_CAR)?.let(RadarRoute.Companion::fromJson)
            val transit = obj.optJSONObject(FIELD_TRANSIT)?.let(RadarRoute.Companion::fromJson)

            return RadarRoutes(geodesic, foot, bike, car, transit)
        }
    }
}
