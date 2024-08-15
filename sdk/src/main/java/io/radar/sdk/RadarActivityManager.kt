package io.radar.sdk

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.google.android.gms.location.ActivityRecognition
import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.ActivityTransitionRequest
import com.google.android.gms.location.DetectedActivity

@SuppressLint("MissingPermission")
internal class RadarActivityManager (private val context: Context) {

    private val transitions = mutableListOf<ActivityTransition>()
    val request: ActivityTransitionRequest

    internal companion object {
        private var isActivityUpdatesStarted = false

        fun getActivityType(int: Int): Radar.RadarActivityType {
            return when (int) {
                0 -> Radar.RadarActivityType.CAR
                1 -> Radar.RadarActivityType.BIKE
                3 -> Radar.RadarActivityType.STATIONARY
                7 -> Radar.RadarActivityType.FOOT
                8 -> Radar.RadarActivityType.RUN
                else -> Radar.RadarActivityType.UNKNOWN
            }
        }
    }


    init {
        transitions += ActivityTransition.Builder()
            .setActivityType(DetectedActivity.IN_VEHICLE)
            .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
            .build()
        transitions +=
            ActivityTransition.Builder()
                .setActivityType(DetectedActivity.ON_BICYCLE)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                .build()
        transitions +=
            ActivityTransition.Builder()
                .setActivityType(DetectedActivity.RUNNING)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                .build()
        transitions +=
            ActivityTransition.Builder()
                .setActivityType(DetectedActivity.STILL)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                .build()
        transitions +=
            ActivityTransition.Builder()
                .setActivityType(DetectedActivity.WALKING)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                .build()
        request = ActivityTransitionRequest(transitions)
    }

    private val activityClient = ActivityRecognition.getClient(context)

    internal fun startActivityUpdates() = kotlin.runCatching {
        val hasPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACTIVITY_RECOGNITION
        ) == PackageManager.PERMISSION_GRANTED
        } else {
            false
        }

        if (!hasPermission) {
            Radar.logger.d("Permission for activity recognition not granted")
            return@runCatching
        }
       if (!isActivityUpdatesStarted) {
            Radar.logger.d("trying to start activity updates")

            val task = activityClient.requestActivityTransitionUpdates(
                request, RadarLocationReceiver.getActivityPendingIntent(context)
            )
            task.addOnSuccessListener {
                isActivityUpdatesStarted = true
                Radar.logger.d("Activity updates started")
            }

            task.addOnFailureListener { e: Exception ->
                Radar.logger.e("Activity updates failed to start")
            }
       } else {
            Radar.logger.d("Activity updates already started")
       }
    }

    internal fun stopActivityUpdates() {
        if (!isActivityUpdatesStarted) {
            return
        }
        activityClient.removeActivityUpdates(RadarLocationReceiver.getActivityPendingIntent(context))
        isActivityUpdatesStarted = false
    }

}
