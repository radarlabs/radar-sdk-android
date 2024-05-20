package io.radar.sdk.model

import org.json.JSONObject

/**
 * Represents server-side configuration settings.
 */
internal data class RadarSdkConfiguration(
    val logLevel: Int?
) {
    companion object {
        private const val LOG_LEVEL = "logLevel"

        fun fromJson(json: JSONObject?): RadarSdkConfiguration {
            return if (json == null) {
                default()
            } else {
                RadarSdkConfiguration(
                    json.optInt(LOG_LEVEL),
                )
            }
        }

        fun default(): RadarSdkConfiguration {
            return RadarSdkConfiguration(
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
