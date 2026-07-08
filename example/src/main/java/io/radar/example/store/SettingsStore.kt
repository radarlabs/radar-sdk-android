package io.radar.example.store

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.edit
import io.radar.sdk.Radar
import org.json.JSONObject

/**
 * SDK-backed identity + tracking snapshot, plus a persisted publishable-key override.
 * Mirrors the iOS `SettingsStore`.
 *
 * The default publishable key is a placeholder — set a real test key at runtime via the
 * settings sheet (persisted to SharedPreferences), or replace [DEFAULT_PUBLISHABLE_KEY].
 */
class SettingsStore(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    var publishableKeyOverride by mutableStateOf(prefs.getString(KEY_PK, "") ?: "")
        private set

    val resolvedPublishableKey: String
        get() = publishableKeyOverride.ifBlank { DEFAULT_PUBLISHABLE_KEY }

    // Identity
    var userId by mutableStateOf("")
        private set
    var description by mutableStateOf("")
        private set
    var metadataText by mutableStateOf("—")
        private set

    // Tracking snapshot
    var isTracking by mutableStateOf(false)
        private set
    var usingRemoteTrackingOptions by mutableStateOf(false)
        private set
    var trackingOptionsJson by mutableStateOf("—")
        private set

    /** Re-read all SDK-backed state. Call after any SDK identity/tracking mutation. */
    fun refresh() {
        userId = Radar.getUserId() ?: ""
        description = Radar.getDescription() ?: ""
        metadataText = try {
            Radar.getMetadata()?.toString(2) ?: "—"
        } catch (e: Exception) {
            "—"
        }
        isTracking = Radar.isTracking()
        usingRemoteTrackingOptions = Radar.isUsingRemoteTrackingOptions()
        trackingOptionsJson = try {
            Radar.getTrackingOptions()?.toJson()?.toString(2) ?: "—"
        } catch (e: Exception) {
            "—"
        }
    }

    fun applyUserId(value: String) {
        Radar.setUserId(value.ifBlank { null })
        userId = value
    }

    fun applyDescription(value: String) {
        Radar.setDescription(value.ifBlank { null })
        description = value
    }

    fun savePublishableKeyOverride(value: String) {
        publishableKeyOverride = value
        prefs.edit { putString(KEY_PK, value) }
    }

    /** Apply a preset's identity + tracking action, logging to the console. */
    fun apply(preset: TestPreset, logStore: LogStore) {
        logStore.writeAction("apply preset: ${preset.name}")
        Radar.setUserId(preset.userId)
        val metadata = if (preset.metadata.isEmpty()) {
            null
        } else {
            JSONObject().apply { preset.metadata.forEach { (k, v) -> put(k, v) } }
        }
        Radar.setMetadata(metadata)
        when (val action = preset.trackingAction) {
            is TestPreset.TrackingAction.Start -> Radar.startTracking(action.options)
            TestPreset.TrackingAction.Stop -> Radar.stopTracking()
            TestPreset.TrackingAction.LeaveUnchanged -> Unit
        }
        refresh()
    }

    companion object {
        /** Placeholder — replace or override at runtime. Safe to commit (no real key). */
        const val DEFAULT_PUBLISHABLE_KEY = "prj_test_pk_"

        private const val PREFS = "radar_example_settings"
        private const val KEY_PK = "publishableKeyOverride"
    }
}
