package io.radar.sdk

import android.app.*
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import androidx.annotation.RequiresApi


@RequiresApi(Build.VERSION_CODES.O)
class RadarForegroundService : Service() {

    internal companion object {
        internal var started: Boolean = false

        private const val NOTIFICATION_ID = 20160525 // random notification ID (Radar's birthday!)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null) {
            if (intent.action == "start") {
                try {
                    startForegroundService(intent.extras)
                } catch (e: Exception) {
                    Radar.logger.e("Error starting foreground service", e)
                }
            } else if (intent.action == "stop") {
                try {
                    stopForeground(true)
                    stopSelf()
                } catch (e: Exception) {
                    Radar.logger.e("Error stopping foreground service", e)
                }
            }
        }

        return START_STICKY
    }

    private fun startForegroundService(extras: Bundle?) {
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.deleteNotificationChannel("RadarSDK")
        var id = extras?.getInt("id") ?: 0
        id = if (id == 0) NOTIFICATION_ID else id
        var importance = extras?.getInt("importance") ?: 0
        importance = if (importance == 0) NotificationManager.IMPORTANCE_DEFAULT else importance
        val title = extras?.getString("title")
        val text = extras?.getString("text") ?: "Location tracking started"
        var icon = extras?.getInt("icon") ?: 0
        icon = if (icon == 0) this.applicationInfo.icon else icon
        val smallIcon = resources.getIdentifier(icon.toString(), "drawable", applicationContext.packageName)
        val channel = NotificationChannel("RadarSDK", "RadarSDK", importance)
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
        var builder = Notification.Builder(applicationContext, "RadarSDK")
            .setContentText(text as CharSequence?)
            .setOngoing(true)
            .setSmallIcon(smallIcon)
        if (title != null && title.isNotEmpty()) {
            builder = builder.setContentTitle(title as CharSequence?)
        }
        try {
            extras?.getString("activity")?.let {
                val activityClass = Class.forName(it)
                val intent = Intent(this, activityClass)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    PendingIntent.FLAG_IMMUTABLE
                } else {
                    0
                }
                val pendingIntent = PendingIntent.getActivity(this, 0, intent, flags)
                builder = builder.setContentIntent(pendingIntent)
            }
        } catch (e: ClassNotFoundException) {
            Radar.logger.e("Error setting foreground service content intent", e)
        }
        val notification = builder.build()
        startForeground(id, notification)
    }

    override fun onBind(intent: Intent): IBinder? {
        throw UnsupportedOperationException()
    }

}