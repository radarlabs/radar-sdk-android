package io.radar.sdk

import org.json.JSONObject
import java.util.Date

/**
 * An options class used to configure background tracking.
 *
 * @see [](https://radar.com/documentation/sdk/android)
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
     * Determines the fastest location update interval in seconds when moving.
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
    var syncGeofences: Boolean,

    /**
     * Determines how many nearby geofences to sync from the server to the client when `syncGeofences` is enabled.
     */
    var syncGeofencesLimit: Int,

    /**
     * If set, starts a foreground service and shows a notification during tracking.
     */
    var foregroundServiceEnabled: Boolean,

    /**
     * Determines whether to monitor beacons.
     */
    var beacons: Boolean
) {

    /**
     * The location accuracy options.
     */
    enum class RadarTrackingOptionsDesiredAccuracy(internal val desiredAccuracy: Int) {
        /** Uses PRIORITY_HIGH_ACCURACY. */
        HIGH(3),
        /** Uses PRIORITY_BALANCED_POWER_ACCURACY. */
        MEDIUM(2),
        /** Uses PRIORITY_LOW_POWER. */
        LOW(1),
        /** Uses PRIORITY_NO_POWER. */
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
        /** Replays all failed location updates. */
        ALL(2),
        /** Replays failed stops. */
        STOPS(1),
        /** Replays no location updates. */
        NONE(0);

        internal companion object {
            internal const val STOPS_STR = "stops"
            internal const val NONE_STR = "none"
            internal const val ALL_STR = "all"

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
                    ALL_STR -> ALL
                    else -> NONE
                }
            }
        }

        fun toRadarString(): String {
            return when(this) {
                STOPS -> STOPS_STR
                NONE -> NONE_STR
                ALL -> ALL_STR
            }
        }
    }

    enum class RadarTrackingOptionsSync(internal val sync: Int) {
        /** Does not sync location updates to the server. */
        NONE(0),
        /** Syncs only stops and exits to the server. */
        STOPS_AND_EXITS(1),
        /** Syncs all location updates to the server. */
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

    data class RadarTrackingOptionsForegroundService(
        /**
         * Determines the notification text. Defaults to `"Location tracking started"`.
         */
        val text: String? = null,

        /**
         * Determines the notification title. Optional.
         */
        val title: String? = null,

        /**
         * Determines the notification icon, like `R.drawable.ic_your_icon`. Optional, defaults to `applicationContext.applicationInfo.icon`.
         */
        val icon: Int? = null,

        /**
         * Determines when to show the notification. Use `false` to show the notification always, use `true` to show the notification only during location updates. Optional, defaults to `false`.
         */
        val updatesOnly: Boolean = false,

        /**
         * Determines the activity to start when the notification is tapped, like `"com.yourapp.MainActivity"`. Optional.
         */
        val activity: String? = null,

        /**
         * Determines the importance of the notification, one of `android.app.NotificationManager.IMPORTANCE_*`. Optional, defaults to `android.app.NotificationManager.IMPORTANCE_DEFAULT`.
         */
        val importance: Int? = null,

        /**
         * Determines the id of the notification. Optional, defaults to `20160525`.
         */
        val id: Int? = null,

        /**
         * Determines the user-facing channel name, which can be viewed in notification settings for the application.
         * Optional, defaults to `"Location Services"`.
         */
        val channelName: String? = null,

        /**
         * Determines the notification icon, like `R.drawable.ic_your_icon`. Optional, defaults to `applicationContext.applicationInfo.icon`.
         */
        var iconString: String? = null,

        /**
         * Determines the color notification icon. Optional.
         */
        var iconColor: String? = null,
    ) {

        companion object {
            internal const val KEY_FOREGROUND_SERVICE_TEXT = "text"
            internal const val KEY_FOREGROUND_SERVICE_TITLE = "title"
            internal const val KEY_FOREGROUND_SERVICE_ICON = "icon"
            internal const val KEY_FOREGROUND_SERVICE_ICON_STRING = "iconString"
            internal const val KEY_FOREGROUND_SERVICE_ICON_COLOR = "iconColor"
            internal const val KEY_FOREGROUND_SERVICE_UPDATES_ONLY = "updatesOnly"
            internal const val KEY_FOREGROUND_SERVICE_ACTIVITY = "activity"
            internal const val KEY_FOREGROUND_SERVICE_IMPORTANCE = "importance"
            internal const val KEY_FOREGROUND_SERVICE_ID = "id"
            internal const val KEY_FOREGROUND_SERVICE_CHANNEL_NAME = "channelName"

            @JvmStatic
            fun fromJson(obj: JSONObject?): RadarTrackingOptionsForegroundService? {
                if (obj == null) {
                    return null
                }

                val text = if (obj.isNull(KEY_FOREGROUND_SERVICE_TEXT)) null else obj.optString(KEY_FOREGROUND_SERVICE_TEXT)
                val title = if (obj.isNull(KEY_FOREGROUND_SERVICE_TITLE)) null else obj.optString(KEY_FOREGROUND_SERVICE_TITLE)
                val icon = if (obj.isNull(KEY_FOREGROUND_SERVICE_ICON)) null else obj.optInt(KEY_FOREGROUND_SERVICE_ICON)
                val iconString = if (obj.isNull(KEY_FOREGROUND_SERVICE_ICON_STRING)) null else obj.optString(KEY_FOREGROUND_SERVICE_ICON_STRING)
                val iconColor = if (obj.isNull(KEY_FOREGROUND_SERVICE_ICON_COLOR)) null else obj.optString(KEY_FOREGROUND_SERVICE_ICON_COLOR)
                val updatesOnly: Boolean = obj.optBoolean(KEY_FOREGROUND_SERVICE_UPDATES_ONLY)
                val activity = if (obj.isNull(KEY_FOREGROUND_SERVICE_ACTIVITY)) null else obj.optString(KEY_FOREGROUND_SERVICE_ACTIVITY)
                val importance = if (obj.isNull(KEY_FOREGROUND_SERVICE_IMPORTANCE)) null else obj.optInt(KEY_FOREGROUND_SERVICE_IMPORTANCE)
                val id = if (obj.isNull(KEY_FOREGROUND_SERVICE_ID)) null else obj.optInt(KEY_FOREGROUND_SERVICE_ID)
                val channelName = if (obj.isNull(KEY_FOREGROUND_SERVICE_CHANNEL_NAME)) null else obj.optString(KEY_FOREGROUND_SERVICE_CHANNEL_NAME)
                return RadarTrackingOptionsForegroundService(text, title, icon, updatesOnly, activity, importance, id, channelName, iconString, iconColor)
            }
        }

        fun toJson(): JSONObject {
            val obj = JSONObject()

            obj.put(KEY_FOREGROUND_SERVICE_TEXT, text)
            obj.put(KEY_FOREGROUND_SERVICE_TITLE, title)
            obj.put(KEY_FOREGROUND_SERVICE_ICON, icon)
            obj.put(KEY_FOREGROUND_SERVICE_ICON_STRING, iconString)
            obj.put(KEY_FOREGROUND_SERVICE_ICON_COLOR, iconColor)
            obj.put(KEY_FOREGROUND_SERVICE_ACTIVITY, activity)
            obj.put(KEY_FOREGROUND_SERVICE_UPDATES_ONLY, updatesOnly)
            obj.put(KEY_FOREGROUND_SERVICE_IMPORTANCE, importance)
            obj.put(KEY_FOREGROUND_SERVICE_ID, id)
            obj.put(KEY_FOREGROUND_SERVICE_CHANNEL_NAME, channelName)
            return obj
        }

    }

    companion object {

        /**
         * Updates about every 30 seconds while moving or stopped. Starts a foreground service. Moderate battery usage.
         */
        @JvmField
        val CONTINUOUS = RadarTrackingOptions(
            desiredStoppedUpdateInterval = 30,
            fastestStoppedUpdateInterval = 30,
            desiredMovingUpdateInterval = 30,
            fastestMovingUpdateInterval = 30,
            desiredSyncInterval = 20,
            desiredAccuracy = RadarTrackingOptionsDesiredAccuracy.HIGH,
            stopDuration = 140,
            stopDistance = 70,
            startTrackingAfter = null,
            stopTrackingAfter = null,
            replay = RadarTrackingOptionsReplay.NONE,
            sync = RadarTrackingOptionsSync.ALL,
            useStoppedGeofence = false,
            stoppedGeofenceRadius = 0,
            useMovingGeofence = false,
            movingGeofenceRadius = 0,
            syncGeofences = true,
            syncGeofencesLimit = 0,
            foregroundServiceEnabled = true,
            beacons = false
        )

        /**
         * Updates about every 2.5 minutes while moving and shuts down when stopped to save battery. Once stopped, the device will need to move more than 100 meters to wake up and start moving again. Low battery usage.
         *
         * Note that location updates may be delayed significantly by Doze Mode, App Standby, and Background Location Limits, or if the device has connectivity issues, low battery, or wi-fi disabled.
         */
        @JvmField
        val RESPONSIVE = RadarTrackingOptions(
            desiredStoppedUpdateInterval = 0,
            fastestStoppedUpdateInterval = 0,
            desiredMovingUpdateInterval = 150,
            fastestMovingUpdateInterval = 30,
            desiredSyncInterval = 20,
            desiredAccuracy = RadarTrackingOptionsDesiredAccuracy.MEDIUM,
            stopDuration = 140,
            stopDistance = 70,
            startTrackingAfter = null,
            stopTrackingAfter = null,
            replay = RadarTrackingOptionsReplay.STOPS,
            sync = RadarTrackingOptionsSync.ALL,
            useStoppedGeofence = true,
            stoppedGeofenceRadius = 100,
            useMovingGeofence = true,
            movingGeofenceRadius = 100,
            syncGeofences = true,
            syncGeofencesLimit = 10,
            foregroundServiceEnabled = false,
            beacons = false
        )

        /**
         * Updates as fast as every 6 minutes while moving and periodically when stopped. Once stopped, the device will need to move more than 100 meters and wait for at least 15 minutes to wake up and start moving again. Lowest battery usage.
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
            sync = RadarTrackingOptionsSync.ALL,
            useStoppedGeofence = false,
            stoppedGeofenceRadius = 0,
            useMovingGeofence = false,
            movingGeofenceRadius = 0,
            syncGeofences = true,
            syncGeofencesLimit = 10,
            foregroundServiceEnabled = false,
            beacons = false
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
        internal const val KEY_SYNC_GEOFENCES_LIMIT = "syncGeofencesLimit"
        internal const val KEY_FOREGROUND_SERVICE_ENABLED = "foregroundServiceEnabled"
        internal const val KEY_BEACONS = "beacons"

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
                startTrackingAfter = if (obj.has(KEY_START_TRACKING_AFTER)) {
                    var startTrackingAfterLong = obj.optLong(
                        KEY_START_TRACKING_AFTER
                    )
                    if (startTrackingAfterLong != 0L) {
                        Date(startTrackingAfterLong)
                    } else {
                        RadarUtils.isoStringToDate(obj.optString(KEY_START_TRACKING_AFTER))
                    }
                } else null,
                stopTrackingAfter = if (obj.has(KEY_STOP_TRACKING_AFTER)) {
                    var stopTrackingAfterLong = obj.optLong(
                        KEY_STOP_TRACKING_AFTER
                    )
                    if (stopTrackingAfterLong != 0L) {
                        Date(stopTrackingAfterLong)
                    } else {
                        RadarUtils.isoStringToDate(obj.optString(KEY_STOP_TRACKING_AFTER))
                    }
                } else null,
                replay = replay,
                sync = sync,
                useStoppedGeofence = obj.optBoolean(KEY_USE_STOPPED_GEOFENCE),
                stoppedGeofenceRadius = obj.optInt(KEY_STOPPED_GEOFENCE_RADIUS, 100),
                useMovingGeofence = obj.optBoolean(KEY_USE_MOVING_GEOFENCE),
                movingGeofenceRadius = obj.optInt(KEY_MOVING_GEOFENCE_RADIUS, 100),
                syncGeofences = obj.optBoolean(KEY_SYNC_GEOFENCES),
                syncGeofencesLimit = obj.optInt(KEY_SYNC_GEOFENCES_LIMIT, 10),
                foregroundServiceEnabled = obj.optBoolean(KEY_FOREGROUND_SERVICE_ENABLED, false),
                beacons = obj.optBoolean(KEY_BEACONS)
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
        obj.put(KEY_SYNC_GEOFENCES_LIMIT, syncGeofencesLimit)
        obj.put(KEY_FOREGROUND_SERVICE_ENABLED, foregroundServiceEnabled)
        obj.put(KEY_BEACONS, beacons)
        return obj
    }

}
