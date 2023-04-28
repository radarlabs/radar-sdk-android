package io.radar.sdk

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Context.NOTIFICATION_SERVICE
import android.os.Build
import androidx.core.app.NotificationCompat
import io.radar.sdk.model.RadarEvent

class RadarNotificationHelper {

    internal companion object {
        private const val CHANNEL_NAME = "Location"
        private const val CHANNEL_ID = "RadarNotificationHelper"
        private const val NOTIFICATION_ID = 20160525 // Radar's birthday!

        internal fun showNotifications(context: Context, events: Array<RadarEvent>) {
            if (Build.VERSION.SDK_INT < 26) {
                return
            }

            for (event in events) {
                var notificationText: String? = null

                if (event.type == RadarEvent.RadarEventType.USER_ENTERED_GEOFENCE) {
                    notificationText = event.geofence?.metadata?.optString("radar:entryNotificationText")
                } else if (event.type == RadarEvent.RadarEventType.USER_EXITED_GEOFENCE) {
                    notificationText = event.geofence?.metadata?.optString("radar:exitNotificationText")
                } else if (event.type == RadarEvent.RadarEventType.USER_ENTERED_BEACON) {
                    notificationText = event.beacon?.metadata?.optString("radar:entryNotificationText")
                } else if (event.type == RadarEvent.RadarEventType.USER_EXITED_BEACON) {
                    notificationText = event.beacon?.metadata?.optString("radar:exitNotificationText")
                } else if (event.type == RadarEvent.RadarEventType.USER_APPROACHING_TRIP_DESTINATION) {
                    notificationText = event.trip?.metadata?.optString("radar:approachingNotificationText")
                } else if (event.type == RadarEvent.RadarEventType.USER_ARRIVED_AT_TRIP_DESTINATION) {
                    notificationText = event.trip?.metadata?.optString("radar:arrivalNotificationText")
                }

                if (notificationText != null) {
                    val id = event._id

                    val notificationManager =
                        context.getSystemService(NOTIFICATION_SERVICE) as? NotificationManager

                    val importance = NotificationManager.IMPORTANCE_HIGH
                    val channel = NotificationChannel(CHANNEL_NAME, CHANNEL_NAME, importance)
                    notificationManager?.createNotificationChannel(channel)

                    val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                        .setSmallIcon(context.applicationContext.applicationInfo.icon)
                        .setContentText(notificationText)
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                        .build()
                    notificationManager?.notify(id, NOTIFICATION_ID, notification)
                }
            }
        }
    }

}