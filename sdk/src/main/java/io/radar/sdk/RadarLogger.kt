package io.radar.sdk

import android.content.Context
import android.util.Log
import io.radar.sdk.Radar.RadarLogLevel
import io.radar.sdk.Radar.RadarLogType

internal class RadarLogger(
    private val context: Context
) {

    internal companion object {
        private const val TAG = "RadarLogger"
    }

    fun d(message: String, type: RadarLogType? = null, throwable: Throwable? = null) {
        val level = RadarSettings.getLogLevel(this.context)
        if (level >= RadarLogLevel.DEBUG) {
            Log.d(TAG, message, throwable)

            Radar.sendLog(RadarLogLevel.DEBUG, message)
        }
    }

    fun i(message: String, type: RadarLogType? = null, throwable: Throwable? = null) {
        val level = RadarSettings.getLogLevel(this.context)
        if (level >= RadarLogLevel.INFO) {
            Log.i(TAG, message, throwable)

            Radar.sendLog(RadarLogLevel.INFO, message)
        }
    }

    fun w(message: String, type: RadarLogType? = null, throwable: Throwable? = null) {
        val level = RadarSettings.getLogLevel(this.context)
        if (level >= RadarLogLevel.WARNING) {
            Log.w(TAG, message, throwable)

            Radar.sendLog(RadarLogLevel.WARNING, message)
        }
    }

    fun e(message: String, type: RadarLogType? = null, throwable: Throwable? = null) {
        val level = RadarSettings.getLogLevel(this.context)
        if (level >= RadarLogLevel.ERROR) {
            Log.e(TAG, message, throwable)

            Radar.sendLog(RadarLogLevel.ERROR, message)
        }
    }

}