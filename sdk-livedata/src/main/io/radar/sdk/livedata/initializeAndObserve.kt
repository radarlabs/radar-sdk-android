package io.radar.sdk.livedata

import android.content.Context
import io.radar.sdk.Radar

/**
 * Initializes the Radar SDK. Call this method from the main thread in your `Application` class before calling any
 * other Radar methods.
 *
 * @see [](https://radar.io/documentation/sdk/android#initialize-sdk)
 *
 * @param[context] The context
 * @param[publishableKey] Your publishable API key
 * @return a LiveData adapter to [io.radar.sdk.RadarReceiver]
 */
fun Radar.initializeAndObserve(context: Context?, publishableKey: String? = null): RadarModel {
    val model = RadarModel()
    initialize(context, publishableKey, model.receiver)
    return model
}