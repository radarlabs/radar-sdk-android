package io.radar.sdk.model

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * Represents a point. For more information about Points, see [](https://radar.io/documentation/points).
 *
 * @see [](https://radar.io/documentation/points)
 */
class RadarPoint (
    /**
     * The Radar ID of the point.
     */
    val _id: String,

    /**
     * The description of the point.
     */
    val description: String,

    /**
     * The tag of the point.
     */
    val tag: String?,

    /**
     * The external ID of the point.
     */
    val externalId: String?,

    /**
     * The optional set of custom key-value pairs for the point.
     */
    val metadata: JSONObject?,

    /**
     * The location of the point.
     */
    val location: RadarCoordinate
) {
    internal companion object {
        private const val FIELD_ID = "_id"
        private const val FIELD_DESCRIPTION = "description"
        private const val FIELD_TAG = "tag"
        private const val FIELD_EXTERNAL_ID = "externalId"
        private const val FIELD_METADATA = "metadata"
        private const val FIELD_LOCATION = "geometry"
        private const val FIELD_COORDINATES = "coordinates"

        @JvmStatic
        fun fromJson(obj: JSONObject?): RadarPoint? {
            if (obj == null) {
                return null
            }

            val id = obj.optString(FIELD_ID)
            val description = obj.optString(FIELD_DESCRIPTION)
            val tag: String? = obj.optString(FIELD_TAG)
            val externalId: String? = obj.optString(FIELD_EXTERNAL_ID)
            val metadata: JSONObject? = obj.optJSONObject(FIELD_METADATA)
            val locationObj = obj.optJSONObject(FIELD_LOCATION)
            val location = locationObj?.optJSONArray(FIELD_COORDINATES)?.let { coordinate ->
                RadarCoordinate(
                    coordinate.optDouble(1),
                    coordinate.optDouble(0)
                )
            } ?: RadarCoordinate(0.0, 0.0)

            return RadarPoint(id, description, tag, externalId, metadata, location)
        }

        @Throws(JSONException::class)
        fun fromJson(array: JSONArray?): Array<RadarPoint>? {
            if (array == null) {
                return null
            }

            return Array(array.length()) { index ->
                fromJson(array.optJSONObject(index))
            }.filterNotNull().toTypedArray()
        }
    }
}