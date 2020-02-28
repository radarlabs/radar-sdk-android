package io.radar.sdk.model

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * Represents a point. For more information about points, see [] (TODO)
 * */
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
     * The optional tag of the point.
     */
    val tag: String?,
    /**
     * The optional external ID of the point.
     */
    val externalId: String?,
    /**
     * The optional dictionary for additional metadata.
     */
    val metadata: JSONObject?,
    /**
     * The coordinate of the point
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
        fun deserialize(obj: JSONObject?): RadarPoint? {
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
        fun deserializeArray(array: JSONArray?): Array<RadarPoint>? {
            if (array == null) {
                return null
            }

            return Array(array.length()) { index ->
                deserialize(array.optJSONObject(index))
            }.filterNotNull().toTypedArray()
        }
    }
}