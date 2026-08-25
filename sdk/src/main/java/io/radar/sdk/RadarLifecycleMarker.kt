package io.radar.sdk

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

internal class RadarLifecycleMarker(
    private val preferences: SharedPreferences
) {
    private companion object {
        private const val KEY_APP_LIFECYCLE_MARKER = "app_lifecycle_marker"
    }

    private val lock = Any()
    private var didBeginProcess = false

    constructor(context: Context) : this(
        context.getSharedPreferences("RadarSDK", Context.MODE_PRIVATE)
    )

    @Suppress("ApplySharedPref")
    fun beginProcess(): Boolean {
        synchronized(lock) {
            if (didBeginProcess) {
                return false
            }

            val previousValue = preferences.getBoolean(KEY_APP_LIFECYCLE_MARKER, false)
            // The next process must see this marker before this method returns.
            preferences.edit(commit = true) {
                putBoolean(KEY_APP_LIFECYCLE_MARKER, true)
            }
            didBeginProcess = true
            return previousValue
        }
    }
}
