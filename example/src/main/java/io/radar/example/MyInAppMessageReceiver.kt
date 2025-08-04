package io.radar.example

import android.util.Log
import io.radar.sdk.RadarInAppMessageOperation
import io.radar.sdk.RadarInAppMessageReceiver
import io.radar.sdk.model.RadarInAppMessage

class MyInAppMessageReceiver : RadarInAppMessageReceiver {

    private var callCount = 0

    override fun onNewInAppMessage(payload: RadarInAppMessage): RadarInAppMessageOperation {
        callCount++
        Log.d("MyInAppMessageReceiver", "beforeInAppMessageDisplayed: ${payload.title}, call count: $callCount")
        return if (callCount % 2 == 1) {
            RadarInAppMessageOperation.DISPLAY
        } else {
            RadarInAppMessageOperation.DISCARD
        }
    }

    override fun onInAppMessageDismissed(payload: RadarInAppMessage) {
        Log.d("MyInAppMessageReceiver", "onInAppMessageDismissed: ${payload.title}")
    }

    override fun onInAppMessageButtonClicked(payload: RadarInAppMessage) {
        Log.d("MyInAppMessageReceiver", "onInAppMessageButtonClicked: ${payload.title}")
    }
    
}