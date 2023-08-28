package io.radar.sdk.util

import io.radar.sdk.Radar
import io.radar.sdk.RadarSettings
import io.radar.sdk.model.RadarReplay
import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import java.util.concurrent.LinkedBlockingDeque
import org.json.JSONObject
import org.json.JSONArray

/**
 * A buffer for replay events.
 */

internal class RadarSimpleReplayBuffer(private val context: Context) : RadarReplayBuffer {

    private companion object {
        const val MAXIMUM_CAPACITY = 120
        const val PREFERENCES_NAME = "RadarReplayBufferPreferences"
        const val KEY_REPLAYS = "radar-replays"
    }

    private val buffer = LinkedBlockingDeque<RadarReplay>(MAXIMUM_CAPACITY)

    override fun write(replayParams: JSONObject) {
        if (buffer.size >= MAXIMUM_CAPACITY) {
            buffer.removeFirst()
        }
        buffer.offer(RadarReplay(replayParams))
        val featureSettings = RadarSettings.getFeatureSettings(context)
        if (featureSettings.usePersistence) {
            // If buffer length is above 50, remove every fifth replay from the persisted buffer 
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
                    buffer.clear()
                    // clear the shared preferences
                    getSharedPreferences(context).edit { remove(KEY_REPLAYS) }
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