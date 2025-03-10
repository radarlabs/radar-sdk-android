package io.radar.sdk

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Context.NOTIFICATION_SERVICE
import android.graphics.Color
import android.os.Build
import androidx.core.app.NotificationCompat
import io.radar.sdk.model.RadarEvent

class RadarNotificationHelper {

    internal companion object {
        private const val CHANNEL_NAME = "Location"
        private const val NOTIFICATION_ID = 20160525 // Radar's birthday!
        val SUMMARY_NOTIFICATION_GROUP = "io.radar.sdk.event_notification"
        val SUMMARY_ID = 0

        @SuppressLint("DiscouragedApi", "LaunchActivityFromNotification")
        internal fun showNotifications(context: Context, events: Array<RadarEvent>) {
            if (Build.VERSION.SDK_INT < 26) {
                return
            }

            var notificationsCount = 0
            val notificationManager =
                context.getSystemService(NOTIFICATION_SERVICE) as? NotificationManager

            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_NAME, CHANNEL_NAME, importance)
            channel.enableVibration(true)
            notificationManager?.createNotificationChannel(channel)

            for (event in events) {
                var notificationText: String? = "dummy var"

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

                if (notificationText != null && notificationText.isNotEmpty()) {
                    val id = event._id



                    val notificationOptions = RadarSettings.getNotificationOptions(context);

                    val iconString = notificationOptions?.getEventIcon() ?: context.applicationContext.applicationInfo.icon.toString()
                    val smallIcon = context.applicationContext.resources.getIdentifier(iconString, "drawable", context.applicationContext.packageName)

                    val builder = NotificationCompat.Builder(context, CHANNEL_NAME)
                        .setSmallIcon(smallIcon)
                        .setAutoCancel(true)
                        .setContentText(notificationText)
                        .setStyle(NotificationCompat.BigTextStyle().bigText(notificationText))
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT)

                    val iconColor = notificationOptions?.getEventColor() ?: ""
                    if (iconColor.isNotEmpty()) {
                        builder.setColor(Color.parseColor(iconColor))
                    }

                    builder.setContentIntent(RadarNotificationReceiver.getNotificationConversionPendingIntent(context))
                    builder.setAutoCancel(true)
                    builder.setGroup(SUMMARY_NOTIFICATION_GROUP)
                    
                    notificationManager?.notify(id, NOTIFICATION_ID, builder.build())
                    notificationsCount++
                }
            }
            if (notificationsCount > 0) {
                val notificationOptions = RadarSettings.getNotificationOptions(context);

                    val iconString = notificationOptions?.getEventIcon()?: context.applicationContext.applicationInfo.icon.toString()
                    val smallIcon = context.applicationContext.resources.getIdentifier(iconString, "drawable", context.applicationContext.packageName)
                    val notificationText = "You have ${notificationsCount} location sensitive notifications"
                    val builder = NotificationCompat.Builder(context, CHANNEL_NAME)
                        .setSmallIcon(smallIcon)
                        .setAutoCancel(true)
                        .setContentText(notificationText)
                        .setStyle(NotificationCompat.BigTextStyle().bigText(notificationText))
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT)

                    val iconColor = notificationOptions?.getEventColor()?: ""
                    if (iconColor.isNotEmpty()) {
                        builder.setColor(Color.parseColor(iconColor))
                    }

                    builder.setContentIntent(RadarNotificationReceiver.getNotificationConversionPendingIntent(context))
                    builder.setAutoCancel(true)
                    builder.setGroup(SUMMARY_NOTIFICATION_GROUP)
                    builder.setGroupSummary(true)
                    
                    notificationManager?.notify(SUMMARY_ID, builder.build())
            }


        }

    }

}