package io.radar.example.map.overlays

import android.content.Context
import android.location.Location
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.ui.graphics.vector.ImageVector
import io.radar.example.store.TripDestination
import io.radar.sdk.model.RadarCircleGeometry
import io.radar.sdk.model.RadarCoordinate
import io.radar.sdk.model.RadarGeofence
import io.radar.sdk.model.RadarPolygonGeometry
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.FillLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.Point
import org.maplibre.geojson.Polygon

/** Renders geofences from `Radar.searchGeofences(...)` as green tappable shapes. */
class NearbyGeofencesSource : MapOverlaySource() {
    override val id = "nearbyGeofences"
    override val label = "Nearby geofences"
    override val icon: ImageVector = Icons.Filled.Explore

    private val srcId = "src_$id"
    private val fillLayerId = "layer_${id}_fill"
    private val lineLayerId = "layer_${id}_line"

    override val layerIds = listOf(fillLayerId, lineLayerId)
    override val hitLayerIds = listOf(fillLayerId)

    private var features: FeatureCollection = FeatureCollection.fromFeatures(emptyArray<Feature>())

    override fun attach(style: Style) {
        if (style.getSource(srcId) == null) style.addSource(GeoJsonSource(srcId, features))
        if (style.getLayer(fillLayerId) == null) {
            style.addLayer(
                FillLayer(fillLayerId, srcId).withProperties(
                    PropertyFactory.fillColor(OverlayColors.Green),
                    PropertyFactory.fillOpacity(0.15f),
                ),
            )
        }
        if (style.getLayer(lineLayerId) == null) {
            style.addLayer(
                LineLayer(lineLayerId, srcId).withProperties(
                    PropertyFactory.lineColor(OverlayColors.Green),
                    PropertyFactory.lineWidth(1.5f),
                ),
            )
        }
    }

    override suspend fun load(context: Context, near: LatLng?) {
        val ll = near ?: return
        val location = Location("overlay").apply {
            latitude = ll.latitude
            longitude = ll.longitude
        }
        val geofences = searchGeofencesNear(location, radius = 5000, limit = 100)
        features = FeatureCollection.fromFeatures(geofences.mapNotNull { toFeature(it) })
    }

    override fun render(style: Style) {
        (style.getSource(srcId) as? GeoJsonSource)?.setGeoJson(features)
    }

    override fun hitTest(feature: Feature): TripDestination? {
        val gid = feature.getStringProperty("id") ?: return null
        val lat = feature.getNumberProperty("centerLat")?.toDouble() ?: return null
        val lng = feature.getNumberProperty("centerLng")?.toDouble() ?: return null
        return TripDestination.Geofence(
            id = gid,
            tag = feature.getStringProperty("tag"),
            externalId = feature.getStringProperty("externalId"),
            title = feature.getStringProperty("description") ?: "Geofence",
            coordinate = LatLng(lat, lng),
        )
    }

    private fun toFeature(geofence: RadarGeofence): Feature? {
        val (polygon, center) = when (val geom = geofence.geometry) {
            is RadarCircleGeometry -> circlePolygon(geom.center.latitude, geom.center.longitude, geom.radius) to geom.center
            is RadarPolygonGeometry -> polygonFor(geom) to geom.center
            else -> return null
        }
        return Feature.fromGeometry(polygon).apply {
            addStringProperty("id", geofence._id)
            geofence.tag?.let { addStringProperty("tag", it) }
            geofence.externalId?.let { addStringProperty("externalId", it) }
            addStringProperty("description", geofence.description)
            addNumberProperty("centerLat", center.latitude)
            addNumberProperty("centerLng", center.longitude)
        }
    }

    private fun polygonFor(geom: RadarPolygonGeometry): Polygon {
        val coords = geom.coordinates
        return if (!coords.isNullOrEmpty()) {
            val ring = coords.map { Point.fromLngLat(it.longitude, it.latitude) }.toMutableList()
            if (ring.first() != ring.last()) ring.add(ring.first())
            Polygon.fromLngLats(listOf(ring))
        } else {
            circlePolygon(geom.center.latitude, geom.center.longitude, geom.radius)
        }
    }
}
