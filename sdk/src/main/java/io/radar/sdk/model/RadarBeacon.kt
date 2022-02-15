package io.radar.sdk.model

import org.json.JSONArray
import org.json.JSONObject

/**
 * Represents a Bluetooth beacon.
 *
 * @see [](https://radar.io/documentation/beacons)
 */
@Suppress("LongParameterList")
class RadarBeacon(
    /**
     * The Radar ID of the point.
     */
    @Suppress("ConstructorParameterNaming")
    val _id: String,

    /**
     * The description of the beacon.
     */
    val description: String,

    /**
     * The tag of the beacon.
     */
    val tag: String?,

    /**
     * The externalId of the beacon.
     */
    val externalId: String?,

    /**
     * The UUID of the beacon.
     */
    val uuid: String,

    /**
     * The major ID of the beacon.
     */
    val major: String,

    /**
     * The minor ID of the beacon.
     */
    val minor: String,

    /**
     * The optional set of custom key-value pairs for the beacon.
     */
    val metadata: JSONObject?,

    /**
     * The location of the beacon.
     */
    val location: RadarCoordinate
) {
    internal companion object {
        private const val FIELD_ID = "_id"
        private const val FIELD_DESCRIPTION = "description"
        private const val FIELD_TAG = "tag"
        private const val FIELD_EXTERNAL_ID = "externalId"
        private const val FIELD_UUID = "uuid"
        private const val FIELD_MAJOR = "major"
        private const val FIELD_MINOR = "minor"
        private const val FIELD_METADATA = "metadata"
        private const val FIELD_GEOMETRY = "geometry"
        private const val FIELD_COORDINATES = "coordinates"

        @JvmStatic
        fun fromJson(obj: JSONObject?): RadarBeacon? {
            if (obj == null) {
                return null
            }

            val id: String = obj.optString(FIELD_ID) ?: ""
            val description: String = obj.optString(FIELD_DESCRIPTION) ?: ""
            val tag = obj.optString(FIELD_TAG) ?: null
            val externalId = obj.optString(FIELD_EXTERNAL_ID) ?: null
            val uuid: String = obj.optString(FIELD_UUID) ?: ""
            val major: String = obj.optString(FIELD_MAJOR) ?: ""
            val minor: String = obj.optString(FIELD_MINOR) ?: ""
            val metadata: JSONObject? = obj.optJSONObject(FIELD_METADATA) ?: null
            val geometryObj = obj.optJSONObject(FIELD_GEOMETRY)
            val geometryCoordinatesObj = geometryObj?.optJSONArray(FIELD_COORDINATES)
            val geometry = RadarCoordinate(
                geometryCoordinatesObj?.optDouble(1) ?: 0.0,
                geometryCoordinatesObj?.optDouble(0) ?: 0.0
            )

            return RadarBeacon(id, description, tag, externalId, uuid, major, minor, metadata, geometry)
        }

        @JvmStatic
        fun fromJson(arr: JSONArray?): Array<RadarBeacon>? {
            if (arr == null) {
                return null
            }

            return Array(arr.length()) { index ->
                fromJson(arr.optJSONObject(index))
            }.filterNotNull().toTypedArray()
        }

        @JvmStatic
        fun toJson(beacons: Array<RadarBeacon>?): JSONArray? {
            if (beacons == null) {
                return null
            }

            val arr = JSONArray()
            beacons.forEach { beacon ->
                arr.put(beacon.toJson())
            }
            return arr
        }
    }

    fun toJson(): JSONObject {
        val obj = JSONObject()
        obj.putOpt(FIELD_ID, this._id)
        obj.putOpt(FIELD_UUID, this.uuid)
        obj.putOpt(FIELD_MAJOR, this.major)
        obj.putOpt(FIELD_MINOR, this.minor)
        obj.putOpt(FIELD_METADATA, this.metadata)
        return obj
    }

}
