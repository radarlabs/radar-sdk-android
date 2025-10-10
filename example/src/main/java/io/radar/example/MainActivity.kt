package io.radar.example

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.LinearLayout
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.net.toUri
import io.radar.sdk.Radar
import io.radar.sdk.RadarVerifiedReceiver
import io.radar.sdk.model.RadarVerifiedLocationToken

const val HOST = "https://shicheng.ngrok.dev" // "https://api.radar.io"
const val PUBLISHABLE_KEY = "prj_test_pk_3508428416f485c5f54d8e8bb1f616ee405b1995"

fun requestForegroundPermission(activity: Activity) {
    if (ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
        ActivityCompat.requestPermissions(activity, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
    } else {
        Log.v("example", "Foreground location permission already granted")
    }
}

@RequiresApi(Build.VERSION_CODES.Q)
fun requestBackgroundPermission(activity: Activity) {
    if (ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
        ActivityCompat.requestPermissions(activity, arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION), 2)
    } else {
        Log.v("example", "Background location permission already granted")
    }
}

@RequiresApi(Build.VERSION_CODES.Q)
fun requestActivityRecognitionPermission(activity: Activity) {
    if (ContextCompat.checkSelfPermission(activity, Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED) {
        ActivityCompat.requestPermissions(activity, arrayOf(Manifest.permission.ACTIVITY_RECOGNITION), 3)
    } else {
        Log.v("example", "Activity recognition permission already granted")
    }
}

class MainActivity : AppCompatActivity() {

    val demoFunctions: ArrayList<(button: Button) -> Unit> = ArrayList()
    private lateinit var listView: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        getSharedPreferences("RadarSDK", Context.MODE_PRIVATE).edit {
            putString("host", HOST)
        }

        Radar.initialize(this, PUBLISHABLE_KEY, MyRadarReceiver(), Radar.RadarLocationServicesProvider.GOOGLE, true, createCustomNotification())
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

    @SuppressLint("NewApi")
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
