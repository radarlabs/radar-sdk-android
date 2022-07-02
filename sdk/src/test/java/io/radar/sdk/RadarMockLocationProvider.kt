package io.radar.sdk

import android.app.PendingIntent
import android.location.Location

internal class RadarMockLocationProvider() : RadarAbstractLocationClient() {

    internal var mockLocation: Location? = null

    override fun getCurrentLocation(
        desiredAccuracy: RadarTrackingOptions.RadarTrackingOptionsDesiredAccuracy,
        block: (location: Location?) -> Unit
    ) {
        block(mockLocation)
    }

    override fun requestLocationUpdates(
        desiredAccuracy: RadarTrackingOptions.RadarTrackingOptionsDesiredAccuracy,
        interval: Int,
        fastestInterval: Int,
        pendingIntent: PendingIntent
    ) {

    }

    override fun removeLocationUpdates(pendingIntent: PendingIntent) {

    }

    override fun getLastLocation(block: (location: Location?) -> Unit) {

    }

    override fun addGeofences(
        abstractGeofences: Array<RadarAbstractGeofence>,
        abstractGeofenceRequest: RadarAbstractGeofenceRequest,
        pendingIntent: PendingIntent,
        block: (success: Boolean) -> Unit
    ) {

    }

    override fun removeGeofences(pendingIntent: PendingIntent) {

    }

}