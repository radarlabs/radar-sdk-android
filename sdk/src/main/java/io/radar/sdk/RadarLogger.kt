package io.radar.sdk

import android.util.Log
import io.radar.sdk.Radar.RadarLogLevel

internal class RadarLogger {

    internal companion object {
        private const val TAG = "RadarLogger"
    }

    fun d(message: String, throwable: Throwable? = null) {
        val level = Radar.settings.getLogLevel()
        if (level >= RadarLogLevel.DEBUG) {
            Log.d(TAG, message, throwable)

            Radar.sendLog(RadarLogLevel.DEBUG, message)
        }
    }

    fun i(message: String, throwable: Throwable? = null) {
        val level = Radar.settings.getLogLevel()
        if (level >= RadarLogLevel.INFO) {
            Log.i(TAG, message, throwable)

            Radar.sendLog(RadarLogLevel.INFO, message)
        }
    }

    fun w(message: String, throwable: Throwable? = null) {
        val level = Radar.settings.getLogLevel()
        if (level >= RadarLogLevel.WARNING) {
            Log.w(TAG, message, throwable)

            Radar.sendLog(RadarLogLevel.WARNING, message)
        }
    }

    fun e(message: String, throwable: Throwable? = null) {
        val level = Radar.settings.getLogLevel()
        if (level >= RadarLogLevel.ERROR) {
            Log.e(TAG, message, throwable)

            Radar.sendLog(RadarLogLevel.ERROR, message)
        }
    }

}