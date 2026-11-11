package io.radar.sdk.model

import io.radar.sdk.RadarUtils
import java.util.Date
import org.json.JSONObject

// Parsers that `RadarEvent` and `RadarConversion` share.
internal object RadarEventParsing {

    // Returns null when the value is missing, is not a string, or is not a valid ISO date.
    fun date(obj: JSONObject, key: String): Date? {
        val str = obj.opt(key) as? String ?: return null
        return RadarUtils.isoStringToDate(str)
    }

    // Accepts a boolean, a number (not zero is true), or the string "true" or "false". Returns false for all
    // other values.
    fun boolean(obj: JSONObject, key: String): Boolean = when (val value = obj.opt(key)) {
        is Number -> value.toDouble() != 0.0
        else -> obj.optBoolean(key)
    }

    // Accepts a JSON object or a string that holds a JSON object. Returns null for all other values.
    fun metadata(obj: JSONObject, key: String): JSONObject? = when (val value = obj.opt(key)) {
        is JSONObject -> value

        is String -> try {
            JSONObject(value)
        } catch (_: Exception) {
            null
        }

        else -> null
    }
}
