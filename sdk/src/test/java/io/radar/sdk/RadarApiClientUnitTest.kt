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
import io.radar.sdk.model.RadarPlace
import io.radar.sdk.model.RadarRouteMatrix
import io.radar.sdk.model.RadarRoutes
import io.radar.sdk.model.RadarUser
import org.awaitility.kotlin.await
import org.json.JSONObject
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.util.*

/**
 * Unit tests [RadarApiClient]. This class makes heavy use of mocks, so it's best to think of this as ensuring the
 * API calls walk through correctly and the callbacks are hit as expected.
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
    }

    @Test
    fun testGetConfig() {
        app.settings.setConfig(null)
        Assert.assertNull(app.settings.getConfig())
        api.getConfig()
        awaitSuccess()
    }

    @Test
    fun testTrack() {
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
                ) = Unit
            })
        awaitSuccess()
    }

    @Test
    fun testGetContext() {
        api.getContext(location, object : RadarApiClient.RadarContextApiCallback {
            override fun onComplete(status: Radar.RadarStatus, res: JSONObject?, context: RadarContext?) = Unit
        })
        awaitSuccess()
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
                override fun onComplete(status: Radar.RadarStatus, res: JSONObject?, places: Array<RadarPlace>?) = Unit
            })
        awaitSuccess()
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
                override fun onComplete(
                    status: Radar.RadarStatus,
                    res: JSONObject?,
                    geofences: Array<RadarGeofence>?) = Unit
            })
        awaitSuccess()
    }

    @Test
    fun testSearchBeacons() {
        api.searchBeacons(location, 1, 10, object : RadarApiClient.RadarSearchBeaconsApiCallback {
            override fun onComplete(status: Radar.RadarStatus, res: JSONObject?, beacons: Array<RadarBeacon>?) = Unit
        })
        awaitSuccess()
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
                override fun onComplete(
                    status: Radar.RadarStatus,
                    res: JSONObject?,
                    addresses: Array<RadarAddress>?) = Unit
            })
        awaitSuccess()
    }

    @Test
    fun testGeocode() {
        api.geocode("20 jay street brooklyn ny", object : RadarApiClient.RadarGeocodeApiCallback {
            override fun onComplete(status: Radar.RadarStatus, res: JSONObject?, addresses: Array<RadarAddress>?) = Unit
        })
        awaitSuccess()
    }

    @Test
    fun testReverseGeocode() {
        api.reverseGeocode(location, object : RadarApiClient.RadarGeocodeApiCallback {
            override fun onComplete(status: Radar.RadarStatus, res: JSONObject?, addresses: Array<RadarAddress>?) = Unit
        })
        awaitSuccess()
    }

    @Test
    fun testIpGeocode() {
        api.ipGeocode(object : RadarApiClient.RadarIpGeocodeApiCallback {
            override fun onComplete(
                status: Radar.RadarStatus,
                res: JSONObject?,
                address: RadarAddress?,
                proxy: Boolean
            ) = Unit
        })
        awaitSuccess()
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
                override fun onComplete(status: Radar.RadarStatus, res: JSONObject?, routes: RadarRoutes?) = Unit
            })
        awaitSuccess()
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
                override fun onComplete(status: Radar.RadarStatus, res: JSONObject?, matrix: RadarRouteMatrix?) = Unit
            }
        )
        awaitSuccess()
    }

    private fun awaitSuccess() {
        await.until { status == Radar.RadarStatus.SUCCESS }
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