package io.radar.example.store

import android.os.Handler
import android.os.Looper
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

/**
 * Which environments a publishable key is valid on. A key's string doesn't say whether it belongs
 * to staging or production, so [KeyChecker] asks each environment's `/v1/config`.
 */
sealed interface KeyCheck {
    data object NotChecked : KeyCheck

    data object Checking : KeyCheck

    data class Done(
        val validOn: List<RadarEnvironment>,
        /** Environments that couldn't be reached or gave an unexpected response. */
        val unknown: List<RadarEnvironment>,
    ) : KeyCheck

    /** Whether the key is known not to work on [environment]. Local is never checked. */
    fun mismatches(environment: RadarEnvironment): Boolean =
        this is Done && environment in RadarEnvironment.checkable &&
            environment !in validOn && environment !in unknown

    fun summary(): String = when (this) {
        NotChecked -> "not checked"
        Checking -> "checking…"
        is Done -> {
            val valid = if (validOn.isEmpty()) "valid on neither" else "valid on " + validOn.joinToString(" and ") { it.label }
            if (unknown.isEmpty()) valid else "$valid (${unknown.joinToString { it.label }} unreachable)"
        }
    }
}

object KeyChecker {

    private val executor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())

    /** Probes every [RadarEnvironment.checkable] environment off the main thread; calls back on main. */
    fun check(publishableKey: String, onResult: (KeyCheck.Done) -> Unit) {
        executor.execute {
            val validOn = mutableListOf<RadarEnvironment>()
            val unknown = mutableListOf<RadarEnvironment>()
            for (environment in RadarEnvironment.checkable) {
                when (probe(environment.host(""), publishableKey)) {
                    true -> validOn += environment
                    false -> Unit
                    null -> unknown += environment
                }
            }
            val result = KeyCheck.Done(validOn, unknown)
            mainHandler.post { onResult(result) }
        }
    }

    /** true = accepted, false = rejected (401/403), null = unreachable or unexpected response. */
    private fun probe(host: String, publishableKey: String): Boolean? {
        var connection: HttpURLConnection? = null
        return try {
            connection = (URL("$host/v1/config?installId=example-key-check&usage=keyCheck").openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = TIMEOUT_MS
                readTimeout = TIMEOUT_MS
                setRequestProperty("Authorization", publishableKey)
            }
            when (connection.responseCode) {
                in 200..299 -> true
                401, 403 -> false
                else -> null
            }
        } catch (e: IOException) {
            null
        } finally {
            connection?.disconnect()
        }
    }

    private const val TIMEOUT_MS = 5_000
}
