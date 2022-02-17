package io.radar.example

import android.Manifest
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import io.radar.sdk.Radar
import io.radar.sdk.RadarTrackingOptions
import io.radar.sdk.RadarTripOptions
import io.radar.sdk.ktx.getAutoComplete
import io.radar.sdk.ktx.getCurrentLocation
import io.radar.sdk.ktx.getDistanceBetween
import io.radar.sdk.ktx.getGeocode
import io.radar.sdk.ktx.getGeofences
import io.radar.sdk.ktx.getIpGeocode
import io.radar.sdk.ktx.getLocationContext
import io.radar.sdk.ktx.getMatrixFor
import io.radar.sdk.ktx.getPlaces
import io.radar.sdk.ktx.getReverseGeocode
import io.radar.sdk.ktx.startTripTracking
import io.radar.sdk.ktx.trackLocation
import io.radar.sdk.livedata.initializeAndObserve
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import java.util.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_BACKGROUND_LOCATION),
                0
            )
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 0)
        }

        // Optionally, use Live Data for the receiver.
        MyRadarLiveDataReceiver(
            this,
            Radar.initializeAndObserve(this, "prj_test_pk_0000000000000000000000000000000000000000")
        )
        // Otherwise use:
//        val receiver = MyRadarReceiver()
//        Radar.initialize(this, "prj_test_pk_0000000000000000000000000000000000000000", receiver)

        // Optionally use coroutines
        MainScope().launch {
            radarCoroutines()
        }
        // Otherwise use:
        radarCallbackMethods()
    }

    private fun radarCallbackMethods() {

        Radar.getLocation { status, location, stopped ->
            Log.v("example", "Location: status = ${status}; location = $location; stopped = $stopped")
        }

        Radar.trackOnce { status, location, events, user ->
            Log.v("example", "Track once: status = ${status}; location = $location; events = $events; user = $user")
        }

        val options = RadarTrackingOptions.CONTINUOUS
        Radar.startTracking(options)

        Radar.getContext { status, location, context ->
            Log.v(
                "example",
                "Context: status = $status; location = $location; context?.geofences = ${context?.geofences}; context?.place = ${context?.place}; context?.country = ${context?.country}"
            )
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
            null,
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

        Radar.ipGeocode { status, address, proxy ->
            Log.v(
                "example",
                "IP geocode: status = $status; country = ${address?.countryCode}; city = ${address?.city}; proxy = $proxy"
            )
        }

        val origin = Location("example")
        origin.latitude = 40.78382
        origin.longitude = -73.97536

        val destination = Location("example")
        destination.latitude = 40.70390
        destination.longitude = -73.98670

        Radar.autocomplete(
            "brooklyn",
            origin,
            arrayOf("locality"),
            10,
            "US"
        ) { status, addresses ->
            Log.v("example", "Autocomplete: status = $status; address = ${addresses?.get(0)?.formattedAddress}")
        }

        Radar.getDistance(
            origin,
            destination,
            EnumSet.of(Radar.RadarRouteMode.FOOT, Radar.RadarRouteMode.CAR),
            Radar.RadarRouteUnits.IMPERIAL
        ) { status, routes ->
            Log.v(
                "example",
                "Distance: status = $status; routes.car.distance.value = ${routes?.car?.distance?.value}; routes.car.distance.text = ${routes?.car?.distance?.text}; routes.car.duration.value = ${routes?.car?.duration?.value}; routes.car.duration.text = ${routes?.car?.duration?.text}"
            )
        }

        val tripOptions = RadarTripOptions(
            "299",
            null,
            "store",
            "123",
            Radar.RadarRouteMode.CAR
        )
        Radar.startTrip(tripOptions)

        var i = 0
        Radar.mockTracking(
            origin,
            destination,
            Radar.RadarRouteMode.CAR,
            3,
            3
        ) { status, location, events, user ->
            Log.v("example", "Mock track: status = ${status}; location = $location; events = $events; user = $user")

            if (i == 2) {
                Radar.completeTrip()
            }

            i++
        }

        val origin1 = Location("example")
        origin1.latitude = 40.78382
        origin1.longitude = -73.97536

        val origin2 = Location("example")
        origin2.latitude = 40.70390
        origin2.longitude = -73.98670

        val origins = arrayOf(origin1, origin2)

        val destination1 = Location("example")
        destination1.latitude = 40.64189
        destination1.longitude = -73.78779

        val destination2 = Location("example")
        destination2.latitude = 35.99801
        destination2.longitude = -78.94294

        val destinations = arrayOf(destination1, destination2)

        Radar.getMatrix(
            origins,
            destinations,
            Radar.RadarRouteMode.CAR,
            Radar.RadarRouteUnits.IMPERIAL
        ) { status, matrix ->
            Log.v(
                "example",
                "Matrix: status = $status; matrix[0][0].duration.text = ${
                    matrix?.routeBetween(
                        0,
                        0
                    )?.duration?.text
                }; matrix[0][1].duration.text = ${
                    matrix?.routeBetween(
                        0,
                        1
                    )?.duration?.text
                }; matrix[1][0].duration.text = ${
                    matrix?.routeBetween(
                        1,
                        0
                    )?.duration?.text
                };  matrix[1][1].duration.text = ${matrix?.routeBetween(1, 1)?.duration?.text}"
            )
        }
    }

    private suspend fun radarCoroutines() {

        val response = Radar.getCurrentLocation()
        Log.v(
            "example",
            "Location: status = ${response.status}; location = ${response.location}; stopped = ${response.stopped}"
        )

        val track = Radar.trackLocation()
        Log.v(
            "example",
            "Track once: status = ${track.status}; " +
                    "location = ${track.location}; " +
                    "events = ${track.events}; " +
                    "user = ${track.user}"
        )

        val ctx = Radar.getLocationContext()
        Log.v(
            "example",
            "Context: status = ${ctx.status}; location = ${ctx.location}; context?.geofences = ${ctx.context?.geofences}; context?.place = ${ctx.context?.place}; context?.country = ${ctx.context?.country}"
        )

        val places = Radar.getPlaces(
            1000,
            arrayOf("walmart"),
            null,
            null,
            10
        )
        Log.v(
            "example",
            "Search places: status = ${places.status}; location = ${places.location}; places = ${places.places}"
        )

        val geo = Radar.getGeofences(
            1000,
            arrayOf("store"),
            null,
            10
        )
        Log.v(
            "example",
            "Search geofences: status = ${geo.status}; location = ${geo.location}; geofences = ${geo.geofences}"
        )

        val geocode = Radar.getGeocode("20 jay street brooklyn")
        Log.v(
            "example",
            "Geocode: status = ${geocode.status}; address = ${geocode.addresses?.get(0)?.formattedAddress}"
        )

        val rev = Radar.getReverseGeocode()
        Log.v(
            "example",
            "Reverse geocode: status = ${rev.status}; coordinate = ${rev.addresses?.first()?.formattedAddress}"
        )

        val ip = Radar.getIpGeocode()
        Log.v(
            "example",
            "IP geocode: status = ${ip.status}; " +
                    "country = ${ip.address?.countryCode}; " +
                    "city = ${ip.address?.city}; " +
                    "proxy = ${ip.proxy}"
        )

        val origin = Location("example")
        origin.latitude = 40.78382
        origin.longitude = -73.97536

        val destination = Location("example")
        destination.latitude = 40.70390
        destination.longitude = -73.98670

        val auto = Radar.getAutoComplete(
            query = "brooklyn",
            near = origin,
            layers = arrayOf("locality"),
            limit = 10,
            country = "US"
        )
        Log.v(
            "example",
            "Autocomplete: status = ${auto.status}; address = ${auto.addresses?.get(0)?.formattedAddress}"
        )

        val dist = Radar.getDistanceBetween(
            origin,
            destination,
            EnumSet.of(Radar.RadarRouteMode.FOOT, Radar.RadarRouteMode.CAR),
            Radar.RadarRouteUnits.IMPERIAL
        )
        Log.v(
            "example",
            "Distance: status = ${dist.status}; " +
                    "routes.car.distance.value = ${dist.routes?.car?.distance?.value}; " +
                    "routes.car.distance.text = ${dist.routes?.car?.distance?.text}; " +
                    "routes.car.duration.value = ${dist.routes?.car?.duration?.value}; " +
                    "routes.car.duration.text = ${dist.routes?.car?.duration?.text}"
        )

        val tripOptions = RadarTripOptions(
            "299",
            null,
            "store",
            "123",
            Radar.RadarRouteMode.CAR
        )
        Radar.startTripTracking(tripOptions)
        // No mockTracking with coroutines, since it would require Flows, and solution would be redundant to the
        // existing method
        Radar.completeTrip()

        val origin1 = Location("example")
        origin1.latitude = 40.78382
        origin1.longitude = -73.97536

        val origin2 = Location("example")
        origin2.latitude = 40.70390
        origin2.longitude = -73.98670

        val origins = arrayOf(origin1, origin2)

        val destination1 = Location("example")
        destination1.latitude = 40.64189
        destination1.longitude = -73.78779

        val destination2 = Location("example")
        destination2.latitude = 35.99801
        destination2.longitude = -78.94294

        val destinations = arrayOf(destination1, destination2)

        val matrix = Radar.getMatrixFor(
            origins,
            destinations,
            Radar.RadarRouteMode.CAR,
            Radar.RadarRouteUnits.IMPERIAL
        )
        Log.v(
            "example",
            "Matrix: status = ${matrix.status}; matrix[0][0].duration.text = ${
                matrix.matrix?.routeBetween(
                    0,
                    0
                )?.duration?.text
            }; matrix[0][1].duration.text = ${
                matrix.matrix?.routeBetween(
                    0,
                    1
                )?.duration?.text
            }; matrix[1][0].duration.text = ${
                matrix.matrix?.routeBetween(
                    1,
                    0
                )?.duration?.text
            };  matrix[1][1].duration.text = ${matrix.matrix?.routeBetween(1, 1)?.duration?.text}"
        )
    }

}
