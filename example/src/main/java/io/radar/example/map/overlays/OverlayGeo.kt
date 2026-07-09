package io.radar.example.map.overlays

import android.content.Context
import android.location.Location
import io.radar.sdk.Radar
import io.radar.sdk.model.RadarGeofence
import io.radar.sdk.model.RadarPlace
import kotlinx.coroutines.suspendCancellableCoroutine
import org.maplibre.geojson.Point
import org.maplibre.geojson.Polygon
import kotlin.coroutines.resume
import kotlin.math.cos
import kotlin.math.sin

/** Shared ARGB colors for map overlays. */
object OverlayColors {
    val Green = 0xFF16A34A.toInt() // nearby geofences
    val Teal = 0xFF0D9488.toInt() // nearby places
    val Purple = 0xFF7C3AED.toInt() // synced data / trip events
    val Blue = 0xFF2563EB.toInt() // trip breadcrumbs / pending legs
    val Orange = 0xFFEA580C.toInt() // current trip leg
    val Gray = 0xFF6B7280.toInt() // completed legs
    val Red = 0xFFDC2626.toInt() // trip destination / canceled legs
}

/** Approximate a circle of [radiusInMeters] around ([lat], [lng]) as a closed polygon ring. */
fun circlePolygon(lat: Double, lng: Double, radiusInMeters: Double, points: Int = 64): Polygon {
    val coordinates = mutableListOf<Point>()
    for (i in 0 until points) {
        val angle = 2 * Math.PI * i / points
        val dx = radiusInMeters * cos(angle)
        val dy = radiusInMeters * sin(angle)
        val deltaLat = dy / 111320.0
        val deltaLng = dx / (111320.0 * cos(Math.toRadians(lat)))
        coordinates.add(Point.fromLngLat(lng + deltaLng, lat + deltaLat))
    }
    if (coordinates.isNotEmpty()) coordinates.add(coordinates[0])
    return Polygon.fromLngLats(listOf(coordinates))
}

/** Suspend wrapper around `Radar.searchGeofences(...)` (block variant). */
suspend fun searchGeofencesNear(near: Location, radius: Int, limit: Int): Array<RadarGeofence> =
    suspendCancellableCoroutine { cont ->
        Radar.searchGeofences(near, radius, null, null, limit, true) { _, _, geofences ->
            if (cont.isActive) cont.resume(geofences ?: emptyArray())
        }
    }

/** Suspend wrapper around `Radar.searchPlaces(...)` (block variant). */
suspend fun searchPlacesNear(near: Location, radius: Int, limit: Int): Array<RadarPlace> =
    suspendCancellableCoroutine { cont ->
        Radar.searchPlaces(near, radius, null, null, null, null, limit) { _, _, places ->
            if (cont.isActive) cont.resume(places ?: emptyArray())
        }
    }

/** Read the SDK's synced offline geofences from `filesDir/RadarSDK/offlineData.json`. */
fun readOfflineGeofences(context: Context): List<OfflineGeofence> {
    val file = java.io.File(java.io.File(context.filesDir, "RadarSDK"), "offlineData.json")
    if (!file.exists()) return emptyList()
    return try {
        val json = org.json.JSONObject(file.readBytes().toString(Charsets.UTF_8))
        val geofences = json.optJSONArray("geofences") ?: return emptyList()
        (0 until geofences.length()).mapNotNull { i ->
            val obj = geofences.optJSONObject(i) ?: return@mapNotNull null
            val center = obj.optJSONObject("geometryCenter")?.optJSONArray("coordinates") ?: return@mapNotNull null
            OfflineGeofence(
                id = obj.optString("_id"),
                tag = obj.optString("tag").takeIf { it.isNotEmpty() },
                externalId = obj.optString("externalId").takeIf { it.isNotEmpty() },
                description = obj.optString("description"),
                latitude = center.optDouble(1),
                longitude = center.optDouble(0),
                radius = obj.optDouble("geometryRadius", 100.0),
            )
        }
    } catch (e: Exception) {
        emptyList()
    }
}

/** A geofence parsed from the SDK's synced offline data file. */
data class OfflineGeofence(
    val id: String,
    val tag: String?,
    val externalId: String?,
    val description: String,
    val latitude: Double,
    val longitude: Double,
    val radius: Double,
)
