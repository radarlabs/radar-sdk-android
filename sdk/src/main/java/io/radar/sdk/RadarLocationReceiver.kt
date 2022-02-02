package io.radar.sdk

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import com.google.android.gms.location.LocationResult

class RadarLocationReceiver : BroadcastReceiver() {

    internal companion object {

        internal const val ACTION_LOCATION = "io.radar.sdk.LocationReceiver.LOCATION"
        internal const val ACTION_BUBBLE_GEOFENCE = "io.radar.sdk.LocationReceiver.GEOFENCE"
        internal const val ACTION_SYNCED_GEOFENCES = "io.radar.sdk.LocationReceiver.SYNCED_GEOFENCES"
        internal const val ACTION_BEACON = "io.radar.sdk.LocationReceiver.BEACON"

        private const val REQUEST_CODE_LOCATION = 201605250
        private const val REQUEST_CODE_BUBBLE_GEOFENCE = 201605251
        private const val REQUEST_CODE_SYNCED_GEOFENCES = 201605252
        private const val REQUEST_CODE_BEACON = 201605253

        internal fun getLocationPendingIntent(context: Context): PendingIntent {
            val intent = baseIntent(context).apply {
                action = ACTION_LOCATION
            }
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
            return PendingIntent.getBroadcast(
                context,
                REQUEST_CODE_LOCATION,
                intent,
                flags
            )
        }

        internal fun getBubbleGeofencePendingIntent(context: Context): PendingIntent {
            val intent = baseIntent(context).apply {
                action = ACTION_BUBBLE_GEOFENCE
            }
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
            return PendingIntent.getBroadcast(
                context,
                REQUEST_CODE_BUBBLE_GEOFENCE,
                intent,
                flags
            )
        }

        internal fun getSyncedGeofencesPendingIntent(context: Context): PendingIntent {
            val intent = baseIntent(context).apply {
                action = ACTION_SYNCED_GEOFENCES
            }
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
            return PendingIntent.getBroadcast(
                context,
                REQUEST_CODE_SYNCED_GEOFENCES,
                intent,
                flags
            )
        }

        internal fun getBeaconPendingIntent(context: Context): PendingIntent {
            val intent = baseIntent(context).apply {
                action = ACTION_BEACON
            }
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
            return PendingIntent.getBroadcast(
                context,
                REQUEST_CODE_BEACON,
                intent,
                flags
            )
        }

        private fun baseIntent(context: Context): Intent = Intent(context, RadarLocationReceiver::class.java)

    }

    @SuppressLint("MissingPermission")
    override fun onReceive(context: Context, intent: Intent) {
        Radar.app.logger.d("Broadcast Received", "action" to intent.action)
        when (intent.action) {
            ACTION_BUBBLE_GEOFENCE, ACTION_SYNCED_GEOFENCES -> {
                val event = GeofencingEvent.fromIntent(intent)
                event.triggeringLocation.also {
                    val source = when (event.geofenceTransition) {
                        Geofence.GEOFENCE_TRANSITION_ENTER -> Radar.RadarLocationSource.GEOFENCE_ENTER
                        Geofence.GEOFENCE_TRANSITION_DWELL -> Radar.RadarLocationSource.GEOFENCE_DWELL
                        else -> Radar.RadarLocationSource.GEOFENCE_EXIT
                    }

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && !RadarForegroundService.started) {
                        RadarJobScheduler.scheduleJob(context, it, source)
                    } else {
                        Radar.handleLocation(context, it, source)
                    }
                }
            }
            ACTION_LOCATION -> {
                val result = LocationResult.extractResult(intent)
                result.lastLocation.also {
                    val source = Radar.RadarLocationSource.BACKGROUND_LOCATION

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && !RadarForegroundService.started) {
                        RadarJobScheduler.scheduleJob(context, it, source)
                    } else {
                        Radar.handleLocation(context, it, source)
                    }
                }
            }
            ACTION_BEACON -> {
                Radar.handleBeacon(context, Radar.RadarLocationSource.BEACON_ENTER)
            }
            Intent.ACTION_BOOT_COMPLETED -> {
                Radar.handleBootCompleted(context)
            }
        }
    }

}