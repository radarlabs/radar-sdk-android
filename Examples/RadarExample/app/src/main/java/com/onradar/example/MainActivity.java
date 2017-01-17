package com.onradar.example;

import android.location.Location;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

import com.onradar.sdk.Radar;
import com.onradar.sdk.RadarCallback;
import com.onradar.sdk.model.RadarEvent;
import com.onradar.sdk.model.RadarGeofence;
import com.onradar.sdk.model.RadarUser;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        String publishableKey = ""; // replace with your publishable API key
        Radar.initialize(this, publishableKey);

        String userId = Utils.getUserId(this);
        Radar.setUserId(userId);

        if (!Radar.checkSelfPermissions()) {
            Radar.requestPermissions(this);
        }

        TextView userIdTextView = (TextView)findViewById(R.id.user_id_text_view);
        userIdTextView.setText(userId);

        final Button trackOnceButton = (Button)findViewById(R.id.track_once_button);
        trackOnceButton.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View view) {
                trackOnceButton.setEnabled(false);

                Radar.trackOnce(new RadarCallback() {
                    @Override
                    public void onCallback(@NonNull Radar.RadarStatus status, Location location, RadarEvent[] events, RadarUser user) {
                        trackOnceButton.setEnabled(true);

                        String statusString = Utils.stringForStatus(status);
                        Snackbar.make(trackOnceButton, statusString, Snackbar.LENGTH_SHORT).show();

                        if (status == Radar.RadarStatus.SUCCESS) {
                            Log.i(TAG, statusString);

                            RadarGeofence[] geofences = user.getGeofences();
                            for (RadarGeofence geofence : geofences) {
                                String geofenceString = Utils.stringForGeofence(geofence);
                                Log.i(TAG, geofenceString);
                            }

                            for (RadarEvent event : events) {
                                String eventString = Utils.stringForEvent(event);
                                Log.i(TAG, eventString);
                            }
                        } else {
                            Log.e(TAG, statusString);
                        }
                    }
                });
            }
        });

        final Switch trackingSwitch = (Switch)findViewById(R.id.tracking_switch);
        trackingSwitch.setChecked(Radar.isTracking());
        trackingSwitch.setOnCheckedChangeListener(new Switch.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    Radar.startTracking();
                } else {
                    Radar.stopTracking();
                }
            }
        });
    }

}
