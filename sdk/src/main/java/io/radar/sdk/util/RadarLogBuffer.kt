package io.radar.sdk.util

import android.content.Context
import io.radar.sdk.Radar
import io.radar.sdk.model.RadarLog
import java.util.Date

internal interface RadarLogBuffer {

    abstract val context: Context

    fun write(
        level: Radar.RadarLogLevel,
        type: Radar.RadarLogType?,
        message: String,
        createdAt: Date = Date()
    )

    fun getFlushableLogs(): Flushable<RadarLog>

    /**
     * Persist the logs to disk
     */
    fun persistLogs()

    fun setPersistentLogFeatureFlag(persistentLogFeatureFlag: Boolean)
}