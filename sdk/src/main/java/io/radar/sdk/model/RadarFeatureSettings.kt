package io.radar.sdk.model

import org.json.JSONObject

/**
 * Represents server-side feature settings.
 */
internal data class RadarFeatureSettings(
    val maxConcurrentJobs: Int,
    val schedulerRequiresNetwork: Boolean,
    val usePersistence: Boolean
) {
    companion object {
        private const val MAX_CONCURRENT_JOBS = "maxConcurrentJobs"
        private const val DEFAULT_MAX_CONCURRENT_JOBS = 1
        private const val USE_PERSISTENCE = "usePersistence"
        private const val SCHEDULER_REQUIRES_NETWORK = "networkAny"

        fun fromJson(json: JSONObject?): RadarFeatureSettings {
            return if (json == null) {
                default()
            } else {
                RadarFeatureSettings(
                    json.optInt(MAX_CONCURRENT_JOBS, DEFAULT_MAX_CONCURRENT_JOBS),
                    json.optBoolean(SCHEDULER_REQUIRES_NETWORK)
                    json.optBoolean(USE_PERSISTENCE, false)
                )
            }
        }

        fun default(): RadarFeatureSettings {
            return RadarFeatureSettings(maxConcurrentJobs = DEFAULT_MAX_CONCURRENT_JOBS, schedulerRequiresNetwork = false, usePersistence = false)
        }
    }

    fun toJson(): JSONObject {
        return JSONObject().apply {
            putOpt(MAX_CONCURRENT_JOBS, maxConcurrentJobs)
            putOpt(SCHEDULER_REQUIRES_NETWORK, schedulerRequiresNetwork)
            putOpt(USE_PERSISTENCE, usePersistence)
        }
    }
}
