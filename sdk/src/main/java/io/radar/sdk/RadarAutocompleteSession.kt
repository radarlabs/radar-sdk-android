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
 * session.autocomplete("brooklyn roasting", near, limit = 10) { status, addresses ->
 *     val chosen = addresses?.firstOrNull() ?: return@autocomplete
 *     session.select(chosen) { selectStatus, enrichedAddress -> }
 * }
 * ```
 */
class RadarAutocompleteSession internal constructor(
    /** The token this session sends. */
    val sessionToken: String
) {

    /**
     * Opens a session with a newly minted token.
     */
    constructor() : this(UUID.randomUUID().toString())

    init {
        // Resuming from a token is for wrapper SDKs, whose Java bridges can reach this internal
        // constructor. The server packs the token into each resultId as raw UUID bytes.
        require(UUID_PATTERN.matches(sessionToken)) { "sessionToken must be a UUID" }
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
     * Reports which result the user chose. Selections are how autocomplete quality is measured
     * and improved.
     *
     * Returns the enriched address where available.
     *
     * Completes with [RadarStatus.ERROR_BAD_REQUEST] if the address did not come from
     * [autocomplete].
     *
     * @param[address] The selected result, as returned by [autocomplete].
     * @param[callback] An optional callback.
     */
    @JvmOverloads
    fun select(
        address: RadarAddress,
        callback: RadarAutocompleteSelectCallback? = null
    ) {
        Radar.selectAutocompleteAddress(address, callback)
    }

    /**
     * Reports which result the user chose. Selections are how autocomplete quality is measured
     * and improved.
     *
     * Returns the enriched address where available.
     *
     * Completes with [RadarStatus.ERROR_BAD_REQUEST] if the address did not come from
     * [autocomplete].
     *
     * @param[address] The selected result, as returned by [autocomplete].
     * @param[block] A block callback.
     */
    fun select(
        address: RadarAddress,
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

    private companion object {
        val UUID_PATTERN = Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")
    }
}
