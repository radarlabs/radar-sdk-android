package io.radar.sdk

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

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
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
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
        if (!Radar.initialized) {
            Radar.initialize(context)
        }

        Radar.logger.d("Received broadcast | action = ${intent.action}")

        when (intent.action) {
            ACTION_BUBBLE_GEOFENCE, ACTION_SYNCED_GEOFENCES -> {
                val location = Radar.locationManager.getLocationFromGeofenceIntent(intent)
                val source = Radar.locationManager.getSourceFromGeofenceIntent(intent)

                if (location == null || source == null) {
                    return
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !RadarForegroundService.started) {
                    RadarJobScheduler.scheduleJob(context, location, source)
                } else {
                    Radar.handleLocation(context, location, source)
                }
            }
            ACTION_LOCATION -> {
                val location = Radar.locationManager.getLocationFromLocationIntent(intent)
                val source = Radar.RadarLocationSource.BACKGROUND_LOCATION

                if (location == null) {
                    return
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !RadarForegroundService.started) {
                    RadarJobScheduler.scheduleJob(context, location, source)
                } else {
                    Radar.handleLocation(context, location, source)
                }
            }
            ACTION_BEACON -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val bleCallbackType = intent.getIntExtra(BluetoothLeScanner.EXTRA_CALLBACK_TYPE, -1)
                    if (bleCallbackType != -1) {
                        val source = if (bleCallbackType == ScanSettings.CALLBACK_TYPE_MATCH_LOST) Radar.RadarLocationSource.BEACON_EXIT else Radar.RadarLocationSource.BEACON_ENTER
                        val scanResults: ArrayList<ScanResult>? = intent.getParcelableArrayListExtra(BluetoothLeScanner.EXTRA_LIST_SCAN_RESULT)
                        try {
                            val beacons = RadarBeaconUtils.beaconsForScanResults(scanResults)
                            RadarJobScheduler.scheduleJob(context, beacons, source)
                        } catch (e: Exception) {
                            Radar.logger.e("Error scheduling beacons job", Radar.RadarLogType.SDK_EXCEPTION, e)
                        }
                    }
                }
            }
            Intent.ACTION_BOOT_COMPLETED -> {
                Radar.handleBootCompleted(context)
            }
        }
    }

}
