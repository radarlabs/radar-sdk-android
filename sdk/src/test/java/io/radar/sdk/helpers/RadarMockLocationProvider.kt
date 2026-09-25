package io.radar.sdk.helpers

import android.app.PendingIntent
import android.content.Intent
import android.location.Location
import io.radar.sdk.Radar
import io.radar.sdk.RadarAbstractLocationClient
import io.radar.sdk.RadarTrackingOptions

internal class RadarMockLocationProvider : RadarAbstractLocationClient() {

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

    override fun removeGeofences(pendingIntent: PendingIntent, block: ((success: Boolean) -> Unit)?) {
    }

    override fun getLocationFromGeofenceIntent(intent: Intent): Location = mockLocation!!

    override fun getSourceFromGeofenceIntent(intent: Intent): Radar.RadarLocationSource = Radar.RadarLocationSource.GEOFENCE_ENTER

    override fun getLocationFromLocationIntent(intent: Intent): Location = mockLocation!!
}
