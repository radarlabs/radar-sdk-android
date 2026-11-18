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

class RadarJobScheduler : JobService() {

    internal companion object {
        private const val EXTRA_LATITUDE = "latitude"
        private const val EXTRA_LONGITUDE = "longitude"
        private const val EXTRA_ACCURACY = "accuracy"
        private const val EXTRA_PROVIDER = "provider"
        private const val EXTRA_TIME = "time"
        private const val EXTRA_ELAPSED_REALTIME_NANOS = "elapsedRealtimeNanos"
        private const val EXTRA_SOURCE = "source"
        private const val EXTRA_BEACONS = "beacons"

        private const val BASE_JOB_ID_LOCATIONS = 20160525 // Radar's birthday!
        private const val BASE_JOB_ID_BEACONS = 20210216 // Beacons launch date

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
            val extras = locationExtras(location, source)

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
         * Saves a location for a job. Keeps elapsedRealtimeNanos so the track can compute the location's age from the
         * monotonic clock. Without it, the age would be the time since boot. Location jobs aren't persisted, so the
         * job always runs in the same boot the location came from.
         */
        internal fun locationExtras(location: Location, source: RadarLocationSource) = PersistableBundle().apply {
            putDouble(EXTRA_LATITUDE, location.latitude)
            putDouble(EXTRA_LONGITUDE, location.longitude)
            putDouble(EXTRA_ACCURACY, location.accuracy.toDouble())
            putString(EXTRA_PROVIDER, location.provider)
            putLong(EXTRA_TIME, location.time)
            putLong(EXTRA_ELAPSED_REALTIME_NANOS, location.elapsedRealtimeNanos)
            putString(EXTRA_SOURCE, source.name)
        }

        internal fun locationFromExtras(extras: PersistableBundle) = Location(extras.getString(EXTRA_PROVIDER)).apply {
            latitude = extras.getDouble(EXTRA_LATITUDE)
            longitude = extras.getDouble(EXTRA_LONGITUDE)
            accuracy = extras.getDouble(EXTRA_ACCURACY).toFloat()
            time = extras.getLong(EXTRA_TIME)
            elapsedRealtimeNanos = extras.getLong(EXTRA_ELAPSED_REALTIME_NANOS)
        }
    }

    override fun onStartJob(params: JobParameters): Boolean {
        if (!Radar.initialized) {
            Radar.initialize(this.applicationContext)
        }

        val extras = params.extras
        val beaconsArr = extras.getStringArray(EXTRA_BEACONS)

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
            val location = locationFromExtras(extras)

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
        val beaconsArr = extras.getStringArray(EXTRA_BEACONS)
        val source = extras.getString(EXTRA_SOURCE)

        if (beaconsArr != null) {
            Radar.logger.d("Stopping beacons job | source = $source; beaconsArr = ${beaconsArr.joinToString(",")}")
        } else {
            val location = locationFromExtras(extras)

            Radar.logger.d("Stopping location job | source = $source; location = $location")
        }

        return false
    }
}
