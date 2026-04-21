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
import io.radar.sdk.Radar
import io.radar.sdk.RadarTrackingOptions
import android.os.Handler
import android.os.HandlerThread

internal class RadarSimpleReplayBuffer(private val context: Context) : RadarReplayBuffer {

    private companion object {
        const val MAXIMUM_CAPACITY = 120
        const val PREFERENCES_NAME = "RadarReplayBufferPreferences"
        const val KEY_REPLAYS = "radar-replays"
    }

    private val buffer = LinkedBlockingDeque<RadarReplay>(MAXIMUM_CAPACITY)
    private var batchHandlerThread: HandlerThread? = null
    private var batchHandler: Handler? = null
    private var batchTimerRunnable: Runnable? = null

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

    // region Batch methods

    private fun ensureBatchHandler(): Handler {
        return synchronized(this) {
            batchHandler ?: run {
                val thread = HandlerThread("RadarBatchTimer").apply { start() }
                batchHandlerThread = thread
                Handler(thread.looper).also { batchHandler = it }
            }
        }
    }

    override fun addToBatch(batchParams: JSONObject, options: RadarTrackingOptions) {
        val params = JSONObject(batchParams.toString())
        params.put("replayed", true)
        params.put("updatedAtMs", System.currentTimeMillis())
        params.remove("updatedAtMsDiff")

        write(params)

        synchronized(this) {
            if (options.batchInterval > 0 && batchTimerRunnable == null) {
                scheduleBatchTimer(options.batchInterval)
            }
        }

        Radar.logger.d("Added to batch | size = ${buffer.size}")
    }

    override fun shouldFlushBatch(options: RadarTrackingOptions): Boolean {
        if (buffer.size == 0) {
            return false
        }

        if (options.batchSize > 0 && buffer.size >= options.batchSize) {
            Radar.logger.d("Batch size limit reached")
            return true
        }

        return false
    }

    override fun scheduleBatchTimer(interval: Int) {
        if (interval <= 0) return

        synchronized(this) {
            val handler = ensureBatchHandler()
            batchTimerRunnable?.let { handler.removeCallbacks(it) }

            Radar.logger.d("Scheduling batch timer | interval = $interval")

            val runnable = Runnable {
                Radar.logger.d("Batch timer fired")
                synchronized(this) { batchTimerRunnable = null }
                flushBatch()
            }

            batchTimerRunnable = runnable
            handler.postDelayed(runnable, interval * 1000L)
        }
    }

    override fun cancelBatchTimer() {
        synchronized(this) {
            batchTimerRunnable?.let { runnable ->
                Radar.logger.d("Canceling batch timer")
                batchHandler?.removeCallbacks(runnable)
                batchTimerRunnable = null
            }
        }
    }

    override fun flushBatch() {
        if (Radar.isFlushingReplays) {
            Radar.logger.d("Skipping batch flush; already flushing replays")
            return  // keep the timer alive so we retry later
        }
        cancelBatchTimer()
        Radar.flushReplays()
    }

    override fun batchCount(): Int {
        return buffer.size
    }

    override fun shutdown() {
        cancelBatchTimer()
        synchronized(this) {
            batchHandlerThread?.quit()
            batchHandlerThread = null
            batchHandler = null
        }
    }

    // endregion
}