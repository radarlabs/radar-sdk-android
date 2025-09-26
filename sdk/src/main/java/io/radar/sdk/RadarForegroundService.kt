package io.radar.sdk

import android.annotation.SuppressLint
import android.app.*
import android.content.Intent
import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import androidx.annotation.RequiresApi
import androidx.core.net.toUri
import io.radar.sdk.Radar.RadarLogType
import io.radar.sdk.RadarTrackingOptions.RadarTrackingOptionsForegroundService.Companion.KEY_FOREGROUND_SERVICE_CHANNEL_NAME
import androidx.core.graphics.toColorInt


@RequiresApi(Build.VERSION_CODES.O)
class RadarForegroundService : Service() {

    private lateinit var logger: RadarLogger

    internal companion object {
        internal var started: Boolean = false

        private const val NOTIFICATION_ID = 20160525 // random notification ID (Radar's birthday!)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!this::logger.isInitialized) {
            logger = RadarLogger(applicationContext)
        }

        if (intent != null) {
            if (intent.action == "start") {
                try {
                    startForegroundService(intent.extras)
                } catch (e: Exception) {
                    logger.e("Error starting foreground service", RadarLogType.SDK_EXCEPTION, e)
                }
            } else if (intent.action == "stop") {
                try {
                    stopForeground(true)
                    stopSelf()
                } catch (e: Exception) {
                    logger.e("Error stopping foreground service", RadarLogType.SDK_EXCEPTION, e)
                }
            }
        }

        return START_STICKY
    }

    @SuppressLint("DiscouragedApi")
    private fun startForegroundService(extras: Bundle?) {
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.deleteNotificationChannel("RadarSDK")
        var id = extras?.getInt("id") ?: 0
        id = if (id == 0) NOTIFICATION_ID else id
        
        val customNotification = RadarNotificationHelper.getCustomForegroundNotification()

        // Use the custom notification
        if (customNotification != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(id, customNotification, FOREGROUND_SERVICE_TYPE_LOCATION)
            } else {
                startForeground(id, customNotification)
            }
            return
        }

        // Fall back to default notification building
        buildDefaultNotification(extras, id)
    }

    private fun buildDefaultNotification(extras: Bundle?, id: Int) {
        val importance = extras?.getInt("importance", NotificationManager.IMPORTANCE_DEFAULT) ?: NotificationManager.IMPORTANCE_DEFAULT
        val title = extras?.getString("title")
        val text = extras?.getString("text") ?: "Location tracking started"
        val icon = extras?.getInt("icon") ?: 0
        val iconString = extras?.getString("iconString") ?: this.applicationInfo.icon.toString()
        val iconColor = extras?.getString("iconColor") ?: ""
        var smallIcon = if (icon != 0) {
            icon
        } else {
            try {
                // Try to get the resource ID directly if it's a valid resource name
                @Suppress("DiscouragedApi")
                val resourceId = resources.getIdentifier(iconString, "drawable", applicationContext.packageName)
                if (resourceId != 0) resourceId else applicationInfo.icon
            } catch (e: Exception) {
                applicationInfo.icon
            }
        }
        val channelName = extras?.getString(KEY_FOREGROUND_SERVICE_CHANNEL_NAME) ?: "Location Services"
        val channel = NotificationChannel("RadarSDK", channelName, importance)
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
        var builder = Notification.Builder(applicationContext, "RadarSDK")
            .setContentText(text as CharSequence?)
            .setOngoing(true)
            .setSmallIcon(smallIcon)
        if (!title.isNullOrEmpty()) {
            builder = builder.setContentTitle(title as CharSequence?)
        }
        if (iconColor.isNotEmpty()) {
            builder.setColor(iconColor.toColorInt())
        }
        try {
            val intent: Intent
            val deepLinkString = extras?.getString("deepLink")

            if (deepLinkString != null) {
                // If deep link is provided, use it
                intent = Intent(Intent.ACTION_VIEW, deepLinkString.toUri())
                intent.addCategory(Intent.CATEGORY_BROWSABLE)
            } else {
                // If no deep link, open the main app
                val packageManager = applicationContext.packageManager
                intent = packageManager.getLaunchIntentForPackage(applicationContext.packageName) ?: 
                    Intent(applicationContext, Class.forName(extras?.getString("activity")))
            }

            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            val flags = PendingIntent.FLAG_IMMUTABLE
            val pendingIntent = PendingIntent.getActivity(this, 0, intent, flags)
            builder = builder.setContentIntent(pendingIntent)
        } catch (e: Exception) {
            logger.e("Error setting foreground service content intent", RadarLogType.SDK_EXCEPTION, e)
        }
        val notification = builder.build()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(id, notification, FOREGROUND_SERVICE_TYPE_LOCATION);
        } else {
            startForeground(id, notification)
        }
    }

    override fun onBind(intent: Intent): IBinder? {
        throw UnsupportedOperationException()
    }
}
