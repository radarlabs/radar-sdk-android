package io.radar.sdk.model

import org.json.JSONArray
import org.json.JSONObject

/**
 * Represents a timezone.
 */
class RadarTimezone(
    /**
     * 
     */
    val id: String,

    /**
     * 
     */
    val name: String,

    /**
     * 
     */
    val code: String,

    /**
     * 
     */
    val currentTime: String,
    
    /**
     * 
     */
    val utcOffset: Int,
    
    /**
     * 
     */
    val dstOffset: Int,
) {
    internal companion object {
        private const val FIELD_ID = "id"
        private const val FIELD_NAME = "name"
        private const val FIELD_CODE = "code"
        private const val FIELD_CURRENT_TIME = "currentTime"
        private const val FIELD_UTC_OFFSET = "utcOffset"
        private const val FIELD_DST_OFFSET = "dstOffset"

        @JvmStatic
        fun fromJson(obj: JSONObject?): RadarTimezone? {
            if (obj == null) {
                return null
            }
            val id = obj.getString("id")
            val name = obj.getString("name")
            val code = obj.getString("code")
            val currentTime = obj.getString("currentTime")
            val utcOffset = obj.getInt("utcOffset")
            val dstOffset = obj.getInt("dstOffset")

            return RadarTimezone(
                id,
                name,
                code,
                currentTime,
                utcOffset,
                dstOffset,
            )
        }
    }

    fun toJson(): JSONObject {
        val obj = JSONObject()
        obj.putOpt(FIELD_ID, id)
        obj.putOpt(FIELD_NAME, name)
        obj.putOpt(FIELD_CODE, code)
        obj.putOpt(FIELD_CURRENT_TIME, currentTime)
        obj.putOpt(FIELD_UTC_OFFSET, utcOffset)
        obj.putOpt(FIELD_DST_OFFSET, dstOffset)
        return obj
    }

}
