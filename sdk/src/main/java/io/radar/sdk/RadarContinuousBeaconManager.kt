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
import io.radar.sdk.Radar.RadarBeaconCallback
import io.radar.sdk.Radar.RadarLogType
import io.radar.sdk.Radar.RadarStatus
import io.radar.sdk.model.RadarBeacon
import org.json.JSONObject

/**
 * Continuously ranges nearby beacons while the app is in the foreground, so `trackVerified` can
 * attach the last ranging result without waiting on a one-shot ranging window.
 *
 * Uses its own searches and scan, separate from the one-shot ranging in `RadarOneShotBeaconManager`.
 * Ranging pauses when the app enters the background and resumes when it returns to the
 * foreground, until `stop()` is called. Must be used from the main thread.
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
        const val SEARCH_RADIUS = 1000
        const val SEARCH_LIMIT = 10

        // About the farthest a device can range a beacon. Ranging results are only used where the
        // search included every beacon within this distance of the device.
        const val BEACON_RANGE_METERS = 100.0

        // How long the first round of ranging runs, matching the one-shot ranging window. Requests
        // made before it finishes wait for it and get every beacon it ranged.
        const val WARM_UP_MS = 5000L

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

    internal var started = false
        private set

    // Whether the first round of the running scan has finished. Until then, requests wait for it.
    internal var warmedUp = false
        private set

    // Where `searchResult` was searched from.
    internal var searchLocation: Location? = null
        private set

    // The beacons being ranged, and the search they came from.
    internal var searchResult: SearchResult? = null
        private set

    // Where the running search, if any, was started from. A result from any other search is
    // stale. Compared by identity, which is safe because every search starts from a different
    // `Location` object: `lastLocation` returns a new one on every call, and each `trackVerified`
    // call has its own.
    private var pendingSearchLocation: Location? = null

    // The running scan, if any.
    private var scanCallback: ScanCallback? = null

    // The last ranging result: each beacon ranged by the running scan, and when it was last ranged.
    private var rangedBeacons = mutableMapOf<String, Pair<RadarBeacon, Long>>()

    // Requests waiting for the first round of the running scan.
    private val pendingCallbacks = mutableListOf<RadarBeaconCallback>()

    private val warmUpRunnable = Runnable {
        if (ranging) {
            warmedUp = true
            // Every beacon ranged since the scan started, including any last seen more than
            // `MAX_BEACON_AGE_MS` ago, early in the round.
            completeCallbacks(rangedBeacons.values.map { it.first }.toTypedArray())
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
     * Completes `callback` with the last ranging result near `location`, or with the result of the
     * running scan's first round if it hasn't finished yet.
     *
     * Returns `false`, without calling `callback`, if continuous ranging can't serve the request:
     * it's stopped, it isn't scanning, or its search didn't include every beacon in range of
     * `location`. Range beacons once instead. Unless stopped, it then searches for beacons near
     * `location`, so later requests there can be served.
     */
    fun rangeBeacons(location: Location, callback: RadarBeaconCallback): Boolean {
        if (!started) {
            return false
        }

        if (ranging && !bluetoothAvailable()) {
            // Bluetooth turned off and the state broadcast hasn't arrived yet.
            logger.d("Pausing continuous beacon manager: Bluetooth not available")
            pause()
        }

        if (ranging) {
            if (covers(location)) {
                addCallback(callback)
                return true
            }

            reset()
        }

        resume(location)
        return false
    }

    /**
     * Beacons ranged within `MAX_BEACON_AGE_MS`, or `null` if it is not scanning or its first
     * round hasn't finished. An empty array means no beacons are nearby.
     */
    internal fun beacons(): Array<RadarBeacon>? {
        if (!ranging || !warmedUp) {
            return null
        }

        return currentBeacons()
    }

    /** Replaces the beacons being ranged with `result`, searched from `searchedFrom`. */
    internal fun update(result: SearchResult, searchedFrom: Location) {
        if (!started) {
            return
        }

        val previousKeys = searchResult?.filterKeys ?: emptySet()
        searchLocation = searchedFrom
        searchResult = result
        if (result.filterKeys == previousKeys && ranging) {
            return
        }

        startScan()
    }

    private fun addCallback(callback: RadarBeaconCallback) {
        if (warmedUp) {
            callback.onComplete(RadarStatus.SUCCESS, currentBeacons())
        } else {
            pendingCallbacks.add(callback)
        }
    }

    private fun completeCallbacks(beacons: Array<RadarBeacon>? = null) {
        if (pendingCallbacks.isEmpty()) {
            return
        }

        val result = beacons ?: currentBeacons()
        val callbacks = pendingCallbacks.toList()
        pendingCallbacks.clear()

        logger.d("Calling continuous beacon manager callbacks | callbacks.size = ${callbacks.size}; beacons.size = ${result.size}")

        callbacks.forEach { it.onComplete(RadarStatus.SUCCESS, result) }
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
        searchResult = null
        searchLocation = null
        pendingSearchLocation = null
    }

    /**
     * Resumes ranging the last search's beacons, if any, and searches again from `location`, or
     * from the last known location if `location` is `null`.
     */
    private fun resume(location: Location? = null) {
        if (!started || ranging) {
            return
        }

        if (!permissionsHelper.fineLocationPermissionGranted(context) && !permissionsHelper.coarseLocationPermissionGranted(context)) {
            logger.d("Continuous beacon manager not started: location not authorized")
            return
        }

        if (!permissionsHelper.bluetoothPermissionsGranted(context)) {
            logger.d("Continuous beacon manager not started: Bluetooth permissions not granted")
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

        val searchFrom = location ?: lastLocation()
        if (searchFrom == null) {
            logger.d("Continuous beacon manager waiting for a location to search beacons")
            return
        }

        pendingSearchLocation = searchFrom
        searchBeacons(searchFrom) { result ->
            if (pendingSearchLocation !== searchFrom) {
                logger.d("Continuous beacon manager ignoring stale search")
                return@searchBeacons
            }
            pendingSearchLocation = null
            if (result == null) {
                logger.d("Continuous beacon manager search failed")
                return@searchBeacons
            }
            update(result, searchFrom)
        }
    }

    /**
     * Stops the running scan and forgets its result. Unless `keepCallbacks`, requests waiting for
     * its first round complete with what it has ranged so far.
     */
    private fun pause(keepCallbacks: Boolean = false) {
        handler.removeCallbacks(warmUpRunnable)
        if (!keepCallbacks) {
            completeCallbacks()
        }
        scanCallback?.let { callback ->
            try {
                scanner.stop(callback)
            } catch (e: Exception) {
                logger.d("Continuous beacon manager error stopping scan", RadarLogType.SDK_EXCEPTION, e)
            }
        }
        scanCallback = null
        warmedUp = false
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

        // Requests waiting on the old scan wait for the new one instead, so they get beacons from
        // the latest search.
        pause(keepCallbacks = true)
        val filters = searchResult?.scanFilters(logger).orEmpty()
        if (filters.isEmpty()) {
            completeCallbacks()
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
            completeCallbacks()
            return
        }
        scanCallback = callback
        handler.postDelayed(warmUpRunnable, WARM_UP_MS)
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

    /** Whether the search included every beacon in range of `location`. */
    private fun covers(location: Location): Boolean {
        val searchLocation = searchLocation ?: return false
        val distance = location.distanceTo(searchLocation).toDouble()
        val coverage = coverageRadius(searchLocation)
        if (distance + BEACON_RANGE_METERS > coverage) {
            logger.d("Continuous beacon manager search doesn't cover location | distance = ${distance.toInt()}m; coverage = ${minOf(coverage, 1_000_000.0).toInt()}m")
            return false
        }
        return true
    }

    /** How far from `searchLocation` the search result included every beacon. */
    private fun coverageRadius(searchLocation: Location): Double {
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
