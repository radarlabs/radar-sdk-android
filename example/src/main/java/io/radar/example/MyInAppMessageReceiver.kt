package io.radar.example

import android.util.Log
import io.radar.sdk.RadarInAppMessageOperation
import io.radar.sdk.RadarInAppMessageReceiver
import io.radar.sdk.model.RadarInAppMessagePayload

class MyInAppMessageReceiver : RadarInAppMessageReceiver {

    private var callCount = 0

    override fun beforeInAppMessageDisplayed(payload: RadarInAppMessagePayload): RadarInAppMessageOperation {
        callCount++
        Log.d("MyInAppMessageReceiver", "beforeInAppMessageDisplayed: ${payload.title}, call count: $callCount")
        return if (callCount % 2 == 1) {
            RadarInAppMessageOperation.DISPLAY
        } else {
            RadarInAppMessageOperation.ENQUEUE
        }
    }

    override fun onInAppMessageDismissed(payload: RadarInAppMessagePayload) {
        Log.d("MyInAppMessageReceiver", "onInAppMessageDismissed: ${payload.title}")
    }

    override fun onInAppMessageButtonClicked(payload: RadarInAppMessagePayload) {
        Log.d("MyInAppMessageReceiver", "onInAppMessageButtonClicked: ${payload.title}")
    }
    
    
}