package io.radar.sdk

import android.app.PendingIntent
import android.content.Intent
import android.location.Location
import io.radar.sdk.Radar.RadarLogType

/**
 * Location client for an app that set a location services provider whose library is not in the app.
 *
 * Each call logs [message] and fails: location calls return `null` (callers report `ERROR_LOCATION`),
 * and geofence calls report failure. The app does not crash, and the problem is visible on all devices.
 */
internal class RadarUnavailableLocationClient(
    private val logger: RadarLogger,
    private val message: String
) : RadarAbstractLocationClient() {

    override fun getCurrentLocation(
        desiredAccuracy: RadarTrackingOptions.RadarTrackingOptionsDesiredAccuracy,
        block: (location: Location?) -> Unit
    ) {
        logError()
        block(null)
    }

    override fun requestLocationUpdates(
        desiredAccuracy: RadarTrackingOptions.RadarTrackingOptionsDesiredAccuracy,
        interval: Int,
        fastestInterval: Int,
        pendingIntent: PendingIntent
    ) {
        logError()
    }

    override fun removeLocationUpdates(pendingIntent: PendingIntent) {
        // No updates were requested, so there is nothing to remove.
    }

    override fun getLastLocation(block: (location: Location?) -> Unit) {
        logError()
        block(null)
    }

    override fun addGeofences(
        abstractGeofences: Array<RadarAbstractGeofence>,
        abstractGeofenceRequest: RadarAbstractGeofenceRequest,
        pendingIntent: PendingIntent,
        block: (success: Boolean) -> Unit
    ) {
        logError()
        block(false)
    }

    override fun removeGeofences(
        pendingIntent: PendingIntent,
        block: ((success: Boolean) -> Unit)?
    ) {
        // No geofences were added, so there is nothing to remove.
        block?.invoke(true)
    }

    override fun getLocationFromGeofenceIntent(intent: Intent): Location? = null

    override fun getSourceFromGeofenceIntent(intent: Intent): Radar.RadarLocationSource? = null

    override fun getLocationFromLocationIntent(intent: Intent): Location? = null

    private fun logError() {
        logger.e(message, RadarLogType.SDK_ERROR)
    }
}
