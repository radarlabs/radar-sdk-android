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

        fun fromJson(obj: JSONObject): RadarSegment {
            val description = obj.optString(FIELD_DESCRIPTION)
            val externalId = obj.optString(FIELD_EXTERNAL_ID)

            return RadarSegment(description, externalId)
        }

        fun fromJSONArray(array: JSONArray?): Array<RadarSegment>? {
            if (array == null) {
                return null
            }

            return Array(array.length()) { index ->
                fromJson(array.optJSONObject(index))
            }.filterNotNull().toTypedArray()
        }
    }
}
