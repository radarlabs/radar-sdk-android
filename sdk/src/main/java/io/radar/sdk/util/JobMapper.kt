package io.radar.sdk.util

import androidx.annotation.VisibleForTesting
import java.util.concurrent.ConcurrentHashMap

/**
 * Thread-safe storage for use with [io.radar.sdk.RadarJobScheduler], for storing and accessing Job IDs and their
 * associated counters, which are used for logging and debugging.
 */
internal class JobMapper {

    companion object {
        /**
         * Default Job ID (Radar's birthday!)
         */
        @VisibleForTesting
        const val DEFAULT_JOB_ID = 20160525
    }

    private val counter: MutableMap<Int, Int> = ConcurrentHashMap()
    var size: Int = 0
        private set

    /**
     * Get the next-available Job ID. This uses the given [maxConcurrentJobs] to adjust the number of available Job
     * IDs, as needed. It returns the next empty Job ID (ie, where its counter is equal to zero). If no such Job ID
     * is available, it clears the highest counter and returns the associated Job ID.
     */
    fun getJobId(maxConcurrentJobs: Int): Int {
        if (maxConcurrentJobs == 0 || counter.isEmpty()) {
            return DEFAULT_JOB_ID
        }
        adjustSize(maxConcurrentJobs)
        for (element in counter) {
            if (element.value == 0) {
                return element.key
            }
        }
        return if (size < maxConcurrentJobs) {
            val key = counter.maxByOrNull { it.key }!!.key + 1
            counter[key] = 0
            key
        } else {
            // Overwrites the longest-scheduled-job
            val entry = counter.maxByOrNull { it.value }!!
            counter[entry.key] = 0
            entry.key
        }
    }

    /**
     * Adjusts the number of available jobs. This ensures the map stays a fixed size based on the number of available
     * Job IDs allowed.
     */
    @VisibleForTesting
    fun adjustSize(maxConcurrentJobs: Int) {
        if (size > maxConcurrentJobs) {
            // Less jobs available
            for (i in size until maxConcurrentJobs) {
                counter.remove(DEFAULT_JOB_ID + i)
            }
        }
        this.size = maxConcurrentJobs
    }

    /**
     * Get the counter for the given Job ID
     */
    fun get(jobId: Int): Int = counter[jobId] ?: 0

    /**
     * Get and increment the counter for the given Job ID
     */
    fun incAndGet(jobId: Int): Int {
        val value = get(jobId) + 1
        counter[jobId] = value
        return value
    }

    /**
     * Reset the counter for the given Job ID
     */
    fun clear(jobId: Int) {
        counter.remove(jobId)
    }

}