package io.radar.sdk

import android.content.Context
import android.location.Location
import io.radar.sdk.model.*
import org.json.JSONArray
import org.json.JSONObject
import java.util.Date
import java.util.UUID

internal class RadarOfflineEventManager(
    private val context: Context,
    private val syncManager: RadarSyncManager,
    private val logger: RadarLogger
) {
    private val lock = Any()
    private var offlineGeofenceIds: Set<String> = emptySet()

    fun reset() {
        synchronized(lock) {
            offlineGeofenceIds = emptySet()
        }
    }

    // region Event generation

    fun generateEvents(
        location: Location,
        callback: (List<RadarEvent>, RadarUser?, Location) -> Unit
    ) {
        val state = syncManager.syncStore.read() ?: RadarSyncState()
        val baselineIds = state.lastSyncedGeofenceIds.toSet()


       val effectiveIds = synchronized(lock) {
           offlineGeofenceIds.ifEmpty { baselineIds }
       }

        val entries = syncManager.getGeofenceEntries(location, effectiveIds)
        val exits = syncManager.getGeofenceExits(location, effectiveIds)

        val isoString = RadarUtils.dateToISOString(Date()) ?: return
        val isLive = (RadarSettings.getPublishableKey(context) ?: "").startsWith("prj_live")

        val events = mutableListOf<RadarEvent>()

        for (geofence in entries) {
            makeGeofenceEvent("user.entered_geofence", geofence, location, isoString, isLive)?.let {
                events.add(it)
                logger.i("OfflineEventManager: Generated geofence entry for ${geofence._id}")
            }
        }

        for (geofence in exits) {
            makeGeofenceEvent("user.exited_geofence", geofence, location, isoString, isLive)?.let {
                events.add(it)
                logger.i("OfflineEventManager: Generated geofence exit for ${geofence._id}")
            }
        }

        val currentGeofences = syncManager.getGeofences(location)
        synchronized(lock) {
            offlineGeofenceIds = currentGeofences.map { it._id }.toSet()
        }

        val user = buildSyntheticUser(location, currentGeofences)
        callback(events, user, location)
    }

    fun handleTrackFailure(location: Location) {
        val sdkConfig = RadarSettings.getSdkConfiguration(context)
        if (!sdkConfig.offlineEventGenerationEnabled) return

        generateEvents(location) { events, user, _ ->
            if (events.isNotEmpty() && user != null) {
                Radar.sendEvents(events.toTypedArray(), user)
            }
        }
    }

    // endregion


    // region Tracking options ramp-up/down

    fun updateTrackingOptions(geofenceTags: List<String>): RadarTrackingOptions? {
        val sdkConfig = RadarSettings.getSdkConfiguration(context)
        val remoteOptions = sdkConfig.remoteTrackingOptions

        val rampUpTags = RadarRemoteTrackingOptions.geofenceTags("inGeofence", remoteOptions)
        val inRampedUpGeofences = if (rampUpTags != null) {
            rampUpTags.toSet().intersect(geofenceTags.toSet()).isNotEmpty()
        } else {
            false
        }

        return when {
            inRampedUpGeofences -> {
                logger.d("OfflineEventManager: Ramping up tracking options")
                RadarRemoteTrackingOptions.trackingOptions("inGeofence", remoteOptions)
            }
            Radar.getTripOptions() != null -> {
                val onTripOptions = RadarRemoteTrackingOptions.trackingOptions("onTrip", remoteOptions)
                if (onTripOptions != null) {
                    logger.d("OfflineEventManager: Using on-trip tracking options")
                    onTripOptions
                } else {
                    logger.d("OfflineEventManager: Using default tracking options")
                    RadarRemoteTrackingOptions.trackingOptions("default", remoteOptions)
                }
            }
            else -> {
                logger.d("OfflineEventManager: Using default tracking options")
                RadarRemoteTrackingOptions.trackingOptions("default", remoteOptions)
            }
        }
    }

    fun updateTrackingOptions(location: Location): RadarTrackingOptions? {
        val currentGeofences = syncManager.getGeofences(location)
        val tags = currentGeofences.mapNotNull { it.tag }
        return updateTrackingOptions(tags)
    }

    // endregion


    // region Private helpers

    private fun makeGeofenceEvent(
        type: String,
        geofence: RadarGeofence,
        location: Location,
        isoDate: String,
        live: Boolean
    ): RadarEvent? {
        val eventJson = JSONObject().apply {
            put("_id", "${geofence._id}_offline_${UUID.randomUUID()}")
            put("createdAt", isoDate)
            put("actualCreatedAt", isoDate)
            put("live", live)
            put("type", type)
            put("geofence", geofence.toJson())
            put("verification", 0)
            put("confidence", 1)
            put("duration", 0)
            put("location", JSONObject().apply {
                put("coordinates", JSONArray().apply {
                    put(location.longitude)
                    put(location.latitude)
                })
            })
            put("locationAccuracy", location.accuracy)
            put("replayed", false)
            put("metadata", JSONObject().apply { put("offline", true) })
        }
        return RadarEvent.fromJson(eventJson)
    }

    private fun buildSyntheticUser(
        location: Location,
        geofences: List<RadarGeofence>
    ): RadarUser? {
        val cachedUser = RadarState.getLastUser(context)

        val userJson = JSONObject().apply {
            put("location", JSONObject().apply {
                put("coordinates", JSONArray().apply {
                    put(location.longitude)
                    put(location.latitude)
                })
            })
            put("locationAccuracy", location.accuracy)
            put("geofences", RadarGeofence.toJson(geofences.toTypedArray()))
            put("stopped", RadarState.getStopped(context))
            put("foreground", RadarActivityLifecycleCallbacks.foreground)
            put("source", "OFFLINE_DETECTION")

            cachedUser?._id?.let { put("_id", it) }
            cachedUser?.userId?.let { put("userId", it) }
            cachedUser?.deviceId?.let { put("deviceId", it) }
            cachedUser?.description?.let { put("description", it) }
            cachedUser?.metadata?.let { put("metadata", it) }
            cachedUser?.country?.let { put("country", it.toJson()) }
            cachedUser?.state?.let { put("state", it.toJson()) }
            cachedUser?.dma?.let { put("dma", it.toJson()) }
            cachedUser?.postalCode?.let { put("postalCode", it.toJson()) }
            cachedUser?.trip?.let { put("trip", it.toJson()) }
        }
        return RadarUser.fromJson(userJson)
    }

    // endregion
}