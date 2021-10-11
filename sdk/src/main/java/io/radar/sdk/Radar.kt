package io.radar.sdk

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.location.Location
import android.os.Build
import android.os.Handler
import io.radar.sdk.model.*
import io.radar.sdk.model.RadarEvent.RadarEventVerification
import org.json.JSONObject
import java.util.*

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
     * Called when a beacon ranging request succeeds, fails, or times out.
     */
    interface RadarBeaconCallback {

        /**
         * Called when a beacon ranging request succeeds, fails, or times out. Receives the request status and, if successful, the nearby beacon identifiers.
         *
         * @param[status] RadarStatus The request status.
         * @param[nearbyBeacons] Array<String>? If successful, the nearby beacon identifiers.
         */
        fun onComplete(
            status: RadarStatus,
            nearbyBeacons: Array<String>? = null
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
     * Called when a trip update succeeds, fails, or times out.
     */
    interface RadarTripCallback {

        /**
         * Called when a trip update succeeds, fails, or times out. Receives the request status and, if successful, the trip and an array of the events generated.
         *
         * @param[status] RadarStatus The request status.
         * @param[trip] RadarTrip? If successful, the trip.
         * @param[events] Array<RadarEvent>? If successful, an array of the events generated.
         */
        fun onComplete(
            status: RadarStatus,
            trip: RadarTrip? = null,
            events: Array<RadarEvent>? = null
        )

    }

    /**
     * Called when a context request succeeds, fails, or times out.
     */
    interface RadarContextCallback {

        /**
         * Called when a context request succeeds, fails, or times out. Receives the request status and, if successful, the location and the context.
         *
         * @param[status] RadarStatus The request status.
         * @param[location] Location? If successful, the location.
         * @param[context] RadarContext? If successful, the context.
         */
        fun onComplete(
            status: RadarStatus,
            location: Location? = null,
            context: RadarContext? = null
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
         * Called when a geocoding request succeeds, fails, or times out. Receives the request status and, if successful, the geocoding results (an array of addresses).
         *
         * @param[status] RadarStatus The request status.
         * @param[addresses] Array<RadarAddress>? If successful, the geocoding results (an array of addresses).
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
         * Called when an IP geocoding request succeeds, fails, or times out. Receives the request status and, if successful, the geocoding result (a partial address) and a boolean indicating whether the IP address is a known proxy.
         *
         * @param[status] RadarStatus The request status.
         * @param[address] RadarAddress? If successful, the geocoding result (a partial address).
         * @param[proxy] Boolean A boolean indicating whether the IP address is a known proxy.
         */
        fun onComplete(
            status: RadarStatus,
            address: RadarAddress? = null,
            proxy: Boolean = false
        )
    }

    /**
     * Called when a distance request succeeds, fails, or times out.
     */
    interface RadarRouteCallback {
        /**
         * Called when a distance request succeeds, fails, or times out. Receives the request status and, if successful, the routes.
         *
         * @param[status] RadarStatus The request status.
         * @param[routes] RadarRoutes? If successful, the routes.
         */
        fun onComplete(
            status: RadarStatus,
            routes: RadarRoutes? = null
        )
    }

    /**
     * Called when a matrix request succeeds, fails, or times out.
     */
    interface RadarMatrixCallback {
        /**
         * Called when a matrix request succeeds, fails, or times out. Receives the request status and, if successful, the matrix.
         *
         * @param[status] RadarStatus The request status.
         * @param[matrix] RadarRoutesMatrix? If successful, the matrix.
         */
        fun onComplete(
            status: RadarStatus,
            matrix: RadarRouteMatrix? = null
        )
    }

    /**
     * The status types for a request. See [](https://radar.io/documentation/sdk/android#foreground-tracking).
     */
    enum class RadarStatus {
        /** Success */
        SUCCESS,
        /** SDK not initialized */
        ERROR_PUBLISHABLE_KEY,
        /** Location permissions not granted */
        ERROR_PERMISSIONS,
        /** Location services error or timeout (20 seconds) */
        ERROR_LOCATION,
        /** Beacon ranging error or timeout (5 seconds) */
        ERROR_BLUETOOTH,
        /** Network error or timeout (10 seconds) */
        ERROR_NETWORK,
        /** Bad request (missing or invalid params) */
        ERROR_BAD_REQUEST,
        /** Unauthorized (invalid API key) */
        ERROR_UNAUTHORIZED,
        /** Payment required (organization disabled or usage exceeded) */
        ERROR_PAYMENT_REQUIRED,
        /** Forbidden (insufficient permissions or no beta access) */
        ERROR_FORBIDDEN,
        /** Not found */
        ERROR_NOT_FOUND,
        /** Too many requests (rate limit exceeded) */
        ERROR_RATE_LIMIT,
        /** Internal server error */
        ERROR_SERVER,
        /** Unknown error */
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
        /** Mock */
        MOCK_LOCATION,
        /** Beacon enter */
        BEACON_ENTER,
        /** Beacon exit */
        BEACON_EXIT,
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
            @JvmStatic
            fun fromInt(value: Int) : RadarLogLevel {
                return values().first { it.value == value }
            }
        }
    }

    /**
     * The travel modes for routes. See [](https://radar.io/documentation/api#routing).
     */
    enum class RadarRouteMode {
        /** Foot */
        FOOT,
        /** Bike */
        BIKE,
        /** Car */
        CAR,
        /** Truck */
        TRUCK,
        /** Motorbike */
        MOTORBIKE
    }

    /**
     * The distance units for routes. See [](https://radar.io/documentation/api#routing).
     */
    enum class RadarRouteUnits {
        /** Imperial (feet) */
        IMPERIAL,
        /** Metric (meters) */
        METRIC
    }

    private var initialized = false
    private lateinit var context: Context
    private lateinit var handler: Handler
    private var receiver: RadarReceiver? = null
    internal lateinit var logger: RadarLogger
    internal lateinit var apiClient: RadarApiClient
    internal lateinit var locationManager: RadarLocationManager
    internal lateinit var beaconManager: RadarBeaconManager

    /**
     * Initializes the Radar SDK. Call this method from the main thread in your `Application` class before calling any other Radar methods.
     *
     * @see [](https://radar.io/documentation/sdk/android#initialize-sdk)
     *
     * @param[context] The context
     * @param[publishableKey] Your publishable API key
     * @param[receiver] An optional receiver for the client-side delivery of events
     */
    @JvmStatic
    fun initialize(context: Context?, publishableKey: String? = null, receiver: RadarReceiver? = null) {
        if (context == null) {
            return
        }

        this.initialized = true
        this.context = context.applicationContext
        this.handler = Handler(this.context.mainLooper)
        this.receiver = receiver

        if (!this::logger.isInitialized) {
            this.logger = RadarLogger(this.context)
        }

        RadarSettings.updateSessionId(this.context)

        if (publishableKey != null) {
            RadarSettings.setPublishableKey(this.context, publishableKey)
        }

        if (!this::apiClient.isInitialized) {
            this.apiClient = RadarApiClient(this.context, logger)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (!this::beaconManager.isInitialized) {
                this.beaconManager = RadarBeaconManager(this.context, logger)
            }
        }
        if (!this::locationManager.isInitialized) {
            this.locationManager = RadarLocationManager(this.context, apiClient, logger)
            this.locationManager.updateTracking()
        }

        this.logger.d("Initializing")

        RadarUtils.loadAdId(this.context)

        val application = this.context as? Application
        application?.registerActivityLifecycleCallbacks(RadarActivityLifecycleCallbacks())

        this.apiClient.getConfig(object : RadarApiClient.RadarGetConfigApiCallback {
            override fun onComplete(
                status: RadarStatus,
                res: JSONObject?,
                meta: RadarApiClient.RadarMeta?
            ) {
                locationManager?.updateTrackingFromMeta(context, meta)
            }
        })

        logger.i("📍️ Radar initialized")
    }

    /**
     * Identifies the user. Until you identify the user, Radar will automatically identify the user by `deviceId` (Android ID).
     *
     * @see [](https://radar.io/documentation/sdk/android#identify-user)
     *
     * @param[userId] A stable unique ID for the user. If null, the previous `userId` will be cleared.
     */
    @JvmStatic
    fun setUserId(userId: String?) {
        if (!initialized) {
            return
        }

        RadarSettings.setUserId(context, userId)
    }

    /**
     * Returns the current `userId`.
     *
     * @see [](https://radar.io/documentation/sdk/android#identify-user)
     *
     * @return The current `userId`.
     */
    @JvmStatic
    fun getUserId(): String? {
        if (!initialized) {
            return null
        }

        return RadarSettings.getUserId(context)
    }

    /**
     * Sets an optional description for the user, displayed in the dashboard.
     *
     * @see [](https://radar.io/documentation/sdk/android#identify-user)
     *
     * @param[description] A description for the user. If null, the previous `description` will be cleared.
     */
    @JvmStatic
    fun setDescription(description: String?) {
        if (!initialized) {
            return
        }

        RadarSettings.setDescription(context, description)
    }

    /**
     * Returns the current `description`.
     *
     * @see [](https://radar.io/documentation/sdk/android#identify-user)
     *
     * @return The current `description`.
     */
    @JvmStatic
    fun getDescription(): String? {
        if (!initialized) {
            return null
        }

        return RadarSettings.getDescription(context)
    }

    /**
     * Sets an optional set of custom key-value pairs for the user.
     *
     * @see [](https://radar.io/documentation/sdk/android#identify-user)
     *
     * @param[metadata] A set of custom key-value pairs for the user. Must have 16 or fewer keys and values of type string, boolean, or number. If `null`, the previous `metadata` will be cleared.
     */
    @JvmStatic
    fun setMetadata(metadata: JSONObject?) {
        if (!initialized) {
            return
        }

        RadarSettings.setMetadata(context, metadata)
    }

    /**
     * Returns the current `metadata`.
     *
     * @see [](https://radar.io/documentation/sdk/android#identify-user)
     *
     * @return The current `metadata`.
     */
    @JvmStatic
    fun getMetadata(): JSONObject? {
        if (!initialized) {
            return null
        }

        return RadarSettings.getMetadata(context)
    }

    /**
     * Enables `adId` (Android advertising ID) collection. Disabled by default.
     *
     * @param[enabled] A boolean indicating whether `adId` should be collected.
     */
    @JvmStatic
    fun setAdIdEnabled(enabled: Boolean) {
        RadarSettings.setAdIdEnabled(context, enabled)
    }

    /**
     * Gets the device's current location.
     *
     * @see [](https://radar.io/documentation/sdk/android#get-location)
     *
     * @param[callback] An optional callback.
     */
    @JvmStatic
    fun getLocation(callback: RadarLocationCallback? = null) {
        if (!initialized) {
            callback?.onComplete(RadarStatus.ERROR_PUBLISHABLE_KEY)

            return
        }

        locationManager.getLocation(object : RadarLocationCallback {
            override fun onComplete(status: RadarStatus, location: Location?, stopped: Boolean) {
                handler.post {
                    callback?.onComplete(status, location, stopped)
                }
            }
        })
    }

    /**
     * Gets the device's current location.
     *
     * @see [](https://radar.io/documentation/sdk/android#get-location)
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
     * Gets the device's current location with the desired accuracy.
     *
     * @see [](https://radar.io/documentation/sdk/android#get-location)
     *
     * @param[desiredAccuracy] The desired accuracy.
     * @param[callback] An optional callback.
     */
    @JvmStatic
    fun getLocation(desiredAccuracy: RadarTrackingOptions.RadarTrackingOptionsDesiredAccuracy, callback: RadarLocationCallback? = null) {
        if (!initialized) {
            callback?.onComplete(RadarStatus.ERROR_PUBLISHABLE_KEY)

            return
        }

        locationManager.getLocation(desiredAccuracy, RadarLocationSource.FOREGROUND_LOCATION, object : RadarLocationCallback {
            override fun onComplete(status: RadarStatus, location: Location?, stopped: Boolean) {
                handler.post {
                    callback?.onComplete(status, location, stopped)
                }
            }
        })
    }

    /**
     * Gets the device's current location with the desired accuracy.
     *
     * @see [](https://radar.io/documentation/sdk/android#get-location)
     *
     * @param[desiredAccuracy] The desired accuracy.
     * @param[block] A block callback.
     */
    fun getLocation(desiredAccuracy: RadarTrackingOptions.RadarTrackingOptionsDesiredAccuracy, block: (status: RadarStatus, location: Location?, stopped: Boolean) -> Unit) {
        getLocation(desiredAccuracy, object : RadarLocationCallback {
            override fun onComplete(status: RadarStatus, location: Location?, stopped: Boolean) {
                block(status, location, stopped)
            }
        })
    }

    /**
     * Tracks the user's location once in the foreground.
     *
     * @see [](https://radar.io/documentation/sdk/android#foreground-tracking)
     *
     * @param[callback] An optional callback.
     */
    @JvmStatic
    fun trackOnce(callback: RadarTrackCallback? = null) {
        var desiredAccuracy = RadarTrackingOptions.RadarTrackingOptionsDesiredAccuracy.MEDIUM
        if (RadarUtils.isEmulator()) {
            desiredAccuracy = RadarTrackingOptions.RadarTrackingOptionsDesiredAccuracy.HIGH
        }
        trackOnce(desiredAccuracy, false, callback)
    }

    /**
     * Tracks the user's location once in the foreground.
     *
     * @see [](https://radar.io/documentation/sdk/android#foreground-tracking)
     *
     * @param[block] A block callback.
     */
    fun trackOnce(block: (status: RadarStatus, location: Location?, events: Array<RadarEvent>?, user: RadarUser?) -> Unit) {
        var desiredAccuracy = RadarTrackingOptions.RadarTrackingOptionsDesiredAccuracy.MEDIUM
        if (RadarUtils.isEmulator()) {
            desiredAccuracy = RadarTrackingOptions.RadarTrackingOptionsDesiredAccuracy.HIGH
        }
        trackOnce(desiredAccuracy, false, block)
    }

    /**
     * Tracks the user's location once with the desired accuracy and optionally ranges beacons in the foreground.
     *
     * @see [](https://radar.io/documentation/sdk/android#foreground-tracking)
     *
     * @param[desiredAccuracy] The desired accuracy.
     * @param[beacons] A boolean indicating whether to range beacons.
     * @param[callback] An optional callback.
     */
    @JvmStatic
    fun trackOnce(desiredAccuracy: RadarTrackingOptions.RadarTrackingOptionsDesiredAccuracy, beacons: Boolean, callback: RadarTrackCallback? = null) {
        if (!initialized) {
            callback?.onComplete(RadarStatus.ERROR_PUBLISHABLE_KEY)

            return
        }

        locationManager.getLocation(desiredAccuracy, RadarLocationSource.FOREGROUND_LOCATION, object : RadarLocationCallback {
            override fun onComplete(status: RadarStatus, location: Location?, stopped: Boolean) {
                if (status != RadarStatus.SUCCESS || location == null) {
                    handler.post {
                        callback?.onComplete(status)
                    }

                    return
                }

                val callTrackApi = { nearbyBeacons: Array<String>? ->
                    apiClient.track(location, stopped, true, RadarLocationSource.FOREGROUND_LOCATION, false, nearbyBeacons, object : RadarApiClient.RadarTrackApiCallback {
                        override fun onComplete(
                            status: RadarStatus,
                            res: JSONObject?,
                            events: Array<RadarEvent>?,
                            user: RadarUser?,
                            nearbyGeofences: Array<RadarGeofence>?,
                            meta: RadarApiClient.RadarMeta?,
                        ) {
                            callback?.onComplete(status, location, events, user)
                        }
                    })
                }

                if (beacons && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    apiClient.searchBeacons(location, 1000, 10, object : RadarApiClient.RadarSearchBeaconsApiCallback {
                        override fun onComplete(status: RadarStatus, res: JSONObject?, beacons: Array<RadarBeacon>?) {
                            if (status != RadarStatus.SUCCESS || beacons == null) {
                                callTrackApi(null)

                                return
                            }

                            beaconManager.rangeBeacons(beacons, object : RadarBeaconCallback {
                                override fun onComplete(status: RadarStatus, nearbyBeacons: Array<String>?) {
                                    if (status != RadarStatus.SUCCESS || nearbyBeacons == null) {
                                        callTrackApi(null)

                                        return
                                    }

                                    callTrackApi(nearbyBeacons)
                                }
                            })
                        }
                    })
                } else {
                    callTrackApi(null)
                }
            }
        })
    }

    /**
     * Tracks the user's location once with the desired accuracy and optionally ranges beacons in the foreground.
     *
     * @see [](https://radar.io/documentation/sdk/android#foreground-tracking)
     *
     * @param[desiredAccuracy] The desired accuracy.
     * @param[beacons] A boolean indicating whether to range beacons.
     * @param[block] A block callback.
     */
    @JvmStatic
    fun trackOnce(desiredAccuracy: RadarTrackingOptions.RadarTrackingOptionsDesiredAccuracy, beacons: Boolean, block: (status: RadarStatus, location: Location?, events: Array<RadarEvent>?, user: RadarUser?) -> Unit) {
        trackOnce(desiredAccuracy, beacons, object : RadarTrackCallback {
            override fun onComplete(status: RadarStatus, location: Location?, events: Array<RadarEvent>?, user: RadarUser?) {
                block(status, location, events, user)
            }
        })
    }

    /**
     * Manually updates the user's location. Note that these calls are subject to rate limits.
     *
     * @see [](https://radar.io/documentation/sdk/android#foreground-tracking)
     *
     * @param[location] A location for the user.
     * @param[callback] An optional callback.
     */
    @JvmStatic
    fun trackOnce(location: Location, callback: RadarTrackCallback?) {
        if (!initialized) {
            callback?.onComplete(RadarStatus.ERROR_PUBLISHABLE_KEY)

            return
        }

        apiClient.track(location, false, true, RadarLocationSource.MANUAL_LOCATION, false, null, object : RadarApiClient.RadarTrackApiCallback {
            override fun onComplete(
                status: RadarStatus,
                res: JSONObject?,
                events: Array<RadarEvent>?,
                user: RadarUser?,
                nearbyGeofences: Array<RadarGeofence>?,
                meta: RadarApiClient.RadarMeta?,
            ) {
                callback?.onComplete(status, location, events, user)
            }
        })
    }

    /**
     * Manually updates the user's location. Note that these calls are subject to rate limits.
     *
     * @see [](https://radar.io/documentation/sdk/android#foreground-tracking)
     *
     * @param[location] A location for the user.
     * @param[block] A block callback.
     */
    @JvmStatic
    fun trackOnce(location: Location, block: (status: RadarStatus, location: Location?, events: Array<RadarEvent>?, user: RadarUser?) -> Unit) {
        trackOnce(location, object : RadarTrackCallback {
            override fun onComplete(status: RadarStatus, location: Location?, events: Array<RadarEvent>?, user: RadarUser?) {
                block(status, location, events, user)
            }
        })
    }

    /**
     * Starts tracking the user's location in the background.
     *
     * @see [](https://radar.io/documentation/sdk/android#background-tracking-for-geofencing)
     *
     * @param[options] Configurable tracking options.
     */
    @JvmStatic
    fun startTracking(options: RadarTrackingOptions) {
        if (!initialized) {
            return
        }

        locationManager.startTracking(options)
    }

    /**
     * Mocks tracking the user's location from an origin to a destination.
     *
     * @see [](https://radar.io/documentation/sdk/android#mock-tracking-for-testing)
     *
     * @param[origin] The origin.
     * @param[destination] The destination.
     * @param[mode] The travel mode.
     * @param[steps] The number of mock location updates.
     * @param[interval] The interval in seconds between each mock location update. A number between 1 and 60.
     * @param[callback] An optional callback.
     */
    @JvmStatic
    fun mockTracking(
        origin: Location,
        destination: Location,
        mode: RadarRouteMode,
        steps: Int,
        interval: Int,
        callback: RadarTrackCallback?
    ) {
        if (!initialized) {
            return
        }

        apiClient.getDistance(origin, destination, EnumSet.of(mode), RadarRouteUnits.METRIC, steps, object : RadarApiClient.RadarDistanceApiCallback {
            override fun onComplete(
                status: RadarStatus,
                res: JSONObject?,
                routes: RadarRoutes?
            ) {
                val coordinates = when (mode) {
                    RadarRouteMode.FOOT -> routes?.foot?.geometry?.coordinates
                    RadarRouteMode.BIKE -> routes?.bike?.geometry?.coordinates
                    RadarRouteMode.CAR -> routes?.car?.geometry?.coordinates
                    RadarRouteMode.TRUCK -> routes?.truck?.geometry?.coordinates
                    RadarRouteMode.MOTORBIKE -> routes?.motorbike?.geometry?.coordinates
                }

                if (coordinates == null) {
                    handler.post {
                        callback?.onComplete(status)
                    }

                    return
                }

                var intervalLimit = interval
                if (interval < 1) {
                    intervalLimit = 1
                } else if (interval > 60) {
                    intervalLimit = 60
                }

                var i = 0
                val track = object : Runnable {
                    override fun run() {
                        val track = this
                        val coordinate = coordinates[i]
                        val location = Location("RadarSDK").apply {
                            latitude = coordinate.latitude
                            longitude = coordinate.longitude
                            accuracy = 5f
                        }
                        val stopped = (i == 0) || (i == coordinates.size - 1)

                        apiClient.track(location, stopped, false, RadarLocationSource.MOCK_LOCATION, false, null, object : RadarApiClient.RadarTrackApiCallback {
                            override fun onComplete(
                                status: RadarStatus,
                                res: JSONObject?,
                                events: Array<RadarEvent>?,
                                user: RadarUser?,
                                nearbyGeofences: Array<RadarGeofence>?,
                                meta: RadarApiClient.RadarMeta?,
                            ) {
                                callback?.onComplete(status, location, events, user)

                                if (i < coordinates.size - 1) {
                                    handler.postDelayed(track, intervalLimit * 1000L)
                                }

                                i++
                            }
                        })
                    }
                }

                handler.post(track)
            }
        })
    }

    /**
     * Mocks tracking the user's location from an origin to a destination.
     *
     * @see [](https://radar.io/documentation/sdk/android#mock-tracking-for-testing)
     *
     * @param[origin] The origin.
     * @param[destination] The destination.
     * @param[mode] The travel mode.
     * @param[steps] The number of mock location updates.
     * @param[interval] The interval in seconds between each mock location update. A number between 1 and 60.
     * @param[block] A block callback.
     */
    @JvmStatic
    fun mockTracking(
        origin: Location,
        destination: Location,
        mode: RadarRouteMode,
        steps: Int,
        interval: Int,
        block: (status: RadarStatus, location: Location?, events: Array<RadarEvent>?, user: RadarUser?) -> Unit
    ) {
        mockTracking(origin, destination, mode, steps, interval, object : RadarTrackCallback {
            override fun onComplete(status: RadarStatus, location: Location?, events: Array<RadarEvent>?, user: RadarUser?) {
                block(status, location, events, user)
            }
        })
    }

    /**
     * Stops tracking the user's location in the background.
     *
     * @see [](https://radar.io/documentation/sdk/android#background-tracking-for-geofencing)
     */
    @JvmStatic
    fun stopTracking() {
        if (!initialized) {
            return
        }

        locationManager.stopTracking()
    }

    /**
     * Returns a boolean indicating whether tracking has been started.
     *
     * @see [](https://radar.io/documentation/sdk/android#background-tracking-for-geofencing)
     *
     * @return A boolean indicating whether tracking has been started.
     */
    @JvmStatic
    fun isTracking(): Boolean {
        if (!initialized) {
            return false
        }

        return RadarSettings.getTracking(context)
    }

    /**
     * Returns the current tracking options.
     *
     * @see [](https://radar.io/documentation/sdk/tracking)
     *
     * @return The current tracking options.
     */
    @JvmStatic
    fun getTrackingOptions(): RadarTrackingOptions? {
        if (!initialized) {
            return null
        }

        return RadarSettings.getTrackingOptions(context)
    }

    /**
     * Settings for the foreground notification when the foregroundServiceEnabled parameter
     * is true on Radar tracking options.
     *
     * @see [](https://radar.io/documentation/sdk/tracking)
     *
     * @param[options] Foreground service options
     */
    @JvmStatic
    fun setForegroundServiceOptions(options: RadarTrackingOptions.RadarTrackingOptionsForegroundService) {
        if (!initialized) {
            return
        }

        RadarSettings.setForegroundService(context, options)
    }

    /**
     * Accepts an event. Events can be accepted after user check-ins or other forms of verification. Event verifications will be used to improve the accuracy and confidence level of future events.
     *
     * @see [](https://radar.io/documentation/places#verify-events)
     *
     * @param[eventId] The ID of the event to accept.
     * @param[verifiedPlaceId] For place entry events, the ID of the verified place. May be `null`.
     */
    @JvmStatic
    fun acceptEvent(eventId: String, verifiedPlaceId: String? = null) {
        if (!initialized) {
            return
        }

        apiClient.verifyEvent(eventId, RadarEventVerification.ACCEPT, verifiedPlaceId)
    }

    /**
     * Rejects an event. Events can be accepted after user check-ins or other forms of verification. Event verifications will be used to improve the accuracy and confidence level of future events.
     *
     * @see [](https://radar.io/documentation/places#verify-events)
     *
     * @param[eventId] The ID of the event to reject.
     */
    @JvmStatic
    fun rejectEvent(eventId: String) {
        if (!initialized) {
            return
        }

        apiClient.verifyEvent(eventId, RadarEventVerification.REJECT)
    }

    /**
     * Returns the current trip options.
     *
     * @see [](https://radar.io/documentation/trip-tracking)
     *
     * @return The current trip options.
     */
    @JvmStatic
    fun getTripOptions(): RadarTripOptions? {
        if (!initialized) {
            return null
        }

        return RadarSettings.getTripOptions(context)
    }

    /**
     * Starts a trip.
     *
     * @see [](https://radar.io/documentation/trip-tracking)
     *
     * @param[options] Configurable trip options.
     * @param[callback] An optional callback.
     */
    @JvmStatic
    fun startTrip(options: RadarTripOptions, callback: RadarTripCallback? = null) {
        if (!initialized) {
            return
        }

        apiClient.updateTrip(options, RadarTrip.RadarTripStatus.STARTED, object : RadarApiClient.RadarTripApiCallback {
            override fun onComplete(
                status: RadarStatus,
                res: JSONObject?,
                trip: RadarTrip?,
                events: Array<RadarEvent>?
            ) {
                if (status == RadarStatus.SUCCESS) {
                    RadarSettings.setTripOptions(context, options)

                    // flush location update to generate events
                    locationManager.getLocation(null)
                }

                handler.post {
                    callback?.onComplete(status, trip, events)
                }
            }
        })
    }

    /**
     * Starts a trip.
     *
     * @see [](https://radar.io/documentation/trip-tracking)
     *
     * @param[options] Configurable trip options.
     * @param[block] An optional block callback.
     */
    @JvmStatic
    fun startTrip(options: RadarTripOptions, block: (status: RadarStatus, trip: RadarTrip?, events: Array<RadarEvent>?) -> Unit) {
        startTrip(options, object : RadarTripCallback {
            override fun onComplete(
                status: RadarStatus,
                trip: RadarTrip?,
                events: Array<RadarEvent>?
            ) {
                block(status, trip, events)
            }
        })
    }

    /**
     * Manually updates a trip.
     *
     * @see [](https://radar.io/documentation/trip-tracking)
     *
     * @param[options] Configurable trip options.
     * @param[status] The trip status. To avoid updating status, pass UNKNOWN.
     * @param[callback] An optional callback.
     */
    @JvmStatic
    fun updateTrip(options: RadarTripOptions, status: RadarTrip.RadarTripStatus?, callback: RadarTripCallback? = null) {
        if (!initialized) {
            return
        }

        apiClient.updateTrip(options, status, object : RadarApiClient.RadarTripApiCallback {
            override fun onComplete(
                status: RadarStatus,
                res: JSONObject?,
                trip: RadarTrip?,
                events: Array<RadarEvent>?
            ) {
                if (status == RadarStatus.SUCCESS) {
                    RadarSettings.setTripOptions(context, options)

                    // flush location update to generate events
                    locationManager.getLocation(null)
                }

                handler.post {
                    callback?.onComplete(status, trip, events)
                }
            }
        })
    }

    /**
     * Manually updates a trip.
     *
     * @see [](https://radar.io/documentation/trip-tracking)
     *
     * @param[options] Configurable trip options.
     * @param[status] The trip status. To avoid updating status, pass UNKNOWN.
     * @param[block] An optional block callback.
     */
    @JvmStatic
    fun updateTrip(options: RadarTripOptions, status: RadarTrip.RadarTripStatus?, block: (status: RadarStatus, trip: RadarTrip?, events: Array<RadarEvent>?) -> Unit) {
        updateTrip(options, status, object : RadarTripCallback {
            override fun onComplete(
                status: RadarStatus,
                trip: RadarTrip?,
                events: Array<RadarEvent>?
            ) {
                block(status, trip, events)
            }
        })
    }

    /**
     * Completes a trip.
     *
     * @param[callback] An optional callback.
     *
     * @see [](https://radar.io/documentation/trip-tracking)
     */
    @JvmStatic
    fun completeTrip(callback: RadarTripCallback? = null) {
        if (!initialized) {
            return
        }

        val options = RadarSettings.getTripOptions(context)
        apiClient.updateTrip(options, RadarTrip.RadarTripStatus.COMPLETED, object : RadarApiClient.RadarTripApiCallback {
            override fun onComplete(
                status: RadarStatus,
                res: JSONObject?,
                trip: RadarTrip?,
                events: Array<RadarEvent>?
            ) {
                if (status == RadarStatus.SUCCESS || status == RadarStatus.ERROR_NOT_FOUND) {
                    RadarSettings.setTripOptions(context, null)

                    // flush location update to generate events
                    locationManager.getLocation(null)
                }

                handler.post {
                    callback?.onComplete(status, trip, events)
                }
            }
        })
    }

    /**
     * Completes a trip.
     *
     * @see [](https://radar.io/documentation/trip-tracking)
     *
     * @param[block] An optional block callback.
     */
    @JvmStatic
    fun completeTrip(block: (status: RadarStatus, trip: RadarTrip?, events: Array<RadarEvent>?) -> Unit) {
        completeTrip(object : RadarTripCallback {
            override fun onComplete(
                status: RadarStatus,
                trip: RadarTrip?,
                events: Array<RadarEvent>?
            ) {
                block(status, trip, events)
            }
        })
    }

    /**
     * Cancels a trip.
     *
     * @param[callback] An optional callback.
     *
     * @see [](https://radar.io/documentation/trip-tracking)
     */
    @JvmStatic
    fun cancelTrip(callback: RadarTripCallback? = null) {
        if (!initialized) {
            return
        }

        val options = RadarSettings.getTripOptions(context)
        apiClient.updateTrip(options, RadarTrip.RadarTripStatus.CANCELED, object : RadarApiClient.RadarTripApiCallback {
            override fun onComplete(
                status: RadarStatus,
                res: JSONObject?,
                trip: RadarTrip?,
                events: Array<RadarEvent>?
            ) {
                if (status == RadarStatus.SUCCESS || status == RadarStatus.ERROR_NOT_FOUND) {
                    RadarSettings.setTripOptions(context, null)

                    // flush location update to generate events
                    locationManager.getLocation(null)
                }

                handler.post {
                    callback?.onComplete(status, trip, events)
                }
            }
        })
    }

    /**
     * Cancels a trip.
     *
     * @see [](https://radar.io/documentation/trip-tracking)
     *
     * @param[block] An optional block callback.
     */
    @JvmStatic
    fun cancelTrip(block: (status: RadarStatus, trip: RadarTrip?, events: Array<RadarEvent>?) -> Unit) {
        cancelTrip(object : RadarTripCallback {
            override fun onComplete(
                status: RadarStatus,
                trip: RadarTrip?,
                events: Array<RadarEvent>?
            ) {
                block(status, trip, events)
            }
        })
    }

    /**
     * Gets the device's current location, then searches for places near that location, sorted by distance.
     *
     * @see [](https://radar.io/documentation/api#search-places)
     *
     * @param[radius] The radius to search, in meters. A number between 100 and 10000.
     * @param[chains] An array of chain slugs to filter. See [](https://radar.io/documentation/places/chains)
     * @param[categories] An array of categories to filter. See [](https://radar.io/documentation/places/categories)
     * @param[groups] An array of groups to filter. See [](https://radar.io/documentation/places/groups)
     * @param[limit] The max number of places to return. A number between 1 and 100.
     * @param[callback] A callback.
     */
    @JvmStatic
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
                    handler.post {
                        callback.onComplete(status)
                    }

                    return
                }

                apiClient.searchPlaces(location, radius, chains, categories, groups, limit, object : RadarApiClient.RadarSearchPlacesApiCallback {
                    override fun onComplete(status: RadarStatus, res: JSONObject?, places: Array<RadarPlace>?) {
                        handler.post {
                            callback.onComplete(status, location, places)
                        }
                    }
                })
            }
        })
    }

    /**
     * Gets the device's current location, then searches for places near that location, sorted by distance.
     *
     * @see [](https://radar.io/documentation/api#search-places)
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
     * @see [](https://radar.io/documentation/api#search-places)
     *
     * @param[near] The location to search.
     * @param[radius] The radius to search, in meters. A number between 100 and 10000.
     * @param[chains] An array of chain slugs to filter. See [](https://radar.io/documentation/places/chains)
     * @param[categories] An array of categories to filter. See [](https://radar.io/documentation/places/categories)
     * @param[groups] An array of groups to filter. See [](https://radar.io/documentation/places/groups)
     * @param[limit] The max number of places to return. A number between 1 and 100.
     * @param[callback] A callback.
     */
    @JvmStatic
    fun searchPlaces(
        near: Location,
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

        apiClient.searchPlaces(near, radius, chains, categories, groups, limit, object : RadarApiClient.RadarSearchPlacesApiCallback {
            override fun onComplete(status: RadarStatus, res: JSONObject?, places: Array<RadarPlace>?) {
                handler.post {
                    callback.onComplete(status, near, places)
                }
            }
        })
    }

    /**
     * Search for places near a location, sorted by distance.
     *
     * @see [](https://radar.io/documentation/api#search-places)
     *
     * @param[near] The location to search.
     * @param[radius] The radius to search, in meters. A number between 100 and 10000.
     * @param[chains] An array of chain slugs to filter. See [](https://radar.io/documentation/places/chains)
     * @param[categories] An array of categories to filter. See [](https://radar.io/documentation/places/categories)
     * @param[groups] An array of groups to filter. See [](https://radar.io/documentation/places/groups)
     * @param[limit] The max number of places to return. A number between 1 and 100.
     * @param[block] A block callback.
     */
    fun searchPlaces(
        near: Location,
        radius: Int,
        chains: Array<String>?,
        categories: Array<String>?,
        groups: Array<String>?,
        limit: Int?,
        block: (status: RadarStatus, location: Location?, places: Array<RadarPlace>?) -> Unit
    ) {
        searchPlaces(
            near,
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
     * @see [](https://radar.io/documentation/api#search-geofences)
     *
     * @param[radius] The radius to search, in meters. A number between 100 and 10000.
     * @param[tags] An array of tags to filter. See [](https://radar.io/documentation/geofences)
     * @param[metadata] A dictionary of metadata to filter. See [](https://radar.io/documentation/geofences)
     * @param[limit] The max number of places to return. A number between 1 and 100.
     * @param[callback] A callback.
     */
    @JvmStatic
    fun searchGeofences(
        radius: Int,
        tags: Array<String>?,
        metadata: JSONObject?,
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
                    handler.post {
                        callback.onComplete(status)
                    }

                    return
                }

                apiClient.searchGeofences(location, radius, tags, metadata, limit, object : RadarApiClient.RadarSearchGeofencesApiCallback {
                    override fun onComplete(status: RadarStatus, res: JSONObject?, geofences: Array<RadarGeofence>?) {
                        handler.post {
                            callback.onComplete(status, location, geofences)
                        }
                    }
                })
            }
        })
    }

    /**
     * Gets the device's current location, then searches for geofences near that location, sorted by distance.
     *
     * @see [](https://radar.io/documentation/api#search-geofences)
     *
     * @param[radius] The radius to search, in meters. A number between 100 and 10000.
     * @param[tags] An array of tags to filter. See [](https://radar.io/documentation/geofences)
     * @param[metadata] A dictionary of metadata to filter. See [](https://radar.io/documentation/geofences)
     * @param[limit] The max number of places to return. A number between 1 and 100.
     * @param[block] A block callback.
     */
    fun searchGeofences(
        radius: Int,
        tags: Array<String>?,
        metadata: JSONObject?,
        limit: Int?,
        block: (status: RadarStatus, location: Location?, geofences: Array<RadarGeofence>?) -> Unit
    ) {
        searchGeofences(
            radius,
            tags,
            metadata,
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
     * @see [](https://radar.io/documentation/api#search-geofences)
     *
     * @param[near] The location to search.
     * @param[radius] The radius to search, in meters. A number between 100 and 10000.
     * @param[tags] An array of tags to filter. See [](https://radar.io/documentation/geofences)
     * @param[metadata] A dictionary of metadata to filter. See [](https://radar.io/documentation/geofences)
     * @param[limit] The max number of places to return. A number between 1 and 100.
     * @param[callback] A callback.
     */
    @JvmStatic
    fun searchGeofences(
        near: Location,
        radius: Int,
        tags: Array<String>?,
        metadata: JSONObject?,
        limit: Int?,
        callback: RadarSearchGeofencesCallback
    ) {
        if (!initialized) {
            callback.onComplete(RadarStatus.ERROR_PUBLISHABLE_KEY)

            return
        }

        apiClient.searchGeofences(near, radius, tags, metadata, limit, object : RadarApiClient.RadarSearchGeofencesApiCallback {
            override fun onComplete(status: RadarStatus, res: JSONObject?, geofences: Array<RadarGeofence>?) {
                handler.post {
                    callback.onComplete(status, near, geofences)
                }
            }
        })
    }

    /**
     * Search for geofences near a location, sorted by distance.
     *
     * @see [](https://radar.io/documentation/api#search-geofences)
     *
     * @param[near] The location to search.
     * @param[radius] The radius to search, in meters. A number between 100 and 10000.
     * @param[tags] An array of tags to filter. See [](https://radar.io/documentation/geofences)
     * @param[metadata] A dictionary of metadata to filter. See [](https://radar.io/documentation/geofences)
     * @param[limit] The max number of places to return. A number between 1 and 100.
     * @param[block] A block callback.
     */
    fun searchGeofences(
        near: Location,
        radius: Int,
        tags: Array<String>?,
        metadata: JSONObject?,
        limit: Int?,
        block: (status: RadarStatus, location: Location?, geofences: Array<RadarGeofence>?) -> Unit
    ) {
        searchGeofences(
            near,
            radius,
            tags,
            metadata,
            limit,
            object : RadarSearchGeofencesCallback {
                override fun onComplete(status: RadarStatus, location: Location?, geofences: Array<RadarGeofence>?) {
                    block(status, location, geofences)
                }
            }
        )
    }


    /**
     * Autocompletes partial addresses and place names, sorted by relevance.
     *
     * @see [](https://radar.io/documentation/api#autocomplete)
     *
     * @param[query] The partial address or place name to autocomplete.
     * @param[near] A location for the search.
     * @param[limit] The max number of addresses to return. A number between 1 and 100.
     * @param[callback] A callback.
     */
    @JvmStatic
    fun autocomplete(
            query: String,
            near: Location? = null,
            limit: Int? = null,
            callback: RadarGeocodeCallback
    ) {
        if (!initialized) {
            callback.onComplete(RadarStatus.ERROR_PUBLISHABLE_KEY)

            return
        }

        apiClient.autocomplete(query, near, null, limit, null, object : RadarApiClient.RadarGeocodeApiCallback {
            override fun onComplete(status: RadarStatus, res: JSONObject?, addresses: Array<RadarAddress>?) {
                handler.post {
                    callback.onComplete(status, addresses)
                }
            }
        })
    }

    /**
     * Autocompletes partial addresses and place names, sorted by relevance.
     *
     * @see [](https://radar.io/documentation/api#autocomplete)
     *
     * @param[query] The partial address or place name to autocomplete.
     * @param[near] A location for the search.
     * @param[limit] The max number of addresses to return. A number between 1 and 100.
     * @param[block] A block callback.
     */
    fun autocomplete(
            query: String,
            near: Location? = null,
            limit: Int? = null,
            block: (status: RadarStatus, addresses: Array<RadarAddress>?) -> Unit
    ) {
        autocomplete(
                query,
                near,
                null,
                limit,
                null,
                object : RadarGeocodeCallback {
                    override fun onComplete(status: RadarStatus, addresses: Array<RadarAddress>?) {
                        block(status, addresses)
                    }
                }
        )
    }

    /**
     * Autocompletes partial addresses and place names, sorted by relevance.
     *
     * @see [](https://radar.io/documentation/api#autocomplete)
     *
     * @param[query] The partial address or place name to autocomplete.
     * @param[near] A location for the search.
     * @param[layers] Optional layer filters.
     * @param[limit] The max number of addresses to return. A number between 1 and 100.
     * @param[country] An optional country filter. A string, the unique 2-letter country code.
     * @param[callback] A callback.
     */
    @JvmStatic
    fun autocomplete(
        query: String,
        near: Location? = null,
        layers: Array<String>? = null,
        limit: Int? = null,
        country: String? = null,
        callback: RadarGeocodeCallback
    ) {
        if (!initialized) {
            callback.onComplete(RadarStatus.ERROR_PUBLISHABLE_KEY)

            return
        }

        apiClient.autocomplete(query, near, layers, limit, country, object : RadarApiClient.RadarGeocodeApiCallback {
            override fun onComplete(status: RadarStatus, res: JSONObject?, addresses: Array<RadarAddress>?) {
                handler.post {
                    callback.onComplete(status, addresses)
                }
            }
        })
    }

    /**
     * Autocompletes partial addresses and place names, sorted by relevance.
     *
     * @see [](https://radar.io/documentation/api#autocomplete)
     *
     * @param[query] The partial address or place name to autocomplete.
     * @param[near] A location for the search.
     * @param[layers] Optional layer filters.
     * @param[limit] The max number of addresses to return. A number between 1 and 100.
     * @param[country] An optional country filter. A string, the unique 2-letter country code.
     * @param[block] A block callback.
     */
    fun autocomplete(
        query: String,
        near: Location? = null,
        layers: Array<String>? = null,
        limit: Int? = null,
        country: String? = null,
        block: (status: RadarStatus, addresses: Array<RadarAddress>?) -> Unit
    ) {
        autocomplete(
            query,
            near,
            layers,
            limit,
            country,
            object : RadarGeocodeCallback {
                override fun onComplete(status: RadarStatus, addresses: Array<RadarAddress>?) {
                    block(status, addresses)
                }
            }
        )
    }

    /**
     * Geocodes an address, converting address to coordinates.
     *
     * @see [](https://radar.io/documentation/api#forward-geocode)
     *
     * @param[query] The address to geocode.
     * @param[callback] A callback.
     */
    @JvmStatic
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
                handler.post {
                    callback.onComplete(status, addresses)
                }
            }
        })
    }

    /**
     * Geocodes an address, converting address to coordinates.
     *
     * @see [](https://radar.io/documentation/api#forward-geocode)
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
     * @see [](https://radar.io/documentation/api#reverse-geocode)
     *
     * @param[callback] A callback.
     */
    @JvmStatic
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
                    handler.post {
                        callback.onComplete(status)
                    }

                    return
                }

                apiClient.reverseGeocode(location, object: RadarApiClient.RadarGeocodeApiCallback {
                    override fun onComplete(status: RadarStatus, res: JSONObject?, addresses: Array<RadarAddress>?) {
                        handler.post {
                            callback.onComplete(status, addresses)
                        }
                    }
                })
            }
        })
    }

    /**
     * Gets the device's current location, then reverse geocodes that location, converting coordinates to address.
     *
     * @see [](https://radar.io/documentation/api#reverse-geocode)
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
     * @see [](https://radar.io/documentation/api#reverse-geocode)
     *
     * @param[location] The location to reverse geocode.
     * @param[callback] A callback.
     */
    @JvmStatic
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
                handler.post {
                    callback.onComplete(status, addresses)
                }
            }
        })
    }

    /**
     * Reverse geocodes a location, converting coordinates to address.
     *
     * @see [](https://radar.io/documentation/api#reverse-geocode)
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
     * Geocodes the device's current IP address, converting IP address to partial address.
     *
     * @see [](https://radar.io/documentation/api#ip-geocode)
     *
     * @param[callback] A callback.
     */
    @JvmStatic
    fun ipGeocode(
        callback: RadarIpGeocodeCallback
    ) {
        if (!initialized) {
            callback.onComplete(RadarStatus.ERROR_PUBLISHABLE_KEY)

            return
        }

        apiClient.ipGeocode(object: RadarApiClient.RadarIpGeocodeApiCallback {
            override fun onComplete(status: RadarStatus, res: JSONObject?, address: RadarAddress?, proxy: Boolean) {
                handler.post {
                    callback.onComplete(status, address, proxy)
                }
            }
        })
    }

    /**
     * Geocodes the device's current IP address, converting IP address to partial address.
     *
     * @see [](https://radar.io/documentation/api#ip-geocode)
     *
     * @param[block] A block callback.
     */
    fun ipGeocode(
        block: (status: RadarStatus, address: RadarAddress?, proxy: Boolean) -> Unit
    ) {
        ipGeocode(
            object: RadarIpGeocodeCallback {
                override fun onComplete(status: RadarStatus, address: RadarAddress?, proxy: Boolean) {
                    block(status, address, proxy)
                }
            }
        )
    }

    /**
     * Gets the device's current location, then calculates the travel distance and duration to a destination.
     *
     * @see [](https://radar.io/documentation/api#distance)
     *
     * @param[destination] The destination.
     * @param[modes] The travel modes.
     * @param[units] The distance units.
     * @param[callback] A callback.
     */
    @JvmStatic
    fun getDistance(
        destination: Location,
        modes: EnumSet<RadarRouteMode>,
        units: RadarRouteUnits,
        callback: RadarRouteCallback
    ) {
        if (!initialized) {
            callback.onComplete(RadarStatus.ERROR_PUBLISHABLE_KEY)

            return
        }

        locationManager.getLocation(object: RadarLocationCallback {
            override fun onComplete(status: RadarStatus, location: Location?, stopped: Boolean) {
                if (status != RadarStatus.SUCCESS || location == null) {
                    handler.post {
                        callback.onComplete(status)
                    }

                    return
                }

                apiClient.getDistance(location, destination, modes, units, -1, object : RadarApiClient.RadarDistanceApiCallback {
                    override fun onComplete(
                        status: RadarStatus,
                        res: JSONObject?,
                        routes: RadarRoutes?
                    ) {
                        handler.post {
                            callback.onComplete(status, routes)
                        }
                    }
                })
            }
        })
    }

    /**
     * Gets the device's current location, then calculates the travel distance and duration to a destination.
     *
     * @see [](https://radar.io/documentation/api#distance)
     *
     * @param[destination] The destination.
     * @param[modes] The travel modes.
     * @param[units] The distance units.
     * @param[block] A block callback.
     */
    fun getDistance(
        destination: Location,
        modes: EnumSet<RadarRouteMode>,
        units: RadarRouteUnits,
        block: (status: RadarStatus, routes: RadarRoutes?) -> Unit
    ) {
        getDistance(
            destination,
            modes,
            units,
            object: RadarRouteCallback {
                override fun onComplete(status: RadarStatus, routes: RadarRoutes?) {
                    block(status, routes)
                }
            }
        )
    }

    /**
     * Calculates the travel distance and duration from an origin to a destination.
     *
     * @see [](https://radar.io/documentation/api#distance)
     *
     * @param[origin] The origin.
     * @param[destination] The destination.
     * @param[modes] The travel modes.
     * @param[units] The distance units.
     * @param[callback] A callback.
     */
    @JvmStatic
    fun getDistance(
        origin: Location,
        destination: Location,
        modes: EnumSet<RadarRouteMode>,
        units: RadarRouteUnits,
        callback: RadarRouteCallback
    ) {
        if (!initialized) {
            callback.onComplete(RadarStatus.ERROR_PUBLISHABLE_KEY)

            return
        }

        apiClient.getDistance(origin, destination, modes, units, -1, object : RadarApiClient.RadarDistanceApiCallback {
            override fun onComplete(
                status: RadarStatus,
                res: JSONObject?,
                routes: RadarRoutes?
            ) {
                handler.post {
                    callback.onComplete(status, routes)
                }
            }
        })
    }

    /**
     * Calculates the travel distance and duration from an origin to a destination.
     *
     * @see [](https://radar.io/documentation/api#distance)
     *
     * @param[origin] The origin.
     * @param[destination] The destination.
     * @param[modes] The travel modes.
     * @param[units] The distance units.
     * @param[block] A block callback.
     */
    fun getDistance(
        origin: Location,
        destination: Location,
        modes: EnumSet<RadarRouteMode>,
        units: RadarRouteUnits,
        block: (status: RadarStatus, routes: RadarRoutes?) -> Unit
    ) {
        getDistance(
            origin,
            destination,
            modes,
            units,
            object: RadarRouteCallback {
                override fun onComplete(status: RadarStatus, routes: RadarRoutes?) {
                    block(status, routes)
                }
            }
        )
    }

    /**
     * Calculates the travel distances and durations between multiple origins and destinations for up to 25 routes.
     *
     * @see [](https://radar.io/documentation/api#matrix)
     *
     * @param[origins] The origins.
     * @param[destinations] The destinations.
     * @param[mode] The travel mode.
     * @param[units] The distance units.
     * @param[callback] A callback.
     */
    @JvmStatic
    fun getMatrix(
        origins: Array<Location>,
        destinations: Array<Location>,
        mode: RadarRouteMode,
        units: RadarRouteUnits,
        callback: RadarMatrixCallback
    ) {
        if (!initialized) {
            callback.onComplete(RadarStatus.ERROR_PUBLISHABLE_KEY)

            return
        }

        apiClient.getMatrix(origins, destinations, mode, units, object : RadarApiClient.RadarMatrixApiCallback {
            override fun onComplete(
                status: RadarStatus,
                res: JSONObject?,
                matrix: RadarRouteMatrix?
            ) {
                handler.post {
                    callback.onComplete(status, matrix)
                }
            }
        })
    }

    /**
     * Calculates the travel distances and durations between multiple origins and destinations for up to 25 routes.
     *
     * @see [](https://radar.io/documentation/api#matrix)
     *
     * @param[origins] The origins.
     * @param[destinations] The destinations.
     * @param[mode] The travel mode.
     * @param[units] The distance units.
     * @param[block] A block callback.
     */
    fun getMatrix(
        origins: Array<Location>,
        destinations: Array<Location>,
        mode: RadarRouteMode,
        units: RadarRouteUnits,
        block: (status: RadarStatus, matrix: RadarRouteMatrix?) -> Unit
    ) {
        getMatrix(
            origins,
            destinations,
            mode,
            units,
            object: RadarMatrixCallback {
                override fun onComplete(status: RadarStatus, matrix: RadarRouteMatrix?) {
                    block(status, matrix)
                }
            }
        )
    }

    /**
     * Gets the device's current location, then gets context for that location without sending device or user identifiers to the server.
     *
     * @param[callback] A callback.
     */
    @JvmStatic
    fun getContext(
        callback: RadarContextCallback
    ) {
        if (!initialized) {
            callback.onComplete(RadarStatus.ERROR_PUBLISHABLE_KEY)

            return
        }

        locationManager.getLocation(object: RadarLocationCallback {
            override fun onComplete(status: RadarStatus, location: Location?, stopped: Boolean) {
                if (status != RadarStatus.SUCCESS || location == null) {
                    handler.post {
                        callback.onComplete(status)
                    }

                    return
                }

                apiClient.getContext(location, object : RadarApiClient.RadarContextApiCallback {
                    override fun onComplete(status: RadarStatus, res: JSONObject?, context: RadarContext?) {
                        handler.post {
                            callback.onComplete(status, location, context)
                        }
                    }
                })
            }
        })
    }

    /**
     * Gets the device's current location, then gets context for that location without sending device or user identifiers to the server.
     *
     * @param[block] A block callback.
     */
    fun getContext(block: (status: RadarStatus, location: Location?, context: RadarContext?) -> Unit) {
        getContext(object : RadarContextCallback {
            override fun onComplete(status: RadarStatus, location: Location?, context: RadarContext?) {
                block(status, location, context)
            }
        })
    }

    /**
     * Gets context for a location without sending device or user identifiers to the server.
     *
     * @param[location] The location.
     * @param[callback] A callback.
     */
    @JvmStatic
    fun getContext(location: Location, callback: RadarContextCallback) {
        if (!initialized) {
            callback.onComplete(RadarStatus.ERROR_PUBLISHABLE_KEY)

            return
        }

        apiClient.getContext(location, object : RadarApiClient.RadarContextApiCallback {
            override fun onComplete(status: RadarStatus, res: JSONObject?, context: RadarContext?) {
                handler.post {
                    callback.onComplete(status, location, context)
                }
            }
        })
    }

    /**
     * Gets context for a location without sending device or user identifiers to the server.
     *
     * @param[location] The location.
     * @param[block] A block callback.
     */
    fun getContext(location: Location, block: (status: RadarStatus, location: Location?, context: RadarContext?) -> Unit) {
        getContext(location, object : RadarContextCallback {
            override fun onComplete(status: RadarStatus, location: Location?, context: RadarContext?) {
                block(status, location, context)
            }
        })
    }

    /**
     * Sets the log level for debug logs.
     *
     * @param[level] The log level.
     */
    @JvmStatic
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
    @JvmStatic
    fun stringForSource(source: RadarLocationSource): String? {
        return when (source) {
            RadarLocationSource.FOREGROUND_LOCATION -> "FOREGROUND_LOCATION"
            RadarLocationSource.BACKGROUND_LOCATION -> "BACKGROUND_LOCATION"
            RadarLocationSource.MANUAL_LOCATION -> "MANUAL_LOCATION"
            RadarLocationSource.GEOFENCE_ENTER -> "GEOFENCE_ENTER"
            RadarLocationSource.GEOFENCE_DWELL -> "GEOFENCE_DWELL"
            RadarLocationSource.GEOFENCE_EXIT -> "GEOFENCE_EXIT"
            RadarLocationSource.MOCK_LOCATION -> "MOCK_LOCATION"
            RadarLocationSource.BEACON_ENTER -> "BEACON_ENTER"
            RadarLocationSource.BEACON_EXIT -> "BEACON_EXIT"
            else -> "UNKNOWN"
        }
    }

    /**
     * Returns a display string for a travel mode value.
     *
     * @param[mode] A travel mode value.
     *
     * @return A display string for the travel mode value.
     */
    @JvmStatic
    fun stringForMode(mode: RadarRouteMode): String? {
        return when (mode) {
            RadarRouteMode.FOOT -> "foot"
            RadarRouteMode.BIKE -> "bike"
            RadarRouteMode.CAR -> "car"
            RadarRouteMode.TRUCK -> "truck"
            RadarRouteMode.MOTORBIKE -> "motorbike"
        }
    }

    /**
     * Returns a display string for a trip status value.
     *
     * @param[status] A trip status value.
     *
     * @return A display string for the trip status value.
     */
    @JvmStatic
    fun stringForTripStatus(status: RadarTrip.RadarTripStatus): String? {
        return when (status) {
            RadarTrip.RadarTripStatus.STARTED -> "started"
            RadarTrip.RadarTripStatus.APPROACHING -> "approaching"
            RadarTrip.RadarTripStatus.ARRIVED -> "arrived"
            RadarTrip.RadarTripStatus.EXPIRED -> "expired"
            RadarTrip.RadarTripStatus.COMPLETED -> "completed"
            RadarTrip.RadarTripStatus.CANCELED -> "canceled"
            else -> "unknown"
        }
    }

    /**
     * Returns a JSON object for a location.
     *
     * @param[location] A location.
     *
     * @return A JSON object for the location.
     */
    @JvmStatic
    fun jsonForLocation(location: Location): JSONObject {
        val obj = JSONObject()
        obj.put("latitude", location.latitude)
        obj.put("longitude", location.longitude)
        obj.put("accuracy", location.accuracy)
        obj.put("altitude", location.altitude)
        obj.put("speed", location.speed)
        obj.put("course", location.bearing)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            obj.put("verticalAccuracy", location.verticalAccuracyMeters)
            obj.put("speedAccuracy", location.speedAccuracyMetersPerSecond)
            obj.put("courseAccuracy", location.bearingAccuracyDegrees)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            obj.put("mocked", location.isFromMockProvider)
        }
        return obj
    }

    internal fun handleLocation(context: Context, location: Location, source: RadarLocationSource) {
        if (!initialized) {
            initialize(context)
        }

        locationManager.handleLocation(location, source)
    }

    internal fun handleBeacon(context: Context, source: RadarLocationSource) {
        if (!initialized) {
            initialize(context)
        }

        locationManager.handleBeacon(source)
    }

    internal fun handleBootCompleted(context: Context) {
        if (!initialized) {
            initialize(context)
        }

        locationManager.handleBootCompleted()
    }

    internal fun sendEvents(events: Array<RadarEvent>, user: RadarUser? = null) {
        if (events.isEmpty()) {
            return
        }

        receiver?.onEventsReceived(context, events, user)

        for (event in events) {
            logger.i("📍 Radar event received | type = ${RadarEvent.stringForType(event.type)}; link = https://radar.io/dashboard/events/${event._id}")
        }
    }

    internal fun sendLocation(location: Location, user: RadarUser) {
        receiver?.onLocationUpdated(context, location, user)

        logger.i("📍 Radar location updated | coordinates = (${location.latitude}, ${location.longitude}); accuracy = ${location.accuracy} meters; link = https://radar.io/dashboard/users/${user._id}")
    }

    internal fun sendClientLocation(location: Location, stopped: Boolean, source: RadarLocationSource) {
        receiver?.onClientLocationUpdated(context, location, stopped, source)
    }

    internal fun sendError(status: RadarStatus) {
        receiver?.onError(context, status)

        logger.i("📍️ Radar error received | status = $status")
    }

    internal fun sendLog(message: String) {
        receiver?.onLog(context, message)
    }

}
