package io.radar.example.store

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.edit
import io.radar.example.MainActivity
import io.radar.sdk.Radar
import org.json.JSONObject

/**
 * SDK-backed identity + tracking snapshot, plus the persisted environment: which
 * [RadarEnvironment] to use, a publishable key per environment, and the Local server URL.
 *
 * The environment applied at launch is fixed for the life of the process. Changing it goes
 * through [saveEnvironment] followed by a restart (see `MainActivity.restart`).
 */
class SettingsStore(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    init {
        migrateLegacyKeyOverride()
    }

    /** The environment this process was launched with. */
    val environment: RadarEnvironment = RadarEnvironment.fromName(prefs.getString(KEY_ENVIRONMENT, null))

    /** The Local server URL this process was launched with. */
    val localUrl: String = prefs.getString(KEY_LOCAL_URL, "") ?: ""

    /** The key for [environment], or "" if none is set. */
    val resolvedPublishableKey: String = publishableKey(environment)

    /** The API host for [environment]. */
    val host: String
        get() = environment.host(localUrl)

    /** Which environments [resolvedPublishableKey] is valid on. */
    var keyCheck by mutableStateOf<KeyCheck>(KeyCheck.NotChecked)
        private set

    /** The saved key for [env], falling back to its default in `MainActivity` (placeholders count as unset). */
    fun publishableKey(env: RadarEnvironment): String =
        prefs.getString(keyPrefsKey(env), null) ?: defaultPublishableKey(env)

    /** The saved Local URL, which may differ from [localUrl] if it was changed since launch. */
    fun savedLocalUrl(): String = prefs.getString(KEY_LOCAL_URL, "") ?: ""

    /** Persists the environment settings. Takes effect on the next launch. */
    fun saveEnvironment(env: RadarEnvironment, publishableKey: String, localUrl: String) {
        prefs.edit(commit = true) {
            putString(KEY_ENVIRONMENT, env.name)
            putString(keyPrefsKey(env), publishableKey.trim())
            putString(KEY_LOCAL_URL, localUrl.trim().trimEnd('/'))
        }
    }

    /**
     * Checks which environments [resolvedPublishableKey] is valid on, and logs an error if it isn't
     * valid on the selected one.
     */
    fun runKeyCheck(logStore: LogStore) {
        val key = resolvedPublishableKey
        if (key.isBlank()) {
            logStore.writeError("No publishable key set for ${environment.label}", "Set one in Settings → Environment.")
            return
        }
        keyCheck = KeyCheck.Checking
        KeyChecker.check(key) { result ->
            keyCheck = result
            if (result.mismatches(environment)) {
                logStore.writeError(
                    "Key ${keyPrefix(key)} is not valid on ${environment.label}",
                    "Key check: ${result.summary()}. Switch environment or change the key in Settings → Environment.",
                )
            } else {
                logStore.writeResult("key check: ${result.summary()}", environmentSummary())
            }
        }
    }

    /** One line describing where requests go, for error context. */
    fun environmentSummary(): String =
        "env = ${environment.label}; host = $host; key = ${keyPrefix(resolvedPublishableKey)}; key check = ${keyCheck.summary()}"

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

    /** Moves the single pre-environment key override into the Production slot. */
    private fun migrateLegacyKeyOverride() {
        val legacy = prefs.getString(KEY_LEGACY_PK, null) ?: return
        prefs.edit {
            if (legacy.isNotBlank() && !prefs.contains(keyPrefsKey(RadarEnvironment.PRODUCTION))) {
                putString(keyPrefsKey(RadarEnvironment.PRODUCTION), legacy)
            }
            remove(KEY_LEGACY_PK)
        }
    }

    companion object {
        private const val PREFS = "radar_example_settings"
        private const val KEY_LEGACY_PK = "publishableKeyOverride"
        private const val KEY_ENVIRONMENT = "environment"
        private const val KEY_LOCAL_URL = "localUrl"

        private fun keyPrefsKey(env: RadarEnvironment) = "publishableKey.${env.name}"

        private fun defaultPublishableKey(env: RadarEnvironment): String {
            val key = when (env) {
                RadarEnvironment.PRODUCTION -> MainActivity.DEFAULT_PUBLISHABLE_KEY
                RadarEnvironment.STAGING -> MainActivity.DEFAULT_STAGING_PUBLISHABLE_KEY
                RadarEnvironment.LOCAL -> ""
            }
            // hooks/clean-filter replaces real keys with "prj_live_pk_..." in git.
            return if (key.endsWith("...")) "" else key
        }

        /** A key shortened for display, e.g. `prj_live_pk_0c38…`. */
        fun keyPrefix(key: String): String = when {
            key.isBlank() -> "(none)"
            key.length <= 16 -> key
            else -> key.take(16) + "…"
        }
    }
}
