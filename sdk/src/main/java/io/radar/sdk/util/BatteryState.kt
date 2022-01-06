package io.radar.sdk.util

import android.os.PowerManager
import io.radar.sdk.RadarBatteryManager

/**
 * Contains information about the battery state
 */
internal data class BatteryState(
    val isCharging: Boolean,
    val percent: Float,
    val powerSaveMode: Boolean?,
    val isIgnoringBatteryOptimizations: Boolean,
    val locationPowerSaveMode: Int,
    val isDeviceIdleMode: Boolean
) {

    /**
     * The performance state. This uses Android's [PowerManager] to gather information
     * about the current optimization settings that could affect the SDK's performance, and returns
     * a simplified status which identifies whether or not battery optimizations may be affecting
     * the SDK's responsiveness.
     */
    val performanceState: PerformanceState

    init {
        if (powerSaveMode != null) {
            val isAffectedByPowerSaver = powerSaveMode && !isIgnoringBatteryOptimizations
            val isLocationAffectedByPowerSaver = locationPowerSaveMode != RadarBatteryManager.locationUnaffected
            if (isDeviceIdleMode) {
                if (isAffectedByPowerSaver) {
                    if (isLocationAffectedByPowerSaver) {
                        //Idle, with power saver and location throttled
                        performanceState = PerformanceState.LOWEST
                    } else {
                        //Idle with Power Saver
                        performanceState = PerformanceState.LOW
                    }
                } else {
                    //Idle Only
                    performanceState = PerformanceState.IDLE
                }
            } else if (isAffectedByPowerSaver) {
                if (isLocationAffectedByPowerSaver) {
                    //Optimized And Location Throttled
                    performanceState = PerformanceState.LOCATIONS_LOW_PERFORMANCE
                } else {
                    performanceState = PerformanceState.OPTIMIZED
                }
            } else {
                performanceState = PerformanceState.OK
            }
        } else {
            performanceState = PerformanceState.OK
        }
    }

    /**
     * Get the string value for the current location power saving mode
     * @see [PowerManager.getLocationPowerSaveMode]
     */
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
}