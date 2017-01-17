package com.onradar.example;

import android.content.Context;
import android.provider.Settings;

import com.onradar.sdk.Radar;
import com.onradar.sdk.model.RadarEvent;
import com.onradar.sdk.model.RadarGeofence;

class Utils {

    static String getUserId(Context context) {
        if (context == null) {
            return null;
        }

        return Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
    }

    static String stringForStatus(Radar.RadarStatus status) {
        switch (status) {
            case SUCCESS:
                return "Success";
            case ERROR_PUBLISHABLE_KEY:
                return "Publishable Key Error";
            case ERROR_USER_ID:
                return "User ID Error";
            case ERROR_PERMISSIONS:
                return "Permissions Error";
            case ERROR_LOCATION:
                return "Location Error";
            case ERROR_NETWORK:
                return "Network Error";
            case ERROR_UNAUTHORIZED:
                return "Unauthorized Error";
            case ERROR_SERVER:
                return "Server Error";
            default:
                return "Unknown Error";
        }

    }

    static String stringForGeofence(RadarGeofence geofence) {
        String description = geofence.getDescription();
        String tag = geofence.getTag() == null ? "null" : geofence.getTag();
        String externalId = geofence.getExternalId() == null ? "null" : geofence.getExternalId();
        return description + " / " + tag + " / " + externalId;
    }

    static String stringForEvent(RadarEvent event) {
        String type = event.getType() == RadarEvent.RadarEventType.USER_ENTERED_GEOFENCE ? "user.entered_geofence" : "user.exited_geofence";
        String description = event.getGeofence().getDescription();
        return type + " / " + description;
    }

}
