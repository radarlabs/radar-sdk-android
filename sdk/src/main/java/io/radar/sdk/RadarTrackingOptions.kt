package io.radar.sdk

import org.json.JSONObject
import java.util.Date

/**
 * An options class used to configure background tracking.
 *
 * @see [](https://radar.io/documentation/sdk#android-background)
 */
data class RadarTrackingOptions(
    var desiredStoppedUpdateInterval: Int,
    var fastestStoppedUpdateInterval: Int,
    var desiredMovingUpdateInterval: Int,
    var fastestMovingUpdateInterval: Int,
    var desiredSyncInterval: Int,
    var desiredAccuracy: RadarTrackingOptionsDesiredAccuracy,
    var stopDuration: Int,
    var stopDistance: Int,
    var startTrackingAfter: Date?,
    var stopTrackingAfter: Date?,
    var replay: RadarTrackingOptionsReplay,
    var sync: RadarTrackingOptionsSync,
    var useStoppedGeofence: Boolean,
    var stoppedGeofenceRadius: Int,
    var useMovingGeofence: Boolean,
    var movingGeofenceRadius: Int
) {

    enum class RadarTrackingOptionsDesiredAccuracy(internal val desiredAccuracy: Int) {
        HIGH(3),
        MEDIUM(2),
        LOW(1);

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

    enum class RadarTrackingOptionsReplay(internal val replay: Int) {
        REPLAY_STOPS(1),
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
        NONE(0),
        STOPS_AND_EXITS(1),
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

        @JvmField
        val EFFICIENT = RadarTrackingOptions(
            desiredStoppedUpdateInterval = 0,
            fastestStoppedUpdateInterval = 0,
            desiredMovingUpdateInterval = 900,
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