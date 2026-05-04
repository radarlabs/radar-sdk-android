package io.radar.sdk

import android.app.Notification
import android.app.Activity
import kotlin.time.Duration

/**
 * Initialize options for Radar
 *
 * @param[radarReceiver] An optional receiver for the client-side delivery of events.
 * @param[locationProvider] The location services provider.
 * @param[fraud] A boolean indicating whether to enable additional fraud detection signals for location verification.
 * @param[customForegroundNotification] An optional custom notification which is used for the foreground service
 * @param[inAppMessageReceiver] An optional receiver to handle in app message events.
 * @param[silentPush] A boolean indicating if silent push notification should be configured. see [RadarFirebaseMessagingService] for configuration details
 * @param[publishableKey] The project publishable key, if set, authToken should not be set.
 * @param[authToken] A JWT auth token, if set, publishableKey should not be set.
 * @param[activity] An optional activity that overwrites the activity from context.
 * @param[networkTimeout] Connect and base read timeout for Radar API requests (e.g. `10.seconds` from `kotlin.time`).
 * If set, the value is persisted. If null, a previously saved value or the default (10 seconds) is used.
 *
 */
data class RadarInitializeOptions(
    val radarReceiver: RadarReceiver? = null,
    val locationProvider: Radar.RadarLocationServicesProvider = Radar.RadarLocationServicesProvider.GOOGLE,
    val fraud: Boolean = false,
    val customForegroundNotification: Notification? = null,
    val inAppMessageReceiver: RadarInAppMessageReceiver? = null,
    val silentPush: Boolean = false,
    val publishableKey: String? = null,
    val authToken: String? = null,
    val activity: Activity? = null,
    val networkTimeout: Duration? = null,
) {
    class Builder {
        private var radarReceiver: RadarReceiver? = null
        private var locationProvider: Radar.RadarLocationServicesProvider = Radar.RadarLocationServicesProvider.GOOGLE
        private var fraud: Boolean = false
        private var customForegroundNotification: Notification? = null
        private var inAppMessageReceiver: RadarInAppMessageReceiver? = null
        private var silentPush: Boolean = false
        private var publishableKey: String? = null
        private var authToken: String? = null
        private var activity: Activity? = null
        private var networkTimeout: Duration? = null

        fun radarReceiver(radarReceiver: RadarReceiver) = apply { this.radarReceiver = radarReceiver }
        fun locationProvider(locationProvider: Radar.RadarLocationServicesProvider) = apply { this.locationProvider = locationProvider }
        fun fraud(fraud: Boolean) = apply { this.fraud = fraud }
        fun customForegroundNotification(customForegroundNotification: Notification) = apply { this.customForegroundNotification = customForegroundNotification }
        fun inAppMessageReceiver(inAppMessageReceiver: RadarInAppMessageReceiver) = apply { this.inAppMessageReceiver = inAppMessageReceiver }
        fun silentPush(silentPush: Boolean) = apply { this.silentPush = silentPush }
        fun publishableKey(publishableKey: String) = apply { this.publishableKey = publishableKey }
        fun authToken(authToken: String) = apply { this.authToken = authToken }
        fun activity(activity: Activity) = apply { this.activity = activity }
        fun networkTimeout(networkTimeout: Duration) = apply { this.networkTimeout = networkTimeout }

        fun build() = RadarInitializeOptions(
            radarReceiver = radarReceiver,
            locationProvider = locationProvider,
            fraud = fraud,
            customForegroundNotification = customForegroundNotification,
            inAppMessageReceiver = inAppMessageReceiver,
            silentPush = silentPush,
            publishableKey = publishableKey,
            authToken = authToken,
            activity = activity,
            networkTimeout = networkTimeout,
        )
    }

    companion object {
        @JvmStatic
        fun builder() = Builder()
    }
}

