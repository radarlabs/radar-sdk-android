package io.radar.sdk.model

import org.json.JSONObject

/**
 * Contains remote-configurable feature settings
 */
internal data class RadarFeatureSettings(
    val maxConcurrentJobs: Int,
    val schedulerRequiresNetwork: Boolean
) {
    companion object {
        private const val MAX_CONCURRENT_JOBS = "concurrent_jobs_int"
        private const val DEFAULT_MAX_CONCURRENT_JOBS = 1
        private const val SCHEDULER_REQUIRES_NETWORK = "scheduler_require_network_bool"

        fun fromJson(json: JSONObject?): RadarFeatureSettings {
            return if (json == null) {
                default()
            } else {
                RadarFeatureSettings(
                    json.optInt(MAX_CONCURRENT_JOBS, DEFAULT_MAX_CONCURRENT_JOBS),
                    json.optBoolean(SCHEDULER_REQUIRES_NETWORK)
                )
            }
        }

        fun default(): RadarFeatureSettings {
            return RadarFeatureSettings(DEFAULT_MAX_CONCURRENT_JOBS, false)
        }
    }

    fun toJson(): JSONObject {
        return JSONObject().apply {
            putOpt(MAX_CONCURRENT_JOBS, maxConcurrentJobs)
            putOpt(SCHEDULER_REQUIRES_NETWORK, schedulerRequiresNetwork)
        }
    }
}
