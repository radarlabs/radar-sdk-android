package io.radar.sdk

import android.content.Context
import android.location.Location
import android.os.Build
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.tasks.Task
import io.radar.sdk.model.*
import org.json.JSONObject
import org.junit.Test
import org.junit.Assert.*
import org.junit.Before
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper
import java.net.URL
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
@Config(sdk=[Build.VERSION_CODES.P])
class RadarTest {

    internal class RadarApiHelperMock : RadarApiHelper() {

        internal var mockStatus: Radar.RadarStatus = Radar.RadarStatus.ERROR_UNKNOWN
        internal var mockResponse: JSONObject? = null

        override fun request(context: Context,
                                 method: String,
                                 url: URL,
                                 headers: Map<String, String>?,
                                 params: JSONObject?,
                                 callback: RadarApiCallback?) {
            callback?.onComplete(mockStatus, mockResponse)
        }

    }

    internal class RadarPermissionsHelperMock : RadarPermissionsHelper() {

        internal var mockFineLocationPermissionGranted: Boolean = false

        override fun fineLocationPermissionGranted(context: Context): Boolean {
            return mockFineLocationPermissionGranted
        }

    }

    internal class FusedLocationProviderClientMock(
        context: Context
    ) : FusedLocationProviderClient(context) {

        internal var mockLocation: Location? = null

        override fun requestLocationUpdates(
            request: LocationRequest?,
            callback: LocationCallback?,
            looper: Looper?
        ): Task<Void> {
            if (mockLocation != null) {
                callback?.onLocationResult(LocationResult.create(listOf(mockLocation)))
            }
            return super.requestLocationUpdates(request, callback, looper)
        }

    }

    internal class RadarTestUtils {

        internal companion object {

            fun jsonObjectFromResource(resource: String): JSONObject? {
                val str = RadarTest::class.java.getResource(resource)!!.readText()
                return JSONObject(str)
            }

        }

    }

    companion object {
        const val publishableKey = "prj_test_pk_0000000000000000000000000000000000000000"
        private val context: Context = ApplicationProvider.getApplicationContext()
        private val apiHelperMock = RadarApiHelperMock()
        private val locationClientMock = FusedLocationProviderClientMock(context)
        private val permissionsHelperMock = RadarPermissionsHelperMock()
    }

    @Before
    fun setUp() {
        Radar.initialize(context, publishableKey)

        Radar.apiClient.apiHelper = apiHelperMock
        Radar.locationManager.locationClient = locationClientMock
        Radar.locationManager.permissionsHelper = permissionsHelperMock
    }

    @Test
    fun test_Radar_initialize() {
        assertEquals(publishableKey, RadarSettings.getPublishableKey(context))
    }

    @Test
    fun test_Radar_setUserId() {
        val userId = "userId"
        Radar.setUserId(userId)
        assertEquals(userId, Radar.getUserId())
    }

    @Test
    fun test_Radar_setUserId_null() {
        Radar.setUserId(null)
        assertNull(Radar.getUserId())
    }

    @Test
    fun test_Radar_setDescription() {
        val description = "description"
        Radar.setDescription(description)
        assertEquals(description, Radar.getDescription())
    }

    @Test
    fun test_Radar_setDescription_null() {
        Radar.setDescription(null)
        assertNull(Radar.getDescription())
    }

    @Test
    fun test_Radar_setMetadata() {
        val metadata = JSONObject(mapOf("foo" to "bar", "baz" to true, "qux" to 1))
        Radar.setMetadata(metadata)
        assertEquals(metadata.toString(), Radar.getMetadata()?.toString())
    }

    @Test
    fun test_Radar_setMetadata_null() {
        Radar.setMetadata(null)
        assertNull(Radar.getMetadata())
    }

    @Test
    fun test_Radar_getLocation_errorPermissions() {
        permissionsHelperMock.mockFineLocationPermissionGranted = false
        locationClientMock.mockLocation = null

        val latch = CountDownLatch(1)
        var callbackStatus: Radar.RadarStatus? = null

        Radar.getLocation { status, location, stopped ->
            callbackStatus = status
            latch.countDown()
        }

        latch.await(30, TimeUnit.SECONDS)

        assertEquals(Radar.RadarStatus.ERROR_PERMISSIONS, callbackStatus)
    }

    @Test
    fun test_Radar_getLocation_errorLocation() {
        permissionsHelperMock.mockFineLocationPermissionGranted = true
        locationClientMock.mockLocation = null

        val latch = CountDownLatch(1)
        var callbackStatus: Radar.RadarStatus? = null

        Radar.getLocation { status, location, stopped ->
            callbackStatus = status
            latch.countDown()
        }

        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
        latch.await(30, TimeUnit.SECONDS)

        assertEquals(Radar.RadarStatus.ERROR_LOCATION, callbackStatus)
    }

    @Test
    fun test_Radar_getLocation_success() {
        permissionsHelperMock.mockFineLocationPermissionGranted = true
        val mockLocation = Location("RadarSDK")
        mockLocation.latitude = 40.783826
        mockLocation.longitude = -73.975363
        mockLocation.accuracy = 65f
        mockLocation.time = System.currentTimeMillis()
        locationClientMock.mockLocation = mockLocation

        val latch = CountDownLatch(1)
        var callbackStatus: Radar.RadarStatus? = null
        var callbackLocation: Location? = null

        Radar.getLocation { status, location, stopped ->
            callbackStatus = status
            callbackLocation = location
            latch.countDown()
        }

        latch.await(30, TimeUnit.SECONDS)

        assertEquals(Radar.RadarStatus.SUCCESS, callbackStatus)
        assertEquals(mockLocation, callbackLocation)
    }

    @Test
    fun test_Radar_trackOnce_errorPermissions() {
        permissionsHelperMock.mockFineLocationPermissionGranted = false
        locationClientMock.mockLocation = null

        val latch = CountDownLatch(1)
        var callbackStatus: Radar.RadarStatus? = null

        Radar.trackOnce { status, location, events, user ->
            callbackStatus = status
            latch.countDown()
        }

        latch.await(30, TimeUnit.SECONDS)

        assertEquals(Radar.RadarStatus.ERROR_PERMISSIONS, callbackStatus)
    }

    @Test
    fun test_Radar_trackOnce_errorLocation() {
        permissionsHelperMock.mockFineLocationPermissionGranted = true
        locationClientMock.mockLocation = null

        val latch = CountDownLatch(1)
        var callbackStatus: Radar.RadarStatus? = null

        Radar.trackOnce { status, location, events, user ->
            callbackStatus = status
            latch.countDown()
        }

        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
        latch.await(30, TimeUnit.SECONDS)

        assertEquals(Radar.RadarStatus.ERROR_LOCATION, callbackStatus)
    }

    @Test
    fun test_Radar_trackOnce_success() {
        permissionsHelperMock.mockFineLocationPermissionGranted = true
        val mockLocation = Location("RadarSDK")
        mockLocation.latitude = 40.783826
        mockLocation.longitude = -73.975363
        mockLocation.accuracy = 65f
        mockLocation.time = System.currentTimeMillis()
        locationClientMock.mockLocation = mockLocation
        apiHelperMock.mockStatus = Radar.RadarStatus.SUCCESS
        apiHelperMock.mockResponse = RadarTestUtils.jsonObjectFromResource("/track.json")

        val latch = CountDownLatch(1)
        var callbackStatus: Radar.RadarStatus? = null
        var callbackLocation: Location? = null
        var callbackEvents: Array<RadarEvent>? = null
        var callbackUser: RadarUser? = null

        Radar.trackOnce { status, location, events, user ->
            callbackStatus = status
            callbackLocation = location
            callbackEvents = events
            callbackUser = user
            latch.countDown()
        }

        latch.await(30, TimeUnit.SECONDS)

        assertEquals(Radar.RadarStatus.SUCCESS, callbackStatus)
        assertEquals(mockLocation, callbackLocation)
        assertNotNull(callbackEvents)
        assertNotNull(callbackUser)
    }

    @Test
    fun test_Radar_trackOnce_location_success() {
        permissionsHelperMock.mockFineLocationPermissionGranted = false
        val mockLocation = Location("RadarSDK")
        mockLocation.latitude = 40.783826
        mockLocation.longitude = -73.975363
        mockLocation.accuracy = 65f
        mockLocation.time = System.currentTimeMillis()
        apiHelperMock.mockStatus = Radar.RadarStatus.SUCCESS
        apiHelperMock.mockResponse = RadarTestUtils.jsonObjectFromResource("/track.json")

        val latch = CountDownLatch(1)
        var callbackStatus: Radar.RadarStatus? = null
        var callbackLocation: Location? = null
        var callbackEvents: Array<RadarEvent>? = null
        var callbackUser: RadarUser? = null

        Radar.trackOnce(mockLocation) { status, location, events, user ->
            callbackStatus = status
            callbackLocation = location
            callbackEvents = events
            callbackUser = user
            latch.countDown()
        }

        latch.await(30, TimeUnit.SECONDS)

        assertEquals(Radar.RadarStatus.SUCCESS, callbackStatus)
        assertEquals(mockLocation, callbackLocation)
        assertNotNull(callbackEvents)
        assertNotNull(callbackUser)
    }

    @Test
    fun test_Radar_startTracking_default() {
        permissionsHelperMock.mockFineLocationPermissionGranted = true

        Radar.startTracking()
        assertEquals(RadarTrackingOptions.EFFICIENT, Radar.getTrackingOptions())
        assertTrue(Radar.isTracking())
    }

    @Test
    fun test_Radar_startTracking_continuous() {
        permissionsHelperMock.mockFineLocationPermissionGranted = true

        Radar.stopTracking()

        val options = RadarTrackingOptions.CONTINUOUS
        Radar.startTracking(options)
        assertEquals(options, Radar.getTrackingOptions())
        assertTrue(Radar.isTracking())
    }

    @Test
    fun test_Radar_startTracking_responsive() {
        permissionsHelperMock.mockFineLocationPermissionGranted = true

        Radar.stopTracking()

        val options = RadarTrackingOptions.RESPONSIVE
        Radar.startTracking(options)
        assertEquals(options, Radar.getTrackingOptions())
        assertTrue(Radar.isTracking())
    }

    @Test
    fun test_Radar_startTracking_efficient() {
        permissionsHelperMock.mockFineLocationPermissionGranted = true

        Radar.stopTracking()

        val options = RadarTrackingOptions.EFFICIENT
        Radar.startTracking(options)
        assertEquals(options, Radar.getTrackingOptions())
        assertTrue(Radar.isTracking())
    }

    @Test
    fun test_Radar_startTracking_custom() {
        permissionsHelperMock.mockFineLocationPermissionGranted = true

        Radar.stopTracking()

        val options = RadarTrackingOptions.EFFICIENT
        options.desiredAccuracy = RadarTrackingOptions.RadarTrackingOptionsDesiredAccuracy.LOW
        val now = Date()
        options.startTrackingAfter = now
        options.stopTrackingAfter = Date(now.time + 1000)
        options.sync = RadarTrackingOptions.RadarTrackingOptionsSync.NONE
        Radar.startTracking(options)
        assertEquals(options, Radar.getTrackingOptions())
        assertTrue(Radar.isTracking())
    }

    @Test
    fun test_Radar_stopTracking() {
        Radar.stopTracking()
        assertFalse(Radar.isTracking())
    }

    @Test
    fun test_Radar_acceptEventId() {
        apiHelperMock.mockStatus = Radar.RadarStatus.SUCCESS
        apiHelperMock.mockResponse = RadarTestUtils.jsonObjectFromResource("/events_verification.json")
        Radar.acceptEvent("eventId")
    }

    @Test
    fun test_Radar_acceptEventId_verifiedPlaceId() {
        apiHelperMock.mockStatus = Radar.RadarStatus.SUCCESS
        apiHelperMock.mockResponse = RadarTestUtils.jsonObjectFromResource("/events_verification.json")
        Radar.acceptEvent("eventId", "verifiedPlaceId")
    }

    @Test
    fun test_Radar_rejectEventId() {
        apiHelperMock.mockStatus = Radar.RadarStatus.SUCCESS
        apiHelperMock.mockResponse = RadarTestUtils.jsonObjectFromResource("/events_verification.json")
        Radar.rejectEvent("eventId")
    }

    @Test
    fun test_Radar_searchPlaces_errorPermissions() {
        permissionsHelperMock.mockFineLocationPermissionGranted = false
        locationClientMock.mockLocation = null

        val latch = CountDownLatch(1)
        var callbackStatus: Radar.RadarStatus? = null

        Radar.searchPlaces(1000, arrayOf("walmart"), null, null, 100) { status, location, places ->
            callbackStatus = status
            latch.countDown()
        }

        latch.await(30, TimeUnit.SECONDS)

        assertEquals(Radar.RadarStatus.ERROR_PERMISSIONS, callbackStatus)
    }

    @Test
    fun test_Radar_searchPlaces_errorLocation() {
        permissionsHelperMock.mockFineLocationPermissionGranted = true
        locationClientMock.mockLocation = null

        val latch = CountDownLatch(1)
        var callbackStatus: Radar.RadarStatus? = null

        Radar.searchPlaces(1000, arrayOf("walmart"), null, null, 100) { status, location, places ->
            callbackStatus = status
            latch.countDown()
        }

        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
        latch.await(30, TimeUnit.SECONDS)

        assertEquals(Radar.RadarStatus.ERROR_LOCATION, callbackStatus)
    }

    @Test
    fun test_Radar_searchPlaces_success() {
        permissionsHelperMock.mockFineLocationPermissionGranted = true
        val mockLocation = Location("RadarSDK")
        mockLocation.latitude = 40.783826
        mockLocation.longitude = -73.975363
        mockLocation.accuracy = 65f
        mockLocation.time = System.currentTimeMillis()
        locationClientMock.mockLocation = mockLocation
        apiHelperMock.mockStatus = Radar.RadarStatus.SUCCESS
        apiHelperMock.mockResponse = RadarTestUtils.jsonObjectFromResource("/search_places.json")

        val latch = CountDownLatch(1)
        var callbackStatus: Radar.RadarStatus? = null
        var callbackLocation: Location? = null
        var callbackPlaces: Array<RadarPlace>? = null

        Radar.searchPlaces(1000, arrayOf("walmart"), null, null, 100) { status, location, places ->
            callbackStatus = status
            callbackLocation = location
            callbackPlaces = places
            latch.countDown()
        }

        latch.await(30, TimeUnit.SECONDS)

        assertEquals(Radar.RadarStatus.SUCCESS, callbackStatus)
        assertEquals(mockLocation, callbackLocation)
        assertNotNull(callbackPlaces)
    }

    @Test
    fun test_Radar_searchPlaces_location_success() {
        permissionsHelperMock.mockFineLocationPermissionGranted = false
        val mockLocation = Location("RadarSDK")
        mockLocation.latitude = 40.783826
        mockLocation.longitude = -73.975363
        mockLocation.accuracy = 65f
        mockLocation.time = System.currentTimeMillis()
        apiHelperMock.mockStatus = Radar.RadarStatus.SUCCESS
        apiHelperMock.mockResponse = RadarTestUtils.jsonObjectFromResource("/search_places.json")

        val latch = CountDownLatch(1)
        var callbackStatus: Radar.RadarStatus? = null
        var callbackLocation: Location? = null
        var callbackPlaces: Array<RadarPlace>? = null

        Radar.searchPlaces(mockLocation, 1000, arrayOf("walmart"), null, null, 100) { status, location, places ->
            callbackStatus = status
            callbackLocation = location
            callbackPlaces = places
            latch.countDown()
        }

        latch.await(30, TimeUnit.SECONDS)

        assertEquals(Radar.RadarStatus.SUCCESS, callbackStatus)
        assertEquals(mockLocation, callbackLocation)
        assertNotNull(callbackPlaces)
    }

    @Test
    fun test_Radar_searchGeofences_errorPermissions() {
        permissionsHelperMock.mockFineLocationPermissionGranted = false
        locationClientMock.mockLocation = null

        val latch = CountDownLatch(1)
        var callbackStatus: Radar.RadarStatus? = null

        Radar.searchGeofences(1000, arrayOf("store"), 100) { status, location, geofences ->
            callbackStatus = status
            latch.countDown()
        }

        latch.await(30, TimeUnit.SECONDS)

        assertEquals(Radar.RadarStatus.ERROR_PERMISSIONS, callbackStatus)
    }

    @Test
    fun test_Radar_searchGeofences_errorLocation() {
        permissionsHelperMock.mockFineLocationPermissionGranted = true
        locationClientMock.mockLocation = null

        val latch = CountDownLatch(1)
        var callbackStatus: Radar.RadarStatus? = null

        Radar.searchGeofences(1000, arrayOf("store"), 100) { status, location, geofences ->
            callbackStatus = status
            latch.countDown()
        }

        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
        latch.await(30, TimeUnit.SECONDS)

        assertEquals(Radar.RadarStatus.ERROR_LOCATION, callbackStatus)
    }

    @Test
    fun test_Radar_searchGeofences_success() {
        permissionsHelperMock.mockFineLocationPermissionGranted = true
        val mockLocation = Location("RadarSDK")
        mockLocation.latitude = 40.783826
        mockLocation.longitude = -73.975363
        mockLocation.accuracy = 65f
        mockLocation.time = System.currentTimeMillis()
        locationClientMock.mockLocation = mockLocation
        apiHelperMock.mockStatus = Radar.RadarStatus.SUCCESS
        apiHelperMock.mockResponse = RadarTestUtils.jsonObjectFromResource("/search_geofences.json")

        val latch = CountDownLatch(1)
        var callbackStatus: Radar.RadarStatus? = null
        var callbackLocation: Location? = null
        var callbackGeofences: Array<RadarGeofence>? = null

        Radar.searchGeofences(1000, arrayOf("store"), 100) { status, location, geofences ->
            callbackStatus = status
            callbackLocation = location
            callbackGeofences = geofences
            latch.countDown()
        }

        latch.await(30, TimeUnit.SECONDS)

        assertEquals(Radar.RadarStatus.SUCCESS, callbackStatus)
        assertEquals(mockLocation, callbackLocation)
        assertNotNull(callbackGeofences)
    }

    @Test
    fun test_Radar_searchGeofences_location_success() {
        permissionsHelperMock.mockFineLocationPermissionGranted = false
        val mockLocation = Location("RadarSDK")
        mockLocation.latitude = 40.783826
        mockLocation.longitude = -73.975363
        mockLocation.accuracy = 65f
        mockLocation.time = System.currentTimeMillis()
        apiHelperMock.mockStatus = Radar.RadarStatus.SUCCESS
        apiHelperMock.mockResponse = RadarTestUtils.jsonObjectFromResource("/search_geofences.json")

        val latch = CountDownLatch(1)
        var callbackStatus: Radar.RadarStatus? = null
        var callbackLocation: Location? = null
        var callbackGeofences: Array<RadarGeofence>? = null

        Radar.searchGeofences(mockLocation, 1000, arrayOf("store"), 100) { status, location, geofences ->
            callbackStatus = status
            callbackLocation = location
            callbackGeofences = geofences
            latch.countDown()
        }

        latch.await(30, TimeUnit.SECONDS)

        assertEquals(Radar.RadarStatus.SUCCESS, callbackStatus)
        assertEquals(mockLocation, callbackLocation)
        assertNotNull(callbackGeofences)
    }

    @Test
    fun test_Radar_autocomplete_success() {
        permissionsHelperMock.mockFineLocationPermissionGranted = false
        apiHelperMock.mockStatus = Radar.RadarStatus.SUCCESS
        apiHelperMock.mockResponse = RadarTestUtils.jsonObjectFromResource("/search_autocomplete.json")

        val near = Location("RadarSDK")
        near.latitude = 40.783826
        near.longitude = -73.975363

        val latch = CountDownLatch(1)
        var callbackStatus: Radar.RadarStatus? = null
        var callbackAddresses: Array<RadarAddress>? = null

        Radar.autocomplete("brooklyn roasting", near, 10) { status, addresses ->
            callbackStatus = status
            callbackAddresses = addresses
            latch.countDown()
        }

        latch.await(30, TimeUnit.SECONDS)

        assertEquals(Radar.RadarStatus.SUCCESS, callbackStatus)
        assertNotNull(callbackAddresses)
    }

    @Test
    fun test_Radar_geocode_error() {
        permissionsHelperMock.mockFineLocationPermissionGranted = false
        apiHelperMock.mockStatus = Radar.RadarStatus.ERROR_SERVER

        val query = "20 jay street brooklyn"

        val latch = CountDownLatch(1)
        var callbackStatus: Radar.RadarStatus? = null
        var callbackAddresses: Array<RadarAddress>? = null

        Radar.geocode(query) { status, addresses ->
            callbackStatus = status
            callbackAddresses = addresses
            latch.countDown()
        }

        latch.await(30, TimeUnit.SECONDS)

        assertEquals(Radar.RadarStatus.ERROR_SERVER, callbackStatus)
        assertNull(callbackAddresses)
    }

    @Test
    fun test_Radar_geocode_success() {
        permissionsHelperMock.mockFineLocationPermissionGranted = false;
        apiHelperMock.mockStatus = Radar.RadarStatus.SUCCESS
        apiHelperMock.mockResponse = RadarTestUtils.jsonObjectFromResource("/geocode.json")

        val query = "20 jay street brooklyn"

        val latch = CountDownLatch(1)
        var callbackStatus: Radar.RadarStatus? = null
        var callbackAddresses: Array<RadarAddress>? = null

        Radar.geocode(query) { status, addresses ->
            callbackStatus = status
            callbackAddresses = addresses
            latch.countDown()
        }

        latch.await(30, TimeUnit.SECONDS)

        assertEquals(Radar.RadarStatus.SUCCESS, callbackStatus)
        assertNotNull(callbackAddresses)
        assertNotNull(callbackAddresses?.get(0)?.coordinate)
    }

    @Test
    fun test_Radar_reverseGeocode_errorPermissions() {
        permissionsHelperMock.mockFineLocationPermissionGranted = false;
        locationClientMock.mockLocation = null

        val latch = CountDownLatch(1)

        var callbackStatus: Radar.RadarStatus? = null
        var callbackAddresses: Array<RadarAddress>? = null

        Radar.reverseGeocode { status, addresses ->
            callbackStatus = status
            callbackAddresses = addresses
            latch.countDown()
        }

        latch.await(30, TimeUnit.SECONDS)

        assertEquals(Radar.RadarStatus.ERROR_PERMISSIONS, callbackStatus)
        assertNull(callbackAddresses)
    }

    @Test
    fun test_Radar_reverseGeocode_errorLocation() {
        permissionsHelperMock.mockFineLocationPermissionGranted = true
        locationClientMock.mockLocation = null

        val latch = CountDownLatch(1)

        var callbackStatus: Radar.RadarStatus? = null
        var callbackAddresses: Array<RadarAddress>? = null

        Radar.reverseGeocode { status, addresses ->
            callbackStatus = status
            callbackAddresses = addresses
            latch.countDown()
        }

        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
        latch.await(30, TimeUnit.SECONDS)

        assertEquals(Radar.RadarStatus.ERROR_LOCATION, callbackStatus)
        assertNull(callbackAddresses)
    }

    @Test
    fun test_Radar_reverseGeocode_success() {
        permissionsHelperMock.mockFineLocationPermissionGranted = true
        val mockLocation = Location("RadarSDK")
        mockLocation.latitude = 40.783826
        mockLocation.longitude = -73.975363
        mockLocation.accuracy = 65f
        mockLocation.time = System.currentTimeMillis()
        locationClientMock.mockLocation = mockLocation
        apiHelperMock.mockStatus = Radar.RadarStatus.SUCCESS
        apiHelperMock.mockResponse = RadarTestUtils.jsonObjectFromResource("/geocode.json")

        val latch = CountDownLatch(1)
        var callbackStatus: Radar.RadarStatus? = null
        var callbackAddresses: Array<RadarAddress>? = null

        Radar.reverseGeocode { status, addresses ->
            callbackStatus = status
            callbackAddresses = addresses
            latch.countDown()
        }

        latch.await(30, TimeUnit.SECONDS)

        assertEquals(Radar.RadarStatus.SUCCESS, callbackStatus)
        assertNotNull(callbackAddresses)
        assertNotNull(callbackAddresses?.get(0)?.coordinate)
    }

    @Test
    fun test_Radar_reverseGeocode_location_error() {
        permissionsHelperMock.mockFineLocationPermissionGranted = false
        apiHelperMock.mockStatus = Radar.RadarStatus.ERROR_SERVER

        val mockLocation = Location("RadarSDK")
        mockLocation.latitude = 40.783826
        mockLocation.longitude = -73.975363

        val latch = CountDownLatch(1)

        var callbackStatus: Radar.RadarStatus? = null
        var callbackAddresses: Array<RadarAddress>? = null

        Radar.reverseGeocode(mockLocation) { status, addresses ->
            callbackStatus = status
            callbackAddresses = addresses
            latch.countDown()
        }

        latch.await(30, TimeUnit.SECONDS)

        assertEquals(Radar.RadarStatus.ERROR_SERVER, callbackStatus)
        assertNull(callbackAddresses)
    }

    @Test
    fun test_Radar_reverseGeocode_location_success() {
        permissionsHelperMock.mockFineLocationPermissionGranted = false
        apiHelperMock.mockStatus = Radar.RadarStatus.SUCCESS
        apiHelperMock.mockResponse = RadarTestUtils.jsonObjectFromResource("/geocode.json")

        val mockLocation = Location("RadarSDK")
        mockLocation.latitude = 40.783826
        mockLocation.longitude = -73.975363

        val latch = CountDownLatch(1)

        var callbackStatus: Radar.RadarStatus? = null
        var callbackAddresses: Array<RadarAddress>? = null

        Radar.reverseGeocode(mockLocation) { status, addresses ->
            callbackStatus = status
            callbackAddresses = addresses
            latch.countDown()
        }

        latch.await(30, TimeUnit.SECONDS)

        assertEquals(Radar.RadarStatus.SUCCESS, callbackStatus)
        assertNotNull(callbackAddresses)
        assertNotNull(callbackAddresses?.get(0)?.coordinate)
    }

    @Test
    fun test_Radar_ipGeocode_error() {
        permissionsHelperMock.mockFineLocationPermissionGranted = false
        apiHelperMock.mockStatus = Radar.RadarStatus.ERROR_SERVER

        val latch = CountDownLatch(1)

        var callbackStatus: Radar.RadarStatus? = null
        var callbackCountry: RadarRegion? = null

        Radar.ipGeocode() { status, country ->
            callbackStatus = status
            callbackCountry = country
            latch.countDown()
        }

        latch.await(30, TimeUnit.SECONDS)

        assertEquals(Radar.RadarStatus.ERROR_SERVER, callbackStatus)
        assertNull(callbackCountry)
    }

    @Test
    fun test_Radar_ipGeocode_success() {
        permissionsHelperMock.mockFineLocationPermissionGranted = false
        apiHelperMock.mockStatus = Radar.RadarStatus.SUCCESS
        apiHelperMock.mockResponse = RadarTestUtils.jsonObjectFromResource("/geocode_ip.json")

        val latch = CountDownLatch(1)

        var callbackStatus: Radar.RadarStatus? = null
        var callbackCountry: RadarRegion? = null

        Radar.ipGeocode() { status, country ->
            callbackStatus = status
            callbackCountry = country
            latch.countDown()
        }

        latch.await(30, TimeUnit.SECONDS)

        assertEquals(Radar.RadarStatus.SUCCESS, callbackStatus)
        assertNotNull(callbackCountry)
        assertNotNull(callbackCountry?.code)
    }

    @Test
    fun test_Radar_getDistance_success() {
        permissionsHelperMock.mockFineLocationPermissionGranted = true
        val mockLocation = Location("RadarSDK")
        mockLocation.latitude = 40.783826
        mockLocation.longitude = -73.975363
        mockLocation.accuracy = 65f
        mockLocation.time = System.currentTimeMillis()
        locationClientMock.mockLocation = mockLocation
        apiHelperMock.mockStatus = Radar.RadarStatus.SUCCESS
        apiHelperMock.mockResponse = RadarTestUtils.jsonObjectFromResource("/route_distance.json")

        val destination = Location("RadarSDK")
        destination.latitude = 40.783826
        destination.longitude = -73.975363

        val latch = CountDownLatch(1)
        var callbackStatus: Radar.RadarStatus? = null
        var callbackRoutes: RadarRoutes? = null

        Radar.getDistance(destination, EnumSet.of(Radar.RadarRouteMode.FOOT, Radar.RadarRouteMode.CAR), Radar.RadarRouteUnits.IMPERIAL) { status, routes ->
            callbackStatus = status
            callbackRoutes = routes
            latch.countDown()
        }

        latch.await(30, TimeUnit.SECONDS)

        assertEquals(Radar.RadarStatus.SUCCESS, callbackStatus)
        assertNotNull(callbackRoutes)
    }

}
