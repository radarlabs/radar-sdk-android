package io.radar.sdk.model

import org.json.JSONArray
import org.json.JSONObject

/**
 * Represents a location coordinate.
 */
class RadarCoordinate(
    val latitude: Double,
    val longitude: Double
) {

    internal companion object {
        private const val FIELD_COORDINATES = "coordinates"

        @JvmStatic
        fun fromJson(obj: JSONObject?): RadarCoordinate? {
            if (obj == null) {
                return null
            }

            val coordinatesObj = obj.optJSONArray(FIELD_COORDINATES)
            val longitude = coordinatesObj?.optDouble(0) ?: 0.0
            val latitude = coordinatesObj?.optDouble(1) ?: 0.0

            return RadarCoordinate(latitude, longitude)
        }

        @JvmStatic
        fun fromJson(arr: JSONArray?): Array<RadarCoordinate>? {
            if (arr == null) {
                return null
            }

            return Array(arr.length()) { index ->
                fromJson(arr.optJSONObject(index))
            }.filterNotNull().toTypedArray()
        }
    }

    fun toJson(): JSONObject {
        val obj = JSONObject()
        obj.putOpt("type", "Point")
        val coordinatesObj = JSONArray()
        coordinatesObj.put(this.longitude)
        coordinatesObj.put(this.latitude)
        obj.putOpt("coordinates", coordinatesObj)
        return obj
    }

}