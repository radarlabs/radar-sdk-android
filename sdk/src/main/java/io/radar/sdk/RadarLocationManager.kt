package io.radar.sdk

import android.annotation.SuppressLint
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Build
import com.google.android.gms.location.*
import io.radar.sdk.Radar.RadarLocationCallback
import io.radar.sdk.Radar.RadarLocationSource
import io.radar.sdk.Radar.RadarStatus
import io.radar.sdk.RadarApiClient.RadarTrackApiCallback
import io.radar.sdk.RadarTrackingOptions.RadarTrackingOptionsDesiredAccuracy
import io.radar.sdk.model.*
import org.json.JSONObject
import java.util.*
import kotlin.collections.ArrayList

@SuppressLint("MissingPermission")
internal class RadarLocationManager(
    private val context: Context,
    private val apiClient: RadarApiClient,
    private val logger: RadarLogger,
    internal var permissionsHelper: RadarPermissionsHelper = RadarPermissionsHelper()
) {

    @SuppressLint("VisibleForTests")
    internal var locationClient = FusedLocationProviderClient(context)
    @SuppressLint("VisibleForTests")
    internal var geofencingClient = GeofencingClient(context)
    private var started = false
    private var startedDesiredAccuracy = RadarTrackingOptionsDesiredAccuracy.NONE
    private var startedInterval = 0
    private var startedFastestInterval = 0
    private val callbacks = ArrayList<RadarLocationCallback>()

    internal companion object {
        private const val BUBBLE_MOVING_GEOFENCE_REQUEST_ID = "radar_moving"
        private const val BUBBLE_STOPPED_GEOFENCE_REQUEST_ID = "radar_stopped"
        private const val SYNCED_GEOFENCES_REQUEST_ID_PREFIX = "radar_sync"
    }

    private fun addCallback(callback: RadarLocationCallback?) {
        if (callback == null) {
            return
        }

        synchronized(callbacks) {
            callbacks.add(callback)
        }
    }

    private fun callCallbacks(status: RadarStatus, location: Location? = null) {
        synchronized(callbacks) {
            if (callbacks.isEmpty()) {
                return
            }

            logger.d("Calling callbacks | callbacks.size = ${callbacks.size}")

            for (callback in callbacks) {
                callback.onComplete(status, location, RadarState.getStopped(context))
            }
            callbacks.clear()
        }
    }

    fun getLocation(callback: RadarLocationCallback? = null) {
        getLocation(RadarTrackingOptionsDesiredAccuracy.MEDIUM, RadarLocationSource.FOREGROUND_LOCATION, callback)
    }

    fun getLocation(desiredAccuracy: RadarTrackingOptionsDesiredAccuracy, source: RadarLocationSource, callback: RadarLocationCallback? = null) {
        if (!permissionsHelper.fineLocationPermissionGranted(context) && !permissionsHelper.coarseLocationPermissionGranted(context)) {
            Radar.sendError(RadarStatus.ERROR_PERMISSIONS)

            callback?.onComplete(RadarStatus.ERROR_PERMISSIONS)

            return
        }

        this.addCallback(callback)

        val locationManager = this

        val desiredPriority = when(desiredAccuracy) {
            RadarTrackingOptionsDesiredAccuracy.HIGH -> LocationRequest.PRIORITY_HIGH_ACCURACY
            RadarTrackingOptionsDesiredAccuracy.MEDIUM -> LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY
            RadarTrackingOptionsDesiredAccuracy.LOW -> LocationRequest.PRIORITY_LOW_POWER
            RadarTrackingOptionsDesiredAccuracy.NONE -> LocationRequest.PRIORITY_NO_POWER
        }

        logger.d("Requesting location")

        locationClient.getCurrentLocation(desiredPriority, null).addOnSuccessListener { location ->
            if (location == null) {
                logger.d("Location timeout")

                callCallbacks(RadarStatus.ERROR_LOCATION)
            } else {
                logger.d("Successfully requested location")

                locationManager.handleLocation(location, source)
            }


        }.addOnCanceledListener {
            logger.d("Location request canceled")

            callCallbacks(RadarStatus.ERROR_LOCATION)
        }
    }

    fun startTracking(options: RadarTrackingOptions = RadarTrackingOptions.EFFICIENT) {
        this.stopLocationUpdates()

        if (!permissionsHelper.fineLocationPermissionGranted(context) && !permissionsHelper.coarseLocationPermissionGranted(context)) {
            Radar.sendError(RadarStatus.ERROR_PERMISSIONS)
            return
        }

        RadarSettings.setTracking(context, true)
        RadarSettings.setTrackingOptions(context, options)
        RadarSettings.setFallbackTrackingOptions(context, options)

        this.updateTracking()
    }

    fun stopTracking() {
        this.started = false
        RadarSettings.setTracking(context, false)
        RadarSettings.setListenToServerTrackingOptions(context, false)
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

    internal fun handleBeacon(source: RadarLocationSource) {
        logger.d("Handling beacon | source = $source")

        this.getLocation(RadarTrackingOptionsDesiredAccuracy.MEDIUM, source)
    }

    internal fun handleBootCompleted() {
        logger.d("Handling boot completed")

        this.started = false
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

        logger.d("Updating tracking | options = $options; location = $location")

        val now = Date()
        if (!tracking && options.startTrackingAfter != null && options.startTrackingAfter!!.before(now)) {
            logger.d("Starting time-based tracking | startTrackingAfter = ${options.startTrackingAfter}")

            tracking = true
            RadarSettings.setTracking(context, true)
        } else if (tracking && options.stopTrackingAfter != null && options.stopTrackingAfter!!.before(now)) {
            logger.d("Stopping time-based tracking | startTrackingAfter = ${options.startTrackingAfter}")

            tracking = false
            RadarSettings.setTracking(context, false)
        }

        if (tracking) {
            val foregroundService = RadarSettings.getForegroundService(context)
            if (foregroundService != null && options.foregroundServiceEnabled) {
                if (!foregroundService.updatesOnly) {
                    this.startForegroundService(foregroundService)
                }
            } else if (RadarForegroundService.started) {
                this.stopForegroundService()
            }

            val stopped = RadarState.getStopped(context)
            if (stopped) {
                if (options.desiredStoppedUpdateInterval == 0) {
                    this.stopLocationUpdates()
                } else {
                    this.startLocationUpdates(options.desiredAccuracy, options.desiredStoppedUpdateInterval, options.fastestStoppedUpdateInterval)
                }

                if (options.useStoppedGeofence && location != null) {
                    this.replaceBubbleGeofence(location, true)
                } else {
                    this.removeBubbleGeofences()
                }
            } else {
                if (options.desiredMovingUpdateInterval == 0) {
                    this.stopLocationUpdates()
                } else {
                    this.startLocationUpdates(options.desiredAccuracy, options.desiredMovingUpdateInterval, options.fastestMovingUpdateInterval)
                }

                if (options.useMovingGeofence && location != null) {
                    this.replaceBubbleGeofence(location, false)
                } else {
                    this.removeBubbleGeofences()
                }
            }
        } else {
            this.stopForegroundService()
            this.stopLocationUpdates()
            this.removeAllGeofences()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Radar.beaconManager.stopMonitoringBeacons()
            }
        }
    }

    internal fun updateTrackingFromMeta(context: Context, meta: RadarApiClient.RadarMeta?) {
        if (meta?.remoteTrackingOptions != null) {
            // use remotely-configured options if specified
            RadarSettings.setTrackingOptions(context, meta.remoteTrackingOptions)
            RadarSettings.setListenToServerTrackingOptions(context, true)
        } else {
            // fallback
            RadarSettings.revertToFallbackTrackingOptions(context)
            RadarSettings.setListenToServerTrackingOptions(context, false)
        }
        updateTracking()
    }

    private fun replaceBubbleGeofence(location: Location?, stopped: Boolean) {
        if (location == null) {
            return
        }

        this.removeBubbleGeofences()

        val options = RadarSettings.getTrackingOptions(context)

        if (stopped && options.useStoppedGeofence) {
            val identifier = BUBBLE_STOPPED_GEOFENCE_REQUEST_ID
            val radius = options.stoppedGeofenceRadius.toFloat()

            val geofence = Geofence.Builder()
                .setRequestId(identifier)
                .setCircularRegion(location.latitude, location.longitude, radius)
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_EXIT)
                .build()

            val request = GeofencingRequest.Builder()
                .addGeofence(geofence)
                .setInitialTrigger(Geofence.GEOFENCE_TRANSITION_EXIT)
                .build()

            logger.d("Adding stopped bubble geofence | latitude = ${location.latitude}; longitude = ${location.longitude}; radius = $radius; identifier = $identifier")

            geofencingClient.addGeofences(request, RadarLocationReceiver.getBubbleGeofencePendingIntent(context)).run {
                addOnSuccessListener {
                    logger.d("Successfully added stopped bubble geofence")
                }
                addOnFailureListener {
                    logger.d("Error adding stopped bubble geofence | message = ${it.message}")
                }
            }
        } else if (!stopped && options.useMovingGeofence) {
            val identifier = BUBBLE_MOVING_GEOFENCE_REQUEST_ID
            val radius = options.movingGeofenceRadius.toFloat()

            val geofence = Geofence.Builder()
                .setRequestId(identifier)
                .setCircularRegion(location.latitude, location.longitude, radius)
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setLoiteringDelay(options.stopDuration * 1000 + 10000)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_DWELL or Geofence.GEOFENCE_TRANSITION_EXIT)
                .build()

            val request = GeofencingRequest.Builder()
                .addGeofence(geofence)
                .setInitialTrigger(Geofence.GEOFENCE_TRANSITION_DWELL or Geofence.GEOFENCE_TRANSITION_EXIT)
                .build()

            logger.d("Adding moving bubble geofence | latitude = ${location.latitude}; longitude = ${location.longitude}; radius = $radius; identifier = $identifier")

            geofencingClient.addGeofences(request, RadarLocationReceiver.getBubbleGeofencePendingIntent(context)).run {
                addOnSuccessListener {
                    logger.d("Successfully added moving bubble geofence")
                }
                addOnFailureListener {
                    logger.d("Error adding moving bubble geofence | message = ${it.message}")
                }
            }
        }
    }

    private fun replaceSyncedGeofences(radarGeofences: Array<RadarGeofence>?) {
        this.removeSyncedGeofences()

        val options = RadarSettings.getTrackingOptions(context)
        if (!options.syncGeofences || radarGeofences == null) {
            return
        }

        val geofences = mutableListOf<Geofence>()
        radarGeofences.forEachIndexed { i, radarGeofence ->
            var center: RadarCoordinate? = null
            var radius = 100.0
            if (radarGeofence.geometry is RadarCircleGeometry) {
                center = radarGeofence.geometry.center
                radius = radarGeofence.geometry.radius
            } else if (radarGeofence.geometry is RadarPolygonGeometry) {
                center = radarGeofence.geometry.center
                radius = radarGeofence.geometry.radius
            }
            if (center != null) {
                try {
                    val identifier = "${SYNCED_GEOFENCES_REQUEST_ID_PREFIX}_${i}"
                    val geofence = Geofence.Builder()
                        .setRequestId(identifier)
                        .setCircularRegion(center.latitude, center.longitude, radius.toFloat())
                        .setExpirationDuration(Geofence.NEVER_EXPIRE)
                        .setLoiteringDelay(options.stopDuration * 1000 + 10000)
                        .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_DWELL or Geofence.GEOFENCE_TRANSITION_EXIT)
                        .build()
                    geofences.add(geofence)

                    logger.d("Adding synced geofence | latitude = ${center.latitude}; longitude = ${center.longitude}; radius = $radius; identifier = $identifier")
                } catch (e: Exception) {
                    logger.d("Error building synced geofence | latitude = ${center.latitude}; longitude = ${center.longitude}; radius = $radius")
                }
            }
        }

        if (geofences.size == 0) {
            logger.d("No synced geofences")

            return
        }

        val request = GeofencingRequest.Builder()
            .addGeofences(geofences)
            .setInitialTrigger(0)
            .build()

        geofencingClient.addGeofences(request, RadarLocationReceiver.getSyncedGeofencesPendingIntent(context)).run {
            addOnSuccessListener {
                logger.d("Successfully added synced geofences")
            }
            addOnFailureListener {
                logger.d("Error adding synced geofences | message = ${it.message}")
            }
        }
    }

    private fun removeBubbleGeofences() {
        geofencingClient.removeGeofences(RadarLocationReceiver.getBubbleGeofencePendingIntent(context))
    }

    private fun removeSyncedGeofences() {
        geofencingClient.removeGeofences(RadarLocationReceiver.getSyncedGeofencesPendingIntent(context))
    }

    private fun removeAllGeofences() {
        this.removeBubbleGeofences()
        this.removeSyncedGeofences()
    }

    fun handleLocation(location: Location?, source: RadarLocationSource) {
        logger.d("Handling location | location = $location")

        if (location == null || !RadarUtils.valid(location)) {
            logger.d("Invalid location | source = $source; location = $location")

            Radar.sendError(RadarStatus.ERROR_LOCATION)

            callCallbacks(RadarStatus.ERROR_LOCATION)

            return
        }

        val options = RadarSettings.getTrackingOptions(context)
        val wasStopped = RadarState.getStopped(context)
        var stopped: Boolean

        val force = (source == RadarLocationSource.FOREGROUND_LOCATION || source == RadarLocationSource.MANUAL_LOCATION)
        if (!force && location.accuracy > 1000 && options.desiredAccuracy != RadarTrackingOptionsDesiredAccuracy.LOW) {
            logger.d("Skipping location: inaccurate | accuracy = ${location.accuracy}")

            this.updateTracking(location)

            return
        }

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
                logger.d("Skipping location: old | lastMovedAt = $lastMovedAt; location.time = $location.time")

                return
            }
            distance = location.distanceTo(lastMovedLocation)
            duration = (location.time - lastMovedAt) / 1000
            stopped = (distance < options.stopDistance && duration > options.stopDuration)

            logger.d("Calculating stopped | stopped = $stopped; distance = $distance; duration = $duration; location.time = ${location.time}; lastMovedAt = $lastMovedAt")

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

        Radar.sendClientLocation(location, stopped, source)

        if (source != RadarLocationSource.MANUAL_LOCATION) {
            this.updateTracking(location)
        }

        callCallbacks(RadarStatus.SUCCESS, location)

        var sendLocation = location

        val lastFailedStoppedLocation = RadarState.getLastFailedStoppedLocation(context)
        var replayed = false
        if (options.replay == RadarTrackingOptions.RadarTrackingOptionsReplay.STOPS && lastFailedStoppedLocation != null && !justStopped) {
            sendLocation = lastFailedStoppedLocation
            stopped = true
            replayed = true
            RadarState.setLastFailedStoppedLocation(context, null)

            logger.d("Replaying location | location = $location; stopped = $stopped")
        }

        val lastSentAt = RadarState.getLastSentAt(context)
        val ignoreSync =
            lastSentAt == 0L || this.callbacks.count() > 0 || justStopped || replayed
        val now = System.currentTimeMillis()
        val lastSyncInterval = now - lastSentAt
        if (!ignoreSync) {
            if (!force && stopped && wasStopped && distance < options.stopDistance && (options.desiredStoppedUpdateInterval == 0 || options.sync != RadarTrackingOptions.RadarTrackingOptionsSync.ALL)) {
                logger.d("Skipping sync: already stopped | stopped = $stopped; wasStopped = $wasStopped")

                return
            }

            if (lastSyncInterval < options.desiredSyncInterval) {
                logger.d("Skipping sync: desired sync interval | desiredSyncInterval = ${options.desiredSyncInterval}; lastSyncInterval = $lastSyncInterval")

                return
            }

            if (!force && !justStopped && lastSyncInterval < 1000L) {
                logger.d("Skipping sync: rate limit | justStopped = $justStopped; lastSyncInterval = $lastSyncInterval")

                return
            }

            if (options.sync == RadarTrackingOptions.RadarTrackingOptionsSync.NONE) {
                logger.d("Skipping sync: sync mode | sync = ${options.sync}")

                return
            }

            val canExit = RadarState.getCanExit(context)
            if (!canExit && options.sync == RadarTrackingOptions.RadarTrackingOptionsSync.STOPS_AND_EXITS) {
                logger.d("Skipping sync: can't exit | sync = ${options.sync}; canExit = $canExit")

                return
            }
        }
        RadarState.updateLastSentAt(context)

        if (source == RadarLocationSource.FOREGROUND_LOCATION) {
            return
        }

        this.sendLocation(sendLocation, stopped, source, replayed)
    }

    private fun sendLocation(location: Location, stopped: Boolean, source: RadarLocationSource, replayed: Boolean) {
        val options = RadarSettings.getTrackingOptions(context)
        val foregroundService = RadarSettings.getForegroundService(context)

        if (foregroundService != null && foregroundService.updatesOnly) {
            this.startForegroundService(foregroundService)
        }

        logger.d("Sending location | source = $source; location = $location; stopped = $stopped; replayed = $replayed")

        val locationManager = this

        val callTrackApi = { nearbyBeacons: Array<String>? ->
            this.apiClient.track(location, stopped, RadarActivityLifecycleCallbacks.foreground, source, replayed, nearbyBeacons, object : RadarTrackApiCallback {
                override fun onComplete(
                    status: RadarStatus,
                    res: JSONObject?,
                    events: Array<RadarEvent>?,
                    user: RadarUser?,
                    nearbyGeofences: Array<RadarGeofence>?,
                    meta: RadarApiClient.RadarMeta?
                ) {
                    if (user != null) {
                        val inGeofences = user.geofences != null && user.geofences.isNotEmpty()
                        val atPlace = user.place != null
                        val atHome = user.insights?.state?.home ?: false
                        val atOffice = user.insights?.state?.office ?: false
                        val canExit = inGeofences || atPlace || atHome || atOffice
                        RadarState.setCanExit(context, canExit)
                    }

                    locationManager.replaceSyncedGeofences(nearbyGeofences)

                    if (foregroundService != null && foregroundService.updatesOnly) {
                        locationManager.stopForegroundService()
                    }

                    updateTrackingFromMeta(context, meta)
                }
            })
        }

        if (options.beacons && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Radar.apiClient.searchBeacons(location, 1000, 10, object : RadarApiClient.RadarSearchBeaconsApiCallback {
                override fun onComplete(status: RadarStatus, res: JSONObject?, beacons: Array<RadarBeacon>?) {
                    if (status != RadarStatus.SUCCESS || beacons == null) {
                        callTrackApi(null)

                        return
                    }

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        Radar.beaconManager.startMonitoringBeacons(beacons)
                    }

                    Radar.beaconManager.rangeBeacons(beacons, object : Radar.RadarBeaconCallback {
                        override fun onComplete(status: RadarStatus, nearbyBeacons: Array<String>?) {
                            if (status != RadarStatus.SUCCESS || nearbyBeacons == null) {
                                callTrackApi(null)

                                return
                            }

                            callTrackApi(nearbyBeacons)
                        }
                    })
                }
            })
        } else {
            callTrackApi(null)
        }
    }

    private fun startForegroundService(foregroundService: RadarTrackingOptions.RadarTrackingOptionsForegroundService) {
        if (Build.VERSION.SDK_INT >= 26) {
            try {
                if (RadarForegroundService.started) {
                    logger.d("Already started foreground service")
                } else {
                    val intent = Intent(context, RadarForegroundService::class.java)
                    intent.action = "start"
                    intent.putExtra("id", foregroundService.id)
                        .putExtra("importance", foregroundService.importance ?: NotificationManager.IMPORTANCE_DEFAULT)
                        .putExtra("title", foregroundService.title)
                        .putExtra("text", foregroundService.text)
                        .putExtra("icon", foregroundService.icon )
                        .putExtra("activity", foregroundService.activity)
                    logger.d("Starting foreground service with intent | $intent")
                    context.applicationContext.startForegroundService(intent)
                    RadarForegroundService.started = true
                }
            } catch (e: Exception) {
                logger.e("Error starting foreground service with intent", e)
            }
        }
    }

    private fun stopForegroundService() {
        if (Build.VERSION.SDK_INT >= 26) {
            try {
                val intent = Intent(context, RadarForegroundService::class.java)
                intent.action = "stop"
                logger.d("Stopping foreground service with intent")
                context.applicationContext.startService(intent)
                RadarForegroundService.started = false
            } catch (e: Exception) {
                logger.e("Error stopping foreground service with intent", e)
            }
        }
    }

}