package io.radar.sdk.util

import io.radar.sdk.Radar
import io.radar.sdk.model.RadarReplay
import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
// TODO: determine if we need the above and below
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
        const val KEY_REPLAYS = "replays"
    }

    private val buffer = LinkedBlockingDeque<RadarReplay>(MAXIMUM_CAPACITY)

    override fun write(replayParams: JSONObject) {
        if (buffer.size >= MAXIMUM_CAPACITY) {
            buffer.removeFirst()
        }
        buffer.offer(RadarReplay(replayParams))
        saveToSharedPreferences()
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
                }
            }
        }
    }

    private fun saveToSharedPreferences() {
        val replaysAsJsonArray = JSONArray(buffer.map { it.toJson() })
        getSharedPreferences(context).edit { putString(KEY_REPLAYS, replaysAsJsonArray.toString()) }
    }

    override fun loadFromSharedPreferences() {
        val replaysAsString = getSharedPreferences(context).getString(KEY_REPLAYS, null)
        replaysAsString?.let {
            val replaysJsonArray = JSONArray(it)
            for (i in 0 until replaysJsonArray.length()) {
                val replayJson = replaysJsonArray.getJSONObject(i)
                buffer.offer(RadarReplay(replayJson))
            }
        }
    }

    private fun getSharedPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    }
}