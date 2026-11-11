package io.radar.sdk

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.UPSIDE_DOWN_CAKE])
class RadarLocationClientFactoryTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val logger = RadarLogger(context)

    @Test
    fun createLocationClient_google_returnsGoogleClient() {
        val client = RadarLocationManager.createLocationClient(
            context,
            logger,
            Radar.RadarLocationServicesProvider.GOOGLE
        ) { error("Google provider must not check for HMS") }

        assertTrue(client is RadarGoogleLocationClient)
    }

    @Test
    fun createLocationClient_huaweiWithoutHms_returnsUnavailableClient() {
        var checkedClass: String? = null
        val client = RadarLocationManager.createLocationClient(
            context,
            logger,
            Radar.RadarLocationServicesProvider.HUAWEI
        ) { className ->
            checkedClass = className
            false
        }

        assertEquals(RadarLocationManager.HUAWEI_LOCATION_CLASS, checkedClass)
        assertTrue(client is RadarUnavailableLocationClient)
    }

    @Test
    fun createLocationClient_huaweiDefaultCheck_returnsUnavailableClientWhenHmsIsNotOnClasspath() {
        // HMS location is compileOnly, so it is not on the unit test classpath.
        val client = RadarLocationManager.createLocationClient(
            context,
            logger,
            Radar.RadarLocationServicesProvider.HUAWEI
        )

        assertTrue(client is RadarUnavailableLocationClient)
    }

    @Test
    fun unavailableClient_failsEachCallWithoutThrowing() {
        val client = RadarUnavailableLocationClient(logger, RadarLocationManager.HUAWEI_UNAVAILABLE_MESSAGE)
        val pendingIntent = PendingIntent.getBroadcast(context, 0, Intent(), PendingIntent.FLAG_IMMUTABLE)
        val accuracy = RadarTrackingOptions.RadarTrackingOptionsDesiredAccuracy.HIGH

        var currentLocationCalled = false
        client.getCurrentLocation(accuracy) { location ->
            currentLocationCalled = true
            assertNull(location)
        }
        assertTrue(currentLocationCalled)

        var lastLocationCalled = false
        client.getLastLocation { location ->
            lastLocationCalled = true
            assertNull(location)
        }
        assertTrue(lastLocationCalled)

        var addSuccess: Boolean? = null
        client.addGeofences(
            emptyArray(),
            RadarAbstractLocationClient.RadarAbstractGeofenceRequest(),
            pendingIntent
        ) { success -> addSuccess = success }
        assertFalse(addSuccess!!)

        var removeSuccess: Boolean? = null
        client.removeGeofences(pendingIntent) { success -> removeSuccess = success }
        assertTrue(removeSuccess!!)

        client.requestLocationUpdates(accuracy, 60, 30, pendingIntent)
        client.removeLocationUpdates(pendingIntent)
        assertNull(client.getLocationFromGeofenceIntent(Intent()))
        assertNull(client.getSourceFromGeofenceIntent(Intent()))
        assertNull(client.getLocationFromLocationIntent(Intent()))
    }
}
