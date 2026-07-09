package io.radar.example.tests.panels

import android.Manifest
import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import io.radar.example.components.ActionButton
import io.radar.example.components.ActionButtonStyle
import io.radar.example.components.TogglePanel
import io.radar.example.store.LocalLogStore

private const val CHANNEL_ID = "radar_example_local"

@Composable
fun NotificationsPanel() {
    val log = LocalLogStore.current
    val context = LocalContext.current

    TogglePanel("Notifications") {
        ActionButton("requestNotificationPermission", style = ActionButtonStyle.PRIMARY) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                (context as? Activity)?.let {
                    ActivityCompat.requestPermissions(it, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 10)
                }
                log.writeResult("requestNotificationPermission")
            } else {
                log.writeResult("Notifications permission not required below API 33")
            }
        }
        ActionButton("post local test notification") {
            val manager = context.getSystemService(NotificationManager::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                manager.createNotificationChannel(
                    NotificationChannel(CHANNEL_ID, "Radar Example", NotificationManager.IMPORTANCE_DEFAULT),
                )
            }
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setContentTitle("Radar example")
                .setContentText("This is a local test notification.")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setAutoCancel(true)
                .build()
            try {
                NotificationManagerCompat.from(context).notify(1001, notification)
                log.writeResult("post local test notification")
            } catch (e: SecurityException) {
                log.writeError("post local test notification failed", e.message)
            }
        }
        ActionButton("open notification settings") {
            val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            log.writeResult("open notification settings")
        }
    }
}
