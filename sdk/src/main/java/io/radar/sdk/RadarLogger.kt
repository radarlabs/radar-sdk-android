package io.radar.sdk

import android.content.Context
import android.util.Log
import io.radar.sdk.Radar.RadarLogLevel

internal class RadarLogger(
    private val context: Context
) {

    internal companion object {
        private const val TAG = "RadarLogger"
    }

    fun d(message: String, throwable: Throwable? = null) {
        val level = RadarSettings.getLogLevel(this.context)
        if (level >= RadarLogLevel.DEBUG) {
            Log.d(TAG, message, throwable)

            Radar.sendLog(message)
        }
    }

    fun i(message: String, throwable: Throwable? = null) {
        val level = RadarSettings.getLogLevel(this.context)
        if (level >= RadarLogLevel.INFO) {
            Log.i(TAG, message, throwable)

            Radar.sendLog(message)
        }
    }

    fun w(message: String, throwable: Throwable? = null) {
        val level = RadarSettings.getLogLevel(this.context)
        if (level >= RadarLogLevel.WARNING) {
            Log.w(TAG, message, throwable)

            Radar.sendLog(message)
        }
    }

    fun e(message: String, throwable: Throwable? = null) {
        val level = RadarSettings.getLogLevel(this.context)
        if (level >= RadarLogLevel.ERROR) {
            Log.e(TAG, message, throwable)

            Radar.sendLog(message)
        }
    }

}