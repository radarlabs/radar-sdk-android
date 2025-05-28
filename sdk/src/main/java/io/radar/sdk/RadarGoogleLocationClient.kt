package io.radar.sdk

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.location.Location
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices

@SuppressLint("MissingPermission")
internal class RadarGoogleLocationClient(
    private val context: Context,
    private val logger: RadarLogger
): RadarAbstractLocationClient() {

    @SuppressLint("VisibleForTests")
    val locationClient = LocationServices.getFusedLocationProviderClient(context)
    val geofencingClient = LocationServices.getGeofencingClient(context)

    override fun getCurrentLocation(desiredAccuracy: RadarTrackingOptions.RadarTrackingOptionsDesiredAccuracy, block: (location: Location?) -> Unit) {
        val priority = priorityForDesiredAccuracy(desiredAccuracy)
        
        var currentLocationRequestBuilder = CurrentLocationRequest.Builder()
            .setPriority(priority)
        if (desiredAccuracy == RadarTrackingOptions.RadarTrackingOptionsDesiredAccuracy.HIGH) {
            currentLocationRequestBuilder = currentLocationRequestBuilder.setMaxUpdateAgeMillis(0)
        }
        
        val timeout = RadarSettings.getSdkConfiguration(context).locationManagerTimeout
        if (timeout > 0) {
            logger.d("Requesting location with timeout | timeout = $timeout")
            currentLocationRequestBuilder = currentLocationRequestBuilder.setDurationMillis(timeout.toLong())
        } else {
            logger.d("Requesting location with default timeout")
        }
        val currentLocationRequest = currentLocationRequestBuilder.build()

        locationClient.getCurrentLocation(currentLocationRequest, null).addOnSuccessListener { location ->
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

        var locationRequestBuilder = LocationRequest.Builder(priority, interval * 1000L)
            .setMinUpdateIntervalMillis(fastestInterval * 1000L)
        if (desiredAccuracy == RadarTrackingOptions.RadarTrackingOptionsDesiredAccuracy.HIGH) {
            locationRequestBuilder = locationRequestBuilder.setMaxUpdateAgeMillis(0)
        }
        val locationRequest = locationRequestBuilder.build()

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
        pendingIntent: PendingIntent,
        block: ((success: Boolean) -> Unit)?
    ) {
        geofencingClient.removeGeofences(pendingIntent).run {
            addOnSuccessListener {
                block?.invoke(true)
            }
            addOnFailureListener {
                block?.invoke(false)
            }
        }
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

    override fun getLocationFromLocationIntent(intent: Intent): Location? {
        if (intent == null) {
            return null
        }

        val result = LocationResult.extractResult(intent)

        if (result == null) {
            return null
        }

        return result.lastLocation
    }

}