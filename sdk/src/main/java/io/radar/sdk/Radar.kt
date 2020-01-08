package io.radar.sdk

import android.annotation.SuppressLint
import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.location.Location
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import io.radar.sdk.model.RadarAddress
import io.radar.sdk.model.RadarEvent
import io.radar.sdk.model.RadarEvent.RadarEventVerification
import io.radar.sdk.model.RadarGeofence
import io.radar.sdk.model.RadarPlace
import io.radar.sdk.model.RadarRegion
import io.radar.sdk.model.RadarUser
import org.json.JSONObject

/**
 * The main class used to interact with the Radar SDK.
 *
 * @see [](https://radar.io/documentation/sdk)
 */
@SuppressLint("StaticFieldLeak")
object Radar {

    /**
     * Called when a location request succeeds, fails, or times out.
     */
    interface RadarLocationCallback {

        /**
         * Called when a location request succeeds, fails, or times out. Receives the request status and, if successful, the location.
         *
         * @param[status] RadarStatus The request status.
         * @param[location] Location? If successful, the location.
         * @param[stopped] Boolean A boolean indicating whether the device is stopped.
         */
        fun onComplete(
            status: RadarStatus,
            location: Location? = null,
            stopped: Boolean = false
        )

    }

    /**
     * Called when a track request succeeds, fails, or times out.
     */
    interface RadarTrackCallback {

        /**
         * Called when a track request succeeds, fails, or times out. Receives the request status and, if successful, the user's location, an array of the events generated, and the user.
         *
         * @param[status] RadarStatus The request status.
         * @param[location] Location? If successful, the user's location.
         * @param[events] Array<RadarEvent>? If successful, an array of the events generated.
         * @param[user] RadarUser? If successful, the user.
         */
        fun onComplete(
            status: RadarStatus,
            location: Location? = null,
            events: Array<RadarEvent>? = null,
            user: RadarUser? = null
        )

    }

    /**
     * Called when a place search request succeeds, fails, or times out.
     */
    interface RadarSearchPlacesCallback {
        /**
         * Called when a place search request succeeds, fails, or times out. Receives the request status and, if successful, the location and an array of places sorted by distance.
         *
         * @param[status] RadarStatus The request status.
         * @param[location] Location? If successful, the location.
         * @param[places] Array<RadarPlace>? If successful, an array of places sorted by distance.
         */
        fun onComplete(
            status: RadarStatus,
            location: Location? = null,
            places: Array<RadarPlace>? = null
        )
    }

    /**
     * Called when a geofence search request succeeds, fails, or times out.
     */
    interface RadarSearchGeofencesCallback {
        /**
         * Called when a geofence search request succeeds, fails, or times out. Receives the request status and, if successful, the location and an array of geofences sorted by distance.
         *
         * @param[status] RadarStatus The request status.
         * @param[location] Location? If successful, the location.
         * @param[geofences] Array<RadarGeofence>? If successful, an array of geofences sorted by distance.
         */
        fun onComplete(
            status: RadarStatus,
            location: Location? = null,
            geofences: Array<RadarGeofence>? = null
        )
    }

    /**
     * Called when a geocoding request succeeds, fails, or times out.
     */
    interface RadarGeocodeCallback {
        /**
         * Called when a geocoding request succeeds, fails, or times out. Receives the request status and, if successful, the raw response and an array of addresses.
         *
         * @param[status] RadarStatus The request status.
         * @param[addresses] Array<RadarAddress>? If successsful, an array of geocoded addresses.
         */
        fun onComplete(
            status: RadarStatus,
            addresses: Array<RadarAddress>? = null
        )
    }

    /**
     * Called when an IP geocoding request succeeds, fails, or times out.
     */
    interface RadarIpGeocodeCallback {
        /**
         * Called when an IP geocoding request succeeds, fails, or times out. Receives the request status and, if successful, the raw response and an array of regions.
         *
         * @param[status] RadarStatus The request status.
         * @param[country] RadarRegion? If successsful, the region of the IP address.
         */
        fun onComplete(
            status: RadarStatus,
            country: RadarRegion? = null
        )
    }

    /**
     * The status types for a request. See [](https://radar.io/documentation/sdk#android-foreground).
     */
    enum class RadarStatus {
        /** The request succeeded */
        SUCCESS,
        /** The SDK was not initialized with a publishable API key */
        ERROR_PUBLISHABLE_KEY,
        /** Location permissions have not been granted */
        ERROR_PERMISSIONS,
        /** The user has not granted location permissions for the app */
        ERROR_LOCATION,
        /** The network was unavailable, or the network connection timed out */
        ERROR_NETWORK,
        /** The publishable API key is invalid */
        ERROR_UNAUTHORIZED,
        /** An internal server error occurred */
        ERROR_SERVER,
        /** Exceeded rate limit */
        ERROR_RATE_LIMIT,
        /** An unknown error occurred */
        ERROR_UNKNOWN
    }

    /**
     * The sources for location updates.
     */
    enum class RadarLocationSource {
        /** Foreground */
        FOREGROUND_LOCATION,
        /** Background */
        BACKGROUND_LOCATION,
        /** Manual */
        MANUAL_LOCATION,
        /** Geofence enter */
        GEOFENCE_ENTER,
        /** Geofence dwell */
        GEOFENCE_DWELL,
        /** Geofence exit */
        GEOFENCE_EXIT,
        /** Unknown */
        UNKNOWN
    }

    /**
     * The levels for debug logs.
     */
    enum class RadarLogLevel(val value: Int) {
        /** None */
        NONE(0),
        /** Error */
        ERROR(1),
        /** Warning */
        WARNING(2),
        /** Info */
        INFO(3),
        /** Debug */
        DEBUG(4);


        companion object {
            fun fromInt(value: Int) : RadarLogLevel {
                return values().first { it.value == value }
            }
        }
    }

    private var initialized = false
    private lateinit var context: Context
    private lateinit var logger: RadarLogger
    internal lateinit var apiClient: RadarApiClient
    internal lateinit var locationManager: RadarLocationManager

    /**
     * Initializes the Radar SDK. Call this method from the main thread in your `Application` class before calling any other Radar methods. See [](https://radar.io/documentation/sdk#android-initialize).
     *
     * @param[context] The context
     * @param[publishableKey] Your publishable API key
     */
    fun initialize(context: Context?, publishableKey: String? = null) {
        if (context == null) {
            return
        }

        initialized = true
        this.context = context.applicationContext

        if (publishableKey != null) {
            RadarSettings.setPublishableKey(this.context, publishableKey)
        }

        if (!this::logger.isInitialized) {
            this.logger = RadarLogger()
        }
        if (!this::apiClient.isInitialized) {
            this.apiClient = RadarApiClient(this.context)
        }
        if (!this::locationManager.isInitialized) {
            this.locationManager = RadarLocationManager(this.context, apiClient, logger)
        }

        this.logger.d(this.context, "Initializing")

        RadarUtils.loadAdId(this.context)

        val application = this.context as? Application
        application?.registerActivityLifecycleCallbacks(RadarActivityLifecycleCallbacks())

        this.apiClient.getConfig()
    }

    /**
     * Identifies the user. Until you identify the user, Radar will automatically identify the user by `deviceId` (Android ID). See [](https://radar.io/documentation/sdk#android-identify).
     *
     * @param[userId] A stable unique ID for the user. If null, the previous `userId` will be cleared.
     */
    fun setUserId(userId: String?) {
        if (!initialized) {
            return
        }

        RadarSettings.setUserId(context, userId)
    }

    /**
     * Returns the current `userId`.
     *
     * @return The current `userId`.
     */
    fun getUserId(): String? {
        if (!initialized) {
            return null
        }

        return RadarSettings.getUserId(context)
    }

    /**
     * Sets an optional description for the user, displayed in the dashboard.
     *
     * @param[description] A description for the user. If null, the previous `description` will be cleared.
     */
    fun setDescription(description: String?) {
        if (!initialized) {
            return
        }

        RadarSettings.setDescription(context, description)
    }

    /**
     * Returns the current `description`.
     *
     * @return The current `description`.
     */
    fun getDescription(): String? {
        if (!initialized) {
            return null
        }

        return RadarSettings.getDescription(context)
    }

    /**
     * Sets an optional set of custom key-value pairs for the user.
     *
     * @param[metadata] A set of custom key-value pairs for the user. Must have 16 or fewer keys and values of type string, boolean, or number. If `null`, the previous `metadata` will be cleared.
     */
    fun setMetadata(metadata: JSONObject?) {
        if (!initialized) {
            return
        }

        RadarSettings.setMetadata(context, metadata)
    }

    /**
     * Returns the current `metadata`.
     *
     * @return The current `metadata`.
     */
    fun getMetadata(): JSONObject? {
        if (!initialized) {
            return null
        }

        return RadarSettings.getMetadata(context)
    }

    /**
     * Enables `adId` (Android advertising ID) collection.
     *
     * @param[enabled] A boolean indicating whether `adId` should be collected.
     */
    fun setAdIdEnabled(enabled: Boolean) {
        RadarSettings.setAdIdEnabled(context, enabled)
    }

    /**
     * Gets the device's current location.
     *
     * @param[callback] An optional callback.
     */
    fun getLocation(callback: RadarLocationCallback? = null) {
        if (!initialized) {
            callback?.onComplete(RadarStatus.ERROR_PUBLISHABLE_KEY)

            return
        }

        locationManager.getLocation(callback)
    }

    /**
     * Gets the device's current location.
     *
     * @param[block] A block callback.
     */
    fun getLocation(block: (status: RadarStatus, location: Location?, stopped: Boolean) -> Unit) {
        getLocation(object : RadarLocationCallback {
            override fun onComplete(status: RadarStatus, location: Location?, stopped: Boolean) {
                block(status, location, stopped)
            }
        })
    }

    /**
     * Tracks the user's location once in the foreground.
     *
     * @param[callback] An optional callback.
     */
    fun trackOnce(callback: RadarTrackCallback? = null) {
        if (!initialized) {
            callback?.onComplete(RadarStatus.ERROR_PUBLISHABLE_KEY)

            return
        }

        locationManager.getLocation(object : RadarLocationCallback {
            override fun onComplete(status: RadarStatus, location: Location?, stopped: Boolean) {
                if (status != RadarStatus.SUCCESS || location == null) {
                    callback?.onComplete(status)

                    return
                }

                apiClient.track(location, stopped, RadarLocationSource.FOREGROUND_LOCATION, false, object : RadarApiClient.RadarTrackApiCallback {
                    override fun onComplete(status: RadarStatus, res: JSONObject?, events: Array<RadarEvent>?, user: RadarUser?) {
                        callback?.onComplete(status, location, events, user)
                    }
                })
            }
        })
    }

    /**
     * Tracks the user's location once in the foreground.
     *
     * @param[block] A block callback.
     */
    fun trackOnce(block: (status: RadarStatus, location: Location?, events: Array<RadarEvent>?, user: RadarUser?) -> Unit) {
        trackOnce(object : RadarTrackCallback {
            override fun onComplete(status: RadarStatus, location: Location?, events: Array<RadarEvent>?, user: RadarUser?) {
                block(status, location, events, user)
            }
        })
    }

    /**
     * Manually updates the user's location. Note that these calls are subject to rate limits. See [](https://radar.io/documentation/sdk#android-manual).
     *
     * @param[location] A location for the user.
     * @param[callback] An optional callback.
     */
    fun trackOnce(location: Location, callback: RadarTrackCallback?) {
        if (!initialized) {
            callback?.onComplete(RadarStatus.ERROR_PUBLISHABLE_KEY)

            return
        }

        apiClient.track(location, false, RadarLocationSource.MANUAL_LOCATION, false, object : RadarApiClient.RadarTrackApiCallback {
            override fun onComplete(status: RadarStatus, res: JSONObject?, events: Array<RadarEvent>?, user: RadarUser?) {
                callback?.onComplete(status, location, events, user)
            }
        })
    }

    /**
     * Manually updates the user's location. Note that these calls are subject to rate limits. See [](https://radar.io/documentation/sdk#android-manual).
     *
     * @param[location] A location for the user.
     * @param[block] An a block callback.
     */
    fun trackOnce(location: Location, block: (status: RadarStatus, location: Location?, events: Array<RadarEvent>?, user: RadarUser?) -> Unit) {
        trackOnce(location, object : RadarTrackCallback {
            override fun onComplete(status: RadarStatus, location: Location?, events: Array<RadarEvent>?, user: RadarUser?) {
                block(status, location, events, user)
            }
        })
    }

    /**
     * Starts tracking the user's location in the background. Before calling this method, the user should have granted backgroundlocation permissions for the app. See [](https://radar.io/documentation/sdk#android-background).
     *
     * @param[options] Configurable tracking options.
     */
    fun startTracking(options: RadarTrackingOptions = RadarTrackingOptions.EFFICIENT) {
        if (!initialized) {
            return
        }

        locationManager.startTracking(options)
    }

    /**
     * Stops tracking the user's location in the background. See [](https://radar.io/documentation/sdk#android-background).
     */
    fun stopTracking() {
        if (!initialized) {
            return
        }

        locationManager.stopTracking()
    }

    /**
     * Returns a boolean indicating whether tracking has been started.
     *
     * @return A boolean indicating whether tracking has been started.
     */
    fun isTracking(): Boolean {
        if (!initialized) {
            return false
        }

        return RadarSettings.getTracking(context)
    }

    /**
     * Returns the current tracking options.
     *
     * @return The current tracking options.
     */
    fun getTrackingOptions(): RadarTrackingOptions? {
        if (!initialized) {
            return null
        }

        return RadarSettings.getTrackingOptions(context)
    }

    /**
     * Accepts an event. Events can be accepted after user check-ins or other forms of verification. Event verifications will be used to improve the accuracy and confidence level of future events. See [](https://radar.io/documentation/sdk#android-verify).
     *
     * @param[eventId] The ID of the event to accept.
     * @param[verifiedPlaceId] For place entry events, the ID of the verified place. May be `null`.
     */
    fun acceptEvent(eventId: String, verifiedPlaceId: String? = null) {
        if (!initialized) {
            return
        }

        apiClient.verifyEvent(eventId, RadarEventVerification.ACCEPT, verifiedPlaceId)
    }

    /**
     * Rejects an event. Events can be accepted after user check-ins or other forms of verification. Event verifications will be used to improve the accuracy and confidence level of future events. See [](https://radar.io/documentation/sdk#android-verify).
     *
     * @param[eventId] The ID of the event to reject.
     */
    fun rejectEvent(eventId: String) {
        if (!initialized) {
            return
        }

        apiClient.verifyEvent(eventId, RadarEventVerification.REJECT)
    }

    /**
     * Gets the device's current location, then searches for places near that location, sorted by distance.
     *
     * @param[radius] The radius to search, in meters. A number between 100 and 10000.
     * @param[chains] An array of chain slugs to filter. See [](https://radar.io/documentation/places/chains)
     * @param[categories] An array of categories to filter. See [](https://radar.io/documentation/places/categories)
     * @param[groups] An array of groups to filter. See [](https://radar.io/documentation/places/groups)
     * @param[limit] The max number of places to return. A number between 1 and 100.
     * @param[callback] A callback.
     */
    fun searchPlaces(
        radius: Int,
        chains: Array<String>?,
        categories: Array<String>?,
        groups: Array<String>?,
        limit: Int?,
        callback: RadarSearchPlacesCallback
    ) {
        if (!initialized) {
            callback.onComplete(RadarStatus.ERROR_PUBLISHABLE_KEY)

            return
        }

        locationManager.getLocation(object : RadarLocationCallback {
            override fun onComplete(status: RadarStatus, location: Location?, stopped: Boolean) {
                if (status != RadarStatus.SUCCESS || location == null) {
                    callback.onComplete(status)

                    return
                }

                apiClient.searchPlaces(location, radius, chains, categories, groups, limit, object : RadarApiClient.RadarSearchPlacesApiCallback {
                    override fun onComplete(status: RadarStatus, res: JSONObject?, places: Array<RadarPlace>?) {
                        callback.onComplete(status, location, places)
                    }
                })
            }
        })
    }

    /**
     * Gets the device's current location, then searches for places near that location, sorted by distance.
     *
     * @param[radius] The radius to search, in meters. A number between 100 and 10000.
     * @param[chains] An array of chain slugs to filter. See [](https://radar.io/documentation/places/chains)
     * @param[categories] An array of categories to filter. See [](https://radar.io/documentation/places/categories)
     * @param[groups] An array of groups to filter. See [](https://radar.io/documentation/places/groups)
     * @param[limit] The max number of places to return. A number between 1 and 100.
     * @param[block] A block callback.
     */
    fun searchPlaces(
        radius: Int,
        chains: Array<String>?,
        categories: Array<String>?,
        groups: Array<String>?,
        limit: Int?,
        block: (status: RadarStatus, location: Location?, places: Array<RadarPlace>?) -> Unit
    ) {
        searchPlaces(
            radius,
            chains,
            categories,
            groups,
            limit,
            object : RadarSearchPlacesCallback {
                override fun onComplete(status: RadarStatus, location: Location?, places: Array<RadarPlace>?) {
                    block(status, location, places)
                }
            }
        )
    }

    /**
     * Search for places near a location, sorted by distance.
     *
     * @param[location] The location to search.
     * @param[radius] The radius to search, in meters. A number between 100 and 10000.
     * @param[chains] An array of chain slugs to filter. See [](https://radar.io/documentation/places/chains)
     * @param[categories] An array of categories to filter. See [](https://radar.io/documentation/places/categories)
     * @param[groups] An array of groups to filter. See [](https://radar.io/documentation/places/groups)
     * @param[limit] The max number of places to return. A number between 1 and 100.
     * @param[callback] A callback.
     */
    fun searchPlaces(
        location: Location,
        radius: Int,
        chains: Array<String>?,
        categories: Array<String>?,
        groups: Array<String>?,
        limit: Int?,
        callback: RadarSearchPlacesCallback
    ) {
        if (!initialized) {
            callback.onComplete(RadarStatus.ERROR_PUBLISHABLE_KEY)

            return
        }

        apiClient.searchPlaces(location, radius, chains, categories, groups, limit, object : RadarApiClient.RadarSearchPlacesApiCallback {
            override fun onComplete(status: RadarStatus, res: JSONObject?, places: Array<RadarPlace>?) {
                callback.onComplete(status, location, places)
            }
        })
    }

    /**
     * Search for places near a location, sorted by distance.
     *
     * @param[location] The location to search.
     * @param[radius] The radius to search, in meters. A number between 100 and 10000.
     * @param[chains] An array of chain slugs to filter. See [](https://radar.io/documentation/places/chains)
     * @param[categories] An array of categories to filter. See [](https://radar.io/documentation/places/categories)
     * @param[groups] An array of groups to filter. See [](https://radar.io/documentation/places/groups)
     * @param[limit] The max number of places to return. A number between 1 and 100.
     * @param[block] A block callback.
     */
    fun searchPlaces(
        location: Location,
        radius: Int,
        chains: Array<String>?,
        categories: Array<String>?,
        groups: Array<String>?,
        limit: Int?,
        block: (status: RadarStatus, location: Location?, places: Array<RadarPlace>?) -> Unit
    ) {
        searchPlaces(
            location,
            radius,
            chains,
            categories,
            groups,
            limit,
            object : RadarSearchPlacesCallback {
                override fun onComplete(status: RadarStatus, location: Location?, places: Array<RadarPlace>?) {
                    block(status, location, places)
                }
            }
        )
    }

    /**
     * Gets the device's current location, then searches for geofences near that location, sorted by distance.
     *
     * @param[radius] The radius to search, in meters. A number between 100 and 10000.
     * @param[tags] An array of tags to filter. See [](https://radar.io/documentation/geofences)
     * @param[limit] The max number of places to return. A number between 1 and 100.
     * @param[callback] A callback.
     */
    fun searchGeofences(
        radius: Int,
        tags: Array<String>?,
        limit: Int?,
        callback: RadarSearchGeofencesCallback
    ) {
        if (!initialized) {
            callback.onComplete(RadarStatus.ERROR_PUBLISHABLE_KEY)

            return
        }

        locationManager.getLocation(object : RadarLocationCallback {
            override fun onComplete(status: RadarStatus, location: Location?, stopped: Boolean) {
                if (status != RadarStatus.SUCCESS || location == null) {
                    callback.onComplete(status)

                    return
                }

                apiClient.searchGeofences(location, radius, tags, limit, object : RadarApiClient.RadarSearchGeofencesApiCallback {
                    override fun onComplete(status: RadarStatus, res: JSONObject?, geofences: Array<RadarGeofence>?) {
                        callback.onComplete(status, location, geofences)
                    }
                })
            }
        })
    }

    /**
     * Gets the device's current location, then searches for geofences near that location, sorted by distance.
     *
     * @param[radius] The radius to search, in meters. A number between 100 and 10000.
     * @param[tags] An array of tags to filter. See [](https://radar.io/documentation/geofences)
     * @param[limit] The max number of places to return. A number between 1 and 100.
     * @param[block] A block callback.
     */
    fun searchGeofences(
        radius: Int,
        tags: Array<String>?,
        limit: Int?,
        block: (status: RadarStatus, location: Location?, geofences: Array<RadarGeofence>?) -> Unit
    ) {
        searchGeofences(
            radius,
            tags,
            limit,
            object : RadarSearchGeofencesCallback {
                override fun onComplete(status: RadarStatus, location: Location?, geofences: Array<RadarGeofence>?) {
                    block(status, location, geofences)
                }
            }
        )
    }

    /**
     * Search for geofences near a location, sorted by distance.
     *
     * @param[location] The location to search.
     * @param[radius] The radius to search, in meters. A number between 100 and 10000.
     * @param[tags] An array of tags to filter. See [](https://radar.io/documentation/geofences)
     * @param[limit] The max number of places to return. A number between 1 and 100.
     * @param[callback] A callback.
     */
    fun searchGeofences(
        location: Location,
        radius: Int,
        tags: Array<String>?,
        limit: Int?,
        callback: RadarSearchGeofencesCallback
    ) {
        if (!initialized) {
            callback.onComplete(RadarStatus.ERROR_PUBLISHABLE_KEY)

            return
        }

        apiClient.searchGeofences(location, radius, tags, limit, object : RadarApiClient.RadarSearchGeofencesApiCallback {
            override fun onComplete(status: RadarStatus, res: JSONObject?, geofences: Array<RadarGeofence>?) {
                callback.onComplete(status, location, geofences)
            }
        })
    }

    /**
     * Search for geofences near a location, sorted by distance.
     *
     * @param[location] The location to search.
     * @param[radius] The radius to search, in meters. A number between 100 and 10000.
     * @param[tags] An array of tags to filter. See [](https://radar.io/documentation/geofences)
     * @param[limit] The max number of places to return. A number between 1 and 100.
     * @param[block] A block callback.
     */
    fun searchGeofences(
        location: Location,
        radius: Int,
        tags: Array<String>?,
        limit: Int?,
        block: (status: RadarStatus, location: Location?, geofences: Array<RadarGeofence>?) -> Unit
    ) {
        searchGeofences(
            location,
            radius,
            tags,
            limit,
            object : RadarSearchGeofencesCallback {
                override fun onComplete(status: RadarStatus, location: Location?, geofences: Array<RadarGeofence>?) {
                    block(status, location, geofences)
                }
            }
        )
    }

    /**
     * Geocodes an address, converting address to coordinates.
     *
     * @param[query] The address to geocode.
     * @param[callback] A callback.
     */
    fun geocode(
        query: String,
        callback: RadarGeocodeCallback
    ) {
        if (!initialized) {
            callback.onComplete(RadarStatus.ERROR_PUBLISHABLE_KEY)

            return
        }

        apiClient.geocode(query, object: RadarApiClient.RadarGeocodeApiCallback {
            override fun onComplete(status: RadarStatus, res: JSONObject?, addresses: Array<RadarAddress>?) {
                callback.onComplete(status, addresses)
            }
        })
    }

    /**
     * Geocodes an addresss, converting address to coordinates.
     *
     * @param[query] The address to geocode.
     * @param[block] A block callback.
     */
    fun geocode(
        query: String,
        block: (status: RadarStatus, addresses: Array<RadarAddress>?) -> Unit
    ) {
        geocode(
            query,
            object: RadarGeocodeCallback {
                override fun onComplete(status: RadarStatus, addresses: Array<RadarAddress>?) {
                    block(status, addresses)
                }
            }
        )
    }

    /**
     * Gets the device's current location, then reverse geocodes that location, converting coordinates to address.
     *
     * @param[callback] A callback.
     */
    fun reverseGeocode(
        callback: RadarGeocodeCallback
    ) {
        if (!initialized) {
            callback.onComplete(RadarStatus.ERROR_PUBLISHABLE_KEY)

            return
        }

        locationManager.getLocation(object: RadarLocationCallback {
            override fun onComplete(status: RadarStatus, location: Location?, stopped: Boolean) {
                if (status != RadarStatus.SUCCESS || location == null) {
                    callback.onComplete(status)

                    return
                }

                apiClient.reverseGeocode(location, object: RadarApiClient.RadarGeocodeApiCallback {
                    override fun onComplete(status: RadarStatus, res: JSONObject?, addresses: Array<RadarAddress>?) {
                        callback.onComplete(status, addresses)
                    }
                })
            }
        })
    }

    /**
     * Gets the device's current location, then reverse geocodes that location, converting coordinates to address.
     *
     * @param[block] A block callback.
     */
    fun reverseGeocode(
        block: (status: RadarStatus, addresses: Array<RadarAddress>?) -> Unit
    ) {
        reverseGeocode(
            object: RadarGeocodeCallback {
                override fun onComplete(status: RadarStatus, addresses: Array<RadarAddress>?) {
                    block(status, addresses)
                }
            }
        )
    }

    /**
     * Reverse geocodes a location, converting coordinates to address.
     *
     * @param[location] The location to reverse geocode.
     * @param[callback] A callback.
     */
    fun reverseGeocode(
        location: Location,
        callback: RadarGeocodeCallback
    ) {
        if (!initialized) {
            callback.onComplete(RadarStatus.ERROR_PUBLISHABLE_KEY)

            return
        }

        apiClient.reverseGeocode(location, object: RadarApiClient.RadarGeocodeApiCallback {
            override fun onComplete(status: RadarStatus, res: JSONObject?, addresses: Array<RadarAddress>?) {
                callback.onComplete(status, addresses)
            }
        })
    }

    /**
     * Reverse geocodes a location, converting coordinates to address.
     *
     * @param[location] The location to geocode.
     * @param[block] A block callback.
     */
    fun reverseGeocode(
        location: Location,
        block: (status: RadarStatus, addresses: Array<RadarAddress>?) -> Unit
    ) {
        reverseGeocode(
            location,
            object: RadarGeocodeCallback {
                override fun onComplete(status: RadarStatus, addresses: Array<RadarAddress>?) {
                    block(status, addresses)
                }
            }
        )
    }

    /**
     * Geocodes the device's current IP address, converting IP address to country.
     *
     * @param[callback] A callback.
     */
    fun ipGeocode(
        callback: RadarIpGeocodeCallback
    ) {
        if (!initialized) {
            callback.onComplete(RadarStatus.ERROR_PUBLISHABLE_KEY)

            return
        }

        apiClient.ipGeocode(null, object: RadarApiClient.RadarIpGeocodeApiCallback {
            override fun onComplete(status: RadarStatus, res: JSONObject?, country: RadarRegion?) {
                callback.onComplete(status, country)
            }
        })
    }

    /**
     * Geocodes the device's current IP address, converting IP address to country.
     *
     * @param[block] A block callback.
     */
    fun ipGeocode(
        block: (status: RadarStatus, country: RadarRegion?) -> Unit
    ) {
        ipGeocode(
            object: RadarIpGeocodeCallback {
                override fun onComplete(status: RadarStatus, country: RadarRegion?) {
                    block(status, country)
                }
            }
        )
    }

    /**
     * Sets the log level for debug logs.
     *
     * @param[level] The log level.
     */
    fun setLogLevel(level: RadarLogLevel) {
        if (!initialized) {
            return
        }

        RadarSettings.setLogLevel(context, level)
    }

    /**
     * Returns a display string for a location source value.
     *
     * @param[source] A location source value.
     *
     * @return A display string for the location source value.
     */
    fun stringForSource(source: RadarLocationSource): String? {
        return when (source) {
            RadarLocationSource.FOREGROUND_LOCATION -> "foregroundLocation"
            RadarLocationSource.BACKGROUND_LOCATION -> "backgroundLocation"
            RadarLocationSource.MANUAL_LOCATION -> "manualLocation"
            RadarLocationSource.GEOFENCE_ENTER -> "geofenceEnter"
            RadarLocationSource.GEOFENCE_DWELL -> "geofenceDwell"
            RadarLocationSource.GEOFENCE_EXIT -> "geofenceExit"
            else -> "unknown"
        }
    }

    internal fun handleLocation(context: Context, location: Location, source: RadarLocationSource) {
        if (!initialized) {
            initialize(context)
        }

        locationManager.handleLocation(location, source)
    }

    internal fun handleBootCompleted(context: Context) {
        if (!initialized) {
            initialize(context)
        }

        locationManager.handleBootCompleted()
    }

    internal fun broadcastIntent(intent: Intent) {
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent)

        val matches = context.packageManager.queryBroadcastReceivers(intent, 0)
        matches.forEach { resolveInfo ->
            val explicitIntent = Intent(intent)
            if (context.packageName == resolveInfo.activityInfo.packageName) {
                val componentName = ComponentName(
                    resolveInfo.activityInfo.applicationInfo.packageName,
                    resolveInfo.activityInfo.name
                )
                explicitIntent.component = componentName

                context.sendBroadcast(explicitIntent)
            }
        }
    }

}
