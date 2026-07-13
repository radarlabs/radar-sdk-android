package io.radar.example.map.overlays

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Flag
import androidx.compose.ui.graphics.vector.ImageVector
import io.radar.example.store.TripBuilderStore
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.Point

/** Red pin(s) at the active trip's destination(s). */
class TripDestinationSource(private val store: TripBuilderStore) : MapOverlaySource() {
    override val id = "tripDestination"
    override val label = "Trip destination"
    override val icon: ImageVector = Icons.Filled.Flag
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
                    PropertyFactory.circleColor(OverlayColors.Red),
                    PropertyFactory.circleRadius(9f),
                    PropertyFactory.circleStrokeColor(0xFFFFFFFF.toInt()),
                    PropertyFactory.circleStrokeWidth(2f),
                ),
            )
        }
    }

    override fun render(style: Style) {
        val trip = store.activeTrip
        val features = mutableListOf<Feature>()
        trip?.destinationLocation?.let { dest ->
            features.add(Feature.fromGeometry(Point.fromLngLat(dest.longitude, dest.latitude)))
        }
        trip?.legs?.forEach { leg ->
            leg.coordinates?.let { coord ->
                features.add(Feature.fromGeometry(Point.fromLngLat(coord.longitude, coord.latitude)))
            }
        }
        (style.getSource(srcId) as? GeoJsonSource)?.setGeoJson(FeatureCollection.fromFeatures(features))
    }
}
