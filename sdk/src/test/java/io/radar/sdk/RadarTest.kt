package io.radar.sdk

import android.content.Context
import android.location.Location
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.radar.sdk.model.*
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import io.radar.sdk.RadarTrackingOptions.RadarTrackingOptionsSyncGeofences

@RunWith(AndroidJUnit4::class)
@Config(sdk=[Build.VERSION_CODES.P])
class RadarTest {

    companion object {
        const val LATCH_TIMEOUT = 5L

        const val publishableKey = "prj_test_pk_0000000000000000000000000000000000000000"

        private val context: Context = ApplicationProvider.getApplicationContext()
        private val apiHelperMock = RadarApiHelperMock()
        private val locationClientMock = RadarMockLocationProvider()
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

    private fun assertTripOk(trip: RadarTrip?) {
        assertNotNull(trip)
        assertNotNull(trip?.externalId)
        assertNotNull(trip?.metadata)
        assertNotNull(trip?.destinationGeofenceTag)
        assertNotNull(trip?.destinationGeofenceExternalId)
        assertNotNull(trip?.destinationLocation)
        assertNotNull(trip?.mode)
        assertNotNull(trip?.etaDistance)
        assertNotEquals(trip?.etaDistance, 0)
        assertNotNull(trip?.etaDuration)
        assertNotEquals(trip?.etaDuration, 0)
        assertEquals(trip?.status, RadarTrip.RadarTripStatus.STARTED)
    }

    private fun assertFraudOk(fraud: RadarFraud?) {
        assertNotNull(fraud)
        assertTrue(fraud!!.proxy)
        assertTrue(fraud.mocked)
        assertTrue(fraud.compromised)
        assertTrue(fraud.jumped)
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
        assertRegionOk(user?.country)
        assertRegionOk(user?.state)
        assertRegionOk(user?.dma)
        assertRegionOk(user?.postalCode)
        assertChainsOk(user?.nearbyPlaceChains)
        assertSegmentsOk(user?.segments)
        assertChainsOk(user?.topChains)
        assertNotEquals(user?.source, Radar.RadarLocationSource.UNKNOWN)
        assertTripOk(user?.trip)
        assertFraudOk(user?.fraud)
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
        assertRouteOk(routes?.foot)
        assertRouteOk(routes?.bike)
        assertRouteOk(routes?.car)
    }

    private fun assertRouteOk(route: RadarRoute?) {
        assertNotNull(route)
        assertNotNull(route?.distance?.text)
        assertNotEquals(route?.distance?.value, 0)
        assertNotNull(route?.duration?.text)
        assertNotEquals(route?.duration?.value, 0)
    }

    @Before
    fun setUp() {
        Radar.logger = RadarLogger(context)
        Radar.apiClient = RadarApiClient(context, Radar.logger)
        Radar.apiClient.apiHelper = apiHelperMock
        setUpLogConversionTest()

        Radar.initialize(context, publishableKey)

        Radar.locationManager.locationClient = locationClientMock
        Radar.locationManager.permissionsHelper = permissionsHelperMock

        // Clear any existing user tags to ensure clean state
        RadarSettings.removeUserTags(context, arrayOf("premium", "beta_user", "vip", "test_tag_1", "test_tag_2", "nonexistent_tag"))
        
        // Clear captured parameters from previous tests
        apiHelperMock.clearCapturedParams()
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
    fun test_Radar_setNotificationsOptions() {
        val notificationOptions = RadarNotificationOptions(
            "foo",
            "red",
            "bar",
            "blue",
            "hello",
            "white")
        Radar.setNotificationOptions(notificationOptions)
        assertEquals(notificationOptions, RadarSettings.getNotificationOptions(context))
    }


    @Test
    fun test_Radar_notificationSettingDefaults() {
        Radar.setForegroundServiceOptions(RadarTrackingOptions.RadarTrackingOptionsForegroundService(
            text = "Text",
            title = "Title",
            icon = 1337,
            updatesOnly = true,
        ))
        // Radar.setNotificationOptions has side effects on foregroundServiceOptions. 
        Radar.setNotificationOptions(RadarNotificationOptions(
            "foo",
            "red",
            "bar",
            "blue",
            "hello",
            "white"))
        assertEquals("bar", RadarSettings.getForegroundService(context).iconString)
        assertEquals("blue", RadarSettings.getForegroundService(context).iconColor)
        // We do not clear existing values of iconString and iconColor with null values.
        Radar.setForegroundServiceOptions(RadarTrackingOptions.RadarTrackingOptionsForegroundService(
            text = "Text",
            title = "Title",
            icon = 1337,
            updatesOnly = true,
        ))
        assertEquals("bar", RadarSettings.getForegroundService(context).iconString)
        assertEquals("blue", RadarSettings.getForegroundService(context).iconColor)
        Radar.setForegroundServiceOptions(RadarTrackingOptions.RadarTrackingOptionsForegroundService(
            text = "Text",
            title = "Title",
            iconString = "test",
            iconColor = "red",
            icon = 1337,
            updatesOnly = true,
        ))
        assertEquals("test", RadarSettings.getForegroundService(context).iconString)
        assertEquals("red", RadarSettings.getForegroundService(context).iconColor)
    }

    @Test
    fun test_Radar_setMetadata_null() {
        Radar.setMetadata(null)
        assertNull(Radar.getMetadata())
    }

    @Test
    fun test_Radar_addUserTags() {
        val userTags = arrayOf("premium", "beta_user")
        Radar.addUserTags(userTags)
        
        val retrievedTags = Radar.getUserTags()
        assertNotNull(retrievedTags)
        assertEquals(2, retrievedTags!!.size)
        assertTrue(retrievedTags.contains("premium"))
        assertTrue(retrievedTags.contains("beta_user"))
        
        // Clean up
        Radar.removeUserTags(userTags)
    }

    @Test
    fun test_Radar_removeUserTags() {
        // Add tags first
        Radar.addUserTags(arrayOf("premium", "beta_user"))
        
        // Remove some tags
        Radar.removeUserTags(arrayOf("beta_user"))
        
        val retrievedTags = Radar.getUserTags()
        assertNotNull(retrievedTags)
        assertEquals(1, retrievedTags!!.size)
        assertTrue(retrievedTags.contains("premium"))
        assertFalse(retrievedTags.contains("beta_user"))
        
        // Clean up
        Radar.removeUserTags(arrayOf("premium"))
    }

    @Test
    fun test_Radar_getUserTags_initial() {
        // Clear any existing tags
        Radar.removeUserTags(arrayOf("premium", "beta_user"))
        
        // Initially should be null
        assertNull(Radar.getUserTags())
    }

    @Test
    fun test_Radar_addUserTags_duplicates() {
        // Add tags first time
        Radar.addUserTags(arrayOf("premium", "beta_user"))
        
        // Add same tags again (should not create duplicates)
        Radar.addUserTags(arrayOf("premium", "beta_user"))
        
        val retrievedTags = Radar.getUserTags()
        assertNotNull(retrievedTags)
        assertEquals(2, retrievedTags!!.size)
        assertTrue(retrievedTags.contains("premium"))
        assertTrue(retrievedTags.contains("beta_user"))
        
        // Clean up
        Radar.removeUserTags(arrayOf("premium", "beta_user"))
    }

    @Test
    fun test_Radar_addUserTags_additional() {
        // Add initial tags
        Radar.addUserTags(arrayOf("premium"))
        
        // Add additional tags
        Radar.addUserTags(arrayOf("beta_user", "vip"))
        
        val retrievedTags = Radar.getUserTags()
        assertNotNull(retrievedTags)
        assertEquals(3, retrievedTags!!.size)
        assertTrue(retrievedTags.contains("premium"))
        assertTrue(retrievedTags.contains("beta_user"))
        assertTrue(retrievedTags.contains("vip"))
        
        // Clean up
        Radar.removeUserTags(arrayOf("premium", "beta_user", "vip"))
    }

    @Test
    fun test_Radar_removeUserTags_all() {
        // Add tags first
        Radar.addUserTags(arrayOf("premium", "beta_user"))
        
        // Remove all tags
        Radar.removeUserTags(arrayOf("premium", "beta_user"))
        
        val retrievedTags = Radar.getUserTags()
        assertNull(retrievedTags)
        
        // No cleanup needed since all tags were removed
    }

    @Test
    fun test_Radar_removeUserTags_nonexistent() {
        // Add some tags
        Radar.addUserTags(arrayOf("premium"))
        
        // Try to remove tags that don't exist
        Radar.removeUserTags(arrayOf("nonexistent_tag"))
        
        val retrievedTags = Radar.getUserTags()
        assertNotNull(retrievedTags)
        assertEquals(1, retrievedTags!!.size)
        assertTrue(retrievedTags.contains("premium"))
        
        // Clean up
        Radar.removeUserTags(arrayOf("premium"))
    }

    @Test
    fun test_Radar_userTags_persistence() {
        // Add tags
        Radar.addUserTags(arrayOf("premium", "beta_user"))
        
        // Verify they are stored
        val retrievedTags = Radar.getUserTags()
        assertNotNull(retrievedTags)
        assertEquals(2, retrievedTags!!.size)
        
        // Verify they are stored in RadarSettings directly
        val settingsTags = RadarSettings.getUserTags(context)
        assertNotNull(settingsTags)
        assertEquals(2, settingsTags!!.size)
        assertTrue(settingsTags.contains("premium"))
        assertTrue(settingsTags.contains("beta_user"))
        
        // Clean up
        Radar.removeUserTags(arrayOf("premium", "beta_user"))
    }

    @Test
    fun test_Radar_userTags_included_in_track_request() {
        // Set up location permissions and mock location
        permissionsHelperMock.mockFineLocationPermissionGranted = true
        val mockLocation = Location("RadarSDK")
        mockLocation.latitude = 40.78382
        mockLocation.longitude = -73.97536
        mockLocation.accuracy = 65f
        mockLocation.time = System.currentTimeMillis()
        locationClientMock.mockLocation = mockLocation

        // Set up mock response
        apiHelperMock.mockStatus = Radar.RadarStatus.SUCCESS
        apiHelperMock.mockResponse = RadarTestUtils.jsonObjectFromResource("/track.json")

        // Add user tags
        Radar.addUserTags(arrayOf("premium", "beta_user"))

        // Clear captured parameters
        apiHelperMock.clearCapturedParams()

        // Perform track request
        val latch = CountDownLatch(1)
        var callbackStatus: Radar.RadarStatus? = null

        Radar.trackOnce { status, _, _, _ ->
            callbackStatus = status
            latch.countDown()
        }

        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
        latch.await(LATCH_TIMEOUT, TimeUnit.SECONDS)

        // Verify the request was successful
        assertEquals(Radar.RadarStatus.SUCCESS, callbackStatus)

        // Verify the track request was made
        assertEquals("POST", apiHelperMock.lastCapturedMethod)
        assertEquals("v1/track", apiHelperMock.lastCapturedPath)

        // Verify user tags were included in the request
        val capturedParams = apiHelperMock.lastCapturedParams
        assertNotNull(capturedParams)
        
        val userTagsArray = capturedParams!!.optJSONArray("userTags")
        assertNotNull(userTagsArray)
        assertEquals(2, userTagsArray!!.length())
        
        val userTagsList = mutableListOf<String>()
        for (i in 0 until userTagsArray.length()) {
            userTagsList.add(userTagsArray.getString(i))
        }
        
        assertTrue(userTagsList.contains("premium"))
        assertTrue(userTagsList.contains("beta_user"))
        
        // Clean up
        Radar.removeUserTags(arrayOf("premium", "beta_user"))
    }

    @Test
    fun test_Radar_userTags_not_included_when_empty() {
        // Set up location permissions and mock location
        permissionsHelperMock.mockFineLocationPermissionGranted = true
        val mockLocation = Location("RadarSDK")
        mockLocation.latitude = 40.78382
        mockLocation.longitude = -73.97536
        mockLocation.accuracy = 65f
        mockLocation.time = System.currentTimeMillis()
        locationClientMock.mockLocation = mockLocation

        // Set up mock response
        apiHelperMock.mockStatus = Radar.RadarStatus.SUCCESS
        apiHelperMock.mockResponse = RadarTestUtils.jsonObjectFromResource("/track.json")

        // Ensure no user tags are set
        Radar.removeUserTags(arrayOf("premium", "beta_user"))

        // Clear captured parameters
        apiHelperMock.clearCapturedParams()

        // Perform track request
        val latch = CountDownLatch(1)
        var callbackStatus: Radar.RadarStatus? = null

        Radar.trackOnce { status, _, _, _ ->
            callbackStatus = status
            latch.countDown()
        }

        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
        latch.await(LATCH_TIMEOUT, TimeUnit.SECONDS)

        // Verify the request was successful
        assertEquals(Radar.RadarStatus.SUCCESS, callbackStatus)

        // Verify the track request was made
        assertEquals("POST", apiHelperMock.lastCapturedMethod)
        assertEquals("v1/track", apiHelperMock.lastCapturedPath)

        // Verify user tags were NOT included in the request
        val capturedParams = apiHelperMock.lastCapturedParams
        assertNotNull(capturedParams)
        
        val userTagsArray = capturedParams!!.optJSONArray("userTags")
        assertNull(userTagsArray)
        
        // No cleanup needed since no tags were set
    }

    @Test
    fun test_Radar_userTags_manual_track() {
        // Set up mock response
        apiHelperMock.mockStatus = Radar.RadarStatus.SUCCESS
        apiHelperMock.mockResponse = RadarTestUtils.jsonObjectFromResource("/track.json")

        // Add user tags
        Radar.addUserTags(arrayOf("premium", "beta_user"))

        // Clear captured parameters
        apiHelperMock.clearCapturedParams()

        // Create a manual location
        val manualLocation = Location("RadarSDK")
        manualLocation.latitude = 40.78382
        manualLocation.longitude = -73.97536
        manualLocation.accuracy = 65f
        manualLocation.time = System.currentTimeMillis()

        // Perform manual track
        val latch = CountDownLatch(1)
        var callbackStatus: Radar.RadarStatus? = null

        Radar.trackOnce(manualLocation) { status, _, _, _ ->
            callbackStatus = status
            latch.countDown()
        }

        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
        latch.await(LATCH_TIMEOUT, TimeUnit.SECONDS)

        // Verify the request was successful
        assertEquals(Radar.RadarStatus.SUCCESS, callbackStatus)

        // Verify user tags were included in the request
        val capturedParams = apiHelperMock.lastCapturedParams
        assertNotNull(capturedParams)
        
        val userTagsArray = capturedParams!!.optJSONArray("userTags")
        assertNotNull(userTagsArray)
        assertEquals(2, userTagsArray!!.length())
        
        val userTagsList = mutableListOf<String>()
        for (i in 0 until userTagsArray.length()) {
            userTagsList.add(userTagsArray.getString(i))
        }
        
        assertTrue(userTagsList.contains("premium"))
        assertTrue(userTagsList.contains("beta_user"))
        
        // Clean up
        Radar.removeUserTags(arrayOf("premium", "beta_user"))
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

        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
        latch.await(LATCH_TIMEOUT, TimeUnit.SECONDS)

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
        latch.await(LATCH_TIMEOUT, TimeUnit.SECONDS)

        assertEquals(Radar.RadarStatus.ERROR_LOCATION, callbackStatus)
    }

    @Test
    fun test_Radar_getLocation_success() {
        permissionsHelperMock.mockFineLocationPermissionGranted = true
        val mockLocation = Location("RadarSDK")
        mockLocation.latitude = 40.78382
        mockLocation.longitude = -73.97536
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

        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
        latch.await(LATCH_TIMEOUT, TimeUnit.SECONDS)

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

        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
        latch.await(LATCH_TIMEOUT, TimeUnit.SECONDS)

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
        latch.await(LATCH_TIMEOUT, TimeUnit.SECONDS)

        assertEquals(Radar.RadarStatus.ERROR_LOCATION, callbackStatus)
    }

    @Test
    fun test_Radar_trackOnce_success() {
        permissionsHelperMock.mockFineLocationPermissionGranted = true
        val mockLocation = Location("RadarSDK")
        mockLocation.latitude = 40.78382
        mockLocation.longitude = -73.97536
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

        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
        latch.await(LATCH_TIMEOUT, TimeUnit.SECONDS)

        assertEquals(Radar.RadarStatus.SUCCESS, callbackStatus)
        assertEquals(mockLocation, callbackLocation)
        assertEventsOk(callbackEvents)
        assertUserOk(callbackUser)
    }

    @Test
    fun test_Radar_trackOnce_location_success() {
        permissionsHelperMock.mockFineLocationPermissionGranted = false
        val mockLocation = Location("RadarSDK")
        mockLocation.latitude = 40.78382
        mockLocation.longitude = -73.97536
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

        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
        latch.await(LATCH_TIMEOUT, TimeUnit.SECONDS)

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

        Radar.setForegroundServiceOptions(RadarTrackingOptions.RadarTrackingOptionsForegroundService(
            text="Text",
            title = "Title",
            icon = 1337,
            iconString = "test",
            updatesOnly = true,
            iconColor = "#FF0000"
        ))

        val options = RadarTrackingOptions.EFFICIENT
        options.desiredAccuracy = RadarTrackingOptions.RadarTrackingOptionsDesiredAccuracy.LOW
        val now = Date()
        options.startTrackingAfter = now
        options.stopTrackingAfter = Date(now.time + 1000)
        options.sync = RadarTrackingOptions.RadarTrackingOptionsSync.NONE
        options.syncGeofences = RadarTrackingOptionsSyncGeofences.NEAREST
        options.syncGeofencesLimit = 100
        Radar.startTracking(options)
        assertEquals(options, Radar.getTrackingOptions())
        assertTrue(Radar.isTracking())
    }

    @Test
    fun test_Radar_startTracking_custom_enum_int() {
        permissionsHelperMock.mockFineLocationPermissionGranted = true

        Radar.stopTracking()

        val options = RadarTrackingOptions.CONTINUOUS
        options.desiredAccuracy = RadarTrackingOptions.RadarTrackingOptionsDesiredAccuracy.LOW
        options.replay = RadarTrackingOptions.RadarTrackingOptionsReplay.STOPS
        options.sync = RadarTrackingOptions.RadarTrackingOptionsSync.NONE
        val json = options.toJson()
        json.put("desiredAccuracy", 1)
        json.put("replay", 1)
        json.put("sync", 0)
        val newOptions = RadarTrackingOptions.fromJson(json)
        Radar.startTracking(newOptions)
        assertEquals(newOptions, Radar.getTrackingOptions())
        assertEquals(newOptions, options)
        assertTrue(Radar.isTracking())
    }

    @Test
    fun test_Radar_startTracking_custom_enum_string() {
        permissionsHelperMock.mockFineLocationPermissionGranted = true

        Radar.stopTracking()

        val options = RadarTrackingOptions.CONTINUOUS
        options.desiredAccuracy = RadarTrackingOptions.RadarTrackingOptionsDesiredAccuracy.LOW
        options.replay = RadarTrackingOptions.RadarTrackingOptionsReplay.STOPS
        options.sync = RadarTrackingOptions.RadarTrackingOptionsSync.NONE
        val json = options.toJson()
        json.put("desiredAccuracy", "low")
        json.put("replay", "stops")
        json.put("sync", "none")
        val newOptions = RadarTrackingOptions.fromJson(json)
        Radar.startTracking(newOptions)
        assertEquals(newOptions, Radar.getTrackingOptions())
        assertEquals(newOptions, options)
        assertTrue(Radar.isTracking())
    }

    @Test
    fun test_Radar_stopTracking() {
        Radar.stopTracking()
        assertFalse(Radar.isTracking())
    }

    @Test
    fun test_Radar_mockTracking() {
        permissionsHelperMock.mockFineLocationPermissionGranted = false
        apiHelperMock.mockStatus = Radar.RadarStatus.SUCCESS
        apiHelperMock.mockResponse = RadarTestUtils.jsonObjectFromResource("/route_distance.json")

        val origin = Location("RadarSDK")
        origin.latitude = 40.78382
        origin.longitude = -73.97536

        val destination = Location("RadarSDK")
        destination.latitude = 40.70390
        destination.longitude = -73.98670

        val steps = 20
        val latch = CountDownLatch(steps)

        Radar.mockTracking(origin, destination, Radar.RadarRouteMode.CAR, steps, 1) { _, _, _, _ ->
            latch.countDown()
        }

        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
        latch.await(LATCH_TIMEOUT, TimeUnit.SECONDS)
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
    fun test_Radar_startTrip() {
        val tripOptions = getTestTripOptions()

        Radar.startTrip(tripOptions) { status, trip, events ->
            assertEquals(tripOptions, Radar.getTripOptions())
            assertFalse(Radar.isTracking())
        }
    }

    @Test
    fun test_Radar_startTrip_trackingOptions() {
        // responsive mode before trip
        val responsive = RadarTrackingOptions.RESPONSIVE
        Radar.startTracking(responsive)
        assertTrue(Radar.isTracking())

        val tripOptions = getTestTripOptions()

        // start trip w/ continuous mode
        val onTripTrackingOptions = RadarTrackingOptions.CONTINUOUS
        Radar.startTrip(tripOptions, onTripTrackingOptions) { status, trip, events ->
            assertEquals(tripOptions, Radar.getTripOptions())
            assertEquals(onTripTrackingOptions, Radar.getTrackingOptions())
            assertEquals(responsive, RadarSettings.getPreviousTrackingOptions(context))
            assertTrue(Radar.isTracking())
        }

        // returns back to responsive mode after trip
        Radar.completeTrip() { _, _, _ ->
            assertEquals(null, RadarSettings.getPreviousTrackingOptions(context))
            assertEquals(responsive, Radar.getTrackingOptions())
            assertTrue(Radar.isTracking())
        }
    }

    @Test
    fun test_Radar_startTrip_scheduledArrivalAt() {
        val tripOptions = getTestTripOptions()
        assertNull(tripOptions.scheduledArrivalAt)
        val tripOptionsJson = tripOptions.toJson()
        assertFalse(tripOptionsJson.has("scheduledArrivalAt"))

        val newScheduledArrivalAt = Date()
        tripOptions.scheduledArrivalAt = newScheduledArrivalAt
        val newTripOptionsJson = tripOptions.toJson()
        assertEquals(newTripOptionsJson.getString("scheduledArrivalAt"), RadarUtils.dateToISOString(tripOptions.scheduledArrivalAt))
    }

    @Test
    fun test_Radar_startTrip_notTracking() {
        // not tracking before trip
        Radar.stopTracking()

        val tripOptions = getTestTripOptions()

        // start trip w/ continuous mode
        val continuous = RadarTrackingOptions.CONTINUOUS
        Radar.startTrip(tripOptions, continuous) { status, trip, events ->
            assertEquals(tripOptions, Radar.getTripOptions())
            assertEquals(continuous, Radar.getTrackingOptions())
            assertEquals(null, RadarSettings.getPreviousTrackingOptions(context))
            assertTrue(Radar.isTracking())
        }

        // returns back to not tracking after trip
        Radar.completeTrip() { _, _, _ ->
            assertEquals(null, RadarSettings.getPreviousTrackingOptions(context))
            assertEquals(continuous, Radar.getTrackingOptions())
            assertFalse(Radar.isTracking())
        }
    }

    @Test
    fun test_Radar_completeTrip() {
        Radar.completeTrip()
        assertNull(Radar.getTripOptions())
    }

    @Test
    fun test_Radar_cancelTrip() {
        Radar.cancelTrip()
        assertNull(Radar.getTripOptions())
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

        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
        latch.await(LATCH_TIMEOUT, TimeUnit.SECONDS)

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
        latch.await(LATCH_TIMEOUT, TimeUnit.SECONDS)

        assertEquals(Radar.RadarStatus.ERROR_LOCATION, callbackStatus)
    }

    @Test
    fun test_Radar_getContext_success() {
        permissionsHelperMock.mockFineLocationPermissionGranted = true
        val mockLocation = Location("RadarSDK")
        mockLocation.latitude = 40.78382
        mockLocation.longitude = -73.97536
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

        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
        latch.await(LATCH_TIMEOUT, TimeUnit.SECONDS)

        assertEquals(Radar.RadarStatus.SUCCESS, callbackStatus)
        assertEquals(mockLocation, callbackLocation)
        assertContextOk(callbackContext)
    }

    @Test
    fun test_Radar_getContext_location_success() {
        permissionsHelperMock.mockFineLocationPermissionGranted = false
        val mockLocation = Location("RadarSDK")
        mockLocation.latitude = 40.78382
        mockLocation.longitude = -73.97536
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

        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
        latch.await(LATCH_TIMEOUT, TimeUnit.SECONDS)

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

        Radar.searchPlaces(1000, arrayOf("walmart"), null, null, null, null, 100) { status, _, _ ->
            callbackStatus = status
            latch.countDown()
        }

        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
        latch.await(LATCH_TIMEOUT, TimeUnit.SECONDS)

        assertEquals(Radar.RadarStatus.ERROR_PERMISSIONS, callbackStatus)
    }

    @Test
    fun test_Radar_searchPlaces_errorLocation() {
        permissionsHelperMock.mockFineLocationPermissionGranted = true
        locationClientMock.mockLocation = null

        val latch = CountDownLatch(1)
        var callbackStatus: Radar.RadarStatus? = null

        Radar.searchPlaces(1000, arrayOf("walmart"), null, null, null, null, 100) { status, _, _ ->
            callbackStatus = status
            latch.countDown()
        }

        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
        latch.await(LATCH_TIMEOUT, TimeUnit.SECONDS)

        assertEquals(Radar.RadarStatus.ERROR_LOCATION, callbackStatus)
    }

    @Test
    fun test_Radar_searchPlaces_success() {
        permissionsHelperMock.mockFineLocationPermissionGranted = true
        val mockLocation = Location("RadarSDK")
        mockLocation.latitude = 40.78382
        mockLocation.longitude = -73.97536
        mockLocation.accuracy = 65f
        mockLocation.time = System.currentTimeMillis()
        locationClientMock.mockLocation = mockLocation
        apiHelperMock.mockStatus = Radar.RadarStatus.SUCCESS
        apiHelperMock.mockResponse = RadarTestUtils.jsonObjectFromResource("/search_places.json")

        val latch = CountDownLatch(1)
        var callbackStatus: Radar.RadarStatus? = null
        var callbackLocation: Location? = null
        var callbackPlaces: Array<RadarPlace>? = null

        Radar.searchPlaces(1000, arrayOf("walmart"), null, null, null, null, 100) { status, location, places ->
            callbackStatus = status
            callbackLocation = location
            callbackPlaces = places
            latch.countDown()
        }

        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
        latch.await(LATCH_TIMEOUT, TimeUnit.SECONDS)

        assertEquals(Radar.RadarStatus.SUCCESS, callbackStatus)
        assertEquals(mockLocation, callbackLocation)
        assertPlacesOk(callbackPlaces)
    }

    @Test
    fun test_Radar_searchPlacesWithChainMetadata_success() {
        permissionsHelperMock.mockFineLocationPermissionGranted = true
        val mockLocation = Location("RadarSDK")
        mockLocation.latitude = 40.78382
        mockLocation.longitude = -73.97536
        mockLocation.accuracy = 65f
        mockLocation.time = System.currentTimeMillis()
        locationClientMock.mockLocation = mockLocation
        apiHelperMock.mockStatus = Radar.RadarStatus.SUCCESS
        apiHelperMock.mockResponse = RadarTestUtils.jsonObjectFromResource("/search_places_chain_metadata.json")

        val latch = CountDownLatch(1)
        var callbackStatus: Radar.RadarStatus? = null
        var callbackLocation: Location? = null
        var callbackPlaces: Array<RadarPlace>? = null
        val chainMetadata = mapOf("orderActive" to "true")

        Radar.searchPlaces(1000, arrayOf("walmart"), chainMetadata,  null, null, null, 100) { status, location, places ->
            callbackStatus = status
            callbackLocation = location
            callbackPlaces = places
            latch.countDown()
        }

        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
        latch.await(LATCH_TIMEOUT, TimeUnit.SECONDS)

        assertEquals(Radar.RadarStatus.SUCCESS, callbackStatus)
        assertEquals(mockLocation, callbackLocation)
        assertPlacesOk(callbackPlaces)
        assertEquals(chainMetadata["orderActive"], callbackPlaces!!.first()!!.chain!!.metadata!!["orderActive"])
    }

    @Test
    fun test_Radar_searchPlaces_location_success() {
        permissionsHelperMock.mockFineLocationPermissionGranted = false
        val mockLocation = Location("RadarSDK")
        mockLocation.latitude = 40.78382
        mockLocation.longitude = -73.97536
        mockLocation.accuracy = 65f
        mockLocation.time = System.currentTimeMillis()
        apiHelperMock.mockStatus = Radar.RadarStatus.SUCCESS
        apiHelperMock.mockResponse = RadarTestUtils.jsonObjectFromResource("/search_places.json")

        val latch = CountDownLatch(1)
        var callbackStatus: Radar.RadarStatus? = null
        var callbackLocation: Location? = null
        var callbackPlaces: Array<RadarPlace>? = null

        Radar.searchPlaces(mockLocation, 1000, arrayOf("walmart"), null, null, null, 100) { status, location, places ->
            callbackStatus = status
            callbackLocation = location
            callbackPlaces = places
            latch.countDown()
        }

        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
        latch.await(LATCH_TIMEOUT, TimeUnit.SECONDS)

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

        val metadata = JSONObject()
        metadata.put("foo", "bar")

        Radar.searchGeofences(1000, arrayOf("store"), metadata, 100) { status, _, _ ->
            callbackStatus = status
            latch.countDown()
        }

        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
        latch.await(LATCH_TIMEOUT, TimeUnit.SECONDS)

        assertEquals(Radar.RadarStatus.ERROR_PERMISSIONS, callbackStatus)
    }

    @Test
    fun test_Radar_searchGeofences_errorLocation() {
        permissionsHelperMock.mockFineLocationPermissionGranted = true
        locationClientMock.mockLocation = null

        val latch = CountDownLatch(1)
        var callbackStatus: Radar.RadarStatus? = null

        val metadata = JSONObject()
        metadata.put("foo", "bar")

        Radar.searchGeofences(1000, arrayOf("store"), metadata, 100) { status, _, _ ->
            callbackStatus = status
            latch.countDown()
        }

        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
        latch.await(LATCH_TIMEOUT, TimeUnit.SECONDS)

        assertEquals(Radar.RadarStatus.ERROR_LOCATION, callbackStatus)
    }

    @Test
    fun test_Radar_searchGeofences_success() {
        permissionsHelperMock.mockFineLocationPermissionGranted = true
        val mockLocation = Location("RadarSDK")
        mockLocation.latitude = 40.78382
        mockLocation.longitude = -73.97536
        mockLocation.accuracy = 65f
        mockLocation.time = System.currentTimeMillis()
        locationClientMock.mockLocation = mockLocation
        apiHelperMock.mockStatus = Radar.RadarStatus.SUCCESS
        apiHelperMock.mockResponse = RadarTestUtils.jsonObjectFromResource("/search_geofences.json")

        val latch = CountDownLatch(1)
        var callbackStatus: Radar.RadarStatus? = null
        var callbackLocation: Location? = null
        var callbackGeofences: Array<RadarGeofence>? = null

        val metadata = JSONObject()
        metadata.put("foo", "bar")

        Radar.searchGeofences(1000, arrayOf("store"), metadata, 100) { status, location, geofences ->
            callbackStatus = status
            callbackLocation = location
            callbackGeofences = geofences
            latch.countDown()
        }

        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
        latch.await(LATCH_TIMEOUT, TimeUnit.SECONDS)

        assertEquals(Radar.RadarStatus.SUCCESS, callbackStatus)
        assertEquals(mockLocation, callbackLocation)
        assertGeofencesOk(callbackGeofences)

        // test that a geofence.toJson has a radius and geometryCenter
        val geofence = callbackGeofences!!.first()
        val geofenceJson = geofence.toJson()
        assertNotNull(geofenceJson)
        assertTrue(geofenceJson.has("geometryRadius"))
        assertTrue(geofenceJson.has("geometryCenter"))

        assertTrue(geofenceJson.has("operatingHours"))

    }

    @Test
    fun test_Radar_searchGeofences_location_success() {
        permissionsHelperMock.mockFineLocationPermissionGranted = false
        val mockLocation = Location("RadarSDK")
        mockLocation.latitude = 40.78382
        mockLocation.longitude = -73.97536
        mockLocation.accuracy = 65f
        mockLocation.time = System.currentTimeMillis()
        apiHelperMock.mockStatus = Radar.RadarStatus.SUCCESS
        apiHelperMock.mockResponse = RadarTestUtils.jsonObjectFromResource("/search_geofences.json")

        val latch = CountDownLatch(1)
        var callbackStatus: Radar.RadarStatus? = null
        var callbackLocation: Location? = null
        var callbackGeofences: Array<RadarGeofence>? = null

        Radar.searchGeofences(mockLocation, 1000, arrayOf("store"), null, 100) { status, location, geofences ->
            callbackStatus = status
            callbackLocation = location
            callbackGeofences = geofences
            latch.countDown()
        }

        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
        latch.await(LATCH_TIMEOUT, TimeUnit.SECONDS)

        assertEquals(Radar.RadarStatus.SUCCESS, callbackStatus)
        assertEquals(mockLocation, callbackLocation)
        assertGeofencesOk(callbackGeofences)
    }

    @Test
    fun test_Radar_autocomplete_success() {
        permissionsHelperMock.mockFineLocationPermissionGranted = false
        apiHelperMock.mockStatus = Radar.RadarStatus.SUCCESS
        apiHelperMock.mockResponse = RadarTestUtils.jsonObjectFromResource("/search_autocomplete.json")

        val near = Location("RadarSDK")
        near.latitude = 40.78382
        near.longitude = -73.97536

        val latch = CountDownLatch(1)
        var callbackStatus: Radar.RadarStatus? = null
        var callbackAddresses: Array<RadarAddress>? = null

        Radar.autocomplete("brooklyn roasting", near, null, 10, null) { status, addresses ->
            callbackStatus = status
            callbackAddresses = addresses
            latch.countDown()
        }

        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
        latch.await(LATCH_TIMEOUT, TimeUnit.SECONDS)

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

        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
        latch.await(LATCH_TIMEOUT, TimeUnit.SECONDS)

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

        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
        latch.await(LATCH_TIMEOUT, TimeUnit.SECONDS)

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

        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
        latch.await(LATCH_TIMEOUT, TimeUnit.SECONDS)

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
        latch.await(LATCH_TIMEOUT, TimeUnit.SECONDS)

        assertEquals(Radar.RadarStatus.ERROR_LOCATION, callbackStatus)
        assertNull(callbackAddresses)
    }

    @Test
    fun test_Radar_reverseGeocode_success() {
        permissionsHelperMock.mockFineLocationPermissionGranted = true
        locationClientMock.mockLocation = null

        permissionsHelperMock.mockFineLocationPermissionGranted = true
        val mockLocation = Location("RadarSDK")
        mockLocation.latitude = 40.78382
        mockLocation.longitude = -73.97536
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

        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
        latch.await(LATCH_TIMEOUT, TimeUnit.SECONDS)

        assertEquals(Radar.RadarStatus.SUCCESS, callbackStatus)
        assertAddressesOk(callbackAddresses)
    }

    @Test
    fun test_Radar_reverseGeocode_location_error() {
        permissionsHelperMock.mockFineLocationPermissionGranted = false
        apiHelperMock.mockStatus = Radar.RadarStatus.ERROR_SERVER

        val mockLocation = Location("RadarSDK")
        mockLocation.latitude = 40.78382
        mockLocation.longitude = -73.97536

        val latch = CountDownLatch(1)

        var callbackStatus: Radar.RadarStatus? = null
        var callbackAddresses: Array<RadarAddress>? = null

        Radar.reverseGeocode(mockLocation) { status, addresses ->
            callbackStatus = status
            callbackAddresses = addresses
            latch.countDown()
        }

        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
        latch.await(LATCH_TIMEOUT, TimeUnit.SECONDS)

        assertEquals(Radar.RadarStatus.ERROR_SERVER, callbackStatus)
        assertNull(callbackAddresses)
    }

    @Test
    fun test_Radar_reverseGeocode_location_success() {
        permissionsHelperMock.mockFineLocationPermissionGranted = false
        apiHelperMock.mockStatus = Radar.RadarStatus.SUCCESS
        apiHelperMock.mockResponse = RadarTestUtils.jsonObjectFromResource("/geocode.json")

        val mockLocation = Location("RadarSDK")
        mockLocation.latitude = 40.78382
        mockLocation.longitude = -73.97536

        val latch = CountDownLatch(1)

        var callbackStatus: Radar.RadarStatus? = null
        var callbackAddresses: Array<RadarAddress>? = null

        Radar.reverseGeocode(mockLocation) { status, addresses ->
            callbackStatus = status
            callbackAddresses = addresses
            latch.countDown()
        }

        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
        latch.await(LATCH_TIMEOUT, TimeUnit.SECONDS)

        assertEquals(Radar.RadarStatus.SUCCESS, callbackStatus)
        assertAddressesOk(callbackAddresses)
        
        // Add timezone verification
        val address = callbackAddresses?.get(0)
        assertNotNull(address?.timeZone)
        assertEquals("America/New_York", address?.timeZone?.id)
        assertEquals("Eastern Standard Time", address?.timeZone?.name)
        assertEquals("EST", address?.timeZone?.code)
        assertEquals(-18000, address?.timeZone?.utcOffset)
        assertEquals(0, address?.timeZone?.dstOffset)
        
        // Test the Date object
        val timeZoneDate = address?.timeZone?.currentTime
        assertNotNull(timeZoneDate)
        // January 21, 2025 12:19:23 EST
        val expectedTime = Calendar.getInstance(TimeZone.getTimeZone("America/New_York")).apply {
            set(2025, Calendar.JANUARY, 21, 12, 19, 23)
            set(Calendar.MILLISECOND, 0)
        }.time
        assertEquals(expectedTime, timeZoneDate)
        
        // Test the formatted string representation
        val timeZoneJson = address?.timeZone?.toJson()
        val formattedTime = timeZoneJson?.optString("currentTime")
        assertNotNull(formattedTime)
        assertTrue("NYC time should end with -05:00 offset but was: $formattedTime", 
            formattedTime != null && formattedTime.toString().endsWith("-05:00"))
    }

    @Test
    fun test_Radar_reverseGeocode_london_location_success() {
        permissionsHelperMock.mockFineLocationPermissionGranted = false
        apiHelperMock.mockStatus = Radar.RadarStatus.SUCCESS
        apiHelperMock.mockResponse = RadarTestUtils.jsonObjectFromResource("/geocode_london.json")

        val mockLocation = Location("RadarSDK")
        mockLocation.latitude = 51.5074
        mockLocation.longitude = -0.1278

        val latch = CountDownLatch(1)
        var callbackStatus: Radar.RadarStatus? = null
        var callbackAddresses: Array<RadarAddress>? = null

        Radar.reverseGeocode(mockLocation) { status, addresses ->
            callbackStatus = status
            callbackAddresses = addresses
            latch.countDown()
        }

        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
        latch.await(LATCH_TIMEOUT, TimeUnit.SECONDS)

        assertEquals(Radar.RadarStatus.SUCCESS, callbackStatus)
        
        // Add timezone verification
        val address = callbackAddresses?.get(0)
        assertNotNull(address?.timeZone)
        assertEquals("Europe/London", address?.timeZone?.id)
        assertEquals("Greenwich Mean Time", address?.timeZone?.name)
        assertEquals("GMT", address?.timeZone?.code)
        assertEquals(0, address?.timeZone?.utcOffset)
        assertEquals(0, address?.timeZone?.dstOffset)
        
        // Test the Date object
        val timeZoneDate = address?.timeZone?.currentTime
        assertNotNull(timeZoneDate)
        // January 21, 2025 17:22:19 UTC
        val expectedTime = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            set(2025, Calendar.JANUARY, 21, 17, 22, 19)
            set(Calendar.MILLISECOND, 0)
        }.time
        assertEquals(expectedTime, timeZoneDate)
        
        // Test the formatted string representation
        val timeZoneJson = address?.timeZone?.toJson()
        val formattedTime = timeZoneJson?.optString("currentTime")
        assertNotNull(formattedTime)
        assertTrue("London time should end with Z but was: $formattedTime", 
            formattedTime != null && formattedTime.toString().endsWith("Z"))
    }

    @Test
    fun test_Radar_reverseGeocode_darwin_location_success() {
        permissionsHelperMock.mockFineLocationPermissionGranted = false
        apiHelperMock.mockStatus = Radar.RadarStatus.SUCCESS
        apiHelperMock.mockResponse = RadarTestUtils.jsonObjectFromResource("/geocode_darwin.json")

        val mockLocation = Location("RadarSDK")
        mockLocation.latitude = -12.463872
        mockLocation.longitude = 130.844064

        val latch = CountDownLatch(1)
        var callbackStatus: Radar.RadarStatus? = null 
        var callbackAddresses: Array<RadarAddress>? = null

        Radar.reverseGeocode(mockLocation) { status, addresses ->
            callbackStatus = status
            callbackAddresses = addresses
            latch.countDown()
        }

        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
        latch.await(LATCH_TIMEOUT, TimeUnit.SECONDS)

        assertEquals(Radar.RadarStatus.SUCCESS, callbackStatus)
        
        // Add timezone verification
        val address = callbackAddresses?.get(0)
        assertNotNull(address?.timeZone)
        assertEquals("Australia/Darwin", address?.timeZone?.id)
        assertEquals("Australian Central Standard Time", address?.timeZone?.name)
        assertEquals("ACST", address?.timeZone?.code)
        assertEquals(34200, address?.timeZone?.utcOffset)
        assertEquals(0, address?.timeZone?.dstOffset)
        
        // Test the Date object
        val timeZoneDate = address?.timeZone?.currentTime
        assertNotNull(timeZoneDate)
        // January 22, 2025 04:17:35 ACST (+09:30)
        val expectedTime = Calendar.getInstance(TimeZone.getTimeZone("Australia/Darwin")).apply {
            set(2025, Calendar.JANUARY, 22, 4, 17, 35)
            set(Calendar.MILLISECOND, 0)
        }.time
        assertEquals(expectedTime, timeZoneDate)
        
        // Test the formatted string representation
        val timeZoneJson = address?.timeZone?.toJson()
        val formattedTime = timeZoneJson?.optString("currentTime")
        assertNotNull(formattedTime)
        assertTrue("Darwin time should end with +09:30 but was: $formattedTime", 
            formattedTime != null && formattedTime.toString().endsWith("+09:30"))
    }

    @Test
    fun test_Radar_ipGeocode_error() {
        permissionsHelperMock.mockFineLocationPermissionGranted = false
        apiHelperMock.mockStatus = Radar.RadarStatus.ERROR_SERVER

        val latch = CountDownLatch(1)

        var callbackStatus: Radar.RadarStatus? = null
        var callbackAddress: RadarAddress? = null
        var callbackProxy = false

        Radar.ipGeocode { status, address, proxy ->
            callbackStatus = status
            callbackAddress = address
            callbackProxy = proxy
            latch.countDown()
        }

        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
        latch.await(LATCH_TIMEOUT, TimeUnit.SECONDS)

        assertEquals(Radar.RadarStatus.ERROR_SERVER, callbackStatus)
        assertNull(callbackAddress)
        assertFalse(callbackProxy)
    }

    @Test
    fun test_Radar_ipGeocode_success() {
        permissionsHelperMock.mockFineLocationPermissionGranted = false
        apiHelperMock.mockStatus = Radar.RadarStatus.SUCCESS
        apiHelperMock.mockResponse = RadarTestUtils.jsonObjectFromResource("/geocode_ip.json")

        val latch = CountDownLatch(1)

        var callbackStatus: Radar.RadarStatus? = null
        var callbackAddress: RadarAddress? = null
        var callbackProxy = false

        Radar.ipGeocode { status, address, proxy ->
            callbackStatus = status
            callbackAddress = address
            callbackProxy = proxy
            latch.countDown()
        }

        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
        latch.await(LATCH_TIMEOUT, TimeUnit.SECONDS)

        assertEquals(Radar.RadarStatus.SUCCESS, callbackStatus)
        assertAddressOk(callbackAddress)
        assertNotNull(callbackAddress?.dma)
        assertNotNull(callbackAddress?.dmaCode)
        assertTrue(callbackProxy)
    }

    @Test
    fun test_Radar_getDistance_success() {
        permissionsHelperMock.mockFineLocationPermissionGranted = true
        val mockLocation = Location("RadarSDK")
        mockLocation.latitude = 40.78382
        mockLocation.longitude = -73.97536
        mockLocation.accuracy = 65f
        mockLocation.time = System.currentTimeMillis()
        locationClientMock.mockLocation = mockLocation
        apiHelperMock.mockStatus = Radar.RadarStatus.SUCCESS
        apiHelperMock.mockResponse = RadarTestUtils.jsonObjectFromResource("/route_distance.json")

        val destination = Location("RadarSDK")
        destination.latitude = 40.70390
        destination.longitude = -73.98670

        val latch = CountDownLatch(1)
        var callbackStatus: Radar.RadarStatus? = null
        var callbackRoutes: RadarRoutes? = null

        Radar.getDistance(destination, EnumSet.of(Radar.RadarRouteMode.FOOT, Radar.RadarRouteMode.CAR), Radar.RadarRouteUnits.IMPERIAL) { status, routes ->
            callbackStatus = status
            callbackRoutes = routes
            latch.countDown()
        }

        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
        latch.await(LATCH_TIMEOUT, TimeUnit.SECONDS)

        assertEquals(Radar.RadarStatus.SUCCESS, callbackStatus)
        assertRoutesOk(callbackRoutes)
    }

    @Test
    fun test_Radar_trackingOptionsRemote_success() {
        RadarSettings.setRemoteTrackingOptions(context, RadarTrackingOptions.RESPONSIVE)
        assertEquals(RadarTrackingOptions.RESPONSIVE, RadarSettings.getRemoteTrackingOptions(context))

        RadarSettings.setTrackingOptions(context, RadarTrackingOptions.CONTINUOUS)
        assertEquals(RadarTrackingOptions.CONTINUOUS, RadarSettings.getTrackingOptions(context))
        assertEquals(RadarTrackingOptions.RESPONSIVE, RadarSettings.getRemoteTrackingOptions(context))

        RadarSettings.removeRemoteTrackingOptions(context)
        assertEquals(RadarTrackingOptions.CONTINUOUS, RadarSettings.getTrackingOptions(context))
    }

    private fun setUpLogConversionTest() {
        permissionsHelperMock.mockFineLocationPermissionGranted = true
        apiHelperMock.mockStatus = Radar.RadarStatus.SUCCESS
        val mockLocation = Location("RadarSDK")
        mockLocation.latitude = 40.78382
        mockLocation.longitude = -73.97536
        mockLocation.accuracy = 65f
        mockLocation.time = System.currentTimeMillis()
        locationClientMock.mockLocation = mockLocation
        val trackPath = "v1/track"
        apiHelperMock.addMockResponse(trackPath, RadarTestUtils.jsonObjectFromResource("/track.json")!!)
        val eventsPath = "v1/events"
        apiHelperMock.addMockResponse(eventsPath, RadarTestUtils.jsonObjectFromResource("/conversion_event.json")!!)
    }
    @Test
    fun test_Radar_logConversionWithBlock_success() {
        setUpLogConversionTest()

        val conversionType = "test_event" // has to match the property in the conversion_event.json file!
        val latch = CountDownLatch(1)
        var callbackStatus: Radar.RadarStatus? = null
        var callbackEvent: RadarEvent? = null
        val metadata = JSONObject()
        metadata.put("foo", "bar")

        Radar.logConversion(conversionType, metadata) { status, event ->
            callbackStatus = status
            callbackEvent = event

            val conversionMetadata = event?.metadata
            assertNotNull(conversionMetadata)
            assertEquals("bar", conversionMetadata!!.get("foo"))

            latch.countDown()
        }

        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
        latch.await(LATCH_TIMEOUT, TimeUnit.SECONDS)

        assertEquals(Radar.RadarStatus.SUCCESS, callbackStatus)
        assertConversionEvent(callbackEvent, conversionType, metadata)
    }

    @Test
    fun test_Radar_logConversionWithRevenueAndBlock_success() {
        setUpLogConversionTest()

        val conversionType = "test_event" // has to match the property in the conversion_event.json file!
        val latch = CountDownLatch(1)
        var callbackStatus: Radar.RadarStatus? = null
        var callbackEvent: RadarEvent? = null
        val revenue = 0.2
        val metadata = JSONObject()
        metadata.put("foo", "bar")

        Radar.logConversion(conversionType, revenue, metadata) { status, event ->
            callbackStatus = status
            callbackEvent = event

            val conversionMetadata = event?.metadata
            assertNotNull(conversionMetadata)
            assertEquals("bar", conversionMetadata!!.get("foo"))
            assertEquals(revenue, conversionMetadata!!.get("revenue"))

            latch.countDown()
        }

        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
        latch.await(LATCH_TIMEOUT, TimeUnit.SECONDS)

        assertEquals(Radar.RadarStatus.SUCCESS, callbackStatus)
        assertConversionEvent(callbackEvent, conversionType, metadata)
    }

    private fun assertConversionEvent(
        event: RadarEvent?,
        conversionType: String,
        metadata: JSONObject
    ) {
        assertNotNull(event)
        assertEquals(conversionType, event!!.conversionName)
        assertNotNull(event!!.conversionName)

        val returnedMetadata = event!!.metadata
        assertNotNull(returnedMetadata)
        assertEquals(metadata.get("foo"), returnedMetadata!!["foo"])
    }

    private fun getTestTripOptions(): RadarTripOptions {
        val tripOptions = RadarTripOptions("tripExternalId")
        tripOptions.metadata = JSONObject(mapOf("foo" to "bar", "baz" to true, "qux" to 1))
        tripOptions.destinationGeofenceTag = "tripDestinationGeofenceTag"
        tripOptions.destinationGeofenceExternalId = "tripDestinationGeofenceExternalId"
        tripOptions.mode = Radar.RadarRouteMode.FOOT

        return tripOptions
    }

    @Test
    fun test_Radar_setSdkConfiguration() {
        val sdkConfiguration = RadarSdkConfiguration(1, false, false, false, false, false, Radar.RadarLogLevel.WARNING, true, true, true)

        RadarSettings.setUserDebug(context, false)
        RadarSettings.setSdkConfiguration(context, sdkConfiguration)

        assertEquals(Radar.RadarLogLevel.WARNING, RadarSettings.getLogLevel(context))

        apiHelperMock.mockStatus = Radar.RadarStatus.SUCCESS
        apiHelperMock.mockResponse = RadarTestUtils.jsonObjectFromResource("/get_config_response.json")

        val latch = CountDownLatch(1)

        Radar.apiClient.getConfig("sdkConfigUpdate", false, object : RadarApiClient.RadarGetConfigApiCallback {
            override fun onComplete(status: Radar.RadarStatus, config: RadarConfig?) {
                if (config != null) {
                    RadarSettings.setSdkConfiguration(context, config.meta.sdkConfiguration)
                }

                assertEquals(RadarSettings.getLogLevel(context), Radar.RadarLogLevel.INFO)

                latch.countDown()
            }
        })
        
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
        latch.await(LATCH_TIMEOUT, TimeUnit.SECONDS)

        Radar.setLogLevel(Radar.RadarLogLevel.DEBUG)
        val clientSdkConfiguration = RadarSettings.getClientSdkConfiguration(context)
        val logLevel = Radar.RadarLogLevel.valueOf(clientSdkConfiguration.get("logLevel").toString().uppercase())
        assertEquals(Radar.RadarLogLevel.DEBUG, logLevel)

        val savedSdkConfiguration = RadarSettings.getSdkConfiguration(context)
        assertEquals(Radar.RadarLogLevel.INFO, savedSdkConfiguration?.logLevel)
        assertEquals(true, savedSdkConfiguration?.startTrackingOnInitialize)
        assertEquals(true, savedSdkConfiguration?.trackOnceOnAppOpen)
        assertEquals(true,savedSdkConfiguration?.useLocationMetadata)
    }
}
