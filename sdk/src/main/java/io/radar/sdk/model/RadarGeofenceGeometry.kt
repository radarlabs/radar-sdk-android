package io.radar.sdk.model

/**
 * Represents the geometry of a geofence.
 */
sealed class RadarGeofenceGeometry

/**
 * Represents the geometry of a circle geofence.
 */
class RadarCircleGeometry(
    /**
     * The center of the circle geofence.
     */
    val center: RadarCoordinate,

    /**
     * The radius of the circle geofence in meters.
     */
    val radius: Double
) : RadarGeofenceGeometry()

/**
 * Represents the geometry of a polygon geofence.
 */
class RadarPolygonGeometry(
    /**
     * The geometry of the polygon geofence. A closed ring of coordinates.
     */
    val coordinates: Array<RadarCoordinate>,

    /**
     * The calculated centroid of the polygon geofence.
     */
    val center: RadarCoordinate,

    /**
     * The calculated radius of the polygon geofence in meters.
     */
    val radius: Double
) : RadarGeofenceGeometry()