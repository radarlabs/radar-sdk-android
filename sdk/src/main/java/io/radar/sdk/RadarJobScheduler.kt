package io.radar.sdk

import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import android.location.Location
import android.os.Handler
import android.os.Looper
import android.os.PersistableBundle
import io.radar.sdk.Radar.RadarLocationSource
import io.radar.sdk.Radar.stringForSource
import io.radar.sdk.model.RadarBeacon
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class RadarJobScheduler : JobService() {

    internal companion object {
        private const val EXTRA_LATITUDE = "latitude"
        private const val EXTRA_LONGITUDE = "longitude"
        private const val EXTRA_ACCURACY = "accuracy"
        private const val EXTRA_PROVIDER = "provider"
        private const val EXTRA_TIME = "time"
        private const val EXTRA_SOURCE = "source"
        private const val EXTRA_BEACONS = "beacons"
        private const val EXTRA_BEACON_EXIT_CHECK = "beaconExitCheck"

        private const val BASE_JOB_ID_LOCATIONS = 20160525 // Radar's birthday!
        private const val BASE_JOB_ID_BEACONS = 20210216 // Beacons launch date

        // Scheduling the exit check again replaces it. A check that reschedules itself uses the other ID,
        // because replacing a running job stops it.
        internal const val JOB_ID_BEACON_EXIT_CHECK = 20261023
        internal const val JOB_ID_BEACON_EXIT_CHECK_NEXT = 20261024

        // How long after its earliest start the system may delay the beacon exit check.
        private val BEACON_EXIT_CHECK_DEADLINE = 30.seconds

        private val numActiveLocationJobs = AtomicInteger()
        private val numActiveBeaconJobs = AtomicInteger()

        internal fun scheduleJob(
            context: Context,
            location: Location,
            source: RadarLocationSource
        ) {
            if (!Radar.initialized) {
                Radar.initialize(context)
            }

            val componentName = ComponentName(context, RadarJobScheduler::class.java)
            val extras = PersistableBundle().apply {
                putDouble(EXTRA_LATITUDE, location.latitude)
                putDouble(EXTRA_LONGITUDE, location.longitude)
                putDouble(EXTRA_ACCURACY, location.accuracy.toDouble())
                putString(EXTRA_PROVIDER, location.provider)
                putLong(EXTRA_TIME, location.time)
                putString(EXTRA_SOURCE, source.name)
            }

            val sourceStr = stringForSource(source)

            val settings = RadarSettings.getSdkConfiguration(context)
            val jobId = BASE_JOB_ID_LOCATIONS + (numActiveLocationJobs.incrementAndGet() % settings.maxConcurrentJobs)

            val jobInfo = JobInfo.Builder(jobId, componentName)
                .setExtras(extras)
                .setMinimumLatency(0)
                .setOverrideDeadline(0)
                .setRequiredNetworkType(
                    if (settings.schedulerRequiresNetwork) JobInfo.NETWORK_TYPE_ANY else JobInfo.NETWORK_TYPE_NONE
                )
                .build()

            val jobScheduler = context.getSystemService(JOB_SCHEDULER_SERVICE) as JobScheduler
            val result = jobScheduler.schedule(jobInfo)
            if (result == JobScheduler.RESULT_SUCCESS) {
                Radar.logger.d("Scheduling location job | source = $sourceStr; location = $location")
            } else {
                Radar.logger.d("Failed to schedule location job | source = $sourceStr; location = $location")
            }
        }

        internal fun scheduleJob(
            context: Context,
            beacons: Array<RadarBeacon>,
            source: RadarLocationSource
        ) {
            if (!Radar.initialized) {
                Radar.initialize(context)
            }

            val componentName = ComponentName(context, RadarJobScheduler::class.java)
            val beaconsArr = RadarBeaconUtils.stringArrayForBeacons(beacons)
            val extras = PersistableBundle().apply {
                putStringArray(EXTRA_BEACONS, beaconsArr)
                putString(EXTRA_SOURCE, source.name)
            }

            val sourceStr = stringForSource(source)

            val settings = RadarSettings.getSdkConfiguration(context)
            val jobId = BASE_JOB_ID_BEACONS + (numActiveBeaconJobs.incrementAndGet() % settings.maxConcurrentJobs)

            val jobInfo = JobInfo.Builder(jobId, componentName)
                .setExtras(extras)
                .setMinimumLatency(0)
                .setOverrideDeadline(0)
                .setRequiredNetworkType(
                    if (settings.schedulerRequiresNetwork) JobInfo.NETWORK_TYPE_ANY else JobInfo.NETWORK_TYPE_NONE
                )
                .build()

            val jobScheduler = context.getSystemService(JOB_SCHEDULER_SERVICE) as JobScheduler
            val result = jobScheduler.schedule(jobInfo)
            if (result == JobScheduler.RESULT_SUCCESS) {
                Radar.logger.d("Scheduling beacons job | source = $sourceStr; beaconsArr = ${beaconsArr.joinToString(",")}")
            } else {
                Radar.logger.d("Failed to schedule beacons job | source = $sourceStr; beaconsArr = ${beaconsArr.joinToString(",")}")
            }
        }

        /**
         * Schedules a check, after [delay], that confirms beacon exits still pending. Pass the ID of the
         * running exit check, if any, as [runningJobId].
         */
        internal fun scheduleBeaconExitCheck(context: Context, delay: Duration, runningJobId: Int? = null) {
            val jobId = if (runningJobId == JOB_ID_BEACON_EXIT_CHECK) JOB_ID_BEACON_EXIT_CHECK_NEXT else JOB_ID_BEACON_EXIT_CHECK
            val componentName = ComponentName(context, RadarJobScheduler::class.java)
            val extras = PersistableBundle().apply {
                putBoolean(EXTRA_BEACON_EXIT_CHECK, true)
            }

            val settings = RadarSettings.getSdkConfiguration(context)
            val delayMs = delay.inWholeMilliseconds.coerceAtLeast(0L)

            val jobInfo = JobInfo.Builder(jobId, componentName)
                .setExtras(extras)
                .setMinimumLatency(delayMs)
                .setOverrideDeadline(delayMs + BEACON_EXIT_CHECK_DEADLINE.inWholeMilliseconds)
                .setRequiredNetworkType(
                    if (settings.schedulerRequiresNetwork) JobInfo.NETWORK_TYPE_ANY else JobInfo.NETWORK_TYPE_NONE
                )
                .build()

            val jobScheduler = context.getSystemService(JOB_SCHEDULER_SERVICE) as JobScheduler
            val result = jobScheduler.schedule(jobInfo)
            if (result == JobScheduler.RESULT_SUCCESS) {
                Radar.logger.d("Scheduling beacon exit check | delay = $delay")
            } else {
                Radar.logger.d("Failed to schedule beacon exit check | delay = $delay")
            }
        }
    }

    override fun onStartJob(params: JobParameters): Boolean {
        if (!Radar.initialized) {
            Radar.initialize(this.applicationContext)
        }

        val extras = params.extras

        if (extras.getBoolean(EXTRA_BEACON_EXIT_CHECK)) {
            Radar.logger.d("Starting beacon exit check job")

            Radar.handleBeaconExitCheck(this.applicationContext, params.jobId)

            Handler(Looper.getMainLooper()).postDelayed({
                this.jobFinished(params, false)
            }, 10000)

            return true
        }

        val beaconsArr = extras.getStringArray(EXTRA_BEACONS)
        val latitude = extras.getDouble(EXTRA_LATITUDE)
        val longitude = extras.getDouble(EXTRA_LONGITUDE)
        val accuracy = extras.getDouble(EXTRA_ACCURACY).toFloat()
        val provider = extras.getString(EXTRA_PROVIDER)
        val time = extras.getLong(EXTRA_TIME)

        val sourceStr = extras.getString(EXTRA_SOURCE) ?: return false

        val source = RadarLocationSource.valueOf(sourceStr)

        if (beaconsArr != null) {
            val beacons = RadarBeaconUtils.beaconsForStringArray(beaconsArr)

            Radar.logger.d("Starting beacons job | source = $sourceStr; beaconsArr = ${beaconsArr.joinToString(",")}")

            Radar.handleBeacons(this.applicationContext, beacons, source)

            Handler(Looper.getMainLooper()).postDelayed({
                this.jobFinished(params, false)
            }, 10000)

            numActiveBeaconJobs.set(0)

            return true
        } else {
            val location = Location(provider).apply {
                this.latitude = latitude
                this.longitude = longitude
                this.accuracy = accuracy
                this.time = time
            }

            if (Radar.isTestKey()) {
                val batteryState = Radar.batteryManager.getBatteryState()
                Radar.logger.d(
                    "Starting location job | " +
                        "source = $sourceStr; " +
                        "location = $location; " +
                        "standbyBucket = ${Radar.batteryManager.getAppStandbyBucket()}; " +
                        "performanceState = ${batteryState.performanceState.name}; " +
                        "isCharging = ${batteryState.isCharging}; " +
                        "batteryPercentage = ${batteryState.percent}; " +
                        "isPowerSaveMode = ${batteryState.powerSaveMode}; " +
                        "isIgnoringBatteryOptimizations = ${batteryState.isIgnoringBatteryOptimizations}; " +
                        "locationPowerSaveMode = ${batteryState.getPowerLocationPowerSaveModeString()}; " +
                        "isDozeMode = ${batteryState.isDeviceIdleMode}"
                )
            } else {
                Radar.logger.d("Starting location job | source = $sourceStr; location = $location")
            }

            Radar.handleLocation(this.applicationContext, location, source)

            Handler(Looper.getMainLooper()).postDelayed({
                this.jobFinished(params, false)
            }, 10000)

            numActiveLocationJobs.set(0)

            return true
        }
    }

    override fun onStopJob(params: JobParameters): Boolean {
        if (!Radar.initialized) {
            Radar.initialize(this.applicationContext)
        }

        val extras = params.extras

        if (extras.getBoolean(EXTRA_BEACON_EXIT_CHECK)) {
            Radar.logger.d("Stopping beacon exit check job")

            return false
        }
        val beaconsArr = extras.getStringArray(EXTRA_BEACONS)
        val latitude = extras.getDouble(EXTRA_LATITUDE)
        val longitude = extras.getDouble(EXTRA_LONGITUDE)
        val accuracy = extras.getDouble(EXTRA_ACCURACY).toFloat()
        val provider = extras.getString(EXTRA_PROVIDER)
        val time = extras.getLong(EXTRA_TIME)
        val source = extras.getString(EXTRA_SOURCE)

        if (beaconsArr != null) {
            Radar.logger.d("Stopping beacons job | source = $source; beaconsArr = ${beaconsArr.joinToString(",")}")
        } else {
            val location = Location(provider).apply {
                this.latitude = latitude
                this.longitude = longitude
                this.accuracy = accuracy
                this.time = time
            }

            Radar.logger.d("Stopping location job | source = $source; location = $location")
        }

        return false
    }
}
