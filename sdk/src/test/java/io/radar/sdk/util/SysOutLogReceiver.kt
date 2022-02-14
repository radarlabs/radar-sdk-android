package io.radar.sdk.util

import android.content.Context

internal class SysOutLogReceiver : DefaultRadarReceiver() {

    override fun onLog(context: Context, message: String) {
        println(message)
    }

} 