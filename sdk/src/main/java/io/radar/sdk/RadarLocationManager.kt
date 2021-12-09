package io.radar.sdk

import android.annotation.SuppressLint
import android.app.NotificationManager
import android.content.Intent
import android.location.Location
import android.os.Build
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationRequest
import io.radar.sdk.Radar.RadarLocationCallback
import io.radar.sdk.Radar.RadarLocationSource
import io.radar.sdk.Radar.RadarStatus
import io.radar.sdk.RadarApiClient.RadarTrackApiCallback
import io.radar.sdk.RadarTrackingOptions.RadarTrackingOptionsDesiredAccuracy
import io.radar.sdk.model.RadarBeacon
import io.radar.sdk.model.RadarCircleGeometry
import io.radar.sdk.model.RadarCoordinate
import io.radar.sdk.model.RadarEvent
import io.radar.sdk.model.RadarGeofence
import io.radar.sdk.model.RadarPolygonGeometry
import io.radar.sdk.model.RadarUser
import org.json.JSONObject
import java.util.*
import kotlin.collections.ArrayList

@Suppress("TooManyFunctions")
@SuppressLint("MissingPermission")
internal class RadarLocationManager(
    private val context: RadarApplication,
    private val locationClient: FusedLocationProviderClient,
    private val permissionsHelper: RadarPermissionsHelper
) {

    private val geofencingClient = GeofencingClient(context)
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

            context.logger.d("Calling callbacks", "callbacks.size" to callbacks.size)

            for (callback in callbacks) {
                callback.onComplete(status, location, context.state.getStopped())
            }
            callbacks.clear()
        }
    }

    fun getLocation(callback: RadarLocationCallback? = null) {
        getLocation(RadarTrackingOptionsDesiredAccuracy.MEDIUM, RadarLocationSource.FOREGROUND_LOCATION, callback)
    }

    fun getLocation(
        desiredAccuracy: RadarTrackingOptionsDesiredAccuracy,
        source: RadarLocationSource,
        callback: RadarLocationCallback? = null
    ) {
        if (!permissionsHelper.fineLocationPermissionGranted(context)
            && !permissionsHelper.coarseLocationPermissionGranted(context)
        ) {
            context.sendError(RadarStatus.ERROR_PERMISSIONS)
            callback?.onComplete(RadarStatus.ERROR_PERMISSIONS)
            return
        }

        this.addCallback(callback)

        val locationManager = this

        val desiredPriority = when (desiredAccuracy) {
            RadarTrackingOptionsDesiredAccuracy.HIGH -> LocationRequest.PRIORITY_HIGH_ACCURACY
            RadarTrackingOptionsDesiredAccuracy.MEDIUM -> LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY
            RadarTrackingOptionsDesiredAccuracy.LOW -> LocationRequest.PRIORITY_LOW_POWER
            RadarTrackingOptionsDesiredAccuracy.NONE -> LocationRequest.PRIORITY_NO_POWER
        }

        context.logger.d("Requesting location")

        locationClient.getCurrentLocation(desiredPriority, null).addOnSuccessListener { location ->
            if (location == null) {
                context.logger.d("Location timeout")

                callCallbacks(RadarStatus.ERROR_LOCATION)
            } else {
                context.logger.d("Successfully requested location")

                locationManager.handleLocation(location, source)
            }


        }.addOnCanceledListener {
            context.logger.d("Location request canceled")

            callCallbacks(RadarStatus.ERROR_LOCATION)
        }
    }

    fun startTracking(options: RadarTrackingOptions = RadarTrackingOptions.EFFICIENT) {
        this.stopLocationUpdates()

        if (!permissionsHelper.fineLocationPermissionGranted(context)
            && !permissionsHelper.coarseLocationPermissionGranted(context)
        ) {
            context.sendError(RadarStatus.ERROR_PERMISSIONS)
            return
        }

        context.settings.setTracking(true)
        context.settings.setTrackingOptions(options)
        this.updateTracking()
    }

    fun stopTracking() {
        this.started = false
        context.settings.setTracking(false)
        this.updateTracking()
    }

    private fun requiresStartingLocationUpdates(
        desiredAccuracy: RadarTrackingOptionsDesiredAccuracy,
        interval: Int,
        fastestInterval: Int
    ): Boolean {
        return desiredAccuracy != startedDesiredAccuracy
                || interval != startedInterval
                || fastestInterval != startedFastestInterval
    }

    private fun startLocationUpdates(
        desiredAccuracy: RadarTrackingOptionsDesiredAccuracy,
        interval: Int,
        fastestInterval: Int
    ) {
        if (!started || requiresStartingLocationUpdates(desiredAccuracy, interval, fastestInterval)) {
            val priority = when (desiredAccuracy) {
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

            locationClient.requestLocationUpdates(
                locationRequest,
                RadarLocationReceiver.getLocationPendingIntent(context)
            )

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
        context.logger.d("Handling beacon", "source" to source)

        this.getLocation(RadarTrackingOptionsDesiredAccuracy.MEDIUM, source)
    }

    internal fun handleBootCompleted() {
        context.logger.d("Handling boot completed")

        this.started = false
        context.state.setStopped(false)

        locationClient.lastLocation.addOnSuccessListener { location: Location? ->
            updateTracking(location)
        }.addOnFailureListener {
            updateTracking()
        }
    }

    @Suppress("ComplexMethod", "LongMethod")
    internal fun updateTracking(location: Location? = null) {
        var tracking = context.settings.getTracking()
        val options = context.settings.getTrackingOptions()

        context.logger.d("Updating tracking", mapOf("options" to options, "location" to location))

        val now = Date()
        if (!tracking && options.startTrackingAfter != null && options.startTrackingAfter!!.before(now)) {
            context.logger.d("Starting time-based tracking", "startTrackingAfter" to options.startTrackingAfter)
            tracking = true
            context.settings.setTracking(true)
        } else if (tracking && options.stopTrackingAfter != null && options.stopTrackingAfter!!.before(now)) {
            context.logger.d("Stopping time-based tracking ", "startTrackingAfter" to options.startTrackingAfter)
            tracking = false
            context.settings.setTracking(false)
        }

        val foregroundService = options.foregroundService
        if (tracking) {
            if (foregroundService != null) {
                if (!foregroundService.updatesOnly) {
                    this.startForegroundService(foregroundService)
                }
            } else if (RadarForegroundService.started) {
                this.stopForegroundService()
            }

            val stopped = context.state.getStopped()
            if (stopped) {
                if (options.desiredStoppedUpdateInterval == 0) {
                    this.stopLocationUpdates()
                } else {
                    this.startLocationUpdates(
                        options.desiredAccuracy,
                        options.desiredStoppedUpdateInterval,
                        options.fastestStoppedUpdateInterval
                    )
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
                    this.startLocationUpdates(
                        options.desiredAccuracy,
                        options.desiredMovingUpdateInterval,
                        options.fastestMovingUpdateInterval
                    )
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
                context.beaconManager?.stopMonitoringBeacons()
            }
        }
    }

    @Suppress("LongMethod")
    private fun replaceBubbleGeofence(location: Location?, stopped: Boolean) {
        if (location == null) {
            return
        }

        this.removeBubbleGeofences()

        val options = context.settings.getTrackingOptions()

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

            context.logger.d(
                "Adding stopped bubble geofence", mapOf(
                    "latitude" to location.latitude,
                    "longitude" to location.longitude,
                    "radius" to radius,
                    "identifier" to identifier
                )
            )

            geofencingClient.addGeofences(request, RadarLocationReceiver.getBubbleGeofencePendingIntent(context)).run {
                addOnSuccessListener {
                    context.logger.d("Successfully added stopped bubble geofence")
                }
                addOnFailureListener {
                    context.logger.d("Error adding stopped bubble geofence", "message" to it.message)
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

            context.logger.d(
                "Adding moving bubble geofence", mapOf(
                    "latitude" to location.latitude,
                    "longitude" to location.longitude,
                    "radius" to radius,
                    "identifier" to identifier
                )
            )

            geofencingClient.addGeofences(request, RadarLocationReceiver.getBubbleGeofencePendingIntent(context)).run {
                addOnSuccessListener {
                    context.logger.d("Successfully added moving bubble geofence")
                }
                addOnFailureListener {
                    context.logger.d("Error adding moving bubble geofence", "message" to it.message)
                }
            }
        }
    }

    @Suppress("LongMethod")
    private fun replaceSyncedGeofences(radarGeofences: Array<RadarGeofence>?) {
        this.removeSyncedGeofences()

        val options = context.settings.getTrackingOptions()
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
                        .setTransitionTypes(
                            Geofence.GEOFENCE_TRANSITION_ENTER
                                    or Geofence.GEOFENCE_TRANSITION_DWELL
                                    or Geofence.GEOFENCE_TRANSITION_EXIT
                        )
                        .build()
                    geofences.add(geofence)

                    context.logger.d(
                        "Adding synced geofence", mapOf(
                            "latitude" to center.latitude,
                            "longitude" to center.longitude,
                            "radius" to radius,
                            "identifier" to identifier
                        )
                    )
                } catch (e: Exception) {
                    context.logger.d(
                        "Error building synced geofence", mapOf(
                            "latitude" to center.latitude,
                            "longitude" to center.longitude,
                            "radius" to radius
                        )
                    )
                }
            }
        }

        if (geofences.size == 0) {
            context.logger.d("No synced geofences")

            return
        }

        val request = GeofencingRequest.Builder()
            .addGeofences(geofences)
            .setInitialTrigger(0)
            .build()

        geofencingClient.addGeofences(request, RadarLocationReceiver.getSyncedGeofencesPendingIntent(context)).run {
            addOnSuccessListener {
                context.logger.d("Successfully added synced geofences")
            }
            addOnFailureListener {
                context.logger.d("Error adding synced geofences", "message" to it.message)
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

    @Suppress("ComplexMethod", "ComplexCondition", "ReturnCount", "LongMethod")
    fun handleLocation(location: Location?, source: RadarLocationSource) {
        context.logger.d("Handling location", "location" to location)

        if (location == null || !RadarUtils.valid(location)) {
            context.logger.d("Invalid location", mapOf("source" to source, "location" to location))

            context.sendError(RadarStatus.ERROR_LOCATION)

            callCallbacks(RadarStatus.ERROR_LOCATION)

            return
        }

        val options = context.settings.getTrackingOptions()
        val wasStopped = context.state.getStopped()
        var stopped: Boolean

        val force = (source == RadarLocationSource.FOREGROUND_LOCATION || source == RadarLocationSource.MANUAL_LOCATION || source == RadarLocationSource.BEACON_ENTER)
        if (!force && location.accuracy > 1000 && options.desiredAccuracy != RadarTrackingOptionsDesiredAccuracy.LOW) {
            context.logger.d("Skipping location: inaccurate", "accuracy" to location.accuracy)
            this.updateTracking(location)

            return
        }

        var distance = Float.MAX_VALUE
        val duration: Long
        if (options.stopDistance > 0 && options.stopDuration > 0) {
            var lastMovedLocation = context.state.getLastMovedLocation()
            if (lastMovedLocation == null) {
                lastMovedLocation = location
                context.state.setLastMovedLocation(lastMovedLocation)
            }
            var lastMovedAt = context.state.getLastMovedAt()
            if (lastMovedAt == 0L) {
                lastMovedAt = location.time
                context.state.setLastMovedAt(lastMovedAt)
            }
            if (!force && lastMovedAt > location.time) {
                context.logger.d(
                    "Skipping location: old", mapOf(
                        "lastMovedAt" to lastMovedAt,
                        "location.time" to location.time
                    )
                )
                return
            }
            distance = location.distanceTo(lastMovedLocation)
            duration = (location.time - lastMovedAt) / 1000
            stopped = (distance < options.stopDistance && duration > options.stopDuration)
            context.logger.d(
                "Calculating stopped", mapOf(
                    "stopped" to stopped,
                    "distance" to distance,
                    "duration" to duration,
                    "location.time" to location.time,
                    "lastMovedAt" to lastMovedAt
                )
            )

            if (distance > options.stopDistance) {
                context.state.setLastMovedLocation(location)

                if (!stopped) {
                    context.state.setLastMovedAt(location.time)
                }
            }
        } else {
            stopped = force || source == RadarLocationSource.GEOFENCE_DWELL
        }
        val justStopped = stopped && !wasStopped
        context.state.setStopped(stopped)
        context.sendClientLocation(location, stopped, source)

        if (source != RadarLocationSource.MANUAL_LOCATION) {
            this.updateTracking(location)
        }

        callCallbacks(RadarStatus.SUCCESS, location)

        var sendLocation = location

        val lastFailedStoppedLocation = context.state.getLastFailedStoppedLocation()
        var replayed = false
        if (options.replay == RadarTrackingOptions.RadarTrackingOptionsReplay.STOPS
            && lastFailedStoppedLocation != null && !justStopped
        ) {
            sendLocation = lastFailedStoppedLocation
            stopped = true
            replayed = true
            context.state.setLastFailedStoppedLocation(null)
            context.logger.d("Replaying location", mapOf("location" to location, "stopped" to stopped))
        }

        val lastSentAt = context.state.getLastSentAt()
        val ignoreSync =
            lastSentAt == 0L || this.callbacks.count() > 0 || justStopped || replayed
        val now = System.currentTimeMillis()
        val lastSyncInterval = (now - lastSentAt) / 1000L
        if (!ignoreSync) {
            if (!force && stopped && wasStopped && didMoveFarEnoughToTrack(distance, options)) {
                context.logger.d(
                    "Skipping sync: already stopped", mapOf(
                        "stopped" to stopped,
                        "wasStopped" to wasStopped
                    )
                )
                return
            }

            if (lastSyncInterval < options.desiredSyncInterval) {
                context.logger.d(
                    "Skipping sync: desired sync interval", mapOf(
                        "desiredSyncInterval" to options.desiredSyncInterval,
                        "lastSyncInterval" to lastSyncInterval
                    )
                )
                return
            }

            if (!force && !justStopped && lastSyncInterval < 1) {
                context.logger.d(
                    "Skipping sync: rate limit", mapOf(
                        "justStopped" to justStopped,
                        "lastSyncInterval" to lastSyncInterval
                    )
                )
                return
            }

            if (options.sync == RadarTrackingOptions.RadarTrackingOptionsSync.NONE) {
                context.logger.d("Skipping sync: sync mode", "sync" to options.sync)
                return
            }

            val canExit = context.state.getCanExit()
            if (!canExit && options.sync == RadarTrackingOptions.RadarTrackingOptionsSync.STOPS_AND_EXITS) {
                context.logger.d("Skipping sync: can't exit", mapOf("sync" to options.sync, "canExit" to canExit))
                return
            }
        }
        context.state.updateLastSentAt()

        if (source == RadarLocationSource.FOREGROUND_LOCATION) {
            return
        }

        this.sendLocation(sendLocation, stopped, source, replayed)
    }

    private fun didMoveFarEnoughToTrack(distance: Float, options: RadarTrackingOptions): Boolean {
        return distance < options.stopDistance &&
                (options.desiredStoppedUpdateInterval == 0
                        || options.sync != RadarTrackingOptions.RadarTrackingOptionsSync.ALL)
    }

    private fun sendLocation(location: Location, stopped: Boolean, source: RadarLocationSource, replayed: Boolean) {
        val options = context.settings.getTrackingOptions()
        val foregroundService = options.foregroundService

        if (foregroundService != null && foregroundService.updatesOnly) {
            this.startForegroundService(foregroundService)
        }
        context.logger.d(
            "Sending location", mapOf(
                "source" to source,
                "location" to location,
                "stopped" to stopped,
                "replayed" to replayed
            )
        )
        val locationManager = this

        val callTrackApi = { nearbyBeacons: Array<String>? ->
            context.apiClient.track(
                location,
                stopped,
                RadarActivityLifecycleCallbacks.foreground,
                source,
                replayed,
                nearbyBeacons,
                object : RadarTrackApiCallback {
                    override fun onComplete(
                        status: RadarStatus,
                        res: JSONObject?,
                        events: Array<RadarEvent>?,
                        user: RadarUser?,
                        nearbyGeofences: Array<RadarGeofence>?
                    ) {
                        if (user != null) {
                            val inGeofences = user.geofences != null && user.geofences.isNotEmpty()
                            val atPlace = user.place != null
                            val atHome = user.insights?.state?.home ?: false
                            val atOffice = user.insights?.state?.office ?: false
                            val canExit = inGeofences || atPlace || atHome || atOffice
                            context.state.setCanExit(canExit)
                        }

                        locationManager.replaceSyncedGeofences(nearbyGeofences)

                        if (foregroundService != null && foregroundService.updatesOnly) {
                            locationManager.stopForegroundService()
                        }
                    }
                })
        }

        if (options.beacons && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            context.apiClient.searchBeacons(location, 1000, 10, object : RadarApiClient.RadarSearchBeaconsApiCallback {
                override fun onComplete(status: RadarStatus, res: JSONObject?, beacons: Array<RadarBeacon>?) {
                    if (status != RadarStatus.SUCCESS || beacons == null) {
                        callTrackApi(null)

                        return
                    }

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.beaconManager?.startMonitoringBeacons(beacons)
                    }

                    context.beaconManager?.rangeBeacons(beacons, object : Radar.RadarBeaconCallback {
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                if (RadarForegroundService.started) {
                    context.logger.d("Already started foreground service")
                } else {
                    val intent = Intent(context, RadarForegroundService::class.java)
                    intent.action = "start"
                    intent.putExtra("id", foregroundService.id)
                        .putExtra("importance", foregroundService.importance ?: NotificationManager.IMPORTANCE_DEFAULT)
                        .putExtra("title", foregroundService.title)
                        .putExtra("text", foregroundService.text)
                        .putExtra("icon", foregroundService.icon)
                        .putExtra("activity", foregroundService.activity)
                    context.logger.d("Starting foreground service with intent | $intent")
                    context.applicationContext.startForegroundService(intent)
                    RadarForegroundService.started = true
                }
            } catch (e: Exception) {
                context.logger.e("Error starting foreground service with intent", e)
            }
        }
    }

    private fun stopForegroundService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val intent = Intent(context, RadarForegroundService::class.java)
                intent.action = "stop"
                context.logger.d("Stopping foreground service with intent")
                context.applicationContext.startService(intent)
                RadarForegroundService.started = false
            } catch (e: Exception) {
                context.logger.e("Error stopping foreground service with intent", e)
            }
        }
    }

}