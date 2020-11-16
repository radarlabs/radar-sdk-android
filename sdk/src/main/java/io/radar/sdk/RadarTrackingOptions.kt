package io.radar.sdk

import org.json.JSONObject
import java.util.Date

/**
 * An options class used to configure background tracking.
 *
 * @see [](https://radar.io/documentation/sdk/android)
 */
data class RadarTrackingOptions(
    /**
     * Determines the desired location update interval in seconds when stopped. Use 0 to shut down when stopped.
     *
     * Note that location updates may be delayed significantly by Doze Mode, App Standby, and Background Location Limits, or if the device has connectivity issues, low battery, or wi-fi disabled. To avoid these restrictions, you can start a foreground service.
     */
    var desiredStoppedUpdateInterval: Int,

    /**
     * Determines the fastest location update interval in seconds when stopped.
     */
    var fastestStoppedUpdateInterval: Int,

    /**
     * Determines the desired location update interval in seconds when moving.
     *
     * Note that location updates may be delayed significantly by Doze Mode, App Standby, and Background Location Limits, or if the device has connectivity issues, low battery, or wi-fi disabled. To avoid these restrictions, you can start a foreground service.
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
     * Determines when to start tracking. Use `null` to start tracking when `startTracking()` is called.
     */
    var startTrackingAfter: Date?,

    /**
     * Determines when to stop tracking. Use `null` to track until `stopTracking()` is called.
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
     * Determines the radius in meters of the client geofence around the device's current location when moving.
     */
    var movingGeofenceRadius: Int,

    /**
     * Determines whether to sync nearby geofences from the server to the client to improve responsiveness.
     */
    var syncGeofences: Boolean
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
            internal const val HIGH_STR = "high"
            internal const val MEDIUM_STR = "medium"
            internal const val LOW_STR = "low"
            internal const val NONE_STR = "none"

            fun fromInt(desiredAccuracy: Int?): RadarTrackingOptionsDesiredAccuracy {
                for (value in values()) {
                    if (desiredAccuracy == value.desiredAccuracy) {
                        return value
                    }
                }
                return MEDIUM
            }

            fun fromRadarString(desiredAccuracy: String?): RadarTrackingOptionsDesiredAccuracy {
                return when(desiredAccuracy) {
                    HIGH_STR -> HIGH
                    MEDIUM_STR -> MEDIUM
                    LOW_STR -> LOW
                    NONE_STR -> NONE
                    else -> MEDIUM
                }
            }
        }

        fun toRadarString(): String {
            return when(this) {
                HIGH -> HIGH_STR
                MEDIUM -> MEDIUM_STR
                LOW -> LOW_STR
                NONE -> NONE_STR
            }
        }
    }

    /**
     * The replay options for failed location updates.
     */
    enum class RadarTrackingOptionsReplay(internal val replay: Int) {
        /** Replays failed stops */
        STOPS(1),
        /** Replays no location updates */
        NONE(0);

        internal companion object {
            internal const val STOPS_STR = "stops"
            internal const val NONE_STR = "none"

            fun fromInt(replay: Int?): RadarTrackingOptionsReplay {
                for (value in values()) {
                    if (replay == value.replay) {
                        return value
                    }
                }
                return NONE
            }

            fun fromRadarString(replay: String?): RadarTrackingOptionsReplay {
                return when(replay) {
                    STOPS_STR -> STOPS
                    NONE_STR -> NONE
                    else -> NONE
                }
            }
        }

        fun toRadarString(): String {
            return when(this) {
                STOPS -> STOPS_STR
                NONE -> NONE_STR
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
            internal const val NONE_STR = "none"
            internal const val STOPS_AND_EXITS_STR = "stopsAndExits"
            internal const val ALL_STR = "all"

            fun fromInt(sync: Int?): RadarTrackingOptionsSync {
                for (value in values()) {
                    if (sync == value.sync) {
                        return value
                    }
                }
                return STOPS_AND_EXITS
            }

            fun fromRadarString(sync: String?): RadarTrackingOptionsSync {
                return when(sync) {
                    ALL_STR -> ALL
                    STOPS_AND_EXITS_STR -> STOPS_AND_EXITS
                    NONE_STR -> NONE
                    else -> STOPS_AND_EXITS
                }
            }
        }

        fun toRadarString(): String {
            return when(this) {
                ALL -> ALL_STR
                STOPS_AND_EXITS -> STOPS_AND_EXITS_STR
                NONE -> NONE_STR
            }
        }
    }

    companion object {

        /**
         * A preset that updates every 30 seconds and syncs all location updates to the server. High battery usage. Should be used with a foreground service. See [](https://developer.android.com/about/versions/oreo/background-location-limits).
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
            replay = RadarTrackingOptionsReplay.NONE,
            sync = RadarTrackingOptionsSync.ALL,
            useStoppedGeofence = false,
            stoppedGeofenceRadius = 0,
            useMovingGeofence = false,
            movingGeofenceRadius = 0,
            syncGeofences = false
        )

        /**
         * A preset that updates as fast as every 2.5 minutes while moving, shuts down when stopped, and only syncs stops and exits to the server. Must move at least 200 meters to start moving again after a stop. Low battery usage, but may exceed Android vitals bad behavior thresholds for excessive wakeups and excessive wi-fi scans. See [](https://developer.android.com/topic/performance/vitals/wakeup.html) and [](https://developer.android.com/topic/performance/vitals/bg-wifi.html).
         *
         * Note that location updates may be delayed significantly by Doze Mode, App Standby, and Background Location Limits, or if the device has connectivity issues, low battery, or wi-fi disabled.
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
            replay = RadarTrackingOptionsReplay.STOPS,
            sync = RadarTrackingOptionsSync.STOPS_AND_EXITS,
            useStoppedGeofence = true,
            stoppedGeofenceRadius = 100,
            useMovingGeofence = true,
            movingGeofenceRadius = 100,
            syncGeofences = false
        )

        /**
         * A preset that updates as fast as every 6 minutes while moving, periodically when stopped, and only syncs stops and exits to the server. Must move a significant distance to start moving again after a stop. Lowest battery usage and will not exceed Android vitals bad behavior thresholds. Recommended.
         *
         * Note that location updates may be delayed significantly by Doze Mode, App Standby, and Background Location Limits, or if the device has connectivity issues, low battery, or wi-fi disabled.
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
            replay = RadarTrackingOptionsReplay.STOPS,
            sync = RadarTrackingOptionsSync.STOPS_AND_EXITS,
            useStoppedGeofence = false,
            stoppedGeofenceRadius = 0,
            useMovingGeofence = false,
            movingGeofenceRadius = 0,
            syncGeofences = false
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
        internal const val KEY_SYNC_GEOFENCES = "syncGeofences"

        @JvmStatic
        fun fromJson(obj: JSONObject): RadarTrackingOptions {
            val desiredAccuracy = if (obj.has(KEY_DESIRED_ACCURACY) && obj.get(KEY_DESIRED_ACCURACY) is String) {
                RadarTrackingOptionsDesiredAccuracy.fromRadarString(obj.optString(KEY_DESIRED_ACCURACY))
            } else {
                RadarTrackingOptionsDesiredAccuracy.fromInt(obj.optInt(KEY_DESIRED_ACCURACY))
            }

            val replay = if (obj.has(KEY_REPLAY) && obj.get(KEY_REPLAY) is String) {
                RadarTrackingOptionsReplay.fromRadarString(obj.optString(KEY_REPLAY))
            } else {
                RadarTrackingOptionsReplay.fromInt(obj.optInt(KEY_REPLAY))
            }

            val sync = if (obj.has(KEY_SYNC) && obj.get(KEY_SYNC) is String) {
                RadarTrackingOptionsSync.fromRadarString(obj.optString(KEY_SYNC))
            } else {
                RadarTrackingOptionsSync.fromInt(obj.optInt(KEY_SYNC))
            }

            return RadarTrackingOptions(
                desiredStoppedUpdateInterval = obj.optInt(KEY_DESIRED_STOPPED_UPDATE_INTERVAL),
                fastestStoppedUpdateInterval = obj.optInt(KEY_FASTEST_STOPPED_UPDATE_INTERVAL),
                desiredMovingUpdateInterval = obj.optInt(KEY_DESIRED_MOVING_UPDATE_INTERVAL),
                fastestMovingUpdateInterval = obj.optInt(KEY_FASTEST_MOVING_UPDATE_INTERVAL),
                desiredSyncInterval = obj.optInt(KEY_DESIRED_SYNC_INTERVAL),
                desiredAccuracy = desiredAccuracy,
                stopDuration = obj.optInt(KEY_STOP_DURATION),
                stopDistance = obj.optInt(KEY_STOP_DISTANCE),
                startTrackingAfter = if (obj.has(KEY_START_TRACKING_AFTER)) Date(obj.optLong(KEY_START_TRACKING_AFTER)) else null,
                stopTrackingAfter = if (obj.has(KEY_STOP_TRACKING_AFTER)) Date(obj.optLong(KEY_STOP_TRACKING_AFTER)) else null,
                replay = replay,
                sync = sync,
                useStoppedGeofence = obj.optBoolean(KEY_USE_STOPPED_GEOFENCE),
                stoppedGeofenceRadius = obj.optInt(KEY_STOPPED_GEOFENCE_RADIUS, 100),
                useMovingGeofence = obj.optBoolean(KEY_USE_MOVING_GEOFENCE),
                movingGeofenceRadius = obj.optInt(KEY_MOVING_GEOFENCE_RADIUS, 100),
                syncGeofences = obj.optBoolean(KEY_SYNC_GEOFENCES)
            )
        }

    }

    fun toJson(): JSONObject {
        val obj = JSONObject()
        obj.put(KEY_DESIRED_STOPPED_UPDATE_INTERVAL, desiredStoppedUpdateInterval)
        obj.put(KEY_FASTEST_STOPPED_UPDATE_INTERVAL, fastestStoppedUpdateInterval)
        obj.put(KEY_DESIRED_MOVING_UPDATE_INTERVAL, desiredMovingUpdateInterval)
        obj.put(KEY_FASTEST_MOVING_UPDATE_INTERVAL, fastestMovingUpdateInterval)
        obj.put(KEY_DESIRED_SYNC_INTERVAL, desiredSyncInterval)
        obj.put(KEY_DESIRED_ACCURACY, desiredAccuracy.toRadarString())
        obj.put(KEY_STOP_DURATION, stopDuration)
        obj.put(KEY_STOP_DISTANCE, stopDistance)
        obj.put(KEY_START_TRACKING_AFTER, startTrackingAfter?.time)
        obj.put(KEY_STOP_TRACKING_AFTER, stopTrackingAfter?.time)
        obj.put(KEY_REPLAY, replay.toRadarString())
        obj.put(KEY_SYNC, sync.toRadarString())
        obj.put(KEY_USE_STOPPED_GEOFENCE, useStoppedGeofence)
        obj.put(KEY_STOPPED_GEOFENCE_RADIUS, stoppedGeofenceRadius)
        obj.put(KEY_USE_MOVING_GEOFENCE, useMovingGeofence)
        obj.put(KEY_MOVING_GEOFENCE_RADIUS, movingGeofenceRadius)
        obj.put(KEY_SYNC_GEOFENCES, syncGeofences)
        return obj
    }

}