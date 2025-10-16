package io.radar.sdk.util

import io.radar.sdk.Radar
import io.radar.sdk.RadarSettings
import io.radar.sdk.RadarTrackingOptions
import io.radar.sdk.model.RadarReplay
import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import java.util.concurrent.LinkedBlockingDeque
import java.util.Timer
import java.util.TimerTask
import org.json.JSONObject
import org.json.JSONArray

internal class RadarSimpleReplayBuffer(private val context: Context) : RadarReplayBuffer {

    private companion object {
        const val MAXIMUM_CAPACITY = 120
        const val PREFERENCES_NAME = "RadarReplayBufferPreferences"
        const val KEY_REPLAYS = "radar-replays"
    }

    private val buffer = LinkedBlockingDeque<RadarReplay>(MAXIMUM_CAPACITY)
    private var batchTimer: Timer? = null
    private var batchStartTime: Long = 0
    private var batchCount: Int = 0

    override fun getSize(): Int {
        return buffer.size
    } 

    override fun write(replayParams: JSONObject) {
        if (buffer.size >= MAXIMUM_CAPACITY) {
            buffer.removeFirst()
        }
        buffer.offer(RadarReplay(replayParams))
        val sdkConfiguration = RadarSettings.getSdkConfiguration(context)
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

    override fun addToBatch(batchParams: JSONObject, options: RadarTrackingOptions) {
        if (batchCount == 0) {
            batchStartTime = System.currentTimeMillis()
        }
        
        batchCount++
        write(batchParams)

        // Schedule timer if interval is set and timer not already running
        if (options.batchInterval > 0 && batchTimer == null) {
            scheduleBatchTimer(options)
        }
    }

    override fun shouldFlushBatch(options: RadarTrackingOptions): Boolean {
        // Return false if no batched items
        if (batchCount == 0) {
            return false
        }
        
        // Check size limit
        if (options.batchSize > 0 && batchCount >= options.batchSize) {
            return true
        }

        return false
    }

    override fun flushBatch(): Boolean {
        if (batchCount == 0) {
            return false
        }
        
        cancelBatchTimer()
        batchStartTime = 0
        batchCount = 0
        
        Radar.flushReplays()
        
        return true
    }

    override fun scheduleBatchTimer(options: RadarTrackingOptions) {
        if (options.batchInterval <= 0) return

        cancelBatchTimer()
        batchTimer = Timer()
        batchTimer?.schedule(object : TimerTask() {
            override fun run() {
                if (batchCount > 0) {
                    // Timer fired, trigger flush
                    flushBatch()
                }
            }
        }, options.batchInterval * 1000L)
    }

    override fun cancelBatchTimer() {
        batchTimer?.cancel()
        batchTimer = null
    }

    private fun getSharedPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    }
}