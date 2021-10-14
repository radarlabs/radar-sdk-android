package io.radar.sdk.util

/**
 * Abstraction from [android.os.Handler]
 */
internal interface RadarPostable {

    fun post(runnable: Runnable)

    fun postDelayed(runnable: Runnable, delayMillis: Long)

}