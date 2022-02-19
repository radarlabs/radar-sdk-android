package io.radar.sdk

import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import android.location.Location
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PersistableBundle
import androidx.annotation.RequiresApi
import io.radar.sdk.Radar.RadarLocationSource
import io.radar.sdk.Radar.stringForSource
import java.util.concurrent.atomic.AtomicInteger

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
class RadarJobScheduler : JobService() {

    internal companion object {
        private const val EXTRA_LATITUDE = "latitude"
        private const val EXTRA_LONGITUDE = "longitude"
        private const val EXTRA_ACCURACY = "accuracy"
        private const val EXTRA_PROVIDER = "provider"
        private const val EXTRA_TIME = "time"
        private const val EXTRA_SOURCE = "source"
        /**
         * Base Job ID (Radar's birthday!)
         */
        private const val BASE_JOB_ID = 20160525

        /**
         * Current number of active jobs
         */
        private val counter = AtomicInteger()

        internal fun scheduleJob(context: Context, location: Location, source: RadarLocationSource) {
            val componentName = ComponentName(context, RadarJobScheduler::class.java)
            val extras = PersistableBundle().apply {
                putDouble(EXTRA_LATITUDE, location.latitude)
                putDouble(EXTRA_LONGITUDE, location.longitude)
                putDouble(EXTRA_ACCURACY, location.accuracy.toDouble())
                putString(EXTRA_PROVIDER, location.provider)
                putLong(EXTRA_TIME, location.time)
                putString(EXTRA_SOURCE, source.name)
            }

            val settings = RadarSettings.getFeatureSettings(context)
            val jobId = BASE_JOB_ID + (counter.incrementAndGet() % settings.maxConcurrentJobs)

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
                Radar.logger.d(
                    "Scheduling Location Job |" +
                            " location = $location;" +
                            " source = ${stringForSource(source)};"
                )
            } else {
                Radar.logger.d(
                    "Failed to Schedule Location Job |" +
                            " location = $location;" +
                            " source = ${stringForSource(source)};"
                )
            }

        }
    }

    override fun onStartJob(params: JobParameters): Boolean {
        val extras = params.extras
        val latitude = extras.getDouble(EXTRA_LATITUDE)
        val longitude = extras.getDouble(EXTRA_LONGITUDE)
        val accuracy = extras.getDouble(EXTRA_ACCURACY).toFloat()
        val provider = extras.getString(EXTRA_PROVIDER)
        val time = extras.getLong(EXTRA_TIME)

        val location = Location(provider).apply {
            this.latitude = latitude
            this.longitude = longitude
            this.accuracy = accuracy
            this.time = time
        }

        val sourceStr = extras.getString(EXTRA_SOURCE)
        if (Radar.isTestKey()) {
            val batteryState = Radar.batteryManager.getBatteryState()
            Radar.logger.d(
                "Starting Location Job | " +
                        "location = $location; " +
                        "source = ${sourceStr}; " +
                        "standbyBucket = ${Radar.batteryManager.getAppStandbyBucket()}; " +
                        "performanceState = ${batteryState.performanceState.name}; " +
                        "isCharging = ${batteryState.isCharging}; " +
                        "batteryPercentage = ${batteryState.percent}; " +
                        "isPowerSaveMode = ${batteryState.powerSaveMode}; " +
                        "isIgnoringBatteryOptimizations = ${batteryState.isIgnoringBatteryOptimizations}; " +
                        "locationPowerSaveMode = ${batteryState.getPowerLocationPowerSaveModeString()}; " +
                        "isDozeMode = ${batteryState.isDeviceIdleMode}"
            )
        }

        if (sourceStr == null) {
            return false
        }

        val source = RadarLocationSource.valueOf(sourceStr)

        Radar.handleLocation(this.applicationContext, location, source)

        Handler(Looper.getMainLooper()).postDelayed({
            this.jobFinished(params, false)
        }, 10000)

        counter.set(0)
        return true
    }

    override fun onStopJob(params: JobParameters): Boolean {
        if (Radar.isTestKey()) {
            val extras = params.extras
            val latitude = extras.getDouble(EXTRA_LATITUDE)
            val longitude = extras.getDouble(EXTRA_LONGITUDE)
            val accuracy = extras.getDouble(EXTRA_ACCURACY).toFloat()
            val provider = extras.getString(EXTRA_PROVIDER)
            val time = extras.getLong(EXTRA_TIME)

            val location = Location(provider).apply {
                this.latitude = latitude
                this.longitude = longitude
                this.accuracy = accuracy
                this.time = time
            }

            val source = extras.getString(EXTRA_SOURCE)
            //this may occur, for example, if a new location job is scheduled before the current job finishes executing.
            Radar.logger.d("Stopping Location Job | location = $location; source = $source")
        }

        return false
    }

}