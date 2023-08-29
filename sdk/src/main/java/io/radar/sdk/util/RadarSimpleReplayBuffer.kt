package io.radar.sdk.util

import io.radar.sdk.Radar
import io.radar.sdk.model.RadarReplay
// TODO: determine if we need the above and below
import java.util.concurrent.LinkedBlockingDeque
import org.json.JSONObject

/**
 * A buffer for replay events.
 */

internal class RadarSimpleReplayBuffer : RadarReplayBuffer {

    private companion object {
        const val MAXIMUM_CAPACITY = 120
    }

    private val buffer = LinkedBlockingDeque<RadarReplay>(MAXIMUM_CAPACITY)

    override fun write(replayParams: JSONObject) {
        if (buffer.size >= MAXIMUM_CAPACITY) {
            buffer.removeFirst()
        }
        buffer.offer(RadarReplay(replayParams))
    }

    override fun getFlushableReplaysStash(): Flushable<RadarReplay> {
        val replays = buffer.toList()

        return object : Flushable<RadarReplay> {

            override fun get(): List<RadarReplay> {
                return replays
            }

            override fun onFlush(success: Boolean) {
                if (success) {
                    // buffer.clear()
                    buffer.removeAll(replays) // only clear the replays from buffer that were successfully flushed
                }
            }
        }
    }
}