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
            if (intent.action == "start") {
                if (started) {
                    logger.d("Foreground service already started")
                } else {
                    try {
                        startForegroundService(intent.extras)
                        started = true
                    } catch (e: Exception) {
                        logger.e("Error starting foreground service", e)
                    }
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
        var icon = extras?.getInt("icon")
        if (icon == null || icon == 0) {
            icon = 17301546 // r_drawable_ic_dialog_map
        }
        val smallIcon = resources.getIdentifier(icon.toString(), "drawable", applicationContext.packageName)
        val channel = NotificationChannel("RadarSDK", "RadarSDK", importance)
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
        var builder = Notification.Builder(applicationContext, "RadarSDK")
            .setContentTitle(title as CharSequence?)
            .setContentText(text as CharSequence?)
            .setOngoing(true)
            .setSmallIcon(smallIcon)
        try {
            extras?.getString("activity")?.let {
                val activityClass = Class.forName(it)
                val intent = Intent(this, activityClass)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                val pendingIntent = PendingIntent.getActivity(this, 0, intent, 0)
                builder = builder.setContentIntent(pendingIntent)
            }
        } catch (e: ClassNotFoundException) {
            logger.e("Error setting foreground service PendingIntent", e)
        }
        val notification = builder.build()
        startForeground(id, notification)
    }

    override fun onBind(intent: Intent): IBinder? {
        throw UnsupportedOperationException()
    }

}