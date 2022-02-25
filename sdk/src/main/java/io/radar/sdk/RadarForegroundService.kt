package io.radar.sdk

import android.app.*
import android.content.Context
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
            private set

        private const val NOTIFICATION_ID = 20160525 // random notification ID (Radar's birthday!)
        private const val RADAR_CHANNEL = "RadarSDK"
        private const val STOP = "stop"
        private const val START = "start"
        private const val ID = "id"
        private const val IMPORTANCE = "importance"
        private const val TITLE = "title"
        private const val TEXT = "text"
        private const val ICON = "icon"
        private const val ACTIVITY = "activity"

        fun stopService(context: Context): Boolean {
            return if (started) {
                val intent = Intent(context, RadarForegroundService::class.java)
                intent.action = STOP
                context.applicationContext.startService(intent)
                true
            } else {
                false
            }
        }

        fun startService(
            context: Context,
            foregroundService: RadarTrackingOptions.RadarTrackingOptionsForegroundService
        ): Intent {
            val intent = Intent(context, RadarForegroundService::class.java)
            intent.action = START
            intent.putExtra(ID, foregroundService.id)
                .putExtra(IMPORTANCE, foregroundService.importance ?: NotificationManager.IMPORTANCE_DEFAULT)
                .putExtra(TITLE, foregroundService.title)
                .putExtra(TEXT, foregroundService.text)
                .putExtra(ICON, foregroundService.icon )
                .putExtra(ACTIVITY, foregroundService.activity)
            context.applicationContext.startForegroundService(intent)
            return intent
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!this::logger.isInitialized) {
            logger = RadarLogger(applicationContext)
        }

        if (intent != null) {
            if (intent.action == START) {
                try {
                    startForegroundService(intent.extras)
                } catch (e: Exception) {
                    logger.e("Error starting foreground service", e)
                }
            } else if (intent.action == STOP) {
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
        started = true
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.deleteNotificationChannel(RADAR_CHANNEL)
        var id = extras?.getInt(ID) ?: 0
        id = if (id == 0) NOTIFICATION_ID else id
        var importance = extras?.getInt(IMPORTANCE) ?: 0
        importance = if (importance == 0) NotificationManager.IMPORTANCE_DEFAULT else importance
        val title = extras?.getString(TITLE)
        val text = extras?.getString(TEXT) ?: "Location tracking started"
        var icon = extras?.getInt(ICON) ?: 0
        icon = if (icon == 0) this.applicationInfo.icon else icon
        val smallIcon = resources.getIdentifier(icon.toString(), "drawable", applicationContext.packageName)
        val channel = NotificationChannel(RADAR_CHANNEL, RADAR_CHANNEL, importance)
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
        var builder = Notification.Builder(applicationContext, RADAR_CHANNEL)
            .setContentText(text as CharSequence?)
            .setOngoing(true)
            .setSmallIcon(smallIcon)
        if (title != null && title.isNotEmpty()) {
            builder = builder.setContentTitle(title as CharSequence?)
        }
        try {
            extras?.getString(ACTIVITY)?.let {
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
            logger.e("Error setting foreground service content intent", e)
        }
        val notification = builder.build()
        startForeground(id, notification)
    }

    override fun onBind(intent: Intent): IBinder? {
        throw UnsupportedOperationException()
    }

    override fun onDestroy() {
        super.onDestroy()
        started = false
    }

}