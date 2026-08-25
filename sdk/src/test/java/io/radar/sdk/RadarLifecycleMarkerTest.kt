package io.radar.sdk

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [android.os.Build.VERSION_CODES.P])
class RadarLifecycleMarkerTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val preferences = context.getSharedPreferences("RadarLifecycleMarkerTest", Context.MODE_PRIVATE)

    @Before
    fun setUp() {
        preferences.edit().clear().commit()
    }

    @Test
    fun beginProcessTracksPreviousProcessAndPersistsMarker() {
        val marker = RadarLifecycleMarker(preferences)

        assertFalse(marker.beginProcess())
        assertTrue(preferences.getBoolean("app_lifecycle_marker", false))
        assertFalse(marker.beginProcess())
        assertTrue(RadarLifecycleMarker(preferences).beginProcess())
    }
}
