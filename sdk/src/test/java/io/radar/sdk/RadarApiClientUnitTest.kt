package io.radar.sdk

import android.location.Location
import android.os.Build
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.every
import io.mockk.slot
import io.mockk.spyk
import io.radar.sdk.model.RadarAddress
import io.radar.sdk.model.RadarBeacon
import io.radar.sdk.model.RadarContext
import io.radar.sdk.model.RadarEvent
import io.radar.sdk.model.RadarGeofence
import io.radar.sdk.model.RadarLog
import io.radar.sdk.model.RadarPlace
import io.radar.sdk.model.RadarRouteMatrix
import io.radar.sdk.model.RadarRoutes
import io.radar.sdk.model.RadarTrip
import io.radar.sdk.model.RadarUser
import org.awaitility.kotlin.await
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.net.URL
import java.util.*

/**
 * Unit tests [RadarApiClient].
 *
 * @see [RadarApiClientIntegrationTest]
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.P])
class RadarApiClientUnitTest {

    private val apiHelper = RadarApiHelperMock()
    private val interceptor = spyk(apiHelper.helper)
    private val app = RadarApplication(
        context = ApplicationProvider.getApplicationContext(),
        receiver = null,
        apiHelper = interceptor
    )
    private val api = app.apiClient
    private var status: Radar.RadarStatus? = null
    private var request: RadarApiRequest? = null

    private val location: Location = Location("test")

    init {
        location.latitude = 40.6782
        location.longitude = -73.9442
    }

    @Before
    fun setUp() {
        val slot = slot<RadarApiRequest>()
        every { interceptor.request(capture(slot)) } answers {
            val request = slot.captured
            this@RadarApiClientUnitTest.request = request
            apiHelper.mockStatus = Radar.RadarStatus.SUCCESS
            apiHelper.helper.request(
                RadarApiRequest.Builder(request.method, request.url, request.sleep)
                    .headers(request.headers)
                    .params(request.params)
                    .callback(object : RadarApiHelper.RadarApiCallback {
                        override fun onComplete(status: Radar.RadarStatus, res: JSONObject?) {
                            this@RadarApiClientUnitTest.status = status
                            request.callback?.onComplete(status, res)
                        }
                    })
                    .build()
            )
        }
        app.settings.setLogLevel(Radar.RadarLogLevel.DEBUG)
        app.settings.setPublishableKey(RadarTest.publishableKey)
    }

    @After
    fun tearDown() {
        app.settings.setPublishableKey(null)
        app.settings.setHost(null)
        status = null
        request = null
    }

    private fun Location.coords(): String {
        return "$latitude,$longitude"
    }

    private fun verifyRequest(request: RadarApiRequest) {
        assertNotNull(this.request)
        assertEquals(request.method, this.request!!.method)
        assertEquals(request.url, this.request!!.url)
        assertEquals(request.headers, this.request!!.headers)
        if (this.request!!.params == null) {
            assertNull(request.params)
        } else {
            assertEquals(request.params!!.length(), this.request!!.params!!.length())
            request.params.keys().forEach { key ->
                val expected = request.params[key]
                val actual = this.request!!.params!![key]
                assertEquals(expected, actual)
            }
        }
        assertEquals(request.sleep, this.request!!.sleep)
    }

    /**
     * Helps test success and failure branches of API methods
     */
    private fun executeApiCall(call: () -> Unit) {
        app.settings.setPublishableKey(null)
        call.invoke()
        awaitFailure()
        app.settings.setPublishableKey(RadarTest.publishableKey)
        call.invoke()
        awaitSuccess()
    }

    private fun getDefaultHeaders(): Map<String, String> {
        return mapOf(
            "Authorization" to RadarTest.publishableKey,
            "Content-Type" to "application/json",
            "X-Radar-Config" to "true",
            "X-Radar-Device-Make" to RadarUtils.deviceMake,
            "X-Radar-Device-Model" to RadarUtils.deviceModel,
            "X-Radar-Device-OS" to RadarUtils.deviceOS,
            "X-Radar-Device-Type" to RadarUtils.deviceType,
            "X-Radar-SDK-Version" to RadarUtils.sdkVersion
        )
    }

    @Test
    fun testGetConfig() {
        app.settings.setConfig(null)
        assertNull(app.settings.getConfig())
        api.getConfig()
        awaitSuccess()
        verifyRequest(
            RadarApiRequest.get(
                URL(
                    "https://api.radar.io/v1/config" +
                            "?installId=${app.settings.getInstallId()}" +
                            "&sessionId=${app.settings.getSessionId()}" +
                            "&locationAuthorization=NOT_DETERMINED" +
                            "&locationAccuracyAuthorization=FULL"
                ), false
            ).headers(getDefaultHeaders()).build()
        )
    }

    @Test
    fun testTrack() {
        mockDeviceId()
        executeApiCall {
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
                        this@RadarApiClientUnitTest.status = status
                    }

                }
            )
        }
    }

    @Test
    fun testGetContext() {
        executeApiCall {
            api.getContext(location, object : RadarApiClient.RadarContextApiCallback {
                override fun onComplete(status: Radar.RadarStatus, res: JSONObject?, context: RadarContext?) {
                    this@RadarApiClientUnitTest.status = status
                }
            })
        }
        verifyRequest(
            RadarApiRequest.get(
                URL("https://api.radar.io/v1/context?coordinates=${location.coords()}"),
                false
            ).headers(getDefaultHeaders()).build()
        )
    }

    private fun searchPlaces(chains: Array<String>?, categories: Array<String>?, groups: Array<String>?) {
        executeApiCall {
            api.searchPlaces(
                location = location,
                radius = 1,
                chains = chains,
                categories = categories,
                groups = groups,
                limit = 10,
                callback = object : RadarApiClient.RadarSearchPlacesApiCallback {
                    override fun onComplete(status: Radar.RadarStatus, res: JSONObject?, places: Array<RadarPlace>?) {
                        this@RadarApiClientUnitTest.status = status
                    }
                })
        }
        val search = when {
            chains != null -> {
                "&chains=${chains.joinToString(",")}"
            }
            categories != null -> {
                "&categories=${categories.joinToString(",")}"
            }
            groups != null -> {
                "&groups=${groups.joinToString(",")}"
            }
            else -> {
                throw AssertionError("Must pass one non-null parameter")
            }
        }
        verifyRequest(
            RadarApiRequest.get(
                URL(
                    "https://api.radar.io/v1/search/places" +
                            "?near=${location.coords()}" +
                            "&radius=1" +
                            "&limit=10" +
                            search
                ),
                false
            ).headers(getDefaultHeaders()).build()
        )
    }

    @Test
    fun testSearchPlaces() {
        searchPlaces(arrayOf("radar"), null, null)
        searchPlaces(null, arrayOf("restaurant", "bar"), null)
        searchPlaces(null, null, arrayOf("major-us-airport"))
    }

    @Test
    fun testSearchGeofences() {
        executeApiCall {
            api.searchGeofences(
                location = location,
                radius = 1,
                tags = arrayOf("restaurant", "bar"),
                metadata = JSONObject(mapOf("foo" to "bar", "ra" to "dar")),
                limit = 10,
                callback = object : RadarApiClient.RadarSearchGeofencesApiCallback {
                    override fun onComplete(
                        status: Radar.RadarStatus,
                        res: JSONObject?,
                        geofences: Array<RadarGeofence>?
                    ) {
                        this@RadarApiClientUnitTest.status = status
                    }
                })
        }
        verifyRequest(
            RadarApiRequest.get(
                URL(
                    "https://api.radar.io/v1/search/geofences" +
                            "?near=${location.coords()}" +
                            "&radius=1" +
                            "&limit=10" +
                            "&tags=restaurant,bar" +
                            "&metadata[foo]=bar" +
                            "&metadata[ra]=dar"
                ),
                false
            ).headers(getDefaultHeaders()).build()
        )
    }

    @Test
    fun testSearchBeacons() {
        executeApiCall {
            api.searchBeacons(location, 1, 10, object : RadarApiClient.RadarSearchBeaconsApiCallback {
                override fun onComplete(status: Radar.RadarStatus, res: JSONObject?, beacons: Array<RadarBeacon>?) {
                    this@RadarApiClientUnitTest.status = status
                }
            })
        }
        verifyRequest(
            RadarApiRequest.get(
                URL(
                    "https://api.radar.io/v1/search/beacons" +
                            "?near=${location.coords()}" +
                            "&radius=1" +
                            "&limit=10"
                ),
                false
            ).headers(getDefaultHeaders()).build()
        )
    }

    @Test
    fun testAutocomplete() {
        executeApiCall {
            api.autocomplete(
                query = "brooklyn roasting",
                near = location,
                layers = null,
                limit = 10,
                country = "USA",
                callback = object : RadarApiClient.RadarGeocodeApiCallback {
                    override fun onComplete(
                        status: Radar.RadarStatus,
                        res: JSONObject?,
                        addresses: Array<RadarAddress>?
                    ) {
                        this@RadarApiClientUnitTest.status = status
                    }
                })
        }
        verifyRequest(
            RadarApiRequest.get(
                URL(
                    "https://api.radar.io/v1/search/autocomplete" +
                            "?query=brooklyn%20roasting" +
                            "&near=${location.coords()}" +
                            "&limit=10" +
                            "&country=USA"
                ),
                false
            ).headers(getDefaultHeaders()).build()
        )
    }

    @Test
    fun testUpdateTrip() {
        val tripId = UUID.randomUUID().toString()
        val apiCall = {
            api.updateTrip(
                RadarTripOptions(tripId),
                RadarTrip.RadarTripStatus.STARTED,
                object : RadarApiClient.RadarTripApiCallback {
                    override fun onComplete(
                        status: Radar.RadarStatus,
                        res: JSONObject?,
                        trip: RadarTrip?,
                        events: Array<RadarEvent>?
                    ) {
                        this@RadarApiClientUnitTest.status = status
                    }

                })
        }
        app.settings.setPublishableKey(null)
        apiCall.invoke()
        awaitFailure()
        app.settings.setPublishableKey(RadarTest.publishableKey)
        try {
            apiCall.invoke()
        } catch (ignored: Exception) {
            //PATCH not supported in Junit test, but the request parameters can still be verified
        }
        verifyRequest(
            RadarApiRequest.Builder("PATCH", URL("https://api.radar.io/v1/trips/$tripId"), false)
                .headers(getDefaultHeaders())
                .params(JSONObject(mapOf("status" to "started", "mode" to "car")))
                .build()
        )
    }

    @Test
    fun testVerifyEvent() {
        val eventId = UUID.randomUUID().toString()
        val placeId = UUID.randomUUID().toString()
        api.verifyEvent(eventId, RadarEvent.RadarEventVerification.ACCEPT, placeId)
        awaitSuccess()
        val params = JSONObject()
        params.put("verification", RadarEvent.RadarEventVerification.ACCEPT)
        params.put("verifiedPlaceId", placeId)
        verifyRequest(
            RadarApiRequest.Builder("PUT", URL("https://api.radar.io/v1/events/$eventId/verification"), false)
                .headers(getDefaultHeaders())
                .params(params)
                .build()
        )
    }

    @Test
    fun testGeocode() {
        executeApiCall {
            api.geocode("20 jay street brooklyn ny", object : RadarApiClient.RadarGeocodeApiCallback {
                override fun onComplete(status: Radar.RadarStatus, res: JSONObject?, addresses: Array<RadarAddress>?) {
                    this@RadarApiClientUnitTest.status = status
                }
            })
        }
        verifyRequest(
            RadarApiRequest.get(
                URL("https://api.radar.io/v1/geocode/forward?query=20%20jay%20street%20brooklyn%20ny"),
                false
            ).headers(getDefaultHeaders()).build()
        )
    }

    @Test
    fun testReverseGeocode() {
        executeApiCall {
            api.reverseGeocode(location, object : RadarApiClient.RadarGeocodeApiCallback {
                override fun onComplete(status: Radar.RadarStatus, res: JSONObject?, addresses: Array<RadarAddress>?) {
                    this@RadarApiClientUnitTest.status = status
                }
            })
        }
        verifyRequest(
            RadarApiRequest.get(
                URL("https://api.radar.io/v1/geocode/reverse?coordinates=${location.coords()}"),
                false
            ).headers(getDefaultHeaders()).build()
        )
    }

    @Test
    fun testIpGeocode() {
        executeApiCall {
            api.ipGeocode(object : RadarApiClient.RadarIpGeocodeApiCallback {
                override fun onComplete(
                    status: Radar.RadarStatus,
                    res: JSONObject?,
                    address: RadarAddress?,
                    proxy: Boolean
                ) {
                    this@RadarApiClientUnitTest.status = status
                }
            })
        }
        verifyRequest(
            RadarApiRequest.get(URL("https://api.radar.io/v1/geocode/ip"), false)
                .headers(getDefaultHeaders())
                .build()
        )
    }

    @Test
    fun testGetDistance() {
        val destination = Location(location)
        destination.latitude++
        destination.longitude--
        executeApiCall {
            api.getDistance(
                origin = location,
                destination = destination,
                modes = EnumSet.of(Radar.RadarRouteMode.BIKE, Radar.RadarRouteMode.FOOT),
                units = Radar.RadarRouteUnits.IMPERIAL,
                geometryPoints = 20,
                callback = object : RadarApiClient.RadarDistanceApiCallback {
                    override fun onComplete(status: Radar.RadarStatus, res: JSONObject?, routes: RadarRoutes?) {
                        this@RadarApiClientUnitTest.status = status
                    }
                })
        }
        verifyRequest(
            RadarApiRequest.get(
                URL(
                    "https://api.radar.io/v1/route/distance" +
                            "?origin=${location.coords()}" +
                            "&destination=${destination.coords()}" +
                            "&modes=foot,bike" +
                            "&units=imperial" +
                            "&geometry=linestring" +
                            "&geometryPoints=20"
                ),
                false
            ).headers(getDefaultHeaders()).build()
        )
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

        executeApiCall {
            api.getMatrix(
                origins = arrayOf(brooklyn, manhattan),
                destinations = arrayOf(atlanticCity, trenton),
                mode = Radar.RadarRouteMode.TRUCK,
                units = Radar.RadarRouteUnits.METRIC,
                callback = object : RadarApiClient.RadarMatrixApiCallback {
                    override fun onComplete(status: Radar.RadarStatus, res: JSONObject?, matrix: RadarRouteMatrix?) {
                        this@RadarApiClientUnitTest.status = status
                    }
                }
            )
        }
        verifyRequest(
            RadarApiRequest.get(
                URL(
                    "https://api.radar.io/v1/route/matrix" +
                            "?origins=${brooklyn.coords()}|${manhattan.coords()}" +
                            "&destinations=${atlanticCity.coords()}|${trenton.coords()}" +
                            "&mode=truck" +
                            "&units=metric"
                ),
                false
            ).headers(getDefaultHeaders()).build()
        )
    }

    @Test
    fun testLog() {
        val size = kotlin.random.Random.nextInt(10)
        val list = mutableListOf<RadarLog>()
        repeat(size) { list += RadarLog(Radar.RadarLogLevel.DEBUG, UUID.randomUUID().toString()) }
        api.log(list, object : RadarApiClient.RadarLogCallback {
            override fun onComplete(status: Radar.RadarStatus, res: JSONObject?) = Unit
        })
        awaitSuccess()
    }

    private fun awaitSuccess() {
        await.until { status == Radar.RadarStatus.SUCCESS }
    }

    private fun awaitFailure() {
        await.until { status == Radar.RadarStatus.ERROR_PUBLISHABLE_KEY }
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