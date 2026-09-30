package io.radar.sdk

import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.radar.sdk.model.RadarBeacon
import io.radar.sdk.model.RadarCoordinate
import java.time.Duration
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.P])
class RadarBeaconManagerContinuousRangingTest {

    companion object {
        private const val UUID = "2f234454-cf6d-4a0f-adf2-f4911ba9ffa6"
        private const val LAT = 40.78382
        private const val LNG = -73.97536
    }

    private class PermissionsHelper : RadarPermissionsHelper() {
        var granted = true
        override fun fineLocationPermissionGranted(context: Context) = granted
        override fun coarseLocationPermissionGranted(context: Context) = granted
        override fun bluetoothPermissionsGranted(context: Context) = granted
    }

    private class FakeScanner : RadarBeaconManager.Scanner {
        var available = true
        var availabilityError: Exception? = null
        var starts = 0
        var stops = 0
        var active: ScanCallback? = null

        override fun isAvailable(): Boolean {
            availabilityError?.let { throw it }
            return available
        }

        override fun start(filters: List<ScanFilter>, callback: ScanCallback) {
            starts++
            active = callback
        }

        override fun stop(callback: ScanCallback) {
            stops++
            if (active === callback) {
                active = null
            }
        }
    }

    /** A ranging request, and its result once it completes. */
    private class Request : Radar.RadarBeaconCallback {
        var completed = false
        var beacons: Array<RadarBeacon>? = null

        override fun onComplete(status: Radar.RadarStatus, beacons: Array<RadarBeacon>?) {
            completed = true
            this.beacons = beacons
        }
    }

    private lateinit var manager: RadarBeaconManager
    private lateinit var scanner: FakeScanner
    private lateinit var permissions: PermissionsHelper
    private var clock = 0L
    private var foreground = true
    private var lastLocation: Location? = null
    private val searches = mutableListOf<Pair<Location, (RadarBeaconManager.ContinuousSearchResult?) -> Unit>>()

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        Radar.initialize(context, "prj_test_pk_0000000000000000000000000000000000000000")
        idle()

        // So one-shot ranging gets past its Bluetooth checks.
        shadowOf(context.packageManager).setSystemFeature(PackageManager.FEATURE_BLUETOOTH, true)
        shadowOf(BluetoothAdapter.getDefaultAdapter()).setEnabled(true)

        scanner = FakeScanner()
        permissions = PermissionsHelper()
        clock = 100_000L
        foreground = true
        lastLocation = location(LAT, LNG)
        searches.clear()

        manager = RadarBeaconManager(context, Radar.logger, permissions)
        manager.scanner = scanner
        manager.now = { clock }
        manager.isForeground = { foreground }
        // A new `Location` on every call, like `RadarState.getLastLocation`.
        manager.lastLocation = { lastLocation?.let { Location(it) } }
        manager.searchBeacons = { location, completion -> searches.add(Pair(location, completion)) }
    }

    @After
    fun tearDown() {
        manager.stopContinuousRanging()
    }

    // region Helpers

    private fun idle(ms: Long = 0) {
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(ms))
    }

    private fun location(lat: Double, lng: Double): Location {
        val location = Location("test")
        location.latitude = lat
        location.longitude = lng
        return location
    }

    private fun beacon(minor: String, rssi: Int = -60, lat: Double = LAT, lng: Double = LNG) = RadarBeacon(
        uuid = UUID,
        major = "1",
        minor = minor,
        rssi = rssi,
        location = RadarCoordinate(lat, lng),
        type = RadarBeacon.RadarBeaconType.IBEACON
    )

    private fun startAndSearch(result: RadarBeaconManager.ContinuousSearchResult) {
        manager.startContinuousRanging()
        assertEquals(1, searches.size)
        searches.removeAt(0).second(result)
    }

    /** Requests continuously ranged beacons near `location`, or `null` if continuous ranging can't serve it. */
    private fun request(location: Location = location(LAT, LNG)): Request? {
        val request = Request()
        return if (manager.rangeContinuousBeacons(location, request)) request else null
    }

    /** Ranges beacons from a search near `location` with continuous ranging, or `null` if continuous ranging can't. */
    private fun rangeSearched(result: RadarBeaconManager.ContinuousSearchResult, location: Location = location(LAT, LNG)): Request? {
        val request = Request()
        return if (manager.rangeWithContinuousRanging(result, location, request)) request else null
    }

    private fun appForeground(isForeground: Boolean) {
        foreground = isForeground
        RadarActivityLifecycleCallbacks.foregroundListeners.forEach {
            if (isForeground) it.onForeground() else it.onBackground()
        }
    }

    // endregion

    @Test
    fun rangeContinuousBeacons_notStarted_returnsFalse() {
        assertNull(request())
        assertEquals(0, scanner.starts)
    }

    @Test
    fun rangeContinuousBeacons_duringFirstRound_waitsForItsResult() {
        startAndSearch(RadarBeaconManager.ContinuousSearchResult(beacons = listOf(beacon("2"))))

        val request = request()!!
        manager.handleContinuousRanged(listOf(beacon("2", rssi = -70)))
        assertFalse(request.completed)

        idle(RadarBeaconManager.WARM_UP_MS)

        assertTrue(request.completed)
        assertEquals(listOf(-70), request.beacons!!.map { it.rssi })
    }

    @Test
    fun rangeContinuousBeacons_duringFirstRound_includesBeaconsRangedEarlyInTheRound() {
        startAndSearch(RadarBeaconManager.ContinuousSearchResult(beacons = listOf(beacon("2"), beacon("3"))))

        val request = request()!!
        manager.handleContinuousRanged(listOf(beacon("2")))
        clock += RadarBeaconManager.MAX_BEACON_AGE_MS + 1
        manager.handleContinuousRanged(listOf(beacon("3")))

        idle(RadarBeaconManager.WARM_UP_MS - 1)
        assertFalse(request.completed)

        idle(1)
        assertEquals(listOf("2", "3"), request.beacons!!.map { it.minor }.sorted())
    }

    @Test
    fun rangeContinuousBeacons_afterFirstRound_completesWithLastResult() {
        startAndSearch(RadarBeaconManager.ContinuousSearchResult(beacons = listOf(beacon("2"))))
        manager.handleContinuousRanged(listOf(beacon("2")))
        idle(RadarBeaconManager.WARM_UP_MS)

        val request = request()!!

        assertTrue(request.completed)
        assertEquals(1, request.beacons!!.size)
    }

    @Test
    fun rangeContinuousBeacons_afterFirstRoundWithoutResults_completesEmpty() {
        startAndSearch(RadarBeaconManager.ContinuousSearchResult(beacons = listOf(beacon("2"))))
        idle(RadarBeaconManager.WARM_UP_MS)

        val request = request()!!

        assertTrue(request.completed)
        assertEquals(0, request.beacons!!.size)
    }

    @Test
    fun continuousBeacons_returnsBeaconsUntilTheyExpire() {
        startAndSearch(RadarBeaconManager.ContinuousSearchResult(beacons = listOf(beacon("2"))))
        assertNull(manager.continuousBeacons())

        manager.handleContinuousRanged(listOf(beacon("2", rssi = -70)))
        idle(RadarBeaconManager.WARM_UP_MS)
        assertEquals(listOf(-70), manager.continuousBeacons()!!.map { it.rssi })

        clock += 1000
        manager.handleContinuousRanged(listOf(beacon("2", rssi = -50)))
        assertEquals(listOf(-50), manager.continuousBeacons()!!.map { it.rssi })

        clock += RadarBeaconManager.MAX_BEACON_AGE_MS + 1
        assertEquals(0, manager.continuousBeacons()!!.size)
    }

    @Test
    fun handleContinuousRanged_zeroRssi_isIgnored() {
        startAndSearch(RadarBeaconManager.ContinuousSearchResult(beacons = listOf(beacon("2"))))
        manager.handleContinuousRanged(listOf(beacon("2", rssi = 0)))
        idle(RadarBeaconManager.WARM_UP_MS)

        assertEquals(0, manager.continuousBeacons()!!.size)
    }

    @Test
    fun startContinuousRanging_withoutPermissions_doesNotRange() {
        permissions.granted = false
        manager.startContinuousRanging()

        assertTrue(searches.isEmpty())
        assertFalse(manager.continuousRanging)
    }

    @Test
    fun rangeWithContinuousRanging_withoutLocationToSearch_startsContinuousRangingAndWaits() {
        lastLocation = null
        manager.startContinuousRanging()

        assertTrue(searches.isEmpty())
        assertFalse(manager.continuousRanging)
        assertNull(request())

        val request = rangeSearched(RadarBeaconManager.ContinuousSearchResult(beacons = listOf(beacon("2"))))!!

        assertTrue(manager.continuousRanging)
        assertFalse(request.completed)

        manager.handleContinuousRanged(listOf(beacon("2")))
        idle(RadarBeaconManager.WARM_UP_MS)

        assertTrue(request.completed)
        assertEquals(1, request.beacons!!.size)
    }

    @Test
    fun rangeWithContinuousRanging_differentBeacons_switchesAndWaitsForTheNewScan() {
        startAndSearch(RadarBeaconManager.ContinuousSearchResult(beacons = listOf(beacon("2"))))
        manager.handleContinuousRanged(listOf(beacon("2")))
        val waiting = request()!!

        val request = rangeSearched(RadarBeaconManager.ContinuousSearchResult(uuids = listOf(UUID)))!!

        assertEquals(2, scanner.starts)
        assertEquals(setOf("uuid:$UUID"), manager.continuousSearchResult!!.filterKeys)
        assertFalse(waiting.completed)
        assertFalse(request.completed)

        manager.handleContinuousRanged(listOf(beacon("3")))
        idle(RadarBeaconManager.WARM_UP_MS)

        assertEquals(listOf("3"), request.beacons!!.map { it.minor })
        assertEquals(listOf("3"), waiting.beacons!!.map { it.minor })
    }

    @Test
    fun rangeWithContinuousRanging_sameBeaconsAfterFirstRound_completesWithoutRestartingScan() {
        startAndSearch(RadarBeaconManager.ContinuousSearchResult(beacons = listOf(beacon("2"))))
        manager.handleContinuousRanged(listOf(beacon("2")))
        idle(RadarBeaconManager.WARM_UP_MS)

        val request = rangeSearched(RadarBeaconManager.ContinuousSearchResult(beacons = listOf(beacon("2"))))!!

        assertEquals(1, scanner.starts)
        assertTrue(request.completed)
        assertEquals(1, request.beacons!!.size)
    }

    @Test
    fun rangeBeacons_doesNotChangeContinuousRanging() {
        startAndSearch(RadarBeaconManager.ContinuousSearchResult(beacons = listOf(beacon("2"))))

        manager.rangeBeacons(arrayOf(beacon("3")), false, Request())

        assertEquals(1, scanner.starts)
        assertEquals(setOf("$UUID-1-2"), manager.continuousSearchResult!!.filterKeys)
    }

    @Test
    fun rangeWithContinuousRanging_continuousRangingOff_returnsFalse() {
        assertNull(rangeSearched(RadarBeaconManager.ContinuousSearchResult(beacons = listOf(beacon("2")))))

        assertEquals(0, scanner.starts)
        assertNull(manager.continuousSearchResult)
    }

    @Test
    fun rangeWithContinuousRanging_searchedFarAway_coversOnlyAroundWhereItWasSearched() {
        lastLocation = null
        manager.startContinuousRanging()

        // Searched about 2km away from where `trackVerified` is later called.
        rangeSearched(RadarBeaconManager.ContinuousSearchResult(beacons = listOf(beacon("2", lat = LAT + 0.018))), location(LAT + 0.018, LNG))
        idle(RadarBeaconManager.WARM_UP_MS)

        assertEquals(LAT + 0.018, manager.continuousSearchLocation!!.latitude, 0.0)
        assertNull(request(location(LAT, LNG)))
        assertNull(manager.continuousSearchResult)
    }

    @Test
    fun updateContinuousRanging_sameBeacons_doesNotRestartScan() {
        startAndSearch(RadarBeaconManager.ContinuousSearchResult(beacons = listOf(beacon("2"))))
        manager.updateContinuousRanging(RadarBeaconManager.ContinuousSearchResult(beacons = listOf(beacon("2"))))

        assertEquals(1, scanner.starts)

        manager.updateContinuousRanging(RadarBeaconManager.ContinuousSearchResult(beacons = listOf(beacon("3"))))
        assertEquals(2, scanner.starts)
    }

    @Test
    fun search_failed_doesNotRange() {
        manager.startContinuousRanging()
        searches.removeAt(0).second(null)

        assertFalse(manager.continuousRanging)
        assertNull(request())
    }

    @Test
    fun stopContinuousRanging_invalidatesRunningSearch() {
        manager.startContinuousRanging()
        val pending = searches.removeAt(0)
        manager.stopContinuousRanging()
        manager.startContinuousRanging()
        pending.second(RadarBeaconManager.ContinuousSearchResult(beacons = listOf(beacon("2"))))

        assertFalse(manager.continuousRanging)

        // The newer search still applies.
        searches.removeAt(0).second(RadarBeaconManager.ContinuousSearchResult(beacons = listOf(beacon("3"))))

        assertTrue(manager.continuousRanging)
        assertEquals(setOf("$UUID-1-3"), manager.continuousSearchResult!!.filterKeys)
    }

    @Test
    fun startupSearch_returningAfterNewerSearch_isIgnored() {
        manager.startContinuousRanging()
        val startup = searches.removeAt(0)

        rangeSearched(RadarBeaconManager.ContinuousSearchResult(beacons = listOf(beacon("3"))))
        startup.second(RadarBeaconManager.ContinuousSearchResult(beacons = listOf(beacon("2"))))

        assertEquals(1, scanner.starts)
        assertEquals(setOf("$UUID-1-3"), manager.continuousSearchResult!!.filterKeys)
    }

    @Test
    fun search_returnsWhileBluetoothOff_startsWhenBluetoothTurnsOn() {
        manager.startContinuousRanging()
        scanner.available = false
        searches.removeAt(0).second(RadarBeaconManager.ContinuousSearchResult(beacons = listOf(beacon("2"))))

        assertFalse(manager.continuousRanging)
        assertEquals(0, scanner.starts)

        scanner.available = true
        manager.onBluetoothStateChanged(true)

        assertTrue(manager.continuousRanging)
        assertEquals(1, scanner.starts)
    }

    @Test
    fun scanFilters_invalidIdentifier_keepsTheOthers() {
        val result = RadarBeaconManager.ContinuousSearchResult(uuids = listOf("not-a-uuid", UUID))

        assertEquals(1, result.scanFilters(Radar.logger).size)
    }

    @Test
    fun stopContinuousRanging_stopsScanClearsResultAndCompletesWaitingRequests() {
        startAndSearch(RadarBeaconManager.ContinuousSearchResult(beacons = listOf(beacon("2"))))
        manager.handleContinuousRanged(listOf(beacon("2")))
        val waiting = request()!!

        manager.stopContinuousRanging()

        assertTrue(waiting.completed)
        assertEquals(1, waiting.beacons!!.size)
        assertFalse(manager.continuousRanging)
        assertNull(scanner.active)
        assertNull(manager.continuousBeacons())
        assertNull(request())
    }

    @Test
    fun background_pausesAndCompletesWaitingRequests_foreground_resumesAndSearchesAgain() {
        startAndSearch(RadarBeaconManager.ContinuousSearchResult(beacons = listOf(beacon("2"))))
        manager.handleContinuousRanged(listOf(beacon("2")))
        val waiting = request()!!

        appForeground(false)
        idle(RadarBeaconManager.BACKGROUND_PAUSE_DELAY_MS)

        assertFalse(manager.continuousRanging)
        assertNull(scanner.active)
        assertTrue(waiting.completed)

        appForeground(true)

        assertTrue(manager.continuousRanging)
        assertEquals(1, searches.size)
        assertNull(manager.continuousBeacons())
    }

    @Test
    fun background_quickReturnToForeground_keepsScanning() {
        startAndSearch(RadarBeaconManager.ContinuousSearchResult(beacons = listOf(beacon("2"))))

        appForeground(false)
        appForeground(true)
        idle(RadarBeaconManager.BACKGROUND_PAUSE_DELAY_MS)

        assertTrue(manager.continuousRanging)
        assertEquals(1, scanner.starts)
    }

    @Test
    fun stopContinuousRanging_removesForegroundListener() {
        manager.startContinuousRanging()
        manager.stopContinuousRanging()

        appForeground(false)
        appForeground(true)

        assertFalse(manager.continuousRanging)
        assertEquals(1, searches.size)
    }

    @Test
    fun rangeContinuousBeacons_uuidSearch_coversAnyLocation() {
        startAndSearch(RadarBeaconManager.ContinuousSearchResult(uuids = listOf(UUID)))
        idle(RadarBeaconManager.WARM_UP_MS)

        assertNotNull(request(location(LAT + 1, LNG)))
    }

    @Test
    fun rangeContinuousBeacons_partialSearch_coversSearchRadius() {
        startAndSearch(RadarBeaconManager.ContinuousSearchResult(beacons = listOf(beacon("2"))))
        idle(RadarBeaconManager.WARM_UP_MS)

        // About 500m away.
        assertNotNull(request(location(LAT + 0.0045, LNG)))
        // About 1000m away.
        assertNull(request(location(LAT + 0.009, LNG)))
        assertFalse(manager.continuousRanging)
        assertNull(manager.continuousSearchResult)
    }

    @Test
    fun rangeContinuousBeacons_fullSearch_coversFarthestBeacon() {
        // Ten beacons, the farthest about 300m away.
        val beacons = (0 until RadarBeaconManager.SEARCH_LIMIT).map { i ->
            beacon("$i", lat = LAT + 0.0003 * i)
        }
        startAndSearch(RadarBeaconManager.ContinuousSearchResult(beacons = beacons))
        idle(RadarBeaconManager.WARM_UP_MS)

        // About 100m away: within 300m - 100m.
        assertNotNull(request(location(LAT + 0.0009, LNG)))
        // About 250m away: beacons within 100m of it may not have been included.
        assertNull(request(location(LAT + 0.00225, LNG)))
    }

    @Test
    fun rangeContinuousBeacons_bluetoothUnavailable_pausesAndReturnsFalse() {
        startAndSearch(RadarBeaconManager.ContinuousSearchResult(beacons = listOf(beacon("2"))))
        idle(RadarBeaconManager.WARM_UP_MS)

        scanner.available = false

        assertNull(request())
        assertFalse(manager.continuousRanging)
    }

    @Test
    fun bluetoothStateBroadcast_pausesWhenOffAndResumesWhenOn() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        startAndSearch(RadarBeaconManager.ContinuousSearchResult(beacons = listOf(beacon("2"))))
        manager.handleContinuousRanged(listOf(beacon("2")))

        scanner.available = false
        context.sendBroadcast(Intent(BluetoothAdapter.ACTION_STATE_CHANGED).putExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_OFF))
        idle()

        assertFalse(manager.continuousRanging)
        assertNull(scanner.active)

        scanner.available = true
        context.sendBroadcast(Intent(BluetoothAdapter.ACTION_STATE_CHANGED).putExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_ON))
        idle()

        assertTrue(manager.continuousRanging)
        assertEquals(2, scanner.starts)
        assertEquals(1, searches.size)
    }

    @Test
    fun bluetoothStateBroadcast_afterStop_isIgnored() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        manager.startContinuousRanging()
        manager.stopContinuousRanging()

        context.sendBroadcast(Intent(BluetoothAdapter.ACTION_STATE_CHANGED).putExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_ON))
        idle()

        assertFalse(manager.continuousRanging)
        assertEquals(1, searches.size)
    }

    @Test
    fun bluetoothAvailabilityError_isTreatedAsUnavailable() {
        scanner.availabilityError = SecurityException("Need android.permission.BLUETOOTH_CONNECT")
        manager.startContinuousRanging()

        assertFalse(manager.continuousRanging)
        assertTrue(searches.isEmpty())

        scanner.availabilityError = null
        manager.onBluetoothStateChanged(true)
        searches.removeAt(0).second(RadarBeaconManager.ContinuousSearchResult(beacons = listOf(beacon("2"))))
        idle(RadarBeaconManager.WARM_UP_MS)

        scanner.availabilityError = SecurityException("Need android.permission.BLUETOOTH_CONNECT")

        assertNull(request())
        assertFalse(manager.continuousRanging)
    }
}
