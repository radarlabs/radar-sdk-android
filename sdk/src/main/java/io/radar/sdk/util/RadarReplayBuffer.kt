package io.radar.sdk.util

import io.radar.sdk.RadarTrackingOptions
import io.radar.sdk.model.RadarReplay
import org.json.JSONObject

internal interface RadarReplayBuffer {

    fun getSize(): Int

    fun write(replayParams: JSONObject)

    fun getFlushableReplaysStash(): Flushable<RadarReplay>

    fun loadFromSharedPreferences()

    // Batching methods
    fun addToBatch(batchParams: JSONObject, options: RadarTrackingOptions)

    fun shouldFlushBatch(options: RadarTrackingOptions): Boolean

    fun flushBatch(): Boolean

    fun scheduleBatchTimer(options: RadarTrackingOptions)

    fun cancelBatchTimer()
}