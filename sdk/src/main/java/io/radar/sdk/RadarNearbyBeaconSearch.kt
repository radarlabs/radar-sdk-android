package io.radar.sdk

import android.location.Location

/**
 * The nearby beacon search shared by one-shot ranging (`RadarOneShotBeaconManager`) and
 * continuous ranging (`RadarContinuousBeaconManager`).
 *
 * When continuous ranging can't serve `trackVerified`, `trackVerified` makes one search, ranges its
 * result once with the one-shot manager, and hands the same result to the continuous manager with
 * `RadarContinuousBeaconManager.onSearched`. The continuous manager uses this search for its own
 * periodic refreshes too, so both managers range the same beacons, and a request that continuous
 * ranging can't serve searches only once.
 */
internal object RadarNearbyBeaconSearch {

    // Radius and maximum number of beacons for nearby beacon searches.
    const val RADIUS_METERS = 1000
    const val LIMIT = 10

    /**
     * Searches beacons near `location`. On failure, `callback` gets the last saved beacons, which
     * may not be near `location`.
     */
    fun search(location: Location, callback: RadarApiClient.RadarSearchBeaconsApiCallback) {
        Radar.apiClient.searchBeacons(location, RADIUS_METERS, LIMIT, callback, false)
    }
}
