package io.radar.sdk

import android.content.Context
import android.content.Intent
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.radar.sdk.util.BatteryState
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowPowerManager
import java.util.Random
import kotlin.collections.ArrayList

/**
 * Tests [RadarBatteryManager]. This utilized sticky broadcasts in order to simulate the documented
 * Android mechanism for retrieving battery information.
 *
 * @see : https://developer.android.com/training/monitoring-device-state/battery-monitoring
 */
@RunWith(Enclosed::class)
class RadarBatteryManagerTests {

    @RunWith(AndroidJUnit4::class)
    @Config(sdk = [Build.VERSION_CODES.P])
    class RadarBatteryManagerTest {
        companion object {
            private const val SCALE: Int = 100
        }

        private val batteryManager: RadarBatteryManager = RadarBatteryManager(getApplicationContext())

        private val intent: Intent = Intent(Intent.ACTION_BATTERY_CHANGED)
        private val charging: Boolean = Random().nextBoolean()
        private val battery: Int = Random().nextInt(SCALE)

        @Before
        fun setup() {
            if (charging) {
                intent.putExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_CHARGING)
            } else {
                intent.putExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_NOT_CHARGING)
            }
            intent.putExtra(BatteryManager.EXTRA_LEVEL, battery)
            intent.putExtra(BatteryManager.EXTRA_SCALE, SCALE)
            val context: Context = getApplicationContext()
            context.sendStickyBroadcast(intent)
        }

        @After
        fun teardown() {
            val context: Context = getApplicationContext()
            context.removeStickyBroadcast(intent)
        }

        @Test
        fun testGetBatteryState() {
            val batteryState = batteryManager.getBatteryState()
            assertEquals(battery.toFloat(), batteryState.percent)
            assertEquals(charging, batteryState.isCharging)
        }

        @Test
        fun testGetAppStandbyBucket() {
            assertNotNull(batteryManager.getAppStandbyBucket())
        }
    }

    @RunWith(ParameterizedRobolectricTestRunner::class)
    @Config(sdk = [Build.VERSION_CODES.P], shadows = [ShadowPowerManager::class])
    internal class RadarBatteryManagerParameterizedTest(
        private val powerSaveMode: Boolean,
        private val locationPowerSaveMode: Int,
        private val idleMode: Boolean,
        private val ignoreBatteryOptimizations: Boolean
    ) {

        companion object {

            private val powerSaveModes = arrayOf(true, false)

            private val locationPowerSaveModes = arrayOf(
                PowerManager.LOCATION_MODE_ALL_DISABLED_WHEN_SCREEN_OFF,
                PowerManager.LOCATION_MODE_FOREGROUND_ONLY,
                PowerManager.LOCATION_MODE_GPS_DISABLED_WHEN_SCREEN_OFF,
                PowerManager.LOCATION_MODE_NO_CHANGE,
                PowerManager.LOCATION_MODE_THROTTLE_REQUESTS_WHEN_SCREEN_OFF
            )

            // Doze mode
            private val idleModes = arrayOf(true, false)

            private val ignoreBatteryOptimizations = arrayOf(true, false)

            @JvmStatic
            @ParameterizedRobolectricTestRunner.Parameters(
                name = """{index}:
                Power Save Mode: {0},
                Location Power Save Mode: {1},
                Idle Mode: {2},
                Is Ignore Battery Optimization: {3}
            """
            )
            fun data(): Collection<Any> {
                // This many for-loops is not ideal, however Robolectric does not easily support JUnit Theories, so this
                // is the best option
                val data = ArrayList<Any>(
                    powerSaveModes.size * locationPowerSaveModes.size
                            * idleModes.size * ignoreBatteryOptimizations.size
                )
                for (powerSaveMode in powerSaveModes) {
                    for (locationPowerSaveMode in locationPowerSaveModes) {
                        for (idleMode in idleModes) {
                            for (ignoreBatteryOptimization in ignoreBatteryOptimizations) {
                                data.add(
                                    arrayOf(
                                        powerSaveMode,
                                        locationPowerSaveMode,
                                        idleMode,
                                        ignoreBatteryOptimization
                                    )
                                )
                            }
                        }
                    }
                }
                return data
            }

        }

        @Test
        fun testPowerMode() {
            val context: Context = getApplicationContext()
            val powerManager = Shadows.shadowOf(context.getSystemService(Context.POWER_SERVICE) as PowerManager)
            val batteryManager = RadarBatteryManager(context)
            powerManager.setIsPowerSaveMode(powerSaveMode)
            powerManager.setLocationPowerSaveMode(locationPowerSaveMode)
            powerManager.setIsDeviceIdleMode(idleMode)
            powerManager.setIgnoringBatteryOptimizations(context.packageName, ignoreBatteryOptimizations)
            val performanceState = batteryManager.getBatteryState().performanceState
            if (powerSaveMode) {
                if (idleMode) {
                    if (locationPowerSaveMode == PowerManager.LOCATION_MODE_NO_CHANGE && !ignoreBatteryOptimizations) {
                        assertEquals(BatteryState.PerformanceState.LOW, performanceState)
                    } else if (ignoreBatteryOptimizations) {
                        assertEquals(BatteryState.PerformanceState.IDLE, performanceState)
                    } else {
                        assertEquals(BatteryState.PerformanceState.LOWEST, performanceState)
                    }
                } else if (locationPowerSaveMode == PowerManager.LOCATION_MODE_NO_CHANGE
                    && !ignoreBatteryOptimizations
                ) {
                    assertEquals(BatteryState.PerformanceState.OPTIMIZED, performanceState)
                } else if (ignoreBatteryOptimizations) {
                    assertEquals(BatteryState.PerformanceState.OK, performanceState)
                } else {
                    assertEquals(BatteryState.PerformanceState.LOCATIONS_LOW_PERFORMANCE, performanceState)
                }
            } else if (idleMode) {
                assertEquals(BatteryState.PerformanceState.IDLE, performanceState)
            } else {
                assertEquals(BatteryState.PerformanceState.OK, performanceState)
            }
        }
    }
}
