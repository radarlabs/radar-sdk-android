package io.radar.example.tests.panels

import android.location.Location
import androidx.compose.runtime.Composable
import io.radar.example.Utils
import io.radar.example.components.ActionButton
import io.radar.example.components.TogglePanel
import io.radar.example.store.LocalLogStore
import io.radar.sdk.Radar
import io.radar.sdk.model.RadarAddress
import io.radar.sdk.model.RadarCoordinate
import java.util.EnumSet

@Composable
fun SearchPanel() {
    val log = LocalLogStore.current

    val origin = Location("example").apply { latitude = 40.78382; longitude = -73.97536 }
    val destination = Location("example").apply { latitude = 40.70390; longitude = -73.98670 }

    TogglePanel("Search & Geocoding") {
        ActionButton("searchPlaces") {
            Radar.searchPlaces(1000, arrayOf("mcdonalds"), null, null, arrayOf("US", "SG"), 10) { status, location, places ->
                log.writeStatus(status, "searchPlaces: ${Utils.stringForRadarStatus(status)}", "places = ${places?.size ?: 0}\nfirst = ${places?.firstOrNull()?.name}")
            }
        }
        ActionButton("searchGeofences") {
            Radar.searchGeofences(1000, arrayOf("store"), null, 10) { status, location, geofences ->
                log.writeStatus(status, "searchGeofences: ${Utils.stringForRadarStatus(status)}", "geofences = ${geofences?.size ?: 0}\nfirst = ${geofences?.firstOrNull()?.description}")
            }
        }
        ActionButton("geocode") {
            Radar.geocode("20 jay street brooklyn") { status, addresses ->
                log.writeStatus(status, "geocode: ${Utils.stringForRadarStatus(status)}", addresses?.firstOrNull()?.formattedAddress)
            }
        }
        ActionButton("reverseGeocode") {
            val location = Location("example").apply { latitude = 40.70390; longitude = -73.98670 }
            Radar.reverseGeocode(location) { status, addresses ->
                log.writeStatus(status, "reverseGeocode: ${Utils.stringForRadarStatus(status)}", addresses?.firstOrNull()?.formattedAddress)
            }
        }
        ActionButton("ipGeocode") {
            Radar.ipGeocode { status, address, proxy, throwable ->
                log.writeStatus(status, "ipGeocode: ${Utils.stringForRadarStatus(status)}", "country = ${address?.countryCode}\ncity = ${address?.city}\nproxy = $proxy\nthrowable = ${throwable?.message}")
            }
        }
        ActionButton("validateAddress") {
            val address = RadarAddress(
                coordinate = RadarCoordinate(.0, .0),
                city = "New York",
                stateCode = "NY",
                postalCode = "10003",
                countryCode = "US",
                street = "Broadway",
                number = "841",
            )
            Radar.validateAddress(address) { status, validated, verificationStatus ->
                log.writeStatus(
                    status,
                    "validateAddress: ${Utils.stringForRadarStatus(status)}",
                    "address = ${validated?.formattedAddress}\nverification = ${Radar.stringForVerificationStatus(verificationStatus)}",
                )
            }
        }
        ActionButton("autocomplete") {
            Radar.autocomplete("brooklyn", origin, arrayOf("locality"), 10, "US", true) { status, addresses ->
                log.writeStatus(status, "autocomplete: ${Utils.stringForRadarStatus(status)}", "results = ${addresses?.size ?: 0}\nfirst = ${addresses?.firstOrNull()?.formattedAddress}")
            }
        }
        ActionButton("getDistance") {
            Radar.getDistance(
                origin,
                destination,
                EnumSet.of(Radar.RadarRouteMode.FOOT, Radar.RadarRouteMode.CAR),
                Radar.RadarRouteUnits.IMPERIAL,
            ) { status, routes ->
                log.writeStatus(status, "getDistance: ${Utils.stringForRadarStatus(status)}", "car = ${routes?.car?.distance?.text} / ${routes?.car?.duration?.text}")
            }
        }
        ActionButton("getMatrix") {
            val origins = arrayOf(
                Location("example").apply { latitude = 40.78382; longitude = -73.97536 },
                Location("example").apply { latitude = 40.70390; longitude = -73.98670 },
            )
            val destinations = arrayOf(
                Location("example").apply { latitude = 40.64189; longitude = -73.78779 },
                Location("example").apply { latitude = 35.99801; longitude = -78.94294 },
            )
            Radar.getMatrix(origins, destinations, Radar.RadarRouteMode.CAR, Radar.RadarRouteUnits.IMPERIAL) { status, matrix ->
                log.writeStatus(status, "getMatrix: ${Utils.stringForRadarStatus(status)}", "matrix[0][0] = ${matrix?.routeBetween(0, 0)?.duration?.text}")
            }
        }
    }
}
