package io.radar.sdk.util

import io.radar.sdk.Radar
import io.radar.sdk.RadarSettings
import io.radar.sdk.RadarTrackingOptions
import io.radar.sdk.model.RadarReplay
import android.content.Context
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import androidx.core.content.edit
import java.util.concurrent.LinkedBlockingDeque
import java.util.concurrent.atomic.AtomicInteger
import org.json.JSONObject
import org.json.JSONArray

internal class RadarSimpleReplayBuffer(private val context: Context) : RadarReplayBuffer {

    private companion object {
        const val MAXIMUM_CAPACITY = 120
        const val PREFERENCES_NAME = "RadarReplayBufferPreferences"
        const val KEY_REPLAYS = "radar-replays"
    }

    private val buffer = LinkedBlockingDeque<RadarReplay>(MAXIMUM_CAPACITY)
    private val handler = Handler(Looper.getMainLooper())
    private var batchTimerRunnable: Runnable? = null
    private val batchCount = AtomicInteger(0)

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
        batchCount.incrementAndGet()
        write(batchParams)

        // Schedule timer if interval is set and timer not already running
        if (options.batchInterval > 0 && batchTimerRunnable == null) {
            scheduleBatchTimer(options)
        }
    }

    override fun shouldFlushBatch(options: RadarTrackingOptions): Boolean {
        val currentCount = batchCount.get()
        
        // Return false if no batched items
        if (currentCount == 0) {
            return false
        }
        
        // Check size limit
        if (options.batchSize > 0 && currentCount >= options.batchSize) {
            return true
        }

        return false
    }

    override fun flushBatch(): Boolean {
        val currentCount = batchCount.getAndSet(0)
        if (currentCount == 0) {
            return false
        }
        
        cancelBatchTimer()
        Radar.flushReplays()
        
        return true
    }

    override fun scheduleBatchTimer(options: RadarTrackingOptions) {
        if (options.batchInterval <= 0) return

        cancelBatchTimer()
        
        batchTimerRunnable = Runnable {
            if (batchCount.get() > 0) {
                // Timer fired, trigger flush
                flushBatch()
            }
        }
        
        handler.postDelayed(batchTimerRunnable!!, (options.batchInterval * 1000L))
    }

    override fun cancelBatchTimer() {
        batchTimerRunnable?.let { runnable ->
            handler.removeCallbacks(runnable)
            batchTimerRunnable = null
        }
    }

    private fun getSharedPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    }
}