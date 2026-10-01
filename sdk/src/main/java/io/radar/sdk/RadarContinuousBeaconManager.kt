package io.radar.sdk

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
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
 * Continuously ranges nearby beacons while the app is in the foreground, so `trackVerified` can
 * attach the last ranging result without waiting on a one-shot ranging window.
 *
 * Uses its own searches and scan, separate from the one-shot ranging in `RadarOneShotBeaconManager`.
 * It searches for up to `RadarNearbyBeaconSearch.LIMIT` nearby beacons, scans for them, and
 * searches again every `REFRESH_INTERVAL_MS`. Ranging pauses when the app enters the background
 * and resumes when it returns to the foreground, until `stop()` is called. Must be used from the
 * main thread.
 */
@RequiresApi(Build.VERSION_CODES.O)
@SuppressLint("MissingPermission")
internal class RadarContinuousBeaconManager(
    private val context: Context,
    private val logger: RadarLogger,
    @SuppressLint("VisibleForTests")
    internal var permissionsHelper: RadarPermissionsHelper = RadarPermissionsHelper()
) {

    internal companion object {
        // Beacons not ranged within this many milliseconds are treated as out of range.
        const val MAX_BEACON_AGE_MS = 5000L

        // How often the beacons being ranged are searched again, so they follow the device.
        const val REFRESH_INTERVAL_MS = 60_000L

        // Requests farther than this from where the beacons were searched aren't served, since the
        // beacons near them may not have been searched. A fast-moving device would otherwise get
        // another place's beacons until the next refresh.
        const val MAX_SEARCH_DISTANCE_METERS = 100.0

        // How long a scan runs before its beacons are used, matching the one-shot ranging window.
        // Until then, an empty result could just mean the scan hasn't heard the beacons yet.
        const val MIN_SCAN_MS = 5000L

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
        companion object {
            /**
             * The result of a `RadarNearbyBeaconSearch` response, or `null` if it failed. On
             * failure the API client returns the last saved beacons, which may not be near where
             * the search was made from, so they aren't used.
             */
            fun fromResponse(
                status: RadarStatus,
                beacons: Array<RadarBeacon>?,
                uuids: Array<String>?,
                uids: Array<String>?
            ): SearchResult? = if (status == RadarStatus.SUCCESS) {
                SearchResult(uuids?.toList().orEmpty(), uids?.toList().orEmpty(), beacons?.toList().orEmpty())
            } else {
                null
            }
        }

        // UUIDs and UIDs take precedence over specific beacons, matching one-shot ranging.
        val usesIdentifiers: Boolean
            get() = uuids.isNotEmpty() || uids.isNotEmpty()

        val filterKeys: Set<String>
            get() = if (usesIdentifiers) {
                uuids.map { "uuid:${it.lowercase()}" }.toSet() + uids.map { "uid:${it.lowercase()}" }
            } else {
                beacons.map { beaconKey(it) }.toSet()
            }

        fun scanFilters(logger: RadarLogger): List<ScanFilter> {
            // Built one at a time, so an invalid identifier doesn't drop the filters after it.
            fun build(description: String, block: () -> ScanFilter?): ScanFilter? = try {
                block()
            } catch (e: Exception) {
                logger.d("Continuous beacon manager error building scan filter | $description", RadarLogType.SDK_EXCEPTION, e)
                null
            }

            return if (usesIdentifiers) {
                uuids.mapNotNull { uuid -> build("uuid = $uuid") { RadarBeaconUtils.getScanFilterForBeacon(uuid) } } +
                    uids.mapNotNull { uid -> build("uid = $uid") { RadarBeaconUtils.getScanFilterForBeaconUID(uid) } }
            } else {
                beacons.mapNotNull { beacon -> build("beacon = ${beaconKey(beacon)}") { RadarBeaconUtils.getScanFilterForBeacon(beacon) } }
            }
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

    private val handler = Handler(Looper.getMainLooper())

    internal var scanner: Scanner = BluetoothScanner(context)
    internal var now: () -> Long = { SystemClock.elapsedRealtime() }
    internal var isForeground: () -> Boolean = { RadarActivityLifecycleCallbacks.foreground }
    internal var lastLocation: () -> Location? = { RadarState.getLastLocation(context) }
    internal var searchBeacons: (Location, (SearchResult?) -> Unit) -> Unit = { location, completion ->
        RadarNearbyBeaconSearch.search(
            location,
            object : RadarApiClient.RadarSearchBeaconsApiCallback {
                override fun onComplete(
                    status: RadarStatus,
                    res: JSONObject?,
                    beacons: Array<RadarBeacon>?,
                    uuids: Array<String>?,
                    uids: Array<String>?
                ) {
                    val result = SearchResult.fromResponse(status, beacons, uuids, uids)
                    handler.post { completion(result) }
                }
            }
        )
    }

    internal var started = false
        private set

    // Where and when `searchResult` was searched.
    internal var searchLocation: Location? = null
        private set
    internal var searchedAt: Long? = null
        private set

    // The beacons being ranged, and the search they came from.
    internal var searchResult: SearchResult? = null
        private set

    // Where the running search, if any, was started from. A result from any other search is
    // stale. Compared by identity, which is safe because every search starts from a different
    // `Location` object: `lastLocation` returns a new one on every call, and each `trackVerified`
    // call has its own.
    private var pendingSearchLocation: Location? = null

    // The running scan, if any, and when it started.
    private var scanCallback: ScanCallback? = null
    private var scanStartedAt = 0L

    // The last ranging result: each beacon ranged by the running scan, and when it was last ranged.
    private var rangedBeacons = mutableMapOf<String, Pair<RadarBeacon, Long>>()

    private val refreshRunnable = Runnable {
        if (started && isForeground() && bluetoothAvailable()) {
            lastLocation()?.let { search(it) }
        }
    }

    private val backgroundPauseRunnable = Runnable {
        if (!isForeground()) {
            logger.d("Pausing continuous beacon manager in background")
            pause()
        }
    }

    // Android stops scans when Bluetooth turns off without calling `onScanFailed`, so ranging would
    // otherwise keep reporting that no beacons are nearby, and never restart the scan.
    private val bluetoothStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)) {
                BluetoothAdapter.STATE_ON -> onBluetoothStateChanged(true)
                BluetoothAdapter.STATE_TURNING_OFF, BluetoothAdapter.STATE_OFF -> onBluetoothStateChanged(false)
            }
        }
    }
    private var bluetoothStateReceiverRegistered = false

    internal val ranging: Boolean
        get() = scanCallback != null

    fun start() {
        if (started) {
            logger.d("Continuous beacon manager already started")
            return
        }

        logger.d("Starting continuous beacon manager")

        started = true
        registerBluetoothStateReceiver()
        resume()
    }

    fun stop() {
        logger.d("Stopping continuous beacon manager")

        started = false
        handler.removeCallbacks(backgroundPauseRunnable)
        unregisterBluetoothStateReceiver()
        reset()
    }

    /**
     * Beacons ranged near `location` within `MAX_BEACON_AGE_MS`. An empty array means no beacons
     * are nearby.
     *
     * Returns `null` if continuous ranging can't serve the request: it's stopped, it isn't
     * scanning, its scan has run less than `MIN_SCAN_MS`, or its beacons were searched more than
     * `MAX_SEARCH_DISTANCE_METERS` from `location`. Range beacons once instead, with a
     * `RadarNearbyBeaconSearch` from `location`, and pass the result to `onSearched` so later
     * requests there can be served.
     */
    fun beacons(location: Location): Array<RadarBeacon>? {
        if (!started) {
            return null
        }

        if (ranging && !bluetoothAvailable()) {
            // Bluetooth turned off and the state broadcast hasn't arrived yet.
            logger.d("Pausing continuous beacon manager: Bluetooth not available")
            pause()
        }

        if (ranging) {
            if (isNearSearchLocation(location)) {
                if (now() - scanStartedAt < MIN_SCAN_MS) {
                    logger.d("Continuous beacon manager scan not ready")
                    return null
                }
                return currentBeacons()
            }
        }

        // Forget the old search, and wait for the caller's search from `location`.
        reset()
        return null
    }

    /**
     * Ranges the beacons from a `RadarNearbyBeaconSearch` made from `searchedFrom`, or `null` if it
     * failed, replacing any search still running. Schedules the next refresh.
     */
    internal fun onSearched(searchedFrom: Location, result: SearchResult?) {
        if (!started) {
            return
        }

        pendingSearchLocation = null
        scheduleRefresh(REFRESH_INTERVAL_MS)
        if (result == null) {
            logger.d("Continuous beacon manager search failed")
            return
        }

        if (!permissionsGranted()) {
            return
        }

        update(result, searchedFrom)
    }

    /** Replaces the beacons being ranged with `result`, searched from `searchedFrom`. */
    internal fun update(result: SearchResult, searchedFrom: Location) {
        if (!started) {
            return
        }

        val previousKeys = searchResult?.filterKeys ?: emptySet()
        searchLocation = searchedFrom
        searchedAt = now()
        searchResult = result
        if (result.filterKeys == previousKeys && ranging) {
            return
        }

        startScan()
    }

    private fun currentBeacons(): Array<RadarBeacon> {
        val cutoff = now() - MAX_BEACON_AGE_MS
        rangedBeacons = rangedBeacons.filterValues { it.second >= cutoff }.toMutableMap()
        return rangedBeacons.values.map { it.first }.toTypedArray()
    }

    internal fun onForeground() {
        handler.removeCallbacks(backgroundPauseRunnable)
        if (!started || ranging) {
            return
        }

        logger.d("Resuming continuous beacon manager in foreground")
        resume()
    }

    internal fun onBackground() {
        if (!started) {
            return
        }

        handler.removeCallbacks(backgroundPauseRunnable)
        handler.postDelayed(backgroundPauseRunnable, BACKGROUND_PAUSE_DELAY_MS)
    }

    internal fun onBluetoothStateChanged(enabled: Boolean) {
        if (!started) {
            return
        }

        if (enabled) {
            logger.d("Resuming continuous beacon manager: Bluetooth enabled")
            resume()
        } else {
            logger.d("Pausing continuous beacon manager: Bluetooth disabled")
            pause()
        }
    }

    private fun registerBluetoothStateReceiver() {
        if (bluetoothStateReceiverRegistered) {
            return
        }

        try {
            context.applicationContext.registerReceiver(bluetoothStateReceiver, IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED))
            bluetoothStateReceiverRegistered = true
        } catch (e: Exception) {
            logger.d("Continuous beacon manager error registering Bluetooth state receiver", RadarLogType.SDK_EXCEPTION, e)
        }
    }

    private fun unregisterBluetoothStateReceiver() {
        if (!bluetoothStateReceiverRegistered) {
            return
        }

        try {
            context.applicationContext.unregisterReceiver(bluetoothStateReceiver)
        } catch (e: Exception) {
            logger.d("Continuous beacon manager error unregistering Bluetooth state receiver", RadarLogType.SDK_EXCEPTION, e)
        }
        bluetoothStateReceiverRegistered = false
    }

    /** Stops ranging and forgets the beacons, invalidating any search that's still running. */
    private fun reset() {
        pause()
        handler.removeCallbacks(refreshRunnable)
        searchResult = null
        searchLocation = null
        searchedAt = null
        pendingSearchLocation = null
    }

    /**
     * Resumes ranging the last search's beacons, if any, and searches again from `location`. If
     * `location` is `null`, searches from the last known location, unless the last search is less
     * than `REFRESH_INTERVAL_MS` old.
     */
    private fun resume(location: Location? = null) {
        if (!started || ranging) {
            return
        }

        if (!permissionsGranted()) {
            return
        }

        if (!bluetoothAvailable()) {
            logger.d("Continuous beacon manager not started: Bluetooth not available")
            return
        }

        if (!isForeground()) {
            return
        }

        if (!searchResult?.filterKeys.isNullOrEmpty()) {
            startScan()
        }

        val searchAge = searchedAt?.let { now() - it }
        if (location == null && searchAge != null && searchAge < REFRESH_INTERVAL_MS) {
            scheduleRefresh(REFRESH_INTERVAL_MS - searchAge)
            return
        }

        val searchFrom = location ?: lastLocation()
        if (searchFrom == null) {
            logger.d("Continuous beacon manager waiting for a location to search beacons")
            return
        }

        search(searchFrom)
    }

    /** Searches beacons near `searchFrom` and ranges them, then schedules the next search. */
    private fun search(searchFrom: Location) {
        handler.removeCallbacks(refreshRunnable)
        pendingSearchLocation = searchFrom
        searchBeacons(searchFrom) { result ->
            if (pendingSearchLocation !== searchFrom) {
                logger.d("Continuous beacon manager ignoring stale search")
                return@searchBeacons
            }
            onSearched(searchFrom, result)
        }
    }

    private fun permissionsGranted(): Boolean {
        if (!permissionsHelper.fineLocationPermissionGranted(context) && !permissionsHelper.coarseLocationPermissionGranted(context)) {
            logger.d("Continuous beacon manager not started: location not authorized")
            return false
        }

        if (!permissionsHelper.bluetoothPermissionsGranted(context)) {
            logger.d("Continuous beacon manager not started: Bluetooth permissions not granted")
            return false
        }

        return true
    }

    private fun scheduleRefresh(delayMs: Long) {
        handler.removeCallbacks(refreshRunnable)
        handler.postDelayed(refreshRunnable, delayMs)
    }

    /** Stops the running scan and forgets its result. */
    private fun pause() {
        scanCallback?.let { callback ->
            try {
                scanner.stop(callback)
            } catch (e: Exception) {
                logger.d("Continuous beacon manager error stopping scan", RadarLogType.SDK_EXCEPTION, e)
            }
        }
        scanCallback = null
        rangedBeacons.clear()
    }

    private fun startScan() {
        if (!isForeground()) {
            // Don't keep ranging beacons from an older search.
            pause()
            return
        }

        if (!bluetoothAvailable()) {
            // The scanner does nothing while Bluetooth is off. Leave the scan stopped so it
            // restarts when Bluetooth turns back on.
            logger.d("Continuous beacon manager not started: Bluetooth not available")
            pause()
            return
        }

        pause()
        val filters = searchResult?.scanFilters(logger).orEmpty()
        if (filters.isEmpty()) {
            return
        }

        searchResult?.filterKeys?.forEach { key -> logger.d("Continuous beacon manager ranging | $key") }

        val callback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult?) {
                handleScanResult(result)
            }

            override fun onBatchScanResults(results: MutableList<ScanResult>?) {
                results?.forEach { handleScanResult(it) }
            }

            override fun onScanFailed(errorCode: Int) {
                logger.d("Continuous beacon manager scan failed | errorCode = $errorCode")
                if (scanCallback === this) {
                    pause()
                }
            }
        }

        try {
            scanner.start(filters, callback)
        } catch (e: Exception) {
            logger.e("Continuous beacon manager error starting scan", RadarLogType.SDK_EXCEPTION, e)
            return
        }
        scanCallback = callback
        scanStartedAt = now()
    }

    // Treats an exception from the scanner as Bluetooth being unavailable rather than crashing the
    // host app, which calls in through `trackVerified` and lifecycle callbacks.
    private fun bluetoothAvailable(): Boolean = try {
        scanner.isAvailable()
    } catch (e: Exception) {
        logger.d("Continuous beacon manager error checking Bluetooth availability", RadarLogType.SDK_EXCEPTION, e)
        false
    }

    private fun handleScanResult(result: ScanResult?) {
        try {
            val scanRecord = result?.scanRecord ?: return
            RadarBeaconUtils.getBeacon(result, scanRecord)?.let { handleRanged(listOf(it)) }
        } catch (e: Exception) {
            logger.d("Continuous beacon manager error handling scan result", RadarLogType.SDK_EXCEPTION, e)
        }
    }

    internal fun handleRanged(beacons: List<RadarBeacon>) {
        if (!ranging) {
            return
        }

        val timestamp = now()
        for (beacon in beacons) {
            if (beacon.rssi == null || beacon.rssi == 0) {
                continue
            }
            rangedBeacons[beaconKey(beacon)] = Pair(beacon, timestamp)
        }
    }

    private fun isNearSearchLocation(location: Location): Boolean {
        val searchLocation = searchLocation ?: return false
        val distance = location.distanceTo(searchLocation)
        if (distance > MAX_SEARCH_DISTANCE_METERS) {
            logger.d("Continuous beacon manager searched too far from location | distance = ${distance.toInt()}m")
            return false
        }
        return true
    }
}

private fun beaconKey(beacon: RadarBeacon): String = "${beacon.uuid.lowercase()}-${beacon.major.lowercase()}-${beacon.minor.lowercase()}"
