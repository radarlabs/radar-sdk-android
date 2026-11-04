package io.radar.sdk

import android.content.Context
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.radar.sdk.helpers.RadarApiHelperMock
import io.radar.sdk.model.RadarAddress
import io.radar.sdk.model.RadarCoordinate
import java.net.URLDecoder
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.UPSIDE_DOWN_CAKE])
class RadarApiClientValidateAddressTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val apiHelperMock = RadarApiHelperMock()
    private lateinit var apiClient: RadarApiClient

    @Before
    fun setUp() {
        RadarSettings.setPublishableKey(context, "prj_test_pk_0000000000000000000000000000000000000000")
        apiClient = RadarApiClient(context, RadarLogger(context))
        apiClient.apiHelper = apiHelperMock
        apiHelperMock.mockStatus = Radar.RadarStatus.SUCCESS
        apiHelperMock.mockResponse = JSONObject()
        apiHelperMock.clearCapturedParams()
    }

    private fun address(
        street: String? = null,
        number: String? = null,
        addressLabel: String? = null,
        unit: String? = null,
        postalCode: String? = "10003"
    ) = RadarAddress(
        coordinate = RadarCoordinate(40.7342, -73.9911),
        countryCode = "US",
        stateCode = "NY",
        city = "New York",
        postalCode = postalCode,
        street = street,
        number = number,
        addressLabel = addressLabel,
        unit = unit
    )

    private class Result(
        val status: Radar.RadarStatus,
        val address: RadarAddress?,
        val verificationStatus: Radar.RadarAddressVerificationStatus?
    )

    private fun streetRequest(
        street: String = "Broadway",
        number: String = "841",
        unit: String? = null,
        city: String = "New York",
        stateCode: String = "NY",
        postalCode: String = "10003",
        countryCode: String = "US"
    ) = RadarValidateAddressRequest(
        RadarValidateAddressRequest.Line.StreetAndNumber(street, number),
        unit,
        city,
        stateCode,
        postalCode,
        countryCode
    )

    private fun labelRequest(
        addressLabel: String = "841 Broadway",
        unit: String? = null,
        city: String = "New York",
        stateCode: String = "NY",
        postalCode: String = "10003",
        countryCode: String = "US"
    ) = RadarValidateAddressRequest(
        RadarValidateAddressRequest.Line.Label(addressLabel),
        unit,
        city,
        stateCode,
        postalCode,
        countryCode
    )

    private fun send(address: RadarAddress): Result? = send(RadarValidateAddressRequest.from(address))

    private fun send(request: RadarValidateAddressRequest): Result? {
        var result: Result? = null
        apiClient.validateAddress(
            request,
            object : RadarApiClient.RadarValidateAddressAPICallback {
                override fun onComplete(
                    status: Radar.RadarStatus,
                    res: JSONObject?,
                    address: RadarAddress?,
                    verificationStatus: Radar.RadarAddressVerificationStatus?
                ) {
                    result = Result(status, address, verificationStatus)
                }
            }
        )
        return result
    }

    /**
     * Sends the address and returns the decoded query parameters of the captured request path.
     */
    private fun validate(address: RadarAddress): Map<String, String> = validate(RadarValidateAddressRequest.from(address))

    private fun validate(request: RadarValidateAddressRequest): Map<String, String> {
        send(request)
        val (path, query) = apiHelperMock.lastCapturedPath!!.split("?", limit = 2)
        assertEquals("v1/addresses/validate", path)
        return query.split("&").associate { pair ->
            val (key, value) = pair.split("=", limit = 2)
            URLDecoder.decode(key, "UTF-8") to URLDecoder.decode(value, "UTF-8")
        }
    }

    @Test
    fun `street, number, and label sends only street and number`() {
        val params = validate(address(street = "Broadway", number = "841", addressLabel = "841 Broadway"))

        assertEquals("Broadway", params["street"])
        assertEquals("841", params["number"])
        assertFalse(params.containsKey("addressLabel"))
        assertEquals("US", params["countryCode"])
        assertEquals("NY", params["stateCode"])
        assertEquals("New York", params["city"])
        assertEquals("10003", params["postalCode"])
    }

    @Test
    fun `label only sends the label`() {
        val params = validate(address(addressLabel = "841 Broadway"))

        assertEquals("841 Broadway", params["addressLabel"])
        assertFalse(params.containsKey("street"))
        assertFalse(params.containsKey("number"))
    }

    @Test
    fun `street without a number sends the label`() {
        val params = validate(address(street = "Broadway", addressLabel = "841 Broadway"))

        assertEquals("841 Broadway", params["addressLabel"])
        assertFalse(params.containsKey("street"))
        assertFalse(params.containsKey("number"))
    }

    @Test
    fun `empty street sends the label`() {
        val params = validate(address(street = "", number = "841", addressLabel = "841 Broadway"))

        assertEquals("841 Broadway", params["addressLabel"])
        assertFalse(params.containsKey("street"))
        assertFalse(params.containsKey("number"))
    }

    @Test
    fun `reserved characters in values round-trip`() {
        val params = validate(address(addressLabel = "1st & Main=A+B", unit = "Apt #2 & 3"))

        val path = apiHelperMock.lastCapturedPath!!
        assertTrue(path, path.contains("addressLabel=1st%20%26%20Main%3DA%2BB"))
        assertTrue(path, path.contains("unit=Apt%20%232%20%26%203"))
        assertEquals("1st & Main=A+B", params["addressLabel"])
        assertEquals("Apt #2 & 3", params["unit"])
    }

    @Test
    fun `JSON null number sends the label`() {
        val address = RadarAddress.fromJson(
            JSONObject()
                .put("countryCode", "US")
                .put("stateCode", "NY")
                .put("city", "New York")
                .put("postalCode", "10003")
                .put("street", "Broadway")
                .put("number", JSONObject.NULL)
                .put("addressLabel", "841 Broadway")
        )!!

        val params = validate(address)

        assertEquals("841 Broadway", params["addressLabel"])
        assertFalse(params.containsKey("street"))
        assertFalse(params.containsKey("number"))
        assertFalse(params.containsKey("unit"))
    }

    @Test
    fun `success returns the parsed address and verification status`() {
        apiHelperMock.mockResponse = JSONObject()
            .put(
                "address",
                JSONObject()
                    .put("latitude", 40.7342)
                    .put("longitude", -73.9911)
                    .put("number", "841")
                    .put("street", "Broadway")
                    .put("postalCode", "10003")
                    .put("plus4", "4704")
            )
            .put("result", JSONObject().put("verificationStatus", "verified"))

        val result = send(address(street = "Broadway", number = "841"))!!

        assertEquals(Radar.RadarStatus.SUCCESS, result.status)
        assertEquals("841", result.address?.number)
        assertEquals("Broadway", result.address?.street)
        assertEquals("4704", result.address?.plus4)
        assertEquals(Radar.RadarAddressVerificationStatus.VERIFIED, result.verificationStatus)
    }

    @Test
    fun `missing postal code returns bad request without sending a request`() {
        val result = send(address(addressLabel = "841 Broadway", postalCode = null))

        assertEquals(Radar.RadarStatus.ERROR_BAD_REQUEST, result?.status)
        assertNull(apiHelperMock.lastCapturedPath)
    }

    @Test
    fun `no street and number or label returns bad request without sending a request`() {
        val result = send(address(street = "Broadway"))

        assertEquals(Radar.RadarStatus.ERROR_BAD_REQUEST, result?.status)
        assertNull(apiHelperMock.lastCapturedPath)
    }

    @Test
    fun `street form sends only street and number fields`() {
        val params = validate(streetRequest(unit = "4"))

        assertEquals(
            mapOf(
                "countryCode" to "US",
                "stateCode" to "NY",
                "city" to "New York",
                "postalCode" to "10003",
                "street" to "Broadway",
                "number" to "841",
                "unit" to "4"
            ),
            params
        )
    }

    @Test
    fun `label form sends only label fields`() {
        val params = validate(labelRequest(unit = "4"))

        assertEquals(
            mapOf(
                "countryCode" to "US",
                "stateCode" to "NY",
                "city" to "New York",
                "postalCode" to "10003",
                "addressLabel" to "841 Broadway",
                "unit" to "4"
            ),
            params
        )
    }

    @Test
    fun `empty unit is not sent`() {
        val params = validate(labelRequest(unit = ""))

        assertFalse(params.containsKey("unit"))
    }

    @Test
    fun `empty required field returns bad request without sending a request`() {
        val requests = listOf(
            streetRequest(street = ""),
            streetRequest(number = ""),
            streetRequest(city = ""),
            streetRequest(stateCode = ""),
            streetRequest(postalCode = ""),
            streetRequest(countryCode = ""),
            labelRequest(addressLabel = ""),
            labelRequest(city = ""),
            labelRequest(stateCode = ""),
            labelRequest(postalCode = ""),
            labelRequest(countryCode = "")
        )

        for (request in requests) {
            apiHelperMock.clearCapturedParams()

            val result = send(request)

            assertEquals(request.toString(), Radar.RadarStatus.ERROR_BAD_REQUEST, result?.status)
            assertNull(request.toString(), apiHelperMock.lastCapturedPath)
        }
    }

    @Test
    fun `bad request response returns bad request`() {
        apiHelperMock.mockStatus = Radar.RadarStatus.ERROR_BAD_REQUEST
        apiHelperMock.mockResponse = null

        val result = send(streetRequest())!!

        assertEquals(Radar.RadarStatus.ERROR_BAD_REQUEST, result.status)
        assertNull(result.address)
    }

    @Test
    fun `server error response returns server error`() {
        apiHelperMock.mockStatus = Radar.RadarStatus.ERROR_SERVER
        apiHelperMock.mockResponse = null

        val result = send(labelRequest())!!

        assertEquals(Radar.RadarStatus.ERROR_SERVER, result.status)
        assertNull(result.address)
    }
}
