package io.radar.sdk.model

import io.radar.sdk.model.RadarUserInsightsLocation.RadarUserInsightsLocationType.HOME
import io.radar.sdk.model.RadarUserInsightsLocation.RadarUserInsightsLocationType.OFFICE
import org.json.JSONException
import org.json.JSONObject
import java.text.ParseException

/**
 * Represents the learned home, work, and traveling state and locations of the current user. For more information about Insights, see [](https://radar.io/documentation/insights).
 *
 * @see [](https://radar.io/documentation/insights)
 */
class RadarUserInsights private constructor(
    /**
     * The learned home location of the user. May be `null` if not yet learned, or if Insights is turned off.
     */
    val homeLocation: RadarUserInsightsLocation?,

    /**
     * The learned work location of the user. May be `null` if not yet learned, or if Insights is turned off.
     */
    val officeLocation: RadarUserInsightsLocation?,

    /**
     * The state of the user, based on learned home and work locations.
     */
    val state: RadarUserInsightsState?
) {

    internal companion object {
        internal const val FIELD_LOCATIONS = "locations"
        internal const val FIELD_STATE = "state"

        @Throws(JSONException::class, ParseException::class)
        fun fromJson(obj: JSONObject): RadarUserInsights {
            var homeLocation: RadarUserInsightsLocation? = null
            var officeLocation: RadarUserInsightsLocation? = null
            obj.optJSONArray(FIELD_LOCATIONS)?.let { locations ->
                for (i in 0 until locations.length()) {
                    val location = RadarUserInsightsLocation.fromJson(locations.optJSONObject(i))
                    when (location?.type) {
                        HOME -> homeLocation = location
                        OFFICE -> officeLocation = location
                        else -> {}
                    }
                }
            }

            val state = obj.optJSONObject(FIELD_STATE)?.let(RadarUserInsightsState.Companion::fromJson)

            return RadarUserInsights(homeLocation, officeLocation, state)
        }
    }

}
