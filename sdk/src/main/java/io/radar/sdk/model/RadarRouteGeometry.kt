package io.radar.sdk.model

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * Represents the distance of a route.
 */
class RadarRouteGeometry(
    /**
     * The geometry of the route.
     */
    val coordinates: Array<RadarCoordinate>?
) {

    internal companion object {
        private const val FIELD_TYPE = "type"
        private const val FIELD_COORDINATES = "coordinates"

        @JvmStatic
        fun fromJson(obj: JSONObject?): RadarRouteGeometry? {
            if (obj == null) {
                return null
            }

            obj.optJSONArray(FIELD_COORDINATES)?.let { coordinatesArr ->
                val coordinates = Array(coordinatesArr.length()) { index ->
                    coordinatesArr.optJSONArray(index)?.let { coordinate ->
                        RadarCoordinate(
                            coordinate.optDouble(1),
                            coordinate.optDouble(0)
                        )
                    } ?: RadarCoordinate(0.0, 0.0)
                }
                return RadarRouteGeometry(coordinates)
            }

            return null
        }
    }

    fun toJson(): JSONObject {
        val obj = JSONObject()
        obj.putOpt(FIELD_TYPE, "LineString")
        val coordinatesArr = JSONArray()
        this.coordinates?.forEach { coordinate ->
            val coordinateArr = JSONArray()
            coordinateArr.put(coordinate.longitude)
            coordinateArr.put(coordinate.latitude)
            coordinatesArr.put(coordinateArr)
        }
        obj.putOpt(FIELD_COORDINATES, coordinatesArr)
        return obj
    }

}