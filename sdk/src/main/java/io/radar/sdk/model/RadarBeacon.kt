package io.radar.sdk.model

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * Represents a Bluetooth beacon. For more information about Beacons, see [](https://radar.io/documentation/beacons).
 *
 * @see [](https://radar.io/documentation/beacons)
 */
class RadarBeacon (
    /**
     * The Radar ID of the point.
     */
    val _id: String,

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
    val minor: String
) {
    internal companion object {
        private const val FIELD_ID = "_id"
        private const val FIELD_UUID = "uuid"
        private const val FIELD_MAJOR = "major"
        private const val FIELD_MINOR = "minor"

        @JvmStatic
        fun fromJson(obj: JSONObject?): RadarBeacon? {
            if (obj == null) {
                return null
            }

            val id: String = obj.optString(FIELD_ID) ?: ""
            val uuid: String = obj.optString(FIELD_UUID) ?: ""
            val major: String = obj.optString(FIELD_MAJOR) ?: ""
            val minor: String = obj.optString(FIELD_MINOR) ?: ""

            return RadarBeacon(id, uuid, major, minor)
        }

        @JvmStatic
        @Throws(JSONException::class)
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
        obj.putOpt(FIELD_UUID, this.uuid)
        obj.putOpt(FIELD_MAJOR, this.major)
        obj.putOpt(FIELD_MINOR, this.minor)
        return obj
    }

}
