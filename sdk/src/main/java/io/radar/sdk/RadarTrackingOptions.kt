package io.radar.sdk

import org.json.JSONObject
import java.util.Date

/**
 * An options class used to configure background tracking.
 *
 * @see [](https://radar.io/documentation/sdk#android-background)
 */
data class RadarTrackingOptions(
    /**
     * Determines the desired location update interval in seconds when stopped. Use 0 to shut down when stopped.
     *
     * Note that location updates may be delayed significantly by Android Doze Mode and App Standby and Background Location Limits, or if the device has connectivity issues, low battery, or wi-fi disabled. To avoid these restrictions, you can start a foreground service.
     */
    var desiredStoppedUpdateInterval: Int,

    /**
     * Determines the fastest location update interval in seconds when stopped.
     */
    var fastestStoppedUpdateInterval: Int,

    /**
     * Determines the desired location update interval in seconds when moving.
     *
     * Note that location updates may be delayed significantly by Android Doze Mode and App Standby and Background Location Limits, or if the device has connectivity issues, low battery, or wi-fi disabled. To avoid these restrictions, you can start a foreground service.
     */
    var desiredMovingUpdateInterval: Int,

    /**
     * Determines the fastest location update interval in seconds when stopped.
     */
    var fastestMovingUpdateInterval: Int,

    /**
     * Determines the desired sync interval in seconds.
     */
    var desiredSyncInterval: Int,

    /**
     * Determines the desired accuracy of location updates.
     */
    var desiredAccuracy: RadarTrackingOptionsDesiredAccuracy,

    /**
     * With `stopDistance`, determines the duration in seconds after which the device is considered stopped.
     */
    var stopDuration: Int,

    /**
     * With `stopDuration`, determines the distance in meters within which the device is considered stopped.
     */
    var stopDistance: Int,

    /**
     * Determines when to start tracking. Use `null` to start tracking when {@link Radar#startTracking} is called.
     */
    var startTrackingAfter: Date?,

    /**
     * Determines when to stop tracking. Use `null` to track until {@link Radar#stopTracking} is called.
     */
    var stopTrackingAfter: Date?,

    /**
     * Determines which failed location updates to replay to the server.
     */
    var replay: RadarTrackingOptionsReplay,

    /**
     * Determines which location updates to sync to the server.
     */
    var sync: RadarTrackingOptionsSync,

    /**
     * Determines whether to create a client geofence around the device's current location when stopped. See [](https://developer.android.com/training/location/geofencing).
     */
    var useStoppedGeofence: Boolean,

    /**
     * Determines the radius in meters of the client geofence around the device's current location when stopped.
     */
    var stoppedGeofenceRadius: Int,

    /**
     * Determines whether to create a client geofence around the device's current location when moving. See [](https://developer.android.com/training/location/geofencing).
     */
    var useMovingGeofence: Boolean,

    /**
     * Determines the radius in meters of the client geofence around the device's current location when stopped.
     */
    var movingGeofenceRadius: Int
) {

    /**
     * The location accuracy options.
     */
    enum class RadarTrackingOptionsDesiredAccuracy(internal val desiredAccuracy: Int) {
        /** Uses PRIORITY_HIGH_ACCURACY */
        HIGH(3),
        /** Uses PRIORITY_BALANCED_POWER_ACCURACY */
        MEDIUM(2),
        /** Uses PRIORITY_LOW_POWER */
        LOW(1),
        /** Uses PRIORITY_NO_POWER */
        NONE(0);

        internal companion object {
            fun fromInt(desiredAccuracy: Int?): RadarTrackingOptionsDesiredAccuracy {
                for (value in values()) {
                    if (desiredAccuracy == value.desiredAccuracy) {
                        return value
                    }
                }
                return MEDIUM
            }
        }
    }

    /**
     * The replay options for failed location updates.
     */
    enum class RadarTrackingOptionsReplay(internal val replay: Int) {
        /** Replays failed stops */
        REPLAY_STOPS(1),
        /** Replays no location updates */
        REPLAY_OFF(0);

        internal companion object {
            fun fromInt(replay: Int?): RadarTrackingOptionsReplay {
                for (value in values()) {
                    if (replay == value.replay) {
                        return value
                    }
                }
                return REPLAY_OFF
            }
        }
    }

    enum class RadarTrackingOptionsSync(internal val sync: Int) {
        /** Syncs no location updates to the server */
        NONE(0),
        /** Syncs only stops and exits to the server */
        STOPS_AND_EXITS(1),
        /** Syncs all location updates to the server */
        ALL(2);

        internal companion object {
            fun fromInt(sync: Int?): RadarTrackingOptionsSync {
                for (value in values()) {
                    if (sync == value.sync) {
                        return value
                    }
                }
                return STOPS_AND_EXITS
            }
        }
    }

    companion object {

        /**
         * A preset that updates every 30 seconds and syncs all location updates to the server.
         *
         * High battery usage and should be used with a foreground service. See [](https://developer.android.com/about/versions/oreo/background-location-limits).
         */
        @JvmField
        val CONTINUOUS = RadarTrackingOptions(
            desiredStoppedUpdateInterval = 30,
            fastestStoppedUpdateInterval = 30,
            desiredMovingUpdateInterval = 30,
            fastestMovingUpdateInterval = 30,
            desiredSyncInterval = 20,
            desiredAccuracy = RadarTrackingOptionsDesiredAccuracy.HIGH,
            stopDuration = 0,
            stopDistance = 0,
            startTrackingAfter = null,
            stopTrackingAfter = null,
            replay = RadarTrackingOptionsReplay.REPLAY_OFF,
            sync = RadarTrackingOptionsSync.ALL,
            useStoppedGeofence = false,
            stoppedGeofenceRadius = 0,
            useMovingGeofence = false,
            movingGeofenceRadius = 0
        )

        /**
         * A preset that updates as fast as every 2.5 minutes while moving, shuts down when stopped, and only syncs stops and exits to the server. Low battery usage, but may exceed Android vitals bad behavior thresholds for excessive wakeups and excessive wi-fi scans. See [](https://developer.android.com/topic/performance/vitals/wakeup.html) and [](https://developer.android.com/topic/performance/vitals/bg-wifi.html).
         *
         * Note that location updates may be delayed significantly by Android Doze Mode and App Standby and Background Location Limits, or if the device has connectivity issues, low battery, or wi-fi disabled.
         */
        @JvmField
        val RESPONSIVE = RadarTrackingOptions(
            desiredStoppedUpdateInterval = 0,
            fastestStoppedUpdateInterval = 0,
            desiredMovingUpdateInterval = 150,
            fastestMovingUpdateInterval = 150,
            desiredSyncInterval = 140,
            desiredAccuracy = RadarTrackingOptionsDesiredAccuracy.MEDIUM,
            stopDuration = 140,
            stopDistance = 70,
            startTrackingAfter = null,
            stopTrackingAfter = null,
            replay = RadarTrackingOptionsReplay.REPLAY_STOPS,
            sync = RadarTrackingOptionsSync.STOPS_AND_EXITS,
            useStoppedGeofence = true,
            stoppedGeofenceRadius = 200,
            useMovingGeofence = true,
            movingGeofenceRadius = 100
        )

        /**
         * A preset that updates as fast as every 6 minutes while moving, periodically when stopped, and only syncs stops and exits to the server. The default, lowest battery usage and will not exceed Android vitals bad behavior thresholds.
         *
         * Note that location updates may be delayed significantly by Android Doze Mode and App Standby and Background Location Limits, or if the device has connectivity issues, low battery, or wi-fi disabled.
         */
        @JvmField
        val EFFICIENT = RadarTrackingOptions(
            desiredStoppedUpdateInterval = 3600,
            fastestStoppedUpdateInterval = 1200,
            desiredMovingUpdateInterval = 1200,
            fastestMovingUpdateInterval = 360,
            desiredSyncInterval = 140,
            desiredAccuracy = RadarTrackingOptionsDesiredAccuracy.MEDIUM,
            stopDuration = 140,
            stopDistance = 70,
            startTrackingAfter = null,
            stopTrackingAfter = null,
            replay = RadarTrackingOptionsReplay.REPLAY_STOPS,
            sync = RadarTrackingOptionsSync.STOPS_AND_EXITS,
            useStoppedGeofence = false,
            stoppedGeofenceRadius = 0,
            useMovingGeofence = false,
            movingGeofenceRadius = 0
        )

        internal const val KEY_DESIRED_STOPPED_UPDATE_INTERVAL = "desiredStoppedUpdateInterval"
        internal const val KEY_FASTEST_STOPPED_UPDATE_INTERVAL = "fastestStoppedUpdateInterval"
        internal const val KEY_DESIRED_MOVING_UPDATE_INTERVAL = "desiredMovingUpdateInterval"
        internal const val KEY_FASTEST_MOVING_UPDATE_INTERVAL = "fastestMovingUpdateInterval"
        internal const val KEY_DESIRED_SYNC_INTERVAL = "desiredSyncInterval"
        internal const val KEY_DESIRED_ACCURACY = "desiredAccuracy"
        internal const val KEY_STOP_DURATION = "stopDuration"
        internal const val KEY_STOP_DISTANCE = "stopDistance"
        internal const val KEY_START_TRACKING_AFTER = "startTrackingAfter"
        internal const val KEY_STOP_TRACKING_AFTER = "stopTrackingAfter"
        internal const val KEY_REPLAY = "replay"
        internal const val KEY_SYNC = "sync"
        internal const val KEY_USE_STOPPED_GEOFENCE = "useStoppedGeofence"
        internal const val KEY_STOPPED_GEOFENCE_RADIUS = "stoppedGeofenceRadius"
        internal const val KEY_USE_MOVING_GEOFENCE = "useMovingGeofence"
        internal const val KEY_MOVING_GEOFENCE_RADIUS = "movingGeofenceRadius"

        fun fromJson(obj: JSONObject): RadarTrackingOptions {
            return RadarTrackingOptions(
                desiredStoppedUpdateInterval = obj.optInt(KEY_DESIRED_STOPPED_UPDATE_INTERVAL),
                fastestStoppedUpdateInterval = obj.optInt(KEY_FASTEST_STOPPED_UPDATE_INTERVAL),
                desiredMovingUpdateInterval = obj.optInt(KEY_DESIRED_MOVING_UPDATE_INTERVAL),
                fastestMovingUpdateInterval = obj.optInt(KEY_FASTEST_MOVING_UPDATE_INTERVAL),
                desiredSyncInterval = obj.optInt(KEY_DESIRED_SYNC_INTERVAL),
                desiredAccuracy = RadarTrackingOptionsDesiredAccuracy.fromInt(obj.optInt(KEY_DESIRED_ACCURACY)),
                stopDuration = obj.optInt(KEY_STOP_DURATION),
                stopDistance = obj.optInt(KEY_STOP_DISTANCE),
                startTrackingAfter = if (obj.has(KEY_START_TRACKING_AFTER)) Date(obj.optLong(KEY_START_TRACKING_AFTER)) else null,
                stopTrackingAfter = if (obj.has(KEY_STOP_TRACKING_AFTER)) Date(obj.optLong(KEY_STOP_TRACKING_AFTER)) else null,
                replay = RadarTrackingOptionsReplay.fromInt(obj.optInt(KEY_REPLAY)),
                sync = RadarTrackingOptionsSync.fromInt(obj.optInt(KEY_SYNC)),
                useStoppedGeofence = obj.optBoolean(KEY_USE_STOPPED_GEOFENCE),
                stoppedGeofenceRadius = obj.optInt(KEY_STOPPED_GEOFENCE_RADIUS, 200),
                useMovingGeofence = obj.optBoolean(KEY_USE_MOVING_GEOFENCE),
                movingGeofenceRadius = obj.optInt(KEY_MOVING_GEOFENCE_RADIUS, 200)
            )
        }

    }

    internal fun toJson(): JSONObject {
        val obj = JSONObject()
        obj.put(KEY_DESIRED_STOPPED_UPDATE_INTERVAL, desiredStoppedUpdateInterval)
        obj.put(KEY_FASTEST_STOPPED_UPDATE_INTERVAL, fastestStoppedUpdateInterval)
        obj.put(KEY_DESIRED_MOVING_UPDATE_INTERVAL, desiredMovingUpdateInterval)
        obj.put(KEY_FASTEST_MOVING_UPDATE_INTERVAL, fastestMovingUpdateInterval)
        obj.put(KEY_DESIRED_SYNC_INTERVAL, desiredSyncInterval)
        obj.put(KEY_DESIRED_ACCURACY, desiredAccuracy.desiredAccuracy)
        obj.put(KEY_STOP_DURATION, stopDuration)
        obj.put(KEY_STOP_DISTANCE, stopDistance)
        obj.put(KEY_START_TRACKING_AFTER, startTrackingAfter?.time)
        obj.put(KEY_STOP_TRACKING_AFTER, stopTrackingAfter?.time)
        obj.put(KEY_REPLAY, replay.replay)
        obj.put(KEY_SYNC, sync.sync)
        obj.put(KEY_USE_STOPPED_GEOFENCE, useStoppedGeofence)
        obj.put(KEY_STOPPED_GEOFENCE_RADIUS, stoppedGeofenceRadius)
        obj.put(KEY_USE_MOVING_GEOFENCE, useMovingGeofence)
        obj.put(KEY_MOVING_GEOFENCE_RADIUS, movingGeofenceRadius)
        return obj
    }

}