package io.radar.sdk.model

import org.json.JSONObject
import io.radar.sdk.Radar

/**
 * Represents server-side configuration settings.
 */
internal data class RadarSDKConfiguration(
    val logLevel: Radar.RadarLogLevel?
) {
    companion object {
        private const val LOG_LEVEL = "logLevel"

        fun fromJson(json: JSONObject?): RadarSDKConfiguration {
            if (json == null) {
                return default()
            }

            val logLevelString = json.optString(LOG_LEVEL)?.uppercase()
            return RadarSDKConfiguration(
                if (logLevelString != null) Radar.RadarLogLevel.valueOf(logLevelString) else null,
            )
        }

        fun default(): RadarSDKConfiguration {
            return RadarSDKConfiguration(
                null
            )
        }
    }

    fun toJson(): JSONObject {
        return JSONObject().apply {
            putOpt(LOG_LEVEL, logLevel)
        }
    }
}
