package io.radar.sdk.util

import android.content.Context
import io.radar.sdk.Radar
import io.radar.sdk.model.RadarLog
import java.util.Date

internal interface RadarLogBuffer {

    abstract val context: Context

    /**
     * Write a log to the buffer
     *
     * @param[level] log level
     * @param[message] log message
     */
    fun write(
        level: Radar.RadarLogLevel,
        type: Radar.RadarLogType?,
        message: String,
        createdAt: Date = Date()
    )

    /**
     * Creates a stash of the logs currently in the buffer and returns them as a [Flushable] so that a successful
     * callback can cleanup this log buffer by deleting old log files.
     *
     * @return a [Flushable] containing all stored logs
     */
    fun getFlushableLogs(): Flushable<RadarLog>

    /**
     * Persist the logs to disk
     */
    fun persistLogs()

    fun setPersistentLogFeatureFlag(persistentLogFeatureFlag: Boolean)
}