package io.radar.sdk.model

import org.json.JSONException
import org.json.JSONObject

/**
 * Represents the learned home, work, and traveling state of the current user. For more information about Insights, see [](https://radar.io/documentation/insights).
 *
 * @see [](https://radar.io/documentation/insights)
 */
class RadarUserInsightsState(
    /**
     * A boolean indicating whether the user is at home, based on learned home location.
     */
    val home: Boolean,

    /**
     * A boolean indicating whether the user is at work, based on learned work location.
     */
    val office: Boolean,

    /**
     * A boolean indicating whether the user is traveling, based on learned home location.
     */
    val traveling: Boolean
) {

    internal companion object {
        private const val FIELD_HOME = "home"
        private const val FIELD_OFFICE = "office"
        private const val FIELD_TRAVELING = "traveling"

        @Throws(JSONException::class)
        fun fromJson(obj: JSONObject): RadarUserInsightsState {
            val home = obj.optBoolean(FIELD_HOME)
            val office = obj.optBoolean(FIELD_OFFICE)
            val traveling = obj.optBoolean(FIELD_TRAVELING)

            return RadarUserInsightsState(home, office, traveling)
        }
    }

}
