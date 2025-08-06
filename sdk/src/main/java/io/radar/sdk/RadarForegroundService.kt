package io.radar.sdk

import android.annotation.SuppressLint
import android.app.*
import android.content.Intent
import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import androidx.annotation.RequiresApi
import androidx.core.net.toUri
import io.radar.sdk.Radar.RadarLogType
import io.radar.sdk.RadarTrackingOptions.RadarTrackingOptionsForegroundService.Companion.KEY_FOREGROUND_SERVICE_CHANNEL_NAME


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
        
        // Check if a custom notification is set
        val customNotification = RadarNotificationHelper.getCustomForegroundNotification()

        if (customNotification != null) {
            // Use the custom notification
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
        Radar.logger.i("title: $title")
        val text = extras?.getString("text") ?: "Location tracking started"
        Radar.logger.i("text: $text")
        val icon = extras?.getInt("icon") ?: 0
        val iconString = extras?.getString("iconString") ?: this.applicationInfo.icon.toString()
        val iconColor = extras?.getString("iconColor") ?: ""
        var smallIcon = resources.getIdentifier(iconString, "drawable", applicationContext.packageName) 
        if (icon != 0){
           smallIcon = resources.getIdentifier(icon.toString(), "drawable", applicationContext.packageName)  
        }
        val channelName = extras?.getString(KEY_FOREGROUND_SERVICE_CHANNEL_NAME) ?: "Location Services"
        val channel = NotificationChannel("RadarSDK", channelName, importance)
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
        
        // Create notification builder
        var builder = Notification.Builder(applicationContext, "RadarSDK")
            .setContentText(text as CharSequence?)
            .setOngoing(true)
            .setSmallIcon(smallIcon)
        
        if (!title.isNullOrEmpty()) {
            builder = builder.setContentTitle(title as CharSequence?)
        }
        
        if (iconColor.isNotEmpty()) {
            builder.setColor(Color.parseColor(iconColor))
        }

        // Add big picture style if image is provided
        val imageResourceName = extras?.getString("imageResourceName")
        val imageUrl = extras?.getString("imageUrl")
        if (!imageResourceName.isNullOrEmpty() || !imageUrl.isNullOrEmpty()) {
            try {
                val bigPicture: Bitmap? = when {
                    !imageResourceName.isNullOrEmpty() -> {
                        val imageResId = resources.getIdentifier(imageResourceName, "drawable", applicationContext.packageName)
                        if (imageResId != 0) {
                            BitmapFactory.decodeResource(resources, imageResId)
                        } else null
                    }
                    !imageUrl.isNullOrEmpty() -> {
                        // For simplicity, we'll use a default image if URL is provided
                        // In a real implementation, you'd want to download the image asynchronously
                        val defaultImageResId = resources.getIdentifier("ic_launcher_foreground", "drawable", applicationContext.packageName)
                        if (defaultImageResId != 0) {
                            BitmapFactory.decodeResource(resources, defaultImageResId)
                        } else null
                    }
                    else -> null
                }
                
                if (bigPicture != null) {
                    val bigPictureStyle = Notification.BigPictureStyle()
                        .bigPicture(bigPicture)
                        .setBigContentTitle(title ?: "Location Tracking")
                        .setSummaryText(text)
                    
                    builder = builder.setStyle(bigPictureStyle)
                }
            } catch (e: Exception) {
                logger.e("Error setting notification big picture", RadarLogType.SDK_EXCEPTION, e)
            }
        }

        // Add default Google action button
        addDefaultGoogleAction(builder)

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
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_IMMUTABLE
            } else {
                0
            }
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

    private fun addDefaultGoogleAction(builder: Notification.Builder) {
        try {
            val googleIntent = Intent(Intent.ACTION_VIEW, "https://www.google.com".toUri())
            googleIntent.addCategory(Intent.CATEGORY_BROWSABLE)
            googleIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            
            val googleFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_IMMUTABLE
            } else {
                0
            }
            val googlePendingIntent = PendingIntent.getActivity(this, 1, googleIntent, googleFlags)
            
            builder.addAction(
                android.R.drawable.ic_menu_view, // Default Android view icon
                "Open Google",
                googlePendingIntent
            )
        } catch (e: Exception) {
            logger.e("Error adding default Google action", RadarLogType.SDK_EXCEPTION, e)
        }
    }

    override fun onBind(intent: Intent): IBinder? {
        throw UnsupportedOperationException()
    }

}