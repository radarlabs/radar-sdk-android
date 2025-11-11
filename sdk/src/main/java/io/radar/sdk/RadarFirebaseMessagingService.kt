package io.radar.sdk

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class RadarFirebaseMessagingService: FirebaseMessagingService() {


    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        print("Received")
    }


    override fun onNewToken(token: String) {
        super.onNewToken(token)
        RadarSettings.pushNotificationToken = token
    }
}