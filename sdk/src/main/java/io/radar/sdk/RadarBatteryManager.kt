package io.radar.sdk

import android.annotation.SuppressLint
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import io.radar.sdk.util.BatteryState

internal class RadarBatteryManager(
    private val context: Context
) {
    companion object {
        val locationUnaffected = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            PowerManager.LOCATION_MODE_NO_CHANGE
        } else {
            0
        }
    }

    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager?
    private val usageStatsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager?
    } else {
        null
    }

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
        return BatteryState(
            isCharging = isCharging,
            percent = batteryPct ?: 0F,
            powerSaveMode = isPowerSaveMode(),
            isIgnoringBatteryOptimizations = isIgnoringBatteryOptimizations(),
            locationPowerSaveMode = getLocationPowerSaveMode(),
            isDeviceIdleMode = isDeviceIdleMode()
        )
    }

    @SuppressLint("NewApi")
    fun getAppStandbyBucket(): Int? = usageStatsManager?.appStandbyBucket

    private fun isPowerSaveMode(): Boolean? {
        return powerManager?.isPowerSaveMode
    }

    private fun getLocationPowerSaveMode(): Int {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && powerManager != null) {
            return powerManager.locationPowerSaveMode
        }
        return locationUnaffected
    }

    private fun isDeviceIdleMode(): Boolean {
        if (powerManager != null) {
            return powerManager.isDeviceIdleMode
        }
        return false
    }

    private fun isIgnoringBatteryOptimizations(): Boolean {
        if (powerManager != null) {
            return powerManager.isIgnoringBatteryOptimizations(context.packageName)
        }
        return false
    }

}