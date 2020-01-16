package io.radar.sdk

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import com.google.android.gms.location.*
import io.radar.sdk.RadarApiClient.RadarTrackApiCallback
import io.radar.sdk.Radar.RadarLocationCallback
import io.radar.sdk.Radar.RadarLocationSource
import io.radar.sdk.Radar.RadarStatus
import io.radar.sdk.RadarTrackingOptions.RadarTrackingOptionsDesiredAccuracy
import io.radar.sdk.model.RadarEvent
import io.radar.sdk.model.RadarUser
import org.json.JSONObject
import java.lang.Exception
import java.util.*
import kotlin.collections.ArrayList

@SuppressLint("MissingPermission")
internal class RadarLocationManager(
    private val context: Context,
    private val apiClient: RadarApiClient,
    private val logger: RadarLogger,
    internal var permissionsHelper: RadarPermissionsHelper = RadarPermissionsHelper()
) {

    internal var locationClient = FusedLocationProviderClient(context)
    internal var geofencingClient = GeofencingClient(context)
    private var started = false
    private var startedDesiredAccuracy = RadarTrackingOptionsDesiredAccuracy.NONE
    private var startedInterval = 0
    private var startedFastestInterval = 0
    private val callbacks = ArrayList<RadarLocationCallback>()

    internal companion object {
        internal const val GEOFENCE_MOVING_REQUEST_ID = "radar_moving"
        internal const val GEOFENCE_STOPPED_REQUEST_ID = "radar_stopped"
    }

    private fun addCallback(callback: RadarLocationCallback?) {
        if (callback == null) {
            return
        }

        synchronized(callbacks) {
            callbacks.add(callback)
        }

        Handler().postAtTime({
            synchronized(callbacks) {
                if (callbacks.contains(callback)) {
                    callback.onComplete(RadarStatus.ERROR_LOCATION)
                }
            }
        }, "timeout", SystemClock.uptimeMillis() + 20000L)
    }

    private fun callCallbacks(status: RadarStatus, location: Location? = null) {
        synchronized(callbacks) {
            if (callbacks.isEmpty()) {
                return
            }

            logger.d(this.context, "Calling callbacks | callbacks.size = ${callbacks.size}")

            for (callback in callbacks) {
                callback.onComplete(status, location, RadarState.getStopped(context))
            }
            callbacks.clear()
        }
    }

    fun getLocation(callback: RadarLocationCallback?) {
        getLocation(RadarTrackingOptionsDesiredAccuracy.MEDIUM, callback)
    }

    fun getLocation(desiredAccuracy: RadarTrackingOptionsDesiredAccuracy, callback: RadarLocationCallback?) {
        this.addCallback(callback)

        if (!permissionsHelper.fineLocationPermissionGranted(context)) {
            val errorIntent = RadarReceiver.createErrorIntent(RadarStatus.ERROR_PERMISSIONS)
            Radar.broadcastIntent(errorIntent)

            callback?.onComplete(RadarStatus.ERROR_PERMISSIONS)

            return
        }

        val locationManager = this

        val desiredPriority = when(desiredAccuracy) {
            RadarTrackingOptionsDesiredAccuracy.HIGH -> LocationRequest.PRIORITY_HIGH_ACCURACY
            RadarTrackingOptionsDesiredAccuracy.MEDIUM -> LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY
            RadarTrackingOptionsDesiredAccuracy.LOW -> LocationRequest.PRIORITY_LOW_POWER
            else -> LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY
        }

        val locationRequest = LocationRequest().apply {
            priority = desiredPriority
            interval = 5000L
            fastestInterval = 5000L
        }

        logger.d(this.context, "Requesting location")

        locationClient.requestLocationUpdates(locationRequest, object : LocationCallback() {
            override fun onLocationResult(result: LocationResult?) {
                locationClient.removeLocationUpdates(this)
                locationManager.handleLocation(result?.lastLocation, RadarLocationSource.FOREGROUND_LOCATION)
            }
        }, Looper.getMainLooper())
    }

    fun startTracking(options: RadarTrackingOptions = RadarTrackingOptions.EFFICIENT) {
        this.stopLocationUpdates()

        if (!permissionsHelper.fineLocationPermissionGranted(context)) {
            val errorIntent = RadarReceiver.createErrorIntent(RadarStatus.ERROR_PERMISSIONS)
            Radar.broadcastIntent(errorIntent)

            return
        }

        RadarSettings.setTracking(context, true)
        RadarSettings.setTrackingOptions(context, options)
        this.updateTracking()
    }

    fun stopTracking() {
        this.started = false
        RadarSettings.setTracking(context, false)
        this.updateTracking()
    }

    private fun startLocationUpdates(desiredAccuracy: RadarTrackingOptionsDesiredAccuracy, interval: Int, fastestInterval: Int) {
        if (!started || (desiredAccuracy != startedDesiredAccuracy) || (interval != startedInterval) || (fastestInterval != startedFastestInterval)) {
            val priority = when(desiredAccuracy) {
                RadarTrackingOptionsDesiredAccuracy.HIGH -> LocationRequest.PRIORITY_HIGH_ACCURACY
                RadarTrackingOptionsDesiredAccuracy.MEDIUM -> LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY
                RadarTrackingOptionsDesiredAccuracy.LOW -> LocationRequest.PRIORITY_LOW_POWER
                else -> LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY
            }

            val locationRequest = LocationRequest().apply {
                this.priority = priority
                this.interval = interval * 1000L
                this.fastestInterval = fastestInterval * 1000L
            }

            locationClient.requestLocationUpdates(locationRequest, RadarLocationReceiver.getLocationPendingIntent(context))

            this.started = true
            this.startedDesiredAccuracy = desiredAccuracy
            this.startedInterval = interval
            this.startedFastestInterval = fastestInterval
        }
    }

    private fun stopLocationUpdates() {
        locationClient.removeLocationUpdates(RadarLocationReceiver.getLocationPendingIntent(context))

        this.started = false
    }

    internal fun handleBootCompleted() {
        RadarState.setStopped(context, false)

        locationClient.lastLocation.addOnSuccessListener { location: Location? ->
            updateTracking(location)
        }.addOnFailureListener {
            updateTracking()
        }
    }

    internal fun updateTracking(location: Location? = null) {
        var tracking = RadarSettings.getTracking(context)
        val options = RadarSettings.getTrackingOptions(context)

        logger.d(this.context, "Updating tracking | options = $options; location = $location")

        val now = Date()
        if (!tracking && options.startTrackingAfter != null && options.startTrackingAfter!!.before(now)) {
            logger.d(this.context, "Starting time-based tracking | startTrackingAfter = ${options.startTrackingAfter}")

            tracking = true
            RadarSettings.setTracking(context, true)
        } else if (tracking && options.stopTrackingAfter != null && options.stopTrackingAfter!!.before(now)) {
            logger.d(this.context, "Stopping time-based tracking | startTrackingAfter = ${options.startTrackingAfter}")

            tracking = false
            RadarSettings.setTracking(context, false)
        }

        if (tracking) {
            val stopped = RadarState.getStopped(context)
            val justStopped = RadarState.getStopped(context)
            if (stopped) {
                if (options.desiredStoppedUpdateInterval == 0) {
                    this.stopLocationUpdates()
                } else {
                    this.startLocationUpdates(options.desiredAccuracy, options.desiredStoppedUpdateInterval, options.fastestStoppedUpdateInterval)
                }

                if (justStopped && options.useStoppedGeofence && location != null) {
                    this.updateGeofences(location, true)
                } else {
                    this.removeGeofences()
                }
            } else {
                if (options.desiredMovingUpdateInterval == 0) {
                    this.stopLocationUpdates()
                } else {
                    this.startLocationUpdates(options.desiredAccuracy, options.desiredMovingUpdateInterval, options.fastestMovingUpdateInterval)
                }

                if (options.useMovingGeofence && location != null) {
                    this.updateGeofences(location, false)
                } else {
                    this.removeGeofences()
                }
            }
        } else {
            this.stopLocationUpdates()
            this.removeGeofences()
        }
    }

    private fun updateGeofences(location: Location?, stopped: Boolean) {
        if (location == null) {
            return
        }

        this.removeGeofences()

        val options = RadarSettings.getTrackingOptions(context)

        if (stopped && options.useStoppedGeofence) {
            val geofence = Geofence.Builder()
                .setRequestId(GEOFENCE_STOPPED_REQUEST_ID)
                .setCircularRegion(location.latitude, location.longitude, options.stoppedGeofenceRadius.toFloat())
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_EXIT)
                .build()

            val request = GeofencingRequest.Builder()
                .addGeofence(geofence)
                .setInitialTrigger(Geofence.GEOFENCE_TRANSITION_EXIT)
                .build()

            geofencingClient.addGeofences(request, RadarLocationReceiver.getGeofencePendingIntent(context))
        } else if (!stopped && options.useMovingGeofence) {
            val geofence = Geofence.Builder()
                .setRequestId(GEOFENCE_MOVING_REQUEST_ID)
                .setCircularRegion(location.latitude, location.longitude, options.movingGeofenceRadius.toFloat())
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setLoiteringDelay(options.desiredMovingUpdateInterval * 1000)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_DWELL)
                .build()

            val request = GeofencingRequest.Builder()
                .addGeofence(geofence)
                .setInitialTrigger(Geofence.GEOFENCE_TRANSITION_DWELL)
                .build()

            geofencingClient.addGeofences(request, RadarLocationReceiver.getGeofencePendingIntent(context))
        }
    }

    private fun removeGeofences() {
        geofencingClient.removeGeofences(RadarLocationReceiver.getGeofencePendingIntent(context))
    }

    fun handleLocation(location: Location?, source: RadarLocationSource) {
        logger.d(this.context, "Handling location | location = $location")

        if (location == null || !RadarUtils.valid(location)) {
            logger.d(this.context, "Invalid location | source = $source; location = $location")

            val errorIntent = RadarReceiver.createErrorIntent(RadarStatus.ERROR_LOCATION)
            Radar.broadcastIntent(errorIntent)

            callCallbacks(RadarStatus.ERROR_LOCATION)

            return
        }

        val options = RadarSettings.getTrackingOptions(context)
        val wasStopped = RadarState.getStopped(context)
        var stopped: Boolean

        val force = (source == RadarLocationSource.FOREGROUND_LOCATION || source == RadarLocationSource.MANUAL_LOCATION)
        if (!force && location.accuracy > 1000 && options.desiredAccuracy != RadarTrackingOptionsDesiredAccuracy.LOW) {
            logger.d(this.context, "Skipping location: inaccurate | accuracy = ${location.accuracy}")

            this.updateTracking(location)

            return
        }

        Handler().removeCallbacksAndMessages("timeout")

        var distance = Float.MAX_VALUE
        val duration: Long
        if (options.stopDistance > 0 && options.stopDuration > 0) {
            var lastMovedLocation = RadarState.getLastMovedLocation(context)
            if (lastMovedLocation == null) {
                lastMovedLocation = location
                RadarState.setLastMovedLocation(context, lastMovedLocation)
            }
            var lastMovedAt = RadarState.getLastMovedAt(context)
            if (lastMovedAt == 0L) {
                lastMovedAt = location.time
                RadarState.setLastMovedAt(context, lastMovedAt)
            }
            if (!force && lastMovedAt > location.time) {
                logger.d(this.context, "Skipping location: old | lastMovedAt = $lastMovedAt; location.time = $location.time")

                return
            }
            distance = location.distanceTo(lastMovedLocation)
            duration = (location.time - lastMovedAt) / 1000
            stopped = (distance < options.stopDistance && duration > options.stopDuration)

            logger.d(this.context, "Calculating stopped | stopped = $stopped; distance = $distance; duration = $duration; location.time = ${location.time}; lastMovedAt = $lastMovedAt")

            if (distance > options.stopDistance) {
                RadarState.setLastMovedLocation(context, location)

                if (!stopped) {
                    RadarState.setLastMovedAt(context, location.time)
                }
            }
        } else {
            stopped = force || source == RadarLocationSource.GEOFENCE_DWELL
        }
        val justStopped = stopped && !wasStopped
        RadarState.setStopped(context, stopped)

        val locationIntent = RadarReceiver.createLocationIntent(location, stopped, source)
        Radar.broadcastIntent(locationIntent)

        if (source != RadarLocationSource.MANUAL_LOCATION) {
            this.updateTracking(location)
        }

        callCallbacks(RadarStatus.SUCCESS, location)

        var sendLocation = location

        val lastFailedStoppedLocation = RadarState.getLastFailedStoppedLocation(context)
        var replayed = false
        if (options.replay == RadarTrackingOptions.RadarTrackingOptionsReplay.REPLAY_STOPS && lastFailedStoppedLocation != null && !justStopped) {
            sendLocation = lastFailedStoppedLocation
            stopped = true
            replayed = true
            RadarState.setLastFailedStoppedLocation(context, null)

            logger.d(this.context, "Replaying location | location = $location; stopped = $stopped")
        }

        val lastSentAt = RadarState.getLastSentAt(context)
        val ignoreSync =
            lastSentAt == 0L || this.callbacks.count() > 0 || justStopped || replayed
        val now = System.currentTimeMillis()
        val lastSyncInterval = now - lastSentAt
        if (!ignoreSync) {
            if (!force && stopped && wasStopped && distance < options.stopDistance && (options.desiredStoppedUpdateInterval == 0 || options.sync != RadarTrackingOptions.RadarTrackingOptionsSync.ALL)) {
                logger.d(this.context, "Skipping sync: already stopped | stopped = $stopped; wasStopped = $wasStopped")

                return
            }

            if (lastSyncInterval < options.desiredSyncInterval) {
                logger.d(this.context, "Skipping sync: desired sync interval | desiredSyncInterval = ${options.desiredSyncInterval}; lastSyncInterval = $lastSyncInterval")

                return
            }

            if (!force && !justStopped && lastSyncInterval < 1000L) {
                logger.d(this.context, "Skipping sync: rate limit | justStopped = $justStopped; lastSyncInterval = $lastSyncInterval")

                return
            }

            if (options.sync == RadarTrackingOptions.RadarTrackingOptionsSync.NONE) {
                logger.d(this.context, "Skipping sync: sync mode | sync = ${options.sync}")

                return
            }

            val canExit = RadarState.getCanExit(context)
            if (!canExit && options.sync == RadarTrackingOptions.RadarTrackingOptionsSync.STOPS_AND_EXITS) {
                logger.d(this.context, "Skipping sync: can't exit | sync = ${options.sync}; canExit = $canExit")

                return
            }
        }
        RadarState.updateLastSentAt(context)

        if (source == RadarLocationSource.FOREGROUND_LOCATION) {
            return
        }

        if (lastSyncInterval < 1000L) {
            logger.d(this.context, "Scheduling location send")

            Handler().postAtTime({
                this.sendLocation(sendLocation, stopped, source, replayed)
            }, "send", SystemClock.uptimeMillis() + 2000L)
        } else {
            this.sendLocation(sendLocation, stopped, source, replayed)
        }
    }

    private fun sendLocation(location: Location, stopped: Boolean, source: RadarLocationSource, replayed: Boolean) {
        logger.d(this.context, "Sending location | source = $source; location = $location; stopped = $stopped; replayed = $replayed")

        this.apiClient.track(location, stopped, source, replayed, object : RadarTrackApiCallback {
            override fun onComplete(status: RadarStatus, res: JSONObject?, events: Array<RadarEvent>?, user: RadarUser?) {
                if (user != null) {
                    RadarSettings.setId(context, user._id)

                    val inGeofences = user.geofences != null && user.geofences.isNotEmpty()
                    val atPlace = user.place != null
                    val atHome = user.insights?.state?.home ?: false
                    val atOffice = user.insights?.state?.office ?: false
                    val canExit = inGeofences || atPlace || atHome || atOffice
                    RadarState.setCanExit(context, canExit)
                }
            }
        })
    }

}