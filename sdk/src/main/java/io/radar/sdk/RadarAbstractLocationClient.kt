package io.radar.sdk

import android.app.PendingIntent
import android.content.Intent
import android.location.Location

internal abstract class RadarAbstractLocationClient() {

    data class RadarAbstractGeofence(
        val requestId: String,
        val latitude: Double,
        val longitude: Double,
        val radius: Float,
        val transitionEnter: Boolean = false,
        val transitionExit: Boolean = false,
        val transitionDwell: Boolean = false,
        val dwellDuration: Int = 0
    )

    data class RadarAbstractGeofenceRequest(
        val initialTriggerEnter: Boolean = false,
        val initialTriggerExit: Boolean = false,
        val initialTriggerDwell: Boolean = false
    )

    abstract fun getCurrentLocation(
        desiredAccuracy: RadarTrackingOptions.RadarTrackingOptionsDesiredAccuracy,
        block: (location: Location?) -> Unit
    )

    abstract fun requestLocationUpdates(
        desiredAccuracy: RadarTrackingOptions.RadarTrackingOptionsDesiredAccuracy,
        interval: Int,
        fastestInterval: Int,
        pendingIntent: PendingIntent
    )

    abstract fun removeLocationUpdates(
        pendingIntent: PendingIntent
    )

    abstract fun getLastLocation(
        block: (location: Location?) -> Unit
    )

    abstract fun addGeofences(
        abstractGeofences: Array<RadarAbstractGeofence>,
        abstractGeofenceRequest: RadarAbstractGeofenceRequest,
        pendingIntent: PendingIntent,
        block: (success: Boolean) -> Unit
    )

    abstract fun removeGeofences(
        pendingIntent: PendingIntent,
        block: (success: Boolean) -> Unit
    )

    abstract fun getLocationFromGeofenceIntent(
        intent: Intent
    ): Location?

    abstract fun getSourceFromGeofenceIntent(
        intent: Intent
    ): Radar.RadarLocationSource?

    abstract fun getLocationFromLocationIntent(
        intent: Intent
    ): Location?

}