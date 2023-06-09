package io.radar.sdk

import android.app.PendingIntent
import android.content.Intent
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

    override fun getLocationsFromLocationIntent(intent: Intent): List<Location>? {
        return listOf(mockLocation!!)
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

    override fun getLocationFromGeofenceIntent(intent: Intent): Location {
        return mockLocation!!
    }

    override fun getSourceFromGeofenceIntent(intent: Intent): Radar.RadarLocationSource {
        return Radar.RadarLocationSource.GEOFENCE_ENTER
    }
}