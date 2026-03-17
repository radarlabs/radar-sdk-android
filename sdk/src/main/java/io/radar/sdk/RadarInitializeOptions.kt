package io.radar.sdk

import android.app.Notification

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
 *
 */
class RadarInitializeOptions(
    val radarReceiver: RadarReceiver? = null,
    val locationProvider: Radar.RadarLocationServicesProvider = Radar.RadarLocationServicesProvider.GOOGLE,
    val fraud: Boolean = false,
    val customForegroundNotification: Notification? = null,
    val inAppMessageReceiver: RadarInAppMessageReceiver? = null,
    val silentPush: Boolean = false,
    val publishableKey: String? = null,
    val authToken: String? = null,
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

        fun radarReceiver(radarReceiver: RadarReceiver) = apply { this.radarReceiver = radarReceiver }
        fun locationProvider(locationProvider: Radar.RadarLocationServicesProvider) = apply { this.locationProvider = locationProvider }
        fun fraud(fraud: Boolean) = apply { this.fraud = fraud }
        fun customForegroundNotification(customForegroundNotification: Notification) = apply { this.customForegroundNotification = customForegroundNotification }
        fun inAppMessageReceiver(inAppMessageReceiver: RadarInAppMessageReceiver) = apply { this.inAppMessageReceiver = inAppMessageReceiver }
        fun silentPush(silentPush: Boolean) = apply { this.silentPush = silentPush}
        fun publishableKey(publishableKey: String) = apply { this.publishableKey = publishableKey}
        fun authToken(authToken: String) = apply { this.authToken = authToken}

        fun build() = RadarInitializeOptions(
            radarReceiver=radarReceiver,
            locationProvider=locationProvider,
            fraud=fraud,
            customForegroundNotification=customForegroundNotification,
            inAppMessageReceiver=inAppMessageReceiver,
            silentPush=silentPush,
            publishableKey=publishableKey,
            authToken=authToken
        )
    }

    companion object {
        @JvmStatic
        fun builder() = Builder()
    }
}

