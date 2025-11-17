package io.radar.sdk

import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class RadarFirebaseMessagingService: FirebaseMessagingService() {

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
        RadarSettings.pushNotificationToken = token
    }

    companion object {
        fun initialize() {
            FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
                Radar.setPushNotificationToken(token)
                println("Got token")
            }.addOnFailureListener {
                Radar.logger.w("failed to get firebase token")
            }
        }
    }
}
