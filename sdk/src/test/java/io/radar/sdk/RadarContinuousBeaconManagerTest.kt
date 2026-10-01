package io.radar.sdk

import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Build
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.radar.sdk.helpers.RadarFakeBeaconScanner
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

    private lateinit var manager: RadarContinuousBeaconManager
    private lateinit var scanner: RadarFakeBeaconScanner
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

        scanner = RadarFakeBeaconScanner()
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

    // Runs the main looper for `ms`, advancing the manager's clock with it.
    private fun idle(ms: Long = 0) {
        clock += ms
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

    /** Continuously ranged beacons near `location`, or `null` if continuous ranging can't serve it. */
    private fun request(location: Location = location(LAT, LNG)): Array<RadarBeacon>? = manager.beacons(location)

    private fun appForeground(isForeground: Boolean) {
        foreground = isForeground
        if (isForeground) manager.onForeground() else manager.onBackground()
    }

    // endregion

    @Test
    fun beacons_notStarted_returnsNull() {
        assertNull(request())
        assertEquals(0, scanner.starts)
    }

    @Test
    fun beacons_beforeMinScan_returnsNullWithoutResetting() {
        startAndSearch(RadarContinuousBeaconManager.SearchResult(beacons = listOf(beacon("2"))))
        manager.handleRanged(listOf(beacon("2", rssi = -70)))

        idle(RadarContinuousBeaconManager.MIN_SCAN_MS - 1)
        assertNull(request())
        assertTrue(manager.ranging)
        assertEquals(1, scanner.starts)
        assertTrue(searches.isEmpty())

        idle(1)
        assertEquals(listOf(-70), request()!!.map { it.rssi })
    }

    @Test
    fun beacons_afterMinScanWithoutResults_returnsEmpty() {
        startAndSearch(RadarContinuousBeaconManager.SearchResult(beacons = listOf(beacon("2"))))
        idle(RadarContinuousBeaconManager.MIN_SCAN_MS)

        assertEquals(0, request()!!.size)
    }

    @Test
    fun beacons_returnsBeaconsUntilTheyExpire() {
        startAndSearch(RadarContinuousBeaconManager.SearchResult(beacons = listOf(beacon("2"))))
        idle(RadarContinuousBeaconManager.MIN_SCAN_MS)

        manager.handleRanged(listOf(beacon("2", rssi = -70)))
        assertEquals(listOf(-70), request()!!.map { it.rssi })

        clock += 1000
        manager.handleRanged(listOf(beacon("2", rssi = -50)))
        assertEquals(listOf(-50), request()!!.map { it.rssi })

        clock += RadarContinuousBeaconManager.MAX_BEACON_AGE_MS + 1
        assertEquals(0, request()!!.size)
    }

    @Test
    fun beacons_afterRefreshChangesBeacons_returnsNullUntilNewScanRuns() {
        startAndSearch(RadarContinuousBeaconManager.SearchResult(beacons = listOf(beacon("2"))))
        idle(RadarContinuousBeaconManager.MIN_SCAN_MS)
        assertNotNull(request())

        manager.update(RadarContinuousBeaconManager.SearchResult(beacons = listOf(beacon("3"))), location(LAT, LNG))
        assertNull(request())

        idle(RadarContinuousBeaconManager.MIN_SCAN_MS)
        assertNotNull(request())
    }

    @Test
    fun handleRanged_zeroRssi_isIgnored() {
        startAndSearch(RadarContinuousBeaconManager.SearchResult(beacons = listOf(beacon("2"))))
        manager.handleRanged(listOf(beacon("2", rssi = 0)))
        idle(RadarContinuousBeaconManager.MIN_SCAN_MS)

        assertEquals(0, request()!!.size)
    }

    @Test
    fun start_withoutPermissions_doesNotRange() {
        permissions.granted = false
        manager.start()

        assertTrue(searches.isEmpty())
        assertFalse(manager.ranging)
    }

    @Test
    fun beacons_withoutRanging_rangesCallersSearch() {
        lastLocation = null
        manager.start()
        assertTrue(searches.isEmpty())

        // The caller searches from `here` itself, so the manager doesn't search too.
        val here = location(LAT, LNG)
        assertNull(request(here))
        assertTrue(searches.isEmpty())

        manager.onSearched(here, RadarContinuousBeaconManager.SearchResult(beacons = listOf(beacon("2"))))
        assertTrue(manager.ranging)

        manager.handleRanged(listOf(beacon("2")))
        idle(RadarContinuousBeaconManager.MIN_SCAN_MS)

        assertEquals(1, request(here)!!.size)
    }

    @Test
    fun beacons_beyondMaxDistance_resetsAndRangesCallersSearch() {
        startAndSearch(RadarContinuousBeaconManager.SearchResult(beacons = listOf(beacon("2"))))
        idle(RadarContinuousBeaconManager.MIN_SCAN_MS)

        // About 150m away.
        val farAway = location(LAT + 0.00135, LNG)
        assertNull(request(farAway))
        assertFalse(manager.ranging)
        assertNull(manager.searchResult)
        assertTrue(searches.isEmpty())

        manager.onSearched(farAway, RadarContinuousBeaconManager.SearchResult(beacons = listOf(beacon("3", lat = LAT + 0.00135))))

        assertEquals(setOf("$UUID-1-3"), manager.searchResult!!.filterKeys)
        assertSame(farAway, manager.searchLocation)
        idle(RadarContinuousBeaconManager.MIN_SCAN_MS)
        assertNotNull(request(farAway))
    }

    @Test
    fun beacons_duringMinScan_keepsScanAndRangesCallersSearch() {
        startAndSearch(RadarContinuousBeaconManager.SearchResult(beacons = listOf(beacon("2"))))

        assertNull(request())
        assertTrue(manager.ranging)

        // The same beacons, so the scan keeps running and becomes ready on time.
        manager.onSearched(location(LAT, LNG), RadarContinuousBeaconManager.SearchResult(beacons = listOf(beacon("2"))))
        assertEquals(1, scanner.starts)
        idle(RadarContinuousBeaconManager.MIN_SCAN_MS)
        assertNotNull(request())
    }

    @Test
    fun startupSearch_returningAfterCallersSearch_isIgnored() {
        manager.start()
        val startup = searches.removeAt(0)

        assertNull(request())
        manager.onSearched(location(LAT, LNG), RadarContinuousBeaconManager.SearchResult(beacons = listOf(beacon("3"))))
        assertEquals(1, scanner.starts)

        startup.second(RadarContinuousBeaconManager.SearchResult(beacons = listOf(beacon("2"))))
        assertEquals(1, scanner.starts)
        assertEquals(setOf("$UUID-1-3"), manager.searchResult!!.filterKeys)
    }

    @Test
    fun onSearched_failed_schedulesRefresh() {
        manager.start()
        searches.clear()

        manager.onSearched(location(LAT, LNG), null)
        assertFalse(manager.ranging)

        idle(RadarContinuousBeaconManager.REFRESH_INTERVAL_MS)
        assertEquals(1, searches.size)
    }

    @Test
    fun onSearched_notStarted_isIgnored() {
        manager.onSearched(location(LAT, LNG), RadarContinuousBeaconManager.SearchResult(beacons = listOf(beacon("2"))))

        assertFalse(manager.ranging)
        assertNull(manager.searchResult)
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
    fun stop_stopsScanAndClearsResult() {
        startAndSearch(RadarContinuousBeaconManager.SearchResult(beacons = listOf(beacon("2"))))
        manager.handleRanged(listOf(beacon("2")))
        idle(RadarContinuousBeaconManager.MIN_SCAN_MS)

        manager.stop()

        assertFalse(manager.ranging)
        assertNull(scanner.active)
        assertNull(manager.searchResult)
        assertNull(request())
    }

    @Test
    fun background_pauses_foreground_resumesAndSearchesAgain() {
        startAndSearch(RadarContinuousBeaconManager.SearchResult(beacons = listOf(beacon("2"))))
        manager.handleRanged(listOf(beacon("2")))

        appForeground(false)
        idle(RadarContinuousBeaconManager.BACKGROUND_PAUSE_DELAY_MS)

        assertFalse(manager.ranging)
        assertNull(scanner.active)

        clock += RadarContinuousBeaconManager.REFRESH_INTERVAL_MS
        appForeground(true)

        assertTrue(manager.ranging)
        assertEquals(1, searches.size)
        // The scan restarted, so it isn't ready yet.
        assertNull(request())
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
    fun beacons_withinMaxDistance_isServed() {
        // Ten beacons, the farthest about 30m away, as in a dense venue.
        val beacons = (0 until RadarNearbyBeaconSearch.LIMIT).map { i ->
            beacon("$i", lat = LAT + 0.00003 * i)
        }
        startAndSearch(RadarContinuousBeaconManager.SearchResult(beacons = beacons))
        idle(RadarContinuousBeaconManager.MIN_SCAN_MS)

        assertNotNull(request(location(LAT, LNG)))
        // About 90m away.
        assertNotNull(request(location(LAT + 0.00081, LNG)))
    }

    @Test
    fun beacons_uuidSearch_beyondMaxDistance_returnsNull() {
        startAndSearch(RadarContinuousBeaconManager.SearchResult(uuids = listOf(UUID)))
        idle(RadarContinuousBeaconManager.MIN_SCAN_MS)

        // About 150m away.
        assertNull(request(location(LAT + 0.00135, LNG)))
        assertFalse(manager.ranging)
    }

    @Test
    fun refresh_afterInterval_searchesAgainFromLastLocation() {
        startAndSearch(RadarContinuousBeaconManager.SearchResult(beacons = listOf(beacon("2"))))
        lastLocation = location(LAT + 0.001, LNG)

        idle(RadarContinuousBeaconManager.REFRESH_INTERVAL_MS - 1)
        assertTrue(searches.isEmpty())
        idle(1)
        assertEquals(1, searches.size)
        assertEquals(LAT + 0.001, searches[0].first.latitude, 0.0)

        searches.removeAt(0).second(RadarContinuousBeaconManager.SearchResult(beacons = listOf(beacon("3"))))
        assertEquals(2, scanner.starts)
        assertEquals(setOf("$UUID-1-3"), manager.searchResult!!.filterKeys)
        assertEquals(LAT + 0.001, manager.searchLocation!!.latitude, 0.0)

        idle(RadarContinuousBeaconManager.REFRESH_INTERVAL_MS)
        assertEquals(1, searches.size)
    }

    @Test
    fun refresh_afterFailedSearch_retries() {
        manager.start()
        searches.removeAt(0).second(null)

        idle(RadarContinuousBeaconManager.REFRESH_INTERVAL_MS)
        assertEquals(1, searches.size)
    }

    @Test
    fun refresh_inBackgroundOrAfterStop_doesNotSearch() {
        startAndSearch(RadarContinuousBeaconManager.SearchResult(beacons = listOf(beacon("2"))))

        appForeground(false)
        idle(RadarContinuousBeaconManager.REFRESH_INTERVAL_MS)
        assertTrue(searches.isEmpty())

        // The last search is now stale, so returning to the foreground searches again.
        appForeground(true)
        assertEquals(1, searches.size)
        searches.clear()

        manager.stop()
        idle(RadarContinuousBeaconManager.REFRESH_INTERVAL_MS)
        assertTrue(searches.isEmpty())
    }

    @Test
    fun foreground_withinRefreshInterval_doesNotSearchAgain() {
        startAndSearch(RadarContinuousBeaconManager.SearchResult(beacons = listOf(beacon("2"))))

        appForeground(false)
        idle(RadarContinuousBeaconManager.BACKGROUND_PAUSE_DELAY_MS)
        assertFalse(manager.ranging)

        clock += 10_000L
        appForeground(true)
        assertTrue(manager.ranging)
        assertTrue(searches.isEmpty())

        // The refresh runs when the last search turns `REFRESH_INTERVAL_MS` old.
        idle(RadarContinuousBeaconManager.REFRESH_INTERVAL_MS - 10_000L)
        assertEquals(1, searches.size)
    }

    @Test
    fun beacons_bluetoothUnavailable_pausesAndReturnsFalse() {
        startAndSearch(RadarContinuousBeaconManager.SearchResult(beacons = listOf(beacon("2"))))
        idle(RadarContinuousBeaconManager.MIN_SCAN_MS)

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
        // The last search is less than `REFRESH_INTERVAL_MS` old, so it isn't repeated.
        assertTrue(searches.isEmpty())
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
        idle(RadarContinuousBeaconManager.MIN_SCAN_MS)

        scanner.availabilityError = SecurityException("Need android.permission.BLUETOOTH_CONNECT")

        assertNull(request())
        assertFalse(manager.ranging)
    }
}
