package io.radar.sdk.model

import org.json.JSONObject
import io.radar.sdk.Radar

/**
 * Represents server-side configuration settings.
 */
internal data class RadarSdkConfiguration(
    val logLevel: Radar.RadarLogLevel?
) {
    companion object {
        private const val LOG_LEVEL = "logLevel"

        fun fromJson(json: JSONObject?): RadarSdkConfiguration {
            if (json == null) {
                return default()
            }

            val logLevelString = json.optString(LOG_LEVEL)?.uppercase()
            return RadarSdkConfiguration(
                if (logLevelString != null) Radar.RadarLogLevel.valueOf(logLevelString) else null,
            )
        }

        fun default(): RadarSdkConfiguration {
            return RadarSdkConfiguration(
                null
            )
        }
    }

    fun toJson(): JSONObject {
        return JSONObject().apply {
            putOpt(LOG_LEVEL, logLevel.toString().lowercase())
        }
    }
}
