package io.radar.sdk.util

import io.radar.sdk.Radar
import io.radar.sdk.model.RadarReplay
import org.json.JSONObject

internal interface RadarReplayBuffer {

    /**
     * Get the size of the current replay buffer
     */
    fun getSize(): Int

    /**
     * Write an element to the buffer
     *
     */
    fun write(replayParams: JSONObject)

    /**
     * Creates a stash of the replays currently in the buffer and returns them as a [Flushable] so that a successful
     * callback can cleanup this replay buffer by deleting replays that have been flushed.
     *
     * @return a [Flushable] containing all stored replays
     */
    fun getFlushableReplaysStash(): Flushable<RadarReplay>
}