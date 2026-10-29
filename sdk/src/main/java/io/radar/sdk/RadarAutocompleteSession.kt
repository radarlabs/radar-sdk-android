package io.radar.sdk

import android.location.Location
import io.radar.sdk.Radar.RadarAutocompleteSelectCallback
import io.radar.sdk.Radar.RadarGeocodeCallback
import io.radar.sdk.Radar.RadarStatus
import io.radar.sdk.model.RadarAddress
import io.radar.sdk.model.RadarAutocompleteEnrichedAddress
import java.util.UUID

/**
 * An autocomplete search field and the selection a user makes in it.
 *
 * Create one session per address field.
 *
 * ```
 * val session = RadarAutocompleteSession()
 * session.autocomplete("brooklyn roasting", near, 10) { status, addresses ->
 *     val chosen = addresses?.firstOrNull() ?: return@autocomplete
 *     session.select(chosen) { selectStatus, enrichedAddress -> }
 * }
 * ```
 */
class RadarAutocompleteSession {

    /** The token this session sends. */
    val sessionToken: String = UUID.randomUUID().toString()

    /**
     * Autocompletes partial addresses and place names within this session, sorted by relevance.
     *
     * @see [](https://radar.com/documentation/api#autocomplete)
     *
     * @param[query] The partial address or place name to autocomplete.
     * @param[near] A location for the search.
     * @param[layers] Optional layer filters.
     * @param[limit] The max number of addresses to return. A number between 1 and 100.
     * @param[country] An optional country filter. A string, the unique 2-letter country code.
     * @param[mailable] Whether to only include mailable addresses.
     * @param[callback] A callback.
     */
    @JvmOverloads
    fun autocomplete(
        query: String,
        near: Location? = null,
        layers: Array<String>? = null,
        limit: Int? = null,
        country: String? = null,
        mailable: Boolean? = null,
        callback: RadarGeocodeCallback
    ) {
        Radar.autocomplete(query, near, layers, limit, country, mailable, sessionToken, callback)
    }

    /**
     * Autocompletes partial addresses and place names within this session, sorted by relevance.
     *
     * @see [](https://radar.com/documentation/api#autocomplete)
     *
     * @param[query] The partial address or place name to autocomplete.
     * @param[near] A location for the search.
     * @param[layers] Optional layer filters.
     * @param[limit] The max number of addresses to return. A number between 1 and 100.
     * @param[country] An optional country filter. A string, the unique 2-letter country code.
     * @param[mailable] Whether to only include mailable addresses.
     * @param[block] A block callback.
     */
    fun autocomplete(
        query: String,
        near: Location? = null,
        layers: Array<String>? = null,
        limit: Int? = null,
        country: String? = null,
        mailable: Boolean? = null,
        block: (status: RadarStatus, addresses: Array<RadarAddress>?) -> Unit
    ) {
        autocomplete(
            query,
            near,
            layers,
            limit,
            country,
            mailable,
            object : RadarGeocodeCallback {
                override fun onComplete(status: RadarStatus, addresses: Array<RadarAddress>?) {
                    block(status, addresses)
                }
            }
        )
    }

    /**
     * Autocompletes partial addresses and place names within this session, sorted by relevance.
     *
     * @see [](https://radar.com/documentation/api#autocomplete)
     *
     * @param[query] The partial address or place name to autocomplete.
     * @param[near] A location for the search.
     * @param[limit] The max number of addresses to return. A number between 1 and 100.
     * @param[callback] A callback.
     */
    fun autocomplete(
        query: String,
        near: Location?,
        limit: Int?,
        callback: RadarGeocodeCallback
    ) {
        autocomplete(query, near, null, limit, null, null, callback)
    }

    /**
     * Autocompletes partial addresses and place names within this session, sorted by relevance.
     *
     * @see [](https://radar.com/documentation/api#autocomplete)
     *
     * @param[query] The partial address or place name to autocomplete.
     * @param[near] A location for the search.
     * @param[limit] The max number of addresses to return. A number between 1 and 100.
     * @param[block] A block callback.
     */
    fun autocomplete(
        query: String,
        near: Location?,
        limit: Int?,
        block: (status: RadarStatus, addresses: Array<RadarAddress>?) -> Unit
    ) {
        autocomplete(
            query,
            near,
            limit,
            object : RadarGeocodeCallback {
                override fun onComplete(status: RadarStatus, addresses: Array<RadarAddress>?) {
                    block(status, addresses)
                }
            }
        )
    }

    /**
     * Reports which result the user chose, and returns it enriched where available.
     *
     * Completes with [RadarStatus.ERROR_BAD_REQUEST] if the address did not come from
     * [autocomplete].
     *
     * @param[address] The selected address, as returned by [autocomplete].
     * @param[callback] A callback.
     */
    fun select(
        address: RadarAddress?,
        callback: RadarAutocompleteSelectCallback
    ) {
        Radar.selectAutocompleteAddress(address, callback)
    }

    /**
     * Reports which result the user chose, and returns it enriched where available.
     *
     * Completes with [RadarStatus.ERROR_BAD_REQUEST] if the address did not come from
     * [autocomplete].
     *
     * @param[address] The selected address, as returned by [autocomplete].
     * @param[block] A block callback.
     */
    fun select(
        address: RadarAddress?,
        block: (status: RadarStatus, address: RadarAutocompleteEnrichedAddress?) -> Unit
    ) {
        select(
            address,
            object : RadarAutocompleteSelectCallback {
                override fun onComplete(status: RadarStatus, address: RadarAutocompleteEnrichedAddress?) {
                    block(status, address)
                }
            }
        )
    }
}
