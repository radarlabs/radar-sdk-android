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
    val logLevel: Radar.RadarLogLevel?,
    val startTrackingOnEnterForeground: Boolean?,
    val trackOnceOnEnterForeground: Boolean?,
) {
    companion object {
        private const val LOG_LEVEL = "logLevel"
        private const val START_TRACKING_ON_ENTER_FOREGROUND = "startTrackingOnEnterForeground"
        private const val TRACK_ONCE_ON_ENTER_FOREGROUND = "trackOnceOnEnterForeground"

        fun fromJson(json: JSONObject?): RadarSdkConfiguration {
            if (json == null) {
                return default()
            }

            val logLevelString = json.optString(LOG_LEVEL).uppercase()
            val startTrackingOnEnterForeground = json.optBoolean(START_TRACKING_ON_ENTER_FOREGROUND)
            val trackOnceOnEnterForeground = json.optBoolean(TRACK_ONCE_ON_ENTER_FOREGROUND)

            return RadarSdkConfiguration(
                if (logLevelString != null) Radar.RadarLogLevel.valueOf(logLevelString) else Radar.RadarLogLevel.INFO,
                startTrackingOnEnterForeground,
                trackOnceOnEnterForeground
            )
        }

        fun default(): RadarSdkConfiguration {
            return RadarSdkConfiguration(
                Radar.RadarLogLevel.INFO,
                false,
                false,
            )
        }

        fun updateSdkConfigurationFromServer(context: Context) {
            Radar.apiClient.getConfig("sdkConfigUpdate", false, object : RadarApiClient.RadarGetConfigApiCallback {
                override fun onComplete(status: Radar.RadarStatus, config: RadarConfig) {
                    RadarSettings.setSdkConfiguration(context, config?.meta.sdkConfiguration)
                }
            })
        }
    }

    fun toJson(): JSONObject {
        return JSONObject().apply {
            putOpt(LOG_LEVEL, logLevel.toString().lowercase())
            putOpt(START_TRACKING_ON_ENTER_FOREGROUND, startTrackingOnEnterForeground)
            putOpt(TRACK_ONCE_ON_ENTER_FOREGROUND, trackOnceOnEnterForeground)
        }
    }
}
