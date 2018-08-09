package io.radar.example;

import android.content.Context;
import android.provider.Settings;

import io.radar.sdk.Radar;
import io.radar.sdk.model.RadarEvent;

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

    static String stringForEvent(RadarEvent event) {
        switch (event.getType()) {
            case USER_ENTERED_GEOFENCE:
                return "Entered geofence " + (event.getGeofence() != null ? event.getGeofence().getDescription() : "-");
            case USER_EXITED_GEOFENCE:
                return "Exited geofence " + (event.getGeofence() != null ? event.getGeofence().getDescription() : "-");
            case USER_ENTERED_HOME:
                return "Entered home";
            case USER_EXITED_HOME:
                return "Exited home";
            case USER_ENTERED_OFFICE:
                return "Entered office";
            case USER_EXITED_OFFICE:
                return "Exited office";
            case USER_STARTED_TRAVELING:
                return "Started traveling";
            case USER_STOPPED_TRAVELING:
                return "Stopped traveling";
            case USER_ENTERED_PLACE:
                return "Entered place " + (event.getPlace() != null ? event.getPlace().getName() : "-");
            case USER_EXITED_PLACE:
                return "Exited place " + (event.getPlace() != null ? event.getPlace().getName() : "-");
            default:
                return "-";
        }
    }

}
