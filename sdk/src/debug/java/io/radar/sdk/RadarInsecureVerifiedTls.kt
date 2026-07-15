package io.radar.sdk

import android.annotation.SuppressLint
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

/**
 * Debug-only implementation used to test trackVerified against a locally hosted dev server with a
 * self-signed certificate. It disables certificate and hostname validation for every request in a
 * debug build — no Gradle property or flag required. This class lives in the debug source set, so
 * none of this bypass code is compiled into the published (release) SDK.
 */
internal object RadarInsecureVerifiedTls {

    @SuppressLint("BadHostnameVerifier")
    private val insecureHostnameVerifier = HostnameVerifier { _, _ -> true }

    private val insecureSslContext: SSLContext by lazy { createInsecureSslContext() }

    fun applyIfEnabled(connection: HttpsURLConnection) {
        connection.sslSocketFactory = insecureSslContext.socketFactory
        connection.hostnameVerifier = insecureHostnameVerifier
    }

    @SuppressLint("TrustAllX509TrustManager", "CustomX509TrustManager", "TrulyRandom")
    private fun createInsecureSslContext(): SSLContext {
        val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
            override fun checkClientTrusted(certs: Array<X509Certificate>, authType: String) {}
            override fun checkServerTrusted(certs: Array<X509Certificate>, authType: String) {}
        })
        return SSLContext.getInstance("TLS").apply {
            init(null, trustAllCerts, SecureRandom())
        }
    }
}
