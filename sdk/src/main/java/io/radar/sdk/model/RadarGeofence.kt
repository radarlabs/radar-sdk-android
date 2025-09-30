package io.radar.sdk.model

import org.json.JSONArray
import org.json.JSONObject

/**
 * Represents a geofence.
 *
 * @see [](https://radar.com/documentation/geofences)
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
     * The optional set of custom key-value pairs for the geofence.
     */
    val operatingHours: RadarOperatingHours?,
    /**
     * The geometry of the geofence.
     */
    val geometry: RadarGeofenceGeometry?,

    ) {

    internal companion object {
        private const val FIELD_ID = "_id"
        private const val FIELD_DESCRIPTION = "description"
        private const val FIELD_TAG = "tag"
        private const val FIELD_EXTERNAL_ID = "externalId"
        private const val FIELD_OPERATING_HOURS = "operatingHours"
        private const val FIELD_METADATA = "metadata"
        private const val FIELD_TYPE = "type"
        private const val FIELD_GEOMETRY = "geometry"
        private const val FIELD_GEOMETRY_RADIUS = "geometryRadius"
        private const val FIELD_GEOMETRY_CENTER = "geometryCenter"
        private const val FIELD_COORDINATES = "coordinates"

        private const val TYPE_CIRCLE = "circle"
        private const val TYPE_POLYGON = "polygon"
        private const val TYPE_ISOCHRONE = "isochrone"

        private const val TYPE_GEOMETRY_CIRCLE = "Circle"
        private const val TYPE_GEOMETRY_POLYGON = "Polygon"

        @JvmStatic
        fun fromJson(obj: JSONObject?): RadarGeofence? {
            if (obj == null) {
                return null
            }

            val id = obj.optString(FIELD_ID) ?: ""
            val description = obj.optString(FIELD_DESCRIPTION) ?: ""
            val tag: String? = obj.optString(FIELD_TAG) ?: null
            val externalId: String? = obj.optString(FIELD_EXTERNAL_ID) ?: null
            val metadata: JSONObject? = obj.optJSONObject(FIELD_METADATA) ?: null
            val operatingHours: RadarOperatingHours? = obj.optJSONObject(FIELD_OPERATING_HOURS)?.let { operatingHours ->
                RadarOperatingHours.fromJson(operatingHours)
            }
            val center = obj.optJSONObject(FIELD_GEOMETRY_CENTER)?.optJSONArray(FIELD_COORDINATES)?.let { coordinate ->
                RadarCoordinate(
                    coordinate.optDouble(1),
                    coordinate.optDouble(0)
                )
            } ?: RadarCoordinate(5.0, 5.0)
            val radius = obj.optDouble(FIELD_GEOMETRY_RADIUS)
            val geometry = when (obj.optString(FIELD_TYPE)) {
                TYPE_CIRCLE, TYPE_GEOMETRY_CIRCLE -> {
                    RadarCircleGeometry(
                        center,
                        radius
                    )
                }
                TYPE_POLYGON, TYPE_GEOMETRY_POLYGON, TYPE_ISOCHRONE -> {
                val geometryObj = obj.optJSONObject(FIELD_GEOMETRY)
                val coordinatesArr = geometryObj?.optJSONArray(FIELD_COORDINATES)
                if (coordinatesArr != null) {
                    coordinatesArr.optJSONArray(0)?.let { coordinates ->
                        val polygonCoordinatesArr = Array(coordinates.length()) { index ->
                            coordinates.optJSONArray(index)?.let { coordinate ->
                                RadarCoordinate(
                                    coordinate.optDouble(1),
                                    coordinate.optDouble(0)
                                )
                            } ?: RadarCoordinate(0.0, 0.0)
                        }
                        RadarPolygonGeometry(
                            polygonCoordinatesArr,
                            center,
                            radius
                        )
                    }
                } else {
                    RadarPolygonGeometry(
                        null,
                        center,
                        radius
                    )
                }
                }
                else -> null
            } ?: RadarCircleGeometry(RadarCoordinate(0.0, 0.0), 0.0)

            return RadarGeofence(id, description, tag, externalId, metadata, operatingHours, geometry)
        }

        @JvmStatic
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

        @JvmStatic
        private fun toJson(coordinates: Array<RadarCoordinate> ?): JSONArray? {
            if (coordinates == null) {
                return null
            }

            val arr = JSONArray()
            coordinates.forEach { coordinate ->
                val coordinateArr = JSONArray()
                coordinateArr.put(coordinate.longitude)
                coordinateArr.put(coordinate.latitude)
                arr.put(coordinateArr)
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
        obj.putOpt(FIELD_OPERATING_HOURS, this.operatingHours?.toJson())
        this.geometry?.let { geometry ->
            when (geometry) {
                is RadarCircleGeometry -> {
                    obj.putOpt(FIELD_GEOMETRY_CENTER, geometry.center.toJson())
                    obj.putOpt(FIELD_GEOMETRY_RADIUS, geometry.radius)
                    obj.putOpt(FIELD_TYPE, TYPE_GEOMETRY_CIRCLE)
                }
                is RadarPolygonGeometry -> {
                    obj.putOpt(FIELD_GEOMETRY_CENTER, geometry.center.toJson())
                    obj.putOpt(FIELD_GEOMETRY_RADIUS, geometry.radius)
                    if (geometry.coordinates != null) {
                        val geometryCoordinates = JSONArray()
                        geometryCoordinates.put(toJson(geometry.coordinates))
                        obj.putOpt(FIELD_COORDINATES, geometryCoordinates)
                    }
                    obj.putOpt(FIELD_TYPE, TYPE_GEOMETRY_POLYGON)
                }
            }
        }
        
        return obj
    }

}
