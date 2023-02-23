package io.radar.sdk.util

import io.radar.sdk.Radar
import io.radar.sdk.model.RadarReplay
import org.json.JSONObject

internal interface RadarReplayBuffer {

    /**
     * Write an element to the buffer
     *
     */
    fun write(replayParams: JSONObject)

    /**
     * Creates a stash of the logs currently in the buffer and returns them as a [Flushable] so that a successful
     * callback can cleanup this log buffer by deleting old log files.
     *
     * @return a [Flushable] containing all stored logs
     */
    fun getFlushableReplaysStash(): Flushable<RadarReplay>
}