package io.radar.sdk

import android.annotation.SuppressLint
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Build
import android.os.Handler
import android.os.Looper
import io.radar.sdk.Radar.RadarLocationCallback
import io.radar.sdk.Radar.RadarLocationServicesProvider.HUAWEI
import io.radar.sdk.Radar.RadarLocationSource
import io.radar.sdk.Radar.RadarLogType
import io.radar.sdk.Radar.RadarStatus
import io.radar.sdk.RadarApiClient.RadarTrackApiCallback
import io.radar.sdk.RadarTrackingOptions.RadarTrackingOptionsDesiredAccuracy
import io.radar.sdk.model.*
import org.json.JSONObject
import java.util.*

@SuppressLint("MissingPermission")
internal class RadarLocationManager(
    private val context: Context,
    private val apiClient: RadarApiClient,
    private val logger: RadarLogger,
    private val batteryManager: RadarBatteryManager,
    provider: Radar.RadarLocationServicesProvider,
    internal var permissionsHelper: RadarPermissionsHelper = RadarPermissionsHelper(),
) {

    @SuppressLint("VisibleForTests")
    internal var locationClient: RadarAbstractLocationClient = if (provider == HUAWEI) RadarHuaweiLocationClient(context, logger) else RadarGoogleLocationClient(context, logger)
    private var started = false
    private var startedDesiredAccuracy = RadarTrackingOptionsDesiredAccuracy.NONE
    private var startedInterval = 0
    private var startedFastestInterval = 0
    private val callbacks = ArrayList<RadarLocationCallback>()
    private val activityManager = RadarActivityManager(context)

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
        if (!permissionsHelper.locationServicesEnabled(context)) {
            logger.d("Location services are not enabled")
            Radar.sendError(RadarStatus.ERROR_PERMISSIONS, "Location services are not enabled")
            callback?.onComplete(RadarStatus.ERROR_PERMISSIONS)
            return
        }

        if (!permissionsHelper.fineLocationPermissionGranted(context) && !permissionsHelper.coarseLocationPermissionGranted(context)) {
            Radar.sendError(RadarStatus.ERROR_PERMISSIONS)

            callback?.onComplete(RadarStatus.ERROR_PERMISSIONS)

            return
        }

        this.addCallback(callback)

        val locationManager = this

        logger.d("Requesting location")

        locationClient.getCurrentLocation(desiredAccuracy) { location ->
            if (location == null) {
                logger.d("Location timeout")
                Radar.sendError(RadarStatus.ERROR_LOCATION, "Location timeout")
                callCallbacks(RadarStatus.ERROR_LOCATION)
            } else {
                logger.d("Successfully requested location")

                locationManager.handleLocation(location, source)
            }
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
        this.updateTracking()
    }

    fun stopTracking() {
        this.started = false
        RadarSettings.setTracking(context, false)
        // null out startTrackingAfter and stopTrackingAfter in the local tracking options
        // so that tracking isn't restarted with a trackOnce call
        val trackingOptions = RadarSettings.getTrackingOptions(context)
        trackingOptions.startTrackingAfter = null
        trackingOptions.stopTrackingAfter = null
        RadarSettings.setTrackingOptions(context, trackingOptions)
        this.updateTracking()
        val settings = RadarSettings.getSdkConfiguration(context)
        if (settings.extendFlushReplays) {
            Radar.flushReplays()
        }
        if (settings.useLocationMetadata) {
            activityManager.stopActivityUpdates()
        }
    }

    private fun startLocationUpdates(desiredAccuracy: RadarTrackingOptionsDesiredAccuracy, interval: Int, fastestInterval: Int) {
        if (!started || (desiredAccuracy != startedDesiredAccuracy) || (interval != startedInterval) || (fastestInterval != startedFastestInterval)) {
            locationClient.requestLocationUpdates(desiredAccuracy, interval, fastestInterval, RadarLocationReceiver.getLocationPendingIntent(context))

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

    internal fun handleBeacons(beacons: Array<RadarBeacon>?, source: RadarLocationSource) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            logger.d("Handling beacons")

            Radar.beaconManager.handleBeacons(beacons, source)

            val lastLocation = RadarState.getLastLocation(context)

            if (lastLocation == null) {
                logger.d("Not handling beacons, no last location")
            }

            this.handleLocation(lastLocation, source)
        }
    }

    internal fun handleBootCompleted() {
        logger.d("Handling boot completed")

        this.started = false
        RadarState.setStopped(context, false)

        locationClient.getLastLocation { location: Location? ->
            updateTracking(location)
        }
    }

    internal fun updateTracking(location: Location? = null) {
        var tracking = RadarSettings.getTracking(context)
        val options = Radar.getTrackingOptions()
        val localOptions = RadarSettings.getTrackingOptions(context)

        logger.d("Updating tracking | options = $options; location = $location")

        val now = Date()
        if (!tracking && localOptions.startTrackingAfter != null && localOptions.startTrackingAfter!!.before(now)) {
            logger.d("Starting time-based tracking | startTrackingAfter = ${options.startTrackingAfter}")

            tracking = true
            RadarSettings.setTracking(context, true)
        } else if (tracking && localOptions.stopTrackingAfter != null && localOptions.stopTrackingAfter!!.before(now)) {
            logger.d("Stopping time-based tracking | startTrackingAfter = ${options.startTrackingAfter}")

            tracking = false
            RadarSettings.setTracking(context, false)
        }

        if (tracking) {
            if (RadarSettings.getSdkConfiguration(context).useLocationMetadata) {
                activityManager.startActivityUpdates()
            }
            if (options.foregroundServiceEnabled) {
                val foregroundService = RadarSettings.getForegroundService(context)
                if (!foregroundService.updatesOnly) {
                    this.startForegroundService(foregroundService)
                }
            }
            val stopped = RadarState.getStopped(context)
            if (stopped) {
                if (options.desiredStoppedUpdateInterval == 0) {
                    this.stopLocationUpdates()
                } else {
                    this.startLocationUpdates(options.desiredAccuracy, options.desiredStoppedUpdateInterval, options.fastestStoppedUpdateInterval)
                }

                if (options.useStoppedGeofence) {
                    if (location != null) {
                        this.replaceBubbleGeofence(location, true)
                    }
                } else {
                    this.removeBubbleGeofences()
                }
            } else {
                if (options.desiredMovingUpdateInterval == 0) {
                    this.stopLocationUpdates()
                } else {
                    this.startLocationUpdates(options.desiredAccuracy, options.desiredMovingUpdateInterval, options.fastestMovingUpdateInterval)
                }

                if (options.useMovingGeofence) {
                    if (location != null) {
                        this.replaceBubbleGeofence(location, false)
                    } 
                } else {
                    this.removeBubbleGeofences()
                }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !options.foregroundServiceEnabled && RadarForegroundService.started) {
                Handler(Looper.getMainLooper()).postDelayed({
                    this.stopForegroundService()
                }, 5000)
            }
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && RadarForegroundService.started) {
                this.stopForegroundService()
            }
            this.stopLocationUpdates()
            this.removeAllGeofences()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Radar.beaconManager.stopMonitoringBeacons()
            }
        }
    }

    internal fun updateTrackingFromMeta(meta: RadarMeta?) {
        if (meta != null) {
            if (meta.remoteTrackingOptions != null) {
                logger.d("Setting remote tracking options | trackingOptions = ${meta.remoteTrackingOptions}")
                RadarSettings.setRemoteTrackingOptions(context, meta.remoteTrackingOptions)
            } else {
                RadarSettings.removeRemoteTrackingOptions(context)
                logger.d("Removed remote tracking options | trackingOptions = ${Radar.getTrackingOptions()}")
            }
        }
        updateTracking()
    }

    internal fun restartPreviousTrackingOptions() {
        val previousTrackingOptions = RadarSettings.getPreviousTrackingOptions(context)
        logger.d("Restarting previous tracking options | trackingOptions = ${previousTrackingOptions}")
        if (previousTrackingOptions == null) {
            Radar.stopTracking()
        } else {
            Radar.startTracking(previousTrackingOptions)
        }
        RadarSettings.removePreviousTrackingOptions(context)
    }

    internal fun getLocationFromGeofenceIntent(intent: Intent): Location? {
        return locationClient.getLocationFromGeofenceIntent(intent)
    }

    internal fun getSourceFromGeofenceIntent(intent: Intent): RadarLocationSource? {
        return locationClient.getSourceFromGeofenceIntent(intent)
    }

    internal fun getLocationFromLocationIntent(intent: Intent): Location? {
        return locationClient.getLocationFromLocationIntent(intent)
    }

    private fun replaceBubbleGeofence(location: Location, stopped: Boolean) {
        this.removeBubbleGeofences() { success ->
	  this.addBubbleGeofence(location, stopped)
        }
    }

    private fun addBubbleGeofence(location: Location, stopped: Boolean) {
        val options = Radar.getTrackingOptions()

        if (stopped && options.useStoppedGeofence) {
            val identifier = BUBBLE_STOPPED_GEOFENCE_REQUEST_ID
            val radius = options.stoppedGeofenceRadius.toFloat()

            val geofence = RadarAbstractLocationClient.RadarAbstractGeofence(
                requestId = identifier,
                latitude = location.latitude,
                longitude = location.longitude,
                radius = radius,
                transitionExit = true
            )

            val geofences = arrayOf(geofence)

            val request = RadarAbstractLocationClient.RadarAbstractGeofenceRequest(
                initialTriggerExit = true
            )

            logger.d("Adding stopped bubble geofence | latitude = ${location.latitude}; longitude = ${location.longitude}; radius = $radius; identifier = $identifier")

            locationClient.addGeofences(geofences, request, RadarLocationReceiver.getBubbleGeofencePendingIntent(context)) { success ->
                if (success) {
                    logger.d("Successfully added stopped bubble geofence")
                } else {
                    logger.d("Error adding stopped bubble geofence")
                }
            }
        } else if (!stopped && options.useMovingGeofence) {
            val identifier = BUBBLE_MOVING_GEOFENCE_REQUEST_ID
            val radius = options.movingGeofenceRadius.toFloat()

            val geofence = RadarAbstractLocationClient.RadarAbstractGeofence(
                requestId = identifier,
                latitude = location.latitude,
                longitude = location.longitude,
                radius = radius,
                transitionExit = true,
                transitionDwell = true,
                dwellDuration = options.stopDuration * 1000 + 10000
            )

            val geofenceRequest = RadarAbstractLocationClient.RadarAbstractGeofenceRequest(
                initialTriggerExit = true,
                initialTriggerDwell = true
            )

            val geofences = arrayOf(geofence)

            logger.d("Adding moving bubble geofence | latitude = ${location.latitude}; longitude = ${location.longitude}; radius = $radius; identifier = $identifier")

            locationClient.addGeofences(geofences, geofenceRequest, RadarLocationReceiver.getBubbleGeofencePendingIntent(context)) { success ->
                if (success) {
                    logger.d("Successfully added moving bubble geofence")
                } else {
                    logger.d("Error adding moving bubble geofence")
                }
            }
        }
    }

    private fun replaceSyncedGeofences(radarGeofences: Array<RadarGeofence>?) {
        this.removeSyncedGeofences() { success ->
            this.addSyncedGeofences(radarGeofences)
        }
    }

    private fun addSyncedGeofences(radarGeofences: Array<RadarGeofence>?) {
        val options = Radar.getTrackingOptions()
        if (!options.syncGeofences || radarGeofences == null) {
            return
        }

        val geofences = mutableListOf<RadarAbstractLocationClient.RadarAbstractGeofence>()
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
                    val geofence = RadarAbstractLocationClient.RadarAbstractGeofence(
                        requestId = identifier,
                        latitude = center.latitude,
                        longitude = center.longitude,
                        radius = radius.toFloat(),
                        transitionEnter = true,
                        transitionExit = true,
                        transitionDwell = true,
                        dwellDuration = options.stopDuration * 1000 + 10000
                    )
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

        val request = RadarAbstractLocationClient.RadarAbstractGeofenceRequest()

        locationClient.addGeofences(geofences.toTypedArray(), request, RadarLocationReceiver.getSyncedGeofencesPendingIntent(context)) { success ->
            if (success) {
                logger.d("Successfully added synced geofences")
            } else {
                logger.d("Error adding synced geofences")
            }
        }
    }

    private fun removeBubbleGeofences(block: ((success: Boolean) -> Unit)? = null) {
        locationClient.removeGeofences(RadarLocationReceiver.getBubbleGeofencePendingIntent(context)) { success ->
            if (success) {
                logger.d("Removed bubble geofences")
                block?.invoke(true)
            } else {
                logger.d("Error removing bubble geofences")
                block?.invoke(false)
            }
        }
    }

    private fun removeSyncedGeofences(block: ((success: Boolean) -> Unit)? = null) {
        locationClient.removeGeofences(RadarLocationReceiver.getSyncedGeofencesPendingIntent(context)) { success ->
            if (success) {
                logger.d("Removed synced geofences")
                block?.invoke(true)
            } else {
                logger.d("Error removing synced geofences")
                block?.invoke(false)
            }
        }
    }

    private fun removeAllGeofences() {
        this.removeBubbleGeofences()
        this.removeSyncedGeofences()
    }

    fun handleLocation(location: Location?, source: RadarLocationSource) {
        if (Radar.isTestKey()) {
            val latency = if (location == null) -1 else Date().time - location.time
            val standbyBucket = batteryManager.getAppStandbyBucket()
            val batteryState = batteryManager.getBatteryState()
            logger.d(
                "Handling location | " +
                        "location = $location; " +
                        "latency = $latency; " +
                        "standbyBucket = $standbyBucket; " +
                        "performanceState = ${batteryState.performanceState.name}; " +
                        "isCharging = ${batteryState.isCharging}; " +
                        "batteryPercentage = ${batteryState.percent}; " +
                        "isPowerSaveMode = ${batteryState.powerSaveMode}; " +
                        "isIgnoringBatteryOptimizations = ${batteryState.isIgnoringBatteryOptimizations}; " +
                        "locationPowerSaveMode = ${batteryState.getPowerLocationPowerSaveModeString()}; " +
                        "isDozeMode = ${batteryState.isDeviceIdleMode}"
            )
        } else {
            logger.d("Handling location | source = $source; location = $location")
        }
         // Set default accuracy if not provided
        if (location?.accuracy  == 0f) {
            location.accuracy = 1000f
            logger.d("Setting default accuracy of 1000 for location without accuracy")
        }

        if (location == null || !RadarUtils.valid(location)) {
            logger.d("Invalid location | source = $source; location = $location")

            Radar.sendError(RadarStatus.ERROR_LOCATION, "Invalid location | source = $source; location = $location")

            callCallbacks(RadarStatus.ERROR_LOCATION)

            return
        }

        val options = Radar.getTrackingOptions()
        val wasStopped = RadarState.getStopped(context)
        var stopped: Boolean

        val force = (source == RadarLocationSource.FOREGROUND_LOCATION || source == RadarLocationSource.MANUAL_LOCATION || source == RadarLocationSource.BEACON_ENTER || source == RadarLocationSource.BEACON_EXIT)
        if (!force && location.accuracy >= 1000 && options.desiredAccuracy != RadarTrackingOptionsDesiredAccuracy.LOW) {
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
                logger.d("Skipping location: old | lastMovedAt = $lastMovedAt; location.time = ${location.time}")

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

        RadarState.setLastLocation(context, location)

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
        val lastSyncInterval = (now - lastSentAt) / 1000L
        if (!ignoreSync) {
            if (!force && stopped && wasStopped && distance < options.stopDistance && (options.desiredStoppedUpdateInterval == 0 || options.sync != RadarTrackingOptions.RadarTrackingOptionsSync.ALL)) {
                logger.d("Skipping sync: already stopped | stopped = $stopped; wasStopped = $wasStopped")

                return
            }

            // add a 0.1 second buffer to account for the fact that the timer may fire slightly before the desired interval
            val lastSyncIntervalWithBuffer = lastSyncInterval + 0.1
            if (lastSyncIntervalWithBuffer < options.desiredSyncInterval) {
                logger.d("Skipping sync: desired sync interval | desiredSyncInterval = ${options.desiredSyncInterval}; lastSyncInterval = ${lastSyncIntervalWithBuffer}")

                return
            }

            if (!force && !justStopped && lastSyncInterval < 1) {
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
        val options = Radar.getTrackingOptions()
        val foregroundService = RadarSettings.getForegroundService(context)

        if (options.foregroundServiceEnabled && foregroundService.updatesOnly) {
            this.startForegroundService(foregroundService)
        }

        logger.d("Sending location | source = $source; location = $location; stopped = $stopped; replayed = $replayed")

        val locationManager = this

        val callTrackApi = { beacons: Array<RadarBeacon>? ->
            this.apiClient.track(location, stopped, RadarActivityLifecycleCallbacks.foreground, source, replayed, beacons, callback = object : RadarTrackApiCallback {
                override fun onComplete(
                    status: RadarStatus,
                    res: JSONObject?,
                    events: Array<RadarEvent>?,
                    user: RadarUser?,
                    nearbyGeofences: Array<RadarGeofence>?,
                    config: RadarConfig?,
                    token: RadarVerifiedLocationToken?
                ) {
                    locationManager.replaceSyncedGeofences(nearbyGeofences)

                    if (options.foregroundServiceEnabled && foregroundService.updatesOnly) {
                        locationManager.stopForegroundService()
                    }

                    updateTrackingFromMeta(config?.meta)
                }
            })
        }

        if (options.beacons && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
            && permissionsHelper.bluetoothPermissionsGranted(context)) {
            val cache = stopped || source == RadarLocationSource.BEACON_ENTER || source == RadarLocationSource.BEACON_EXIT
            this.apiClient.searchBeacons(location, 1000, 10, object : RadarApiClient.RadarSearchBeaconsApiCallback {
                override fun onComplete(status: RadarStatus, res: JSONObject?, beacons: Array<RadarBeacon>?, uuids: Array<String>?, uids: Array<String>?) {
                   if (!uuids.isNullOrEmpty() || !uids.isNullOrEmpty()) {
                        Radar.beaconManager.startMonitoringBeaconUUIDs(uuids, uids)

                        Radar.beaconManager.rangeBeaconUUIDs(uuids, uids, true, object : Radar.RadarBeaconCallback {
                            override fun onComplete(status: RadarStatus, beacons: Array<RadarBeacon>?) {
                                if (status != RadarStatus.SUCCESS || beacons == null) {
                                    callTrackApi(null)

                                    return
                                }

                                callTrackApi(beacons)
                            }
                        })
                   } else if (beacons != null) {
                        Radar.beaconManager.startMonitoringBeacons(beacons)

                        Radar.beaconManager.rangeBeacons(beacons, true, object : Radar.RadarBeaconCallback {
                            override fun onComplete(status: RadarStatus, beacons: Array<RadarBeacon>?) {
                                if (status != RadarStatus.SUCCESS || beacons == null) {
                                    callTrackApi(null)

                                    return
                                }

                                callTrackApi(beacons)
                            }
                        })
                   } else {
                       callTrackApi(arrayOf())
                   }
                }
            }, cache)
        } else {
            callTrackApi(null)
        }
    }

    private fun startForegroundService(foregroundService: RadarTrackingOptions.RadarTrackingOptionsForegroundService) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
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
                        .putExtra("icon", foregroundService.icon)
                        .putExtra("iconString", foregroundService.iconString)
                        .putExtra("iconColor", foregroundService.iconColor)
                        .putExtra("activity", foregroundService.activity)
                        .putExtra("deepLink", foregroundService.deepLink)
                    logger.d("Starting foreground service with intent | intent = $intent")
                    context.applicationContext.startForegroundService(intent)
                    RadarForegroundService.started = true
                }
            } catch (e: Exception) {
                logger.e("Error starting foreground service with intent", RadarLogType.SDK_EXCEPTION, e)
            }
        }
    }

    private fun stopForegroundService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val intent = Intent(context, RadarForegroundService::class.java)
                intent.action = "stop"
                logger.d("Stopping foreground service with intent")
                context.applicationContext.startService(intent)
                RadarForegroundService.started = false
            } catch (e: Exception) {
                logger.e("Error stopping foreground service with intent", RadarLogType.SDK_EXCEPTION, e)
            }
        }
    }
}
