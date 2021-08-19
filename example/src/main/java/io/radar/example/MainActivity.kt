package io.radar.example

import android.Manifest
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.*
import androidx.core.app.ActivityCompat
import io.radar.sdk.Radar
import io.radar.sdk.RadarTrackingOptions

enum class ActionState {
    TRACK_ONCE,
    TRACK_BG,
    REVERSE_GEOCODE
}

class MainActivity : AppCompatActivity() {

    private val DEFAULT_PUB_KEY = "prj_test_pk_0000000000000000000000000000000000000000"

    private val debugLogsArray = arrayListOf<String>()
    private var debugLogsAdapter: ArrayAdapter<String>? = null

    private var actionState: ActionState = ActionState.TRACK_ONCE

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // UI
        setContentView(R.layout.activity_main)

        this.debugLogsAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_list_item_1,
            this.debugLogsArray
        )
        this.findViewById<ListView>(R.id.debugLogs).adapter = this.debugLogsAdapter

        // request location permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val requestCode = 0
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_BACKGROUND_LOCATION), requestCode)
            } else {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), requestCode)
            }
        }

        //
        // initialize Radar - replace this with your own publishableKey!
        //
        val myRadarPublishableKey = "prj_test_pk_0000000000000000000000000000000000000000"
        if (myRadarPublishableKey == DEFAULT_PUB_KEY)  {
            findViewById<TextView>(R.id.debugLogsTitle).text = "Add your publishable key before using this app!"
        } else {
            Radar.initialize(this, myRadarPublishableKey)
        }
        Radar.setUserId("test-android-user")
    }

    override fun onResume() {
        super.onResume()

        this.findViewById<Button>(R.id.switchAction)
            .setOnClickListener { handleSwitchAction() }

        // initialize to trackOnce, unless app is already tracking
        if (Radar.isTracking()) {
            toggleTrackBackgroundMode()
        } else {
            toggleTrackOnceMode()
        }
    }

    private fun handleSwitchAction() {
        debugLogsAdapter?.clear()

        when (this.actionState) {
            ActionState.TRACK_ONCE -> toggleTrackBackgroundMode()
            ActionState.TRACK_BG -> toggleReverseGeocodeMode()
            ActionState.REVERSE_GEOCODE -> toggleTrackOnceMode()
        }
    }

    private fun toggleTrackOnceMode() {
        // set state
        this.actionState = ActionState.TRACK_ONCE

        // initialize button state
        val actionButton = this.findViewById<Button>(R.id.onAction)
        actionButton.text = "Track once"

        // button handler
        actionButton.setOnClickListener { view ->
            if (view is Button) {
                view.text = "Loading..."
            }

            Radar.trackOnce { status, location, events, user ->
                this@MainActivity.runOnUiThread {
                    this.debugLogsAdapter?.add("Status: $status")
                    this.debugLogsAdapter?.add("Latitude: ${location?.latitude}")
                    this.debugLogsAdapter?.add("Longitude: ${location?.longitude}")
                    this.debugLogsAdapter?.add("Accuracy: ${location?.accuracy}")

                    this.debugLogsAdapter?.add("User: ${user?.toJson()?.toString(2)}")

                    if (events != null && events.isNotEmpty()) {
                        this.debugLogsAdapter?.add("Events:")
                        for (event in events) {
                            this.debugLogsAdapter?.add(event.toJson().toString(2))
                        }
                    }

                    // done loading
                    if (view is Button) {
                        view.text = "Track once"
                    }
                }
            }
        }
    }

    private fun toggleTrackBackgroundMode() {
        // set state
        this.actionState = ActionState.TRACK_BG

        // initialize button state
        val actionButton = this.findViewById<Button>(R.id.onAction)
        fun initButtonText() {
            if (Radar.isTracking()) {
                actionButton.text = "Stop tracking"
            } else {
                actionButton.text = "Track background"
            }
        }
        initButtonText()

        this.debugLogsAdapter?.add("Tracking options:")
        this.debugLogsAdapter?.add(Radar.getTrackingOptions()?.toJson()?.toString(2))

        // button handler
        actionButton.setOnClickListener { view ->
            if (Radar.isTracking()) {
                Radar.stopTracking()
                initButtonText()
            } else {
                Radar.startTracking(RadarTrackingOptions.RESPONSIVE)
                initButtonText()
            }
        }
    }

    private fun toggleReverseGeocodeMode() {
        // set state
        this.actionState = ActionState.REVERSE_GEOCODE

        // initialize button state
        val actionButton = this.findViewById<Button>(R.id.onAction)
        actionButton.text = "Reverse geocode"

        // button handler
        actionButton.setOnClickListener { view ->
            if (view is Button) {
                view.text = "Loading..."
            }

            Radar.reverseGeocode { status, addresses ->
                this@MainActivity.runOnUiThread {
                    this.debugLogsAdapter?.add("Status: $status")

                    if (addresses != null && addresses.isNotEmpty()) {
                        this.debugLogsAdapter?.add("Addresses:")
                        for (address in addresses) {
                            this.debugLogsAdapter?.add(address.toJson().toString(2))
                        }
                    }

                    // done loading
                    if (view is Button) {
                        view.text = "Reverse geocode"
                    }
                }
            }
        }
    }

}
