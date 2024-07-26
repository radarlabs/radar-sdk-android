package io.radar.sdk

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresPermission
import com.google.android.gms.location.ActivityRecognition
import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.ActivityTransitionRequest
import com.google.android.gms.location.DetectedActivity

internal class RadarActivityManager (private val context: Context) {

    private val transitions = mutableListOf<ActivityTransition>()
    val request: ActivityTransitionRequest

    internal companion object {

        private const val REQUEST_ID = 20160525 // random notification ID (Radar's birthday!)
    }

    init {
        // TODO: need complete set
        transitions += ActivityTransition.Builder()
            .setActivityType(DetectedActivity.IN_VEHICLE)
            .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
            .build()
        transitions +=
            ActivityTransition.Builder()
                .setActivityType(DetectedActivity.IN_VEHICLE)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
                .build()

        transitions +=
            ActivityTransition.Builder()
                .setActivityType(DetectedActivity.WALKING)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
                .build()
        request = ActivityTransitionRequest(transitions)
    }

    private val activityClient = ActivityRecognition.getClient(context)
    private val pendingIntent: PendingIntent by lazy {
        PendingIntent.getBroadcast(
            context,
            REQUEST_ID,
            Intent("USER-ACTIVITY-DETECTION-INTENT-ACTION"),
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                PendingIntent.FLAG_CANCEL_CURRENT
            } else {
                PendingIntent.FLAG_MUTABLE
            }
        )
    }



    @SuppressLint("InlinedApi")
    @RequiresPermission(
        anyOf = [
            Manifest.permission.ACTIVITY_RECOGNITION,
            "com.google.android.gms.permission.ACTIVITY_RECOGNITION"
        ]
    )
    internal fun startActivity(context: Context) = kotlin.runCatching {

        val task = activityClient.requestActivityTransitionUpdates(
            request, pendingIntent
        )
        task.addOnSuccessListener {
            // Handle success
        }

        task.addOnFailureListener { e: Exception ->
            // Handle error
        }
    }

    @SuppressLint("InlinedApi")
    @RequiresPermission(
        anyOf = [
            Manifest.permission.ACTIVITY_RECOGNITION,
            "com.google.android.gms.permission.ACTIVITY_RECOGNITION"
        ]
    )
    internal fun stopActivityUpdates(context: Context) {
        activityClient.removeActivityUpdates(pendingIntent)
    }

    internal fun startMotionUpdates() {

    }

    internal fun stopMotionUpdates(){

    }

//    internal fun requestMotionUpdates():Map<String,Float> {
//
//    }
}