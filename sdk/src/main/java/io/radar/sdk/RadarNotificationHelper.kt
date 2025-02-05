package io.radar.sdk

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Context.NOTIFICATION_SERVICE
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import io.radar.sdk.model.RadarEvent

class RadarNotificationHelper {

    internal companion object {
        private const val CHANNEL_NAME = "Location"
        private const val NOTIFICATION_ID = 20160525 // Radar's birthday!

        @SuppressLint("DiscouragedApi")
        internal fun showNotifications(context: Context, events: Array<RadarEvent>) {
            if (Build.VERSION.SDK_INT < 26) {
                return
            }

            for (event in events) {
                var notificationText: String? = event.metadata?.optString("radar:notificationText")
                val id = event._id
                val notificationManager =
                    context.getSystemService(NOTIFICATION_SERVICE) as? NotificationManager

                val importance = NotificationManager.IMPORTANCE_HIGH
                val channel = NotificationChannel(CHANNEL_NAME, CHANNEL_NAME, importance)
                channel.enableVibration(true)
                notificationManager?.createNotificationChannel(channel)

                val notificationOptions = RadarSettings.getNotificationOptions(context);

                val iconString = notificationOptions?.getEventIcon() ?: context.applicationContext.applicationInfo.icon.toString()
                val smallIcon = context.applicationContext.resources.getIdentifier(iconString, "drawable", context.applicationContext.packageName)

                if (notificationText != null) {
                    val notificationTitle: String? = event.metadata?.optString("radar:notificationTitle")
                    val subTitle: String? = event.metadata?.optString("radar:notificationSubTitle")
                    val deeplink: String? = event.metadata?.optString("radar:notificationDeeplink")
                    val campaignId: String? = event.metadata?.optString("radar:notificationCampaignId")

                    val deepLinkUrl = Uri.parse(deeplink)
                    val deepLinkIntent = Intent(Intent.ACTION_VIEW, deepLinkUrl).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    }

                    val builder = NotificationCompat.Builder(context, CHANNEL_NAME)
                        .setSmallIcon(smallIcon)
                        .setAutoCancel(true)
                        .setContentTitle(notificationTitle)
                        .setSubText(subTitle)
                        .setContentText(notificationText)
                        .setStyle(NotificationCompat.BigTextStyle()
                            .bigText(notificationText)
                            .setBigContentTitle(notificationTitle)
                            .setSummaryText(subTitle))
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                        .setContentIntent(PendingIntent.getActivity(
                            context,
                            0,
                            deepLinkIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        ))

                    val iconColor = notificationOptions?.getEventColor() ?: ""
                    if (iconColor.isNotEmpty()) {
                        builder.setColor(Color.parseColor(iconColor))
                    }
                    
                    notificationManager?.notify(id, NOTIFICATION_ID, builder.build())

                    continue
                }

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
                    
                    notificationManager?.notify(id, NOTIFICATION_ID, builder.build())
                }
            }
        }
    }

}