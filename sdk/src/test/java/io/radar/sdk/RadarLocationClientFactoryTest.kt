package io.radar.sdk

import android.content.Context
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
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
    fun createLocationClient_huaweiWithoutHms_fallsBackToGoogleClient() {
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
        assertTrue(client is RadarGoogleLocationClient)
    }

    @Test
    fun createLocationClient_huaweiDefaultCheck_fallsBackWhenHmsIsNotOnClasspath() {
        // HMS location is compileOnly, so it is not on the unit test classpath.
        val client = RadarLocationManager.createLocationClient(
            context,
            logger,
            Radar.RadarLocationServicesProvider.HUAWEI
        )

        assertTrue(client is RadarGoogleLocationClient)
    }
}
