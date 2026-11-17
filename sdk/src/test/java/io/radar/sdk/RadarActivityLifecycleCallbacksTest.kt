package io.radar.sdk

import android.app.Activity
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE])
class RadarActivityLifecycleCallbacksTest {

    @Test
    fun wrapActivityWrapsWindowCallbackOnce() {
        val activity = Robolectric.buildActivity(Activity::class.java).create().get()
        val originalCallback = activity.window.callback

        RadarActivityLifecycleCallbacks(fraud = true).wrapActivity(activity)

        val wrapped = activity.window.callback
        assertTrue(wrapped is RadarActivityLifecycleCallbacks.RadarTouchWindowCallback)
        assertSame(originalCallback, (wrapped as RadarActivityLifecycleCallbacks.RadarTouchWindowCallback).original)
    }

    @Test
    fun wrapActivityFromNewCallbacksDoesNotAddSecondLayer() {
        val activity = Robolectric.buildActivity(Activity::class.java).create().get()
        val originalCallback = activity.window.callback

        // Each Radar.initialize() makes a new callbacks object.
        RadarActivityLifecycleCallbacks(fraud = true).wrapActivity(activity)
        val firstWrapper = activity.window.callback
        RadarActivityLifecycleCallbacks(fraud = true).wrapActivity(activity)

        assertSame(firstWrapper, activity.window.callback)
        val inner = (activity.window.callback as RadarActivityLifecycleCallbacks.RadarTouchWindowCallback).original
        assertFalse(inner is RadarActivityLifecycleCallbacks.RadarTouchWindowCallback)
        assertSame(originalCallback, inner)
    }
}
