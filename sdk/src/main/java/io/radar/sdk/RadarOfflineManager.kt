package io.radar.sdk

import android.content.Context
import android.location.Location
import io.radar.sdk.model.RadarCircleGeometry
import io.radar.sdk.model.RadarCoordinate
import io.radar.sdk.model.RadarEvent
import io.radar.sdk.model.RadarGeofence
import io.radar.sdk.model.RadarPolygonGeometry
import org.json.JSONObject
import java.nio.ByteBuffer
import java.util.Date
import java.util.concurrent.atomic.AtomicInteger
import kotlin.random.Random


// Utility to generate an ObjectId with the ObjectId specification
object ObjectIdGenerator {
    private val machineIdentifier: ByteArray = run {
        val bytes = ByteArray(5)
        Random.nextBytes(bytes)
        bytes
    }
    // Counter: 3 bytes, wrap around when large; start from random
    private val counter: AtomicInteger = AtomicInteger((Math.random() * 0xFFFFFF).toInt())

    @Synchronized
    fun generate(): String {
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

class RadarOfflineManager {
    private var geofences = arrayOf<RadarGeofence>()
    private var inGeofenceTrackingTags = arrayOf<String>()
    private var inGeofenceTrackingOptions: RadarTrackingOptions? = null
    private var onTripTrackingOptions: RadarTrackingOptions? = null
    private var defaultTrackingOptions: RadarTrackingOptions? = null

    private var fileStorage = RadarFileStorage()

    fun sync(context: Context, newGeofences: Array<RadarGeofence>?) {
        val data = fileStorage.readJSON(context, "offlineData.json") ?: return
        data.getJSONArray("geofences")

        geofences = RadarGeofence.fromJson(data.getJSONArray("geofences")) ?: arrayOf()

        if (newGeofences != null) {
            geofences = newGeofences

            val newData = JSONObject().apply {
                put("geofences", RadarGeofence.toJson(geofences))
            }

            fileStorage.writeJSON(context, "offlineData.json", newData)
        }
    }

    fun createGeofenceEvent(context: Context, geofence: RadarGeofence, location: Location, type: String): JSONObject {
        val now = RadarUtils.dateToISOString(Date())
        return JSONObject().apply {
            put("_id", ObjectIdGenerator.generate())
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
                put("coordinates", arrayOf(location.longitude, location.latitude))
            })
            put("locationAccuracy", location.accuracy)
            put("replayed", false)
            put("metadata", JSONObject().apply {
                put("offline", true)
            })
        }
    }

    // offline track in case online track fails
    fun track(context: Context, params: JSONObject): JSONObject? {
        print("Tracked offline")

        val latitude = (params["latitude"] as? Double) ?: return null
        val longitude = (params["longitude"] as? Double) ?: return null


        val location = Location("")
        location.longitude = longitude
        location.latitude = latitude

        val user = RadarSettings.getUser(context) ?: return null

        // Events
        // calculate geofences within and generate events
        val events = mutableListOf<JSONObject>()
        val userGeofences = user.geofences ?: arrayOf()
        val newUserGeofences = mutableListOf<RadarGeofence>()

        for (geofence in geofences) {
            val inside = withinGeofence(geofence, location) ?: continue

            if (inside) {
                if (!userGeofences.contains(geofence)) {
                    // new geofence that the user is now inside, entry event
                    events.add(createGeofenceEvent(context, geofence, location, "user.entered_geofence"))
                }
                newUserGeofences.add(geofence)
            } else { // outside
                if (userGeofences.contains(geofence)) {
                    // user was inside this geofence before, exit event
                    events.add(createGeofenceEvent(context, geofence, location, "user.exited_geofence"))
                }
            }
        }

        // User
        // set user fields
        val newUser: JSONObject = user.toJson() // the json value for the updated user
        newUser.put("geofences", newUserGeofences.map{ it.toJson() })
        newUser.put("location", JSONObject().apply {
            put("coordinates", arrayOf(location.longitude, location.latitude))
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
            put("events", events)
            put("meta", JSONObject().apply {
                put("trackingOptions", trackingOptions?.toJson())
                put("sdkConfiguration", RadarSettings.getSdkConfiguration(context).toJson())
            })
            put("offline", true)
        }

        print(response)
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