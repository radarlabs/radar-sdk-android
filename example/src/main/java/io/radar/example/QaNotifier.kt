package io.radar.example

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

/**
 * Lightweight local-notification helper used by the offline-events QA demo.
 * Mirrors the iOS AppDelegate's `scheduleOfflineEventNotification(...)` flow so QA
 * can correlate device behavior with offline event generation and offline RTO ramps.
 *
 * No Firebase / FCM — these are purely local notifications scheduled on-device.
 */
object QaNotifier {
    private const val CHANNEL_ID = "radar_qa_channel"
    private const val CHANNEL_NAME = "Radar QA"
    private var nextId = 1000

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(NotificationManager::class.java) ?: return
            if (manager.getNotificationChannel(CHANNEL_ID) == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Radar offline-events QA notifications"
                }
                manager.createNotificationChannel(channel)
            }
        }
    }

    private fun hasPostPermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun post(context: Context, title: String, body: String) {
        ensureChannel(context)
        if (!hasPostPermission(context)) return

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(nextId++, notification)
    }
}
