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
            logger = RadarLogger()
        }

        if (intent != null) {
            try {
                if (intent.action == "start" && !started) {
                    startForegroundService(intent.extras)
                    started = true
                } else if (intent.action == "stop") {
                    stopForeground(true)
                    stopSelf()
                    started = false
                }
            } catch (e: Exception) {
                logger.e(applicationContext, "Error starting or stopping foreground service", e)
            }
        }
        return START_STICKY
    }

    private fun startForegroundService(extras: Bundle?) {
        // TODO make notification customizable
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.deleteNotificationChannel("RadarSDK")
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel("RadarSDK", "RadarSDK", importance)
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(
            channel
        )
        val icon =
            resources.getIdentifier("17301546", "drawable", applicationContext.packageName) // r_drawable_ic_dialog_map
        var pendingIntent: PendingIntent?
        try {
            val activityClass = Class.forName("MainActivity")
            val intent = Intent(this, activityClass)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            pendingIntent = PendingIntent.getActivity(this, 0, intent, 0)
        } catch (e: ClassNotFoundException) {
            pendingIntent = null
        }
        val title = "Title"
        val text = "Text"
        val notification = Notification.Builder(applicationContext, "RadarSDK")
            .setContentTitle(title as CharSequence?)
            .setContentText(text as CharSequence?)
            .setOngoing(true)
            .setSmallIcon(icon)
            .setContentIntent(pendingIntent)
            .build()
        startForeground(20160525, notification)
    }

    override fun onBind(intent: Intent): IBinder? {
        throw UnsupportedOperationException()
    }

}