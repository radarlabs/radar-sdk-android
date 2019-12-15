package io.radar.sdk.model

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * Represents a geofence. For more information about Geofences, see [](https://radar.io/documentation/geofences).
 *
 * @see [](https://radar.io/documentation/geofences)
 */
class RadarGeofence(
    /**
     * The Radar ID of the geofence.
     */
    val id: String,

    /**
     * The description of the geofence.
     */
    val description: String,

    /**
     * The tag of the geofence.
     */
    val tag: String?,

    /**
     * The external ID of the geofence.
     */
    val externalId: String?,

    /**
     * The optional set of custom key-value pairs for the geofence.
     */
    val metadata: JSONObject?,

    /**
     * The geometry of the geofence.
     */
    val geometry: RadarGeofenceGeometry
) {

    internal companion object {
        internal const val FIELD_ID = "_id"
        internal const val FIELD_DESCRIPTION = "description"
        internal const val FIELD_TAG = "tag"
        internal const val FIELD_EXTERNAL_ID = "externalId"
        internal const val FIELD_METADATA = "metadata"
        internal const val FIELD_TYPE = "type"
        internal const val FIELD_GEOMETRY_RADIUS = "geometryRadius"
        internal const val FIELD_GEOMETRY_CENTER = "geometryCenter"
        internal const val FIELD_GEOMETRY_POLYGON = "geometry"
        internal const val FIELD_COORDINATES = "coordinates"

        internal const val TYPE_CIRCLE = "circle"
        internal const val TYPE_POLYGON = "polygon"

        @JvmStatic
        fun fromJson(obj: JSONObject?): RadarGeofence? {
            if (obj == null) {
                return null
            }

            val id = obj.optString(FIELD_ID)
            val description = obj.optString(FIELD_DESCRIPTION)
            val tag: String? = obj.optString(FIELD_TAG)
            val externalId: String? = obj.optString(FIELD_EXTERNAL_ID)
            val metadata: JSONObject? = obj.optJSONObject(FIELD_METADATA)

            val type = obj.optString(FIELD_TYPE)

            val geometry = when (type) {
                TYPE_CIRCLE -> {
                    val locationObj = obj.optJSONObject(FIELD_GEOMETRY_CENTER)
                    locationObj?.optJSONArray(FIELD_COORDINATES)?.let { coordinates ->
                        RadarCircleGeometry(
                            RadarCoordinate(coordinates.optDouble(1), coordinates.optDouble(0)),
                            obj.optDouble(FIELD_GEOMETRY_RADIUS)
                        )
                    }
                }
                TYPE_POLYGON -> {
                    val locationObj = obj.optJSONObject(FIELD_GEOMETRY_POLYGON)
                    val coords = locationObj?.optJSONArray(FIELD_COORDINATES)
                    coords?.optJSONArray(0)?.let { array ->
                        val vertices = Array(array.length()) { index ->
                            array.optJSONArray(index)?.let { coord ->
                                RadarCoordinate(coord.optDouble(1), coord.optDouble(0))
                            } ?: RadarCoordinate(0.0, 0.0)
                        }
                        RadarPolygonGeometry(vertices)
                    }
                }
                else -> null
            } ?: RadarCircleGeometry(RadarCoordinate(0.0, 0.0), 0.0)

            return RadarGeofence(id, description, tag, externalId, metadata, geometry)
        }

        @Throws(JSONException::class)
        fun fromJSONArray(array: JSONArray): Array<RadarGeofence> {
            return Array(array.length()) { index ->
                fromJson(array.optJSONObject(index))
            }.filterNotNull().toTypedArray()
        }
    }

}
