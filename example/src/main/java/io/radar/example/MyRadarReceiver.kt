package io.radar.example

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.location.Location
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import io.radar.sdk.Radar
import io.radar.sdk.RadarReceiver
import io.radar.sdk.model.RadarEvent
import io.radar.sdk.model.RadarLocationPermissionStatus
import io.radar.sdk.model.RadarUser

class MyRadarReceiver(private val onPermissionsUpdated: (Context, RadarLocationPermissionStatus) -> Unit)  : RadarReceiver() {

    companion object {

        var identifier = 0

        internal fun notify(context: Context, body: String) {
            identifier++

            val channelId = "example"

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val name = channelId
                val importance = NotificationManager.IMPORTANCE_HIGH
                val channel = NotificationChannel("example", name, importance).apply {
                    description = channelId
                }
                val notificationManager: NotificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.createNotificationChannel(channel)
            }

            val builder = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_notification)
                .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                .setContentText(body)
                .setPriority(NotificationCompat.PRIORITY_MAX)

            with(NotificationManagerCompat.from(context)) {
                notify(identifier % 20, builder.build())
            }
        }

    }

    override fun onEventsReceived(context: Context, events: Array<RadarEvent>, user: RadarUser?) {
        events.forEach { event -> notify(context, Utils.stringForRadarEvent(event)) }
    }

    override fun onLocationUpdated(context: Context, location: Location, user: RadarUser) {
        val body = "${if (user.stopped) "Stopped at" else "Moved to"} location (${location.latitude}, ${location.longitude}) with accuracy ${location.accuracy}"
        // notify(context, body)
    }

    override fun onClientLocationUpdated(context: Context, location: Location, stopped: Boolean, source: Radar.RadarLocationSource) {
        val body = "${if (stopped) "Client stopped at" else "Client moved to"} location (${location.latitude}, ${location.longitude}) with accuracy ${location.accuracy} and source ${source}"
        // notify(context, body)
    }
    override fun onError(context: Context, status: Radar.RadarStatus) {
        // notify(context, Utils.stringForRadarStatus(status))
    }

    override fun onLog(context: Context, message: String) {
        // notify(context, message)
    }

    override fun onLocationPermissionsStatusUpdated(
        context: Context,
        status: RadarLocationPermissionStatus
    ) {
        onPermissionsUpdated(context, status)
    }

}
