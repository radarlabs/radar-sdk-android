package io.radar.example

import android.Manifest
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.core.app.ActivityCompat
import io.radar.sdk.Radar

enum class RadarAction {
    GET_LOCATION,
    TRACK_ONCE
}

class MainActivity : AppCompatActivity() {

    var currentAction = RadarAction.GET_LOCATION

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val requestCode = 0
            val permissions = arrayOf(
                Manifest.permission.ACCESS_COARSE_LOCATION,
            )
            ActivityCompat.requestPermissions(this, permissions, requestCode)
        }

        // initialize Radar
        Radar.initialize(this, "prj_test_pk_0000000000000000000000000000000000000000")
        Radar.setUserId("my-test-user")
    }

    fun doAction(view: View) {
        val debugText: TextView = findViewById(R.id.debug_text)
        when (currentAction) {
            RadarAction.GET_LOCATION -> {
                debugText.text = "Loading"
                Radar.getLocation { status, location, stopped ->
                    if (location != null) {
                        debugText.text = "(${location.latitude}, ${location.longitude})\nAccuracy: ${location.accuracy} Stopped: $stopped"
                    } else {
                        debugText.text = "Location couldn't be fetched (Status: $status)"
                    }
                }
            }
            RadarAction.TRACK_ONCE -> {
                debugText.text = "Loading"
                Radar.trackOnce { status, location, events, user ->
                if (location != null) {
                        debugText.text = "(${location.latitude}, ${location.longitude})\nAccuracy: ${location.accuracy} Events: ${events?.size} User: $user\n(Status: $status)"
                } else {
                        debugText.text = "Location couldn't be fetched (Status: $status)"
                    }
                }
            }

        }
    }

    fun switchAction(view: View) {
        val button: Button = findViewById(R.id.action_button)
        when (currentAction) {
            RadarAction.GET_LOCATION -> {
                button.text = "Track once"
                currentAction = RadarAction.TRACK_ONCE
            }
            else -> {
                button.text = "Get location"
                currentAction = RadarAction.GET_LOCATION
            }
        }
    }


}
