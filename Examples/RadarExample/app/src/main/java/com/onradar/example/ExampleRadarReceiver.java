package com.onradar.example;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.util.Log;

import io.radar.sdk.Radar;
import io.radar.sdk.RadarReceiver;
import io.radar.sdk.model.RadarEvent;
import io.radar.sdk.model.RadarUser;

public class ExampleRadarReceiver extends RadarReceiver {

    private static final String TAG = "ExampleRadarReceiver";
    private static final int NOTIFICATION_ID = 1337;

    @Override
    public void onEventsReceived(@NonNull Context context, @NonNull RadarEvent[] events, @NonNull RadarUser user) {
        for (RadarEvent event : events) {
            String eventString = Utils.stringForEvent(event);
            this.notify(context, "Event", eventString);
        }
    }

    @Override
    public void onError(@NonNull Context context, @NonNull Radar.RadarStatus status) {
        String statusString = Utils.stringForStatus(status);
        Log.e(TAG, statusString);
    }

    private void notify(Context context, String title, String text) {
        Intent intent = new Intent(context, ExampleRadarReceiver.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pending = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        Notification notification = new Notification.Builder(context)
                .setContentTitle(title)
                .setContentText(text)
                .setAutoCancel(true)
                .setContentIntent(pending)
                .setSmallIcon(io.radar.sdk.R.drawable.notification)
                .setSound(Settings.System.DEFAULT_NOTIFICATION_URI)
                .build();

        NotificationManager notificationManager = (NotificationManager)context.getSystemService(android.content.Context.NOTIFICATION_SERVICE);
        notificationManager.notify(TAG, NOTIFICATION_ID, notification);
    }

}
