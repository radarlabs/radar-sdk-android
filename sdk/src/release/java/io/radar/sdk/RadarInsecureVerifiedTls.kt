package io.radar.sdk

import javax.net.ssl.HttpsURLConnection

/**
 * Release (published SDK) implementation. TLS certificate and hostname validation is never
 * bypassed, so this is a no-op. The debug source set provides the counterpart used for testing
 * trackVerified against a locally hosted dev server — that code is not compiled into release builds.
 */
internal object RadarInsecureVerifiedTls {
    fun applyIfEnabled(connection: HttpsURLConnection) {
        // No-op in release builds.
    }
}
