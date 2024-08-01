package io.radar.sdk

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.google.android.gms.location.ActivityRecognition
import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.ActivityTransitionRequest
import com.google.android.gms.location.DetectedActivity
import org.json.JSONObject

@SuppressLint("MissingPermission")
internal class RadarActivityManager (private val context: Context) {

    private val transitions = mutableListOf<ActivityTransition>()
    val request: ActivityTransitionRequest
    private val sensorSnapshotManager:SensorSnapshotManager

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
        sensorSnapshotManager = SensorSnapshotManager(context)
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
       }
       else {
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

    internal fun requestMotionUpdates() {
        sensorSnapshotManager.getSensorSnapshot()
    }
}

class SensorSnapshotManager(private val context: Context) {

    private val sensorManager: SensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private var accelerometerSensor: Sensor? = null
    private var gyroscopeSensor: Sensor? = null
    private var magnetometerSensor: Sensor? = null
    private var accelerometerSensorEventListener: SensorEventListener? = null
    private var gyroscopeSensorEventListener: SensorEventListener? = null
    private var magnetometerSensorEventListener: SensorEventListener? = null

    init {
        accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        gyroscopeSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        magnetometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        
    }

    fun getSensorSnapshot() {
        if (accelerometerSensor == null) {
            Radar.logger.i("accelerometer_sensor not available")
        } else {
            accelerometerSensorEventListener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent) {
                    val sensorData = event.values
                    Radar.logger.d("accelerometerSensor data: ${sensorData.joinToString()}")
                    val jsonObject = JSONObject().apply {
                        put("x", sensorData[0])
                        put("y", sensorData[1])
                        put("z", sensorData[2])
                    }
                    RadarState.setLastAccelerometer(context,jsonObject)

                    sensorManager.unregisterListener(this)
                }

                override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
                    // We don't need to do anything here
                }
            }
        }

        if (gyroscopeSensor == null) {
            Radar.logger.d("gyroscope_sensor not available")
        } else {
            gyroscopeSensorEventListener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent) {
                    val sensorData = event.values
                    Radar.logger.d("gyroscopeSensor data: ${sensorData.joinToString()}")
                    val jsonObject = JSONObject().apply {
                        put("x", sensorData[0])
                        put("y", sensorData[1])
                        put("z", sensorData[2])
                    }
                    RadarState.setLastGyro(context,jsonObject)

                    sensorManager.unregisterListener(this)
                }

                override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
                    // We don't need to do anything here
                }
            }
        }

        if (magnetometerSensor == null) {
            Radar.logger.i("magnetometer_sensor not available")
        } else {
            magnetometerSensorEventListener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent) {
                    val sensorData = event.values
                    Radar.logger.d("magnetometerSensor data: ${sensorData.joinToString()}")
                    val jsonObject = JSONObject().apply {
                        put("x", sensorData[0])
                        put("y", sensorData[1])
                        put("z", sensorData[2])
                    }
                    RadarState.setLastMagnetometer(context,jsonObject)

                    sensorManager.unregisterListener(this)
                }

                override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
                    // We don't need to do anything here
                }
            }
        }

        if (accelerometerSensor != null) {
            sensorManager.registerListener(accelerometerSensorEventListener, accelerometerSensor, SensorManager.SENSOR_DELAY_NORMAL)
        }
        if (gyroscopeSensor != null) {
            sensorManager.registerListener(gyroscopeSensorEventListener, gyroscopeSensor, SensorManager.SENSOR_DELAY_NORMAL)
        }
        if (magnetometerSensor != null) {
            sensorManager.registerListener(magnetometerSensorEventListener, magnetometerSensor, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }
}