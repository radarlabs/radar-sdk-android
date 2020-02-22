package io.radar.sdk.model

import org.json.JSONObject

/**
 * Represents the duration of a route.
 */
class RadarRouteDuration(
    /**
     * The duration in minutes.
     */
    val value: Double,

    /**
     * A display string for the duration.
     */
    val text: String
) {

    internal companion object {
        private const val FIELD_VALUE = "value"
        private const val FIELD_TEXT = "text"

        fun deserialize(obj: JSONObject?): RadarRouteDuration? {
            if (obj == null) {
                return null
            }

            val value = obj.optDouble(FIELD_VALUE)
            val text = obj.optString(FIELD_TEXT)

            return RadarRouteDuration(value, text)
        }
    }

    fun serialize(): JSONObject {
        val obj = JSONObject()
        obj.putOpt(FIELD_VALUE, this.value)
        obj.putOpt(FIELD_TEXT, this.value)
        return obj
    }

}