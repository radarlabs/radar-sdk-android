package io.radar.sdk

import android.content.Context
import android.location.Location
import io.radar.sdk.model.RadarCircleGeometry
import io.radar.sdk.model.RadarCoordinate
import io.radar.sdk.model.RadarEvent
import io.radar.sdk.model.RadarGeofence
import io.radar.sdk.model.RadarPolygonGeometry
import org.json.JSONArray
import org.json.JSONObject
import java.nio.ByteBuffer
import java.util.Date
import java.util.concurrent.atomic.AtomicInteger
import kotlin.random.Random

class RadarOfflineManager(
    private val context: Context,
) {
    private var geofences = arrayOf<RadarGeofence>()
    private var inGeofenceTrackingTags = mutableListOf<String>()
    private var inGeofenceTrackingOptions: RadarTrackingOptions? = null
    private var onTripTrackingOptions: RadarTrackingOptions? = null
    private var defaultTrackingOptions: RadarTrackingOptions? = null
    private var lastSyncTime: Date = Date(0)

    private var fileStorage = RadarFileStorage()

    private companion object {
        private const val SYNC_FREQUENCY_MS = 1000 * 60 * 60 * 4 // every 4 hours

        private val machineIdentifier: ByteArray = run {
            val bytes = ByteArray(5)
            Random.nextBytes(bytes)
            bytes
        }
        // Counter: 3 bytes, wrap around when large; start from random
        private val counter: AtomicInteger = AtomicInteger((Math.random() * 0xFFFFFF).toInt())

        @Synchronized
        fun generateObjectId(): String {
            val id = ByteBuffer.allocate(12)

            // 1. timestamp (4 bytes, big endian)
            val currentSeconds = (System.currentTimeMillis() / 1000).toInt()
            id.putInt(currentSeconds)

            // 2. random/machine ID (3 bytes)
            id.put(machineIdentifier)

            // 3. counter (3 bytes)
            val c = counter.getAndIncrement() and 0xFFFFFF  // mask to 3 bytes
            id.put(((c shr 16) and 0xFF).toByte())
            id.put(((c shr 8) and 0xFF).toByte())
            id.put((c and 0xFF).toByte())

            // Now convert to hex string
            return id.array().joinToString("") { "%02x".format(it) }
        }
    }

    init {
        val data = fileStorage.readJSON(context, "offlineData.json")
        if (data != null) {
            geofences = data.optJSONArray("geofences")?.let { RadarGeofence.fromJson(it) } ?: arrayOf()
            lastSyncTime = data.optString("lastSyncTime").let { RadarUtils.isoStringToDate(it) } ?: Date(0)
            inGeofenceTrackingOptions = data.optJSONObject("inGeofenceTrackingOptions")?.let { RadarTrackingOptions.fromJson(it) }
            onTripTrackingOptions = data.optJSONObject("onTripTrackingOptions")?.let { RadarTrackingOptions.fromJson(it) }
            defaultTrackingOptions = data.optJSONObject("defaultTrackingOptions")?.let { RadarTrackingOptions.fromJson(it) }
            inGeofenceTrackingTags = data.optJSONArray("inGeofenceTrackingTags")?.let { MutableList(it.length()) { i -> it.getString(i) } } ?: mutableListOf()
        }
    }

    fun getOfflineDataRequest(): JSONObject? {
        if (Date().time - lastSyncTime.time < SYNC_FREQUENCY_MS) {
            // data is not stale, don't need to try and sync every time
            return null
        }

        val location = RadarState.getLastLocation(context) ?: return null

        val offlineDataRequest = JSONObject().apply {
            put("geofenceIds", JSONArray(geofences.map { it._id }))
            put("location", "${location.latitude},${location.longitude}")
            put("lastSyncTime", RadarUtils.dateToISOString(lastSyncTime))
        }

        return offlineDataRequest
    }

    fun updateOfflineData(time: Date, offlineData: JSONObject?) {
        if (offlineData == null) {
            return
        }
        val newGeofences = RadarGeofence.fromJson(offlineData.getJSONArray("newGeofences")) ?: arrayOf()
        val removeGeofences = offlineData.getJSONArray("removeGeofences")
        val removeIds = mutableSetOf<String>()
        for (i in 0 until removeGeofences.length()) {
            removeIds.add(removeGeofences.optString(i))
        }

        geofences = geofences.filter { it._id !in removeIds }.toTypedArray() + newGeofences
        inGeofenceTrackingOptions = offlineData.optJSONObject("inGeofenceTrackingOptions")?.let { RadarTrackingOptions.fromJson(it) }
        onTripTrackingOptions = offlineData.optJSONObject("onTripTrackingOptions")?.let { RadarTrackingOptions.fromJson(it) }
        defaultTrackingOptions = offlineData.optJSONObject("defaultTrackingOptions")?.let { RadarTrackingOptions.fromJson(it) }
        inGeofenceTrackingTags = offlineData.optJSONArray("inGeofenceTrackingTags")?.let { MutableList(it.length()) { i -> it.getString(i) } } ?: mutableListOf()
        lastSyncTime = time

        val newData = JSONObject().apply {
            put("geofences", RadarGeofence.toJson(geofences))
            put("lastSyncTime", RadarUtils.dateToISOString(lastSyncTime))
            put("inGeofenceTrackingOptions", offlineData.optJSONObject("inGeofenceTrackingOptions"))
            put("onTripTrackingOptions", offlineData.optJSONObject("onTripTrackingOptions"))
            put("defaultTrackingOptions", offlineData.optJSONObject("defaultTrackingOptions"))
            put("inGeofenceTrackingTags", offlineData.optJSONArray("inGeofenceTrackingTags"))
        }

        fileStorage.writeJSON(context, "offlineData.json", newData)
    }

    private fun createGeofenceEvent(context: Context, geofence: RadarGeofence, location: Location, type: String): JSONObject {
        val now = RadarUtils.dateToISOString(Date())
        return JSONObject().apply {
            put("_id", generateObjectId())
            put("createdAt", now)
            put("actualCreatedAt", now)
            // figure out the import scope issue later
            put("live", RadarUtils.isLive(context))
            put("type", type)
            put("geofence", geofence.toJson())
            put("verification", RadarEvent.RadarEventVerification.UNVERIFY.toString())
            put("confidence", RadarEvent.RadarEventConfidence.LOW) // ??? maybe use accuracy?
            put("duration", 0)
            put("location", JSONObject().apply {
                put("coordinates", JSONArray(listOf(location.longitude, location.latitude)))
            })
            put("locationAccuracy", location.accuracy)
            put("replayed", false)
            put("offline", true)
        }
    }

    // offline track in case online track fails
    fun track(context: Context, params: JSONObject): JSONObject? {
        println("Tracked offline")

        val latitude = (params["latitude"] as? Double) ?: return null
        val longitude = (params["longitude"] as? Double) ?: return null


        val location = Location("")
        location.longitude = longitude
        location.latitude = latitude

        val user = RadarState.getUser(context) ?: return null

        // Events
        // calculate geofences within and generate events
        val events = mutableListOf<JSONObject>()
        val userGeofenceIds = (user.geofences ?: arrayOf()).map { it._id }.toSet()
        val newUserGeofences = mutableListOf<RadarGeofence>()

        for (geofence in geofences) {
            val inside = withinGeofence(geofence, location) ?: continue

            if (inside) {
                if (geofence._id !in userGeofenceIds) {
                    // new geofence that the user is now inside, entry event
                    events.add(createGeofenceEvent(context, geofence, location, "user.entered_geofence"))
                }
                newUserGeofences.add(geofence)
            } else { // outside
                if (geofence._id in userGeofenceIds) {
                    // user was inside this geofence before, exit event
                    events.add(createGeofenceEvent(context, geofence, location, "user.exited_geofence"))
                }
            }
        }

        // User
        // set user fields
        val newUser: JSONObject = user.toJson() // the json value for the updated user
        newUser.put("geofences", JSONArray(newUserGeofences.map { it.toJson() }))
        newUser.put("location", JSONObject().apply {
            put("coordinates", JSONArray(listOf(location.longitude, location.latitude)))
        })

        // RTO
        // find which remote tracking options should be applied
        var trackingOptions: RadarTrackingOptions? = null
        if (inGeofenceTrackingOptions != null &&
            newUserGeofences.any { it.tag != null && inGeofenceTrackingTags.contains(it.tag) }) {
            trackingOptions = inGeofenceTrackingOptions
        } else if (onTripTrackingOptions != null && user.trip != null) {
            trackingOptions = onTripTrackingOptions
        } else if (defaultTrackingOptions != null) {
            trackingOptions = defaultTrackingOptions
        }

        val response = JSONObject().apply {
            put("user", newUser)
            put("events", JSONArray(events))
            put("meta", JSONObject().apply {
                put("trackingOptions", trackingOptions?.toJson())
                put("sdkConfiguration", RadarSettings.getSdkConfiguration(context).toJson())
            })
            put("offline", true)
        }

        return response
    }

    private fun withinGeofence(geofence: RadarGeofence, point: Location): Boolean? {
        var center: RadarCoordinate? = null
        var radius: Double = 100.0

        (geofence.geometry as? RadarCircleGeometry)?.let { geometry ->
            center = geometry.center
            radius = geometry.radius
        }
        (geofence.geometry as? RadarPolygonGeometry)?.let { geometry ->
            center = geometry.center
            radius = geometry.radius
        }

        return center?.let { c ->
            val centerLocation = Location("")
            centerLocation.latitude = c.latitude
            centerLocation.longitude = c.longitude

            return centerLocation.distanceTo(point) <= radius
        }
    }
}
