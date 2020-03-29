package io.radar.sdk.model

import org.json.JSONObject

/**
 * Represents a route between an origin and a destination.
 */
class RadarRoute(
    /**
     * The distance of the route.
     */
    val distance: RadarRouteDistance?,

    /**
     * The duration of the route.
     */
    val duration: RadarRouteDuration?
) {

    internal companion object {
        private const val FIELD_DISTANCE = "distance"
        private const val FIELD_DURATION = "duration"

        fun fromJson(obj: JSONObject?): RadarRoute? {
            if (obj == null) {
                return null
            }

            val distance = RadarRouteDistance.fromJson(obj.optJSONObject(FIELD_DISTANCE))
            val duration = RadarRouteDuration.fromJson(obj.optJSONObject(FIELD_DURATION))

            return RadarRoute(distance, duration)
        }
    }

    fun toJson(): JSONObject {
        val obj = JSONObject()
        obj.putOpt(FIELD_DISTANCE, this.distance?.toJson())
        obj.putOpt(FIELD_DURATION, this.duration?.toJson())
        return obj
    }

}