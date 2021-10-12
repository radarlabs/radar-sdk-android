package io.radar.sdk

import android.util.Log
import io.radar.sdk.Radar.RadarLogLevel
import java.util.concurrent.Executors

internal class RadarLogger(
    private val app: RadarApplication
) {

    /**
     * Using an executor for logging allows the log operations to run in a separate thread
     */
    private val executor = Executors.newSingleThreadExecutor()

    internal companion object {
        private const val TAG = "RadarLogger"
    }

    fun d(message: String, props: Map<String, Any?>, throwable: Throwable? = null) {
        val level = app.settings.getLogLevel()
        if (level >= RadarLogLevel.DEBUG) {
            executor.submit {
                var logMessage = "$message |"
                props.forEach { (key, value) ->
                    if (value != null) {
                        logMessage += " $key = $value;"
                    }
                }
                //remove the last semicolon
                logMessage = logMessage.substring(0, logMessage.length - 1)

                Log.d(TAG, logMessage, throwable)
                app.receiver?.onLog(app, logMessage)
            }
        }
    }

    fun d(message: String, throwable: Throwable? = null) {
        val level = app.settings.getLogLevel()
        if (level >= RadarLogLevel.DEBUG) {
            Log.d(TAG, message, throwable)
            app.receiver?.onLog(app, message)
        }
    }

    fun i(message: String, throwable: Throwable? = null) {
        val level = app.settings.getLogLevel()
        if (level >= RadarLogLevel.INFO) {
            Log.i(TAG, message, throwable)
            app.receiver?.onLog(app, message)
        }
    }

    fun w(message: String, throwable: Throwable? = null) {
        val level = app.settings.getLogLevel()
        if (level >= RadarLogLevel.WARNING) {
            Log.w(TAG, message, throwable)
            app.receiver?.onLog(app, message)
        }
    }

    fun e(message: String, throwable: Throwable? = null) {
        val level = app.settings.getLogLevel()
        if (level >= RadarLogLevel.ERROR) {
            Log.e(TAG, message, throwable)
            app.receiver?.onLog(app, message)
        }
    }

}