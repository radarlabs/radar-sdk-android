package io.radar.example.map.overlays

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Route
import androidx.compose.ui.graphics.vector.ImageVector
import io.radar.example.store.TripBuilderStore
import io.radar.sdk.model.RadarTripLeg
import org.maplibre.android.maps.Style
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.Point

/**
 * Trip-leg destination markers colored by leg status: current leg = orange, pending = blue,
 * completed = gray, canceled/expired = red.
 */
class TripGeofencesSource(private val store: TripBuilderStore) : MapOverlaySource() {
    override val id = "tripGeofences"
    override val label = "Trip legs"
    override val icon: ImageVector = Icons.Filled.Route
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
                    PropertyFactory.circleColor(Expression.toColor(Expression.get("color"))),
                    PropertyFactory.circleRadius(11f),
                    PropertyFactory.circleOpacity(0.35f),
                    PropertyFactory.circleStrokeColor(Expression.toColor(Expression.get("color"))),
                    PropertyFactory.circleStrokeWidth(2f),
                ),
            )
        }
    }

    override fun render(style: Style) {
        val trip = store.activeTrip
        val currentLegId = trip?.currentLegId
        val features = trip?.legs.orEmpty().mapNotNull { leg ->
            val coord = leg.coordinates ?: return@mapNotNull null
            Feature.fromGeometry(Point.fromLngLat(coord.longitude, coord.latitude)).apply {
                addStringProperty("color", colorForLeg(leg, currentLegId))
            }
        }
        (style.getSource(srcId) as? GeoJsonSource)?.setGeoJson(FeatureCollection.fromFeatures(features))
    }

    private fun colorForLeg(leg: RadarTripLeg, currentLegId: String?): String {
        if (leg._id != null && leg._id == currentLegId) return "#EA580C" // orange (current)
        return when (leg.status) {
            RadarTripLeg.RadarTripLegStatus.COMPLETED -> "#6B7280" // gray
            RadarTripLeg.RadarTripLegStatus.CANCELED,
            RadarTripLeg.RadarTripLegStatus.EXPIRED,
            -> "#DC2626" // red
            else -> "#2563EB" // blue (pending / other)
        }
    }
}
