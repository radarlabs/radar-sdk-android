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
 * @param[silentPush] A boolean indicating if silent push notification should be configured.
 */
class RadarInitializeOptions(
    val radarReceiver: RadarReceiver? = null,
    val locationProvider: Radar.RadarLocationServicesProvider = Radar.RadarLocationServicesProvider.GOOGLE,
    val fraud: Boolean = false,
    val customForegroundNotification: Notification? = null,
    val inAppMessageReceiver: RadarInAppMessageReceiver? = null,
    val silentPush: Boolean = false,
)
