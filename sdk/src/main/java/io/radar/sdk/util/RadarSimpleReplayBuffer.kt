package io.radar.sdk.util

import io.radar.sdk.RadarSettings
import io.radar.sdk.model.RadarReplay
import io.radar.sdk.model.RadarSdkConfiguration
import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import java.util.concurrent.LinkedBlockingDeque
import org.json.JSONObject
import org.json.JSONArray

internal class RadarSimpleReplayBuffer(private val context: Context) : RadarReplayBuffer {

    private companion object {
        const val MAXIMUM_CAPACITY = 120
        const val PREFERENCES_NAME = "RadarReplayBufferPreferences"
        const val KEY_REPLAYS = "radar-replays"
    }

    private val buffer = LinkedBlockingDeque<RadarReplay>(MAXIMUM_CAPACITY)

    override fun getSize(): Int {
        return buffer.size
    } 

    override fun write(replayParams: JSONObject) {
        val sdkConfiguration = RadarSettings.getSdkConfiguration(context)
        val maxBufferSize = if (sdkConfiguration.maxReplayBufferSize > 0) {
            sdkConfiguration.maxReplayBufferSize
        } else {
            RadarSdkConfiguration.DEFAULT_MAX_REPLAY_BUFFER_SIZE
        }

        while (buffer.size >= maxBufferSize) {
            buffer.removeFirst()
        }

        buffer.offer(RadarReplay(replayParams))
        if (sdkConfiguration.usePersistence) {
            // if buffer length is above 50, remove every fifth replay from the persisted buffer
            if (buffer.size > 50) {
                val prunedBuffer = buffer.filterIndexed { index, _ -> index % 5 != 0 }
                val prunedReplaysAsJsonArray = JSONArray(prunedBuffer.map { it.toJson() })
                getSharedPreferences(context).edit { putString(KEY_REPLAYS, prunedReplaysAsJsonArray.toString()) }
            } else {
                val replaysAsJsonArray = JSONArray(buffer.map { it.toJson() })
                getSharedPreferences(context).edit { putString(KEY_REPLAYS, replaysAsJsonArray.toString()) }
            }
        }
    }

    override fun getFlushableReplaysStash(): Flushable<RadarReplay> {
        val replays = buffer.toList()

        return object : Flushable<RadarReplay> {

            override fun get(): List<RadarReplay> {
                return replays
            }

            override fun onFlush(success: Boolean) {
                if (success) {
                    buffer.removeAll(replays) // only clear the replays from buffer that were successfully flushed
                    
                    // clear the shared preferences
                    val replaysAsJsonArray = JSONArray(buffer.map { it.toJson() })
                    getSharedPreferences(context).edit { putString(KEY_REPLAYS, replaysAsJsonArray.toString()) }
                }
            }
        }
    }

    override fun loadFromSharedPreferences() {
        val replaysAsString = getSharedPreferences(context).getString(KEY_REPLAYS, null)
        replaysAsString?.let { replays ->
            val replaysAsJsonArray = JSONArray(replays)
            for (i in 0 until replaysAsJsonArray.length()) {
                val replayAsJsonObject = replaysAsJsonArray.getJSONObject(i)
                val replay = RadarReplay.fromJson(replayAsJsonObject)
                buffer.offer(replay)
            }
        }
    }

    private fun getSharedPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    }
}