package io.radar.sdk.model

import android.content.Context
import io.radar.sdk.Radar
import io.radar.sdk.RadarApiClient
import io.radar.sdk.RadarSettings
import org.json.JSONObject

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
    val useLocationMetadata: Boolean,
    val useOpenedAppConversion: Boolean = false,
    val useForegroundLocationUpdatedAtMsDiff: Boolean = false,
    val locationManagerTimeout: Int = 0,
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
        private const val USE_LOCATION_METADATA = "useLocationMetadata"
        private const val USE_OPENED_APP_CONVERSION = "useOpenedAppConversion"
        private const val USE_FOREGROUND_LOCATION_UPDATED_AT_MS_DIFF = "useForegroundLocationUpdatedAtMsDiff"
        private const val LOCATION_MANAGER_TIMEOUT = "locationManagerTimeout"

        fun fromJson(json: JSONObject?): RadarSdkConfiguration {
            // set json as empty object if json is null, which uses fallback values
            val config = json ?: JSONObject();

            return RadarSdkConfiguration(
                config.optInt(MAX_CONCURRENT_JOBS, DEFAULT_MAX_CONCURRENT_JOBS),
                config.optBoolean(SCHEDULER_REQUIRES_NETWORK, false),
                config.optBoolean(USE_PERSISTENCE, false),
                config.optBoolean(EXTEND_FLUSH_REPLAYS, false),
                config.optBoolean(USE_LOG_PERSISTENCE, false),
                config.optBoolean(USE_RADAR_MODIFIED_BEACON, false),
                Radar.RadarLogLevel.valueOf(config.optString(LOG_LEVEL, "info").uppercase()),
                config.optBoolean(START_TRACKING_ON_INITIALIZE, false),
                config.optBoolean(TRACK_ONCE_ON_APP_OPEN, false),
                config.optBoolean(USE_LOCATION_METADATA, false),
                config.optBoolean(USE_OPENED_APP_CONVERSION, true),
                config.optBoolean(USE_FOREGROUND_LOCATION_UPDATED_AT_MS_DIFF, false),
                config.optInt(LOCATION_MANAGER_TIMEOUT, 0),
            )
        }

        fun updateSdkConfigurationFromServer(context: Context) {
            Radar.apiClient.getConfig("sdkConfigUpdate", false, object : RadarApiClient.RadarGetConfigApiCallback {
                override fun onComplete(status: Radar.RadarStatus, config: RadarConfig?) {
                    if (config == null) {
                        return
                    }

                    RadarSettings.setSdkConfiguration(context, config.meta.sdkConfiguration)
                }
            })
        }
    }

    fun toJson(): JSONObject {
        return JSONObject().apply {
            putOpt(SCHEDULER_REQUIRES_NETWORK, schedulerRequiresNetwork)
            putOpt(MAX_CONCURRENT_JOBS, maxConcurrentJobs)
            putOpt(USE_PERSISTENCE, usePersistence)
            putOpt(EXTEND_FLUSH_REPLAYS, extendFlushReplays)
            putOpt(USE_LOG_PERSISTENCE, useLogPersistence)
            putOpt(USE_RADAR_MODIFIED_BEACON, useRadarModifiedBeacon)
            putOpt(LOG_LEVEL, logLevel.toString().lowercase())
            putOpt(START_TRACKING_ON_INITIALIZE, startTrackingOnInitialize)
            putOpt(TRACK_ONCE_ON_APP_OPEN, trackOnceOnAppOpen)
            putOpt(USE_LOCATION_METADATA, useLocationMetadata)
            putOpt(USE_OPENED_APP_CONVERSION, useOpenedAppConversion)
            putOpt(USE_FOREGROUND_LOCATION_UPDATED_AT_MS_DIFF, useForegroundLocationUpdatedAtMsDiff)
            putOpt(LOCATION_MANAGER_TIMEOUT, locationManagerTimeout)
        }
    }
}
