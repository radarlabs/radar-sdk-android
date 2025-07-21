package io.radar.example

import android.util.Log
import io.radar.sdk.RadarInAppMessageOperation
import io.radar.sdk.RadarInAppMessageReceiver
import io.radar.sdk.model.RadarInAppMessagePayload

class MyInAppMessageReceiver : RadarInAppMessageReceiver {


    override fun beforeInAppMessageDisplayed(payload: RadarInAppMessagePayload): RadarInAppMessageOperation {
        Log.d("MyInAppMessageReceiver", "beforeInAppMessageDisplayed: ${payload.title}")
        return RadarInAppMessageOperation.DISPLAY
    }

    override fun onInAppMessageDismissed(payload: RadarInAppMessagePayload) {
        Log.d("MyInAppMessageReceiver", "onInAppMessageDismissed: ${payload.title}")
    }

    override fun onInAppMessageButtonClicked(payload: RadarInAppMessagePayload) {
        Log.d("MyInAppMessageReceiver", "onInAppMessageButtonClicked: ${payload.title}")
    }
    
    
}