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
                val utcOffset = obj.getInt("utcOffset")
                val dstOffset = obj.getInt("dstOffset")
                val currentTimeStr = obj.getString(FIELD_CURRENT_TIME)
                val parsedDate = RadarUtils.isoStringToDate(currentTimeStr) ?: return null

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
        
        // Create formatter based on the timezone offset
        val dateFormat = if (utcOffset == 0) {
            // For UTC/GMT times, use 'Z' format
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        } else {
            // For offset times, use ZZZZZ format
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZZZZZ", Locale.US)
        }
        
        // Set the timezone based on the id and offset
        val tz = if (utcOffset == 0) {
            TimeZone.getTimeZone("UTC")
        } else {
            TimeZone.getTimeZone(id)
        }
        dateFormat.timeZone = tz
        
        val formattedTime = dateFormat.format(currentTime)
        val finalTime = if (utcOffset == 0) {
            formattedTime
        } else {
            // Insert colon between hours and minutes of timezone offset
            formattedTime.substring(0, formattedTime.length - 2) + ":" + formattedTime.substring(formattedTime.length - 2)
        }
        
        obj.putOpt(FIELD_CURRENT_TIME, finalTime)
        obj.putOpt(FIELD_UTC_OFFSET, utcOffset)
        obj.putOpt(FIELD_DST_OFFSET, dstOffset)
        return obj
    }
}