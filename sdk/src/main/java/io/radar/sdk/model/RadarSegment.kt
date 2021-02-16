package io.radar.sdk.model

import org.json.JSONArray
import org.json.JSONObject

/**
 * Represents a user segment.
 */
class RadarSegment(
    /**
     * The description of the segment.
     */
    val description: String,

    /**
     * The external ID of the segment.
     */
    val externalId: String
) {

    internal companion object {
        private const val FIELD_DESCRIPTION = "description"
        private const val FIELD_EXTERNAL_ID = "externalId"

        @JvmStatic
        fun fromJson(obj: JSONObject?): RadarSegment? {
            if (obj == null) {
                return null
            }

            val description = obj.optString(FIELD_DESCRIPTION) ?: ""
            val externalId = obj.optString(FIELD_EXTERNAL_ID) ?: ""

            return RadarSegment(description, externalId)
        }

        @JvmStatic
        fun fromJson(arr: JSONArray?): Array<RadarSegment>? {
            if (arr == null) {
                return null
            }

            return Array(arr.length()) { index ->
                fromJson(arr.optJSONObject(index))
            }.filterNotNull().toTypedArray()
        }

        @JvmStatic
        fun toJson(segments: Array<RadarSegment>?): JSONArray? {
            if (segments == null) {
                return null
            }

            val arr = JSONArray()
            segments.forEach { segment ->
                arr.put(segment.toJson())
            }
            return arr
        }
    }

    fun toJson(): JSONObject {
        val obj = JSONObject()
        obj.putOpt(FIELD_DESCRIPTION, this.description)
        obj.putOpt(FIELD_EXTERNAL_ID, this.externalId)
        return obj
    }
}
