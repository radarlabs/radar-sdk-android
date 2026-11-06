package io.radar.example.store

import android.content.Context
import androidx.core.content.edit

/**
 * NOTE(jasonl): Default API hosts for each environment. Settings can override either host.
 * The example permits local HTTP URLs through `network_security_config.xml`.
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

    /** NOTE(jasonl): Set both SDK hosts before initialization. */
    fun applyTo(context: Context, host: String, verifiedHost: String) {
        context.getSharedPreferences(SDK_PREFS, Context.MODE_PRIVATE).edit {
            putString(SDK_KEY_HOST, host)
            putString(SDK_KEY_VERIFIED_HOST, verifiedHost)
        }
    }

    companion object {
        /** NOTE(jasonl): Check keys on environments with default API hosts. */
        val checkable = listOf(PRODUCTION, STAGING)

        fun fromName(name: String?): RadarEnvironment = entries.firstOrNull { it.name == name } ?: PRODUCTION

        // The SDK's own SharedPreferences (see RadarSettings in the sdk module).
        private const val SDK_PREFS = "RadarSDK"
        private const val SDK_KEY_HOST = "host"
        private const val SDK_KEY_VERIFIED_HOST = "verified_host"
    }
}
