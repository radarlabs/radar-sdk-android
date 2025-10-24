package io.radar.example

import android.app.Activity
import android.location.Location
import android.os.Build
import android.util.Log
import android.view.Gravity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import io.radar.sdk.Radar
import io.radar.sdk.RadarTrackingOptions
import io.radar.sdk.RadarTripOptions
import io.radar.sdk.model.RadarAddress
import io.radar.sdk.model.RadarCoordinate
import org.json.JSONObject
import java.util.Date
import java.util.EnumSet

@Composable
fun CustomButton(label: String, onClick: () -> Unit) {
    val hapticFeedback = LocalHapticFeedback.current
    Button(onClick = {
        hapticFeedback.performHapticFeedback(HapticFeedbackType.Confirm)
        onClick()
    }) {
        Text(label)
    }
}

@Composable
fun TestView() {
    val scrollState = rememberScrollState()

    Column(modifier = Modifier.fillMaxWidth().verticalScroll(scrollState)) {
        val activity = LocalContext.current as Activity
        CustomButton("requestForegroundPermission") {
            requestForegroundPermission(activity)
        }

        CustomButton("requestBackgroundPermission") {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                requestBackgroundPermission(activity)
            }
        }

        CustomButton("requestActivityRecognitionPermission") {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                requestActivityRecognitionPermission(activity)
            }
        }

        CustomButton("getLocation") {
            Radar.getLocation { status, location, stopped ->
                Log.v(
                    "example",
                    "Location: status = ${status}; location = $location; stopped = $stopped"
                )
            }
        }

        CustomButton("startTrackingVerified") {
            Radar.startTrackingVerified(60, false)
        }

        CustomButton("stopTrackingVerified") {
            Radar.stopTrackingVerified()
        }

        CustomButton("getVerifiedLocationToken") {
            Radar.getVerifiedLocationToken { status, token ->
                Log.v("example", "GetVerifiedLocationToken: status = $status; token = ${token?.toJson()}")
            }
        }

        CustomButton("trackVerified") {
            Radar.trackVerified(false) { status, token ->
                Log.v("example", "TrackVerified: status = $status; token = ${token?.toJson()}")
            }
        }

        CustomButton("setExpectedJurisdiction") {
            Radar.setExpectedJurisdiction("US", "CA")
        }

        CustomButton("trackOnce") {
            Radar.trackOnce { status, location, events, user ->
                Log.v(
                    "example",
                    "Track once: status = ${status}; location = $location; events = $events; user = $user"
                )
            }
        }

        CustomButton("startTracking") {
            val options = RadarTrackingOptions.RESPONSIVE
            Radar.startTracking(options)
        }

        CustomButton("stopTracking") {
            Radar.stopTracking()
        }

        CustomButton("getContext") {
            Radar.getContext { status, location, context ->
                Log.v(
                    "example",
                    "Context: status = $status; location = $location; context?.geofences = ${context?.geofences}; context?.place = ${context?.place}; context?.country = ${context?.country}"
                )
            }
        }

        CustomButton("searchPlaces") {
            // In the Radar dashboard settings (https://radar.com/dashboard/settings), add this to
            // the chain metadata: {"mcdonalds":{"orderActive":"true"}}.
            Radar.searchPlaces(
                1000,
                arrayOf("mcdonalds"),
                mapOf("orderActive" to "true"),
                null,
                null,
                arrayOf("US", "SG"),
                10
            ) { status, location, places ->
                Log.v(
                    "example",
                    "Search places: status = $status; location = $location; places = $places"
                )
            }
        }

        CustomButton("searchGeofences") {
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

        CustomButton("geocode") {
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

        CustomButton("reverseGeocode") {
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
                    "Reverse geocode: status = $status; coordinate = ${addresses?.first()?.formattedAddress}, timeZone = ${addresses?.first()?.timeZone?.toJson()}"
                )
            }

            val reverseGeocodeLocationLondon = Location("example")
            reverseGeocodeLocationLondon.latitude = 51.5074
            reverseGeocodeLocationLondon.longitude = -0.1278

            Radar.reverseGeocode(reverseGeocodeLocationLondon) { status, addresses ->
                Log.v(
                    "example",
                    "Reverse geocode: status = $status; coordinate = ${addresses?.first()?.formattedAddress}, timeZone = ${addresses?.first()?.timeZone?.toJson()}"
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

        CustomButton("ipGeocode") {
            Radar.ipGeocode { status, address, proxy ->
                Log.v(
                    "example",
                    "IP geocode: status = $status; country = ${address?.countryCode}; city = ${address?.city}; proxy = $proxy ${address?.toJson()}"
                )
            }
        }

        CustomButton("validateAddress") {
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

        CustomButton("autoComplete") {
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

        CustomButton("getDistance") {
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

        CustomButton("startTrip") {
            val tripOptions = RadarTripOptions(
                "400",
                null,
                "store",
                "123",
                Radar.RadarRouteMode.CAR,
                approachingThreshold = 9
            )
            Radar.startTrip(tripOptions)
        }

        CustomButton("startTrip with start tracking false") {
            val tripOptions = RadarTripOptions(
                "501",
                null,
                "store",
                "123",
                Radar.RadarRouteMode.CAR,
                approachingThreshold = 9,
                startTracking = false
            )
            Radar.startTrip(tripOptions)
        }

        CustomButton("startTrip with tracking options") {
            val tripOptions = RadarTripOptions(
                "502",
                null,
                "store",
                "123",
                Radar.RadarRouteMode.CAR,
                approachingThreshold = 9
            )
            val onTripTrackingOptions = RadarTrackingOptions.CONTINUOUS
            Radar.startTrip(tripOptions, onTripTrackingOptions)
        }

        CustomButton("startTrip with tracking options and startTrackingAfter") {
            val tripOptions = RadarTripOptions(
                "507",
                null,
                "store",
                "123",
                Radar.RadarRouteMode.CAR,
                approachingThreshold = 9,
                startTracking = false
            )
            val onTripTrackingOptions = RadarTrackingOptions.CONTINUOUS
            // startTrackingAfter 3 minutes from now
            onTripTrackingOptions.startTrackingAfter = Date(System.currentTimeMillis() + (3 * 60 * 1000))
            Radar.startTrip(tripOptions, onTripTrackingOptions)
        }

        CustomButton("completeTrip") {
            Radar.completeTrip()
        }

        CustomButton("mockTracking") {
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

        CustomButton("getMatrix") {
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

        CustomButton("logConversion") {
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
    }
}