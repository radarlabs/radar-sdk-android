@file:Suppress("TooManyFunctions")
package io.radar.sdk.ktx

import android.location.Location
import io.radar.sdk.Radar
import io.radar.sdk.RadarTrackingOptions
import io.radar.sdk.RadarTripOptions
import io.radar.sdk.model.RadarAddress
import io.radar.sdk.model.RadarContext
import io.radar.sdk.model.RadarEvent
import io.radar.sdk.model.RadarGeofence
import io.radar.sdk.model.RadarPlace
import io.radar.sdk.model.RadarRouteMatrix
import io.radar.sdk.model.RadarRoutes
import io.radar.sdk.model.RadarTrip
import io.radar.sdk.model.RadarUser
import org.json.JSONObject
import java.util.*
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * Created when a location request succeeds, fails, or times out. Receives the request status and, if successful,
 * the location.
 *
 * @param[status] The request status.
 * @param[location] If successful, the location.
 * @param[stopped] Indicates whether the device is stopped.
 */
data class RadarLocationResponse(
    val status: Radar.RadarStatus,
    val location: Location? = null,
    val stopped: Boolean = false
)

/**
 * Created when a track request succeeds, fails, or times out. Receives the request status and, if successful,
 * the user's location, an array of the events generated, and the user.
 *
 * @param[status] The request status.
 * @param[location] If successful, the user's location.
 * @param[events] If successful, a list of the events generated.
 * @param[user] If successful, the user.
 */
data class RadarTrackResponse(
    val status: Radar.RadarStatus,
    val location: Location? = null,
    val events: List<RadarEvent>? = null,
    val user: RadarUser? = null
)

/**
 * Created when a trip update succeeds, fails, or times out. Receives the request status and, if successful, the
 * trip and an array of the events generated.
 *
 * @param[status] The request status.
 * @param[trip] If successful, the trip.
 * @param[events] If successful, a list of the events generated.
 */
data class RadarTripResponse(
    val status: Radar.RadarStatus,
    val trip: RadarTrip? = null,
    val events: List<RadarEvent>? = null
)

/**
 * Created when a place search request succeeds, fails, or times out. Receives the request status and, if
 * successful, the location and an array of places sorted by distance.
 *
 * @param[status] The request status.
 * @param[location] If successful, the location.
 * @param[places] If successful, a list of places sorted by distance.
 */
data class RadarSearchPlacesResponse(
    val status: Radar.RadarStatus,
    val location: Location? = null,
    val places: List<RadarPlace>? = null
)

/**
 * Created when a geofence search request succeeds, fails, or times out. Receives the request status and, if
 * successful, the location and an array of geofences sorted by distance.
 *
 * @param[status] The request status.
 * @param[location] If successful, the location.
 * @param[geofences] If successful, a list of geofences sorted by distance.
 */
data class RadarSearchGeofenceResponse(
    val status: Radar.RadarStatus,
    val location: Location? = null,
    val geofences: List<RadarGeofence>? = null
)

/**
 * Created when a geocoding request succeeds, fails, or times out. Receives the request status and, if
 * successful, the geocoding results (an array of addresses).
 *
 * @param[status] The request status.
 * @param[addresses] If successful, the geocoding results (a list of addresses).
 */
data class RadarGeocodeResponse(
    val status: Radar.RadarStatus,
    val addresses: List<RadarAddress>? = null
)

/**
 * Created when an IP geocoding request succeeds, fails, or times out. Receives the request status and, if
 * successful, the geocoding result (a partial address) and a boolean indicating whether the IP address is a
 * known proxy.
 *
 * @param[status] The request status.
 * @param[address] If successful, the geocoding result (a partial address).
 * @param[proxy] Indicates whether the IP address is a known proxy.
 */
data class RadarIpGeocodeResponse(
    val status: Radar.RadarStatus,
    val address: RadarAddress? = null,
    val proxy: Boolean = false
)

/**
 * Created when a distance request succeeds, fails, or times out. Receives the request status and, if successful,
 * the routes.
 *
 * @param[status] The request status.
 * @param[routes] If successful, the routes.
 */
data class RadarRouteResponse(
    val status: Radar.RadarStatus,
    val routes: RadarRoutes? = null
)

/**
 * Created when a matrix request succeeds, fails, or times out. Receives the request status and, if successful,
 * the matrix.
 *
 * @param[status] The request status.
 * @param[matrix] If successful, the matrix.
 */
data class RadarMatrixResponse(
    val status: Radar.RadarStatus,
    val matrix: RadarRouteMatrix? = null
)

/**
 * Created when a context request succeeds, fails, or times out. Receives the request status and, if successful,
 * the location and the context.
 *
 * @param[status] The request status.
 * @param[location] If successful, the location.
 * @param[context] If successful, the context.
 */
data class RadarContextResponse(
    val status: Radar.RadarStatus,
    val location: Location? = null,
    val context: RadarContext? = null
)

/**
 * Gets the device's current location.
 *
 * @see [Radar Android SDK docs](https://radar.io/documentation/sdk/android#get-location)
 *
 * @param[desiredAccuracy] The desired accuracy.
 *
 * @return location response
 */
suspend fun Radar.getCurrentLocation(
    desiredAccuracy: RadarTrackingOptions.RadarTrackingOptionsDesiredAccuracy? = null
): RadarLocationResponse {
    return suspendCoroutine { continuation ->
        val callback = object : Radar.RadarLocationCallback {
            override fun onComplete(
                status: Radar.RadarStatus,
                location: Location?,
                stopped: Boolean
            ) {
                continuation.resume(RadarLocationResponse(status, location, stopped))
            }
        }
        if (desiredAccuracy == null) {
            getLocation(callback)
        } else {
            getLocation(desiredAccuracy, callback)
        }
    }
}

/**
 * Tracks the user's location once in the foreground.
 *
 * @see [Radar Android SDK docs](https://radar.io/documentation/sdk/android#foreground-tracking)
 *
 * @return tracking response
 */
suspend fun Radar.trackCurrentLocation(): RadarTrackResponse {
    return suspendCoroutine { continuation ->
        trackOnce(object : Radar.RadarTrackCallback {
            override fun onComplete(
                status: Radar.RadarStatus,
                location: Location?,
                events: Array<RadarEvent>?,
                user: RadarUser?
            ) {
                continuation.resume(RadarTrackResponse(status, location, events?.toList(), user))
            }
        })
    }
}

/**
 * Tracks the user's location once with the desired accuracy and optionally ranges beacons in the foreground.
 *
 * @see [Radar Android SDK docs](https://radar.io/documentation/sdk/android#foreground-tracking)
 *
 * @param[desiredAccuracy] The desired accuracy.
 * @param[beacons] A boolean indicating whether to range beacons.
 *
 * @return tracking response
 */
suspend fun Radar.trackCurrentLocation(
    desiredAccuracy: RadarTrackingOptions.RadarTrackingOptionsDesiredAccuracy,
    beacons: Boolean
): RadarTrackResponse {
    return suspendCoroutine { continuation ->
        trackOnce(desiredAccuracy, beacons, object : Radar.RadarTrackCallback {
            override fun onComplete(
                status: Radar.RadarStatus,
                location: Location?,
                events: Array<RadarEvent>?,
                user: RadarUser?
            ) {
                continuation.resume(RadarTrackResponse(status, location, events?.toList(), user))
            }
        })
    }
}

suspend fun Radar.trackLocation(): RadarTrackResponse {
    return suspendCoroutine { continuation ->
        trackOnce(object : Radar.RadarTrackCallback {
            override fun onComplete(
                status: Radar.RadarStatus,
                location: Location?,
                events: Array<RadarEvent>?,
                user: RadarUser?
            ) {
                continuation.resume(RadarTrackResponse(status, location, events?.toList(), user))
            }
        })
    }
}

/**
 * Tracks the user's location once with the desired accuracy and optionally ranges beacons in the foreground.
 *
 * @see [](https://radar.io/documentation/sdk/android#foreground-tracking)
 *
 * @param[desiredAccuracy] The desired accuracy.
 * @param[beacons] A boolean indicating whether to range beacons.
 * @return tracking response
 */
suspend fun Radar.trackLocation(
    desiredAccuracy: RadarTrackingOptions.RadarTrackingOptionsDesiredAccuracy,
    beacons: Boolean
): RadarTrackResponse {
    return suspendCoroutine { continuation ->
        trackOnce(desiredAccuracy, beacons, object : Radar.RadarTrackCallback {
            override fun onComplete(
                status: Radar.RadarStatus,
                location: Location?,
                events: Array<RadarEvent>?,
                user: RadarUser?
            ) {
                continuation.resume(RadarTrackResponse(status, location, events?.toList(), user))
            }
        })
    }
}

/**
 * Manually updates the user's location. Note that these calls are subject to rate limits.
 *
 * @see [Radar Android SDK docs](https://radar.io/documentation/sdk/android#foreground-tracking)
 *
 * @param[location] A location for the user.
 *
 * @return tracking response
 */
suspend fun Radar.trackLocation(location: Location): RadarTrackResponse {
    return suspendCoroutine { continuation ->
        trackOnce(location, object : Radar.RadarTrackCallback {
            override fun onComplete(
                status: Radar.RadarStatus,
                location: Location?,
                events: Array<RadarEvent>?,
                user: RadarUser?
            ) {
                continuation.resume(RadarTrackResponse(status, location, events?.toList(), user))
            }
        })
    }
}

/**
 * Starts a trip.
 *
 * @see [Radar Trip Tracking API](https://radar.io/documentation/trip-tracking)
 *
 * @param[options] Configurable trip options.
 *
 * @return trip response data
 */
suspend fun Radar.startTripTracking(options: RadarTripOptions): RadarTripResponse {
    return suspendCoroutine { continuation ->
        startTrip(options, object : Radar.RadarTripCallback {
            override fun onComplete(status: Radar.RadarStatus, trip: RadarTrip?, events: Array<RadarEvent>?) {
                continuation.resume(RadarTripResponse(status, trip, events?.toList()))
            }
        })
    }
}

/**
 * Manually updates a trip.
 *
 * @see [Radar Trip Tracking API](https://radar.io/documentation/trip-tracking)
 *
 * @param[options] Configurable trip options.
 * @param[status] The trip status. To avoid updating status, pass UNKNOWN.
 *
 * @return trip response data
 */
suspend fun Radar.updateTripTracking(options: RadarTripOptions, status: RadarTrip.RadarTripStatus?): RadarTripResponse {
    return suspendCoroutine { continuation ->
        updateTrip(options, status, object : Radar.RadarTripCallback {
            override fun onComplete(status: Radar.RadarStatus, trip: RadarTrip?, events: Array<RadarEvent>?) {
                continuation.resume(RadarTripResponse(status, trip, events?.toList()))
            }
        })
    }
}

/**
 * Completes a trip.
 *
 * @see [Radar Trip Tracking API](https://radar.io/documentation/trip-tracking)
 */
suspend fun Radar.completeTripTracking(): RadarTripResponse {
    return suspendCoroutine { continuation ->
        completeTrip(object : Radar.RadarTripCallback {
            override fun onComplete(status: Radar.RadarStatus, trip: RadarTrip?, events: Array<RadarEvent>?) {
                continuation.resume(RadarTripResponse(status, trip, events?.toList()))
            }
        })
    }
}

/**
 * Cancels a trip.
 *
 * @see [Radar Trip Tracking API](https://radar.io/documentation/trip-tracking)
 */
suspend fun Radar.cancelTripTracking(): RadarTripResponse {
    return suspendCoroutine { continuation ->
        cancelTrip(object : Radar.RadarTripCallback {
            override fun onComplete(status: Radar.RadarStatus, trip: RadarTrip?, events: Array<RadarEvent>?) {
                continuation.resume(RadarTripResponse(status, trip, events?.toList()))
            }
        })
    }
}

/**
 * Gets the device's current location, then searches for places near that location, sorted by distance.
 *
 * @see [Radar Places API](https://radar.io/documentation/api#search-places)
 *
 * @param[radius] The radius to search, in meters. A number between 100 and 10000.
 * @param[chains] An array of chain slugs to filter. See [](https://radar.io/documentation/places/chains)
 * @param[categories] An array of categories to filter. See [](https://radar.io/documentation/places/categories)
 * @param[groups] An array of groups to filter. See [](https://radar.io/documentation/places/groups)
 * @param[limit] The max number of places to return. A number between 1 and 100.
 *
 * @return search response data
 */
suspend fun Radar.getPlaces(
    radius: Int,
    chains: Array<String>?,
    categories: Array<String>?,
    groups: Array<String>?,
    limit: Int?
): RadarSearchPlacesResponse {
    return suspendCoroutine { continuation ->
        searchPlaces(radius, chains, categories, groups, limit, object : Radar.RadarSearchPlacesCallback {
            override fun onComplete(status: Radar.RadarStatus, location: Location?, places: Array<RadarPlace>?) {
                continuation.resume(RadarSearchPlacesResponse(status, location, places?.toList()))
            }
        })
    }
}

/**
 * Search for places near a location, sorted by distance.
 *
 * @see [Radar Places API](https://radar.io/documentation/api#search-places)
 *
 * @param[near] The location to search.
 * @param[radius] The radius to search, in meters. A number between 100 and 10000.
 * @param[chains] An array of chain slugs to filter. See [](https://radar.io/documentation/places/chains)
 * @param[categories] An array of categories to filter. See [](https://radar.io/documentation/places/categories)
 * @param[groups] An array of groups to filter. See [](https://radar.io/documentation/places/groups)
 * @param[limit] The max number of places to return. A number between 1 and 100.
 *
 * @return search response data
 */
@Suppress("LongParameterList")
suspend fun Radar.getPlaces(
    near: Location,
    radius: Int,
    chains: Array<String>?,
    categories: Array<String>?,
    groups: Array<String>?,
    limit: Int?
): RadarSearchPlacesResponse {
    return suspendCoroutine { continuation ->
        searchPlaces(near, radius, chains, categories, groups, limit, object : Radar.RadarSearchPlacesCallback {
            override fun onComplete(status: Radar.RadarStatus, location: Location?, places: Array<RadarPlace>?) {
                continuation.resume(RadarSearchPlacesResponse(status, location, places?.toList()))
            }
        })
    }
}

/**
 * Gets the device's current location, then searches for geofences near that location, sorted by distance.
 *
 * @see [Radar Geofences API](https://radar.io/documentation/api#search-geofences)
 *
 * @param[radius] The radius to search, in meters. A number between 100 and 10000.
 * @param[tags] An array of tags to filter. See [](https://radar.io/documentation/geofences)
 * @param[metadata] A dictionary of metadata to filter. See [](https://radar.io/documentation/geofences)
 * @param[limit] The max number of places to return. A number between 1 and 100.
 *
 * @return search response data
 */
suspend fun Radar.getGeofences(
    radius: Int,
    tags: Array<String>?,
    metadata: JSONObject?,
    limit: Int?
): RadarSearchGeofenceResponse {
    return suspendCoroutine { continuation ->
        searchGeofences(radius, tags, metadata, limit, object : Radar.RadarSearchGeofencesCallback {
            override fun onComplete(status: Radar.RadarStatus, location: Location?, geofences: Array<RadarGeofence>?) {
                continuation.resume(RadarSearchGeofenceResponse(status, location, geofences?.toList()))
            }
        })
    }
}

/**
 * Search for geofences near a location, sorted by distance.
 *
 * @see [Radar Geofences API](https://radar.io/documentation/api#search-geofences)
 *
 * @param[near] The location to search.
 * @param[radius] The radius to search, in meters. A number between 100 and 10000.
 * @param[tags] An array of tags to filter. See [](https://radar.io/documentation/geofences)
 * @param[metadata] A dictionary of metadata to filter. See [](https://radar.io/documentation/geofences)
 * @param[limit] The max number of places to return. A number between 1 and 100.
 *
 * @return search response data
 */
suspend fun Radar.getGeofences(
    near: Location,
    radius: Int,
    tags: Array<String>?,
    metadata: JSONObject?,
    limit: Int?
): RadarSearchGeofenceResponse {
    return suspendCoroutine { continuation ->
        searchGeofences(near, radius, tags, metadata, limit, object : Radar.RadarSearchGeofencesCallback {
            override fun onComplete(status: Radar.RadarStatus, location: Location?, geofences: Array<RadarGeofence>?) {
                continuation.resume(RadarSearchGeofenceResponse(status, location, geofences?.toList()))
            }
        })
    }
}

/**
 * Autocompletes partial addresses and place names, sorted by relevance.
 *
 * @see [Radar Autocomplete API](https://radar.io/documentation/api#autocomplete)
 *
 * @param[query] The partial address or place name to autocomplete.
 * @param[near] A location for the search.
 * @param[limit] The max number of addresses to return. A number between 1 and 100.
 *
 * @return geocode response data
 */
suspend fun Radar.getAutoComplete(
    query: String,
    near: Location? = null,
    limit: Int? = null,
): RadarGeocodeResponse {
    return suspendCoroutine { continuation ->
        autocomplete(query, near, limit, object : Radar.RadarGeocodeCallback {
            override fun onComplete(status: Radar.RadarStatus, addresses: Array<RadarAddress>?) {
                continuation.resume(RadarGeocodeResponse(status, addresses?.toList()))
            }
        })
    }
}

/**
 * Autocompletes partial addresses and place names, sorted by relevance.
 *
 * @see [Radar Autocomplete API](https://radar.io/documentation/api#autocomplete)
 *
 * @param[query] The partial address or place name to autocomplete.
 * @param[near] A location for the search.
 * @param[layers] Optional layer filters.
 * @param[limit] The max number of addresses to return. A number between 1 and 100.
 * @param[country] An optional country filter. A string, the unique 2-letter country code.
 *
 * @return geocode response data
 */
suspend fun Radar.getAutoComplete(
    query: String,
    near: Location? = null,
    layers: Array<String>? = null,
    limit: Int? = null,
    country: String? = null
): RadarGeocodeResponse {
    return suspendCoroutine { continuation ->
        autocomplete(query, near, layers, limit, country, object : Radar.RadarGeocodeCallback {
            override fun onComplete(status: Radar.RadarStatus, addresses: Array<RadarAddress>?) {
                continuation.resume(RadarGeocodeResponse(status, addresses?.toList()))
            }
        })
    }
}

/**
 * Geocodes an address, converting address to coordinates.
 *
 * @see [Radar Forward Geocode API](https://radar.io/documentation/api#forward-geocode)
 *
 * @param[query] The address to geocode.
 *
 * @return geocode response data
 */
suspend fun Radar.getGeocode(query: String): RadarGeocodeResponse {
    return suspendCoroutine { continuation ->
        geocode(query, object : Radar.RadarGeocodeCallback {
            override fun onComplete(status: Radar.RadarStatus, addresses: Array<RadarAddress>?) {
                continuation.resume(RadarGeocodeResponse(status, addresses?.toList()))
            }
        })
    }
}

/**
 * Reverse geocodes a location, converting coordinates to address.
 *
 * @see [Radar Reverse Geocode API](https://radar.io/documentation/api#reverse-geocode)
 *
 * @param[location] The location to reverse geocode. If null, the device's current location will be used
 *
 * @return geocode response data
 */
suspend fun Radar.getReverseGeocode(location: Location? = null): RadarGeocodeResponse {
    return suspendCoroutine { continuation ->
        val callback = object : Radar.RadarGeocodeCallback {
            override fun onComplete(status: Radar.RadarStatus, addresses: Array<RadarAddress>?) {
                continuation.resume(RadarGeocodeResponse(status, addresses?.toList()))
            }
        }
        if (location == null) {
            reverseGeocode(callback)
        } else {
            reverseGeocode(location, callback)
        }
    }
}

/**
 * Geocodes the device's current IP address, converting IP address to partial address.
 *
 * @see [Radar IP Geocode API](https://radar.io/documentation/api#ip-geocode)
 *
 * @return IP Geocode response data
 */
suspend fun Radar.getIpGeocode(): RadarIpGeocodeResponse {
    return suspendCoroutine { continuation ->
        ipGeocode(object : Radar.RadarIpGeocodeCallback {
            override fun onComplete(status: Radar.RadarStatus, address: RadarAddress?, proxy: Boolean) {
                continuation.resume(RadarIpGeocodeResponse(status, address, proxy))
            }
        })
    }
}

/**
 * Gets the device's current location, then calculates the travel distance and duration to a destination.
 *
 * @see [Radar Distance API](https://radar.io/documentation/api#distance)
 *
 * @param[destination] The destination.
 * @param[modes] The travel modes.
 * @param[units] The distance units.
 *
 * @return route response data
 */
suspend fun Radar.getDistanceTo(
    destination: Location,
    modes: EnumSet<Radar.RadarRouteMode>,
    units: Radar.RadarRouteUnits): RadarRouteResponse {
    return suspendCoroutine { continuation ->
        getDistance(destination, modes, units, object : Radar.RadarRouteCallback {
            override fun onComplete(status: Radar.RadarStatus, routes: RadarRoutes?) {
                continuation.resume(RadarRouteResponse(status, routes))
            }
        })
    }
}

/**
 * Calculates the travel distance and duration from an origin to a destination.
 *
 * @see [Radar Distance API](https://radar.io/documentation/api#distance)
 *
 * @param[origin] The origin.
 * @param[destination] The destination.
 * @param[modes] The travel modes.
 * @param[units] The distance units.
 *
 * @return route response data
 */
suspend fun Radar.getDistanceBetween(
    origin: Location,
    destination: Location,
    modes: EnumSet<Radar.RadarRouteMode>,
    units: Radar.RadarRouteUnits): RadarRouteResponse {
    return suspendCoroutine { continuation ->
        getDistance(origin, destination, modes, units, object : Radar.RadarRouteCallback {
            override fun onComplete(status: Radar.RadarStatus, routes: RadarRoutes?) {
                continuation.resume(RadarRouteResponse(status, routes))
            }
        })
    }
}

/**
 * Calculates the travel distances and durations between multiple origins and destinations for up to 25 routes.
 *
 * @see [Radar Matrix API](https://radar.io/documentation/api#matrix)
 *
 * @param[origins] The origins.
 * @param[destinations] The destinations.
 * @param[mode] The travel mode.
 * @param[units] The distance units.
 *
 * @return matrix response data
 */
suspend fun Radar.getMatrixFor(
    origins: Array<Location>,
    destinations: Array<Location>,
    mode: Radar.RadarRouteMode,
    units: Radar.RadarRouteUnits): RadarMatrixResponse {
    return suspendCoroutine { continuation ->
        getMatrix(origins, destinations, mode, units, object : Radar.RadarMatrixCallback {
            override fun onComplete(status: Radar.RadarStatus, matrix: RadarRouteMatrix?) {
                continuation.resume(RadarMatrixResponse(status, matrix))
            }
        })
    }
}

/**
 * Gets context for a location without sending device or user identifiers to the server.
 *
 * @see [Radar Context API](https://radar.io/documentation/api#context)
 *
 * @param[location] The location. If null, the device's current location will be sent
 *
 * @return context response data
 */
suspend fun Radar.getLocationContext(location: Location? = null): RadarContextResponse {
    return suspendCoroutine { continuation ->
        val callback = object : Radar.RadarContextCallback {
            override fun onComplete(status: Radar.RadarStatus, location: Location?, context: RadarContext?) {
                continuation.resume(RadarContextResponse(status, location, context))
            }
        }
        if (location == null) {
            getContext(callback)
        } else {
            getContext(location, callback)
        }
    }
}