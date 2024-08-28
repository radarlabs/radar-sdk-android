package io.radar.sdk

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import io.radar.sdk.Radar.RadarLogLevel
import io.radar.sdk.Radar.RadarLogType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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

            Radar.sendLog(RadarLogLevel.DEBUG, message, type)
        }
    }

    fun i(message: String, type: RadarLogType? = null, throwable: Throwable? = null) {
        val level = RadarSettings.getLogLevel(this.context)
        if (level >= RadarLogLevel.INFO) {
            Log.i(TAG, message, throwable)

            Radar.sendLog(RadarLogLevel.INFO, message, type)
        }
    }

    fun w(message: String, type: RadarLogType? = null, throwable: Throwable? = null) {
        val level = RadarSettings.getLogLevel(this.context)
        if (level >= RadarLogLevel.WARNING) {
            Log.w(TAG, message, throwable)

            Radar.sendLog(RadarLogLevel.WARNING, message, type)
        }
    }

    fun e(message: String, type: RadarLogType? = null, throwable: Throwable? = null) {
        val level = RadarSettings.getLogLevel(this.context)
        if (level >= RadarLogLevel.ERROR) {
            Log.e(TAG, message, throwable)

            Radar.sendLog(RadarLogLevel.ERROR, message, type)
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    fun logPastTermination(){
        val activityManager = this.context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        // only run in foreground
        val appProcesses = activityManager.runningAppProcesses
        val isForeground = appProcesses?.any { it.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND && it.processName == context.packageName } ?: false
        if (!isForeground) {
            return
        }
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val sharedPreferences = this.context.getSharedPreferences("RadarSDK", Context.MODE_PRIVATE)
        val previousTimestamp = sharedPreferences.getLong("last_timestamp", 0)
        val currentTimestamp = System.currentTimeMillis()
        with(sharedPreferences.edit()) {
            putLong("last_timestamp", currentTimestamp)
            apply()
        }
        val batteryLevel = this.getBatteryLevel()
        
        val crashLists = activityManager.getHistoricalProcessExitReasons(null, 0, 10)
        if (crashLists.isNotEmpty()) {
            for (crashInfo in crashLists) {                
                if (crashInfo.timestamp > previousTimestamp) {
                    Radar.sendLog(RadarLogLevel.INFO, "App terminating | with reason: ${crashInfo.getDescription()} | at ${dateFormat.format(Date(crashInfo.timestamp))} | with ${batteryLevel * 100}% battery", null, Date(crashInfo.timestamp))
                    break
                }
            }
        }
    }

    fun getBatteryLevel(): Float {
        val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { ifilter ->
            this.context.registerReceiver(null, ifilter)
        }
        val level: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val batteryPct: Float = level / scale.toFloat()
        return batteryPct
    }

    fun logBackgrounding() {
        val batteryLevel = this.getBatteryLevel()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        this.i("App entering background | at ${dateFormat.format(Date())} | with ${batteryLevel * 100}% battery")
    }

     fun logResigningActive() {
        val batteryLevel = this.getBatteryLevel()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        this.i("App resigning active | at ${dateFormat.format(Date())} | with ${batteryLevel * 100}% battery")
    }

}