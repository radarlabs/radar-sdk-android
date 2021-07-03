package io.radar.sdk

import android.app.*
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import androidx.annotation.RequiresApi


@RequiresApi(Build.VERSION_CODES.O)
class RadarForegroundService : Service() {

    private lateinit var logger: RadarLogger

    internal companion object {
        internal var started: Boolean = false
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!this::logger.isInitialized) {
            logger = RadarLogger(applicationContext)
        }

        if (intent != null) {
            if (intent.action == "start" && !started) {
                try {
                    startForegroundService(intent.extras)
                    started = true
                } catch (e: Exception) {
                    logger.e("Error starting foreground service", e)
                }
            } else if (intent.action == "stop") {
                try {
                    stopForeground(true)
                    stopSelf()
                    started = false
                } catch (e: Exception) {
                    logger.e("Error stopping foreground service", e)
                }
            }
        }
        return START_STICKY
    }

    private fun startForegroundService(extras: Bundle?) {
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.deleteNotificationChannel("RadarSDK")
        val id = extras?.getInt("id") ?: 20160525
        val importance = extras?.getInt("importance") ?: NotificationManager.IMPORTANCE_DEFAULT
        val title = extras?.getString("title") ?: "Title"
        val text = extras?.getString("text") ?: "Text"
        val icon = extras?.getString("icon") ?: "17301546" // r_drawable_ic_dialog_map
        val smallIcon = resources.getIdentifier(icon, "drawable", applicationContext.packageName)
        var pendingIntent = PendingIntent.getActivity(this, 0, null, 0)
        try {
            extras?.getString("activity")?.let {
                val activityClass = Class.forName(it)
                val intent = Intent(this, activityClass)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                pendingIntent = PendingIntent.getActivity(this, 0, intent, 0)
            }
        } catch (e: ClassNotFoundException) {
            logger.e("Error setting foreground service PendingIntent", e)
        }
        val channel = NotificationChannel("RadarSDK", "RadarSDK", importance)
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
        val notification = Notification.Builder(applicationContext, "RadarSDK")
            .setContentTitle(title as CharSequence?)
            .setContentText(text as CharSequence?)
            .setOngoing(true)
            .setSmallIcon(smallIcon)
            .setContentIntent(pendingIntent)
            .build()
        startForeground(id, notification)
    }

    override fun onBind(intent: Intent): IBinder? {
        throw UnsupportedOperationException()
    }

}