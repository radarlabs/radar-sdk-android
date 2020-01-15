package io.radar.sdk

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import com.google.android.gms.location.LocationResult

class RadarLocationReceiver : BroadcastReceiver() {

    internal companion object {

        internal const val ACTION_LOCATION = "io.radar.sdk.LocationReceiver.LOCATION"
        internal const val ACTION_GEOFENCE = "io.radar.sdk.LocationReceiver.GEOFENCE"

        internal fun getGeofencePendingIntent(context: Context): PendingIntent {
            val intent = baseIntent(context).apply {
                action = ACTION_GEOFENCE
            }
            return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        }

        internal fun getLocationPendingIntent(context: Context): PendingIntent {
            val intent = baseIntent(context).apply {
                action = ACTION_LOCATION
            }
            return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        }

        private fun baseIntent(context: Context): Intent = Intent(context, RadarLocationReceiver::class.java)

    }

    @SuppressLint("MissingPermission")
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_GEOFENCE -> {
                val event = GeofencingEvent.fromIntent(intent)
                event?.triggeringLocation?.also {
                    val source = when (event.geofenceTransition) {
                        Geofence.GEOFENCE_TRANSITION_ENTER -> Radar.RadarLocationSource.GEOFENCE_ENTER
                        Geofence.GEOFENCE_TRANSITION_DWELL -> Radar.RadarLocationSource.GEOFENCE_DWELL
                        else -> Radar.RadarLocationSource.GEOFENCE_EXIT
                    }
                    RadarJobScheduler.scheduleJob(context, it, source)
                }
            }
            ACTION_LOCATION -> {
                val result = LocationResult.extractResult(intent)
                result?.lastLocation?.also {
                    RadarJobScheduler.scheduleJob(context, it, Radar.RadarLocationSource.BACKGROUND_LOCATION)
                }
            }
            Intent.ACTION_BOOT_COMPLETED -> {
                Radar.handleBootCompleted(context)
            }
        }
    }

}