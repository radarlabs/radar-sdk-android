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

    fun serialize(): JSONObject {
        val obj = JSONObject()
        obj.putOpt("type", "Point")
        val coordinatesObj = JSONArray()
        coordinatesObj.put(this.longitude)
        coordinatesObj.put(this.latitude)
        obj.putOpt("coordinates", coordinatesObj)
        return obj
    }

}