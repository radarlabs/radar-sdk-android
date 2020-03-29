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
    val _id: String,

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
    val geometry: RadarGeofenceGeometry?
) {

    internal companion object {
        private const val FIELD_ID = "_id"
        private const val FIELD_DESCRIPTION = "description"
        private const val FIELD_TAG = "tag"
        private const val FIELD_EXTERNAL_ID = "externalId"
        private const val FIELD_METADATA = "metadata"
        private const val FIELD_TYPE = "type"
        private const val FIELD_GEOMETRY_RADIUS = "geometryRadius"
        private const val FIELD_GEOMETRY_CENTER = "geometryCenter"
        private const val FIELD_GEOMETRY_POLYGON = "geometry"
        private const val FIELD_COORDINATES = "coordinates"

        private const val TYPE_CIRCLE = "circle"
        private const val TYPE_POLYGON = "polygon"

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
            val geometry = when (obj.optString(FIELD_TYPE)) {
                TYPE_CIRCLE -> {
                    val centerObj = obj.optJSONObject(FIELD_GEOMETRY_CENTER)
                    centerObj?.optJSONArray(FIELD_COORDINATES)?.let { coordinate ->
                        RadarCircleGeometry(
                            RadarCoordinate(
                                coordinate.optDouble(1),
                                coordinate.optDouble(0)),
                            obj.optDouble(FIELD_GEOMETRY_RADIUS)
                        )
                    }
                }
                TYPE_POLYGON -> {
                    val polygonObj = obj.optJSONObject(FIELD_GEOMETRY_POLYGON)
                    val coordinatesArr = polygonObj?.optJSONArray(FIELD_COORDINATES)
                    coordinatesArr?.optJSONArray(0)?.let { coordinates ->
                        val polygonCoordinatesArr = Array(coordinates.length()) { index ->
                            coordinates.optJSONArray(index)?.let { coordinate ->
                                RadarCoordinate(
                                    coordinate.optDouble(1),
                                    coordinate.optDouble(0)
                                )
                            } ?: RadarCoordinate(0.0, 0.0)
                        }
                        RadarPolygonGeometry(polygonCoordinatesArr)
                    }
                }
                else -> null
            } ?: RadarCircleGeometry(RadarCoordinate(0.0, 0.0), 0.0)

            return RadarGeofence(id, description, tag, externalId, metadata, geometry)
        }

        @JvmStatic
        @Throws(JSONException::class)
        fun fromJson(arr: JSONArray?): Array<RadarGeofence>? {
            if (arr == null) {
                return null
            }

            return Array(arr.length()) { index ->
                fromJson(arr.optJSONObject(index))
            }.filterNotNull().toTypedArray()
        }

        @JvmStatic
        fun toJson(geofences: Array<RadarGeofence> ?): JSONArray? {
            if (geofences == null) {
                return null
            }

            val arr = JSONArray()
            geofences.forEach { geofence ->
                arr.put(geofence.toJson())
            }
            return arr
        }
    }

    fun toJson(): JSONObject {
        val obj = JSONObject()
        obj.putOpt(FIELD_ID, this._id)
        obj.putOpt(FIELD_TAG, this.tag)
        obj.putOpt(FIELD_EXTERNAL_ID, this.externalId)
        obj.putOpt(FIELD_DESCRIPTION, this.description)
        obj.putOpt(FIELD_METADATA, this.metadata)
        return obj
    }

}
