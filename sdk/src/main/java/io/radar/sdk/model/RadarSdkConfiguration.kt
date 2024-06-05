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
                if (logLevelString != null) Radar.RadarLogLevel.valueOf(logLevelString) else Radar.RadarLogLevel.INFO,
            )
        }

        fun default(): RadarSdkConfiguration {
            return RadarSdkConfiguration(
                Radar.RadarLogLevel.INFO
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
        }
    }
}
