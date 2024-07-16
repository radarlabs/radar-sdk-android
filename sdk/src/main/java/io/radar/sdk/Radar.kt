package io.radar.sdk

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.content.Context
import android.location.Location
import android.os.Build
import android.os.Handler
import androidx.annotation.RequiresApi
import io.radar.sdk.model.*
import io.radar.sdk.model.RadarEvent.RadarEventVerification
import io.radar.sdk.util.RadarLogBuffer
import io.radar.sdk.util.RadarReplayBuffer
import io.radar.sdk.util.RadarSimpleLogBuffer
import io.radar.sdk.util.RadarSimpleReplayBuffer
import org.json.JSONObject
import java.util.*

/**
 * The main class used to interact with the Radar SDK.
 *
 * @see [](https://radar.com/documentation/sdk)
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
         * Called when a beacon ranging request succeeds, fails, or times out. Receives the request status and, if successful, the nearby beacons.
         *
         * @param[status] RadarStatus The request status.
         * @param[beacons] Array<String>? If successful, the nearby beacons.
         */
        fun onComplete(
            status: RadarStatus,
            beacons: Array<RadarBeacon>? = null
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
     * Called when a track verified request succeeds, fails, or times out.
     */
    interface RadarTrackVerifiedCallback {

        /**
         * Called when a track verified request succeeds, fails, or times out. Receives the request status and, if successful, the user's verified location. Verify the token server-side using your secret key.
         *
         * @param[status] RadarStatus The request status.
         * @param[token] RadarVerifiedLocationToken? If successful, the user's verified location.
         */
        fun onComplete(
            status: RadarStatus,
            token: RadarVerifiedLocationToken? = null
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
     * Called when a validateAddress request succeeds, fails, or times out.
     * Receives the request status and, if successful, the address populated with a verification status.
     */

     interface RadarValidateAddressCallback {
        fun onComplete(
            status: RadarStatus,
            address: RadarAddress? = null,
            verificationStatus: RadarAddressVerificationStatus? = null
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
     * Called when a request to log a conversion succeeds, fails, or times out.
     */
    interface RadarLogConversionCallback {
        /**
         * Called when a request to log a conversion succeeds, fails, or times out. Receives the request status and, if successful, the conversion event generated.
         *
         * @param[status] RadarStatus The request status.
         * @param[event] RadarEvent? If successful, the conversion event.
         *
         */
        fun onComplete(
            status: RadarStatus,
            event: RadarEvent? = null
        )
    }

    // interface RadarIndoorSurveyCallback {
    //     fun onComplete(
    //         status: RadarStatus,
    //         indoorsPayload: String
    //     )
    // }

    /**
     * The status types for a request. See [](https://radar.com/documentation/sdk/android#foreground-tracking).
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
            fun fromInt(value: Int): RadarLogLevel {
                return values().first { it.value == value }
            }
        }
    }

    /**
     * The classification type for debug logs.
     */
    enum class RadarLogType(val value: Int) {
        NONE(0),
        SDK_CALL(1),
        SDK_ERROR(2),
        SDK_EXCEPTION(3),
        APP_LIFECYCLE_EVENT(4),
        PERMISSION_EVENT(5);

        companion object {
            @JvmStatic
            fun fromInt(value: Int): RadarLogType {
                return values().first { it.value == value }
            }
        }
    }

    /**
     * The travel modes for routes. See [](https://radar.com/documentation/api#routing).
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
     * The distance units for routes. See [](https://radar.com/documentation/api#routing).
     */
    enum class RadarRouteUnits {
        /** Imperial (feet) */
        IMPERIAL,
        /** Metric (meters) */
        METRIC
    }

    /**
     * The verification status of an address.
     */
    enum class RadarAddressVerificationStatus {
        VERIFIED,
        PARTIALLY_VERIFIED,
        AMBIGUOUS,
        UNVERIFIED,
        NONE
    }

    /**
     * The location services providers.
     */
    enum class RadarLocationServicesProvider {
        /** Google Play Services Location (default) */
        GOOGLE,
        /** Huawei Mobile Services Location */
        HUAWEI
    }

    internal var initialized = false
    internal var isFlushingReplays = false
    private lateinit var context: Context
    private var activity: Activity? = null
    internal lateinit var handler: Handler
    private var receiver: RadarReceiver? = null
    private var verifiedReceiver: RadarVerifiedReceiver? = null
    internal lateinit var logger: RadarLogger
    internal lateinit var apiClient: RadarApiClient
    internal lateinit var indoorSurveyManager: RadarIndoorSurveyManager
    internal lateinit var locationManager: RadarLocationManager
    internal lateinit var beaconManager: RadarBeaconManager
    private lateinit var logBuffer: RadarLogBuffer
    private lateinit var replayBuffer: RadarReplayBuffer
    internal lateinit var batteryManager: RadarBatteryManager
    private lateinit var verificationManager: RadarVerificationManager
    private lateinit var locationPermissionManager: RadarLocationPermissionManager

    /**
     * Used by React Native module to setup the activity.
     */
    @JvmStatic
    fun onActivityCreate(activity: Activity, context: Context?) {
        this.context = context ?: activity;
        this.activity = activity;

        val application = this.context as? Application
        if (!this::locationPermissionManager.isInitialized) {
            this.locationPermissionManager = RadarLocationPermissionManager(this.context, this.activity)
            application?.registerActivityLifecycleCallbacks(locationPermissionManager)
        }
    }

    /**
     * Initializes the Radar SDK. Call this method from the main thread in `Application.onCreate()` before calling any other Radar methods.
     *
     * @see [](https://radar.com/documentation/sdk/android#initialize-sdk)
     *
     * @param[context] The context.
     * @param[publishableKey] Your publishable API key.
     */
    @JvmStatic
    fun initialize(context: Context?, publishableKey: String? = null) {
        initialize(context, publishableKey, null)
    }

    /**
     * Initializes the Radar SDK. Call this method from the main thread in `Application.onCreate()` before calling any other Radar methods.
     *
     * @see [](https://radar.com/documentation/sdk/android#initialize-sdk)
     *
     * @param[context] The context.
     * @param[publishableKey] Your publishable API key.
     * @param[receiver] An optional receiver for the client-side delivery of events.
     * @param[provider] The location services provider.
     * @param[fraud] A boolean indicating whether to enable additional fraud detection signals for location verification.
     */
    @JvmStatic
    fun initialize(context: Context?, publishableKey: String? = null, receiver: RadarReceiver? = null, provider: RadarLocationServicesProvider = RadarLocationServicesProvider.GOOGLE, fraud: Boolean = false) {
        if (context == null) {
            return
        }

        this.context = context.applicationContext
        this.handler = Handler(this.context.mainLooper)

        if (context is Activity) {
            this.activity = context
        }

        if (receiver != null) {
            this.receiver = receiver
        }

        if (!this::logBuffer.isInitialized) {
            this.logBuffer = RadarSimpleLogBuffer(this.context)
        }

        if (!this::replayBuffer.isInitialized) {
            this.replayBuffer = RadarSimpleReplayBuffer(this.context)
        }

        if (!this::logger.isInitialized) {
            this.logger = RadarLogger(this.context)
        }

        if (publishableKey != null) {
            RadarSettings.setPublishableKey(this.context, publishableKey)
        }

        if (!this::apiClient.isInitialized) {
            this.apiClient = RadarApiClient(this.context, logger)
        }

        if (RadarActivityLifecycleCallbacks.foreground) {
            this.logger.d("App is foregrounded")
            RadarSettings.updateSessionId(this.context)
        } else {
            this.logger.d("App is backgrounded, not updating session ID")
        }

        if (!this::batteryManager.isInitialized) {
            this.batteryManager = RadarBatteryManager(this.context)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!this::beaconManager.isInitialized) {
                this.beaconManager = RadarBeaconManager(this.context, logger)
            }
        }
        if (!this::locationManager.isInitialized) {
            this.locationManager = RadarLocationManager(this.context, apiClient, logger, batteryManager, provider)
            RadarSettings.setLocationServicesProvider(this.context, provider)
            this.locationManager.updateTracking()
        }

        this.logger.i("Initializing", RadarLogType.SDK_CALL)

        if (provider == RadarLocationServicesProvider.GOOGLE) {
            this.logger.d("Using Google location services")
        } else if (provider == RadarLocationServicesProvider.HUAWEI) {
            this.logger.d("Using Huawei location services")
        }

        if (!this::indoorSurveyManager.isInitialized) {
            this.indoorSurveyManager = RadarIndoorSurveyManager(this.context, logger, locationManager, apiClient)
        }

        val application = this.context as? Application
        if (fraud) {
            RadarSettings.setSharing(this.context, false)
        }
        application?.registerActivityLifecycleCallbacks(RadarActivityLifecycleCallbacks(fraud))

        if (!this::locationPermissionManager.isInitialized) {
            this.locationPermissionManager = RadarLocationPermissionManager(this.context, this.activity)
            application?.registerActivityLifecycleCallbacks(locationPermissionManager)
        }

        val featureSettings = RadarSettings.getFeatureSettings(this.context)
        if (featureSettings.usePersistence) {
            Radar.loadReplayBufferFromSharedPreferences()
        }

        val usage = "initialize"
        this.apiClient.getConfig(usage, false, object : RadarApiClient.RadarGetConfigApiCallback {
            override fun onComplete(status: RadarStatus, config: RadarConfig) {
                locationManager.updateTrackingFromMeta(config?.meta)
                RadarSettings.setFeatureSettings(context, config?.meta.featureSettings)
            }
        })

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            this.logger.logPastTermination()
        }

        this.initialized = true

        logger.i("📍️ Radar initialized")
    }

    /**
     * Identifies the user. Until you identify the user, Radar will automatically identify the user by `deviceId` (Android ID).
     *
     * @see [](https://radar.com/documentation/sdk/android#identify-user)
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
     * @see [](https://radar.com/documentation/sdk/android#identify-user)
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
     * @see [](https://radar.com/documentation/sdk/android#identify-user)
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
     * @see [](https://radar.com/documentation/sdk/android#identify-user)
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
     * @see [](https://radar.com/documentation/sdk/android#identify-user)
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
     * @see [](https://radar.com/documentation/sdk/android#identify-user)
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
     * Enables anonymous tracking for privacy reasons. Avoids creating user records on the server and avoids sending any stable device IDs, user IDs, and user metadata
     * to the server when calling `trackOnce()` or `startTracking()`. Disabled by default.
     *
     * @param[enabled] A boolean indicating whether anonymous tracking should be enabled.
     */
    @JvmStatic
    fun setAnonymousTrackingEnabled(enabled: Boolean) {
        RadarSettings.setAnonymousTrackingEnabled(context, enabled)
    }

    /**
     * Gets the device's current location.
     *
     * @see [](https://radar.com/documentation/sdk/android#get-location)
     *
     * @param[callback] An optional callback.
     */
    @JvmStatic
    fun getLocation(callback: RadarLocationCallback? = null) {
        if (!initialized) {
            callback?.onComplete(RadarStatus.ERROR_PUBLISHABLE_KEY)

            return
        }
        this.logger.i("getLocation()", RadarLogType.SDK_CALL)

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
     * @see [](https://radar.com/documentation/sdk/android#get-location)
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
     * @see [](https://radar.com/documentation/sdk/android#get-location)
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
        this.logger.i("getLocation()", RadarLogType.SDK_CALL)

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
     * @see [](https://radar.com/documentation/sdk/android#get-location)
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
     * @see [](https://radar.com/documentation/sdk/android#foreground-tracking)
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
     * @see [](https://radar.com/documentation/sdk/android#foreground-tracking)
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
     * @see [](https://radar.com/documentation/sdk/android#foreground-tracking)
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
        this.logger.i("trackOnce()", RadarLogType.SDK_CALL)

        locationManager.getLocation(desiredAccuracy, RadarLocationSource.FOREGROUND_LOCATION, object : RadarLocationCallback {
            override fun onComplete(status: RadarStatus, location: Location?, stopped: Boolean) {
                if (status != RadarStatus.SUCCESS || location == null) {
                    handler.post {
                        callback?.onComplete(status)
                    }

                    return
                }

                val callTrackApi = { beacons: Array<RadarBeacon>?, indoorsPayload: String ->
                    apiClient.track(location, stopped, true, RadarLocationSource.FOREGROUND_LOCATION, false, beacons, indoorsPayload, callback = object : RadarApiClient.RadarTrackApiCallback {
                        override fun onComplete(
                            status: RadarStatus,
                            res: JSONObject?,
                            events: Array<RadarEvent>?,
                            user: RadarUser?,
                            nearbyGeofences: Array<RadarGeofence>?,
                            config: RadarConfig?,
                            token: RadarVerifiedLocationToken?
                        ) {
                            if (status == RadarStatus.SUCCESS ){
                                locationManager.updateTrackingFromMeta(config?.meta)
                            }
                            handler.post {
                                callback?.onComplete(status, location, events, user)
                            }
                        }
                    })
                }

                logger.i("calling RadarIndoorsSurvey", RadarLogType.SDK_CALL)
                // todo: call indoors survey here
                indoorSurveyManager.start("WHEREAMI", 10, location, true, callback = object : RadarIndoorSurveyManager.RadarIndoorSurveyCallback {
                    override fun onComplete(status: RadarStatus, indoorsPayload: String) {
                        if (status != RadarStatus.SUCCESS) {
                            callTrackApi(null, "")
                        } else {
                            callTrackApi(null, indoorsPayload)
                        }
                    }
                })


                // if (beacons && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                //     apiClient.searchBeacons(location, 1000, 10, object : RadarApiClient.RadarSearchBeaconsApiCallback {
                //         override fun onComplete(status: RadarStatus, res: JSONObject?, beacons: Array<RadarBeacon>?, uuids: Array<String>?, uids: Array<String>?) {
                //              if (!uuids.isNullOrEmpty() || !uids.isNullOrEmpty()) {
                //                 beaconManager.startMonitoringBeaconUUIDs(uuids, uids)

                //                 beaconManager.rangeBeaconUUIDs(uuids, uids, false, object : RadarBeaconCallback {
                //                     override fun onComplete(status: RadarStatus, beacons: Array<RadarBeacon>?) {
                //                         if (status != RadarStatus.SUCCESS || beacons == null) {
                //                             callTrackApi(null)

                //                             return
                //                         }

                //                         callTrackApi(beacons)
                //                     }
                //                 })
                //             } else if (beacons != null) {
                //                 beaconManager.startMonitoringBeacons(beacons)

                //                 beaconManager.rangeBeacons(beacons, false, object : RadarBeaconCallback {
                //                     override fun onComplete(status: RadarStatus, beacons: Array<RadarBeacon>?) {
                //                         if (status != RadarStatus.SUCCESS || beacons == null) {
                //                             callTrackApi(null)

                //                             return
                //                         }

                //                         callTrackApi(beacons)
                //                     }
                //                 })
                //             } else {
                //                 callTrackApi(arrayOf());
                //             }
                //         }
                //     }, false)
                // } else {
                //     callTrackApi(null)
                // }
            }
        })
    }

    /**
     * Tracks the user's location once with the desired accuracy and optionally ranges beacons in the foreground.
     *
     * @see [](https://radar.com/documentation/sdk/android#foreground-tracking)
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
     * @see [](https://radar.com/documentation/sdk/android#foreground-tracking)
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

        apiClient.track(location, false, true, RadarLocationSource.MANUAL_LOCATION, false, null, callback = object : RadarApiClient.RadarTrackApiCallback {
            override fun onComplete(
                status: RadarStatus,
                res: JSONObject?,
                events: Array<RadarEvent>?,
                user: RadarUser?,
                nearbyGeofences: Array<RadarGeofence>?,
                config: RadarConfig?,
                token: RadarVerifiedLocationToken?
            ) {
                if (status == RadarStatus.SUCCESS ){
                    locationManager.updateTrackingFromMeta(config?.meta)
                }
                handler.post {
                    callback?.onComplete(status, location, events, user)
                }
            }
        })
    }

    /**
     * Manually updates the user's location. Note that these calls are subject to rate limits.
     *
     * @see [](https://radar.com/documentation/sdk/android#foreground-tracking)
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
     * Tracks the user's location with device integrity information for location verification use cases.
     *
     * Note that you must configure SSL pinning before calling this method.
     *
     * @see [](https://radar.com/documentation/fraud)
     *
     * @param[beacons] A boolean indicating whether to range beacons.
     * @param[callback] An optional callback.
     */
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    @JvmStatic
    fun trackVerified(beacons: Boolean = false, callback: RadarTrackVerifiedCallback? = null) {
        if (!initialized) {
            callback?.onComplete(RadarStatus.ERROR_PUBLISHABLE_KEY)

            return
        }
        this.logger.i("trackVerified()", RadarLogType.SDK_CALL)

        if (!this::verificationManager.isInitialized) {
            this.verificationManager = RadarVerificationManager(this.context, this.logger)
        }

        this.verificationManager.trackVerified(beacons, callback)
    }

    /**
     * Tracks the user's location with device integrity information for location verification use cases.
     *
     * Note that you must configure SSL pinning before calling this method.
     *
     * @see [](https://radar.com/documentation/fraud)
     *
     * @param[beacons] A boolean indicating whether to range beacons.
     * @param[block] A block callback.
     */
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    @JvmStatic
    fun trackVerified(beacons: Boolean = false, block: (status: RadarStatus, token: RadarVerifiedLocationToken?) -> Unit) {
        trackVerified(beacons, object : RadarTrackVerifiedCallback {
            override fun onComplete(status: RadarStatus, token: RadarVerifiedLocationToken?) {
                block(status, token)
            }
        })
    }

    /**
     * Starts tracking the user's location with device integrity information for location verification use cases.
     *
     * Note that you must configure SSL pinning before calling this method.
     *
     * @see [](https://radar.com/documentation/fraud)
     *
     * @param[interval] The interval in seconds between each location update.
     * @param[beacons] A boolean indicating whether to range beacons.
     */
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    @JvmStatic
    fun startTrackingVerified(interval: Int, beacons: Boolean) {
        if (!initialized) {
            return
        }
        this.logger.i("startTrackingVerified()", RadarLogType.SDK_CALL)

        if (!this::verificationManager.isInitialized) {
            this.verificationManager = RadarVerificationManager(this.context, this.logger)
        }

        this.verificationManager.startTrackingVerified(interval, beacons)
    }

    /**
     * Stops tracking the user's location with device integrity information for location verification use cases.
     *
     * @see [](https://radar.com/documentation/fraud)
     */
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    @JvmStatic
    fun stopTrackingVerified() {
        if (!initialized) {
            return
        }
        this.logger.i("stopTrackingVerified()", RadarLogType.SDK_CALL)

        if (!this::verificationManager.isInitialized) {
            this.verificationManager = RadarVerificationManager(this.context, this.logger)
        }

        this.verificationManager.stopTrackingVerified()
    }

    /**
     * Returns the user's last verified location token if still valid, or requests a fresh token if not.
     *
     * Note that you must configure SSL pinning before calling this method.
     *
     * @see [](https://radar.com/documentation/fraud)
     *
     * @param[callback] An optional callback.
     */
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    @JvmStatic
    fun getVerifiedLocationToken(callback: RadarTrackVerifiedCallback? = null) {
        if (!initialized) {
            callback?.onComplete(RadarStatus.ERROR_PUBLISHABLE_KEY)

            return
        }
        this.logger.i("getVerifiedLocationToken()", RadarLogType.SDK_CALL)

        if (!this::verificationManager.isInitialized) {
            this.verificationManager = RadarVerificationManager(this.context, this.logger)
        }

        this.verificationManager.getVerifiedLocationToken(callback)
    }

    /**
     * Returns the user's last verified location token if still valid, or requests a fresh token if not.
     *
     * Note that you must configure SSL pinning before calling this method.
     *
     * @see [](https://radar.com/documentation/fraud)
     *
     * @param[block] A block callback.
     */
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    @JvmStatic
    fun getVerifiedLocationToken(block: (status: RadarStatus, token: RadarVerifiedLocationToken?) -> Unit) {
        getVerifiedLocationToken(object : RadarTrackVerifiedCallback {
            override fun onComplete(status: RadarStatus, token: RadarVerifiedLocationToken?) {
                block(status, token)
            }
        })
    }

    /**
     * Starts tracking the user's location in the background.
     *
     * @see [](https://radar.com/documentation/sdk/android#background-tracking-for-geofencing)
     *
     * @param[options] Configurable tracking options.
     */
    @JvmStatic
    fun startTracking(options: RadarTrackingOptions) {
        if (!initialized) {
            return
        }
        this.logger.i("startTracking()", RadarLogType.SDK_CALL)
        
        locationManager.startTracking(options)
    }

    /**
     * Mocks tracking the user's location from an origin to a destination.
     *
     * @see [](https://radar.com/documentation/sdk/android#mock-tracking-for-testing)
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

                        apiClient.track(location, stopped, false, RadarLocationSource.MOCK_LOCATION, false, null, callback = object : RadarApiClient.RadarTrackApiCallback {
                            override fun onComplete(
                                status: RadarStatus,
                                res: JSONObject?,
                                events: Array<RadarEvent>?,
                                user: RadarUser?,
                                nearbyGeofences: Array<RadarGeofence>?,
                                config: RadarConfig?,
                                token: RadarVerifiedLocationToken?
                            ) {
                                handler.post {
                                    callback?.onComplete(status, location, events, user)
                                }

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
     * @see [](https://radar.com/documentation/sdk/android#mock-tracking-for-testing)
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
     * @see [](https://radar.com/documentation/sdk/android#background-tracking-for-geofencing)
     */
    @JvmStatic
    fun stopTracking() {
        if (!initialized) {
            return
        }
        this.logger.i("stopTracking()", RadarLogType.SDK_CALL)

        locationManager.stopTracking()
    }

    /**
     * Returns a boolean indicating whether tracking has been started.
     *
     * @see [](https://radar.com/documentation/sdk/android#background-tracking-for-geofencing)
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
     *  Returns a boolean indicating whether the local tracking options are over-ridden by remote tracking options.
     *
     * @return A boolean indicating whether the tracking option is being over-ridden by the remote tracking options.
     */
    @JvmStatic
    fun isUsingRemoteTrackingOptions(): Boolean {
        if (!initialized) {
            return false
        }

        return RadarSettings.getRemoteTrackingOptions(context) != null
    }


   /** 
    *  Returns a string of the radar host.
    *
    *  @return A string of the radar host.
    */
    @JvmStatic
    fun getHost(): String? {
        if (!initialized) {
            return null
        }

        return RadarSettings.getHost(context)
    }

   /**
    * Returns a string of the publishable key.
    *
    * @return A string of the publishable key.
    */
    @JvmStatic
    fun getPublishableKey(): String? {
        if (!initialized) {
            return null
        }

        return RadarSettings.getPublishableKey(context)
    }

    /**
     * Returns the current tracking options.
     *
     * @see [](https://radar.com/documentation/sdk/tracking)
     *
     * @return The current tracking options.
     */
    @JvmStatic
    fun getTrackingOptions() = RadarSettings.getRemoteTrackingOptions(context)
        ?: RadarSettings.getTrackingOptions(context)

    /**
     * Settings for the foreground notification when the foregroundServiceEnabled parameter
     * is true on Radar tracking options.
     *
     * @see [](https://radar.com/documentation/sdk/tracking)
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
     * Settings for the all notifications.
     *
     * @see [](https://radar.com/documentation/sdk)
     *
     * @param[options] Notifications options
     */
    @JvmStatic
    fun setNotificationOptions(options: RadarNotificationOptions) {
        if (!initialized) {
            return
        }

        RadarSettings.setNotificationOptions(context, options)
    }


    /**
     * Sets a receiver for client-side delivery of events, location updates, and debug logs.
     *
     * @see [](https://radar.com/documentation/sdk/android#listening-for-events-with-a-receiver)
     *
     * @param[receiver] A delegate for client-side delivery of events, location updates, and debug logs. If `null`, the previous receiver will be cleared.
     */
    @JvmStatic
    fun setReceiver(receiver: RadarReceiver?) {
        if (!initialized) {
            return
        }

        this.receiver = receiver
    }

    /**
     * Sets a receiver for client-side delivery of verified location tokens.
     *
     * @see [](https://radar.com/documentation/sdk/fraud)
     *
     * @param[verifiedReceiver] A delegate for client-side delivery of of verified location tokens. If `null`, the previous receiver will be cleared.
     */
    @JvmStatic
    fun setVerifiedReceiver(verifiedReceiver: RadarVerifiedReceiver?) {
        if (!initialized) {
            return
        }

        this.verifiedReceiver = verifiedReceiver
    }

    /**
     * Accepts an event. Events can be accepted after user check-ins or other forms of verification. Event verifications will be used to improve the accuracy and confidence level of future events.
     *
     * @see [](https://radar.com/documentation/places#verify-events)
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
     * @see [](https://radar.com/documentation/places#verify-events)
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
     * @see [](https://radar.com/documentation/trip-tracking)
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
     * @see [](https://radar.com/documentation/trip-tracking)
     *
     * @param[options] Configurable trip options.
     * @param[callback] An optional callback.
     */
    @JvmStatic
    fun startTrip(options: RadarTripOptions, callback: RadarTripCallback? = null) {
        startTrip(options, null, callback)
    }

    /**
     * Starts a trip.
     *
     * @see [](https://radar.com/documentation/trip-tracking)
     *
     * @param[options] Configurable trip options.
     * @param[trackingOptions] Tracking options to use during the trip
     * @param[callback] An optional callback.
     */
    @JvmStatic
    fun startTrip(options: RadarTripOptions, trackingOptions: RadarTrackingOptions? = null, callback: RadarTripCallback? = null) {
        if (!initialized) {
            return
        }
        this.logger.i("startTrip()", RadarLogType.SDK_CALL)

        apiClient.createTrip(options, object : RadarApiClient.RadarTripApiCallback {
            override fun onComplete(
                status: RadarStatus,
                res: JSONObject?,
                trip: RadarTrip?,
                events: Array<RadarEvent>?
            ) {
                if (status == RadarStatus.SUCCESS) {
                    RadarSettings.setTripOptions(context, options)

                    val isTracking = Radar.isTracking()
                    if (isTracking) {
                        val previousTrackingOptions = RadarSettings.getTrackingOptions(context)
                        RadarSettings.setPreviousTrackingOptions(context, previousTrackingOptions)
                    } else {
                        RadarSettings.removePreviousTrackingOptions(context)
                    }

                    if (trackingOptions != null) {
                        Radar.startTracking(trackingOptions)
                    } else if (!isTracking) {
                        Radar.startTracking(RadarSettings.getRemoteTrackingOptions(context) ?: RadarSettings.getTrackingOptions(context))
                    }

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
     * @see [](https://radar.com/documentation/trip-tracking)
     *
     * @param[options] Configurable trip options.
     * @param[block] An optional block callback.
     */
    @JvmStatic
    fun startTrip(options: RadarTripOptions, block: (status: RadarStatus, trip: RadarTrip?, events: Array<RadarEvent>?) -> Unit) {
        startTrip(options, null, object : RadarTripCallback {
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
     * Starts a trip.
     *
     * @see [](https://radar.com/documentation/trip-tracking)
     *
     * @param[options] Configurable trip options.
     * @param[trackingOptions] Tracking options to use on trip.
     * @param[block] An optional block callback.
     */
    @JvmStatic
    fun startTrip(options: RadarTripOptions, trackingOptions: RadarTrackingOptions?, block: (status: RadarStatus, trip: RadarTrip?, events: Array<RadarEvent>?) -> Unit) {
        startTrip(options, trackingOptions, object : RadarTripCallback {
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
     * @see [](https://radar.com/documentation/trip-tracking)
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
        this.logger.i("updateTrip()", RadarLogType.SDK_CALL)

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
     * @see [](https://radar.com/documentation/trip-tracking)
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
     * @see [](https://radar.com/documentation/trip-tracking)
     */
    @JvmStatic
    fun completeTrip(callback: RadarTripCallback? = null) {
        if (!initialized) {
            return
        }
        this.logger.i("completeTrip()", RadarLogType.SDK_CALL)

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

                    // return to previous tracking options after trip
                    locationManager.restartPreviousTrackingOptions();

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
     * @see [](https://radar.com/documentation/trip-tracking)
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
     * @see [](https://radar.com/documentation/trip-tracking)
     */
    @JvmStatic
    fun cancelTrip(callback: RadarTripCallback? = null) {
        if (!initialized) {
            return
        }
        this.logger.i("cancelTrip()", RadarLogType.SDK_CALL)

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

                    // return to previous tracking options after trip
                    locationManager.restartPreviousTrackingOptions();

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
     * @see [](https://radar.com/documentation/trip-tracking)
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
     * @see [](https://radar.com/documentation/api#search-places)
     *
     * @param[radius] The radius to search, in meters. A number between 100 and 10000.
     * @param[chains] An array of chain slugs to filter. See [](https://radar.com/documentation/places/chains)
     * @param[categories] An array of categories to filter. See [](https://radar.com/documentation/places/categories)
     * @param[groups] An array of groups to filter. See [](https://radar.com/documentation/places/groups)
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
        searchPlaces(radius, chains, null, categories, groups, limit, callback)
    }

    /**
     * Gets the device's current location, then searches for places (with or without chain-specific
     * metadata) near that location, sorted by distance.
     *
     * @see [](https://radar.io/documentation/api#search-places)
     *
     * @param[radius] The radius to search, in meters. A number between 100 and 10000.
     * @param[chains] An array of chain slugs to filter. See [](https://radar.io/documentation/places/chains)
     * @param[chainMetadata] A map of metadata keys and values. Values can be strings, numerics, or booleans.
     * @param[categories] An array of categories to filter. See [](https://radar.io/documentation/places/categories)
     * @param[groups] An array of groups to filter. See [](https://radar.io/documentation/places/groups)
     * @param[limit] The max number of places to return. A number between 1 and 100.
     * @param[callback] A callback.
     */
    @JvmStatic
    fun searchPlaces(
        radius: Int,
        chains: Array<String>?,
        chainMetadata: Map<String, String>?,
        categories: Array<String>?,
        groups: Array<String>?,
        limit: Int?,
        callback: RadarSearchPlacesCallback
    ) {
        if (!initialized) {
            callback.onComplete(RadarStatus.ERROR_PUBLISHABLE_KEY)

            return
        }
        this.logger.i("searchPlaces()", RadarLogType.SDK_CALL)

        locationManager.getLocation(object : RadarLocationCallback {
            override fun onComplete(status: RadarStatus, location: Location?, stopped: Boolean) {
                if (status != RadarStatus.SUCCESS || location == null) {
                    handler.post {
                        callback.onComplete(status)
                    }

                    return
                }

                apiClient.searchPlaces(location, radius, chains, chainMetadata, categories, groups, limit, object : RadarApiClient.RadarSearchPlacesApiCallback {
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
     * @see [](https://radar.com/documentation/api#search-places)
     *
     * @param[radius] The radius to search, in meters. A number between 100 and 10000.
     * @param[chains] An array of chain slugs to filter. See [](https://radar.com/documentation/places/chains)
     * @param[categories] An array of categories to filter. See [](https://radar.com/documentation/places/categories)
     * @param[groups] An array of groups to filter. See [](https://radar.com/documentation/places/groups)
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
        searchPlaces(radius, chains, null, categories, groups, limit, block)
    }

    /**
     * Gets the device's current location, then searches for places (with or without chain-specific
     * metadata) near that location, sorted by distance.
     *
     * @see [](https://radar.io/documentation/api#search-places)
     *
     * @param[radius] The radius to search, in meters. A number between 100 and 10000.
     * @param[chains] An array of chain slugs to filter. See [](https://radar.io/documentation/places/chains)
     * @param[chainMetadata] A map of metadata keys and values. Values can be strings, numerics, or booleans.
     * @param[categories] An array of categories to filter. See [](https://radar.io/documentation/places/categories)
     * @param[groups] An array of groups to filter. See [](https://radar.io/documentation/places/groups)
     * @param[limit] The max number of places to return. A number between 1 and 100.
     * @param[block] A block callback.
     */
    fun searchPlaces(
        radius: Int,
        chains: Array<String>?,
        chainMetadata: Map<String, String>?,
        categories: Array<String>?,
        groups: Array<String>?,
        limit: Int?,
        block: (status: RadarStatus, location: Location?, places: Array<RadarPlace>?) -> Unit
    ) {
        searchPlaces(
            radius,
            chains,
            chainMetadata,
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
     * @see [](https://radar.com/documentation/api#search-places)
     *
     * @param[near] The location to search.
     * @param[radius] The radius to search, in meters. A number between 100 and 10000.
     * @param[chains] An array of chain slugs to filter. See [](https://radar.com/documentation/places/chains)
     * @param[categories] An array of categories to filter. See [](https://radar.com/documentation/places/categories)
     * @param[groups] An array of groups to filter. See [](https://radar.com/documentation/places/groups)
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
        searchPlaces(near, radius, chains, null, categories, groups, limit, callback)
    }

    /**
     * Search for places (with or without chain-specific metadata) near a location, sorted by
     * distance.
     *
     * @see [](https://radar.io/documentation/api#search-places)
     *
     * @param[near] The location to search.
     * @param[radius] The radius to search, in meters. A number between 100 and 10000.
     * @param[chains] An array of chain slugs to filter. See [](https://radar.io/documentation/places/chains)
     * @param[chainMetadata] A map of metadata keys and values. Values can be strings, numerics, or booleans.
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
        chainMetadata: Map<String, String>?,
        categories: Array<String>?,
        groups: Array<String>?,
        limit: Int?,
        callback: RadarSearchPlacesCallback
    ) {
        if (!initialized) {
            callback.onComplete(RadarStatus.ERROR_PUBLISHABLE_KEY)

            return
        }
        this.logger.i("searchPlaces()", RadarLogType.SDK_CALL)

        apiClient.searchPlaces(near, radius, chains, chainMetadata, categories, groups, limit, object : RadarApiClient.RadarSearchPlacesApiCallback {
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
     * @see [](https://radar.com/documentation/api#search-places)
     *
     * @param[near] The location to search.
     * @param[radius] The radius to search, in meters. A number between 100 and 10000.
     * @param[chains] An array of chain slugs to filter. See [](https://radar.com/documentation/places/chains)
     * @param[categories] An array of categories to filter. See [](https://radar.com/documentation/places/categories)
     * @param[groups] An array of groups to filter. See [](https://radar.com/documentation/places/groups)
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
        searchPlaces(near, radius, chains, null, categories, groups, limit, block)
    }

    /**
     * Search for places (with or without chain-specific metadata) near a location, sorted by
     * distance.
     *
     * @see [](https://radar.io/documentation/api#search-places)
     *
     * @param[near] The location to search.
     * @param[radius] The radius to search, in meters. A number between 100 and 10000.
     * @param[chains] An array of chain slugs to filter. See [](https://radar.io/documentation/places/chains)
     * @param[chainMetadata] A map of metadata keys and values. Values can be strings, numerics, or booleans.
     * @param[categories] An array of categories to filter. See [](https://radar.io/documentation/places/categories)
     * @param[groups] An array of groups to filter. See [](https://radar.io/documentation/places/groups)
     * @param[limit] The max number of places to return. A number between 1 and 100.
     * @param[block] A block callback.
     */
    fun searchPlaces(
        near: Location,
        radius: Int,
        chains: Array<String>?,
        chainMetadata: Map<String, String>?,
        categories: Array<String>?,
        groups: Array<String>?,
        limit: Int?,
        block: (status: RadarStatus, location: Location?, places: Array<RadarPlace>?) -> Unit
    ) {
        searchPlaces(
            near,
            radius,
            chains,
            chainMetadata,
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
     * @see [](https://radar.com/documentation/api#search-geofences)
     *
     * @param[radius] The optional radius to search, in meters. A number between 100 and 10000. If `null`, the server defaults to searching without a radius limit.
     * @param[tags] An array of tags to filter. See [](https://radar.com/documentation/geofences)
     * @param[metadata] A dictionary of metadata to filter. See [](https://radar.com/documentation/geofences)
     * @param[limit] The max number of places to return. A number between 1 and 1000. Defaults to 100.
     * @param[includeGeometry] Include geofence geometries in the response. Recommended to be set to false unless you specifically need the geometries. To retrieve more than 100 results, `includeGeometry` must be set to `false`.
     * @param[callback] A callback.
     */
    @JvmStatic
    fun searchGeofences(
        radius: Int?,
        tags: Array<String>?,
        metadata: JSONObject?,
        limit: Int?,
        includeGeometry: Boolean? = false,
        callback: RadarSearchGeofencesCallback
    ) {
        if (!initialized) {
            callback.onComplete(RadarStatus.ERROR_PUBLISHABLE_KEY)

            return
        }
        this.logger.i("searchGeofences()", RadarLogType.SDK_CALL)

        locationManager.getLocation(object : RadarLocationCallback {
            override fun onComplete(status: RadarStatus, location: Location?, stopped: Boolean) {
                if (status != RadarStatus.SUCCESS || location == null) {
                    handler.post {
                        callback.onComplete(status)
                    }

                    return
                }

                apiClient.searchGeofences(location, radius, tags, metadata, limit, includeGeometry, object : RadarApiClient.RadarSearchGeofencesApiCallback {
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
     * @see [](https://radar.com/documentation/api#search-geofences)
     *
     * @param[radius] The optional radius to search, in meters. A number between 100 and 10000. If `null`, the server defaults to searching without a radius limit.
     * @param[tags] An array of tags to filter. See [](https://radar.com/documentation/geofences)
     * @param[metadata] A dictionary of metadata to filter. See [](https://radar.com/documentation/geofences)
     * @param[limit] The max number of places to return. A number between 1 and 1000. Defaults to 100.
     * @param[includeGeometry] Include geofence geometries in the response. Recommended to be set to false unless you specifically need the geometries. To retrieve more than 100 results, `includeGeometry` must be set to `false`.
     * @param[block] A block callback.
     */
    fun searchGeofences(
        radius: Int?,
        tags: Array<String>?,
        metadata: JSONObject?,
        limit: Int?,
        includeGeometry: Boolean? = false,
        block: (status: RadarStatus, location: Location?, geofences: Array<RadarGeofence>?) -> Unit
    ) {
        searchGeofences(
            radius,
            tags,
            metadata,
            limit,
            includeGeometry,
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
     * @see [](https://radar.com/documentation/api#search-geofences)
     *
     * @param[near] The location to search.
     * @param[radius] The optional radius to search, in meters. A number between 100 and 10000. If `null`, the server defaults to searching without a radius limit.
     * @param[tags] An array of tags to filter. See [](https://radar.com/documentation/geofences)
     * @param[metadata] A dictionary of metadata to filter. See [](https://radar.com/documentation/geofences)
     * @param[limit] The max number of places to return. A number between 1 and 1000. Defaults to 100.
     * @param[includeGeometry] Include geofence geometries in the response. Recommended to be set to false unless you specifically need the geometries. To retrieve more than 100 results, `includeGeometry` must be set to `false`.
     * @param[callback] A callback.
     */
    @JvmStatic
    fun searchGeofences(
        near: Location,
        radius: Int?,
        tags: Array<String>?,
        metadata: JSONObject?,
        limit: Int?,
        includeGeometry: Boolean? = false,
        callback: RadarSearchGeofencesCallback
    ) {
        if (!initialized) {
            callback.onComplete(RadarStatus.ERROR_PUBLISHABLE_KEY)

            return
        }
        this.logger.i("searchGeofences()", RadarLogType.SDK_CALL)

        apiClient.searchGeofences(near, radius, tags, metadata, limit, includeGeometry, object : RadarApiClient.RadarSearchGeofencesApiCallback {
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
     * @see [](https://radar.com/documentation/api#search-geofences)
     *
     * @param[near] The location to search.
     * @param[radius] The optional radius to search, in meters. A number between 100 and 10000. If `null`, the server defaults to searching without a radius limit.
     * @param[tags] An array of tags to filter. See [](https://radar.com/documentation/geofences)
     * @param[metadata] A dictionary of metadata to filter. See [](https://radar.com/documentation/geofences)
     * @param[limit] The max number of places to return. A number between 1 and 1000. Defaults to 100.
     * @param[includeGeometry] Include geofence geometries in the response. Recommended to be set to false unless you specifically need the geometries. To retrieve more than 100 results, `includeGeometry` must be set to `false`.
     * @param[block] A block callback.
     */
    fun searchGeofences(
        near: Location,
        radius: Int?,
        tags: Array<String>?,
        metadata: JSONObject?,
        limit: Int?,
        includeGeometry: Boolean? = false,
        block: (status: RadarStatus, location: Location?, geofences: Array<RadarGeofence>?) -> Unit
    ) {
        searchGeofences(
            near,
            radius,
            tags,
            metadata,
            limit,
            includeGeometry,
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
     * @see [](https://radar.com/documentation/api#autocomplete)
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
        this.logger.i("autocomplete()", RadarLogType.SDK_CALL)

        apiClient.autocomplete(query, near, null, limit, null, null, object : RadarApiClient.RadarGeocodeApiCallback {
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
     * @see [](https://radar.com/documentation/api#autocomplete)
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
     * @see [](https://radar.com/documentation/api#autocomplete)
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
        this.logger.i("autocomplete()", RadarLogType.SDK_CALL)

        apiClient.autocomplete(query, near, layers, limit, country, null, object : RadarApiClient.RadarGeocodeApiCallback {
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
     * @see [](https://radar.com/documentation/api#autocomplete)
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
     * Autocompletes partial addresses and place names, sorted by relevance.
     *
     * @see [](https://radar.com/documentation/api#autocomplete)
     *
     * @param[query] The partial address or place name to autocomplete.
     * @param[near] A location for the search.
     * @param[layers] Optional layer filters.
     * @param[limit] The max number of addresses to return. A number between 1 and 100.
     * @param[country] An optional country filter. A string, the unique 2-letter country code.
     * @param[expandUnits] (Deprecated) This is always true, regardless of the value passed here.
     * @param[mailable] Whether to only include mailable addresses.
     * @param[callback] A callback.
     */
    @JvmStatic
    fun autocomplete(
        query: String,
        near: Location? = null,
        layers: Array<String>? = null,
        limit: Int? = null,
        country: String? = null,
        expandUnits: Boolean? = null,
        mailable: Boolean? = null,
        callback: RadarGeocodeCallback
    ) {
        if (!initialized) {
            callback.onComplete(RadarStatus.ERROR_PUBLISHABLE_KEY)

            return
        }

        apiClient.autocomplete(query, near, layers, limit, country, mailable, object : RadarApiClient.RadarGeocodeApiCallback {
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
     * @see [](https://radar.com/documentation/api#autocomplete)
     *
     * @param[query] The partial address or place name to autocomplete.
     * @param[near] A location for the search.
     * @param[layers] Optional layer filters.
     * @param[limit] The max number of addresses to return. A number between 1 and 100.
     * @param[country] An optional country filter. A string, the unique 2-letter country code.
     * @param[expandUnits] (Deprecated) This is always true, regardless of the value passed here.
     * @param[mailable] Whether to only include mailable addresses
     * @param[block] A block callback.
     */

    fun autocomplete(
        query: String,
        near: Location? = null,
        layers: Array<String>? = null,
        limit: Int? = null,
        country: String? = null,
        expandUnits: Boolean? = null,
        mailable: Boolean? = null,
        block: (status: RadarStatus, addresses: Array<RadarAddress>?) -> Unit
    ) {
        autocomplete(
            query,
            near,
            layers,
            limit,
            country,
            expandUnits,
            mailable,
            object : RadarGeocodeCallback {
                override fun onComplete(status: RadarStatus, addresses: Array<RadarAddress>?) {
                    block(status, addresses)
                }
            }
        )
    }

    /**
     * Validates an address, attaching to a verification status, property type, and plus4.
     * 
     * @see [](https://radar.com/documentation/api#validate-an-address)
     * 
     * @param[address] The address to validate.
     * @param[callback] A callback.
     * 
     */

    @JvmStatic
    fun validateAddress(
        address: RadarAddress?,
        callback: RadarValidateAddressCallback
    ) {
        if (!initialized) {
            callback.onComplete(RadarStatus.ERROR_PUBLISHABLE_KEY)

            return
        }

        if (address == null) {
            callback.onComplete(RadarStatus.ERROR_BAD_REQUEST, null)

            return
        }

        apiClient.validateAddress(address, object: RadarApiClient.RadarValidateAddressAPICallback {
            override fun onComplete(status: RadarStatus, res: JSONObject?, address: RadarAddress?, verificationStatus: RadarAddressVerificationStatus?) {
                handler.post {
                    callback.onComplete(status, address, verificationStatus)
                }
            }
        })
    }

    /**
     * Validates an address, attaching to a verification status, property type, and plus4.
     * 
     * @see [](https://radar.com/documentation/api#validate-an-address)
     * 
     * @param[address] The address to validate.
     * @param[block] A block callback.
     * 
     */

    fun validateAddress(
        address: RadarAddress?,
        block: (status: RadarStatus, address: RadarAddress?, verificationStatus: RadarAddressVerificationStatus?) -> Unit
    ) {
        validateAddress(
            address,
            object : RadarValidateAddressCallback {
                override fun onComplete(status: RadarStatus, address: RadarAddress?, verificationStatus: RadarAddressVerificationStatus?) {
                    block(status, address, verificationStatus)
                }
            }
        )
    }

    /**
     * Geocodes an address, converting address to coordinates.
     *
     * @see [](https://radar.com/documentation/api#forward-geocode)
     *
     * @param[query] The address to geocode.
     * @param[layers] Optional layer filters.
     * @param[countries] Optional country filters. A string array of unique 2-letter country codes.
     * @param[callback] A callback.
     */
    @JvmStatic
    fun geocode(
        query: String,
        layers: Array<String>? = null,
        countries: Array<String>? = null,
        callback: RadarGeocodeCallback
    ) {
        if (!initialized) {
            callback.onComplete(RadarStatus.ERROR_PUBLISHABLE_KEY)

            return
        }
        this.logger.i("geocode()", RadarLogType.SDK_CALL)

        apiClient.geocode(query, layers, countries, object: RadarApiClient.RadarGeocodeApiCallback {
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
     * @see [](https://radar.com/documentation/api#forward-geocode)
     *
     * @param[query] The address to geocode.
     * @param[layers] Optional layer filters.
     * @param[countries] Optional country filters. A string array of unique 2-letter country codes.
     * @param[block] A block callback.
     */
    fun geocode(
        query: String,
        layers: Array<String>? = null,
        countries: Array<String>? = null,
        block: (status: RadarStatus, addresses: Array<RadarAddress>?) -> Unit
    ) {
        geocode(
            query,
            layers,
            countries,
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
     * @see [](https://radar.com/documentation/api#reverse-geocode)
     *
     * @param[layers] Optional layer filters.
     * @param[callback] A callback.
     */
    @JvmStatic
    fun reverseGeocode(
        layers: Array<String>? = null,
        callback: RadarGeocodeCallback
    ) {
        if (!initialized) {
            callback.onComplete(RadarStatus.ERROR_PUBLISHABLE_KEY)

            return
        }
        this.logger.i("reverseGeocode()", RadarLogType.SDK_CALL)

        locationManager.getLocation(object: RadarLocationCallback {
            override fun onComplete(status: RadarStatus, location: Location?, stopped: Boolean) {
                if (status != RadarStatus.SUCCESS || location == null) {
                    handler.post {
                        callback.onComplete(status)
                    }

                    return
                }

                apiClient.reverseGeocode(location, layers, object: RadarApiClient.RadarGeocodeApiCallback {
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
     * @see [](https://radar.com/documentation/api#reverse-geocode)
     *
     * @param[layers] Optional layer filters.
     * @param[block] A block callback.
     */
    fun reverseGeocode(
        layers: Array<String>? = null,
        block: (status: RadarStatus, addresses: Array<RadarAddress>?) -> Unit
    ) {
        reverseGeocode(
            layers,
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
     * @see [](https://radar.com/documentation/api#reverse-geocode)
     *
     * @param[location] The location to reverse geocode.
     * @param[layers] Optional layer filters.
     * @param[callback] A callback.
     */
    @JvmStatic
    fun reverseGeocode(
        location: Location,
        layers: Array<String>? = null,
        callback: RadarGeocodeCallback
    ) {
        if (!initialized) {
            callback.onComplete(RadarStatus.ERROR_PUBLISHABLE_KEY)

            return
        }
        this.logger.i("reverseGeocode()", RadarLogType.SDK_CALL)

        apiClient.reverseGeocode(location, layers, object: RadarApiClient.RadarGeocodeApiCallback {
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
     * @see [](https://radar.com/documentation/api#reverse-geocode)
     *
     * @param[location] The location to geocode.
     * @param[layers] Optional 
     * @param[block] A block callback.
     */
    fun reverseGeocode(
        location: Location,
        layers: Array<String>? = null,
        block: (status: RadarStatus, addresses: Array<RadarAddress>?) -> Unit
    ) {
        reverseGeocode(
            location,
            layers,
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
     * @see [](https://radar.com/documentation/api#ip-geocode)
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
        this.logger.i("ipGeocode()", RadarLogType.SDK_CALL)

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
     * @see [](https://radar.com/documentation/api#ip-geocode)
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
     * @see [](https://radar.com/documentation/api#distance)
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
        this.logger.i("getDistance()", RadarLogType.SDK_CALL)

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
     * @see [](https://radar.com/documentation/api#distance)
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
     * @see [](https://radar.com/documentation/api#distance)
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
        this.logger.i("getDistance()", RadarLogType.SDK_CALL)

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
     * @see [](https://radar.com/documentation/api#distance)
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
     * @see [](https://radar.com/documentation/api#matrix)
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
        this.logger.i("getMatrix()", RadarLogType.SDK_CALL)

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
     * @see [](https://radar.com/documentation/api#matrix)
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
        this.logger.i("getContext()", RadarLogType.SDK_CALL)

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
        this.logger.i("getContext()", RadarLogType.SDK_CALL)

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
     * Logs a conversion.
     *
     * @see [](https://radar.com/documentation/api#send-a-custom-event)
     *
     * @param[name] The name of the conversion.
     * @param[metadata] The metadata associated with the conversion.
     * @param[callback] A callback.
     */
    @JvmStatic
    fun logConversion(name: String, metadata: JSONObject? = null, callback: RadarLogConversionCallback) {
        if (!initialized) {
            callback.onComplete(RadarStatus.ERROR_PUBLISHABLE_KEY)

            return
        }

        // if track() has been returned in the last 60 seconds, don't call it again
        val timestampSeconds = System.currentTimeMillis() / 1000
        val lastTrackedTime = RadarSettings.getLastTrackedTime(context)
        val isLastTrackRecent = timestampSeconds - lastTrackedTime < 60
        val doesNotHaveLocationPermissions =
            !locationManager.permissionsHelper.fineLocationPermissionGranted(context)
                    && !locationManager.permissionsHelper.coarseLocationPermissionGranted(context)

        if (isLastTrackRecent || doesNotHaveLocationPermissions) {
            sendLogConversionRequest(name, metadata, callback = callback)

            return
        }

        trackOnce(object : RadarTrackCallback {
            override fun onComplete(status: RadarStatus, location: Location?, events: Array<RadarEvent>?, user: RadarUser?) {
                if (status != RadarStatus.SUCCESS || location == null) {
                    handler.post {
                        callback.onComplete(status)
                    }

                    return
                }

                sendLogConversionRequest(name, metadata, callback)
            }
        })
    }

    /**
     * Logs a conversion.
     *
     * @see [](https://radar.com/documentation/api#send-a-custom-event)
     *
     * @param[name] The name of the conversion.
     * @param[metadata] The metadata associated with the conversion.
     * @param[block] A block callback
     */
    @JvmStatic
    fun logConversion(
        name: String,
        metadata: JSONObject? = null,
        block: (status: RadarStatus, event: RadarEvent?) -> Unit
    ) {
        logConversion(name, metadata, object : RadarLogConversionCallback {
            override fun onComplete(
                status: RadarStatus,
                event: RadarEvent?
            ) {
                block(status, event)
            }
        })
    }

    /**
     * Logs a conversion with revenue.
     *
     * @see [](https://radar.com/documentation/api#send-a-custom-event)
     *
     * @param[name] The name of the conversion.
     * @param[revenue] The revenue generated by the conversion.
     * @param[metadata] The metadata associated with the conversion.
     * @param[callback] A callback.
     */
    @JvmStatic
    fun logConversion(
        name: String,
        revenue: Double,
        metadata: JSONObject? = null,
        callback: RadarLogConversionCallback
    ) {
        val nonNullMetadata = metadata ?: JSONObject()
        nonNullMetadata.put("revenue", revenue);

        logConversion(name, nonNullMetadata, callback)
    }

    /**
     * Logs a conversion with revenue.
     *
     * @see [](https://radar.com/documentation/api#send-a-custom-event)
     *
     * @param[name] The name of the conversion.
     * @param[revenue] The revenue generated by the conversion.
     * @param[metadata] The metadata associated with the conversion.
     * @param[block] A block callback.
     */
    @JvmStatic
    fun logConversion(
        name: String,
        revenue: Double,
        metadata: JSONObject? = null,
        block: (status: RadarStatus, RadarEvent?) -> Unit
    ) {
        logConversion(name, revenue, metadata, object : RadarLogConversionCallback {
            override fun onComplete(
                status: RadarStatus,
                event: RadarEvent?
            ) {
                block(status, event)
            }
        })
    }

    @JvmStatic
    internal fun sendLogConversionRequest(
        name: String,
        metadata: JSONObject? = null,
        callback: RadarLogConversionCallback
    ) {
        apiClient.sendEvent(
            name,
            metadata,
            object : RadarApiClient.RadarSendEventApiCallback {
                override fun onComplete(
                    status: RadarStatus,
                    res: JSONObject?,
                    event: RadarEvent?
                ) {
                    if (status != RadarStatus.SUCCESS) {
                        handler.post {
                            callback.onComplete(status)
                        }

                        return
                    }

                    handler.post {
                        callback.onComplete(status, event)
                    }
                }
            })
    }

    internal fun logOpenedAppConversion() {
        // if opened_app has been logged in the last 1000 milliseconds, don't log it again
        val timestamp = System.currentTimeMillis()
        val lastAppOpenTime = RadarSettings.getLastAppOpenTimeMillis(context)
        if (timestamp - lastAppOpenTime > 1000) {
            RadarSettings.updateLastAppOpenTimeMillis(context)
            sendLogConversionRequest("opened_app", callback = object : RadarLogConversionCallback {
                override fun onComplete(status: RadarStatus, event: RadarEvent?) {
                    logger.i("Conversion name = ${event?.conversionName}: status = $status; event = $event")
                }
            })
        }
    }
    /**
     * Requests foreground location permissions.
     */
    @JvmStatic
    fun requestForegroundLocationPermission() {
        locationPermissionManager.requestForegroundLocationPermission()
    }

    /**
     * Requests background location permissions.
     */
    @JvmStatic
    fun requestBackgroundLocationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            locationPermissionManager.requestBackgroundLocationPermission()
        }
    }

    /**
     * @return A RadarPermissionStatus object with the current location permissions status.
     */
    @JvmStatic
    fun getLocationPermissionStatus():RadarLocationPermissionStatus {
        return locationPermissionManager.getLocationPermissionStatus()
    }

    /**
     * Directs the user to the app settings to enable location permissions.
     */
    @JvmStatic
    fun openAppSettings() {
        locationPermissionManager.openAppSettings()
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
     Log application resigning active.
    */
    @JvmStatic
    fun logResigningActive() {
        if (!initialized) {
            return
        }
        this.logger.logResigningActive()
    }

    /**
    Log application entering background and flush logs in memory buffer into persistent buffer.
    */
     @JvmStatic
    fun logBackgrounding() {
        if (!initialized) {
            return
        }
        this.logger.logBackgrounding()
        this.logBuffer.persistLogs()
    }

    /**
     * Flushes debug logs to the server.
     */
    @JvmStatic
    internal fun flushLogs() {
        if (!initialized || !isTestKey()) {
            return
        }

        val flushable = logBuffer.getFlushableLogs()
        val logs = flushable.get()
        if (logs.isNotEmpty()) {
            apiClient.log(logs, object : RadarApiClient.RadarLogCallback {
                override fun onComplete(status: RadarStatus, res: JSONObject?) {
                    flushable.onFlush(status == RadarStatus.SUCCESS)
                }
            })
        }
    }

    /**
     * Flushes replays to the server.
     */
    @JvmStatic
    internal fun flushReplays(replayParams: JSONObject? = null, callback: RadarTrackCallback? = null) {
        if (!initialized) {
            return
        }

        if (isFlushingReplays) {
            this.logger.d("Already flushing replays")
            callback?.onComplete(RadarStatus.ERROR_SERVER)
            return
        }

        // check if any replays to flush
        if (!hasReplays() && replayParams == null) {
            this.logger.d("No replays to flush")
            return
        }
    
        this.isFlushingReplays = true

        // get a copy of the replays so we can safely clear what was synced up
        val replaysStash = replayBuffer.getFlushableReplaysStash()
        val replays = replaysStash.get().toMutableList()

        // if we have a current track update, mark it as replayed and add to local list
        if (replayParams != null) {
            replayParams.putOpt("replayed", true)
            replayParams.putOpt("updatedAtMs", System.currentTimeMillis())
            replayParams.remove("updatedAtMsDiff")

            replays.add(RadarReplay(replayParams))
        }

        val replayCount = replays.size
        this.logger.d("Flushing $replayCount replays")

        apiClient.replay(replays, object : RadarApiClient.RadarReplayApiCallback {
            override fun onComplete(status: RadarStatus, res: JSONObject?) {
                if (status == RadarStatus.SUCCESS) {
                    logger.d("Successfully flushed replays")
                    replaysStash.onFlush(true) // clear from buffer what was synced
                    Radar.flushLogs()
                } else {
                    if (replayParams != null) {
                        logger.d("Failed to flush replays, adding track update to buffer")
                        Radar.addReplay(replayParams)
                    }
                }
                Radar.isFlushingReplays = false
                handler.post {
                    callback?.onComplete(status)
                }
            }
        })
    }

    @JvmStatic
    internal fun hasReplays(): Boolean {
        val replayCount = replayBuffer.getSize()
        return replayCount > 0
    }

    @JvmStatic
    internal fun addReplay(replayParams: JSONObject) {
        replayBuffer.write(replayParams)
    }

    @JvmStatic
    internal fun loadReplayBufferFromSharedPreferences() {
        replayBuffer.loadFromSharedPreferences()
        val replayCount = replayBuffer.getSize()
        logger.d("Loaded replays | replayCount = $replayCount")
    }

    @JvmStatic
    internal fun isTestKey(): Boolean {
        val key = RadarSettings.getPublishableKey(this.context)
        val userDebug = RadarSettings.getUserDebug(this.context)
        return if (key == null) {
            false
        } else {
            key.startsWith("prj_test") || key.startsWith("org_test") || userDebug
        }
    }

    /**
     * Returns a display string for a location source value.
     *
     * @param[source] A location source value.
     *
     * @return A display string for the location source value.
     */
    @JvmStatic
    fun stringForSource(source: RadarLocationSource): String {
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
    fun stringForMode(mode: RadarRouteMode): String {
        return when (mode) {
            RadarRouteMode.FOOT -> "foot"
            RadarRouteMode.BIKE -> "bike"
            RadarRouteMode.CAR -> "car"
            RadarRouteMode.TRUCK -> "truck"
            RadarRouteMode.MOTORBIKE -> "motorbike"
            else -> "car"
        }
    }
   
    /**
     * Returns a display string for a verification status value.
     *
     * @param[verificationStatus] A verification status value.
     *
     * @return A display string for the address verification status value.
     */
    @JvmStatic
    fun stringForVerificationStatus(verificationStatus: RadarAddressVerificationStatus? = null ): String {
        if (verificationStatus == null) {
            return "UNKNOWN"
        }
        return when(verificationStatus) {
            RadarAddressVerificationStatus.VERIFIED -> "VERIFIED"
            RadarAddressVerificationStatus.PARTIALLY_VERIFIED -> "PARTIALLY_VERIFIED"
            RadarAddressVerificationStatus.AMBIGUOUS -> "AMBIGUOUS"
            RadarAddressVerificationStatus.UNVERIFIED -> "UNVERIFIED"
            else -> "UNKNOWN"
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
    fun stringForTripStatus(status: RadarTrip.RadarTripStatus): String {
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

    /**
     * Gets the version number of the Radar SDK, such as "3.5.1" or "3.5.1-beta.2".
     *
     * @return The current `sdkVersion`.
    */
    @JvmStatic
    fun sdkVersion() : String{

        return RadarUtils.sdkVersion

    }


    internal fun handleLocation(context: Context, location: Location, source: RadarLocationSource) {
        if (!initialized) {
            initialize(context)
        }

        locationManager.handleLocation(location, source)
    }

    internal fun handleBeacons(context: Context, beacons: Array<RadarBeacon>?, source: RadarLocationSource) {
        if (!initialized) {
            initialize(context)
        }

        locationManager.handleBeacons(beacons, source)
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

        RadarNotificationHelper.showNotifications(context, events)

        for (event in events) {
            logger.i("📍 Radar event received | type = ${RadarEvent.stringForType(event.type)}; replayed = ${event.replayed}; link = https://radar.com/dashboard/events/${event._id}")
        }
    }

    internal fun sendLocation(location: Location, user: RadarUser) {
        receiver?.onLocationUpdated(context, location, user)

        logger.i("📍 Radar location updated | coordinates = (${location.latitude}, ${location.longitude}); accuracy = ${location.accuracy} meters; link = https://radar.com/dashboard/users/${user._id}")
    }

    internal fun sendClientLocation(
        location: Location,
        stopped: Boolean,
        source: RadarLocationSource
    ) {
        receiver?.onClientLocationUpdated(context, location, stopped, source)
    }

    internal fun sendError(status: RadarStatus) {
        receiver?.onError(context, status)

        logger.e("📍️ Radar error received | status = $status", RadarLogType.SDK_ERROR)
    }

    internal fun sendLog(level: RadarLogLevel, message: String, type: RadarLogType?, createdAt: Date = Date()) {
        receiver?.onLog(context, message)
        if (isTestKey()) {
            logBuffer.write(level, type, message, createdAt)
        }
    }

    internal fun sendToken(token: RadarVerifiedLocationToken) {
        verifiedReceiver?.onTokenUpdated(context, token)

        logger.i("📍️ Radar token updated | passed = ${token.passed}; expiresAt = ${token.expiresAt}; expiresIn = ${token.expiresIn}; token = ${token.token}")
    }

    internal fun sendLocationPermissionStatus(status: RadarLocationPermissionStatus) {
        receiver?.onLocationPermissionStatusUpdated(context, status)

        logger.i("📍️ Radar location permission updated | status = $status")
    }

    internal fun setLogPersistenceFeatureFlag(enabled: Boolean) {
        this.logBuffer.setPersistentLogFeatureFlag(enabled)
    }

}
