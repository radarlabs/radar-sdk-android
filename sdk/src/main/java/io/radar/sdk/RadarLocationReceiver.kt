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
        internal const val ACTION_BLUETOOTH = "io.radar.sdk.LocationReceiver.BLUETOOTH"

        internal const val REQUEST_CODE_LOCATION = 201605250
        internal const val REQUEST_CODE_BUBBLE_GEOFENCE = 201605251
        internal const val REQUEST_CODE_SYNCED_GEOFENCES = 201605252
        internal const val REQUEST_CODE_BLUETOOTH = 201605253

        internal fun getLocationPendingIntent(context: Context): PendingIntent {
            val intent = baseIntent(context).apply {
                action = ACTION_LOCATION
            }
            return PendingIntent.getBroadcast(context, REQUEST_CODE_LOCATION, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        }

        internal fun getBubbleGeofencePendingIntent(context: Context): PendingIntent {
            val intent = baseIntent(context).apply {
                action = ACTION_BUBBLE_GEOFENCE
            }
            return PendingIntent.getBroadcast(context, REQUEST_CODE_BUBBLE_GEOFENCE, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        }

        internal fun getSyncedGeofencesPendingIntent(context: Context): PendingIntent {
            val intent = baseIntent(context).apply {
                action = ACTION_SYNCED_GEOFENCES
            }
            return PendingIntent.getBroadcast(context, REQUEST_CODE_SYNCED_GEOFENCES, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        }

        internal fun getBluetoothPendingIntent(context: Context): PendingIntent {
            val intent = baseIntent(context).apply {
                action = ACTION_BLUETOOTH
            }
            return PendingIntent.getBroadcast(context, REQUEST_CODE_BLUETOOTH, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        }

        private fun baseIntent(context: Context): Intent = Intent(context, RadarLocationReceiver::class.java)

    }

    @SuppressLint("MissingPermission")
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_BUBBLE_GEOFENCE, ACTION_SYNCED_GEOFENCES -> {
                val event = GeofencingEvent.fromIntent(intent)
                event?.triggeringLocation?.also {
                    val source = when (event.geofenceTransition) {
                        Geofence.GEOFENCE_TRANSITION_ENTER -> Radar.RadarLocationSource.GEOFENCE_ENTER
                        Geofence.GEOFENCE_TRANSITION_DWELL -> Radar.RadarLocationSource.GEOFENCE_DWELL
                        else -> Radar.RadarLocationSource.GEOFENCE_EXIT
                    }

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        RadarJobScheduler.scheduleJob(context, it, source)
                    } else {
                        Radar.handleLocation(context, it, source)
                    }
                }
            }
            ACTION_LOCATION -> {
                val result = LocationResult.extractResult(intent)
                result?.lastLocation?.also {
                    val source = Radar.RadarLocationSource.BACKGROUND_LOCATION

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        RadarJobScheduler.scheduleJob(context, it, source)
                    } else {
                        Radar.handleLocation(context, it, source)
                    }
                }
            }
            ACTION_BLUETOOTH -> {
                Radar.locationManager.handleBluetooth()
            }
            Intent.ACTION_BOOT_COMPLETED -> {
                Radar.handleBootCompleted(context)
            }
        }
    }

}