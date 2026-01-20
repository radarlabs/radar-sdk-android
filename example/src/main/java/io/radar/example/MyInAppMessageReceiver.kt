package io.radar.example

import android.util.Log
import io.radar.sdk.Radar
import io.radar.sdk.RadarInAppMessageReceiver
import io.radar.sdk.model.RadarInAppMessage

class MyInAppMessageReceiver() : RadarInAppMessageReceiver {
   override fun onNewInAppMessage(inAppMessage: RadarInAppMessage) {
       Log.d("MyInAppMessageReceiver", "beforeInAppMessageDisplayed: ${inAppMessage.title}")
       Radar.showInAppMessage(inAppMessage)
   }

    override fun onInAppMessageDismissed(inAppMessage: RadarInAppMessage) {
        Log.d("MyInAppMessageReceiver", "onInAppMessageDismissed: ${inAppMessage.title}")
    }

    override fun onInAppMessageButtonClicked(inAppMessage: RadarInAppMessage) {
        Log.d("MyInAppMessageReceiver", "onInAppMessageButtonClicked: ${inAppMessage.title}")
        super.onInAppMessageButtonClicked(inAppMessage)
    }
}
