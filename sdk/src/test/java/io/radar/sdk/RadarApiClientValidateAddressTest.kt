package io.radar.sdk

import android.content.Context
import android.os.Build
import androidx.core.net.toUri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.radar.sdk.helpers.RadarApiHelperMock
import io.radar.sdk.model.RadarAddress
import io.radar.sdk.model.RadarCoordinate
import java.net.URL
import java.net.URLDecoder
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
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

    private fun send(address: RadarAddress): Radar.RadarStatus? {
        var result: Radar.RadarStatus? = null
        apiClient.validateAddress(
            address,
            object : RadarApiClient.RadarValidateAddressAPICallback {
                override fun onComplete(
                    status: Radar.RadarStatus,
                    res: JSONObject?,
                    address: RadarAddress?,
                    verificationStatus: Radar.RadarAddressVerificationStatus?
                ) {
                    result = status
                }
            }
        )
        return result
    }

    /**
     * Sends the address and returns the decoded query parameters of the URL that `RadarApiHelper`
     * would request, built the same way it builds it.
     */
    private fun validate(address: RadarAddress): Map<String, String> {
        send(address)
        val path = apiHelperMock.lastCapturedPath!!
        val url = URL(RadarSettings.getHost(context).toUri().buildUpon().appendEncodedPath(path).build().toString())
        assertEquals("/v1/addresses/validate", url.path)
        return url.query.split("&").associate { pair ->
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

        assertEquals("1st & Main=A+B", params["addressLabel"])
        assertEquals("Apt #2 & 3", params["unit"])
    }

    @Test
    fun `missing postal code returns bad request without sending a request`() {
        val status = send(address(addressLabel = "841 Broadway", postalCode = null))

        assertEquals(Radar.RadarStatus.ERROR_BAD_REQUEST, status)
        assertNull(apiHelperMock.lastCapturedPath)
    }

    @Test
    fun `no street and number or label returns bad request without sending a request`() {
        val status = send(address(street = "Broadway"))

        assertEquals(Radar.RadarStatus.ERROR_BAD_REQUEST, status)
        assertNull(apiHelperMock.lastCapturedPath)
    }
}
