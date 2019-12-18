package io.radar.sdk.model

import org.json.JSONArray
import org.json.JSONObject

/**
 * Represents a region. For more information about Regions, see [](https://radar.io/documentation/regions).
 *
 * @see [](https://radar.io/documentation/regions
 */
class RadarRegion(
    /**
     * The Radar ID of the region.
     */
    val id: String,

    /**
     * The name of the region.
     */
    val name: String,

    /**
     * The unique code for the region.
     */
    val code: String,

    /**
     * The type of the region.
     */
    val type: String
) {

    internal companion object {
        private const val FIELD_ID = "_id"
        private const val FIELD_TYPE = "type"
        private const val FIELD_NAME = "name"
        private const val FIELD_CODE = "code"

        fun fromJson(obj: JSONObject): RadarRegion {
            val id = obj.optString(FIELD_ID)
            val name = obj.optString(FIELD_NAME)
            val code = obj.optString(FIELD_CODE)
            val type = obj.optString(FIELD_TYPE)

            return RadarRegion(id, name, code, type)
        }

        fun fromJSONArray(array: JSONArray): Array<RadarRegion> {
            return Array(array.length()) { index ->
                fromJson(array.optJSONObject(index))
            }.filterNotNull().toTypedArray()
        }
    }
}
