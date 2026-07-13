package io.radar.example.map.overlays

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.ui.graphics.vector.ImageVector
import io.radar.example.store.TripBuilderStore
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.LineString
import org.maplibre.geojson.Point

/** Blue polyline + dots tracing the device's location updates during an active trip. */
class TripBreadcrumbsSource(private val store: TripBuilderStore) : MapOverlaySource() {
    override val id = "tripBreadcrumbs"
    override val label = "Trip breadcrumbs"
    override val icon: ImageVector = Icons.Filled.Timeline
    override val isTripModeWhitelisted = true
    override val userToggleable = false

    private val lineSrcId = "src_${id}_line"
    private val dotSrcId = "src_${id}_dots"
    private val lineLayerId = "layer_${id}_line"
    private val dotLayerId = "layer_${id}_dots"

    override val layerIds = listOf(lineLayerId, dotLayerId)

    override fun attach(style: Style) {
        if (style.getSource(lineSrcId) == null) style.addSource(GeoJsonSource(lineSrcId))
        if (style.getSource(dotSrcId) == null) style.addSource(GeoJsonSource(dotSrcId))
        if (style.getLayer(lineLayerId) == null) {
            style.addLayer(
                LineLayer(lineLayerId, lineSrcId).withProperties(
                    PropertyFactory.lineColor(OverlayColors.Blue),
                    PropertyFactory.lineWidth(3f),
                ),
            )
        }
        if (style.getLayer(dotLayerId) == null) {
            style.addLayer(
                CircleLayer(dotLayerId, dotSrcId).withProperties(
                    PropertyFactory.circleColor(OverlayColors.Blue),
                    PropertyFactory.circleRadius(3f),
                ),
            )
        }
    }

    override fun render(style: Style) {
        val points = store.breadcrumbs.map { Point.fromLngLat(it.longitude, it.latitude) }
        val lineCollection = if (points.size >= 2) {
            FeatureCollection.fromFeature(Feature.fromGeometry(LineString.fromLngLats(points)))
        } else {
            FeatureCollection.fromFeatures(emptyArray<Feature>())
        }
        val dotCollection = FeatureCollection.fromFeatures(points.map { Feature.fromGeometry(it) })
        (style.getSource(lineSrcId) as? GeoJsonSource)?.setGeoJson(lineCollection)
        (style.getSource(dotSrcId) as? GeoJsonSource)?.setGeoJson(dotCollection)
    }
}
