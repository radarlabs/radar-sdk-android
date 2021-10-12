package io.radar.sdk.model

import org.json.JSONArray
import org.json.JSONObject

/**
 * Represents a region.
 *
 * @see [](https://radar.io/documentation/regions
 */
class RadarRegion(
    /**
     * The Radar ID of the region.
     */
    @Suppress("ConstructorParameterNaming")
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
     * The optional flag of the region
     */
    val flag: String?
) {

    internal companion object {
        private const val FIELD_ID = "_id"
        private const val FIELD_TYPE = "type"
        private const val FIELD_NAME = "name"
        private const val FIELD_CODE = "code"
        private const val FIELD_FLAG = "flag"

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

            return RadarRegion(id, name, code, type, flag)
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
        return obj
    }

}
