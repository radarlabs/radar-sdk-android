package io.radar.sdk

import android.content.Context
import android.util.Log
import io.radar.sdk.Radar.RadarLogLevel

internal class RadarLogger {

    internal companion object {
        private const val TAG = "RadarLogger"
    }

    fun d(context: Context, message: String, throwable: Throwable? = null) {
        val level = RadarSettings.getLogLevel(context)
        if (level >= RadarLogLevel.DEBUG) {
            Log.d(TAG, message, throwable)

            Radar.broadcastLogIntent(message)
        }
    }

    fun i(context: Context, message: String, throwable: Throwable? = null) {
        val level = RadarSettings.getLogLevel(context)
        if (level >= RadarLogLevel.INFO) {
            Log.i(TAG, message, throwable)

            Radar.broadcastLogIntent(message)
        }
    }

    fun w(context: Context, message: String, throwable: Throwable? = null) {
        val level = RadarSettings.getLogLevel(context)
        if (level >= RadarLogLevel.WARNING) {
            Log.w(TAG, message, throwable)

            Radar.broadcastLogIntent(message)
        }
    }

    fun e(context: Context, message: String, throwable: Throwable? = null) {
        val level = RadarSettings.getLogLevel(context)
        if (level >= RadarLogLevel.ERROR) {
            Log.e(TAG, message, throwable)

            Radar.broadcastLogIntent(message)
        }
    }

}