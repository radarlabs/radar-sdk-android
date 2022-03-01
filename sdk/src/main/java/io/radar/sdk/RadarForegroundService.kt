package io.radar.sdk

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import androidx.annotation.RequiresApi
import io.radar.sdk.RadarTrackingOptions.RadarTrackingOptionsForegroundService.Companion.KEY_FOREGROUND_SERVICE_ACTIVITY
import io.radar.sdk.RadarTrackingOptions.RadarTrackingOptionsForegroundService.Companion.KEY_FOREGROUND_SERVICE_CHANNEL_NAME
import io.radar.sdk.RadarTrackingOptions.RadarTrackingOptionsForegroundService.Companion.KEY_FOREGROUND_SERVICE_ICON
import io.radar.sdk.RadarTrackingOptions.RadarTrackingOptionsForegroundService.Companion.KEY_FOREGROUND_SERVICE_ID
import io.radar.sdk.RadarTrackingOptions.RadarTrackingOptionsForegroundService.Companion.KEY_FOREGROUND_SERVICE_IMPORTANCE
import io.radar.sdk.RadarTrackingOptions.RadarTrackingOptionsForegroundService.Companion.KEY_FOREGROUND_SERVICE_TEXT
import io.radar.sdk.RadarTrackingOptions.RadarTrackingOptionsForegroundService.Companion.KEY_FOREGROUND_SERVICE_TITLE


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
            intent.putExtra(KEY_FOREGROUND_SERVICE_ID, foregroundService.id)
                .putExtra(KEY_FOREGROUND_SERVICE_IMPORTANCE, foregroundService.importance ?: NotificationManager.IMPORTANCE_DEFAULT)
                .putExtra(KEY_FOREGROUND_SERVICE_TITLE, foregroundService.title)
                .putExtra(KEY_FOREGROUND_SERVICE_TEXT, foregroundService.text)
                .putExtra(KEY_FOREGROUND_SERVICE_ICON, foregroundService.icon)
                .putExtra(KEY_FOREGROUND_SERVICE_ACTIVITY, foregroundService.activity)
                .putExtra(KEY_FOREGROUND_SERVICE_CHANNEL_NAME, foregroundService.channelName)
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
        val id = extras?.getInt(KEY_FOREGROUND_SERVICE_ID) ?: NOTIFICATION_ID
        val importance = extras?.getInt(KEY_FOREGROUND_SERVICE_IMPORTANCE) ?: NotificationManager.IMPORTANCE_DEFAULT
        val title = extras?.getString(KEY_FOREGROUND_SERVICE_TITLE)
        val text = extras?.getString(KEY_FOREGROUND_SERVICE_TEXT) ?: "Location tracking started"
        var icon = extras?.getInt(KEY_FOREGROUND_SERVICE_ICON) ?: 0
        icon = if (icon == 0) this.applicationInfo.icon else icon
        val smallIcon = resources.getIdentifier(icon.toString(), "drawable", applicationContext.packageName)
        val channelName = extras?.getString(KEY_FOREGROUND_SERVICE_CHANNEL_NAME) ?: "Location Services"
        val channel = NotificationChannel(RADAR_CHANNEL, channelName, importance)
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
            extras?.getString(KEY_FOREGROUND_SERVICE_ACTIVITY)?.let {
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