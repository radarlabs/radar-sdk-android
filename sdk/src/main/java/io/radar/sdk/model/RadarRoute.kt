package io.radar.sdk.model

import org.json.JSONObject

/**
 * Represents a route between an origin and a destination.
 *
 * @see [](https://radar.com/documentation/api#routing)
 */
class RadarRoute(
    /**
     * The distance of the route.
     */
    val distance: RadarRouteDistance?,

    /**
     * The duration of the route.
     */
    val duration: RadarRouteDuration?,

    /**
     * The geometry of the route.
     */
    val geometry: RadarRouteGeometry?
) {

    internal companion object {
        private const val FIELD_DISTANCE = "distance"
        private const val FIELD_DURATION = "duration"
        private const val FIELD_GEOMETRY = "geometry"

        @JvmStatic
        fun fromJson(obj: JSONObject?): RadarRoute? {
            if (obj == null) {
                return null
            }

            val distance = RadarRouteDistance.fromJson(obj.optJSONObject(FIELD_DISTANCE))
            val duration = RadarRouteDuration.fromJson(obj.optJSONObject(FIELD_DURATION))
            val geometry = RadarRouteGeometry.fromJson(obj.optJSONObject(FIELD_GEOMETRY))

            return RadarRoute(distance, duration, geometry)
        }
    }

    fun toJson(): JSONObject {
        val obj = JSONObject()
        obj.putOpt(FIELD_DISTANCE, this.distance?.toJson())
        obj.putOpt(FIELD_DURATION, this.duration?.toJson())
        obj.putOpt(FIELD_GEOMETRY, this.geometry?.toJson())
        return obj
    }

}