package io.radar.example

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.content.edit
import androidx.core.net.toUri
import com.google.firebase.FirebaseApp
import io.radar.sdk.Radar
import io.radar.sdk.RadarInitializeOptions
import io.radar.sdk.RadarVerifiedReceiver
import io.radar.sdk.model.RadarVerifiedLocationToken

const val HOST = "https://api.radar.io"
const val PUBLISHABLE_KEY = "prj_test_pk_"

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)

        getSharedPreferences("RadarSDK", Context.MODE_PRIVATE).edit {
            putString("host", HOST)
        }

        val customNotification = if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O) {
            createCustomNotification()
        } else {
            null
        }

        val options = RadarInitializeOptions(
            radarReceiver = MyRadarReceiver(),
            fraud = true,
            customForegroundNotification = customNotification,
            silentPush = true,
        )
        Radar.initialize(this, PUBLISHABLE_KEY, options)

        Radar.setUserId("android-test-user")
        Radar.sdkVersion().let { Log.i("version", it) }

        // We can also set the foreground service options like this:
        // Radar.setForegroundServiceOptions(RadarTrackingOptions.RadarTrackingOptionsForegroundService(
        //     title = "Title Radar SDK",
        //     text = "Location tracking started Text",
        //     iconString = "ic_notification",
        //     iconColor = "#FF6B8D",
        // ))

        val verifiedReceiver = object : RadarVerifiedReceiver() {
            override fun onTokenUpdated(context: Context, token: RadarVerifiedLocationToken) {
            }
        }
        Radar.setVerifiedReceiver(verifiedReceiver)

        val inAppMessageReceiver = MyInAppMessageReceiver()
        Radar.setInAppMessageReceiver(inAppMessageReceiver)

        setContent {
            MainView()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createCustomNotification(): Notification {
        // Create notification channel (required for Android O+)
        val channelId = "radar_custom_channel"
        val channelName = "Radar Custom Notifications"
        val channel = NotificationChannel(
            channelId,
            channelName,
            NotificationManager.IMPORTANCE_DEFAULT
        )
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)

        // Create intents for actions
        val mainIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val mainPendingIntent = PendingIntent.getActivity(
            this, 0, mainIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        // Google action button
        val googleIntent = Intent(Intent.ACTION_VIEW, "https://www.google.com".toUri())
        googleIntent.addCategory(Intent.CATEGORY_BROWSABLE)
        googleIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        val googlePendingIntent = PendingIntent.getActivity(
            this, 1, googleIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        // Stop tracking action button
        val stopIntent = Intent(this, io.radar.sdk.RadarForegroundService::class.java).apply {
            action = "stop"
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 2, stopIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        // Build the custom notification with image and actions
        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("🚗 Location Tracking Active")
            .setContentText("Your location is being tracked in the background")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setLargeIcon(BitmapFactory.decodeResource(resources, android.R.drawable.ic_menu_mylocation))
            .setOngoing(true)
            .setContentIntent(mainPendingIntent)
            .addAction(
                android.R.drawable.ic_menu_view,
                "Open Google",
                googlePendingIntent
            )
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Stop Tracking",
                stopPendingIntent
            )
            .setStyle(
                NotificationCompat.BigPictureStyle()
                    .bigPicture(BitmapFactory.decodeResource(resources, android.R.drawable.ic_menu_mylocation))
                    .setBigContentTitle("🚗 Location Tracking Active")
                    .setSummaryText("Background location tracking is enabled")
            )
            .build()

        Log.i("MainActivity", "Custom notification created")
        return notification
    }
}
