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
    val maxConcurrentJobs: Int,
    val schedulerRequiresNetwork: Boolean,
    val usePersistence: Boolean,
    val extendFlushReplays: Boolean,
    val useLogPersistence: Boolean,
    val useRadarModifiedBeacon: Boolean,
    val logLevel: Radar.RadarLogLevel,
    val startTrackingOnInitialize: Boolean,
    val trackOnceOnAppOpen: Boolean,
) {
    companion object {
        private const val MAX_CONCURRENT_JOBS = "maxConcurrentJobs"
        private const val DEFAULT_MAX_CONCURRENT_JOBS = 1
        private const val USE_PERSISTENCE = "usePersistence"
        private const val SCHEDULER_REQUIRES_NETWORK = "networkAny"
        private const val EXTEND_FLUSH_REPLAYS = "extendFlushReplays"
        private const val USE_LOG_PERSISTENCE = "useLogPersistence"
        private const val USE_RADAR_MODIFIED_BEACON = "useRadarModifiedBeacon"
        private const val LOG_LEVEL = "logLevel"
        private const val START_TRACKING_ON_INITIALIZE = "startTrackingOnInitialize"
        private const val TRACK_ONCE_ON_APP_OPEN = "trackOnceOnAppOpen"

        fun fromJson(json: JSONObject?): RadarSdkConfiguration? {
            if (json == null) {
                return null
            }

            return RadarSdkConfiguration(
                json.optInt(MAX_CONCURRENT_JOBS, DEFAULT_MAX_CONCURRENT_JOBS),
                json.optBoolean(SCHEDULER_REQUIRES_NETWORK, false),
                json.optBoolean(USE_PERSISTENCE, false),
                json.optBoolean(EXTEND_FLUSH_REPLAYS, false),
                json.optBoolean(USE_LOG_PERSISTENCE, false),
                json.optBoolean(USE_RADAR_MODIFIED_BEACON, false),
                Radar.RadarLogLevel.valueOf(json.optString(LOG_LEVEL, "info").uppercase()),
                json.optBoolean(START_TRACKING_ON_INITIALIZE, false),
                json.optBoolean(TRACK_ONCE_ON_APP_OPEN, false),
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
