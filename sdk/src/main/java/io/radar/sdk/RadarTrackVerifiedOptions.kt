package io.radar.sdk

/**
 * Options for [Radar.trackVerified].
 *
 * @param[beacons] A boolean indicating whether to range beacons.
 * @param[desiredAccuracy] The desired accuracy.
 * @param[reason] An optional reason, displayed in the dashboard and reports.
 * @param[transactionId] An optional transaction ID, displayed in the dashboard and reports.
 */
data class RadarTrackVerifiedOptions(
    val beacons: Boolean = false,
    val desiredAccuracy: RadarTrackingOptions.RadarTrackingOptionsDesiredAccuracy =
        RadarTrackingOptions.RadarTrackingOptionsDesiredAccuracy.MEDIUM,
    val reason: String? = null,
    val transactionId: String? = null
) {
    class Builder {
        private var beacons: Boolean = false
        private var desiredAccuracy: RadarTrackingOptions.RadarTrackingOptionsDesiredAccuracy =
            RadarTrackingOptions.RadarTrackingOptionsDesiredAccuracy.MEDIUM
        private var reason: String? = null
        private var transactionId: String? = null

        fun beacons(beacons: Boolean) = apply { this.beacons = beacons }
        fun desiredAccuracy(desiredAccuracy: RadarTrackingOptions.RadarTrackingOptionsDesiredAccuracy) = apply { this.desiredAccuracy = desiredAccuracy }
        fun reason(reason: String?) = apply { this.reason = reason }
        fun transactionId(transactionId: String?) = apply { this.transactionId = transactionId }

        fun build() = RadarTrackVerifiedOptions(
            beacons = beacons,
            desiredAccuracy = desiredAccuracy,
            reason = reason,
            transactionId = transactionId
        )
    }

    companion object {
        @JvmStatic
        fun builder() = Builder()
    }
}
