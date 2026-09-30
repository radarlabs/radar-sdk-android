package io.radar.sdk

import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.content.Context
import android.location.Location
import android.os.Build
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.radar.sdk.model.RadarBeacon
import io.radar.sdk.model.RadarCoordinate
import java.time.Duration
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
class RadarBeaconRangingCacheTest {

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

    private class FakeScanner : RadarBeaconRangingCache.Scanner {
        var available = true
        var starts = 0
        var stops = 0
        var active: ScanCallback? = null

        override fun isAvailable() = available

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

    private lateinit var cache: RadarBeaconRangingCache
    private lateinit var scanner: FakeScanner
    private lateinit var permissions: PermissionsHelper
    private var clock = 0L
    private var foreground = true
    private var lastLocation: Location? = null
    private val searches = mutableListOf<Pair<Location, (RadarBeaconRangingCache.SearchResult?) -> Unit>>()

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

        cache = RadarBeaconRangingCache(context, Radar.logger)
        cache.scanner = scanner
        cache.permissionsHelper = permissions
        cache.now = { clock }
        cache.isForeground = { foreground }
        // A new `Location` on every call, like `RadarState.getLastLocation`.
        cache.lastLocation = { lastLocation?.let { Location(it) } }
        cache.searchBeacons = { location, completion -> searches.add(Pair(location, completion)) }
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

    private fun startAndSearch(result: RadarBeaconRangingCache.SearchResult) {
        cache.start()
        assertEquals(1, searches.size)
        searches.removeAt(0).second(result)
    }

    // endregion

    @Test
    fun cachedBeacons_notStarted_returnsNull() {
        assertNull(cache.cachedBeacons(location(LAT, LNG)))
        assertEquals(0, scanner.starts)
    }

    @Test
    fun cachedBeacons_beforeWarmUp_returnsNull() {
        startAndSearch(RadarBeaconRangingCache.SearchResult(beacons = listOf(beacon("2"))))

        assertTrue(cache.ranging)
        assertNull(cache.cachedBeacons(location(LAT, LNG)))
    }

    @Test
    fun cachedBeacons_afterWarmUpWithoutResults_returnsEmpty() {
        startAndSearch(RadarBeaconRangingCache.SearchResult(beacons = listOf(beacon("2"))))
        idle(RadarBeaconRangingCache.WARM_UP_MS)

        val cached = cache.cachedBeacons(location(LAT, LNG))
        assertNotNull(cached)
        assertEquals(0, cached!!.size)
    }

    @Test
    fun cachedBeacons_afterRanging_returnsBeaconsUntilTheyExpire() {
        startAndSearch(RadarBeaconRangingCache.SearchResult(beacons = listOf(beacon("2"))))
        cache.handleRanged(listOf(beacon("2", rssi = -70)))

        assertEquals(listOf(-70), cache.cachedBeacons()!!.map { it.rssi })

        clock += 1000
        cache.handleRanged(listOf(beacon("2", rssi = -50)))
        assertEquals(listOf(-50), cache.cachedBeacons()!!.map { it.rssi })

        clock += RadarBeaconRangingCache.MAX_BEACON_AGE_MS + 1
        assertEquals(0, cache.cachedBeacons()!!.size)
    }

    @Test
    fun handleRanged_zeroRssi_isIgnored() {
        startAndSearch(RadarBeaconRangingCache.SearchResult(beacons = listOf(beacon("2"))))
        cache.handleRanged(listOf(beacon("2", rssi = 0)))

        assertEquals(0, cache.cachedBeacons()!!.size)
    }

    @Test
    fun start_withoutPermissions_doesNotRange() {
        permissions.granted = false
        cache.start()

        assertTrue(searches.isEmpty())
        assertFalse(cache.ranging)
    }

    @Test
    fun start_withoutLocation_waitsForSeed() {
        lastLocation = null
        cache.start()

        assertTrue(searches.isEmpty())
        assertFalse(cache.ranging)

        cache.seedIfNeeded(location(LAT, LNG), null, null, arrayOf(beacon("2")))
        assertTrue(cache.ranging)
    }

    @Test
    fun seedIfNeeded_coversOnlyAroundWhereItWasSearched() {
        lastLocation = null
        cache.start()

        // Seeded from a search about 2km away from where `trackVerified` is later called.
        cache.seedIfNeeded(location(LAT + 0.018, LNG), null, null, arrayOf(beacon("2", lat = LAT + 0.018)))
        idle(RadarBeaconRangingCache.WARM_UP_MS)

        assertEquals(LAT + 0.018, cache.searchLocation!!.latitude, 0.0)
        assertNull(cache.cachedBeacons(location(LAT, LNG)))
        assertNull(cache.searchResult)
    }

    @Test
    fun seedIfNeeded_withBeacons_doesNotReplaceThem() {
        startAndSearch(RadarBeaconRangingCache.SearchResult(beacons = listOf(beacon("2"))))

        cache.seedIfNeeded(location(LAT, LNG), arrayOf(UUID), null, null)

        assertEquals(1, scanner.starts)
        assertFalse(cache.searchResult!!.usesIdentifiers)
    }

    @Test
    fun seedIfNeeded_notStarted_isNoOp() {
        cache.seedIfNeeded(location(LAT, LNG), null, null, arrayOf(beacon("2")))

        assertFalse(cache.ranging)
        assertNull(cache.searchResult)
    }

    @Test
    fun update_sameBeacons_doesNotRestartScan() {
        startAndSearch(RadarBeaconRangingCache.SearchResult(beacons = listOf(beacon("2"))))
        cache.update(RadarBeaconRangingCache.SearchResult(beacons = listOf(beacon("2"))))

        assertEquals(1, scanner.starts)

        cache.update(RadarBeaconRangingCache.SearchResult(beacons = listOf(beacon("3"))))
        assertEquals(2, scanner.starts)
    }

    @Test
    fun search_failed_doesNotRange() {
        cache.start()
        searches.removeAt(0).second(null)

        assertFalse(cache.ranging)
        assertNull(cache.cachedBeacons(location(LAT, LNG)))
    }

    @Test
    fun stop_invalidatesRunningSearch() {
        cache.start()
        val pending = searches.removeAt(0)
        cache.stop()
        cache.start()
        pending.second(RadarBeaconRangingCache.SearchResult(beacons = listOf(beacon("2"))))

        assertFalse(cache.ranging)

        // The newer search still applies.
        searches.removeAt(0).second(RadarBeaconRangingCache.SearchResult(beacons = listOf(beacon("3"))))

        assertTrue(cache.ranging)
        assertEquals(setOf("$UUID-1-3"), cache.searchResult!!.filterKeys)
    }

    @Test
    fun stop_stopsScanAndClears() {
        startAndSearch(RadarBeaconRangingCache.SearchResult(beacons = listOf(beacon("2"))))
        cache.handleRanged(listOf(beacon("2")))
        cache.stop()

        assertFalse(cache.ranging)
        assertNull(scanner.active)
        assertNull(cache.cachedBeacons(location(LAT, LNG)))
    }

    @Test
    fun background_pausesAndForeground_resumesAndSearchesAgain() {
        startAndSearch(RadarBeaconRangingCache.SearchResult(beacons = listOf(beacon("2"))))
        cache.handleRanged(listOf(beacon("2")))

        foreground = false
        cache.onBackground()
        idle(RadarBeaconRangingCache.BACKGROUND_PAUSE_DELAY_MS)

        assertFalse(cache.ranging)
        assertNull(scanner.active)

        foreground = true
        cache.onForeground()

        assertTrue(cache.ranging)
        assertEquals(1, searches.size)
        assertEquals(0, cache.cachedBeacons()?.size ?: 0)
    }

    @Test
    fun background_quickReturnToForeground_keepsScanning() {
        startAndSearch(RadarBeaconRangingCache.SearchResult(beacons = listOf(beacon("2"))))

        foreground = false
        cache.onBackground()
        foreground = true
        cache.onForeground()
        idle(RadarBeaconRangingCache.BACKGROUND_PAUSE_DELAY_MS)

        assertTrue(cache.ranging)
        assertEquals(1, scanner.starts)
    }

    @Test
    fun cachedBeacons_uuidSearch_coversAnyLocation() {
        startAndSearch(RadarBeaconRangingCache.SearchResult(uuids = listOf(UUID)))
        idle(RadarBeaconRangingCache.WARM_UP_MS)

        assertNotNull(cache.cachedBeacons(location(LAT + 1, LNG)))
    }

    @Test
    fun cachedBeacons_partialSearch_coversSearchRadius() {
        startAndSearch(RadarBeaconRangingCache.SearchResult(beacons = listOf(beacon("2"))))
        idle(RadarBeaconRangingCache.WARM_UP_MS)

        // About 500m away.
        assertNotNull(cache.cachedBeacons(location(LAT + 0.0045, LNG)))
        // About 1000m away.
        assertNull(cache.cachedBeacons(location(LAT + 0.009, LNG)))
        assertFalse(cache.ranging)
        assertNull(cache.searchResult)
    }

    @Test
    fun cachedBeacons_fullSearch_coversFarthestBeacon() {
        // Ten beacons, the farthest about 300m away.
        val beacons = (0 until RadarBeaconRangingCache.SEARCH_LIMIT).map { i ->
            beacon("$i", lat = LAT + 0.0003 * i)
        }
        startAndSearch(RadarBeaconRangingCache.SearchResult(beacons = beacons))
        idle(RadarBeaconRangingCache.WARM_UP_MS)

        // About 100m away: within 300m - 100m.
        assertNotNull(cache.cachedBeacons(location(LAT + 0.0009, LNG)))
        // About 250m away: beacons within 100m of it may not have been included.
        assertNull(cache.cachedBeacons(location(LAT + 0.00225, LNG)))
    }
}
