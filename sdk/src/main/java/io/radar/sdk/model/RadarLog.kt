package io.radar.sdk.model

import io.radar.sdk.Radar
import org.json.JSONObject
import java.util.*

/**
 * Represents a log line.
 */
internal data class RadarLog(
    val level: Radar.RadarLogLevel,
    val message: String,
    val createdAt: Date = Date()
) : Comparable<RadarLog> {

    companion object {
        private const val CREATED_AT = "createdAt"
        private const val LEVEL = "level"
        private const val MESSAGE = "message"

        @JvmStatic
        fun fromJson(json: JSONObject): RadarLog {
            return RadarLog(
                level = Radar.RadarLogLevel.valueOf(json.optString(LEVEL)),
                message = json.optString(MESSAGE),
                createdAt = Date(json.optLong(CREATED_AT))
            )
        }
    }

    fun toJson(): JSONObject {
        return JSONObject().apply {
            putOpt(CREATED_AT, createdAt.time)
            putOpt(LEVEL, level.name)
            putOpt(MESSAGE, message)
        }
    }

    override fun compareTo(other: RadarLog): Int {
        return createdAt.compareTo(other.createdAt)
    }
}
