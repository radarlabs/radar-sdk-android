package io.radar.sdk

import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/**
 * To use radar silent push, add Firebase dependency to gradle
 * ```
 *   implementation platform('com.google.firebase:firebase-bom:34.5.0')
 *   implementation "com.google.firebase:firebase-messaging"
 * ```
 * add the following to AndroidManifest.xml
 * ```
 *   <service android:name="io.radar.sdk.RadarFirebaseMessagingService"
 *       android:exported="false">
 *       <intent-filter>
 *           <action android:name="com.google.firebase.MESSAGING_EVENT" />
 *       </intent-filter>
 *   </service>
 * ```
 * initialize Firebase with
 * ```
 * FirebaseApp.initializeApp(this)
 * ```
 * then initialize Radar with
 * ```
 * Radar.initialize(context, apiKey, RadarInitializeOptions(silentPush=true))
 * ```
 * or retrieve the firebase messaging token and set it token manually
 * ```
 * FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
 *     Radar.setPushNotificationToken(token)
 * }
 * ```
 */
class RadarFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        // if app was terminated, Radar will not have been initialized, so we need to initialize it
        if (!Radar.isInitialized()) {
            Radar.initialize(this)
        }

        if (message.data["type"] == "radar:trackOnce") {
            Radar.trackOnce()
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)

        if (!Radar.isInitialized()) {
            Radar.initialize(this)
        }

        RadarSettings.pushNotificationToken = token
    }

    companion object {
        fun initialize() {
            FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
                Radar.setPushNotificationToken(token)
            }.addOnFailureListener {
                Radar.logger.w("failed to get firebase token")
            }
        }
    }
}
