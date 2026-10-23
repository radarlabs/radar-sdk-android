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
import io.radar.sdk.RadarTrackingOptions.RadarTrackingOptionsSyncGeofences
import io.radar.sdk.model.RadarBeacon
import io.radar.sdk.model.RadarCircleGeometry
import io.radar.sdk.model.RadarConfig
import io.radar.sdk.model.RadarCoordinate
import io.radar.sdk.model.RadarEvent
import io.radar.sdk.model.RadarGeofence
import io.radar.sdk.model.RadarMeta
import io.radar.sdk.model.RadarPolygonGeometry
import io.radar.sdk.model.RadarUser
import io.radar.sdk.model.RadarVerifiedLocationToken
import java.util.*
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import org.json.JSONObject

@SuppressLint("MissingPermission")
internal class RadarLocationManager(
    private var context: Context,
    private val apiClient: RadarApiClient,
    private val logger: RadarLogger,
    private val batteryManager: RadarBatteryManager,
    provider: Radar.RadarLocationServicesProvider,
    internal var permissionsHelper: RadarPermissionsHelper = RadarPermissionsHelper()
) {

    @SuppressLint("VisibleForTests")
    internal var locationClient: RadarAbstractLocationClient = if (provider == HUAWEI) RadarHuaweiLocationClient(context, logger) else RadarGoogleLocationClient(context, logger)
    private var started = false
    private var startedDesiredAccuracy = RadarTrackingOptionsDesiredAccuracy.NONE
    private var startedInterval = 0
    private var startedFastestInterval = 0
    private val callbacks = ArrayList<RadarLocationCallback>()
    private val activityManager = RadarActivityManager(context)
    private val sensorsManager = RadarSensorsManager(context)
    private var loggedForegroundServiceNotDeclared = false

    // In production, context.applicationContext is always the same Application instance,
    // so this is effectively a no-op. Under Robolectric test suites, different test classes
    // can produce distinct Application contexts while this singleton-held LocationManager
    // persists, causing SharedPreferences reads/writes to diverge.
    internal fun updateContext(context: Context) {
        this.context = context
    }

    internal companion object {
        private const val BUBBLE_MOVING_GEOFENCE_REQUEST_ID = "radar_moving"
        private const val BUBBLE_STOPPED_GEOFENCE_REQUEST_ID = "radar_stopped"
        private const val SYNCED_GEOFENCES_REQUEST_ID_PREFIX = "radar_sync"

        // When beacon monitoring reports every sighting, a beacon not seen for this long counts as
        // entered again.
        internal val BEACON_STALE_AFTER = 60.seconds

        // How long a lost beacon must stay unseen before its exit is tracked, like iOS beacon region exits.
        internal val BEACON_EXIT_DELAY = 30.seconds
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
        if (Radar.getTrackingOptions().useMotion) {
            activityManager.stopActivityUpdates()
        }
        if (Radar.getTrackingOptions().usePressure) {
            sensorsManager.onPause()
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
            // Beacon monitoring events only track while tracking is on. This matches iOS and keeps a
            // leftover scan from sending unverified tracks with a cached location.
            if (!RadarSettings.getTracking(context)) {
                logger.d("Ignoring beacon event: not tracking | source = $source")

                Radar.beaconMonitoringManager.stopMonitoringBeacons()

                return
            }

            logger.d("Handling beacons")

            Radar.oneShotBeaconManager.seedNearbyBeacons(beacons, source)

            val insideBeacons = updateInsideBeacons(beacons, source) ?: return

            trackBeaconEvent(source, insideBeacons)
        }
    }

    /**
     * Confirms beacon exits that have been pending for [BEACON_EXIT_DELAY], and tracks them.
     * [runningJobId] is the exit check job running this, if any.
     */
    internal fun handleBeaconExitCheck(runningJobId: Int? = null) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!RadarSettings.getTracking(context)) {
                logger.d("Ignoring beacon exit check: not tracking")

                Radar.beaconMonitoringManager.stopMonitoringBeacons()

                return
            }

            val now = System.currentTimeMillis()
            val insideBeacons = RadarState.getInsideBeacons(context)
            val exitedKeys = insideBeacons.filterValues { it.lostAt > 0 && (now - it.lostAt).milliseconds >= BEACON_EXIT_DELAY }.keys.toList()
            exitedKeys.forEach { insideBeacons.remove(it) }
            RadarState.setInsideBeacons(context, insideBeacons)

            scheduleBeaconExitCheck(insideBeacons, now, runningJobId)

            if (exitedKeys.isEmpty()) {
                logger.d("No beacon exits to confirm")

                return
            }

            logger.d("Exited beacon region | beacons = ${exitedKeys.joinToString()}; insideBeacons = ${insideBeacons.size}")

            trackBeaconEvent(RadarLocationSource.BEACON_EXIT, insideBeacons.values.map { it.beacon }.toTypedArray())
        }
    }

    // Like iOS, tracks with the current last known location if valid, otherwise the last tracked one.
    private fun trackBeaconEvent(source: RadarLocationSource, insideBeacons: Array<RadarBeacon>) {
        locationClient.getLastLocation { lastKnownLocation ->
            val location = if (lastKnownLocation != null && RadarUtils.valid(lastKnownLocation)) lastKnownLocation else RadarState.getLastLocation(context)

            if (location == null) {
                logger.d("Not handling beacons, no last location")
            }

            this.handleLocation(location, source, insideBeacons)
        }
    }

    // Schedules the next beacon exit check for the earliest pending exit, if any.
    private fun scheduleBeaconExitCheck(insideBeacons: Map<String, RadarState.InsideBeacon>, now: Long, runningJobId: Int? = null) {
        val nextLostAt = insideBeacons.values.filter { it.lostAt > 0 }.minOfOrNull { it.lostAt } ?: return
        val delay = BEACON_EXIT_DELAY - (now - nextLostAt).milliseconds

        RadarJobScheduler.scheduleBeaconExitCheck(context, delay, runningJobId)
    }

    /**
     * Updates the beacons the user is inside for a beacon monitoring event, like iOS beacon region
     * enter and exit. Returns the beacons the user is inside if a beacon was entered or exited, or
     * null if nothing changed.
     *
     * A lost beacon isn't exited right away. Like iOS, the exit waits [BEACON_EXIT_DELAY], and seeing the
     * beacon again in the meantime cancels it, because Bluetooth controllers report losses after missing
     * only a few advertisements.
     */
    private fun updateInsideBeacons(beacons: Array<RadarBeacon>?, source: RadarLocationSource): Array<RadarBeacon>? {
        val now = System.currentTimeMillis()
        val transitions = RadarState.getBeaconMonitoringTransitions(context)
        val insideBeacons = RadarState.getInsideBeacons(context)
        val enteredBeacons = mutableListOf<RadarBeacon>()
        var lost = false

        beacons.orEmpty().forEach { beacon ->
            val key = RadarBeaconUtils.beaconKey(beacon)
            val previous = insideBeacons[key]
            if (source == RadarLocationSource.BEACON_EXIT) {
                if (previous != null && previous.lostAt == 0L) {
                    insideBeacons[key] = previous.copy(lostAt = now)
                    lost = true
                }
            } else {
                // A scan that reports every sighting has no losses, so a beacon not seen recently is entered again.
                val stale = previous != null && !transitions && (now - previous.lastSeenAt).milliseconds >= BEACON_STALE_AFTER
                if (previous == null || stale) {
                    enteredBeacons.add(beacon)
                } else if (previous.lostAt > 0) {
                    logger.d("Beacon found again, canceling exit | beacon = $key")
                }
                insideBeacons[key] = RadarState.InsideBeacon(beacon, now)
            }
        }

        RadarState.setInsideBeacons(context, insideBeacons)

        if (source == RadarLocationSource.BEACON_EXIT) {
            if (lost) {
                logger.d("Beacon lost, confirming exit in $BEACON_EXIT_DELAY")

                scheduleBeaconExitCheck(insideBeacons, now)
            } else {
                logger.d("Already outside beacon region")
            }

            return null
        }

        if (enteredBeacons.isEmpty()) {
            logger.d("Already inside beacon region")

            return null
        }

        logger.d("Entered beacon region | beacons = ${enteredBeacons.joinToString { RadarBeaconUtils.beaconKey(it) }}; insideBeacons = ${insideBeacons.size}")

        return insideBeacons.values.map { it.beacon }.toTypedArray()
    }

    internal fun handleBootCompleted() {
        logger.d("Handling boot completed")

        this.started = false
        RadarState.setStopped(context, false)
        // Beacon monitoring scans don't survive a reboot.
        RadarState.setInsideBeacons(context, emptyMap())

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
            if (Radar.getTrackingOptions().useMotion) {
                activityManager.startActivityUpdates()
            }
            if (Radar.getTrackingOptions().usePressure) {
                sensorsManager.onResume()
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
                Radar.beaconMonitoringManager.stopMonitoringBeacons()
            }
        }

        RadarBootReceiver.update(context, logger)
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
        logger.d("Restarting previous tracking options | trackingOptions = $previousTrackingOptions")
        if (previousTrackingOptions == null) {
            Radar.stopTracking()
        } else {
            Radar.startTracking(previousTrackingOptions)
        }
        RadarSettings.removePreviousTrackingOptions(context)
    }

    internal fun getLocationFromGeofenceIntent(intent: Intent): Location? = locationClient.getLocationFromGeofenceIntent(intent)

    internal fun getSourceFromGeofenceIntent(intent: Intent): RadarLocationSource? = locationClient.getSourceFromGeofenceIntent(intent)

    internal fun getLocationFromLocationIntent(intent: Intent): Location? = locationClient.getLocationFromLocationIntent(intent)

    private fun replaceBubbleGeofence(location: Location, stopped: Boolean) {
        this.removeBubbleGeofences { success ->
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
        this.removeSyncedGeofences { success ->
            this.addSyncedGeofences(radarGeofences)
        }
    }

    private fun addSyncedGeofences(radarGeofences: Array<RadarGeofence>?) {
        val options = Radar.getTrackingOptions()
        if (options.syncGeofences == RadarTrackingOptionsSyncGeofences.NONE || radarGeofences == null) {
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
                    val identifier = "${SYNCED_GEOFENCES_REQUEST_ID_PREFIX}_$i"
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

    fun handleLocation(location: Location?, source: RadarLocationSource, beacons: Array<RadarBeacon>? = null) {
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
        if (location?.accuracy == 0f) {
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

        val force = (source == RadarLocationSource.FOREGROUND_LOCATION || source == RadarLocationSource.MANUAL_LOCATION) ||
            (
                options.sync != RadarTrackingOptions.RadarTrackingOptionsSync.EVENTS &&
                    (source == RadarLocationSource.BEACON_ENTER || source == RadarLocationSource.BEACON_EXIT)
                )
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

        val sdkConfiguration = RadarSettings.getSdkConfiguration(context)
        if (sdkConfiguration.useSyncRegion) {
            if (!Radar.syncManager.hasSyncedRegion()) {
                Radar.syncManager.fetchSyncRegion()
            } else if (sdkConfiguration.offlineEventGenerationEnabled &&
                (
                    Radar.syncManager.isOutsideSyncedRegion(location) ||
                        Radar.syncManager.isNearSyncedRegionBoundary(location)
                    )
            ) {
                Radar.syncManager.fetchSyncRegion()
            }
        }

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
            lastSentAt == 0L || this.callbacks.count() > 0 || justStopped || replayed || source == RadarLocationSource.BEACON_ENTER || source == RadarLocationSource.BEACON_EXIT
        val now = System.currentTimeMillis()
        val lastSyncInterval = (now - lastSentAt) / 1000L
        if (!ignoreSync) {
            if (!force && stopped && wasStopped && distance < options.stopDistance && (options.desiredStoppedUpdateInterval == 0 || (options.sync != RadarTrackingOptions.RadarTrackingOptionsSync.ALL && options.sync != RadarTrackingOptions.RadarTrackingOptionsSync.EVENTS))) {
                logger.d("Skipping sync: already stopped | stopped = $stopped; wasStopped = $wasStopped")

                return
            }

            // add a 0.1 second buffer to account for the fact that the timer may fire slightly before the desired interval
            val lastSyncIntervalWithBuffer = lastSyncInterval + 0.1
            if (lastSyncIntervalWithBuffer < options.desiredSyncInterval) {
                logger.d("Skipping sync: desired sync interval | desiredSyncInterval = ${options.desiredSyncInterval}; lastSyncInterval = $lastSyncIntervalWithBuffer")

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

        if (source != RadarLocationSource.FOREGROUND_LOCATION && source != RadarLocationSource.MANUAL_LOCATION &&
            sdkConfiguration.useSyncRegion && options.sync == RadarTrackingOptions.RadarTrackingOptionsSync.EVENTS
        ) {
            if (location.accuracy >= 1000 && options.desiredAccuracy != RadarTrackingOptionsDesiredAccuracy.LOW) {
                logger.d("Skipping sync region eval: inaccurate | accuracy = ${location.accuracy}")
                return
            }

            // A beacon monitoring event is a beacon state change.
            if (source == RadarLocationSource.BEACON_ENTER || source == RadarLocationSource.BEACON_EXIT) {
                RadarState.updateLastSentAt(context)
                this.sendLocation(sendLocation, stopped, source, replayed, beacons = beacons)
                return
            }

            val geofenceOrPlaceChanged = Radar.syncManager.shouldTrack(location, options)

            if (geofenceOrPlaceChanged) {
                RadarState.updateLastSentAt(context)
                this.sendLocation(sendLocation, stopped, source, replayed, forceTrack = true)
                return
            }

            if (options.beacons && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                permissionsHelper.bluetoothPermissionsGranted(context)
            ) {
                this.sendLocation(sendLocation, stopped, source, replayed, forceTrack = false)
                return
            }

            logger.i("Skipping track: useSyncRegion - no state change detected | source = $source")
            return
        }

        RadarState.updateLastSentAt(context)

        if (source == RadarLocationSource.FOREGROUND_LOCATION) {
            return
        }

        this.sendLocation(sendLocation, stopped, source, replayed, beacons = beacons)
    }

    private fun sendLocation(
        location: Location,
        stopped: Boolean,
        source: RadarLocationSource,
        replayed: Boolean,
        forceTrack: Boolean = true,
        beacons: Array<RadarBeacon>? = null
    ) {
        val options = Radar.getTrackingOptions()
        val foregroundService = RadarSettings.getForegroundService(context)

        if (options.foregroundServiceEnabled && foregroundService.updatesOnly) {
            this.startForegroundService(foregroundService)
        }

        logger.d("Sending location | source = $source; location = $location; stopped = $stopped; replayed = $replayed")

        val locationManager = this

        val callTrackApi = { beacons: Array<RadarBeacon>? ->
            this.apiClient.track(
                location,
                stopped,
                RadarActivityLifecycleCallbacks.foreground,
                source,
                replayed,
                beacons,
                callback = object : RadarTrackApiCallback {
                    override fun onComplete(
                        status: RadarStatus,
                        res: JSONObject?,
                        events: Array<RadarEvent>?,
                        user: RadarUser?,
                        nearbyGeofences: Array<RadarGeofence>?,
                        config: RadarConfig?,
                        token: RadarVerifiedLocationToken?
                    ) {
                        if (RadarSettings.getSdkConfiguration(context).useSyncRegion) {
                            if (status == RadarStatus.SUCCESS && user != null) {
                                Radar.syncManager.reconcileSyncState(user)

                                events?.forEach { event ->
                                    if (event.type == RadarEvent.RadarEventType.USER_DWELLED_IN_GEOFENCE) {
                                        val geofenceId = event.geofence?._id ?: return@forEach
                                        Radar.syncManager.markDwellFired(geofenceId)
                                    }
                                }
                            } else {
                                Radar.syncManager.rollbackSyncState()
                            }
                        }

                        locationManager.replaceSyncedGeofences(nearbyGeofences)

                        if (options.foregroundServiceEnabled && foregroundService.updatesOnly) {
                            locationManager.stopForegroundService()
                        }

                        updateTrackingFromMeta(config?.meta)
                    }
                }
            )
        }

        // Like iOS, beacon monitoring events track right away with the beacons the user is inside,
        // without searching, ranging or monitoring beacons again.
        if (source == RadarLocationSource.BEACON_ENTER || source == RadarLocationSource.BEACON_EXIT) {
            callTrackApi(beacons ?: arrayOf())
            return
        }

        if (options.beacons && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            permissionsHelper.bluetoothPermissionsGranted(context)
        ) {
            val cache = stopped

            if (RadarSettings.getSdkConfiguration(context).useSyncRegion &&
                Radar.syncManager.hasSyncedRegion() &&
                !Radar.syncManager.isOutsideSyncedRegion(location)
            ) {
                val syncedBeacons = Radar.syncManager.getBeacons(location).toTypedArray()
                logger.i("Sync region beacon ranging | syncedBeacons = ${syncedBeacons.size}, forceTrack = $forceTrack")
                if (syncedBeacons.isNotEmpty()) {
                    Radar.beaconMonitoringManager.startMonitoringBeacons(syncedBeacons)
                    Radar.oneShotBeaconManager.rangeBeacons(
                        syncedBeacons,
                        true,
                        object : Radar.RadarBeaconCallback {
                            override fun onComplete(status: RadarStatus, beacons: Array<RadarBeacon>?) {
                                if (status != RadarStatus.SUCCESS) {
                                    logger.i("Beacon ranging failed | status = $status, forceTrack = $forceTrack")
                                    if (forceTrack) {
                                        callTrackApi(null)
                                    } else if (options.foregroundServiceEnabled && foregroundService.updatesOnly) {
                                        locationManager.stopForegroundService()
                                    }
                                    return
                                }
                                if (beacons == null) {
                                    logger.i("Beacon ranging returned null beacons | forceTrack = $forceTrack")
                                    if (forceTrack) {
                                        callTrackApi(arrayOf())
                                    } else if (options.foregroundServiceEnabled && foregroundService.updatesOnly) {
                                        locationManager.stopForegroundService()
                                    }
                                    return
                                }

                                val syncedBeaconMap = syncedBeacons.associateBy { Triple(it.uuid.lowercase(), it.major, it.minor) }
                                val matchedIds = beacons.mapNotNull { ranged ->
                                    syncedBeaconMap[Triple(ranged.uuid.lowercase(), ranged.major, ranged.minor)]?._id
                                }
                                logger.i("Beacon ID matching | synced=${syncedBeaconMap.size}, ranged=${beacons.size}, matchedIds=$matchedIds")
                                if (forceTrack) {
                                    logger.i("Beacon ranging complete (forceTrack) | beacons = ${beacons.size}, matchedIds = ${matchedIds.size}")
                                    Radar.syncManager.saveBeaconState(matchedIds)
                                    callTrackApi(beacons)
                                } else {
                                    val rangedIds = matchedIds.toSet()
                                    if (Radar.syncManager.hasBeaconStateChanged(rangedIds)) {
                                        logger.i("Beacon state changed, sending track | rangedIds = $rangedIds")
                                        RadarState.updateLastSentAt(context)
                                        Radar.syncManager.saveBeaconState(rangedIds.toList())
                                        callTrackApi(beacons)
                                    } else {
                                        logger.i("Skipping track: beacon state unchanged after BLE ranging")
                                        if (options.foregroundServiceEnabled && foregroundService.updatesOnly) {
                                            locationManager.stopForegroundService()
                                        }
                                    }
                                }
                            }
                        }
                    )
                } else {
                    if (forceTrack) {
                        logger.i("No synced beacons nearby, sending empty beacons | forceTrack = $forceTrack")
                        callTrackApi(arrayOf())
                    }
                }
            } else {
                this.apiClient.searchBeacons(
                    location,
                    RadarNearbyBeaconSearch.RADIUS_METERS,
                    RadarNearbyBeaconSearch.LIMIT,
                    object : RadarApiClient.RadarSearchBeaconsApiCallback {
                        override fun onComplete(
                            status: RadarStatus,
                            res: JSONObject?,
                            beacons: Array<RadarBeacon>?,
                            uuids: Array<String>?,
                            uids: Array<String>?
                        ) {
                            if (!uuids.isNullOrEmpty() || !uids.isNullOrEmpty()) {
                                Radar.beaconMonitoringManager.startMonitoringBeaconUUIDs(uuids, uids)

                                Radar.oneShotBeaconManager.rangeBeaconUUIDs(
                                    uuids,
                                    uids,
                                    true,
                                    object : Radar.RadarBeaconCallback {
                                        override fun onComplete(
                                            status: RadarStatus,
                                            beacons: Array<RadarBeacon>?
                                        ) {
                                            if (status != RadarStatus.SUCCESS || beacons == null) {
                                                callTrackApi(null)

                                                return
                                            }

                                            callTrackApi(beacons)
                                        }
                                    }
                                )
                            } else if (beacons != null) {
                                Radar.beaconMonitoringManager.startMonitoringBeacons(beacons)

                                Radar.oneShotBeaconManager.rangeBeacons(
                                    beacons,
                                    true,
                                    object : Radar.RadarBeaconCallback {
                                        override fun onComplete(
                                            status: RadarStatus,
                                            beacons: Array<RadarBeacon>?
                                        ) {
                                            if (status != RadarStatus.SUCCESS || beacons == null) {
                                                callTrackApi(null)

                                                return
                                            }

                                            callTrackApi(beacons)
                                        }
                                    }
                                )
                            } else {
                                callTrackApi(arrayOf())
                            }
                        }
                    },
                    cache
                )
            }
        } else {
            callTrackApi(null)
        }
    }

    private fun startForegroundService(foregroundService: RadarTrackingOptions.RadarTrackingOptionsForegroundService) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!RadarForegroundService.started && !RadarForegroundService.isDeclared(context)) {
                if (!loggedForegroundServiceNotDeclared) {
                    logger.e(
                        "Not starting foreground service. RadarForegroundService is not declared in your app manifest. " +
                            "Declare it to use foregroundServiceEnabled: " +
                            "https://docs.radar.com/sdk/android#foreground-services-and-google-play-policy"
                    )
                    loggedForegroundServiceNotDeclared = true
                }
                return
            }

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
                        .putExtra("channelName", foregroundService.channelName)
                    logger.d("Starting foreground service with intent | intent = $intent")
                    if (context.applicationContext.startForegroundService(intent) != null) {
                        RadarForegroundService.started = true
                    }
                }
            } catch (e: Exception) {
                logger.e("Error starting foreground service with intent", RadarLogType.SDK_EXCEPTION, e)
            }
        }
    }

    private fun stopForegroundService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && RadarForegroundService.isDeclared(context)) {
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
