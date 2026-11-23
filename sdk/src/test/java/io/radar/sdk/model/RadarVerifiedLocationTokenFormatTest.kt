package io.radar.sdk

import android.content.Context
import android.os.Build
import android.os.SystemClock
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.radar.sdk.model.RadarResponseFormat
import io.radar.sdk.model.RadarVerificationLevel
import io.radar.sdk.model.RadarVerifiedLocationToken
import java.time.Duration
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowSystemClock

@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.UPSIDE_DOWN_CAKE])
class RadarVerifiedLocationTokenFormatTest {

    private fun response(format: String? = null): JSONObject = JSONObject().apply {
        put("meta", JSONObject().put("code", 200))
        put("_id", "evaluation-id")
        put("token", "opaque-token")
        put("expiresAt", "2030-01-01T00:00:00.000Z")
        put("expiresIn", 90)
        put("passed", true)
        put("user", JSONObject().put("_id", "user-id"))
        put("events", JSONArray())
        put("failureReasons", JSONArray())
        put("extra", "legacy-field")
        if (format != null) {
            put("format", format)
        }
    }

    private fun parse(obj: JSONObject): RadarVerifiedLocationToken = requireNotNull(RadarVerifiedLocationToken.fromJson(obj))

    @Test
    fun legacyAndExplicitJwsPreserveTheFullResponse() {
        listOf(null, "jws").forEach { format ->
            val obj = response(format)
            val token = parse(obj)

            assertEquals(RadarResponseFormat.JWS, token.format)
            assertNotNull(token.user)
            assertTrue(token.events!!.isEmpty())
            assertTrue(token.failureReasons!!.isEmpty())
            assertEquals("opaque-token", token.token)
            assertEquals("evaluation-id", token._id)
            assertEquals(90, token.expiresIn)
            assertTrue(token.passed)
            assertNotNull(token.expiresAt)
            assertSame(obj, token.fullJson)
            assertSame(obj, token.toJson())
        }
    }

    @Test
    fun jwsRetainsLegacyDefaultsAndRequiredFields() {
        val obj = response().apply {
            remove("_id")
            remove("failureReasons")
            remove("expiresIn")
            remove("passed")
        }
        val token = parse(obj)

        assertEquals("", token._id)
        assertTrue(token.failureReasons!!.isEmpty())
        assertEquals(0, token.expiresIn)
        assertFalse(token.passed)

        listOf("user", "events", "expiresAt").forEach { field ->
            val missing = response().apply { remove(field) }
            assertNull(field, RadarVerifiedLocationToken.fromJson(missing))
        }
        assertNull(RadarVerifiedLocationToken.fromJson(null))
    }

    @Test
    fun jwePreservesMetadataAndExcludesSensitiveFields() {
        listOf(true, false).forEach { passed ->
            val obj = response("jwe").put("passed", passed)
            val token = parse(obj)

            assertEquals(RadarResponseFormat.JWE, token.format)
            assertEquals("evaluation-id", token._id)
            assertEquals("opaque-token", token.token)
            assertEquals(90, token.expiresIn)
            assertEquals(passed, token.passed)
            assertNotNull(token.expiresAt)
            assertNull(token.user)
            assertNull(token.events)
            assertNull(token.failureReasons)
            assertEquals(
                setOf(
                    "meta",
                    "format",
                    "_id",
                    "token",
                    "expiresAt",
                    "expiresIn",
                    "passed"
                ),
                token.toJson().keys().asSequence().toSet()
            )

            // Parsing must not strip fields from the caller's object.
            assertTrue(obj.has("user"))
            assertTrue(obj.has("extra"))
        }
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.M, Build.VERSION_CODES.UPSIDE_DOWN_CAKE])
    fun jweSupportsExpirationWithAndWithoutFractionalSeconds() {
        listOf(
            "2030-01-01T00:00:00.000Z",
            "2030-01-01T00:00:00Z"
        ).forEach { expiration ->
            assertNotNull(parse(response("jwe").put("expiresAt", expiration)).expiresAt)
        }
    }

    @Test
    fun jweRejectsMissingAndMalformedRequiredFields() {
        val invalidValues = mapOf(
            "_id" to listOf(JSONObject.NULL, "", 42),
            "token" to listOf(JSONObject.NULL, "", 42),
            "expiresAt" to listOf(JSONObject.NULL, "", "not-a-date", 42),
            "expiresIn" to listOf(JSONObject.NULL, "90", true, 1.5, 2147483648L),
            "passed" to listOf(JSONObject.NULL, "true", 1)
        )

        invalidValues.forEach { (field, values) ->
            val missing = response("jwe").apply { remove(field) }
            assertNull("Missing $field", RadarVerifiedLocationToken.fromJson(missing))

            values.forEach { value ->
                val obj = response("jwe").put(field, value)
                assertNull("$field=$value", RadarVerifiedLocationToken.fromJson(obj))
            }
        }
    }

    @Test
    fun idExposesOnlyItsIdAndMetadata() {
        val obj = response("id")
        val token = parse(obj)

        assertEquals(RadarResponseFormat.ID, token.format)
        assertEquals("evaluation-id", token._id)
        assertNull(token.user)
        assertNull(token.events)
        assertNull(token.token)
        assertNull(token.expiresAt)
        assertNull(token.failureReasons)
        assertEquals(0, token.expiresIn)
        assertFalse(token.passed)
        assertEquals(
            setOf("meta", "format", "_id"),
            token.toJson().keys().asSequence().toSet()
        )
        assertTrue(obj.has("user"))
        assertTrue(obj.has("token"))
    }

    @Test
    fun idRejectsMissingAndMalformedIds() {
        assertNull(
            RadarVerifiedLocationToken.fromJson(
                JSONObject().put("format", "id")
            )
        )

        listOf(JSONObject.NULL, "", 42).forEach { id ->
            val obj = JSONObject().put("format", "id").put("_id", id)
            assertNull("ID=$id", RadarVerifiedLocationToken.fromJson(obj))
        }
    }

    @Test
    fun rejectsInvalidFormatsInsteadOfFallingBackToJws() {
        listOf("unknown", "", "JWE", JSONObject.NULL, 42).forEach { format ->
            assertNull(
                "Format=$format",
                RadarVerifiedLocationToken.fromJson(response().put("format", format))
            )
        }
    }

    @Test
    fun existingNineArgumentJavaConstructorIsRetained() {
        val original = parse(response())
        val constructor = RadarVerifiedLocationToken::class.java.constructors.single {
            it.parameterTypes.size == 9
        }

        val token = constructor.newInstance(
            original.user,
            original.events,
            original.token,
            original.expiresAt,
            original.expiresIn,
            original.passed,
            original.failureReasons,
            original._id,
            original.fullJson
        ) as RadarVerifiedLocationToken

        assertEquals(RadarResponseFormat.JWS, token.format)
        assertEquals(original.token, token.token)
    }

    @Test
    fun allFormatsParseVerificationLevels() {
        listOf("jws", "jwe", "id").forEach { format ->
            val ipStepUp = parse(
                response(format)
                    .put("verificationLevel", "ip")
                    .put("requiredVerificationLevel", "precise")
            )
            assertEquals(format, RadarVerificationLevel.IP, ipStepUp.verificationLevel)
            assertEquals(format, RadarVerificationLevel.PRECISE, ipStepUp.requiredVerificationLevel)

            val precise = parse(response(format).put("verificationLevel", "precise"))
            assertEquals(format, RadarVerificationLevel.PRECISE, precise.verificationLevel)
            assertNull(format, precise.requiredVerificationLevel)
        }
    }

    @Test
    fun missingVerificationLevelsAreNull() {
        listOf("jws", "jwe", "id").forEach { format ->
            val token = parse(response(format))

            assertNull(format, token.verificationLevel)
            assertNull(format, token.requiredVerificationLevel)
        }
    }

    @Test
    fun invalidVerificationLevelsAreNullAndDoNotRejectTheToken() {
        listOf("jws", "jwe", "id").forEach { format ->
            listOf("unknown", "", "IP", JSONObject.NULL, 42, true, JSONObject()).forEach { value ->
                val token = RadarVerifiedLocationToken.fromJson(
                    response(format)
                        .put("verificationLevel", value)
                        .put("requiredVerificationLevel", value)
                )

                assertNotNull("$format: $value", token)
                assertNull("$format: $value", token!!.verificationLevel)
                assertNull("$format: $value", token.requiredVerificationLevel)
            }
        }
    }

    @Test
    fun reducedResponsesKeepVerificationLevels() {
        listOf("jwe", "id").forEach { format ->
            val json = parse(
                response(format)
                    .put("verificationLevel", "ip")
                    .put("requiredVerificationLevel", "precise")
            ).toJson()

            assertEquals(format, "ip", json.getString("verificationLevel"))
            assertEquals(format, "precise", json.getString("requiredVerificationLevel"))
        }
    }

    @Test
    fun cacheValidityUsesLifetimeAndVerdictWithoutBorderData() {
        val nearBorder = response().apply {
            getJSONObject("user").put(
                "state",
                JSONObject().put("distanceToBorder", 1)
            )
        }

        listOf(response(), nearBorder, response("jwe")).forEach { obj ->
            val manager = cachedManager(obj)
            assertTrue(manager.isLastTokenValid())

            ShadowSystemClock.advanceBy(Duration.ofSeconds(90))
            assertFalse(manager.isLastTokenValid())

            assertFalse(cachedManager(JSONObject(obj.toString()).put("passed", false)).isLastTokenValid())
            assertFalse(cachedManager(JSONObject(obj.toString()).put("expiresIn", 0)).isLastTokenValid())
        }
    }

    @Test
    fun absentAndIdTokensAreNotReusable() {
        assertFalse(cachedManager(null).isLastTokenValid())
        assertFalse(cachedManager(response("id")).isLastTokenValid())
    }

    private fun cachedManager(obj: JSONObject?): RadarVerificationManager {
        val context: Context = ApplicationProvider.getApplicationContext()
        val logger = RadarLogger(context)
        Radar.logger = logger

        val manager = RadarVerificationManager(
            context,
            logger,
            RadarRevealRiskManager(context, logger)
        )
        val token = obj?.let(::parse)

        RadarVerificationManager::class.java.getDeclaredField("lastToken").apply {
            isAccessible = true
            set(manager, token)
        }
        RadarVerificationManager::class.java.getDeclaredField("lastTokenElapsedRealtime").apply {
            isAccessible = true
            setLong(manager, SystemClock.elapsedRealtime())
        }

        return manager
    }
}
