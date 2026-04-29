package io.radar.sdk

import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.radar.sdk.RadarApiHelper.Companion.classifyNetworkException
import io.radar.sdk.RadarApiHelper.Companion.networkErrorMessage
import org.json.JSONException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException
import javax.net.ssl.SSLHandshakeException

/**
 * Unit tests for the diagnostic classification + log formatting added in FENCE-2793.
 *
 * The catch dispatch in [RadarApiHelper.request] routes every IOException through
 * [classifyNetworkException] to assign a stable kind label, then formats the structured
 * error log via [networkErrorMessage]. Both are pure functions and can be tested without
 * standing up a real network call.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.P])
class RadarApiHelperTest {

    @Test
    fun classifyNetworkException_unknownHost_returnsDnsFailure() {
        assertEquals("DNS_FAILURE", classifyNetworkException(UnknownHostException("nodename nor servname provided")))
    }

    @Test
    fun classifyNetworkException_socketTimeout_returnsTimeout() {
        assertEquals("TIMEOUT", classifyNetworkException(SocketTimeoutException("connect timed out")))
    }

    @Test
    fun classifyNetworkException_sslException_returnsSslFailure() {
        assertEquals("SSL_FAILURE", classifyNetworkException(SSLException("Connection closed by peer")))
    }

    @Test
    fun classifyNetworkException_sslHandshake_returnsSslFailure() {
        // SSLHandshakeException is a common subclass of SSLException; ensure the catch
        // hierarchy covers it (e.g. self-signed cert, TLS interception by some blockers).
        assertEquals("SSL_FAILURE", classifyNetworkException(SSLHandshakeException("certificate verify failed")))
    }

    @Test
    fun classifyNetworkException_connectException_returnsConnectRefused() {
        assertEquals("CONNECT_REFUSED", classifyNetworkException(ConnectException("Connection refused")))
    }

    @Test
    fun classifyNetworkException_genericIo_returnsIoOther() {
        assertEquals("IO_OTHER", classifyNetworkException(IOException("unexpected end of stream")))
    }

    @Test
    fun networkErrorMessage_includesAllStructuredFields() {
        val message = networkErrorMessage(
            host = "https://api.radar.io",
            e = UnknownHostException("Unable to resolve host \"api.radar.io\""),
            elapsedMs = 1234L,
            kind = "DNS_FAILURE"
        )

        // Verify each diagnostic field is present and labeled — the format is consumed by
        // logcat scraping and by Radar.sendLog() telemetry, so field names matter.
        assertTrue("missing host: $message", message.contains("host = https://api.radar.io"))
        assertTrue("missing kind: $message", message.contains("kind = DNS_FAILURE"))
        assertTrue("missing exception class: $message", message.contains("exception = UnknownHostException"))
        assertTrue("missing exception message: $message", message.contains("Unable to resolve host"))
        assertTrue("missing elapsedMs: $message", message.contains("elapsedMs = 1234"))
    }

    @Test
    fun networkErrorMessage_useLocalizedMessage_forNonNetworkExceptions() {
        // JSON parse failures and unknown exceptions also flow through networkErrorMessage,
        // so confirm formatting is uniform regardless of exception type.
        val message = networkErrorMessage(
            host = "https://api.radar.io",
            e = JSONException("Unterminated string at character 42"),
            elapsedMs = 7L,
            kind = "JSON_PARSE"
        )

        assertTrue(message.contains("kind = JSON_PARSE"))
        assertTrue(message.contains("exception = JSONException"))
        assertTrue(message.contains("Unterminated string"))
    }
}
