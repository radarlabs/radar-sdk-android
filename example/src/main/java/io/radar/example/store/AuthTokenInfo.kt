package io.radar.example.store

import android.util.Base64
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Reads the unverified header and payload of an auth token (a JWT), for display only. */
object AuthTokenInfo {

    /** e.g. `kid 65f0…, expires 14:05:12 (in 9m)`, or why the token can't be read. */
    fun describe(token: String): String {
        val parts = token.split('.')
        if (parts.size != 3) return "not a JWT"
        val header = decode(parts[0]) ?: return "unreadable header"
        val payload = decode(parts[1])
        val kid = header.optString("kid").ifBlank { "(no kid)" }
        val exp = payload?.optLong("exp", 0L) ?: 0L
        if (exp == 0L) return "kid $kid, no exp"
        val remainingSeconds = exp - System.currentTimeMillis() / 1000
        val time = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date(exp * 1000))
        val relative = if (remainingSeconds >= 0) "in ${duration(remainingSeconds)}" else "expired ${duration(-remainingSeconds)} ago"
        return "kid $kid, expires $time ($relative)"
    }

    private fun decode(segment: String): JSONObject? = try {
        JSONObject(String(Base64.decode(segment, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP), Charsets.UTF_8))
    } catch (e: Exception) {
        null
    }

    private fun duration(seconds: Long): String = when {
        seconds < 60 -> "${seconds}s"
        seconds < 3600 -> "${seconds / 60}m"
        else -> "${seconds / 3600}h"
    }
}
