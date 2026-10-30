package io.radar.example.store

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.core.content.edit

/**
 * The Radar API environment the example app talks to. This is the only place the hosts are
 * defined: pick an environment in Settings → Environment instead of editing constants.
 *
 * [LOCAL] points both the regular and verified host at a dev server URL (e.g.
 * `http://192.168.X.X:8081`, or `http://10.0.2.2:8081` on the emulator). The example permits
 * cleartext traffic for this (see `network_security_config.xml`).
 */
enum class RadarEnvironment(
    val label: String,
    val color: Color,
    private val fixedHost: String?,
    private val fixedVerifiedHost: String?,
) {
    PRODUCTION("Production", Color(0xFFE65100), "https://api.radar.io", "https://api-verified.radar.io"),
    STAGING("Staging", Color(0xFF1565C0), "https://api.radar-staging.com", "https://api-verified.radar-staging.com"),
    LOCAL("Local", Color(0xFF616161), null, null),
    ;

    fun host(localUrl: String): String = fixedHost ?: localUrl

    fun verifiedHost(localUrl: String): String = fixedVerifiedHost ?: localUrl

    /**
     * Writes the host overrides the SDK reads on every request. Production clears them so the SDK
     * falls back to its built-in defaults.
     */
    fun applyTo(context: Context, localUrl: String) {
        context.getSharedPreferences(SDK_PREFS, Context.MODE_PRIVATE).edit {
            if (this@RadarEnvironment == PRODUCTION) {
                remove(SDK_KEY_HOST)
                remove(SDK_KEY_VERIFIED_HOST)
            } else {
                putString(SDK_KEY_HOST, host(localUrl))
                putString(SDK_KEY_VERIFIED_HOST, verifiedHost(localUrl))
            }
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
