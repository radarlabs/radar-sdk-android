package io.radar.sdk

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import java.util.concurrent.atomic.AtomicBoolean

internal class RadarLifecycleMarker(
    private val preferences: SharedPreferences
) {
    internal companion object {
        const val APP_TERMINATING_MESSAGE = "App terminating"

        private const val KEY_APP_LIFECYCLE_MARKER = "app_lifecycle_marker"
    }

    private val didBeginProcess = AtomicBoolean(false)

    constructor(context: Context) : this(
        context.getSharedPreferences("RadarSDK", Context.MODE_PRIVATE)
    )

    @Suppress("ApplySharedPref")
    fun beginProcess(): Boolean {
        if (!didBeginProcess.compareAndSet(false, true)) {
            return false
        }

        try {
            val previousValue = preferences.getBoolean(KEY_APP_LIFECYCLE_MARKER, false)
            // Android may kill a background app without a callback, so this marker stays set.
            preferences.edit(commit = true) {
                putBoolean(KEY_APP_LIFECYCLE_MARKER, true)
            }
            return previousValue
        } catch (exception: RuntimeException) {
            didBeginProcess.set(false)
            throw exception
        }
    }
}
