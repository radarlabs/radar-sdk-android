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
import java.util.*
import org.json.JSONObject

@RequiresApi(Build.VERSION_CODES.O)
@SuppressLint("MissingPermission")
internal class RadarBeaconManager(
    private val context: Context,
    private val logger: RadarLogger,
    @SuppressLint("VisibleForTests")
    internal var permissionsHelper: RadarPermissionsHelper = RadarPermissionsHelper()
) {

    private lateinit var adapter: BluetoothAdapter
    private var started = false
    private val callbacks = Collections.synchronizedList(mutableListOf<RadarBeaconCallback>())
    private var monitoredBeaconIdentifiers = setOf<String>()
    private var nearbyBeacons = mutableSetOf<RadarBeacon>()
    private var beacons = arrayOf<RadarBeacon>()
    private var beaconUUIDs = arrayOf<String>()
    private var beaconUIDs = arrayOf<String>()
    private var scanCallback: ScanCallback? = null
    private val handler = Handler(Looper.getMainLooper())

    internal companion object {
        private const val TIMEOUT_TOKEN = "timeout"

        // Beacons not ranged by continuous ranging within this many milliseconds are treated as
        // out of range.
        const val MAX_BEACON_AGE_MS = 5000L
        const val SEARCH_RADIUS = 1000
        const val SEARCH_LIMIT = 10

        // About the farthest a device can range a beacon. Continuous ranging results are only used
        // where the search included every beacon within this distance of the device.
        const val BEACON_RANGE_METERS = 100.0

        // How long the first round of continuous ranging runs. Android only reports matches, so a
        // scan that has run this long without a match means no beacons are nearby.
        const val WARM_UP_MS = 1000L

        // Delay before pausing continuous ranging on background, so moving between activities
        // doesn't restart the scan. Android fails scans started more than 5 times in 30 seconds.
        const val BACKGROUND_PAUSE_DELAY_MS = 700L
    }

    private fun addCallback(callback: RadarBeaconCallback?) {
        if (callback == null) {
            return
        }

        synchronized(callbacks) {
            callbacks.add(callback)
        }
    }

    private fun callCallbacks(nearbyBeacons: Array<RadarBeacon>? = null) {
        synchronized(callbacks) {
            if (callbacks.isEmpty()) {
                return
            }

            logger.d("Calling callbacks | callbacks.size = ${callbacks.size}")

            for (callback in callbacks) {
                callback.onComplete(RadarStatus.SUCCESS, nearbyBeacons)
            }
            callbacks.clear()
        }
    }

    fun startMonitoringBeacons(beacons: Array<RadarBeacon>) {
        if (RadarSettings.getSdkConfiguration(context).useRadarModifiedBeacon) {
            return
        }

        if (!permissionsHelper.bluetoothPermissionsGranted(context)) {
            logger.d("Bluetooth permissions not granted")

            return
        }

        if (!isBluetoothSupported(context)) {
            logger.d("Bluetooth not supported")

            return
        }

        if (!this::adapter.isInitialized) {
            adapter = BluetoothAdapter.getDefaultAdapter()
        }

        if (!adapter.isEnabled) {
            logger.d("Bluetooth not enabled")

            return
        }

        val newBeaconIdentifiers = beacons.mapNotNull { it._id }.toSet()
        if (monitoredBeaconIdentifiers == newBeaconIdentifiers) {
            logger.d("Already monitoring beacons")

            return
        }

        this.stopMonitoringBeacons()

        if (beacons.isEmpty()) {
            logger.d("No beacons to monitor")

            return
        }

        monitoredBeaconIdentifiers = newBeaconIdentifiers

        val scanFilters = mutableListOf<ScanFilter>()

        for (beacon in beacons) {
            var scanFilter: ScanFilter? = null
            try {
                logger.d("Building scan filter for monitoring | _id = ${beacon._id}")

                scanFilter = RadarBeaconUtils.getScanFilterForBeacon(beacon)
            } catch (e: Exception) {
                logger.d("Error building scan filter for monitoring | _id = ${beacon._id}", RadarLogType.SDK_EXCEPTION, e)
            }

            if (scanFilter != null) {
                logger.d("Starting monitoring beacon | _id = ${beacon._id}; uuid = ${beacon.uuid}; major = ${beacon.major}; minor = ${beacon.minor}")

                scanFilters.add(scanFilter)
            }
        }

        if (scanFilters.size == 0) {
            logger.d("No scan filters for monitoring")

            return
        }

        try {
            val scanSettings = getScanSettings(ScanSettings.SCAN_MODE_LOW_POWER)

            logger.d("Starting monitoring beacons")

            adapter.bluetoothLeScanner.startScan(scanFilters, scanSettings, RadarLocationReceiver.getBeaconPendingIntent(context))
        } catch (e: Exception) {
            logger.e("Error starting monitoring beacons", RadarLogType.SDK_EXCEPTION, e)
        }
    }

    fun startMonitoringBeaconUUIDs(beaconUUIDs: Array<String>?, beaconUIDs: Array<String>?) {
        if (RadarSettings.getSdkConfiguration(context).useRadarModifiedBeacon) {
            return
        }

        if (!permissionsHelper.bluetoothPermissionsGranted(context)) {
            logger.d("Bluetooth permissions not granted")

            return
        }

        if (!isBluetoothSupported(context)) {
            logger.d("Bluetooth not supported")

            return
        }

        if (!this::adapter.isInitialized) {
            adapter = BluetoothAdapter.getDefaultAdapter()
        }

        if (!adapter.isEnabled) {
            logger.d("Bluetooth not enabled")

            return
        }

        val newBeaconIdentifiers = mutableSetOf<String>()
        if (beaconUUIDs != null) {
            newBeaconIdentifiers.addAll(beaconUUIDs)
        }
        if (beaconUIDs != null) {
            newBeaconIdentifiers.addAll(beaconUIDs)
        }
        if (monitoredBeaconIdentifiers == newBeaconIdentifiers) {
            logger.d("Already monitoring beacons")

            return
        }

        this.stopMonitoringBeacons()

        if (beaconUUIDs.isNullOrEmpty() && beaconUIDs.isNullOrEmpty()) {
            logger.d("No beacon UUIDs or UIDs to monitor")

            return
        }

        monitoredBeaconIdentifiers = newBeaconIdentifiers

        val scanFilters = mutableListOf<ScanFilter>()

        if (beaconUUIDs != null) {
            for (beaconUUID in beaconUUIDs) {
                var scanFilter: ScanFilter? = null
                try {
                    logger.d("Building scan filter for monitoring | beaconUUID = $beaconUUID")

                    scanFilter = RadarBeaconUtils.getScanFilterForBeacon(beaconUUID)
                } catch (e: Exception) {
                    logger.d("Error building scan filter for monitoring | beaconUUID = $beaconUUID", RadarLogType.SDK_EXCEPTION, e)
                }

                if (scanFilter != null) {
                    logger.d("Starting monitoring beacon UUID | beaconUUID = $beaconUUID")

                    scanFilters.add(scanFilter)
                }
            }
        }

        if (beaconUIDs != null) {
            for (beaconUID in beaconUIDs) {
                var scanFilter: ScanFilter? = null
                try {
                    logger.d("Building scan filter for monitoring | beaconUID = $beaconUID")

                    scanFilter = RadarBeaconUtils.getScanFilterForBeaconUID(beaconUID)
                } catch (e: Exception) {
                    logger.d("Error building scan filter for monitoring | beaconUID = $beaconUID", RadarLogType.SDK_EXCEPTION, e)
                }

                if (scanFilter != null) {
                    logger.d("Starting monitoring beacon UID | beaconUID = $beaconUID")

                    scanFilters.add(scanFilter)
                }
            }
        }

        if (scanFilters.size == 0) {
            logger.d("No scan filters for monitoring")

            return
        }

        try {
            val scanSettings = getScanSettings(ScanSettings.SCAN_MODE_LOW_POWER)

            logger.d("Starting monitoring beacon UUIDs")

            adapter.bluetoothLeScanner.startScan(scanFilters, scanSettings, RadarLocationReceiver.getBeaconPendingIntent(context))
        } catch (e: Exception) {
            logger.e("Error starting monitoring beacon UUIDs", RadarLogType.SDK_EXCEPTION, e)
        }
    }

    fun stopMonitoringBeacons() {
        if (RadarSettings.getSdkConfiguration(context).useRadarModifiedBeacon) {
            return
        }

        if (!permissionsHelper.bluetoothPermissionsGranted(context)) {
            return
        }

        if (!isBluetoothSupported(context)) {
            return
        }

        if (!this::adapter.isInitialized) {
            adapter = BluetoothAdapter.getDefaultAdapter()
        }

        if (!adapter.isEnabled) {
            logger.d("Bluetooth not enabled")

            return
        }

        logger.d("Stopping monitoring beacons")

        try {
            adapter.bluetoothLeScanner.stopScan(RadarLocationReceiver.getBeaconPendingIntent(context))
        } catch (e: Exception) {
            logger.d("Error stopping monitoring beacons", RadarLogType.SDK_EXCEPTION, e)
        }

        monitoredBeaconIdentifiers = setOf()
    }

    /**
     * Ranges `beacons` once. Pass `searchedFrom` only when `beacons` come from a successful search
     * near that location: while continuous ranging is on, the request then switches continuous
     * ranging to `beacons` and completes with its result instead of starting a separate scan.
     */
    fun rangeBeacons(beacons: Array<RadarBeacon>, background: Boolean, callback: RadarBeaconCallback?, searchedFrom: Location? = null) {
        if (!permissionsHelper.bluetoothPermissionsGranted(context)) {
            logger.d("Bluetooth permissions not granted")

            Radar.sendError(RadarStatus.ERROR_PERMISSIONS)

            callback?.onComplete(RadarStatus.ERROR_PERMISSIONS)

            return
        }

        if (!isBluetoothSupported(context)) {
            logger.d("Bluetooth not supported")

            Radar.sendError(RadarStatus.ERROR_BLUETOOTH)

            callback?.onComplete(RadarStatus.ERROR_BLUETOOTH)

            return
        }

        if (!this::adapter.isInitialized) {
            adapter = BluetoothAdapter.getDefaultAdapter()
        }

        if (!adapter.isEnabled) {
            logger.d("Bluetooth not enabled")

            Radar.sendError(RadarStatus.ERROR_BLUETOOTH)

            callback?.onComplete(RadarStatus.ERROR_BLUETOOTH)

            return
        }

        if (beacons.isEmpty()) {
            logger.d("No beacons to range")

            callback?.onComplete(RadarStatus.SUCCESS, emptyArray())

            return
        }

        if (searchedFrom != null && rangeWithContinuousRanging(ContinuousSearchResult(beacons = beacons.toList()), searchedFrom, callback)) {
            return
        }

        this.addCallback(callback)

        if (this.started) {
            logger.d("Already ranging beacons")

            return
        }

        this.beacons = beacons
        this.started = true

        val scanFilters = mutableListOf<ScanFilter>()

        for (beacon in beacons) {
            var scanFilter: ScanFilter? = null
            try {
                logger.d("Building scan filter for ranging | _id = ${beacon._id}")

                scanFilter = RadarBeaconUtils.getScanFilterForBeacon(beacon)
            } catch (e: Exception) {
                logger.d("Error building scan filter for ranging | _id = ${beacon._id}", RadarLogType.SDK_EXCEPTION, e)
            }

            if (scanFilter != null) {
                logger.d("Starting ranging beacon | type = ${beacon.type}; _id = ${beacon._id}; uuid = ${beacon.uuid}; major = ${beacon.major}; minor = ${beacon.minor}")

                scanFilters.add(scanFilter)
            }
        }

        if (scanFilters.size == 0) {
            logger.d("No scan filters for ranging")

            this.callCallbacks()

            return
        }

        val scanMode = if (background) ScanSettings.SCAN_MODE_LOW_POWER else ScanSettings.SCAN_MODE_LOW_LATENCY
        val scanSettings = getScanSettings(scanMode)

        val beaconManager = this

        this.scanCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult?) {
                super.onScanResult(callbackType, result)

                beaconManager.handleScanResult(callbackType, result)
            }

            override fun onBatchScanResults(results: MutableList<ScanResult>?) {
                super.onBatchScanResults(results)

                results?.forEach { result -> beaconManager.handleScanResult(ScanSettings.CALLBACK_TYPE_FIRST_MATCH, result) }
            }

            override fun onScanFailed(errorCode: Int) {
                super.onScanFailed(errorCode)

                logger.d("Scan failed")

                beaconManager.stopRanging()
            }
        }

        try {
            adapter.bluetoothLeScanner.startScan(scanFilters, scanSettings, scanCallback)
        } catch (e: Exception) {
            logger.e("Error starting ranging beacons", RadarLogType.SDK_EXCEPTION, e)
        }

        handler.postAtTime({
            logger.d("Beacon ranging timeout")

            this.stopRanging()
        }, TIMEOUT_TOKEN, SystemClock.uptimeMillis() + 5000L)
    }

    /** Ranges beacons with `beaconUUIDs` or `beaconUIDs` once. See `rangeBeacons()` for `searchedFrom`. */
    fun rangeBeaconUUIDs(
        beaconUUIDs: Array<String>?,
        beaconUIDs: Array<String>?,
        background: Boolean,
        callback: RadarBeaconCallback?,
        searchedFrom: Location? = null
    ) {
        if (!permissionsHelper.bluetoothPermissionsGranted(context)) {
            logger.d("Bluetooth permissions not granted")

            Radar.sendError(RadarStatus.ERROR_PERMISSIONS)

            callback?.onComplete(RadarStatus.ERROR_PERMISSIONS)

            return
        }

        if (!isBluetoothSupported(context)) {
            logger.d("Bluetooth not supported")

            Radar.sendError(RadarStatus.ERROR_BLUETOOTH)

            callback?.onComplete(RadarStatus.ERROR_BLUETOOTH)

            return
        }

        if (!this::adapter.isInitialized) {
            adapter = BluetoothAdapter.getDefaultAdapter()
        }

        if (!adapter.isEnabled) {
            logger.d("Bluetooth not enabled")

            Radar.sendError(RadarStatus.ERROR_BLUETOOTH)

            callback?.onComplete(RadarStatus.ERROR_BLUETOOTH)

            return
        }

        if (beaconUUIDs.isNullOrEmpty() && beaconUIDs.isNullOrEmpty()) {
            logger.d("No beacon UUIDs or UIDs to range")

            callback?.onComplete(RadarStatus.SUCCESS, emptyArray())

            return
        }

        if (searchedFrom != null &&
            rangeWithContinuousRanging(ContinuousSearchResult(uuids = beaconUUIDs?.toList().orEmpty(), uids = beaconUIDs?.toList().orEmpty()), searchedFrom, callback)
        ) {
            return
        }

        this.addCallback(callback)

        if (this.started) {
            logger.d("Already ranging beacons")

            return
        }

        this.beaconUUIDs = beaconUUIDs ?: arrayOf()
        this.beaconUIDs = beaconUIDs ?: arrayOf()
        this.started = true

        val scanFilters = mutableListOf<ScanFilter>()

        if (beaconUUIDs != null) {
            for (beaconUUID in beaconUUIDs) {
                var scanFilter: ScanFilter? = null
                try {
                    logger.d("Building scan filter for ranging | beaconUUID = $beaconUUID")

                    scanFilter = RadarBeaconUtils.getScanFilterForBeacon(beaconUUID)
                } catch (e: Exception) {
                    logger.d("Error building scan filter for ranging | beaconUUID = $beaconUUID", RadarLogType.SDK_EXCEPTION, e)
                }

                if (scanFilter != null) {
                    logger.d("Starting ranging beacon UUID | beaconUUID = $beaconUUID")

                    scanFilters.add(scanFilter)
                }
            }
        }

        if (beaconUIDs != null) {
            for (beaconUID in beaconUIDs) {
                var scanFilter: ScanFilter? = null
                try {
                    logger.d("Building scan filter for ranging | beaconUID = $beaconUID")

                    scanFilter = RadarBeaconUtils.getScanFilterForBeaconUID(beaconUID)
                } catch (e: Exception) {
                    logger.d("Error building scan filter for ranging | beaconUID = $beaconUID", RadarLogType.SDK_EXCEPTION, e)
                }

                if (scanFilter != null) {
                    logger.d("Starting ranging beacon UID | beaconUID = $beaconUID")

                    scanFilters.add(scanFilter)
                }
            }
        }

        if (scanFilters.size == 0) {
            logger.d("No scan filters for ranging")

            this.callCallbacks()

            return
        }

        val scanMode = if (background) ScanSettings.SCAN_MODE_LOW_POWER else ScanSettings.SCAN_MODE_LOW_LATENCY
        val scanSettings = getScanSettings(scanMode)

        val beaconManager = this

        this.scanCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult?) {
                super.onScanResult(callbackType, result)

                beaconManager.handleScanResult(callbackType, result)
            }

            override fun onBatchScanResults(results: MutableList<ScanResult>?) {
                super.onBatchScanResults(results)

                results?.forEach { result -> beaconManager.handleScanResult(ScanSettings.CALLBACK_TYPE_FIRST_MATCH, result) }
            }

            override fun onScanFailed(errorCode: Int) {
                super.onScanFailed(errorCode)

                logger.d("Scan failed")

                beaconManager.stopRanging()
            }
        }

        try {
            adapter.bluetoothLeScanner.startScan(scanFilters, scanSettings, scanCallback)
        } catch (e: Exception) {
            logger.e("Error starting ranging beacon UUIDs", RadarLogType.SDK_EXCEPTION, e)
        }

        handler.postAtTime({
            logger.d("Beacon ranging timeout")

            this.stopRanging()
        }, TIMEOUT_TOKEN, SystemClock.uptimeMillis() + 5000L)
    }

    private fun stopRanging() {
        if (!permissionsHelper.bluetoothPermissionsGranted(context)) {
            return
        }

        if (!isBluetoothSupported(context)) {
            return
        }

        if (!this::adapter.isInitialized) {
            adapter = BluetoothAdapter.getDefaultAdapter()
        }

        logger.d("Stopping ranging")

        handler.removeCallbacksAndMessages(TIMEOUT_TOKEN)

        try {
            adapter.bluetoothLeScanner.stopScan(scanCallback)
        } catch (e: Exception) {
            logger.d("Error stopping ranging beacons", RadarLogType.SDK_EXCEPTION, e)
        }

        scanCallback = null

        this.callCallbacks(this.nearbyBeacons.toTypedArray())

        this.beacons = arrayOf()
        this.started = false

        this.nearbyBeacons.clear()
    }

    internal fun handleBeacons(beacons: Array<RadarBeacon>?, source: Radar.RadarLocationSource) {
        if (beacons.isNullOrEmpty()) {
            logger.d("No beacons to handle")

            return
        }

        beacons.forEach { beacon ->
            if (source == Radar.RadarLocationSource.BEACON_EXIT) {
                logger.d("Handling beacon exit | beacon.type = ${beacon.type}; beacon.uuid = ${beacon.uuid}; beacon.major = ${beacon.major}; beacon.minor = ${beacon.minor}; beacon.rssi = ${beacon.rssi}")

                nearbyBeacons.remove(beacon)
            } else {
                logger.d("Handling beacon entry | beacon.type = ${beacon.type}; beacon.uuid = ${beacon.uuid}; beacon.major = ${beacon.major}; beacon.minor = ${beacon.minor}; beacon.rssi = ${beacon.rssi}")

                nearbyBeacons.add(beacon)
            }
        }
    }

    internal fun handleScanResult(callbackType: Int, result: ScanResult?, ranging: Boolean = true) {
        logger.d("Handling scan result")

        try {
            result?.scanRecord?.let { scanRecord -> RadarBeaconUtils.getBeacon(result, scanRecord) }?.let { beacon ->
                logger.d("Ranged beacon | beacon.type = ${beacon.type}; beacon.uuid = ${beacon.uuid}; beacon.major = ${beacon.major}; beacon.minor = ${beacon.minor}; beacon.rssi = ${beacon.rssi}")

                if (callbackType == ScanSettings.CALLBACK_TYPE_MATCH_LOST) {
                    logger.d("Handling beacon exit | beacon.type = ${beacon.type}; beacon.uuid = ${beacon.uuid}; beacon.major = ${beacon.major}; beacon.minor = ${beacon.minor}; beacon.rssi = ${beacon.rssi}")

                    nearbyBeacons.remove(beacon)
                } else {
                    logger.d("Handling beacon entry | beacon.type = ${beacon.type}; beacon.uuid = ${beacon.uuid}; beacon.major = ${beacon.major}; beacon.minor = ${beacon.minor}; beacon.rssi = ${beacon.rssi}")

                    nearbyBeacons.add(beacon)
                }
            }
        } catch (e: Exception) {
            logger.e("Error handling scan result", RadarLogType.SDK_EXCEPTION, e)
        }

        if (this.nearbyBeacons.size == this.beacons.size && ranging) {
            logger.d("Finished ranging")

            this.stopRanging()
        }
    }

    // region Continuous ranging

    // Continuous ranging keeps ranging nearby beacons while the app is in the foreground, so
    // `trackVerified` can attach the last ranging result without waiting on a one-shot ranging
    // window. It pauses when the app enters the background and resumes when it returns to the
    // foreground, until `stopContinuousRanging()` is called. Must be used from the main thread.

    internal data class ContinuousSearchResult(
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
            val filters = mutableListOf<ScanFilter>()
            try {
                if (usesIdentifiers) {
                    uuids.forEach { uuid -> RadarBeaconUtils.getScanFilterForBeacon(uuid)?.let { filters.add(it) } }
                    uids.forEach { uid -> RadarBeaconUtils.getScanFilterForBeaconUID(uid)?.let { filters.add(it) } }
                } else {
                    beacons.forEach { beacon -> RadarBeaconUtils.getScanFilterForBeacon(beacon)?.let { filters.add(it) } }
                }
            } catch (e: Exception) {
                logger.d("Continuous ranging error building scan filters", RadarLogType.SDK_EXCEPTION, e)
            }
            return filters
        }
    }

    /** The Bluetooth scanner used by continuous ranging, wrapped so tests can replace it. */
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

    internal var scanner: Scanner = BluetoothScanner(context)
    internal var now: () -> Long = { SystemClock.elapsedRealtime() }
    internal var isForeground: () -> Boolean = { RadarActivityLifecycleCallbacks.foreground }
    internal var lastLocation: () -> Location? = { RadarState.getLastLocation(context) }
    internal var searchBeacons: (Location, (ContinuousSearchResult?) -> Unit) -> Unit = { location, completion ->
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
                        ContinuousSearchResult(uuids?.toList().orEmpty(), uids?.toList().orEmpty(), beacons?.toList().orEmpty())
                    } else {
                        null
                    }
                    handler.post { completion(result) }
                }
            },
            false
        )
    }

    internal var continuousRequested = false
        private set

    // Whether the first round of the running scan has finished. Until then, requests wait for it.
    internal var continuousWarmedUp = false
        private set

    // Where `continuousSearchResult` was searched from, if known.
    internal var continuousSearchLocation: Location? = null
        private set

    // The beacons being ranged, and the search they came from.
    internal var continuousSearchResult: ContinuousSearchResult? = null
        private set

    // Where the running search, if any, was started from. A result from any other search is
    // stale. Compared by identity, which is safe because `lastLocation` returns a new `Location`
    // on every call.
    private var pendingContinuousSearchLocation: Location? = null

    // The running scan, if any.
    private var continuousScanCallback: ScanCallback? = null

    // The last ranging result: each beacon ranged by the running scan, and when it was last ranged.
    private var continuousBeacons = mutableMapOf<String, Pair<RadarBeacon, Long>>()

    // Requests waiting for the first round of the running scan.
    private val pendingContinuousCallbacks = mutableListOf<RadarBeaconCallback>()

    private val continuousWarmUpRunnable = Runnable {
        if (continuousRanging) {
            continuousWarmedUp = true
            completeContinuousCallbacks()
        }
    }

    private val continuousBackgroundPauseRunnable = Runnable {
        if (!isForeground()) {
            logger.d("Pausing continuous ranging in background")
            pauseContinuousRanging()
        }
    }

    private val continuousForegroundListener = object : RadarActivityLifecycleCallbacks.ForegroundListener {
        override fun onForeground() = onContinuousRangingForeground()
        override fun onBackground() = onContinuousRangingBackground()
    }

    // Android stops scans when Bluetooth turns off without calling `onScanFailed`, so continuous
    // ranging would otherwise keep reporting that no beacons are nearby, and never restart the scan.
    private val bluetoothStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)) {
                BluetoothAdapter.STATE_ON -> onBluetoothStateChanged(true)
                BluetoothAdapter.STATE_TURNING_OFF, BluetoothAdapter.STATE_OFF -> onBluetoothStateChanged(false)
            }
        }
    }
    private var bluetoothStateReceiverRegistered = false

    internal val continuousRanging: Boolean
        get() = continuousScanCallback != null

    fun startContinuousRanging() {
        if (continuousRequested) {
            logger.d("Continuous ranging already started")
            return
        }

        logger.d("Starting continuous ranging")

        continuousRequested = true
        RadarActivityLifecycleCallbacks.foregroundListeners.add(continuousForegroundListener)
        registerBluetoothStateReceiver()
        resumeContinuousRanging()
    }

    fun stopContinuousRanging() {
        logger.d("Stopping continuous ranging")

        continuousRequested = false
        handler.removeCallbacks(continuousBackgroundPauseRunnable)
        RadarActivityLifecycleCallbacks.foregroundListeners.remove(continuousForegroundListener)
        unregisterBluetoothStateReceiver()
        resetContinuousRanging(null)
    }

    /**
     * Completes `callback` with the last continuous ranging result near `location`, or with the
     * result of the running scan's first round if it hasn't finished yet.
     *
     * Returns `false`, without calling `callback`, if continuous ranging can't serve the request:
     * it's off, it isn't scanning, or its search didn't include every beacon in range of
     * `location`. In the last case the search is cleared, so the one-shot ranging request that
     * follows, with `searchedFrom`, switches continuous ranging to beacons near `location`.
     */
    fun rangeContinuousBeacons(location: Location, callback: RadarBeaconCallback): Boolean {
        if (!continuousRequested) {
            return false
        }

        if (continuousRanging && !bluetoothAvailable()) {
            // Bluetooth turned off and the state broadcast hasn't arrived yet.
            logger.d("Pausing continuous ranging: Bluetooth not available")
            pauseContinuousRanging()
        }

        if (!continuousRanging) {
            return false
        }

        val searchLocation = continuousSearchLocation
        if (searchLocation != null) {
            val distance = location.distanceTo(searchLocation).toDouble()
            val coverage = continuousCoverageRadius()
            if (distance + BEACON_RANGE_METERS > coverage) {
                logger.d("Continuous ranging search doesn't cover location | distance = ${distance.toInt()}m; coverage = ${minOf(coverage, 1_000_000.0).toInt()}m")
                resetContinuousRanging(location)
                return false
            }
        } else {
            continuousSearchLocation = location
        }

        addContinuousCallback(callback)
        return true
    }

    /**
     * Beacons ranged by continuous ranging within `MAX_BEACON_AGE_MS`, or `null` if it is not
     * scanning or its first round hasn't finished. An empty array means no beacons are nearby.
     */
    internal fun continuousBeacons(): Array<RadarBeacon>? {
        if (!continuousRanging || !continuousWarmedUp) {
            return null
        }

        return currentContinuousBeacons()
    }

    /** Replaces the beacons being ranged continuously. No-op unless continuous ranging is on. */
    internal fun updateContinuousRanging(result: ContinuousSearchResult, searchedFrom: Location? = null) {
        if (!continuousRequested) {
            return
        }

        val previousKeys = continuousSearchResult?.filterKeys ?: emptySet()
        continuousSearchLocation = searchedFrom ?: continuousSearchLocation
        continuousSearchResult = result
        if (result.filterKeys == previousKeys && continuousRanging) {
            return
        }

        startContinuousScan()
    }

    /**
     * Serves a one-shot ranging request from continuous ranging, switching it to `result` if it's
     * ranging other beacons. Returns `false` if continuous ranging is off or can't scan.
     */
    private fun rangeWithContinuousRanging(result: ContinuousSearchResult, searchedFrom: Location, callback: RadarBeaconCallback?): Boolean {
        if (!continuousRequested || callback == null) {
            return false
        }

        updateContinuousRanging(result, searchedFrom)
        if (!continuousRanging) {
            return false
        }

        logger.d("Ranging beacons with continuous ranging")

        addContinuousCallback(callback)
        return true
    }

    private fun addContinuousCallback(callback: RadarBeaconCallback) {
        if (continuousWarmedUp) {
            callback.onComplete(RadarStatus.SUCCESS, currentContinuousBeacons())
        } else {
            pendingContinuousCallbacks.add(callback)
        }
    }

    private fun completeContinuousCallbacks() {
        if (pendingContinuousCallbacks.isEmpty()) {
            return
        }

        val beacons = currentContinuousBeacons()
        val callbacks = pendingContinuousCallbacks.toList()
        pendingContinuousCallbacks.clear()

        logger.d("Calling continuous ranging callbacks | callbacks.size = ${callbacks.size}; beacons.size = ${beacons.size}")

        callbacks.forEach { it.onComplete(RadarStatus.SUCCESS, beacons) }
    }

    private fun currentContinuousBeacons(): Array<RadarBeacon> {
        val cutoff = now() - MAX_BEACON_AGE_MS
        continuousBeacons = continuousBeacons.filterValues { it.second >= cutoff }.toMutableMap()
        return continuousBeacons.values.map { it.first }.toTypedArray()
    }

    private fun onContinuousRangingForeground() {
        handler.removeCallbacks(continuousBackgroundPauseRunnable)
        if (!continuousRequested || continuousRanging) {
            return
        }

        logger.d("Resuming continuous ranging in foreground")
        resumeContinuousRanging()
    }

    private fun onContinuousRangingBackground() {
        if (!continuousRequested) {
            return
        }

        handler.removeCallbacks(continuousBackgroundPauseRunnable)
        handler.postDelayed(continuousBackgroundPauseRunnable, BACKGROUND_PAUSE_DELAY_MS)
    }

    internal fun onBluetoothStateChanged(enabled: Boolean) {
        if (!continuousRequested) {
            return
        }

        if (enabled) {
            logger.d("Resuming continuous ranging: Bluetooth enabled")
            resumeContinuousRanging()
        } else {
            logger.d("Pausing continuous ranging: Bluetooth disabled")
            pauseContinuousRanging()
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
            logger.d("Continuous ranging error registering Bluetooth state receiver", RadarLogType.SDK_EXCEPTION, e)
        }
    }

    private fun unregisterBluetoothStateReceiver() {
        if (!bluetoothStateReceiverRegistered) {
            return
        }

        try {
            context.applicationContext.unregisterReceiver(bluetoothStateReceiver)
        } catch (e: Exception) {
            logger.d("Continuous ranging error unregistering Bluetooth state receiver", RadarLogType.SDK_EXCEPTION, e)
        }
        bluetoothStateReceiverRegistered = false
    }

    /** Stops ranging and forgets the beacons, invalidating any search that's still running. */
    private fun resetContinuousRanging(location: Location?) {
        pauseContinuousRanging()
        continuousSearchResult = null
        continuousSearchLocation = location
        pendingContinuousSearchLocation = null
    }

    private fun resumeContinuousRanging() {
        if (!continuousRequested || continuousRanging) {
            return
        }

        if (!permissionsHelper.fineLocationPermissionGranted(context) && !permissionsHelper.coarseLocationPermissionGranted(context)) {
            logger.d("Continuous ranging not started: location not authorized")
            return
        }

        if (!permissionsHelper.bluetoothPermissionsGranted(context)) {
            logger.d("Continuous ranging not started: Bluetooth permissions not granted")
            return
        }

        if (!bluetoothAvailable()) {
            logger.d("Continuous ranging not started: Bluetooth not available")
            return
        }

        if (!isForeground()) {
            return
        }

        if (!continuousSearchResult?.filterKeys.isNullOrEmpty()) {
            startContinuousScan()
        }

        val location = lastLocation()
        if (location == null) {
            logger.d("Continuous ranging waiting for a location to search beacons")
            return
        }

        pendingContinuousSearchLocation = location
        searchBeacons(location) { result ->
            if (pendingContinuousSearchLocation !== location) {
                logger.d("Continuous ranging ignoring stale search")
                return@searchBeacons
            }
            pendingContinuousSearchLocation = null
            if (result == null) {
                logger.d("Continuous ranging search failed")
                return@searchBeacons
            }
            updateContinuousRanging(result, location)
        }
    }

    /**
     * Stops the running scan and forgets its result. Unless `keepCallbacks`, requests waiting for
     * its first round complete with what it has ranged so far.
     */
    private fun pauseContinuousRanging(keepCallbacks: Boolean = false) {
        handler.removeCallbacks(continuousWarmUpRunnable)
        if (!keepCallbacks) {
            completeContinuousCallbacks()
        }
        continuousScanCallback?.let { callback ->
            try {
                scanner.stop(callback)
            } catch (e: Exception) {
                logger.d("Continuous ranging error stopping scan", RadarLogType.SDK_EXCEPTION, e)
            }
        }
        continuousScanCallback = null
        continuousWarmedUp = false
        continuousBeacons.clear()
    }

    private fun startContinuousScan() {
        if (!isForeground()) {
            // Don't keep ranging beacons from an older search.
            pauseContinuousRanging()
            return
        }

        // Requests waiting on the old scan wait for the new one instead, so they get beacons from
        // the latest search.
        pauseContinuousRanging(keepCallbacks = true)
        val filters = continuousSearchResult?.scanFilters(logger).orEmpty()
        if (filters.isEmpty()) {
            completeContinuousCallbacks()
            return
        }

        continuousSearchResult?.filterKeys?.forEach { key -> logger.d("Continuous ranging | $key") }

        val callback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult?) {
                handleContinuousScanResult(result)
            }

            override fun onBatchScanResults(results: MutableList<ScanResult>?) {
                results?.forEach { handleContinuousScanResult(it) }
            }

            override fun onScanFailed(errorCode: Int) {
                logger.d("Continuous ranging scan failed | errorCode = $errorCode")
                if (continuousScanCallback === this) {
                    pauseContinuousRanging()
                }
            }
        }

        try {
            scanner.start(filters, callback)
        } catch (e: Exception) {
            logger.e("Continuous ranging error starting scan", RadarLogType.SDK_EXCEPTION, e)
            completeContinuousCallbacks()
            return
        }
        continuousScanCallback = callback
        handler.postDelayed(continuousWarmUpRunnable, WARM_UP_MS)
    }

    // Treats an exception from the scanner as Bluetooth being unavailable rather than crashing the
    // host app, which calls in through `trackVerified` and lifecycle callbacks.
    private fun bluetoothAvailable(): Boolean = try {
        scanner.isAvailable()
    } catch (e: Exception) {
        logger.d("Continuous ranging error checking Bluetooth availability", RadarLogType.SDK_EXCEPTION, e)
        false
    }

    private fun handleContinuousScanResult(result: ScanResult?) {
        try {
            val scanRecord = result?.scanRecord ?: return
            RadarBeaconUtils.getBeacon(result, scanRecord)?.let { handleContinuousRanged(listOf(it)) }
        } catch (e: Exception) {
            logger.d("Continuous ranging error handling scan result", RadarLogType.SDK_EXCEPTION, e)
        }
    }

    internal fun handleContinuousRanged(beacons: List<RadarBeacon>) {
        if (!continuousRanging) {
            return
        }

        val timestamp = now()
        for (beacon in beacons) {
            if (beacon.rssi == null || beacon.rssi == 0) {
                continue
            }
            continuousBeacons[beaconKey(beacon)] = Pair(beacon, timestamp)
        }
    }

    /** How far from `continuousSearchLocation` the search result included every beacon. */
    private fun continuousCoverageRadius(): Double {
        val result = continuousSearchResult ?: return 0.0
        // UUID and UID ranging matches every beacon with those identifiers, wherever the device is.
        if (result.usesIdentifiers) {
            return Double.POSITIVE_INFINITY
        }
        // Fewer beacons than the limit means the search returned every beacon in its radius.
        if (result.beacons.size < SEARCH_LIMIT) {
            return SEARCH_RADIUS.toDouble()
        }
        // A full result is the nearest beacons, so it only covers out to the farthest one.
        val searchLocation = continuousSearchLocation ?: return 0.0
        return result.beacons.mapNotNull { beacon ->
            beacon.location?.let { coordinate ->
                val results = FloatArray(1)
                Location.distanceBetween(searchLocation.latitude, searchLocation.longitude, coordinate.latitude, coordinate.longitude, results)
                results[0].toDouble()
            }
        }.maxOrNull() ?: 0.0
    }

    // endregion

    private fun isBluetoothSupported(context: Context): Boolean {
        if (!this::adapter.isInitialized) {
            val defaultAdapter = BluetoothAdapter.getDefaultAdapter()
            if (defaultAdapter != null) {
                adapter = defaultAdapter
            }
        }

        return context.packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH) && adapter != null && adapter.bluetoothLeScanner != null
    }

    private fun getScanSettings(scanMode: Int): ScanSettings = ScanSettings.Builder()
        .setScanMode(scanMode)
        .build()
}

private fun beaconKey(beacon: RadarBeacon): String = "${beacon.uuid.lowercase()}-${beacon.major.lowercase()}-${beacon.minor.lowercase()}"
