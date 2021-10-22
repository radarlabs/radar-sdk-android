package io.radar.sdk

import android.Manifest
import android.location.Location
import android.text.TextUtils
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import io.mockk.every
import io.mockk.slot
import io.mockk.spyk
import io.radar.sdk.model.RadarContext
import io.radar.sdk.model.RadarTrip
import org.awaitility.kotlin.await
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Assume.assumeFalse
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.*

/**
 * These instrumented tests for [RadarApiClient] require Android's HttpUrlConnection implementation to work. Set the
 * endpoint and API key in local or global gradle.properties.
 *
 * @see <a href="https://github.com/robolectric/robolectric/issues/6769">
 *     HttpUrlConnection not shadowed & fails on PATCH requests</a>
 */
@RunWith(AndroidJUnit4::class)
class RadarApiClientInstrumentationTest {

    @get:Rule
    val grantPermissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )

    private val apiKey: String? = BuildConfig::class.java.getDeclaredField("radar_test_key").get(null) as? String
    private val apiHost: String? = BuildConfig::class.java.getDeclaredField("radar_test_host").get(null) as? String
    private val apiHelper = RadarApiHelper(
        logger = RadarLogger(RadarApplication(ApplicationProvider.getApplicationContext(), null))
    )
    private val interceptor = spyk(apiHelper)
    private val app = RadarApplication(
        context = ApplicationProvider.getApplicationContext(),
        receiver = null,
        apiHelper = interceptor
    )
    private val api = app.apiClient
    private var status: Radar.RadarStatus? = null
    private var response: JSONObject? = null
    private var context: RadarContext? = null

    private val location: Location = Location("test")

    init {
        location.latitude = 40.6782
        location.longitude = -73.9442
    }

    @Before
    fun setUp() {
        assumeFalse(TextUtils.isEmpty(apiKey))
        assumeFalse(TextUtils.isEmpty(apiHost))
        val slot = slot<RadarApiRequest>()
        every { interceptor.request(capture(slot)) } answers {
            val request = slot.captured
            apiHelper.request(
                RadarApiRequest.Builder(request.method, request.url, request.sleep)
                    .headers(request.headers)
                    .params(request.params)
                    .callback(object : RadarApiHelper.RadarApiCallback {
                        override fun onComplete(status: Radar.RadarStatus, res: JSONObject?) {
                            this@RadarApiClientInstrumentationTest.status = status
                            this@RadarApiClientInstrumentationTest.response = res
                            request.callback?.onComplete(status, res)
                        }

                    })
                    .build()
            )
        }
        app.settings.setLogLevel(Radar.RadarLogLevel.DEBUG)
        app.settings.setPublishableKey(apiKey)
        app.settings.setHost(apiHost)
    }

    @After
    fun tearDown() {
        app.settings.setPublishableKey(null)
        app.settings.setHost(null)
        status = null
        response = null
        context = null
    }

    /**
     * Tests [RadarApiClient.updateTrip]
     */
    @Test
    fun testUpdateTrip() {
        val tripOptions = RadarTripOptions(
            externalId = "1",
            metadata = JSONObject(mapOf("foo" to "bar", "baz" to true, "qux" to 1)),
            destinationGeofenceTag = null,
            destinationGeofenceExternalId = null,
            mode = Radar.RadarRouteMode.CAR
        )
        api.updateTrip(tripOptions, RadarTrip.RadarTripStatus.STARTED, null)
        awaitSuccess()
        assertNotNull(response)
        status = null
        response = null
        api.updateTrip(tripOptions, RadarTrip.RadarTripStatus.ARRIVED, null)
        awaitSuccess()
        assertNotNull(response)
        status = null
        response = null
        api.updateTrip(tripOptions, RadarTrip.RadarTripStatus.COMPLETED, null)
        awaitSuccess()
        assertNotNull(response)
    }

    private fun awaitSuccess() {
        await.until {
            if (status != null) {
                if (status != Radar.RadarStatus.SUCCESS) {
                    println(status)
                    //Change back to null to prevent excessive logging.
                    status = null
                }
            }
            status == Radar.RadarStatus.SUCCESS
        }
    }
}