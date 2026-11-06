package io.radar.example.store

import android.content.Context
import androidx.core.content.edit

/**
 * The Radar API environment the example app talks to, picked in Settings → Environment.
 * [LOCAL] accepts separate API and verified host URLs (e.g.
 * `http://192.168.X.X:8081`, or `http://10.0.2.2:8081` on the emulator). The example permits
 * cleartext traffic for this (see `network_security_config.xml`).
 */
enum class RadarEnvironment(
    val label: String,
    private val fixedHost: String?,
    private val fixedVerifiedHost: String?,
) {
    PRODUCTION("Production", "https://api.radar.io", "https://api-verified.radar.io"),
    STAGING("Staging", "https://api.radar-staging.com", "https://api-verified.radar-staging.com"),
    LOCAL("Local", null, null),
    ;

    fun host(localUrl: String): String = fixedHost ?: localUrl

    fun verifiedHost(localUrl: String): String = fixedVerifiedHost ?: localUrl

    /** Writes the SDK's host overrides. */
    fun applyTo(context: Context, host: String, verifiedHost: String) {
        context.getSharedPreferences(SDK_PREFS, Context.MODE_PRIVATE).edit {
            putString(SDK_KEY_HOST, host)
            putString(SDK_KEY_VERIFIED_HOST, verifiedHost)
        }
    }

    companion object {
        /** Environments with fixed hosts, which the key check probes. */
        val checkable = listOf(PRODUCTION, STAGING)

        fun fromName(name: String?): RadarEnvironment = entries.firstOrNull { it.name == name } ?: PRODUCTION

        // The SDK's own SharedPreferences (see RadarSettings in the sdk module).
        private const val SDK_PREFS = "RadarSDK"
        private const val SDK_KEY_HOST = "host"
        private const val SDK_KEY_VERIFIED_HOST = "verified_host"
    }
}
