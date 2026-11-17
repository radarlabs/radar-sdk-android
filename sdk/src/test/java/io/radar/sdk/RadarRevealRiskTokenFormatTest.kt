package io.radar.sdk.model

import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.UPSIDE_DOWN_CAKE])
class RadarRevealRiskTokenFormatTest {

    private fun response(format: String? = null): JSONObject =
        JSONObject().apply {
            put("meta", JSONObject().put("code", 200))
            put("_id", "risk-id")
            put("token", "opaque-token")
            put("expiresAt", "2030-01-01T00:00:00.000Z")
            put("expiresIn", 90)
            put("risk", JSONObject().put("level", "low").put("reasons", JSONArray()))
            put("network", JSONObject())
            put("device", JSONObject())
            put("extra", "legacy-field")
            if (format != null) {
                put("format", format)
            }
        }

    private fun parse(obj: JSONObject): RadarRevealRiskToken =
        requireNotNull(RadarRevealRiskToken.fromJson(obj))

    @Test
    fun legacyAndExplicitJwsPreserveUnsignedResponses() {
        listOf(null, "jws").forEach { format ->
            val obj = response(format).apply {
                remove("token")
                remove("expiresAt")
                remove("expiresIn")
            }
            val token = parse(obj)

            assertEquals(RadarResponseFormat.JWS, token.format)
            assertNotNull(token.risk)
            assertNotNull(token.network)
            assertNotNull(token.device)
            assertNull(token.token)
            assertNull(token.expiresAt)
            assertNull(token.expiresIn)
            assertEquals("legacy-field", token.toJson().getString("extra"))
            assertFalse(token.toJson().has("meta"))
            assertTrue(obj.has("meta"))
        }
    }

    @Test
    fun jwsStillRequiresRiskNetworkAndDevice() {
        listOf("risk", "network", "device").forEach { field ->
            val missing = response().apply { remove(field) }
            assertNull(field, RadarRevealRiskToken.fromJson(missing))

            listOf(JSONObject.NULL, "not-an-object").forEach { value ->
                assertNull(
                    "$field=$value",
                    RadarRevealRiskToken.fromJson(response().put(field, value))
                )
            }
        }
    }

    @Test
    fun jweExposesOnlyItsClearFields() {
        val obj = response("jwe")
        val token = parse(obj)

        assertEquals(RadarResponseFormat.JWE, token.format)
        assertEquals("risk-id", token.id)
        assertEquals("opaque-token", token.token)
        assertEquals("2030-01-01T00:00:00.000Z", token.expiresAt)
        assertEquals(90, token.expiresIn)
        assertNull(token.risk)
        assertNull(token.network)
        assertNull(token.device)
        assertEquals(
            setOf("format", "_id", "token", "expiresAt", "expiresIn"),
            token.toJson().keys().asSequence().toSet()
        )
        assertTrue(obj.has("risk"))
        assertTrue(obj.has("meta"))
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.M, Build.VERSION_CODES.UPSIDE_DOWN_CAKE])
    fun jweAcceptsExpirationWithAndWithoutFractionalSeconds() {
        listOf(
            "2030-01-01T00:00:00.000Z",
            "2030-01-01T00:00:00Z"
        ).forEach { expiration ->
            assertEquals(
                expiration,
                parse(response("jwe").put("expiresAt", expiration)).expiresAt
            )
        }
    }

    @Test
    fun jweRejectsMissingAndMalformedRequiredFields() {
        val invalidValues = mapOf(
            "_id" to listOf(JSONObject.NULL, "", 42),
            "token" to listOf(JSONObject.NULL, "", 42),
            "expiresAt" to listOf(JSONObject.NULL, "", "not-a-date", 42),
            "expiresIn" to listOf(JSONObject.NULL, "90", true, 1.5, 2147483648L)
        )

        invalidValues.forEach { (field, values) ->
            val missing = response("jwe").apply { remove(field) }
            assertNull("Missing $field", RadarRevealRiskToken.fromJson(missing))

            values.forEach { value ->
                assertNull(
                    "$field=$value",
                    RadarRevealRiskToken.fromJson(response("jwe").put(field, value))
                )
            }
        }
    }

    @Test
    fun idExposesOnlyItsIdAndFormat() {
        val token = parse(response("id"))

        assertEquals(RadarResponseFormat.ID, token.format)
        assertEquals("risk-id", token.id)
        assertNull(token.risk)
        assertNull(token.network)
        assertNull(token.device)
        assertNull(token.token)
        assertNull(token.expiresAt)
        assertNull(token.expiresIn)
        assertEquals(
            setOf("format", "_id"),
            token.toJson().keys().asSequence().toSet()
        )
    }

    @Test
    fun idRejectsMissingAndMalformedIds() {
        assertNull(
            RadarRevealRiskToken.fromJson(JSONObject().put("format", "id"))
        )

        listOf(JSONObject.NULL, "", 42).forEach { id ->
            assertNull(
                "ID=$id",
                RadarRevealRiskToken.fromJson(response("id").put("_id", id))
            )
        }
    }

    @Test
    fun invalidFormatsDoNotFallBackToJws() {
        assertNull(RadarRevealRiskToken.fromJson(null))

        listOf("unknown", "", "JWE", JSONObject.NULL, 42).forEach { format ->
            assertNull(
                "Format=$format",
                RadarRevealRiskToken.fromJson(response().put("format", format))
            )
        }
    }

    @Test
    fun existingJavaAndKotlinDefaultConstructorsAreRetained() {
        val original = parse(response())
        val constructors = RadarRevealRiskToken::class.java.constructors
        val javaConstructor = constructors.single { it.parameterTypes.size == 8 }

        val copy = javaConstructor.newInstance(
            original.id,
            original.risk,
            original.network,
            original.device,
            original.token,
            original.expiresAt,
            original.expiresIn,
            original.fullJson
        ) as RadarRevealRiskToken

        assertEquals(original.token, copy.token)
        assertEquals(RadarResponseFormat.JWS, copy.format)

        val defaultConstructor = constructors.single {
            it.parameterTypes.lastOrNull()?.name ==
                "kotlin.jvm.internal.DefaultConstructorMarker"
        }
        assertEquals(10, defaultConstructor.parameterTypes.size)

        val unsigned = RadarRevealRiskToken(
            id = original.id,
            risk = original.risk,
            network = original.network,
            device = original.device,
            fullJson = JSONObject()
        )
        assertNull(unsigned.token)
        assertEquals(RadarResponseFormat.JWS, unsigned.format)
    }
}
