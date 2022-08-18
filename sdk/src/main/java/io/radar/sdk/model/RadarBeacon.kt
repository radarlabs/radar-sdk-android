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
     * For iBeacons, the UUID of the beacon. For Eddystone beacons, the UID of the beacon.
     */
    val uuid: String,

    /**
     * For iBeacons, the major ID of the beacon. For Eddystone beacons, the instance ID of the beacon.
     */
    val major: String,

    /**
     * For iBeacons, the minor ID of the beacon.
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
    val location: RadarCoordinate? = null,

    /**
     * The type of the beacon.
     */
    val type: RadarBeaconType
) {

    /**
     * The types for beacons.
     */
    enum class RadarBeaconType {
        /** iBeacon */
        IBEACON,
        /** Eddystone */
        EDDYSTONE
    }

    internal companion object {
        private const val FIELD_ID = "_id"
        private const val FIELD_DESCRIPTION = "description"
        private const val FIELD_TAG = "tag"
        private const val FIELD_EXTERNAL_ID = "externalId"
        private const val FIELD_UUID = "uuid"
        private const val FIELD_MAJOR = "major"
        private const val FIELD_MINOR = "minor"
        private const val FIELD_UID = "uid"
        private const val FIELD_INSTANCE = "instance"
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

            val type = when (obj.optString(FIELD_TYPE)) {
                "eddystone" -> RadarBeaconType.EDDYSTONE
                else -> RadarBeaconType.IBEACON
            }
            val id: String = obj.optString(FIELD_ID) ?: ""
            val description: String = obj.optString(FIELD_DESCRIPTION) ?: ""
            val tag = obj.optString(FIELD_TAG) ?: null
            val externalId = obj.optString(FIELD_EXTERNAL_ID) ?: null
            var uuid = ""
            var major = ""
            if (type == RadarBeaconType.EDDYSTONE) {
                uuid = obj.optString(FIELD_UID) ?: ""
                major = obj.optString(FIELD_INSTANCE) ?: ""
            } else if (type == RadarBeaconType.IBEACON) {
                uuid = obj.optString(FIELD_UUID) ?: ""
                major = obj.optString(FIELD_MAJOR) ?: ""
            }
            val minor: String = obj.optString(FIELD_MINOR) ?: ""
            val metadata: JSONObject? = obj.optJSONObject(FIELD_METADATA) ?: null
            val rssi: Int = obj.optInt(FIELD_RSSI)
            val geometryObj = obj.optJSONObject(FIELD_GEOMETRY)
            val geometryCoordinatesObj = geometryObj?.optJSONArray(FIELD_COORDINATES)
            val geometry = RadarCoordinate(
                geometryCoordinatesObj?.optDouble(1) ?: 0.0,
                geometryCoordinatesObj?.optDouble(0) ?: 0.0
            )

            return RadarBeacon(id, description, tag, externalId, uuid, major, minor, metadata, rssi, geometry, type)
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

        @JvmStatic
        fun stringForType(type: RadarBeaconType): String? {
            return when (type) {
                RadarBeaconType.EDDYSTONE -> "eddystone"
                RadarBeaconType.IBEACON -> "ibeacon"
            }
        }
    }

    fun toJson(): JSONObject {
        val obj = JSONObject()
        obj.putOpt(FIELD_TYPE, stringForType(this.type))
        obj.putOpt(FIELD_ID, this._id)
        if (this.type == RadarBeaconType.EDDYSTONE) {
            obj.putOpt(FIELD_UID, this.uuid.lowercase())
            obj.putOpt(FIELD_INSTANCE, this.major)
        } else if (this.type == RadarBeaconType.IBEACON) {
            obj.putOpt(FIELD_UUID, this.uuid.lowercase())
            obj.putOpt(FIELD_MAJOR, this.major)
            obj.putOpt(FIELD_MINOR, this.minor)
        }
        obj.putOpt(FIELD_METADATA, this.metadata)
        obj.putOpt(FIELD_RSSI, this.rssi)
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

        return this.uuid == other.uuid && this.major == other.major && this.minor == other.minor && this.type == other.type
    }

    override fun hashCode(): Int {
        var result = uuid.hashCode()
        result = 31 * result + major.hashCode()
        result = 31 * result + minor.hashCode()
        return result
    }

}
