package io.radar.sdk.model

import android.content.Context
import org.json.JSONObject
import io.radar.sdk.Radar
import io.radar.sdk.RadarApiClient
import io.radar.sdk.RadarSettings

/**
 * Represents server-side configuration settings.
 */
internal data class RadarSdkConfiguration(
    val logLevel: Radar.RadarLogLevel,
    val startTrackingOnInitialize: Boolean,
    val trackOnceOnAppOpen: Boolean,
) {
    companion object {
        private const val LOG_LEVEL = "logLevel"
        private const val START_TRACKING_ON_INITIALIZE = "startTrackingOnInitialize"
        private const val TRACK_ONCE_ON_APP_OPEN = "trackOnceOnAppOpen"

        fun fromJson(json: JSONObject?): RadarSdkConfiguration? {
            if (json == null) {
                return null
            }

            val logLevelString = json.optString(LOG_LEVEL)
            val startTrackingOnInitialize = json.optBoolean(START_TRACKING_ON_INITIALIZE)
            val trackOnceOnAppOpen = json.optBoolean(TRACK_ONCE_ON_APP_OPEN)

            return RadarSdkConfiguration(
                if (!logLevelString.isNullOrEmpty()) Radar.RadarLogLevel.valueOf(logLevelString.uppercase()) else Radar.RadarLogLevel.INFO,
                startTrackingOnInitialize,
                trackOnceOnAppOpen,
            )
        }

        fun updateSdkConfigurationFromServer(context: Context) {
            Radar.apiClient.getConfig("sdkConfigUpdate", false, object : RadarApiClient.RadarGetConfigApiCallback {
                override fun onComplete(status: Radar.RadarStatus, config: RadarConfig) {
                    RadarSettings.setSdkConfiguration(context, config.meta.sdkConfiguration)
                }
            })
        }
    }

    fun toJson(): JSONObject {
        return JSONObject().apply {
            putOpt(LOG_LEVEL, logLevel.toString().lowercase())
            putOpt(START_TRACKING_ON_INITIALIZE, startTrackingOnInitialize)
            putOpt(TRACK_ONCE_ON_APP_OPEN, trackOnceOnAppOpen)
        }
    }
}
