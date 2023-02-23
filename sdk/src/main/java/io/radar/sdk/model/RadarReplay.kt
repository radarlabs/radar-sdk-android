package io.radar.sdk.model

import io.radar.sdk.Radar
import org.json.JSONObject

/**
 * Represents a replay.
 */

 internal data class RadarReplay(
    val replayParams: JSONObject
) : Comparable<RadarReplay> {

    companion object {
        private const val REPLAY_PARAMS = "replayParams"

        // does this make sense?
        @JvmStatic
        fun fromJson(json: JSONObject): RadarReplay {
            return RadarReplay(
                replayParams = json.optJSONObject(REPLAY_PARAMS)
            )
        }
    }

    // does this make sense as well?
    fun toJson(): JSONObject {
        return JSONObject().apply {
            putOpt(REPLAY_PARAMS, replayParams)
        }
    }

    fun toListofJson(replays: List<RadarReplay>): List<JSONObject> {
        val replayList = mutableListOf<JSONObject>()
        for (replay in replays) {
            replayList.add(replay.toJson())
        }
        return replayList

    }

    override fun compareTo(other: RadarReplay): Int {
        return replayParams.toString().compareTo(other.replayParams.toString())
    }
}