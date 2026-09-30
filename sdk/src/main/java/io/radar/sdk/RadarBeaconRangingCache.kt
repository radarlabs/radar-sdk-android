package io.radar.sdk

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.annotation.RequiresApi
import io.radar.sdk.Radar.RadarLogType
import io.radar.sdk.Radar.RadarStatus
import io.radar.sdk.model.RadarBeacon
import org.json.JSONObject

/**
 * Continuously ranges nearby beacons while the app is in the foreground so `trackVerified` can
 * attach beacons without waiting on a one-shot ranging window.
 *
 * Uses its own scan, separate from the one-shot ranging in `RadarBeaconManager`. Ranging pauses
 * when the app enters the background and resumes when it returns to the foreground, until `stop()`
 * is called. Must be used from the main thread.
 */
@RequiresApi(Build.VERSION_CODES.O)
@SuppressLint("MissingPermission")
internal class RadarBeaconRangingCache(
    private val context: Context,
    private val logger: RadarLogger
) {

    internal companion object {
        // Beacons not ranged within this many milliseconds are treated as out of range.
        const val MAX_BEACON_AGE_MS = 5000L
        const val SEARCH_RADIUS = 1000
        const val SEARCH_LIMIT = 10

        // About the farthest a device can range a beacon. Cached beacons are only used where the
        // search included every beacon within this distance of the device.
        const val BEACON_RANGE_METERS = 100.0

        // Android only reports matches, so a scan that has run this long without a match means no
        // beacons are nearby.
        const val WARM_UP_MS = 1000L

        // Delay before pausing on background, so moving between activities doesn't restart the
        // scan. Android fails scans started more than 5 times in 30 seconds.
        const val BACKGROUND_PAUSE_DELAY_MS = 700L
    }

    internal data class SearchResult(
        // iBeacon proximity UUIDs. Ranging one matches every iBeacon with that UUID, whatever its
        // major and minor.
        val uuids: List<String> = emptyList(),
        // Eddystone namespace IDs (10 bytes, 20 hex characters). Ranging one matches every
        // Eddystone-UID beacon in that namespace, whatever its instance ID.
        val uids: List<String> = emptyList(),
        // Specific beacons, ranged only when there are no UUIDs or UIDs.
        val beacons: List<RadarBeacon> = emptyList()
    ) {
        // UUIDs and UIDs take precedence over specific beacons, matching the one-shot ranging path.
        val usesIdentifiers: Boolean
            get() = uuids.isNotEmpty() || uids.isNotEmpty()

        val filterKeys: Set<String>
            get() = if (usesIdentifiers) {
                uuids.map { "uuid:${it.lowercase()}" }.toSet() + uids.map { "uid:${it.lowercase()}" }
            } else {
                beacons.map { beaconKey(it) }.toSet()
            }

        fun scanFilters(logger: RadarLogger): List<ScanFilter> {
            val filters = mutableListOf<ScanFilter>()
            try {
                if (usesIdentifiers) {
                    uuids.forEach { uuid -> RadarBeaconUtils.getScanFilterForBeacon(uuid)?.let { filters.add(it) } }
                    uids.forEach { uid -> RadarBeaconUtils.getScanFilterForBeaconUID(uid)?.let { filters.add(it) } }
                } else {
                    beacons.forEach { beacon -> RadarBeaconUtils.getScanFilterForBeacon(beacon)?.let { filters.add(it) } }
                }
            } catch (e: Exception) {
                logger.d("Beacon ranging cache error building scan filters", RadarLogType.SDK_EXCEPTION, e)
            }
            return filters
        }
    }

    /** The Bluetooth scanner, wrapped so tests can replace it. */
    internal interface Scanner {
        fun isAvailable(): Boolean
        fun start(filters: List<ScanFilter>, callback: ScanCallback)
        fun stop(callback: ScanCallback)
    }

    private class BluetoothScanner(private val context: Context) : Scanner {
        private val adapter: BluetoothAdapter?
            get() = BluetoothAdapter.getDefaultAdapter()

        override fun isAvailable(): Boolean {
            val adapter = adapter ?: return false
            return context.packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH) &&
                adapter.isEnabled &&
                adapter.bluetoothLeScanner != null
        }

        override fun start(filters: List<ScanFilter>, callback: ScanCallback) {
            val settings = ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build()
            adapter?.bluetoothLeScanner?.startScan(filters, settings, callback)
        }

        override fun stop(callback: ScanCallback) {
            adapter?.bluetoothLeScanner?.stopScan(callback)
        }
    }

    internal var permissionsHelper: RadarPermissionsHelper = RadarPermissionsHelper()
    internal var scanner: Scanner = BluetoothScanner(context)
    internal var now: () -> Long = { SystemClock.elapsedRealtime() }
    internal var isForeground: () -> Boolean = { RadarActivityLifecycleCallbacks.foreground }
    internal var lastLocation: () -> Location? = { RadarState.getLastLocation(context) }
    internal var searchBeacons: (Location, (SearchResult?) -> Unit) -> Unit = { location, completion ->
        Radar.apiClient.searchBeacons(
            location,
            SEARCH_RADIUS,
            SEARCH_LIMIT,
            object : RadarApiClient.RadarSearchBeaconsApiCallback {
                override fun onComplete(
                    status: RadarStatus,
                    res: JSONObject?,
                    beacons: Array<RadarBeacon>?,
                    uuids: Array<String>?,
                    uids: Array<String>?
                ) {
                    // On failure the API client returns the last saved beacons, which may not be
                    // near `location`, so treat it as a failed search.
                    val result = if (status == RadarStatus.SUCCESS) {
                        SearchResult(uuids?.toList().orEmpty(), uids?.toList().orEmpty(), beacons?.toList().orEmpty())
                    } else {
                        null
                    }
                    handler.post { completion(result) }
                }
            },
            false
        )
    }

    private val handler = Handler(Looper.getMainLooper())

    internal var requested = false
        private set
    internal var warmedUp = false
        private set

    // Where `searchResult` was searched from, if known.
    internal var searchLocation: Location? = null
        private set

    // The beacons being ranged, and the search they came from.
    internal var searchResult: SearchResult? = null
        private set

    // Incremented to invalidate a search that's still running.
    private var searchGeneration = 0

    // The running scan, if any.
    private var scanCallback: ScanCallback? = null
    private var cache = mutableMapOf<String, Pair<RadarBeacon, Long>>()
    private val warmUpRunnable = Runnable { warmedUp = scanCallback != null }
    private val backgroundPauseRunnable = Runnable {
        if (!isForeground()) {
            logger.d("Pausing beacon ranging cache in background")
            pause()
        }
    }

    internal val ranging: Boolean
        get() = scanCallback != null

    // region Control

    fun start() {
        if (requested) {
            logger.d("Beacon ranging cache already started")
            return
        }

        logger.d("Starting beacon ranging cache")

        requested = true
        resume()
    }

    fun stop() {
        logger.d("Stopping beacon ranging cache")

        requested = false
        handler.removeCallbacks(backgroundPauseRunnable)
        reset(null)
    }

    /** Stops ranging and forgets the beacons, invalidating any search that's still running. */
    private fun reset(location: Location?) {
        pause()
        searchResult = null
        searchLocation = location
        searchGeneration++
    }

    /** Replaces the beacons being ranged. No-op unless the cache has been started. */
    fun update(result: SearchResult, searchedFrom: Location? = null) {
        if (!requested) {
            return
        }

        val previousKeys = searchResult?.filterKeys ?: emptySet()
        searchLocation = searchedFrom ?: searchLocation
        searchResult = result
        if (result.filterKeys == previousKeys && ranging) {
            return
        }

        startRanging()
    }

    /**
     * Uses the beacons from a one-shot ranging request when the cache has started but has no
     * beacons yet, for example because there was no location to search from.
     */
    fun seedIfNeeded(uuids: Array<String>? = null, uids: Array<String>? = null, beacons: Array<RadarBeacon>? = null) {
        if (!searchResult?.filterKeys.isNullOrEmpty()) {
            return
        }

        update(SearchResult(uuids?.toList().orEmpty(), uids?.toList().orEmpty(), beacons?.toList().orEmpty()))
    }

    /**
     * Like `cachedBeacons()`, but `null` if the search didn't include every beacon in range of
     * `location`. In that case the beacons are cleared, so the one-shot ranging request that
     * follows searches near `location` and seeds the cache.
     */
    fun cachedBeacons(location: Location): Array<RadarBeacon>? {
        if (!requested) {
            return null
        }

        val searchLocation = searchLocation
        if (searchLocation != null) {
            val distance = location.distanceTo(searchLocation).toDouble()
            val coverage = coverageRadius()
            if (distance + BEACON_RANGE_METERS > coverage) {
                logger.d("Beacon ranging cache search doesn't cover location | distance = ${distance.toInt()}m; coverage = ${minOf(coverage, 1_000_000.0).toInt()}m")
                reset(location)
                return null
            }
        } else {
            this.searchLocation = location
        }

        return cachedBeacons()
    }

    /**
     * Beacons ranged within `MAX_BEACON_AGE_MS`, or `null` if the cache is not ranging or has not
     * warmed up yet. An empty array means no beacons are nearby.
     */
    fun cachedBeacons(): Array<RadarBeacon>? {
        if (!ranging || !warmedUp) {
            return null
        }

        val cutoff = now() - MAX_BEACON_AGE_MS
        cache = cache.filterValues { it.second >= cutoff }.toMutableMap()
        return cache.values.map { it.first }.toTypedArray()
    }

    // endregion

    // region Lifecycle

    fun onForeground() {
        handler.removeCallbacks(backgroundPauseRunnable)
        if (!requested || ranging) {
            return
        }

        logger.d("Resuming beacon ranging cache in foreground")
        resume()
    }

    fun onBackground() {
        if (!requested) {
            return
        }

        handler.removeCallbacks(backgroundPauseRunnable)
        handler.postDelayed(backgroundPauseRunnable, BACKGROUND_PAUSE_DELAY_MS)
    }

    // endregion

    // region Ranging

    private fun resume() {
        if (!requested || ranging) {
            return
        }

        if (!permissionsHelper.fineLocationPermissionGranted(context) && !permissionsHelper.coarseLocationPermissionGranted(context)) {
            logger.d("Beacon ranging cache not started: location not authorized")
            return
        }

        if (!permissionsHelper.bluetoothPermissionsGranted(context)) {
            logger.d("Beacon ranging cache not started: Bluetooth permissions not granted")
            return
        }

        if (!scanner.isAvailable()) {
            logger.d("Beacon ranging cache not started: Bluetooth not available")
            return
        }

        if (!isForeground()) {
            return
        }

        if (!searchResult?.filterKeys.isNullOrEmpty()) {
            startRanging()
        }

        val location = lastLocation()
        if (location == null) {
            logger.d("Beacon ranging cache waiting for a location to search beacons")
            return
        }

        val generation = ++searchGeneration
        searchBeacons(location) { result ->
            if (generation != searchGeneration) {
                logger.d("Beacon ranging cache ignoring stale search")
                return@searchBeacons
            }
            if (result == null) {
                logger.d("Beacon ranging cache search failed")
                return@searchBeacons
            }
            update(result, location)
        }
    }

    private fun pause() {
        handler.removeCallbacks(warmUpRunnable)
        scanCallback?.let { callback ->
            try {
                scanner.stop(callback)
            } catch (e: Exception) {
                logger.d("Beacon ranging cache error stopping scan", RadarLogType.SDK_EXCEPTION, e)
            }
        }
        scanCallback = null
        warmedUp = false
        cache.clear()
    }

    private fun startRanging() {
        if (!isForeground()) {
            return
        }

        pause()
        val result = searchResult ?: return
        val filters = result.scanFilters(logger)
        if (filters.isEmpty()) {
            return
        }

        result.filterKeys.forEach { key -> logger.d("Beacon ranging cache ranging | $key") }

        val callback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult?) {
                handleScanResult(result)
            }

            override fun onBatchScanResults(results: MutableList<ScanResult>?) {
                results?.forEach { handleScanResult(it) }
            }

            override fun onScanFailed(errorCode: Int) {
                logger.d("Beacon ranging cache scan failed | errorCode = $errorCode")
                if (scanCallback === this) {
                    pause()
                }
            }
        }

        try {
            scanner.start(filters, callback)
        } catch (e: Exception) {
            logger.e("Beacon ranging cache error starting scan", RadarLogType.SDK_EXCEPTION, e)
            return
        }
        scanCallback = callback
        handler.postDelayed(warmUpRunnable, WARM_UP_MS)
    }

    private fun handleScanResult(result: ScanResult?) {
        try {
            val scanRecord = result?.scanRecord ?: return
            RadarBeaconUtils.getBeacon(result, scanRecord)?.let { handleRanged(listOf(it)) }
        } catch (e: Exception) {
            logger.d("Beacon ranging cache error handling scan result", RadarLogType.SDK_EXCEPTION, e)
        }
    }

    internal fun handleRanged(beacons: List<RadarBeacon>) {
        if (!ranging) {
            return
        }

        warmedUp = true

        val timestamp = now()
        for (beacon in beacons) {
            if (beacon.rssi == null || beacon.rssi == 0) {
                continue
            }
            cache[beaconKey(beacon)] = Pair(beacon, timestamp)
        }
    }

    // endregion

    /** How far from `searchLocation` the search result included every beacon. */
    private fun coverageRadius(): Double {
        val result = searchResult ?: return 0.0
        // UUID and UID ranging matches every beacon with those identifiers, wherever the device is.
        if (result.usesIdentifiers) {
            return Double.POSITIVE_INFINITY
        }
        // Fewer beacons than the limit means the search returned every beacon in its radius.
        if (result.beacons.size < SEARCH_LIMIT) {
            return SEARCH_RADIUS.toDouble()
        }
        // A full result is the nearest beacons, so it only covers out to the farthest one.
        val searchLocation = searchLocation ?: return 0.0
        return result.beacons.mapNotNull { beacon ->
            beacon.location?.let { coordinate ->
                val results = FloatArray(1)
                Location.distanceBetween(searchLocation.latitude, searchLocation.longitude, coordinate.latitude, coordinate.longitude, results)
                results[0].toDouble()
            }
        }.maxOrNull() ?: 0.0
    }
}

private fun beaconKey(beacon: RadarBeacon): String = "${beacon.uuid.lowercase()}-${beacon.major.lowercase()}-${beacon.minor.lowercase()}"
