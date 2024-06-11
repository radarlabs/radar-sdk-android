package io.radar.sdk.util

import io.radar.sdk.model.RadarReplay
import org.json.JSONObject

internal interface RadarReplayBuffer {

    fun getSize(): Int

    fun write(replayParams: JSONObject)

    fun getFlushableReplaysStash(): Flushable<RadarReplay>

    fun loadFromSharedPreferences()
}