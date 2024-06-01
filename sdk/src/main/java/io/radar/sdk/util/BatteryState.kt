package io.radar.sdk.util

import android.os.PowerManager
import io.radar.sdk.RadarBatteryManager

internal data class BatteryState(
    val isCharging: Boolean,
    val percent: Float,
    val powerSaveMode: Boolean?,
    val isIgnoringBatteryOptimizations: Boolean,
    val locationPowerSaveMode: Int,
    val isDeviceIdleMode: Boolean
) {

    val performanceState: PerformanceState

    init {
        if (powerSaveMode != null) {
            val isAffectedByPowerSaver = powerSaveMode && !isIgnoringBatteryOptimizations
            val isLocationAffectedByPowerSaver = locationPowerSaveMode != RadarBatteryManager.locationUnaffected
            when {
                isDeviceIdleMode -> {
                    performanceState = if (isAffectedByPowerSaver) {
                        if (isLocationAffectedByPowerSaver) {
                            PerformanceState.LOWEST
                        } else {
                            PerformanceState.LOW
                        }
                    } else {
                        PerformanceState.IDLE
                    }
                }
                isAffectedByPowerSaver -> {
                    performanceState = if (isLocationAffectedByPowerSaver) {
                        PerformanceState.LOCATIONS_LOW_PERFORMANCE
                    } else {
                        PerformanceState.OPTIMIZED
                    }
                }
                else -> {
                    performanceState = PerformanceState.OK
                }
            }
        } else {
            performanceState = PerformanceState.OK
        }
    }

    fun getPowerLocationPowerSaveModeString() : String {
        return when(locationPowerSaveMode) {
            0 -> "LOCATION_MODE_NO_CHANGE"
            1 -> "LOCATION_MODE_GPS_DISABLED_WHEN_SCREEN_OFF"
            2 -> "LOCATION_MODE_ALL_DISABLED_WHEN_SCREEN_OFF"
            3 -> "LOCATION_MODE_FOREGROUND_ONLY"
            4 -> "LOCATION_MODE_THROTTLE_REQUESTS_WHEN_SCREEN_OFF"
            else -> locationPowerSaveMode.toString()
        }
    }

    internal enum class PerformanceState {
        OK,
        OPTIMIZED,
        LOCATIONS_LOW_PERFORMANCE,
        IDLE,
        LOW,
        LOWEST
    }
}
