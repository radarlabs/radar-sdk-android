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

        fun fromJson(obj: JSONObject): RadarRoute {
            val distance = obj.optJSONObject(FIELD_DISTANCE)?.let(RadarRouteDistance.Companion::fromJson)
            val duration = obj.optJSONObject(FIELD_DURATION)?.let(RadarRouteDuration.Companion::fromJson)

            return RadarRoute(distance, duration)
        }
    }
}