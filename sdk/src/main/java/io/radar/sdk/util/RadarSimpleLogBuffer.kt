package io.radar.sdk.util

import io.radar.sdk.Radar
import io.radar.sdk.model.RadarLog
import java.util.concurrent.LinkedBlockingDeque

/**
 * Basic Log Buffer implementation that is backed by a field.
 */
internal class RadarSimpleLogBuffer : RadarLogBuffer {

    private companion object {
        const val MAXIMUM_CAPACITY = 1000
        const val HALF_CAPACITY = MAXIMUM_CAPACITY / 2
    }

    /**
     * Concurrency-safe list of logs with a maximum capacity of 1000
     */
    private val list = LinkedBlockingDeque<RadarLog>(MAXIMUM_CAPACITY)

    override fun write(level: Radar.RadarLogLevel, message: String) {
        if (!list.offer(RadarLog(level, message))) {
            purgeOldestLogs()
            list.put(RadarLog(level, message))
        }
    }

    override fun getFlushableLogsStash(): Flushable<RadarLog> {
        val logs = mutableListOf<RadarLog>()
        list.drainTo(logs)
        return object : Flushable<RadarLog> {

            override fun get(): List<RadarLog> {
                return logs
            }

            override fun onFlush(success: Boolean) {
                if (!success) {
                    //reverse order to ensure the logs will purge correctly (oldest logs purged first)
                    logs.reverse()
                    logs.forEach {
                        if (!list.offerFirst(it)) {
                            purgeOldestLogs()
                        }
                    }
                }
            }

        }
    }

    /**
     * Clears oldest logs and adds a "purged" log line
     */
    private fun purgeOldestLogs() {
        val logs = mutableListOf<RadarLog>()
        list.drainTo(logs, HALF_CAPACITY)
        write(Radar.RadarLogLevel.DEBUG, "------ purged oldest logs ------")
    }
}