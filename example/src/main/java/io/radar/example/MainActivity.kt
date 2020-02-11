package io.radar.example

import android.Manifest
import android.location.Location
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.core.app.ActivityCompat
import io.radar.sdk.Radar
import io.radar.sdk.RadarTrackingOptions
import io.radar.sdk.model.RadarContext
import io.radar.sdk.model.RadarPlace
import java.util.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (Build.VERSION.SDK_INT >= 23) {
            val requestCode = 0
            if (Build.VERSION.SDK_INT >= 29) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_BACKGROUND_LOCATION), requestCode)
            } else {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), requestCode)
            }
        }

        Radar.initialize(this, "prj_test_pk_0000000000000000000000000000000000000000")
        Radar.setLogLevel(Radar.RadarLogLevel.DEBUG)

        Radar.trackOnce { status, location, events, user ->
            Log.v("example", "Track once: status = ${status}; location = $location; events = $events; user = $user")
        }

        val options = RadarTrackingOptions.RESPONSIVE
        options.sync = RadarTrackingOptions.RadarTrackingOptionsSync.ALL
        Radar.startTracking(options)

        Radar.getContext { status, location, context ->
            Log.v("example", "Context: status = $status; location = $location; context?.geofences = ${context?.geofences}; context?.place = ${context?.place}; context?.country = ${context?.country}")
        }

        Radar.searchPlaces(
            1000,
            arrayOf("walmart"),
            null,
            null,
            10
        ) { status, location, places ->
            Log.v("example", "Search places: status = $status; location = $location; places = $places")
        }

        Radar.searchGeofences(
            1000,
            arrayOf("store"),
            10
        ) { status, location, geofences ->
            Log.v("example", "Search geofences: status = $status; location = $location; geofences = $geofences")
        }

        Radar.geocode("20 jay street brooklyn") { status, addresses ->
            Log.v("example", "Geocode: status = $status; address = ${addresses?.get(0)?.formattedAddress}")
        }

        Radar.reverseGeocode { status, addresses ->
            Log.v("example", "Reverse geocode: status = $status; coordinate = ${addresses?.first()?.formattedAddress}")
        }

        Radar.ipGeocode { status, country ->
            Log.v("example", "IP geocode: status = $status; code = ${country?.code}; flag = ${country?.flag}")
        }

        val origin = Location("example")
        origin.latitude = 40.70390
        origin.longitude = -73.98670

        val destination = Location("example")
        destination.latitude = 40.78382
        destination.longitude = -73.97536

        Radar.autocomplete(
            "brooklyn roasting",
            origin,
            10
        ) { status, addresses ->
            Log.v("example", "Autocomplete: status = $status; address = ${addresses?.get(0)?.formattedAddress}")
        }

        Radar.getDistance(
            origin,
            destination,
            EnumSet.of(Radar.RadarRouteMode.FOOT, Radar.RadarRouteMode.CAR),
            Radar.RadarRouteUnits.IMPERIAL
        ) { status, routes ->
            Log.v("example", "Distance: status = $status; routes.car.distance.value = ${routes?.car?.distance?.value}; routes.car.distance.text = ${routes?.car?.distance?.text}; routes.car.duration.value = ${routes?.car?.duration?.value}; routes.car.duration.text = ${routes?.car?.duration?.text}")
        }
    }

}
