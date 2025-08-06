package io.radar.example

import android.util.Log
import io.radar.sdk.RadarInAppMessageReceiver
import io.radar.sdk.model.RadarInAppMessage

class MyInAppMessageReceiver : RadarInAppMessageReceiver {

   override fun onNewInAppMessage(payload: RadarInAppMessage): RadarInAppMessageOperation {
       Log.d("MyInAppMessageReceiver", "beforeInAppMessageDisplayed: ${payload.title}")
       return RadarInAppMessageOperation.DISPLAY
   }

    override fun onInAppMessageDismissed(payload: RadarInAppMessage) {
        Log.d("MyInAppMessageReceiver", "onInAppMessageDismissed: ${payload.title}")
    }

    override fun onInAppMessageButtonClicked(payload: RadarInAppMessage) {
        Log.d("MyInAppMessageReceiver", "onInAppMessageButtonClicked: ${payload.title}")
    }
    
}