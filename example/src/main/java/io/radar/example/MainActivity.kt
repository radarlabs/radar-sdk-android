package io.radar.example

import android.content.Context
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import io.radar.sdk.Radar
import io.radar.sdk.RadarTrackingOptions
import io.radar.sdk.RadarTripOptions
import io.radar.sdk.RadarVerifiedReceiver
import io.radar.sdk.model.RadarVerifiedLocationToken
import org.json.JSONObject
import java.util.EnumSet
import androidx.core.content.edit
import io.radar.sdk.model.RadarAddress
import io.radar.sdk.model.RadarCoordinate

class MainActivity : AppCompatActivity() {

    val demoFunctions: ArrayList<() -> Unit> = ArrayList()
    private lateinit var listView: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val receiver = MyRadarReceiver()
        Radar.initialize(this, "prj_test_pk_0000000000000000000000000000000000000000", receiver, Radar.RadarLocationServicesProvider.GOOGLE, true)
        Radar.sdkVersion().let { Log.i("version", it) }

        val verifiedReceiver = object : RadarVerifiedReceiver() {
            override fun onTokenUpdated(context: Context, token: RadarVerifiedLocationToken) {

            }
        }
        Radar.setVerifiedReceiver(verifiedReceiver)

        listView = findViewById(R.id.buttonList)
        createButtons()
    }

    fun createButton(text: String, function: () -> Unit) {
        val button = Button(this);
        button.text = text
        button.isAllCaps = false
        button.tag = demoFunctions.size
        button.setOnClickListener {
            function()
        }

        demoFunctions.add(function)
        listView.addView(button)
    }

    fun createButtons() {

        createButton("getLocation") {
            Radar.getLocation { status, location, stopped ->
                Log.v(
                    "example",
                    "Location: status = ${status}; location = $location; stopped = $stopped"
                )
            }
        }

        createButton("trackVerified") {
            Radar.trackVerified(false) { status, token ->
                Log.v("example", "TrackVerified: status = $status; token = ${token?.toJson()}")
            }
        }

        createButton("trackOnce") {
            Radar.trackOnce { status, location, events, user ->
                Log.v(
                    "example",
                    "Track once: status = ${status}; location = $location; events = $events; user = $user"
                )
            }
        }

        createButton("startTracking") {
            val options = RadarTrackingOptions.RESPONSIVE
            Radar.startTracking(options)
        }

        createButton("getContext") {
            Radar.getContext { status, location, context ->
                Log.v(
                    "example",
                    "Context: status = $status; location = $location; context?.geofences = ${context?.geofences}; context?.place = ${context?.place}; context?.country = ${context?.country}"
                )
            }
        }

        createButton("searchPlaces") {
            // In the Radar dashboard settings (https://radar.com/dashboard/settings), add this to
            // the chain metadata: {"mcdonalds":{"orderActive":"true"}}.
            Radar.searchPlaces(
                1000,
                arrayOf("mcdonalds"),
                mapOf("orderActive" to "true"),
                null,
                null,
                10
            ) { status, location, places ->
                Log.v(
                    "example",
                    "Search places: status = $status; location = $location; places = $places"
                )
            }
        }

        createButton("searchGeofences") {
            Radar.searchGeofences(
                1000,
                arrayOf("store"),
                null,
                10
            ) { status, location, geofences ->
                Log.v(
                    "example",
                    "Search geofences: status = $status; location = $location; geofences = $geofences"
                )
            }
        }

        createButton("geocode") {
            Radar.geocode("20 jay street brooklyn") { status, addresses ->
                Log.v(
                    "example",
                    "Geocode: status = $status; address = ${addresses?.get(0)?.formattedAddress}"
                )
            }

            Radar.geocode(
                "20 jay street brooklyn",
                arrayOf("place", "locality"),
                arrayOf("US", "CA")
            ) { status, addresses ->
                Log.v(
                    "example",
                    "Geocode: status = $status; address = ${addresses?.get(0)?.formattedAddress}"
                )
            }
        }

        createButton("reverseGeocode") {
            Radar.reverseGeocode { status, addresses ->
                Log.v(
                    "example",
                    "Reverse geocode: status = $status; coordinate = ${addresses?.first()?.formattedAddress}"
                )
            }

            Radar.reverseGeocode(arrayOf("locality", "state")) { status, addresses ->
                Log.v(
                    "example",
                    "Reverse geocode: status = $status; coordinate = ${addresses?.first()?.formattedAddress}"
                )
            }

            val reverseGeocodeLocation = Location("example")
            reverseGeocodeLocation.latitude = 40.70390
            reverseGeocodeLocation.longitude = -73.98670

            Radar.reverseGeocode(reverseGeocodeLocation) { status, addresses ->
                Log.v(
                    "example",
                    "Reverse geocode: status = $status; coordinate = ${addresses?.first()?.formattedAddress}"
                )
            }

            Radar.reverseGeocode(
                reverseGeocodeLocation,
                arrayOf("locality", "state")
            ) { status, addresses ->
                Log.v(
                    "example",
                    "Reverse geocode: status = $status; coordinate = ${addresses?.first()?.formattedAddress}"
                )
            }
        }

        createButton("ipGeocode") {
            Radar.ipGeocode { status, address, proxy ->
                Log.v(
                    "example",
                    "IP geocode: status = $status; country = ${address?.countryCode}; city = ${address?.city}; proxy = $proxy ${address?.toJson()}"
                )
            }
        }

        createButton("validateAddress") {
            val addressWithStreetAndNumber = RadarAddress(
                coordinate = RadarCoordinate(.0, .0),
                city = "New York",
                stateCode = "NY",
                postalCode = "10003",
                countryCode = "US",
                street = "Broadway",
                number = "841",
            )
            Radar.validateAddress(addressWithStreetAndNumber) { status, address, verificationStatus ->
                Log.v(
                    "example",
                    "Validate address with street + number: status $status; address = ${address?.toJson()}; verificationStatus = ${verificationStatus.toString()}"
                )
            }
            val addressWithAddressLabel = RadarAddress(
                coordinate = RadarCoordinate(.0, .0),
                city = "New York",
                stateCode = "NY",
                postalCode = "10003",
                countryCode = "US",
                addressLabel = "841 Broadway",
            )
            Radar.validateAddress(addressWithAddressLabel) { status, address, verificationStatus ->
                Log.v(
                    "example",
                    "Validate address with address label: status $status; address = ${address?.toJson()}; verificationStatus = ${verificationStatus.toString()}"
                )
            }
        }

        val origin = Location("example")
        origin.latitude = 40.78382
        origin.longitude = -73.97536

        val destination = Location("example")
        destination.latitude = 40.70390
        destination.longitude = -73.98670

        createButton("autoComplete") {
            Radar.autocomplete(
                "brooklyn",
                origin,
                arrayOf("locality"),
                10,
                "US",
                true,
            ) { status, addresses ->
                Log.v(
                    "example",
                    "Autocomplete: status = $status; address = ${addresses?.get(0)?.formattedAddress}"
                )
            }

            Radar.autocomplete(
                "brooklyn",
                origin,
                arrayOf("locality"),
                10,
                "US",
                mailable = true
            ) { status, addresses ->
                Log.v(
                    "example",
                    "Autocomplete: status = $status; address = ${addresses?.get(0)?.formattedAddress}"
                )
            }

            Radar.autocomplete(
                "brooklyn",
                origin,
                arrayOf("locality"),
                10,
                "US"
            ) { status, addresses ->
                Log.v(
                    "example",
                    "Autocomplete: status = $status; address = ${addresses?.get(0)?.formattedAddress}"
                )
            }

            Radar.autocomplete(
                "brooklyn",
                origin,
                arrayOf("locality"),
                10,
                "US",
                true
            ) { status, addresses ->
                Log.v(
                    "example",
                    "Autocomplete: status = $status; address = ${addresses?.get(0)?.formattedAddress}"
                )
                Radar.validateAddress(
                    addresses?.get(0)
                ) { statusValidationRequest, address, verificationStatus ->
                    Log.v(
                        "example",
                        "Validate address: status = $statusValidationRequest; address = ${address?.formattedAddress}, verificationStatus = ${
                            Radar.stringForVerificationStatus(verificationStatus)
                        }"
                    )
                }
            }
        }

        createButton("getDistance") {
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
        }

        createButton("mockTracking") {
            val tripOptions = RadarTripOptions(
                "299",
                null,
                "store",
                "123",
                Radar.RadarRouteMode.CAR,
                approachingThreshold = 0
            )
            val onTripTrackingOptions = RadarTrackingOptions.CONTINUOUS
            Radar.startTrip(tripOptions, onTripTrackingOptions)

            var i = 0
            Radar.mockTracking(
                origin,
                destination,
                Radar.RadarRouteMode.CAR,
                3,
                3
            ) { status, location, events, user ->
                Log.v(
                    "example",
                    "Mock track: status = ${status}; location = $location; events = $events; user = $user"
                )

                if (i == 2) {
                    Radar.completeTrip()
                }

                i++
            }
        }

        createButton("getMatrix") {
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

        createButton("logConversion") {
            val conversionMetadata = JSONObject()
            conversionMetadata.put("one", "two")

            Radar.logConversion(
                "app_open_android",
                conversionMetadata
            ) { status, event ->
                Log.v(
                    "example",
                    "Conversion name = ${event?.conversionName}: status = $status; event = $event"
                )
            }
        }

        createButton("run all demo") {
            for (function in demoFunctions) {
                function()
            }
        }
        demoFunctions.removeAt(demoFunctions.size - 1)
    }
}
