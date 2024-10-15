package io.radar.sdk

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class RadarNotificationReceiver : BroadcastReceiver() {

    internal companion object{
        internal const val ACTION_NOTIFICATION_CONVERSION = "io.radar.sdk.NotificationReceiver.CONVERSION"
        private const val REQUEST_CODE_NOTIFICATION_CONVERSION = 201605255
        internal fun getNotificationConversionPendingIntent(context: Context): PendingIntent? {
            val activityIntent = Intent(context, Radar.activity!!::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            activityIntent.putExtra("radarNotification", true)
            val pendingIntent: PendingIntent = PendingIntent.getActivity(context, 0, activityIntent,
                PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE)
            return pendingIntent
        }
        private fun baseIntent(context: Context): Intent = Intent(context, RadarNotificationReceiver::class.java)
    }
    override fun onReceive(context: Context, intent: Intent) {
        if (!Radar.initialized) {
            Radar.initialize(context)
        }
        if (intent.action == ACTION_NOTIFICATION_CONVERSION) {
            Radar.logger.d("Received broadcast  inside radarnoticiationreciever | action = ${intent.action}")

            val mainIntent = Intent(context, Radar.activity!!::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            context.startActivity(mainIntent)
            Log.i("testing", "running chunk of code")
        }
    }
}