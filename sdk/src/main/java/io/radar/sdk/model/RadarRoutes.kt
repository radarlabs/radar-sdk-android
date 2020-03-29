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
    val geodesic: RadarRoute?,

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

        fun fromJson(obj: JSONObject?): RadarRoutes? {
            if (obj == null) {
                return null
            }

            val geodesic = RadarRoute.fromJson(obj.optJSONObject(FIELD_GEODESIC))
            val foot = RadarRoute.fromJson(obj.optJSONObject(FIELD_FOOT))
            val bike = RadarRoute.fromJson(obj.optJSONObject(FIELD_BIKE))
            val car = RadarRoute.fromJson(obj.optJSONObject(FIELD_CAR))
            val transit = RadarRoute.fromJson(obj.optJSONObject(FIELD_TRANSIT))

            return RadarRoutes(geodesic, foot, bike, car, transit)
        }
    }

    fun toJson(): JSONObject {
        val obj = JSONObject()
        obj.putOpt(FIELD_GEODESIC, this.geodesic?.toJson())
        obj.putOpt(FIELD_FOOT, this.foot?.toJson())
        obj.putOpt(FIELD_BIKE, this.bike?.toJson())
        obj.putOpt(FIELD_CAR, this.car?.toJson())
        obj.putOpt(FIELD_TRANSIT, this.transit?.toJson())
        return obj
    }

}
