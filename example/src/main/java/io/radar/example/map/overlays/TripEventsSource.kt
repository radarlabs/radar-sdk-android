package io.radar.example.map.overlays

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.ui.graphics.vector.ImageVector
import io.radar.example.store.TripBuilderStore
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.Point

/** Purple pins for each trip event captured during an active trip. */
class TripEventsSource(private val store: TripBuilderStore) : MapOverlaySource() {
    override val id = "tripEvents"
    override val label = "Trip events"
    override val icon: ImageVector = Icons.Filled.Bolt
    override val isTripModeWhitelisted = true
    override val userToggleable = false

    private val srcId = "src_$id"
    private val circleLayerId = "layer_${id}_circle"

    override val layerIds = listOf(circleLayerId)

    override fun attach(style: Style) {
        if (style.getSource(srcId) == null) style.addSource(GeoJsonSource(srcId))
        if (style.getLayer(circleLayerId) == null) {
            style.addLayer(
                CircleLayer(circleLayerId, srcId).withProperties(
                    PropertyFactory.circleColor(OverlayColors.Purple),
                    PropertyFactory.circleRadius(6f),
                    PropertyFactory.circleStrokeColor(0xFFFFFFFF.toInt()),
                    PropertyFactory.circleStrokeWidth(1.5f),
                ),
            )
        }
    }

    override fun render(style: Style) {
        val features = store.tripEventMarkers.map { marker ->
            Feature.fromGeometry(Point.fromLngLat(marker.coordinate.longitude, marker.coordinate.latitude))
        }
        (style.getSource(srcId) as? GeoJsonSource)?.setGeoJson(FeatureCollection.fromFeatures(features))
    }
}
