package io.radar.sdk

import android.content.Intent
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Unit tests [RadarLocationReceiver]
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.P])
class RadarLocationReceiverTest {

    /**
     * This test ensures that [RadarLocationReceiver.onReceive] can be hit, even if [Radar.initialize] has not
     * yet been called by the application.
     */
    @Test
    fun testOnReceive() {
        try {
            RadarLocationReceiver().onReceive(ApplicationProvider.getApplicationContext(), Intent(""))
            assertTrue(Radar.initialized)
        } catch (ignored: UninitializedPropertyAccessException) {
            fail("onReceive error - Radar not initialized properly")
        }
    }

}