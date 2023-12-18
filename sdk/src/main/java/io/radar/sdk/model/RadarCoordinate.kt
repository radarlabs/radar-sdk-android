package io.radar.sdk.model

import org.json.JSONArray
import org.json.JSONObject
import android.location.Location


/**
 * Represents a location coordinate.
 */
class RadarCoordinate(
    val latitude: Double,
    val longitude: Double
) {

    internal companion object {
        private const val FIELD_TYPE = "type"
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


    private fun toLocation(): Location {
        val location = Location("") // Provider string is not necessary
        location.latitude = this.latitude
        location.longitude = this.longitude
        return location
    }

    fun distanceTo(other: RadarCoordinate): Float {
        val location1 = this.toLocation()
        val location2 = other.toLocation()
        return location1.distanceTo(location2)
    }

    fun toJson(): JSONObject {
        val obj = JSONObject()
        obj.putOpt(FIELD_TYPE, "Point")
        val coordinatesObj = JSONArray()
        coordinatesObj.put(this.longitude)
        coordinatesObj.put(this.latitude)
        obj.putOpt(FIELD_COORDINATES, coordinatesObj)
        return obj
    }

}