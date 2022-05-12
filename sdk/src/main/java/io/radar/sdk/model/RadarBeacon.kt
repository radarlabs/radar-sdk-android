package io.radar.sdk.model

import org.json.JSONArray
import org.json.JSONObject

/**
 * Represents a Bluetooth beacon.
 *
 * @see [](https://radar.io/documentation/beacons)
 */
class RadarBeacon (
    /**
     * The Radar ID of the beacon.
     */
    val _id: String? = null,

    /**
     * The description of the beacon.
     */
    val description: String? = null,

    /**
     * The tag of the beacon.
     */
    val tag: String? = null,

    /**
     * The externalId of the beacon.
     */
    val externalId: String? = null,

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
    val metadata: JSONObject? = null,

    /**
     * The RSSI of the beacon.
     */
    val rssi: Int? = null,

    /**
     * The location of the beacon.
     */
    val location: RadarCoordinate? = null
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
        private const val FIELD_RSSI = "rssi"
        private const val FIELD_GEOMETRY = "geometry"
        private const val FIELD_COORDINATES = "coordinates"
        private const val FIELD_TYPE = "type"

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
            val rssi: Int = obj.optInt(FIELD_RSSI)
            val geometryObj = obj.optJSONObject(FIELD_GEOMETRY)
            val geometryCoordinatesObj = geometryObj?.optJSONArray(FIELD_COORDINATES)
            val geometry = RadarCoordinate(
                geometryCoordinatesObj?.optDouble(1) ?: 0.0,
                geometryCoordinatesObj?.optDouble(0) ?: 0.0
            )

            return RadarBeacon(id, description, tag, externalId, uuid, major, minor, metadata, rssi, geometry)
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
        fun toJson(beacons: Array<RadarBeacon> ?): JSONArray? {
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
        obj.putOpt(FIELD_UUID, this.uuid.lowercase())
        obj.putOpt(FIELD_MAJOR, this.major)
        obj.putOpt(FIELD_MINOR, this.minor)
        obj.putOpt(FIELD_METADATA, this.metadata)
        obj.putOpt(FIELD_RSSI, this.rssi)
        obj.putOpt(FIELD_TYPE, "ibeacon")
        return obj
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }

        if (this.javaClass != other?.javaClass) {
            return false
        }

        other as RadarBeacon

        return this.uuid == other.uuid && this.major == other.major && this.minor == other.minor
    }

    override fun hashCode(): Int {
        var result = uuid.hashCode()
        result = 31 * result + major.hashCode()
        result = 31 * result + minor.hashCode()
        return result
    }

}
