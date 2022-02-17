package io.radar.sdk.livedata

import android.content.Context
import android.location.Location
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.radar.sdk.Radar
import io.radar.sdk.RadarReceiver
import io.radar.sdk.livedata.model.DeviceLocation
import io.radar.sdk.model.RadarEvent
import io.radar.sdk.model.RadarUser

/**
 * RadarModel allows Kotlin developers to register for Radar events, location updates, and debug logs, via the
 * [LiveData] API.
 *
 * @see [Radar SDK Documentation](https://radar.io/documentation/sdk)
 */
class RadarModel internal constructor() {
    internal val receiver: RadarReceiver

    private val user: MutableLiveData<RadarUser> = MutableLiveData()
    private val events: MutableLiveData<List<RadarEvent>> = MutableLiveData(listOf())
    private val syncedLocation: MutableLiveData<Location> = MutableLiveData()
    private val deviceLocation: MutableLiveData<DeviceLocation> = MutableLiveData()
    private val errorStatus: MutableLiveData<Radar.RadarStatus> = MutableLiveData()
    private val logMessage: MutableLiveData<String> = MutableLiveData()

    init {
        receiver = object : RadarReceiver() {
            override fun onEventsReceived(context: Context, events: Array<RadarEvent>, user: RadarUser?) {
                if (user != null) {
                    this@RadarModel.user.value = user
                }
                this@RadarModel.events.value = events.toList()
            }

            override fun onLocationUpdated(context: Context, location: Location, user: RadarUser) {
                this@RadarModel.user.value = user
                this@RadarModel.syncedLocation.value = location
            }

            override fun onClientLocationUpdated(
                context: Context,
                location: Location,
                stopped: Boolean,
                source: Radar.RadarLocationSource
            ) {
                this@RadarModel.deviceLocation.value = DeviceLocation(location, stopped, source)
            }

            override fun onError(context: Context, status: Radar.RadarStatus) {
                this@RadarModel.errorStatus.value = status
            }

            override fun onLog(context: Context, message: String) {
                this@RadarModel.logMessage.value = message
            }

        }
    }

    /**
     * Subscribe to user identification updates
     *
     * @see [RadarReceiver.onEventsReceived]
     * @see [RadarReceiver.onLocationUpdated]
     */
    fun getUser(): LiveData<RadarUser> {
        return user
    }

    /**
     * Subscribe to the latest server events
     *
     * @see [RadarReceiver.onEventsReceived]
     */
    fun getEvents(): LiveData<List<RadarEvent>> {
        return events
    }

    /**
     * Subscribe to location updates that have been updated and synced to the server
     *
     * @see [RadarReceiver.onLocationUpdated]
     */
    fun getCurrentLocation(): LiveData<Location> {
        return syncedLocation
    }

    /**
     * Subscribe to location updates that have not necessarily synced to the server
     *
     * @see [RadarReceiver.onClientLocationUpdated]
     */
    fun getDeviceLocation(): LiveData<DeviceLocation> {
        return deviceLocation
    }

    /**
     * Subscribe to the latest error status, when a Radar API request fails. This will always contain the latest error.
     *
     * @see [RadarReceiver.onError]
     */
    fun getErrorStatus(): LiveData<Radar.RadarStatus> {
        return errorStatus
    }

    /**
     * Subscribe to the latest debug log messages. This will always contain the most-recent log message.
     *
     * @see [RadarReceiver.onLog]
     */
    fun getLogMessage(): LiveData<String> {
        return logMessage
    }
} 