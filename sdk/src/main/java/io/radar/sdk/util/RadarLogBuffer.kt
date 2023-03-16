package io.radar.sdk.util

import io.radar.sdk.Radar
import io.radar.sdk.model.RadarLog

internal interface RadarLogBuffer {

    /**
     * Write a log to the buffer
     *
     * @param[level] log level
     * @param[message] log message
     */
    fun write(level: Radar.RadarLogLevel, message: String, type: Radar.RadarLogType?)

    /**
     * Creates a stash of the logs currently in the buffer and returns them as a [Flushable] so that a successful
     * callback can cleanup this log buffer by deleting old log files.
     *
     * @return a [Flushable] containing all stored logs
     */
    fun getFlushableLogsStash(): Flushable<RadarLog>
}