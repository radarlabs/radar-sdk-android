package io.radar.sdk.model

import org.json.JSONObject

/**
 * Represents a replay.
 */

internal data class RadarReplay(
    val replayParams: JSONObject
) : Comparable<RadarReplay> {

    companion object {
        private const val REPLAY_PARAMS = "replayParams"

        @JvmStatic
        fun fromJson(json: JSONObject): RadarReplay = RadarReplay(
            replayParams = json.optJSONObject(REPLAY_PARAMS)
        )
    }

    fun toJson(): JSONObject = JSONObject().apply {
        putOpt(REPLAY_PARAMS, replayParams)
    }

    override fun compareTo(other: RadarReplay): Int = replayParams.toString().compareTo(other.replayParams.toString())
}
