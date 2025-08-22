package io.radar.example

import android.app.Activity
import android.util.Log
import io.radar.sdk.RadarInAppMessageOperation
import io.radar.sdk.RadarInAppMessageReceiver
import io.radar.sdk.model.RadarInAppMessage

class MyInAppMessageReceiver(override var activity: Activity? = null) : RadarInAppMessageReceiver {
   override fun onNewInAppMessage(inAppMessage: RadarInAppMessage): RadarInAppMessageOperation {
       Log.d("MyInAppMessageReceiver", "beforeInAppMessageDisplayed: ${inAppMessage.title}")
       return RadarInAppMessageOperation.DISPLAY
   }

    override fun onInAppMessageDismissed(inAppMessage: RadarInAppMessage) {
        Log.d("MyInAppMessageReceiver", "onInAppMessageDismissed: ${inAppMessage.title}")
    }

    override fun onInAppMessageButtonClicked(inAppMessage: RadarInAppMessage) {
        Log.d("MyInAppMessageReceiver", "onInAppMessageButtonClicked: ${inAppMessage.title}")
        Log.d("MyInAppMessageReceiver", "HERE")
        super.onInAppMessageButtonClicked(inAppMessage)
    }
}
