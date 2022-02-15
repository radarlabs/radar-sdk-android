package io.radar.sdk

import android.content.Context
import android.util.Log
import io.radar.sdk.Radar.RadarLogLevel
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@Suppress("TooManyFunctions")
internal class RadarLogger(
    private val context: Context,
    /**
     * Using an executor for logging allows the log operations to run in a separate thread
     */
    private val executor: ExecutorService = Executors.newSingleThreadExecutor()
) {

    internal companion object {
        private const val TAG = "RadarLogger"
    }

    fun d(message: String, throwable: Throwable? = null) = log(RadarLogLevel.DEBUG, message, throwable)

    fun d(message: String, prop: Pair<String, Any?>, throwable: Throwable? = null) = d(message, mapOf(prop), throwable)

    fun d(message: String, props: Map<String, Any?>, throwable: Throwable? = null) =
        log(RadarLogLevel.DEBUG, message, props, throwable)

    fun i(message: String, throwable: Throwable? = null) = log(RadarLogLevel.INFO, message, throwable)

    fun i(message: String, prop: Pair<String, Any?>, throwable: Throwable? = null) = i(message, mapOf(prop), throwable)

    fun i(message: String, props: Map<String, Any?>, throwable: Throwable? = null) =
        log(RadarLogLevel.INFO, message, props, throwable)

    fun w(message: String, throwable: Throwable? = null) = log(RadarLogLevel.WARNING, message, throwable)

    fun w(message: String, prop: Pair<String, Any?>, throwable: Throwable? = null) = w(message, mapOf(prop), throwable)

    fun w(message: String, props: Map<String, Any?>, throwable: Throwable? = null) =
        log(RadarLogLevel.WARNING, message, props, throwable)

    fun e(message: String, throwable: Throwable? = null) = log(RadarLogLevel.ERROR, message, throwable)

    fun e(message: String, prop: Pair<String, Any?>, throwable: Throwable? = null) = e(message, mapOf(prop), throwable)

    fun e(message: String, props: Map<String, Any?>, throwable: Throwable? = null) =
        log(RadarLogLevel.ERROR, message, props, throwable)

    private fun log(logLevel: RadarLogLevel, message: String, throwable: Throwable?) {
        val level = RadarSettings.getLogLevel(context)
        if (level >= logLevel) {
            executor.submit {
                print(logLevel, message, throwable)
            }
        }
    }

    private fun log(logLevel: RadarLogLevel, message: String, props: Map<String, Any?>, throwable: Throwable?) {
        val level = RadarSettings.getLogLevel(context)
        if (level >= logLevel) {
            executor.submit {
                var logMessage = "$message |"
                props.forEach { (key, value) ->
                    if (value != null) {
                        logMessage += " $key = $value;"
                    }
                }
                // Remove the last semicolon.
                logMessage = logMessage.substring(0, logMessage.length - 1)
                print(logLevel, logMessage, throwable)
            }
        }
    }

    private fun print(logLevel: RadarLogLevel, message: String, throwable: Throwable?) {
        Log.println(logLevel.priority, TAG, "$message\n${Log.getStackTraceString(throwable)}")
        Radar.sendLog(logLevel, message)
    }

}