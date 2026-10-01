package io.radar.sdk

import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.content.Context
import android.content.Intent
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
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.P])
class RadarContinuousBeaconManagerTest {

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

    private class FakeScanner : RadarContinuousBeaconManager.Scanner {
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

    private lateinit var manager: RadarContinuousBeaconManager
    private lateinit var scanner: FakeScanner
    private lateinit var permissions: PermissionsHelper
    private var clock = 0L
    private var foreground = true
    private var lastLocation: Location? = null
    private val searches = mutableListOf<Pair<Location, (RadarContinuousBeaconManager.SearchResult?) -> Unit>>()

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        Radar.initialize(context, "prj_test_pk_0000000000000000000000000000000000000000")
        idle()

        scanner = FakeScanner()
        permissions = PermissionsHelper()
        clock = 100_000L
        foreground = true
        lastLocation = location(LAT, LNG)
        searches.clear()

        manager = RadarContinuousBeaconManager(context, Radar.logger, permissions)
        manager.scanner = scanner
        manager.now = { clock }
        manager.isForeground = { foreground }
        // A new `Location` on every call, like `RadarState.getLastLocation`.
        manager.lastLocation = { lastLocation?.let { Location(it) } }
        manager.searchBeacons = { location, completion -> searches.add(Pair(location, completion)) }
    }

    @After
    fun tearDown() {
        manager.stop()
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

    private fun startAndSearch(result: RadarContinuousBeaconManager.SearchResult) {
        manager.start()
        assertEquals(1, searches.size)
        searches.removeAt(0).second(result)
    }

    /** Requests continuously ranged beacons near `location`, or `null` if continuous ranging can't serve it. */
    private fun request(location: Location = location(LAT, LNG)): Request? {
        val request = Request()
        return if (manager.rangeBeacons(location, request)) request else null
    }

    private fun appForeground(isForeground: Boolean) {
        foreground = isForeground
        if (isForeground) manager.onForeground() else manager.onBackground()
    }

    // endregion

    @Test
    fun rangeBeacons_notStarted_returnsFalse() {
        assertNull(request())
        assertEquals(0, scanner.starts)
    }

    @Test
    fun rangeBeacons_duringFirstRound_waitsForItsResult() {
        startAndSearch(RadarContinuousBeaconManager.SearchResult(beacons = listOf(beacon("2"))))

        val request = request()!!
        manager.handleRanged(listOf(beacon("2", rssi = -70)))
        assertFalse(request.completed)

        idle(RadarContinuousBeaconManager.WARM_UP_MS)

        assertTrue(request.completed)
        assertEquals(listOf(-70), request.beacons!!.map { it.rssi })
    }

    @Test
    fun rangeBeacons_duringFirstRound_includesBeaconsRangedEarlyInTheRound() {
        startAndSearch(RadarContinuousBeaconManager.SearchResult(beacons = listOf(beacon("2"), beacon("3"))))

        val request = request()!!
        manager.handleRanged(listOf(beacon("2")))
        clock += RadarContinuousBeaconManager.MAX_BEACON_AGE_MS + 1
        manager.handleRanged(listOf(beacon("3")))

        idle(RadarContinuousBeaconManager.WARM_UP_MS - 1)
        assertFalse(request.completed)

        idle(1)
        assertEquals(listOf("2", "3"), request.beacons!!.map { it.minor }.sorted())
    }

    @Test
    fun rangeBeacons_afterFirstRound_completesWithLastResult() {
        startAndSearch(RadarContinuousBeaconManager.SearchResult(beacons = listOf(beacon("2"))))
        manager.handleRanged(listOf(beacon("2")))
        idle(RadarContinuousBeaconManager.WARM_UP_MS)

        val request = request()!!

        assertTrue(request.completed)
        assertEquals(1, request.beacons!!.size)
    }

    @Test
    fun rangeBeacons_afterFirstRoundWithoutResults_completesEmpty() {
        startAndSearch(RadarContinuousBeaconManager.SearchResult(beacons = listOf(beacon("2"))))
        idle(RadarContinuousBeaconManager.WARM_UP_MS)

        val request = request()!!

        assertTrue(request.completed)
        assertEquals(0, request.beacons!!.size)
    }

    @Test
    fun beacons_returnsBeaconsUntilTheyExpire() {
        startAndSearch(RadarContinuousBeaconManager.SearchResult(beacons = listOf(beacon("2"))))
        assertNull(manager.beacons())

        manager.handleRanged(listOf(beacon("2", rssi = -70)))
        idle(RadarContinuousBeaconManager.WARM_UP_MS)
        assertEquals(listOf(-70), manager.beacons()!!.map { it.rssi })

        clock += 1000
        manager.handleRanged(listOf(beacon("2", rssi = -50)))
        assertEquals(listOf(-50), manager.beacons()!!.map { it.rssi })

        clock += RadarContinuousBeaconManager.MAX_BEACON_AGE_MS + 1
        assertEquals(0, manager.beacons()!!.size)
    }

    @Test
    fun handleRanged_zeroRssi_isIgnored() {
        startAndSearch(RadarContinuousBeaconManager.SearchResult(beacons = listOf(beacon("2"))))
        manager.handleRanged(listOf(beacon("2", rssi = 0)))
        idle(RadarContinuousBeaconManager.WARM_UP_MS)

        assertEquals(0, manager.beacons()!!.size)
    }

    @Test
    fun start_withoutPermissions_doesNotRange() {
        permissions.granted = false
        manager.start()

        assertTrue(searches.isEmpty())
        assertFalse(manager.ranging)
    }

    @Test
    fun rangeBeacons_withoutRanging_searchesFromRequestLocation() {
        lastLocation = null
        manager.start()
        assertTrue(searches.isEmpty())

        val here = location(LAT, LNG)
        assertNull(request(here))
        assertEquals(1, searches.size)
        assertSame(here, searches[0].first)

        searches.removeAt(0).second(RadarContinuousBeaconManager.SearchResult(beacons = listOf(beacon("2"))))
        assertTrue(manager.ranging)

        val request = request(here)!!
        manager.handleRanged(listOf(beacon("2")))
        idle(RadarContinuousBeaconManager.WARM_UP_MS)

        assertTrue(request.completed)
        assertEquals(1, request.beacons!!.size)
    }

    @Test
    fun rangeBeacons_outsideCoverage_searchesFromRequestLocation() {
        startAndSearch(RadarContinuousBeaconManager.SearchResult(beacons = listOf(beacon("2"))))
        idle(RadarContinuousBeaconManager.WARM_UP_MS)

        // About 1000m away.
        val farAway = location(LAT + 0.009, LNG)
        assertNull(request(farAway))
        assertFalse(manager.ranging)
        assertNull(manager.searchResult)
        assertEquals(1, searches.size)
        assertSame(farAway, searches[0].first)

        searches.removeAt(0).second(RadarContinuousBeaconManager.SearchResult(beacons = listOf(beacon("3", lat = LAT + 0.009))))

        assertEquals(setOf("$UUID-1-3"), manager.searchResult!!.filterKeys)
        assertEquals(LAT + 0.009, manager.searchLocation!!.latitude, 0.0)
        assertNotNull(request(farAway))
    }

    @Test
    fun startupSearch_returningAfterMissSearch_isIgnored() {
        manager.start()
        val startup = searches.removeAt(0)

        assertNull(request())
        assertEquals(1, searches.size)

        startup.second(RadarContinuousBeaconManager.SearchResult(beacons = listOf(beacon("2"))))
        assertFalse(manager.ranging)
        assertNull(manager.searchResult)

        searches.removeAt(0).second(RadarContinuousBeaconManager.SearchResult(beacons = listOf(beacon("3"))))
        assertEquals(1, scanner.starts)
        assertEquals(setOf("$UUID-1-3"), manager.searchResult!!.filterKeys)
    }

    @Test
    fun update_sameBeacons_doesNotRestartScan() {
        startAndSearch(RadarContinuousBeaconManager.SearchResult(beacons = listOf(beacon("2"))))
        manager.update(RadarContinuousBeaconManager.SearchResult(beacons = listOf(beacon("2"))), location(LAT, LNG))

        assertEquals(1, scanner.starts)

        manager.update(RadarContinuousBeaconManager.SearchResult(beacons = listOf(beacon("3"))), location(LAT, LNG))
        assertEquals(2, scanner.starts)
    }

    @Test
    fun search_failed_doesNotRange() {
        manager.start()
        searches.removeAt(0).second(null)

        assertFalse(manager.ranging)
        assertNull(request())
    }

    @Test
    fun stop_invalidatesRunningSearch() {
        manager.start()
        val pending = searches.removeAt(0)
        manager.stop()
        manager.start()
        pending.second(RadarContinuousBeaconManager.SearchResult(beacons = listOf(beacon("2"))))

        assertFalse(manager.ranging)

        // The newer search still applies.
        searches.removeAt(0).second(RadarContinuousBeaconManager.SearchResult(beacons = listOf(beacon("3"))))

        assertTrue(manager.ranging)
        assertEquals(setOf("$UUID-1-3"), manager.searchResult!!.filterKeys)
    }

    @Test
    fun search_returnsWhileBluetoothOff_startsWhenBluetoothTurnsOn() {
        manager.start()
        scanner.available = false
        searches.removeAt(0).second(RadarContinuousBeaconManager.SearchResult(beacons = listOf(beacon("2"))))

        assertFalse(manager.ranging)
        assertEquals(0, scanner.starts)

        scanner.available = true
        manager.onBluetoothStateChanged(true)

        assertTrue(manager.ranging)
        assertEquals(1, scanner.starts)
    }

    @Test
    fun scanFilters_invalidIdentifier_keepsTheOthers() {
        val result = RadarContinuousBeaconManager.SearchResult(uuids = listOf("not-a-uuid", UUID))

        assertEquals(1, result.scanFilters(Radar.logger).size)
    }

    @Test
    fun stop_stopsScanClearsResultAndCompletesWaitingRequests() {
        startAndSearch(RadarContinuousBeaconManager.SearchResult(beacons = listOf(beacon("2"))))
        manager.handleRanged(listOf(beacon("2")))
        val waiting = request()!!

        manager.stop()

        assertTrue(waiting.completed)
        assertEquals(1, waiting.beacons!!.size)
        assertFalse(manager.ranging)
        assertNull(scanner.active)
        assertNull(manager.beacons())
        assertNull(request())
    }

    @Test
    fun background_pausesAndCompletesWaitingRequests_foreground_resumesAndSearchesAgain() {
        startAndSearch(RadarContinuousBeaconManager.SearchResult(beacons = listOf(beacon("2"))))
        manager.handleRanged(listOf(beacon("2")))
        val waiting = request()!!

        appForeground(false)
        idle(RadarContinuousBeaconManager.BACKGROUND_PAUSE_DELAY_MS)

        assertFalse(manager.ranging)
        assertNull(scanner.active)
        assertTrue(waiting.completed)

        appForeground(true)

        assertTrue(manager.ranging)
        assertEquals(1, searches.size)
        assertNull(manager.beacons())
    }

    @Test
    fun background_quickReturnToForeground_keepsScanning() {
        startAndSearch(RadarContinuousBeaconManager.SearchResult(beacons = listOf(beacon("2"))))

        appForeground(false)
        appForeground(true)
        idle(RadarContinuousBeaconManager.BACKGROUND_PAUSE_DELAY_MS)

        assertTrue(manager.ranging)
        assertEquals(1, scanner.starts)
    }

    @Test
    fun stop_ignoresForegroundTransitions() {
        manager.start()
        manager.stop()

        appForeground(false)
        appForeground(true)

        assertFalse(manager.ranging)
        assertEquals(1, searches.size)
    }

    @Test
    fun rangeBeacons_uuidSearch_coversAnyLocation() {
        startAndSearch(RadarContinuousBeaconManager.SearchResult(uuids = listOf(UUID)))
        idle(RadarContinuousBeaconManager.WARM_UP_MS)

        assertNotNull(request(location(LAT + 1, LNG)))
    }

    @Test
    fun rangeBeacons_partialSearch_coversSearchRadius() {
        startAndSearch(RadarContinuousBeaconManager.SearchResult(beacons = listOf(beacon("2"))))
        idle(RadarContinuousBeaconManager.WARM_UP_MS)

        // About 500m away.
        assertNotNull(request(location(LAT + 0.0045, LNG)))
        // About 1000m away.
        assertNull(request(location(LAT + 0.009, LNG)))
        assertFalse(manager.ranging)
        assertNull(manager.searchResult)
    }

    @Test
    fun rangeBeacons_fullSearch_coversFarthestBeacon() {
        // Ten beacons, the farthest about 300m away.
        val beacons = (0 until RadarContinuousBeaconManager.SEARCH_LIMIT).map { i ->
            beacon("$i", lat = LAT + 0.0003 * i)
        }
        startAndSearch(RadarContinuousBeaconManager.SearchResult(beacons = beacons))
        idle(RadarContinuousBeaconManager.WARM_UP_MS)

        // About 100m away: within 300m - 100m.
        assertNotNull(request(location(LAT + 0.0009, LNG)))
        // About 250m away: beacons within 100m of it may not have been included.
        assertNull(request(location(LAT + 0.00225, LNG)))
    }

    @Test
    fun rangeBeacons_bluetoothUnavailable_pausesAndReturnsFalse() {
        startAndSearch(RadarContinuousBeaconManager.SearchResult(beacons = listOf(beacon("2"))))
        idle(RadarContinuousBeaconManager.WARM_UP_MS)

        scanner.available = false

        assertNull(request())
        assertFalse(manager.ranging)
    }

    @Test
    fun bluetoothStateBroadcast_pausesWhenOffAndResumesWhenOn() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        startAndSearch(RadarContinuousBeaconManager.SearchResult(beacons = listOf(beacon("2"))))
        manager.handleRanged(listOf(beacon("2")))

        scanner.available = false
        context.sendBroadcast(Intent(BluetoothAdapter.ACTION_STATE_CHANGED).putExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_OFF))
        idle()

        assertFalse(manager.ranging)
        assertNull(scanner.active)

        scanner.available = true
        context.sendBroadcast(Intent(BluetoothAdapter.ACTION_STATE_CHANGED).putExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_ON))
        idle()

        assertTrue(manager.ranging)
        assertEquals(2, scanner.starts)
        assertEquals(1, searches.size)
    }

    @Test
    fun bluetoothStateBroadcast_afterStop_isIgnored() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        manager.start()
        manager.stop()

        context.sendBroadcast(Intent(BluetoothAdapter.ACTION_STATE_CHANGED).putExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_ON))
        idle()

        assertFalse(manager.ranging)
        assertEquals(1, searches.size)
    }

    @Test
    fun bluetoothAvailabilityError_isTreatedAsUnavailable() {
        scanner.availabilityError = SecurityException("Need android.permission.BLUETOOTH_CONNECT")
        manager.start()

        assertFalse(manager.ranging)
        assertTrue(searches.isEmpty())

        scanner.availabilityError = null
        manager.onBluetoothStateChanged(true)
        searches.removeAt(0).second(RadarContinuousBeaconManager.SearchResult(beacons = listOf(beacon("2"))))
        idle(RadarContinuousBeaconManager.WARM_UP_MS)

        scanner.availabilityError = SecurityException("Need android.permission.BLUETOOTH_CONNECT")

        assertNull(request())
        assertFalse(manager.ranging)
    }
}
