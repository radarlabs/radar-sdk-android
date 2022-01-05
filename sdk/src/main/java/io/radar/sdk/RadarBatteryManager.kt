package io.radar.sdk

import android.annotation.SuppressLint
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager

/**
 * Handles battery-related information within the Radar SDK
 */
internal class RadarBatteryManager(
    private val context: Context
) {

    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager?
    private val locationUnaffected = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        PowerManager.LOCATION_MODE_NO_CHANGE
    } else {
        0
    }
    private val usageStatsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager?
    } else {
        null
    }

    /**
     * Get the battery percentage of the device
     *
     * @see <a href="https://developer.android.com/training/monitoring-device-state/battery-monitoring">
     *     Android Battery Monitoring</a>
     */
    fun getBatteryState(): BatteryState {
        val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { filter ->
            context.registerReceiver(null, filter)
        }
        val status: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging: Boolean = status == BatteryManager.BATTERY_STATUS_CHARGING
                || status == BatteryManager.BATTERY_STATUS_FULL

        val batteryPct: Float? = batteryStatus?.let { intent ->
            val level: Int = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale: Int = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            level * 100 / scale.toFloat()
        }
        return BatteryState(isCharging, batteryPct ?: 0F, getPerformanceState())
    }

    @SuppressLint("NewApi")
    fun getAppStandbyBucket(): Int? = usageStatsManager?.appStandbyBucket

    /**
     * Get the performance state. This uses Android's [PowerManager] to gather information
     * about the current optimization settings that could affect the SDK's performance, and returns
     * a simplified status which identifies whether or not battery optimizations may be affecting
     * the SDK's responsiveness.
     */
    private fun getPerformanceState(): PerformanceState {
        val powerSaveMode = isPowerSaveMode()
        if (powerSaveMode != null) {
            val isAffectedByPowerSaver = powerSaveMode && !isIgnoringBatteryOptimizations()
            val isLocationAffectedByPowerSaver = getLocationPowerSaveMode() != locationUnaffected
            if (isDeviceIdleMode()) {
                if (isAffectedByPowerSaver) {
                    if (isLocationAffectedByPowerSaver) {
                        //Idle, with power saver and location throttled
                        return PerformanceState.LOWEST
                    }
                    //Idle with Power Saver
                    return PerformanceState.LOW
                } else {
                    //Idle Only
                    return PerformanceState.IDLE
                }
            } else if (isAffectedByPowerSaver) {
                if (isLocationAffectedByPowerSaver) {
                    //Optimized And Location Throttled
                    return PerformanceState.LOCATIONS_LOW_PERFORMANCE
                }
                return PerformanceState.OPTIMIZED
            }
        }
        return PerformanceState.OK
    }

    private fun isPowerSaveMode(): Boolean? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return powerManager?.isPowerSaveMode
        }
        return null
    }

    private fun getLocationPowerSaveMode(): Int {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && powerManager != null) {
            return powerManager.locationPowerSaveMode
        }
        return locationUnaffected
    }

    /**
     * Check if the device is Idle/doze mode is enabled
     */
    private fun isDeviceIdleMode(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && powerManager != null) {
            return powerManager.isDeviceIdleMode
        }
        return false
    }

    private fun isIgnoringBatteryOptimizations(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && powerManager != null) {
            return powerManager.isIgnoringBatteryOptimizations(context.packageName)
        }
        return false
    }

    internal enum class PerformanceState {

        /**
         * No performance optimizations are in affect, or the app is exempt
         */
        OK,

        /**
         * Performance Mode is optimized. Location settings for performance mode are either
         * fully allowed, or device does not support more granular performance settings.
         */
        OPTIMIZED,

        /**
         * Performance Mode is optimized, and location polling is affected by it.
         */
        LOCATIONS_LOW_PERFORMANCE,

        /**
         * Device is in an Idle state, and performance mode is not optimized.
         */
        IDLE,

        /**
         * Device is Idle and performance is optimized. Location settings for performance mode are either
         * fully allowed, or device does not support more granular performance settings.
         */
        LOW,

        /**
         * Device is idle, performance is optimized, and location polling is affected by the performance
         * optimization.
         */
        LOWEST

    }

    /**
     * Contains information about the battery state
     */
    internal class BatteryState(val isCharging: Boolean, val percent: Float, val performanceState: PerformanceState)

}