package io.radar.example.store

import io.radar.sdk.model.RadarResponseFormat
import io.radar.sdk.model.RadarRevealRiskToken
import io.radar.sdk.model.RadarVerifiedLocationToken
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import org.json.JSONObject

/** Presents SDK results for QA. Access decisions belong to the receiving backend. */
class VerifiedResponse private constructor(
    private val format: RadarResponseFormat,
    private val evaluationId: String?,
    private val token: String?,
    private val expiresAt: String?,
    private val expiresIn: Int?,
    private val passed: Boolean?,
    private val beaconCount: Int?,
    private val response: JSONObject,
) {
    constructor(result: RadarVerifiedLocationToken) : this(
        format = result.format,
        evaluationId = result._id.takeIf { it.isNotEmpty() },
        token = result.token,
        expiresAt = result.expiresAt?.let(::formatDate),
        expiresIn = result.expiresIn.takeUnless { result.format == RadarResponseFormat.ID },
        passed = result.passed.takeUnless { result.format == RadarResponseFormat.ID },
        beaconCount = if (result.format == RadarResponseFormat.JWS) result.user?.beacons?.size ?: 0 else null,
        response = result.toJson(),
    )

    constructor(result: RadarRevealRiskToken) : this(
        format = result.format,
        evaluationId = result.id.takeIf { it.isNotEmpty() },
        token = result.token,
        expiresAt = result.expiresAt,
        expiresIn = result.expiresIn,
        passed = null,
        beaconCount = null,
        response = result.toJson(),
    )

    /** Every response format relays only the opaque token and/or evaluation ID to the backend. */
    val relayJSON: String?
        get() {
            if (token == null && evaluationId == null) return null
            val payload = JSONObject()
            token?.let { payload.put("token", it) }
            evaluationId?.let { payload.put("_id", it) }
            return json(payload)
        }

    val summary: String
        get() = "${format.value}, ID: ${evaluationId ?: "unavailable"}"

    val beaconSummary: String
        get() = beaconCount?.let { "$it beacons" }
            ?: "beacons unavailable in ${format.value} responses"

    val detail: String
        get() = buildList {
            add("Format: ${format.value}")
            add("Evaluation ID: ${evaluationId ?: "unavailable"}")
            add("Expires at: ${expiresAt ?: "unavailable"}")
            add("Expires in: ${expiresIn?.let { "$it seconds" } ?: "unavailable"}")
            passed?.let { add("Passed (unverified): $it") }
            add("The backend must verify the result before allowing access.")
            relayJSON?.let {
                add("")
                add("Backend relay JSON (long-press this entry to copy):")
                add(it)
            }
            add("")
            add("SDK response:")
            add(json(response) ?: "unavailable")
        }.joinToString("\n")

    private companion object {
        fun formatDate(date: Date): String =
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }.format(date)

        fun json(value: JSONObject): String? = try {
            value.toString(2)
        } catch (_: Exception) {
            null
        }
    }
}
