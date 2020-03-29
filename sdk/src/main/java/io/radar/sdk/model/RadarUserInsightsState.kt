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
    val traveling: Boolean,

    /**
    * A boolean indicating whether the user is commuting, based on learned home and work locations.
    */
    val commuting: Boolean
) {

    internal companion object {
        private const val FIELD_HOME = "home"
        private const val FIELD_OFFICE = "office"
        private const val FIELD_TRAVELING = "traveling"
        private const val FIELD_COMMUTING = "commuting"

        @Throws(JSONException::class)
        fun fromJson(obj: JSONObject?): RadarUserInsightsState? {
            if (obj == null) {
                return null
            }

            val home = obj.optBoolean(FIELD_HOME)
            val office = obj.optBoolean(FIELD_OFFICE)
            val traveling = obj.optBoolean(FIELD_TRAVELING)
            val commuting = obj.optBoolean(FIELD_COMMUTING)

            return RadarUserInsightsState(home, office, traveling, commuting)
        }
    }

    fun toJson(): JSONObject {
        val obj = JSONObject()
        obj.putOpt(FIELD_HOME, this.home)
        obj.putOpt(FIELD_OFFICE, this.office)
        obj.putOpt(FIELD_TRAVELING, this.traveling)
        obj.putOpt(FIELD_COMMUTING, this.commuting)
        return obj
    }

}
