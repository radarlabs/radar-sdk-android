package io.radar.sdk.util

import android.os.Handler

/**
 * Simple wrapper for passing a [Handler] as a [RadarPostable]
 */
internal class RadarHandler(private val handler: Handler) : RadarPostable {

    override fun post(runnable: Runnable) {
        handler.post(runnable)
    }

    override fun postDelayed(runnable: Runnable, delayMillis: Long) {
        handler.postDelayed(runnable, delayMillis)
    }
}