package io.radar.sdk

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Build
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingEvent
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices

@SuppressLint("MissingPermission")
internal class RadarGoogleLocationClient(
    context: Context,
    private val logger: RadarLogger
): RadarAbstractLocationClient() {

    @SuppressLint("VisibleForTests")
    val locationClient = LocationServices.getFusedLocationProviderClient(context)
    val geofencingClient = LocationServices.getGeofencingClient(context)

    override fun getCurrentLocation(desiredAccuracy: RadarTrackingOptions.RadarTrackingOptionsDesiredAccuracy, block: (location: Location?) -> Unit) {
        val priority = priorityForDesiredAccuracy(desiredAccuracy)

        logger.d("Requesting location")

        locationClient.getCurrentLocation(priority, null).addOnSuccessListener { location ->
            logger.d("Received current location")

            block(location)
        }.addOnCanceledListener {
            block(null)
        }
    }

    override fun requestLocationUpdates(
        desiredAccuracy: RadarTrackingOptions.RadarTrackingOptionsDesiredAccuracy,
        interval: Int,
        fastestInterval: Int,
        pendingIntent: PendingIntent
    ) {
        val priority = priorityForDesiredAccuracy(desiredAccuracy)

        val locationRequest = LocationRequest().apply {
            this.priority = priority
            this.interval = interval * 1000L
            this.fastestInterval = fastestInterval * 1000L
            this.maxWaitTime = interval * 1000L
        }

        locationClient.requestLocationUpdates(locationRequest, pendingIntent)
    }

    override fun removeLocationUpdates(pendingIntent: PendingIntent) {
        locationClient.removeLocationUpdates(pendingIntent)
    }

    override fun getLastLocation(block: (location: Location?) -> Unit) {
        locationClient.lastLocation.addOnSuccessListener { location ->
            block(location)
        }.addOnFailureListener {
            block(null)
        }
    }

    override fun addGeofences(
        abstractGeofences: Array<RadarAbstractGeofence>,
        abstractGeofenceRequest: RadarAbstractGeofenceRequest,
        pendingIntent: PendingIntent,
        block: (success: Boolean) -> Unit
    ) {
        val geofences = mutableListOf<Geofence>()
        abstractGeofences.forEach { abstractGeofence ->
            var transitionTypes = 0
            if (abstractGeofence.transitionEnter) {
                transitionTypes = transitionTypes or Geofence.GEOFENCE_TRANSITION_ENTER
            }
            if (abstractGeofence.transitionExit) {
                transitionTypes = transitionTypes or Geofence.GEOFENCE_TRANSITION_EXIT
            }
            if (abstractGeofence.transitionDwell) {
                transitionTypes = transitionTypes or Geofence.GEOFENCE_TRANSITION_DWELL
            }

            val geofence = Geofence.Builder()
                .setRequestId(abstractGeofence.requestId)
                .setCircularRegion(abstractGeofence.latitude, abstractGeofence.longitude, abstractGeofence.radius)
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setTransitionTypes(transitionTypes)
                .setLoiteringDelay(abstractGeofence.dwellDuration)
                .build()

            geofences.add(geofence)
        }

        var initialTrigger = 0
        if (abstractGeofenceRequest.initialTriggerEnter) {
            initialTrigger = initialTrigger or Geofence.GEOFENCE_TRANSITION_ENTER
        }
        if (abstractGeofenceRequest.initialTriggerExit) {
            initialTrigger = initialTrigger or Geofence.GEOFENCE_TRANSITION_EXIT
        }
        if (abstractGeofenceRequest.initialTriggerDwell) {
            initialTrigger = initialTrigger or Geofence.GEOFENCE_TRANSITION_DWELL
        }

        val request = GeofencingRequest.Builder()
            .addGeofences(geofences)
            .setInitialTrigger(initialTrigger)
            .build()

        geofencingClient.addGeofences(request, pendingIntent).run {
            addOnSuccessListener {
                block(true)
            }
            addOnFailureListener {
                block(false)
            }
        }
    }

    override fun removeGeofences(
        pendingIntent: PendingIntent
    ) {
        geofencingClient.removeGeofences(pendingIntent)
    }

    private fun priorityForDesiredAccuracy(desiredAccuracy: RadarTrackingOptions.RadarTrackingOptionsDesiredAccuracy) =
        when(desiredAccuracy) {
            RadarTrackingOptions.RadarTrackingOptionsDesiredAccuracy.HIGH -> LocationRequest.PRIORITY_HIGH_ACCURACY
            RadarTrackingOptions.RadarTrackingOptionsDesiredAccuracy.MEDIUM -> LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY
            RadarTrackingOptions.RadarTrackingOptionsDesiredAccuracy.LOW -> LocationRequest.PRIORITY_LOW_POWER
            RadarTrackingOptions.RadarTrackingOptionsDesiredAccuracy.NONE -> LocationRequest.PRIORITY_NO_POWER
        }

    override fun getLocationFromGeofenceIntent(intent: Intent): Location? {
        if (intent == null) {
            return null
        }

        val event = GeofencingEvent.fromIntent(intent)

        if (event == null) {
            return null
        }

        return event.triggeringLocation
    }

    override fun getSourceFromGeofenceIntent(intent: Intent): Radar.RadarLocationSource? {
        if (intent == null) {
            return null
        }

        val event = GeofencingEvent.fromIntent(intent)

        if (event == null) {
            return null
        }

        return when (event.geofenceTransition) {
            Geofence.GEOFENCE_TRANSITION_ENTER -> Radar.RadarLocationSource.GEOFENCE_ENTER
            Geofence.GEOFENCE_TRANSITION_DWELL -> Radar.RadarLocationSource.GEOFENCE_DWELL
            else -> Radar.RadarLocationSource.GEOFENCE_EXIT
        }
    }

    override fun getLocationsFromLocationIntent(intent: Intent): List<Location>? {
        if (intent == null) {
            return null
        }

        val result = LocationResult.extractResult(intent)
        logger.d("Received location intent with result: $result")

        if (result == null) {
            return null
        }

        return result.locations
    }

}