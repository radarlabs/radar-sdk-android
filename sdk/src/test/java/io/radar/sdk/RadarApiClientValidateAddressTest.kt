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

    private fun send(address: RadarAddress): Result? {
        var result: Result? = null
        apiClient.validateAddress(
            address,
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
    private fun validate(address: RadarAddress): Map<String, String> {
        send(address)
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
}
