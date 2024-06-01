package io.radar.sdk.model

import org.json.JSONArray
import org.json.JSONObject

/**
 * Represents a region.
 *
 * @see [](https://radar.com/documentation/regions
 */
class RadarRegion(
    /**
     * The Radar ID of the region.
     */
    val _id: String,

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
    val type: String,

    /**
     * The optional flag of the region.
     */
    val flag: String?,

    /**
     * A boolean indicating whether the jurisdiction is allowed. May be `false` if Fraud is not enabled.
     */
    val allowed: Boolean = false,

    /**
     * A boolean indicating whether all jurisdiction checks for the region have passed. May be `false` if Fraud is not enabled.
     */
    val passed: Boolean = false,

    /**
     * A boolean indicating whether the user is in an exclusion zone for the jurisdiction. May be `false` if Fraud is not enabled.
     */
    val inExclusionZone: Boolean = false,

    /**
     * A boolean indicating whether the user is too close to the border for the jurisdiction. May be `false` if Fraud is not enabled.
     */
    val inBufferZone: Boolean = false,

    /**
     * The distance in meters to the border of the jurisdiction. May be 0 if Fraud is not enabled.
     */
    val distanceToBorder: Double,
) {

    internal companion object {
        private const val FIELD_ID = "_id"
        private const val FIELD_TYPE = "type"
        private const val FIELD_NAME = "name"
        private const val FIELD_CODE = "code"
        private const val FIELD_FLAG = "flag"
        private const val FIELD_ALLOWED = "allowed"
        private const val FIELD_PASSED = "passed"
        private const val FIELD_IN_EXCLUSION_ZONE = "inExclusionZone"
        private const val FIELD_IN_BUFFER_ZONE = "inBufferZone"
        private const val FIELD_DISTANCE_TO_BORDER = "distanceToBorder"

        @JvmStatic
        fun fromJson(obj: JSONObject?): RadarRegion? {
            if (obj == null) {
                return null
            }

            val id = obj.optString(FIELD_ID) ?: ""
            val name = obj.optString(FIELD_NAME) ?: ""
            val code = obj.optString(FIELD_CODE) ?: ""
            val type = obj.optString(FIELD_TYPE) ?: ""
            val flag = obj.optString(FIELD_FLAG) ?: null
            val allowed = obj.optBoolean(FIELD_ALLOWED)
            val passed = obj.optBoolean(FIELD_PASSED)
            val inExclusionZone = obj.optBoolean(FIELD_IN_EXCLUSION_ZONE)
            val inBufferZone = obj.optBoolean(FIELD_IN_BUFFER_ZONE)
            val distanceToBorder = obj.optDouble(FIELD_DISTANCE_TO_BORDER)

            return RadarRegion(id, name, code, type, flag, allowed, passed, inExclusionZone, inBufferZone, distanceToBorder)
        }

        @JvmStatic
        fun fromJson(arr: JSONArray?): Array<RadarRegion>? {
            if (arr == null) {
                return null
            }

            return Array(arr.length()) { index ->
                fromJson(arr.optJSONObject(index))
            }.filterNotNull().toTypedArray()
        }
    }

    fun toJson(): JSONObject {
        val obj = JSONObject()
        obj.putOpt(FIELD_ID, this._id)
        obj.putOpt(FIELD_NAME, this.name)
        obj.putOpt(FIELD_CODE, this.code)
        obj.putOpt(FIELD_TYPE, this.type)
        obj.putOpt(FIELD_FLAG, this.flag)
        obj.putOpt(FIELD_ALLOWED, this.allowed)
        obj.putOpt(FIELD_PASSED, this.passed)
        obj.putOpt(FIELD_IN_EXCLUSION_ZONE, this.inExclusionZone)
        obj.putOpt(FIELD_IN_BUFFER_ZONE, this.inBufferZone)
        if (!this.distanceToBorder.isNaN()) {
            obj.putOpt(FIELD_DISTANCE_TO_BORDER, this.distanceToBorder)
        }
        return obj
    }

}
