package io.radar.example

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
import androidx.core.net.toUri
import com.google.firebase.FirebaseApp
import io.radar.example.map.overlays.MapOverlayRegistry
import io.radar.example.map.overlays.NearbyGeofencesSource
import io.radar.example.map.overlays.NearbyPlacesSource
import io.radar.example.map.overlays.SyncedGeofencesSource
import io.radar.example.map.overlays.TripBreadcrumbsSource
import io.radar.example.map.overlays.TripDestinationSource
import io.radar.example.map.overlays.TripEventsSource
import io.radar.example.map.overlays.TripGeofencesSource
import io.radar.example.store.AppStores
import io.radar.example.store.ConsoleKind
import io.radar.example.store.LogStore
import io.radar.example.store.PermissionsStore
import io.radar.example.store.ProvideStores
import io.radar.example.store.SettingsStore
import io.radar.example.store.TripBuilderStore
import io.radar.example.theme.RadarExampleTheme
import io.radar.sdk.Radar
import io.radar.sdk.RadarInitializeOptions
import io.radar.sdk.RadarVerifiedReceiver
import io.radar.sdk.model.RadarVerifiedLocationToken

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)

        // Stores — instantiated once and injected app-wide via CompositionLocals.
        val logStore = LogStore()
        val settingsStore = SettingsStore(this)
        val permissionsStore = PermissionsStore()
        val mapOverlayRegistry = MapOverlayRegistry(this)
        val tripBuilderStore = TripBuilderStore()

        // Register map overlay sources (order = render order).
        mapOverlayRegistry.register(SyncedGeofencesSource())
        mapOverlayRegistry.register(NearbyGeofencesSource())
        mapOverlayRegistry.register(NearbyPlacesSource())
        mapOverlayRegistry.register(TripGeofencesSource(tripBuilderStore))
        mapOverlayRegistry.register(TripDestinationSource(tripBuilderStore))
        mapOverlayRegistry.register(TripBreadcrumbsSource(tripBuilderStore))
        mapOverlayRegistry.register(TripEventsSource(tripBuilderStore))

        tripBuilderStore.bind(logStore, mapOverlayRegistry)

        val customNotification = if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O) {
            createCustomNotification()
        } else {
            null
        }

        // Single RadarReceiver funnel — LogStore feeds the unified console (fixes the old
        // duplicate-receiver setup).
        val options = RadarInitializeOptions(
            radarReceiver = logStore,
            fraud = true,
            customForegroundNotification = customNotification,
            silentPush = true,
            trackVerifiedAutoFailover = true,
            activity = this,
        )
        Radar.initialize(this, settingsStore.resolvedPublishableKey, options)

        Radar.setUserId("android-test-user")
        Log.i("version", Radar.sdkVersion())
        settingsStore.refresh()

        val verifiedReceiver = object : RadarVerifiedReceiver() {
            override fun onTokenUpdated(context: Context, token: RadarVerifiedLocationToken) {
                logStore.write(ConsoleKind.RESULT, "verified token updated", token.toJson().toString(2))
            }

            override fun onIpChanged(context: Context) {
                logStore.write(ConsoleKind.LOG, "verified onIpChanged")
            }

            override fun onSharingChanged(context: Context, sharing: Boolean) {
                logStore.write(ConsoleKind.LOG, "verified onSharingChanged: sharing = $sharing")
            }
        }
        Radar.setVerifiedReceiver(verifiedReceiver)
        Radar.setInAppMessageReceiver(MyInAppMessageReceiver(logStore))

        val stores = AppStores(
            logStore = logStore,
            settingsStore = settingsStore,
            permissionsStore = permissionsStore,
            tripBuilderStore = tripBuilderStore,
            mapOverlayRegistry = mapOverlayRegistry,
        )

        setContent {
            RadarExampleTheme {
                ProvideStores(stores) {
                    MainView()
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createCustomNotification(): Notification {
        val channelId = "radar_custom_channel"
        val channel = NotificationChannel(
            channelId,
            "Radar Custom Notifications",
            NotificationManager.IMPORTANCE_DEFAULT,
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)

        val mainIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val mainPendingIntent = PendingIntent.getActivity(this, 0, mainIntent, PendingIntent.FLAG_IMMUTABLE)

        val googleIntent = Intent(Intent.ACTION_VIEW, "https://www.google.com".toUri()).apply {
            addCategory(Intent.CATEGORY_BROWSABLE)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val googlePendingIntent = PendingIntent.getActivity(this, 1, googleIntent, PendingIntent.FLAG_IMMUTABLE)

        val stopIntent = Intent(this, io.radar.sdk.RadarForegroundService::class.java).apply { action = "stop" }
        val stopPendingIntent = PendingIntent.getService(this, 2, stopIntent, PendingIntent.FLAG_IMMUTABLE)

        val icon = BitmapFactory.decodeResource(resources, android.R.drawable.ic_menu_mylocation)
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("🚗 Location Tracking Active")
            .setContentText("Your location is being tracked in the background")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setLargeIcon(icon)
            .setOngoing(true)
            .setContentIntent(mainPendingIntent)
            .addAction(android.R.drawable.ic_menu_view, "Open Google", googlePendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop Tracking", stopPendingIntent)
            .setStyle(
                NotificationCompat.BigPictureStyle()
                    .bigPicture(icon)
                    .setBigContentTitle("🚗 Location Tracking Active")
                    .setSummaryText("Background location tracking is enabled"),
            )
            .build()
    }
}
