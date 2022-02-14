package io.radar.sdk.util

/**
 * Simple implementation of [RadarPostable] that immediately runs the given runnables
 */
internal class FastPostable : RadarPostable {

    override fun post(runnable: Runnable) {
        runnable.run()
    }

    override fun postDelayed(runnable: Runnable, delayMillis: Long) {
        runnable.run()
    }

}