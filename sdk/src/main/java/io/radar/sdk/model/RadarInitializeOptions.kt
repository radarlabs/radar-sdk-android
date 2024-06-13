package io.radar.sdk

import org.json.JSONObject
import io.radar.sdk.Radar.RadarLocationServicesProvider

data class RadarInitializeOptions(
    /**
     * A boolean indicating whether to enable additional fraud detection signals for location verification.
     */
    var fraud: Boolean = false,

    /**
     * An optional stable unique ID for the user to set upon initialization. No-op if null.
     */
    var userId: String? = null,

    /**
     * An optional set of custom key-value pairs for the user. Must have 16 or fewer keys and values of type string, boolean, or number. No-op if null. 
     */
    var metadata: JSONObject? = null,

    /**
     * An optional receiver for the client-side delivery of events.
     */
    var receiver: RadarReceiver? = null,

    /**
     * The location services provider.
     */
    var provider: RadarLocationServicesProvider = RadarLocationServicesProvider.GOOGLE,
)
