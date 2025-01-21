package io.radar.sdk.model

import io.radar.sdk.RadarTripOptions
import io.radar.sdk.RadarUtils
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Represents a time zone.
 */
class RadarTimeZone(
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
    val currentTime: Date,

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
        fun fromJson(obj: JSONObject?): RadarTimeZone? {
            if (obj == null) {
                return null
            }
            return try {
                val id = obj.getString("id")
                val name = obj.getString("name")
                val code = obj.getString("code")
                val currentTime = obj.getString("currentTime")
                val utcOffset = obj.getInt("utcOffset")
                val dstOffset = obj.getInt("dstOffset")
                val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZZZZZ", Locale.US)
                val currentTimeStr = obj.getString(FIELD_CURRENT_TIME)
                val parsedDate = RadarUtils.isoStringToDate(currentTimeStr)
                if (parsedDate == null) {
                    return null
                }
    
                return RadarTimeZone(
                    id,
                    name,
                    code,
                    currentTime = parsedDate,
                    utcOffset,
                    dstOffset,
                )
            } catch (e: Exception) {
                null
            }
        }
    }

    fun toJson(): JSONObject {
        val obj = JSONObject()
        obj.putOpt(FIELD_ID, id)
        obj.putOpt(FIELD_NAME, name)
        obj.putOpt(FIELD_CODE, code)
        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZZZZZ", Locale.US)
        obj.putOpt(FIELD_CURRENT_TIME, dateFormat.format(currentTime))
        obj.putOpt(FIELD_UTC_OFFSET, utcOffset)
        obj.putOpt(FIELD_DST_OFFSET, dstOffset)
        return obj
    }

}
