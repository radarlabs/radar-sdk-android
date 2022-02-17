package io.radar.example

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.location.Location
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.MutableLiveData
import io.radar.sdk.livedata.RadarModel

class MyRadarLiveDataReceiver(context: Context, model: RadarModel) {
    private companion object {
        const val CHANNEL_ID = "example"
    }
    private var identifier = 0

    init {
        val text: MutableLiveData<String> = MutableLiveData()
        model.getCurrentLocation().observeForever { location ->
            text.value = "${if (model.getUser().value!!.stopped) "Stopped at" else "Moved to"} ${location.toRadarString()}"
        }
        model.getDeviceLocation().observeForever { (location, stopped, source) ->
            text.value = "${if (stopped) "Client stopped at" else "Client moved to"} ${location.toRadarString()} and source $source"
        }
        model.getErrorStatus().observeForever { status ->
            text.value = Utils.stringForRadarStatus(status)
        }
        model.getEvents().observeForever { events ->
            events.forEach { event -> text.value = Utils.stringForRadarEvent(event) }
        }
        model.getLogMessage().observeForever { message ->
            text.value = message
        }
        text.observeForever { body ->
            identifier++
            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                .setContentText(body)
                .setPriority(NotificationCompat.PRIORITY_MAX)

            with(NotificationManagerCompat.from(context)) {
                notify(identifier % 20, builder.build())
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = CHANNEL_ID
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = CHANNEL_ID
            }
            val notificationManager: NotificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun Location.toRadarString(): String {
        return "location ($latitude, $longitude) with accuracy $accuracy"
    }

}