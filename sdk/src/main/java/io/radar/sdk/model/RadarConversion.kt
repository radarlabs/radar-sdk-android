package io.radar.sdk.model

import io.radar.sdk.RadarUtils
import java.util.Date
import org.json.JSONObject

/**
 * Represents a conversion logged with `Radar.logConversion()`.
 */
class RadarConversion internal constructor(
    /**
     * The Radar ID of the conversion.
     */
    val _id: String,

    /**
     * The name of the conversion, as passed to `Radar.logConversion()`.
     */
    val name: String,

    /**
     * The datetime of the conversion. The server sets it when it receives the conversion.
     */
    val createdAt: Date,

    /**
     * The datetime when the conversion was created on the server. May be `null` if the server did not return it.
     */
    val actualCreatedAt: Date?,

    /**
     * A boolean indicating whether the conversion was logged with your live API key.
     */
    val live: Boolean,

    /**
     * The metadata of the conversion. May be `null` if the conversion had no metadata.
     */
    val metadata: JSONObject?
) {

    internal companion object {
        private const val FIELD_ID = "_id"
        private const val FIELD_TYPE = "type"
        private const val FIELD_NAME = "name"
        private const val FIELD_CREATED_AT = "createdAt"
        private const val FIELD_ACTUAL_CREATED_AT = "actualCreatedAt"
        private const val FIELD_LIVE = "live"
        private const val FIELD_METADATA = "metadata"

        // The value that `RadarEvent` wrote as `type` for a conversion. Hybrid SDK wrappers read it.
        private const val TYPE_CUSTOM = "custom"

        /**
         * Parses the `event` object of a `POST /v1/events` response. Returns `null` for a `null` object, or a
         * missing `_id`, `type`, or `createdAt`. The API docs' sample conversion response has no
         * `actualCreatedAt`, so it is optional.
         */
        @JvmStatic
        fun fromJson(obj: JSONObject?): RadarConversion? {
            if (obj == null) {
                return null
            }

            val id = obj.opt(FIELD_ID) as? String ?: return null
            val name = obj.opt(FIELD_TYPE) as? String ?: return null
            val createdAt = RadarEventParsing.date(obj, FIELD_CREATED_AT) ?: return null

            return RadarConversion(
                _id = id,
                name = name,
                createdAt = createdAt,
                actualCreatedAt = RadarEventParsing.date(obj, FIELD_ACTUAL_CREATED_AT),
                live = obj.optBoolean(FIELD_LIVE),
                metadata = RadarEventParsing.metadata(obj, FIELD_METADATA)
            )
        }
    }

    /**
     * Writes the keys that `RadarEvent.toJson()` wrote for a conversion, with `type` set to `"custom"`, and adds
     * `name`. Hybrid SDK wrappers read these keys.
     */
    fun toJson(): JSONObject {
        val obj = JSONObject()
        obj.put(FIELD_ID, _id)
        obj.put(FIELD_TYPE, TYPE_CUSTOM)
        obj.put(FIELD_NAME, name)
        obj.put(FIELD_LIVE, live)
        obj.putOpt(FIELD_METADATA, metadata)
        obj.put(FIELD_CREATED_AT, RadarUtils.dateToISOString(createdAt))
        obj.putOpt(FIELD_ACTUAL_CREATED_AT, RadarUtils.dateToISOString(actualCreatedAt))
        return obj
    }
}
