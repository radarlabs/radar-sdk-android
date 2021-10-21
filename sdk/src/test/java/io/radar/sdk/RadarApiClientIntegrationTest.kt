package io.radar.sdk

import android.location.Location
import android.os.Build
import android.provider.Settings
import android.text.TextUtils
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.every
import io.mockk.slot
import io.mockk.spyk
import io.radar.sdk.matchers.RangeMatcher.Companion.isAtLeast
import io.radar.sdk.model.RadarAddress
import io.radar.sdk.model.RadarBeacon
import io.radar.sdk.model.RadarContext
import io.radar.sdk.model.RadarEvent
import io.radar.sdk.model.RadarGeofence
import io.radar.sdk.model.RadarPlace
import io.radar.sdk.model.RadarRouteMatrix
import io.radar.sdk.model.RadarRoutes
import io.radar.sdk.model.RadarUser
import io.radar.sdk.util.FastPostable
import io.radar.sdk.util.RadarLogBuffer
import io.radar.sdk.util.SysOutLogReceiver
import org.awaitility.kotlin.await
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThat
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeFalse
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.android.util.concurrent.InlineExecutorService
import org.robolectric.annotation.Config
import java.util.*

/**
 * Integration tests against a real endpoint. Set the endpoint and api key in local or global gradle.properties.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.P])
class RadarApiClientIntegrationTest {

    private val apiKey = System.getProperty("radar_test_key")
    private val apiHost = System.getProperty("radar_test_host")
    private val logBuffer = RadarLogBuffer(ApplicationProvider.getApplicationContext(), InlineExecutorService())
    private val apiHelper = RadarApiHelper(
        logger = RadarLogger(RadarApplication(
            context = ApplicationProvider.getApplicationContext(),
            receiver = SysOutLogReceiver(),
            logBuffer = logBuffer
        )),
        executor = InlineExecutorService(),
        handler = FastPostable()
    )
    private val interceptor = spyk(apiHelper)
    private val app = RadarApplication(
        context = ApplicationProvider.getApplicationContext(),
        receiver = null,
        apiHelper = interceptor,
        logBuffer = logBuffer
    )
    private val api = app.apiClient
    private var status: Radar.RadarStatus? = null
    private var response: JSONObject? = null
    private var model: Any? = null

    private val location: Location = Location("test").apply {
        latitude = 40.6782
        longitude = -73.9442
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
                            this@RadarApiClientIntegrationTest.status = status
                            this@RadarApiClientIntegrationTest.response = res
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
        app.settings.setId(null)
        status = null
        response = null
        model = null
    }

    @Test
    fun testGetConfigWithoutUserId() {
        assertEquals(0, app.logBuffer.size.get())
        val size = writeLogs()
        assertEquals(size, app.logBuffer.size.get())
        getConfig()
        //Logs won't be pushed if the user id is not set yet (ie no tracking endpoint has been hit)
        assertThat(app.logBuffer.size.get(), isAtLeast(size))
    }

    @Test
    fun testGetConfigWithUserId() {
        assertEquals(0, app.logBuffer.size.get())
        val size = writeLogs()
        assertEquals(size, app.logBuffer.size.get())
        track()
        getConfig()
        //This check verifies that the log buffer cleared out too.
        //The only logs should be for logging the api request & response, but check atLeast,
        //since the logs are added asynchronously
        assertThat(app.logBuffer.size.get(), isAtLeast(2))
    }

    @Test
    fun testTrack() {
        assertEquals(0, app.logBuffer.size.get())
        val size = writeLogs()
        assertEquals(size, app.logBuffer.size.get())
        track()
        assertNotNull(response)
        val user = RadarUser.fromJson(response!!.optJSONObject("user"))
        assertEquals(location.latitude, user!!.location.latitude, 0.0)
        assertEquals(location.longitude, user.location.longitude, 0.0)
        //This check verifies that the log buffer cleared out too.
        //The only logs should be for logging the api request & response, but check atLeast,
        //since the logs are added asynchronously
        assertThat(app.logBuffer.size.get(), isAtLeast(2))
    }

    @Test
    fun testGetContext() {
        api.getContext(location, object : RadarApiClient.RadarContextApiCallback {
            override fun onComplete(status: Radar.RadarStatus, res: JSONObject?, context: RadarContext?) {
                this@RadarApiClientIntegrationTest.model = context
            }
        })
        awaitSuccess()
        assertNotNull(model)
    }

    @Test
    fun testSearchPlaces() {
        api.searchPlaces(
            location = location,
            radius = 1,
            chains = null,
            categories = arrayOf("restaurant", "bar"),
            groups = null,
            limit = 10,
            callback = object : RadarApiClient.RadarSearchPlacesApiCallback {
                override fun onComplete(status: Radar.RadarStatus, res: JSONObject?, places: Array<RadarPlace>?) {
                    model = places
                }
            })
        awaitSuccess()
        assertNotNull(response)
        assertTrue((model as Array<*>).size <= 10)
    }

    @Test
    fun testSearchGeofences() {
        api.searchGeofences(
            location = location,
            radius = 1,
            tags = null,
            metadata = null,
            limit = 10,
            callback = object : RadarApiClient.RadarSearchGeofencesApiCallback {
                override fun onComplete(status: Radar.RadarStatus, res: JSONObject?, geofences: Array<RadarGeofence>?) {
                    model = geofences
                }
            })
        awaitSuccess()
        assertNotNull(response)
        assertTrue((model as Array<*>).size <= 10)
    }

    @Test
    fun testSearchBeacons() {
        api.searchBeacons(location, 1, 10, object : RadarApiClient.RadarSearchBeaconsApiCallback {
            override fun onComplete(status: Radar.RadarStatus, res: JSONObject?, beacons: Array<RadarBeacon>?) {
                model = beacons
            }
        })
        awaitSuccess()
        assertNotNull(response)
        assertTrue((model as Array<*>).size <= 10)
    }

    @Test
    fun testAutocomplete() {
        api.autocomplete(
            query = "brooklyn roasting",
            near = location,
            layers = null,
            limit = 10,
            country = "USA",
            callback = object : RadarApiClient.RadarGeocodeApiCallback {
                override fun onComplete(status: Radar.RadarStatus, res: JSONObject?, addresses: Array<RadarAddress>?) {
                    model = addresses
                }
            })
        awaitSuccess()
        assertNotNull(response)
        assertNotNull(model)
        assertTrue((model as Array<*>).size <= 10)
    }

    @Test
    fun testGeocode() {
        api.geocode("20 jay street brooklyn ny", object : RadarApiClient.RadarGeocodeApiCallback {
            override fun onComplete(status: Radar.RadarStatus, res: JSONObject?, addresses: Array<RadarAddress>?) {
                model = addresses
            }
        })
        awaitSuccess()
        assertNotNull(response)
        assertNotNull(model)
    }

    @Test
    fun testReverseGeocode() {
        api.reverseGeocode(location, object : RadarApiClient.RadarGeocodeApiCallback {
            override fun onComplete(status: Radar.RadarStatus, res: JSONObject?, addresses: Array<RadarAddress>?) {
                model = addresses
            }
        })
        awaitSuccess()
        assertNotNull(response)
        assertNotNull(model)
    }

    @Test
    fun testIpGeocode() {
        api.ipGeocode(object : RadarApiClient.RadarIpGeocodeApiCallback {
            override fun onComplete(
                status: Radar.RadarStatus,
                res: JSONObject?,
                address: RadarAddress?,
                proxy: Boolean
            ) {
                model = address
            }
        })
        awaitSuccess()
        assertNotNull(response)
        assertNotNull(model)
    }

    @Test
    fun testGetDistance() {
        val destination = Location(location)
        destination.latitude++
        destination.longitude--
        api.getDistance(
            origin = location,
            destination = destination,
            modes = EnumSet.of(Radar.RadarRouteMode.FOOT, Radar.RadarRouteMode.BIKE),
            units = Radar.RadarRouteUnits.IMPERIAL,
            geometryPoints = 20,
            callback = object : RadarApiClient.RadarDistanceApiCallback {
                override fun onComplete(status: Radar.RadarStatus, res: JSONObject?, routes: RadarRoutes?) {
                    model = routes
                }
            })
        awaitSuccess()
        assertNotNull(response)
        assertNotNull(model)
    }

    @Test
    fun testGetMatrix() {
        val brooklyn = location

        val manhattan = Location(location)
        manhattan.latitude = 40.7831
        manhattan.longitude = -73.9712

        val atlanticCity = Location(location)
        atlanticCity.latitude = 39.3643
        atlanticCity.longitude = -74.4229

        val trenton = Location(location)
        trenton.latitude = 40.2206
        trenton.longitude = -74.7597

        api.getMatrix(
            origins = arrayOf(brooklyn, manhattan),
            destinations = arrayOf(atlanticCity, trenton),
            mode = Radar.RadarRouteMode.TRUCK,
            units = Radar.RadarRouteUnits.METRIC,
            callback = object : RadarApiClient.RadarMatrixApiCallback {
                override fun onComplete(status: Radar.RadarStatus, res: JSONObject?, matrix: RadarRouteMatrix?) {
                    model = matrix
                }
            }
        )
        awaitSuccess()
        assertNotNull(response)
        assertNotNull(model)
    }

    private fun getConfig() {
        app.settings.setConfig(null)
        assertNull(app.settings.getConfig())
        api.getConfig()
        awaitSuccess()
        assertNotNull(response)
        val meta = response!!.optJSONObject("meta")
        if (meta!!.has("config")) {
            assertNotNull(app.settings.getConfig())
        }
    }

    /**
     * Perform a track request. Do this first for tests that require the user id to be configured in radar settings.
     */
    private fun track() {
        mockDeviceId()
        api.track(
            location = location,
            stopped = false,
            foreground = true,
            source = Radar.RadarLocationSource.MANUAL_LOCATION,
            replayed = false,
            nearbyBeacons = null,
            callback = object : RadarApiClient.RadarTrackApiCallback {
                override fun onComplete(
                    status: Radar.RadarStatus,
                    res: JSONObject?,
                    events: Array<RadarEvent>?,
                    user: RadarUser?,
                    nearbyGeofences: Array<RadarGeofence>?
                ) {
                    this@RadarApiClientIntegrationTest.status = status
                    this@RadarApiClientIntegrationTest.response = res
                }
            })
        awaitSuccess()
    }

    private fun writeLogs(): Int {
        val size = kotlin.random.Random.nextInt(10)
        repeat(size) { app.logBuffer.write(Radar.RadarLogLevel.DEBUG, UUID.randomUUID().toString()) }
        return size
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

    /**
     * This relies on Robolectric to mock the device id, which is accessed internally (see [RadarUtils]) during some
     * API requests.
     */
    private fun mockDeviceId() {
        val deviceId = java.lang.Long.toHexString(Random().nextLong())
        Settings.Secure.putString(app.contentResolver, Settings.Secure.ANDROID_ID, deviceId)
    }
}