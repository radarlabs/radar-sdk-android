package io.radar.sdk

import android.annotation.SuppressLint
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Build
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
import kotlin.math.abs


enum class RampingOption {
    RampingOptionNoChange,
    RampingOptionRampUp,
    RampingOptionRampDown
}

// define the StateChange object/class which has a date and a bool
data class StateChange(
    val timestamp: Date,
    val rampedUp: Boolean
)

@SuppressLint("MissingPermission")
internal class RadarLocationManager(
    private val context: Context,
    private val apiClient: RadarApiClient,
    private val logger: RadarLogger,
    private val batteryManager: RadarBatteryManager,
    private val provider: Radar.RadarLocationServicesProvider,
    internal var permissionsHelper: RadarPermissionsHelper = RadarPermissionsHelper(),
) {

    @SuppressLint("VisibleForTests")
    internal var locationClient: RadarAbstractLocationClient = if (provider == HUAWEI) RadarHuaweiLocationClient(context, logger) else RadarGoogleLocationClient(context, logger)
    private var started = false
    private var startedDesiredAccuracy = RadarTrackingOptionsDesiredAccuracy.NONE
    private var startedInterval = 0
    private var startedFastestInterval = 0
    private val callbacks = ArrayList<RadarLocationCallback>()
    private var stateChanges: MutableList<StateChange> = ArrayList()


    internal companion object {
        private const val BUBBLE_MOVING_GEOFENCE_REQUEST_ID = "radar_moving"
        private const val BUBBLE_STOPPED_GEOFENCE_REQUEST_ID = "radar_stopped"
        private const val SYNCED_GEOFENCES_REQUEST_ID_PREFIX = "radar_sync"
        private const val kRampUpGeofenceIdentifierPrefix = "radar_ramp_up"
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

        logger.d("Requesting location")

        locationClient.getCurrentLocation(desiredAccuracy) { location ->
            if (location == null) {
                logger.d("Location timeout")

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
        this.updateTracking()
        val settings = RadarSettings.getFeatureSettings(context)
        if (settings.extendFlushReplays) {
            Radar.flushReplays()
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

    internal fun updateTracking(location: Location? = null, ramping: RampingOption = RampingOption.RampingOptionNoChange) {
        var tracking = RadarSettings.getTracking(context)
        var options = Radar.getTrackingOptions()
        val wasRamped = RadarSettings.getRampedUp(context)
        val onTrip = RadarSettings.getTripOptions(context) != null

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
            if (ramping == RampingOption.RampingOptionRampUp && !wasRamped) {
                if (!onTrip) {
                    RadarSettings.setPreviousTrackingOptions(context, options)
                }

                val rampUpRadius = options.rampUpRadius
                val originalReplayOption = options.replay
                
                options = RadarTrackingOptions.RAMPED_UP
                options.replay = originalReplayOption
                options.rampUpRadius = rampUpRadius

                RadarSettings.setTrackingOptions(context, options)
                RadarSettings.setRampedUp(context, true)
               
                // call location manager changeTrackingState
            } else if (ramping == RampingOption.RampingOptionRampDown && wasRamped) {
                val previousTrackingOptions = RadarSettings.getPreviousTrackingOptions(context)
                if (onTrip) {
                    options = RadarTrackingOptions.CONTINUOUS
                } else if (previousTrackingOptions != null) {
                    options = previousTrackingOptions
                    RadarSettings.removePreviousTrackingOptions(context)
                } else {
                    logger.d("Tried to ramp down but no previous tracking options")
                }

                RadarSettings.setTrackingOptions(context, options)
                RadarSettings.setRampedUp(context, false)
                // call location manager changeTrackingState


            }
            if (options.foregroundServiceEnabled) {
                val foregroundService = RadarSettings.getForegroundService(context)
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

                if (options.useMovingGeofence && location != null) {
                    this.replaceBubbleGeofence(location, false)
                } else {
                    this.removeBubbleGeofences()
                }
            }
        } else {
            if (RadarForegroundService.started) {
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

    internal fun trackStateChange(rampedUp: Boolean) {
        val now = Date()
        val stateChange = StateChange(now, rampedUp)
        stateChanges.add(stateChange)
    }

    internal fun calculateRampedUpTimesAndCleanup(): Map<String, Double> {
        val now = Date()
        val oneHourAgo = Date(now.time - 3600 * 1000) // 1 hour in milliseconds
        val twelveHoursAgo = Date(now.time - 12 * 3600 * 1000) // 12 hours in milliseconds
    
        var totalRampedUpTimeOneHour = 0.0
        var totalRampedUpTimeTwelveHours = 0.0
        var lastRampedUpStartOneHour: Date? = null
        var lastRampedUpStartTwelveHours: Date? = null
        val validChanges = mutableListOf<StateChange>() // Assuming StateChange is a data class
    
        for (change in stateChanges) { // Assuming stateChanges is an iterable property
            if (change.timestamp.before(twelveHoursAgo)) {
                continue // Skip changes older than twelve hours
            }
    
            validChanges.add(change)
    
            if (change.rampedUp) {
                lastRampedUpStartTwelveHours = change.timestamp
            } else if (lastRampedUpStartTwelveHours != null) {
                totalRampedUpTimeTwelveHours += abs(change.timestamp.time - lastRampedUpStartTwelveHours.time) / 1000.0
                lastRampedUpStartTwelveHours = null
            }
    
            if (!change.timestamp.before(oneHourAgo)) {
                if (change.rampedUp) {
                    lastRampedUpStartOneHour = change.timestamp
                } else if (lastRampedUpStartOneHour != null) {
                    totalRampedUpTimeOneHour += abs(change.timestamp.time - lastRampedUpStartOneHour.time) / 1000.0
                    lastRampedUpStartOneHour = null
                }
            }
        }
    
        lastRampedUpStartTwelveHours?.let {
            totalRampedUpTimeTwelveHours += abs(now.time - it.time) / 1000.0
        }
        lastRampedUpStartOneHour?.let {
            totalRampedUpTimeOneHour += abs(now.time - it.time) / 1000.0
        }
    
        stateChanges = validChanges
    
        return mapOf("OneHour" to totalRampedUpTimeOneHour, "TwelveHours" to totalRampedUpTimeTwelveHours)
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

    private fun replaceBubbleGeofence(location: Location?, stopped: Boolean) {
        if (location == null) {
            return
        }

        this.removeBubbleGeofences()

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
        this.removeSyncedGeofences()

        val options = Radar.getTrackingOptions()
        if (!options.syncGeofences || radarGeofences == null) {
            return
        }

        val currentLocation = RadarState.getLastLocation(context)

        var withinRampUpRadius = false

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
                var rampUpRadius = 0.0
                var identifier = "${SYNCED_GEOFENCES_REQUEST_ID_PREFIX}_${i}"
                if (options.rampUpRadius > 0) {
                    rampUpRadius = options.rampUpRadius.toDouble()
                }
    
                radarGeofence.metadata?.let { metadata ->
                    val rampUpRadiusValue = metadata["radar:rampUpRadius"]
                    if (rampUpRadiusValue is String) {  // Check if the value is a String
                        val rampUpRadiusDouble = rampUpRadiusValue.toDoubleOrNull()
                        if (rampUpRadiusDouble != null && rampUpRadiusDouble > 0) {
                            rampUpRadius = rampUpRadiusDouble
                        }
                    }
                }
    
                val tripOptions = RadarSettings.getTripOptions(context)
                if (tripOptions != null && 
                    tripOptions.destinationGeofenceTag == radarGeofence.tag &&
                    tripOptions.destinationGeofenceExternalId == radarGeofence.externalId &&
                    tripOptions.rampUpRadius > 0) {
                        rampUpRadius = tripOptions.rampUpRadius.toDouble()
                }
    
                if (rampUpRadius > 0) {
                    val distance = center.distanceTo(RadarCoordinate(currentLocation?.latitude ?: 0.0, currentLocation?.longitude ?: 0.0))

                    logger.d("distance from geofence center to current location: $distance")
    
                    if (distance <= rampUpRadius) {
                        logger.d("distance from geofence center to current location is less than rampUpRadius, setting withinRampUpRadius to YES")
                        withinRampUpRadius = true
                    } else {
                        radius = rampUpRadius
                        identifier = "${kRampUpGeofenceIdentifierPrefix}_${i}"
                        logger.d("radius is rampUpRadius: $radius")
                    }
                }    
                try {
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

        val rampedUpTimes = calculateRampedUpTimesAndCleanup()
        val totalRampedUpTimeOneHour = rampedUpTimes["OneHour"] ?: 0.0
        val totalRampedUpTimeTwelveHours = rampedUpTimes["TwelveHours"] ?: 0.0
    
        val exceededRampUpTimeLimit = totalRampedUpTimeOneHour > 1200 || totalRampedUpTimeTwelveHours > 7200
        val rampedUp = RadarSettings.getRampedUp(context)
    
        when {
            withinRampUpRadius && !rampedUp && !exceededRampUpTimeLimit -> {
                logger.d("Ramping up")
                updateTracking(currentLocation, ramping = RampingOption.RampingOptionRampUp)
            }
            withinRampUpRadius && !rampedUp && exceededRampUpTimeLimit -> {
                logger.d("Exceeded ramp up time limit, not ramping up")
            }
            withinRampUpRadius && rampedUp && !exceededRampUpTimeLimit -> {
                logger.d("Already ramped up")
            }
            withinRampUpRadius && rampedUp && exceededRampUpTimeLimit -> {
                logger.d("Exceeded ramp up time limit, ramping down")
                updateTracking(currentLocation, ramping = RampingOption.RampingOptionRampDown)
            }
            !withinRampUpRadius && rampedUp -> {
                logger.d("Ramping down")
                updateTracking(currentLocation,ramping = RampingOption.RampingOptionRampDown)
            }
            !withinRampUpRadius && !rampedUp -> {
                logger.d("Already ramped down")
            }
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

    private fun removeBubbleGeofences() {
        locationClient.removeGeofences(RadarLocationReceiver.getBubbleGeofencePendingIntent(context))
        logger.d("Removed bubble geofences")
    }

    private fun removeSyncedGeofences() {
        locationClient.removeGeofences(RadarLocationReceiver.getSyncedGeofencesPendingIntent(context))
        logger.d("Removed synced geofences")
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

        if (location == null || !RadarUtils.valid(location)) {
            logger.d("Invalid location | source = $source; location = $location")

            Radar.sendError(RadarStatus.ERROR_LOCATION)

            callCallbacks(RadarStatus.ERROR_LOCATION)

            return
        }

        val options = Radar.getTrackingOptions()
        val wasStopped = RadarState.getStopped(context)
        var stopped: Boolean

        val force = (source == RadarLocationSource.FOREGROUND_LOCATION || source == RadarLocationSource.MANUAL_LOCATION || source == RadarLocationSource.BEACON_ENTER || source == RadarLocationSource.BEACON_EXIT)
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

            if (lastSyncInterval < options.desiredSyncInterval) {
                logger.d("Skipping sync: desired sync interval | desiredSyncInterval = ${options.desiredSyncInterval}; lastSyncInterval = $lastSyncInterval")

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
                    token: String?
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
                       callTrackApi(null)
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
