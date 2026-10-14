package io.radar.sdk

import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.P])
class RadarForegroundServiceTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun isDeclaredReturnsFalseWhenAppDoesNotDeclareService() {
        assertFalse(RadarForegroundService.isDeclared(context))
    }

    @Test
    fun isDeclaredReturnsTrueWhenAppDeclaresService() {
        shadowOf(context.packageManager).addOrUpdateService(
            ServiceInfo().apply {
                packageName = context.packageName
                name = RadarForegroundService::class.java.name
            }
        )

        assertTrue(RadarForegroundService.isDeclared(context))
    }
}
