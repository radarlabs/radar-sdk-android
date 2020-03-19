package io.radar.sdk

import android.content.Context
import android.location.Location
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.radar.sdk.model.*
import org.json.JSONObject
import org.junit.Test
import org.junit.Assert.*
import org.junit.Before
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
@Config(sdk=[Build.VERSION_CODES.P])
class RadarTest {

    companion object {
        const val publishableKey = "prj_test_pk_0000000000000000000000000000000000000000"
        private val context: Context = ApplicationProvider.getApplicationContext()
        private val apiHelperMock = RadarApiHelperMock()
        private val locationClientMock = FusedLocationProviderClientMock(context)
        private val permissionsHelperMock = RadarPermissionsHelperMock()
    }

    private fun assertGeofencesOk(geofences: Array<RadarGeofence>?) {
        assertNotNull(geofences)
        geofences?.let {
            for (geofence in geofences) {
                assertGeofenceOk(geofence)
            }
        }
    }

    private fun assertGeofenceOk(geofence: RadarGeofence?) {
        assertNotNull(geofence)
        assertNotNull(geofence?.description)
        assertNotNull(geofence?.tag)
        assertNotNull(geofence?.externalId)
        assertNotNull(geofence?.metadata)
        assertNotNull(geofence?.geometry)
    }

    private fun assertPointsOk(points: Array<RadarPoint>?) {
        assertNotNull(points)
        points?.let {
            for (point in points) {
                assertPointOk(point)
            }
        }
    }

    private fun assertPointOk(point: RadarPoint?) {
        assertNotNull(point)
        assertNotNull(point?.description)
        assertNotNull(point?.tag)
        assertNotNull(point?.externalId)
        assertNotNull(point?.metadata)
        assertNotNull(point?.location)
    }

    private fun assertChainsOk(chains: Array<RadarChain>?) {
        assertNotNull(chains)
        chains?.let {
            for (chain in chains) {
                assertChainOk(chain)
            }
        }
    }

    private fun assertChainOk(chain: RadarChain?) {
        assertNotNull(chain)
        assertNotNull(chain?.slug)
        assertNotNull(chain?.name)
        assertNotNull(chain?.externalId)
        assertNotNull(chain?.metadata)
    }

    private fun assertPlacesOk(places: Array<RadarPlace>?) {
        assertNotNull(places)
        places?.let {
            for (place in places) {
                assertPlaceOk(place)
            }
        }
    }

    private fun assertPlaceOk(place: RadarPlace?) {
        assertNotNull(place)
        assertNotNull(place?._id)
        assertNotNull(place?.categories)
        place?.categories?.let {
            assertTrue(place.categories.count() > 0)
        }
        place?.chain?.let {
            assertChainOk(place.chain)
        }
        assertNotNull(place?.location)
    }

    private fun assertInsightsOk(insights: RadarUserInsights?) {
        assertNotNull(insights)
        assertNotNull(insights?.homeLocation)
        assertNotNull(insights?.homeLocation?.updatedAt)
        assertNotEquals(insights?.homeLocation?.confidence, RadarUserInsightsLocation.RadarUserInsightsLocationConfidence.NONE)
        assertNotNull(insights?.officeLocation)
        assertNotNull(insights?.officeLocation?.updatedAt)
        assertNotEquals(insights?.officeLocation?.confidence, RadarUserInsightsLocation.RadarUserInsightsLocationConfidence.NONE)
        assertNotNull(insights?.state)
    }

    private fun assertRegionOk(region: RadarRegion?) {
        assertNotNull(region)
        assertNotNull(region?._id)
        assertNotNull(region?.name)
        assertNotNull(region?.code)
        assertNotNull(region?.type)
    }

    private fun assertSegmentsOk(segments: Array<RadarSegment>?) {
        assertNotNull(segments)
        segments?.let {
            for (segment in segments) {
                assertSegmentOk(segment)
            }
        }
    }

    private fun assertSegmentOk(segment: RadarSegment?) {
        assertNotNull(segment)
        assertNotNull(segment?.description)
        assertNotNull(segment?.externalId)
    }

    private fun assertUserOk(user: RadarUser?) {
        assertNotNull(user)
        assertNotNull(user?._id)
        assertNotNull(user?.userId)
        assertNotNull(user?.deviceId)
        assertNotNull(user?.description)
        assertNotNull(user?.metadata)
        assertNotNull(user?.location)
        assertGeofencesOk(user?.geofences)
        assertPlaceOk(user?.place)
        assertInsightsOk(user?.insights)
        assertRegionOk(user?.country)
        assertRegionOk(user?.state)
        assertRegionOk(user?.dma)
        assertRegionOk(user?.postalCode)
        assertChainsOk(user?.nearbyPlaceChains)
        assertSegmentsOk(user?.segments)
        assertChainsOk(user?.topChains)
    }

    private fun assertEventsOk(events: Array<RadarEvent>?) {
        assertNotNull(events)
        events?.let {
            for (event in events) {
                assertEventOk(event)
            }
        }
    }

    private fun assertEventOk(event: RadarEvent?) {
        assertNotNull(event)
        assertNotNull(event?._id)
        assertNotNull(event?.createdAt)
        assertNotNull(event?.actualCreatedAt)
        assertNotEquals(event?.type, RadarEvent.RadarEventType.UNKNOWN)
        assertNotEquals(event?.confidence, RadarEvent.RadarEventConfidence.NONE)
        assertNotNull(event?.location)
        if (event?.type == RadarEvent.RadarEventType.USER_ENTERED_GEOFENCE) {
            assertGeofenceOk(event.geofence)
        } else if (event?.type == RadarEvent.RadarEventType.USER_EXITED_GEOFENCE) {
            assertGeofenceOk(event.geofence)
            assertTrue(event.duration > 0)
        } else if (event?.type == RadarEvent.RadarEventType.USER_ENTERED_PLACE) {
            assertPlaceOk(event.place)
        } else if (event?.type == RadarEvent.RadarEventType.USER_EXITED_PLACE) {
            assertPlaceOk(event.place)
            assertTrue(event.duration > 0)
        } else if (event?.type == RadarEvent.RadarEventType.USER_NEARBY_PLACE_CHAIN) {
            assertPlaceOk(event.place)
        } else if (event?.type == RadarEvent.RadarEventType.USER_ENTERED_REGION_COUNTRY) {
            assertRegionOk(event.region)
        } else if (event?.type == RadarEvent.RadarEventType.USER_EXITED_REGION_COUNTRY) {
            assertRegionOk(event.region)
        } else if (event?.type == RadarEvent.RadarEventType.USER_ENTERED_REGION_STATE) {
            assertRegionOk(event.region)
        } else if (event?.type == RadarEvent.RadarEventType.USER_EXITED_REGION_STATE) {
            assertRegionOk(event.region)
        } else if (event?.type == RadarEvent.RadarEventType.USER_ENTERED_REGION_DMA) {
            assertRegionOk(event.region)
        } else if (event?.type == RadarEvent.RadarEventType.USER_EXITED_REGION_DMA) {
            assertRegionOk(event.region)
        }
    }

    private fun assertAddressesOk(addresses: Array<RadarAddress>?) {
        assertNotNull(addresses)
        addresses?.let {
            for (address in addresses) {
                assertAddressOk(address)
            }
        }
    }

    private fun assertAddressOk(address: RadarAddress?) {
        assertNotNull(address)
        assertNotEquals(address?.coordinate?.latitude, 0)
        assertNotEquals(address?.coordinate?.longitude, 0)
        assertNotNull(address?.formattedAddress)
        assertNotNull(address?.country)
        assertNotNull(address?.countryCode)
        assertNotNull(address?.countryFlag)
        assertNotNull(address?.state)
        assertNotNull(address?.stateCode)
        assertNotNull(address?.postalCode)
        assertNotNull(address?.city)
        assertNotNull(address?.borough)
        assertNotNull(address?.county)
        assertNotNull(address?.neighborhood)
        assertNotNull(address?.street)
        assertNotNull(address?.number)
        assertNotEquals(address?.confidence, RadarAddress.RadarAddressConfidence.NONE)
    }

    private fun assertContextOk(context: RadarContext?) {
        assertNotNull(context)
        assertGeofencesOk(context?.geofences)
        assertPlaceOk(context?.place)
        assertRegionOk(context?.country)
        assertRegionOk(context?.state)
        assertRegionOk(context?.dma)
        assertRegionOk(context?.postalCode)
    }

    private fun assertRoutesOk(routes: RadarRoutes?) {
        assertNotNull(routes)
        assertNotNull(routes?.geodesic)
        assertNotNull(routes?.geodesic?.distance?.text)
        assertNotEquals(routes?.geodesic?.distance?.value, 0)
        assertNotNull(routes?.foot)
        assertNotNull(routes?.foot?.distance?.text)
        assertNotEquals(routes?.foot?.distance?.value, 0)
        assertNotNull(routes?.foot?.duration?.text)
        assertNotEquals(routes?.foot?.duration?.value, 0)
        assertNotNull(routes?.bike)
        assertNotNull(routes?.bike?.distance?.text)
        assertNotEquals(routes?.bike?.distance?.value, 0)
        assertNotNull(routes?.bike?.duration?.text)
        assertNotEquals(routes?.bike?.duration?.value, 0)
        assertNotNull(routes?.car)
        assertNotNull(routes?.car?.distance?.text)
        assertNotEquals(routes?.car?.distance?.value, 0)
        assertNotNull(routes?.car?.duration?.text)
        assertNotEquals(routes?.car?.duration?.value, 0)
        assertNotNull(routes?.transit)
        assertNotNull(routes?.transit?.distance?.text)
        assertNotEquals(routes?.transit?.distance?.value, 0)
        assertNotNull(routes?.transit?.duration?.text)
        assertNotEquals(routes?.transit?.duration?.value, 0)
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

        Radar.getLocation { status, _, _ ->
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

        Radar.getLocation { status, _, _ ->
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

        Radar.getLocation { status, location, _ ->
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

        Radar.trackOnce { status, _, _, _ ->
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

        Radar.trackOnce { status, _, _, _ ->
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
        assertEventsOk(callbackEvents)
        assertUserOk(callbackUser)
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
        assertEventsOk(callbackEvents)
        assertUserOk(callbackUser)
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
    fun test_Radar_getContext_errorPermissions() {
        permissionsHelperMock.mockFineLocationPermissionGranted = false
        locationClientMock.mockLocation = null

        val latch = CountDownLatch(1)
        var callbackStatus: Radar.RadarStatus? = null

        Radar.getContext { status, _, _ ->
            callbackStatus = status
            latch.countDown()
        }

        latch.await(30, TimeUnit.SECONDS)

        assertEquals(Radar.RadarStatus.ERROR_PERMISSIONS, callbackStatus)
    }

    @Test
    fun test_Radar_getContext_errorLocation() {
        permissionsHelperMock.mockFineLocationPermissionGranted = true
        locationClientMock.mockLocation = null

        val latch = CountDownLatch(1)
        var callbackStatus: Radar.RadarStatus? = null

        Radar.getContext { status, _, _ ->
            callbackStatus = status
            latch.countDown()
        }

        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
        latch.await(30, TimeUnit.SECONDS)

        assertEquals(Radar.RadarStatus.ERROR_LOCATION, callbackStatus)
    }

    @Test
    fun test_Radar_getContext_success() {
        permissionsHelperMock.mockFineLocationPermissionGranted = true
        val mockLocation = Location("RadarSDK")
        mockLocation.latitude = 40.783826
        mockLocation.longitude = -73.975363
        mockLocation.accuracy = 65f
        mockLocation.time = System.currentTimeMillis()
        locationClientMock.mockLocation = mockLocation
        apiHelperMock.mockStatus = Radar.RadarStatus.SUCCESS
        apiHelperMock.mockResponse = RadarTestUtils.jsonObjectFromResource("/context.json")

        val latch = CountDownLatch(1)
        var callbackStatus: Radar.RadarStatus? = null
        var callbackLocation: Location? = null
        var callbackContext: RadarContext? = null

        Radar.getContext { status, location, context ->
            callbackStatus = status
            callbackLocation = location
            callbackContext = context
            latch.countDown()
        }

        latch.await(30, TimeUnit.SECONDS)

        assertEquals(Radar.RadarStatus.SUCCESS, callbackStatus)
        assertEquals(mockLocation, callbackLocation)
        assertContextOk(callbackContext)
    }

    @Test
    fun test_Radar_getContext_location_success() {
        permissionsHelperMock.mockFineLocationPermissionGranted = false
        val mockLocation = Location("RadarSDK")
        mockLocation.latitude = 40.783826
        mockLocation.longitude = -73.975363
        mockLocation.accuracy = 65f
        mockLocation.time = System.currentTimeMillis()
        apiHelperMock.mockStatus = Radar.RadarStatus.SUCCESS
        apiHelperMock.mockResponse = RadarTestUtils.jsonObjectFromResource("/context.json")

        val latch = CountDownLatch(1)
        var callbackStatus: Radar.RadarStatus? = null
        var callbackLocation: Location? = null
        var callbackContext: RadarContext? = null

        Radar.getContext(mockLocation) { status, location, context ->
            callbackStatus = status
            callbackLocation = location
            callbackContext = context
            latch.countDown()
        }

        latch.await(30, TimeUnit.SECONDS)

        assertEquals(Radar.RadarStatus.SUCCESS, callbackStatus)
        assertEquals(mockLocation, callbackLocation)
        assertContextOk(callbackContext)
    }

    @Test
    fun test_Radar_searchPlaces_errorPermissions() {
        permissionsHelperMock.mockFineLocationPermissionGranted = false
        locationClientMock.mockLocation = null

        val latch = CountDownLatch(1)
        var callbackStatus: Radar.RadarStatus? = null

        Radar.searchPlaces(1000, arrayOf("walmart"), null, null, 100) { status, _, _ ->
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

        Radar.searchPlaces(1000, arrayOf("walmart"), null, null, 100) { status, _, _ ->
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
        assertPlacesOk(callbackPlaces)
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
        assertPlacesOk(callbackPlaces)
    }

    @Test
    fun test_Radar_searchGeofences_errorPermissions() {
        permissionsHelperMock.mockFineLocationPermissionGranted = false
        locationClientMock.mockLocation = null

        val latch = CountDownLatch(1)
        var callbackStatus: Radar.RadarStatus? = null

        Radar.searchGeofences(1000, arrayOf("store"), 100) { status, _, _ ->
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

        Radar.searchGeofences(1000, arrayOf("store"), 100) { status, _, _ ->
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
        assertGeofencesOk(callbackGeofences)
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
        assertGeofencesOk(callbackGeofences)
    }

    @Test
    fun test_Radar_searchPoints_errorPermissions() {
        permissionsHelperMock.mockFineLocationPermissionGranted = false
        locationClientMock.mockLocation = null

        val latch = CountDownLatch(1)
        var callbackStatus: Radar.RadarStatus? = null

        Radar.searchPoints(1000, arrayOf("store"), 100) { status, _, _ ->
            callbackStatus = status
            latch.countDown()
        }

        latch.await(30, TimeUnit.SECONDS)

        assertEquals(Radar.RadarStatus.ERROR_PERMISSIONS, callbackStatus)
    }

    @Test
    fun test_Radar_searchPoints_errorLocation() {
        permissionsHelperMock.mockFineLocationPermissionGranted = true
        locationClientMock.mockLocation = null

        val latch = CountDownLatch(1)
        var callbackStatus: Radar.RadarStatus? = null

        Radar.searchPoints(1000, arrayOf("store"), 100) { status, _, _ ->
            callbackStatus = status
            latch.countDown()
        }

        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
        latch.await(30, TimeUnit.SECONDS)

        assertEquals(Radar.RadarStatus.ERROR_LOCATION, callbackStatus)
    }

    @Test
    fun test_Radar_searchPoints_success() {
        permissionsHelperMock.mockFineLocationPermissionGranted = true
        val mockLocation = Location("RadarSDK")
        mockLocation.latitude = 40.7039799
        mockLocation.longitude = -73.9873499
        mockLocation.accuracy = 65f
        mockLocation.time = System.currentTimeMillis()
        locationClientMock.mockLocation = mockLocation
        apiHelperMock.mockStatus = Radar.RadarStatus.SUCCESS
        apiHelperMock.mockResponse = RadarTestUtils.jsonObjectFromResource("/search_points.json")

        val latch = CountDownLatch(1)
        var callbackStatus: Radar.RadarStatus? = null
        var callbackLocation: Location? = null
        var callbackPoints: Array<RadarPoint>? = null

        Radar.searchPoints(1000, arrayOf("store"), 100) { status, location, points ->
            callbackStatus = status
            callbackLocation = location
            callbackPoints = points
            latch.countDown()
        }

        latch.await(30, TimeUnit.SECONDS)

        assertEquals(Radar.RadarStatus.SUCCESS, callbackStatus)
        assertEquals(mockLocation, callbackLocation)
        assertPointsOk(callbackPoints)
    }

    @Test
    fun test_Radar_searchPoints_location_success() {
        permissionsHelperMock.mockFineLocationPermissionGranted = false
        val mockLocation = Location("RadarSDK")
        mockLocation.latitude = 40.7039799
        mockLocation.longitude = -73.9873499
        mockLocation.accuracy = 65f
        mockLocation.time = System.currentTimeMillis()
        apiHelperMock.mockStatus = Radar.RadarStatus.SUCCESS
        apiHelperMock.mockResponse = RadarTestUtils.jsonObjectFromResource("/search_points.json")

        val latch = CountDownLatch(1)
        var callbackStatus: Radar.RadarStatus? = null
        var callbackLocation: Location? = null
        var callbackPoints: Array<RadarPoint>? = null

        Radar.searchPoints(mockLocation, 1000, arrayOf("store"), 100) { status, location, points ->
            callbackStatus = status
            callbackLocation = location
            callbackPoints = points
            latch.countDown()
        }

        latch.await(30, TimeUnit.SECONDS)

        assertEquals(Radar.RadarStatus.SUCCESS, callbackStatus)
        assertEquals(mockLocation, callbackLocation)
        assertPointsOk(callbackPoints)
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
        assertAddressesOk(callbackAddresses)
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
        permissionsHelperMock.mockFineLocationPermissionGranted = false
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
        assertAddressesOk(callbackAddresses)
    }

    @Test
    fun test_Radar_reverseGeocode_errorPermissions() {
        permissionsHelperMock.mockFineLocationPermissionGranted = false
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
        assertAddressesOk(callbackAddresses)
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
        assertAddressesOk(callbackAddresses)
    }

    @Test
    fun test_Radar_ipGeocode_error() {
        permissionsHelperMock.mockFineLocationPermissionGranted = false
        apiHelperMock.mockStatus = Radar.RadarStatus.ERROR_SERVER

        val latch = CountDownLatch(1)

        var callbackStatus: Radar.RadarStatus? = null
        var callbackAddress: RadarAddress? = null

        Radar.ipGeocode { status, address ->
            callbackStatus = status
            callbackAddress = address
            latch.countDown()
        }

        latch.await(30, TimeUnit.SECONDS)

        assertEquals(Radar.RadarStatus.ERROR_SERVER, callbackStatus)
        assertNull(callbackAddress)
    }

    @Test
    fun test_Radar_ipGeocode_success() {
        permissionsHelperMock.mockFineLocationPermissionGranted = false
        apiHelperMock.mockStatus = Radar.RadarStatus.SUCCESS
        apiHelperMock.mockResponse = RadarTestUtils.jsonObjectFromResource("/geocode_ip.json")

        val latch = CountDownLatch(1)

        var callbackStatus: Radar.RadarStatus? = null
        var callbackAddress: RadarAddress? = null

        Radar.ipGeocode { status, address ->
            callbackStatus = status
            callbackAddress = address
            latch.countDown()
        }

        latch.await(30, TimeUnit.SECONDS)

        assertEquals(Radar.RadarStatus.SUCCESS, callbackStatus)
        assertAddressOk(callbackAddress)
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
        assertRoutesOk(callbackRoutes)
    }

}
