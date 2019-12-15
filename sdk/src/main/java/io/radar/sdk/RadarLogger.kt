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

            val logIntent = RadarReceiver.createLogIntent(message)
            Radar.broadcastIntent(logIntent)
        }
    }

    fun i(context: Context, message: String, throwable: Throwable? = null) {
        val level = RadarSettings.getLogLevel(context)
        if (level >= RadarLogLevel.INFO) {
            Log.i(TAG, message, throwable)

            val logIntent = RadarReceiver.createLogIntent(message)
            Radar.broadcastIntent(logIntent)
        }
    }

    fun w(context: Context, message: String, throwable: Throwable? = null) {
        val level = RadarSettings.getLogLevel(context)
        if (level >= RadarLogLevel.WARNING) {
            Log.w(TAG, message, throwable)

            val logIntent = RadarReceiver.createLogIntent(message)
            Radar.broadcastIntent(logIntent)
        }
    }

    fun e(context: Context, message: String, throwable: Throwable? = null) {
        val level = RadarSettings.getLogLevel(context)
        if (level >= RadarLogLevel.ERROR) {
            Log.e(TAG, message, throwable)

            val logIntent = RadarReceiver.createLogIntent(message)
            Radar.broadcastIntent(logIntent)
        }
    }

}