package io.radar.sdk.model

import org.json.JSONObject

/**
 * Represents server-side feature settings.
 */
internal data class RadarFeatureSettings(
    val maxConcurrentJobs: Int,
    val schedulerRequiresNetwork: Boolean,
    val usePersistence: Boolean,
    val extendFlushReplays: Boolean,
    val useLogPersistence: Boolean,
    val useRadarModifiedBeacon: Boolean
) {
    companion object {
        private const val MAX_CONCURRENT_JOBS = "maxConcurrentJobs"
        private const val DEFAULT_MAX_CONCURRENT_JOBS = 1
        private const val USE_PERSISTENCE = "usePersistence"
        private const val SCHEDULER_REQUIRES_NETWORK = "networkAny"
        private const val EXTEND_FLUSH_REPLAYS = "extendFlushReplays"
        private const val USE_LOG_PERSISTENCE = "useLogPersistence"
        private const val USE_RADAR_MODIFIED_BEACON = "useRadarModifiedBeacon"

        fun fromJson(json: JSONObject?): RadarFeatureSettings {
            return if (json == null) {
                default()
            } else {
                RadarFeatureSettings(
                    json.optInt(MAX_CONCURRENT_JOBS, DEFAULT_MAX_CONCURRENT_JOBS),
                    json.optBoolean(SCHEDULER_REQUIRES_NETWORK),
                    json.optBoolean(USE_PERSISTENCE),
                    json.optBoolean(EXTEND_FLUSH_REPLAYS),
                    json.optBoolean(USE_LOG_PERSISTENCE),
                    json.optBoolean(USE_RADAR_MODIFIED_BEACON)
                )
            }
        }

        fun default(): RadarFeatureSettings {
            return RadarFeatureSettings(
                DEFAULT_MAX_CONCURRENT_JOBS,
                false, // networkAny
                false, // usePersistence
                false, // extendFlushReplays
                false, // useLogPersistence
                false, // useRadarModifiedBeacon
            )
        }
    }

    fun toJson(): JSONObject {
        return JSONObject().apply {
            putOpt(MAX_CONCURRENT_JOBS, maxConcurrentJobs)
            putOpt(SCHEDULER_REQUIRES_NETWORK, schedulerRequiresNetwork)
            putOpt(USE_PERSISTENCE, usePersistence)
            putOpt(EXTEND_FLUSH_REPLAYS, extendFlushReplays)
            putOpt(USE_LOG_PERSISTENCE, useLogPersistence)
            putOpt(USE_RADAR_MODIFIED_BEACON, useRadarModifiedBeacon)
        }
    }
}
