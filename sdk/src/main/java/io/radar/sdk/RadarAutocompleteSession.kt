package io.radar.sdk

import android.location.Location
import io.radar.sdk.Radar.RadarAutocompleteSelectCallback
import io.radar.sdk.Radar.RadarGeocodeCallback
import io.radar.sdk.Radar.RadarStatus
import io.radar.sdk.model.RadarAddress
import java.util.UUID

/**
 * One autocomplete session: a single search field and the selection a user makes in it.
 *
 * Create one per address field. Two fields sharing a session also share the server's per-session
 * selection cap and its snapshot window, so typing into one can stop the other's results from
 * enriching — a checkout with billing and shipping wants two sessions, not one.
 *
 * The session mints and owns its token, so there is no token for a caller to get wrong. It stays
 * valid as long as the field is in use; the server expires a session after 20 minutes of
 * inactivity, and the next call through this instance simply opens a fresh one.
 *
 * ```
 * val session = RadarAutocompleteSession()
 * session.autocomplete("brooklyn roasting", near, null, 10, null, null) { status, addresses ->
 *     val chosen = addresses?.firstOrNull() ?: return@autocomplete
 *     session.select(chosen) { selectStatus, enrichedAddress -> }
 * }
 * ```
 */
class RadarAutocompleteSession {

    /**
     * The token this session sends, exposed for correlating with your own analytics. Minted on
     * construction and immutable afterwards, so it is safe to read from any thread.
     */
    val sessionToken: String = UUID.randomUUID().toString()

    /**
     * Autocompletes partial addresses and place names within this session, sorted by relevance.
     *
     * Results carry the opaque handle [select] needs.
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
     * Results carry the opaque handle [select] needs.
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
     * Pass the [RadarAddress] straight back from [autocomplete]; it carries the opaque handle the
     * API attributes the selection with. An address from somewhere else has no handle, so it is
     * logged and not reported, completing with [RadarStatus.ERROR_BAD_REQUEST].
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
     * Pass the [RadarAddress] straight back from [autocomplete]; it carries the opaque handle the
     * API attributes the selection with. An address from somewhere else has no handle, so it is
     * logged and not reported, completing with [RadarStatus.ERROR_BAD_REQUEST].
     *
     * @param[address] The selected address, as returned by [autocomplete].
     * @param[block] A block callback.
     */
    fun select(
        address: RadarAddress?,
        block: (status: RadarStatus, address: RadarAddress?) -> Unit
    ) {
        select(
            address,
            object : RadarAutocompleteSelectCallback {
                override fun onComplete(status: RadarStatus, address: RadarAddress?) {
                    block(status, address)
                }
            }
        )
    }
}
