package io.radar.sdk.model

import org.json.JSONObject

/**
 * Represents learned home, work, and travel state returned with a track response.
 */
class RadarUserLocationInsights(
    /**
     * A boolean indicating whether the user is currently at their learned home location.
     */
    val atHome: Boolean,

    /**
     * A boolean indicating whether the user is currently at their learned work location.
     */
    val atWork: Boolean,

    /**
     * A boolean indicating whether the user is currently traveling away from home.
     */
    val traveling: Boolean,

    /**
     * A nullable boolean indicating whether the user is currently commuting.
     */
    val commuting: Boolean?
) {
    internal companion object {
        private const val FIELD_AT_HOME = "atHome"
        private const val FIELD_AT_WORK = "atWork"
        private const val FIELD_TRAVELING = "traveling"
        private const val FIELD_COMMUTING = "commuting"

        @JvmStatic
        fun fromJson(obj: JSONObject?): RadarUserLocationInsights? {
            if (obj == null) {
                return null
            }

            return RadarUserLocationInsights(
                obj.optBoolean(FIELD_AT_HOME),
                obj.optBoolean(FIELD_AT_WORK),
                obj.optBoolean(FIELD_TRAVELING),
                if (obj.has(FIELD_COMMUTING)) obj.optBoolean(FIELD_COMMUTING) else null
            )
        }
    }

    fun toJson(): JSONObject {
        val obj = JSONObject()
        obj.putOpt(FIELD_AT_HOME, this.atHome)
        obj.putOpt(FIELD_AT_WORK, this.atWork)
        obj.putOpt(FIELD_TRAVELING, this.traveling)
        obj.putOpt(FIELD_COMMUTING, this.commuting)
        return obj
    }
}
