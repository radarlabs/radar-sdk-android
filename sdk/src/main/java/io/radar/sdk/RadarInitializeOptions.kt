package io.radar.sdk

import org.json.JSONObject
import java.util.Date

data class RadarInitializeOptions(
    var fraud: Boolean = false,
    var userId: String? = null,
    var metadata: JSONObject? = null,
) {
    companion object {

        internal const val KEY_FRAUD = "fraud"
        internal const val KEY_USER_ID = "userId"
        internal const val KEY_METADATA = "metadata"



        @JvmStatic
        fun fromJson(obj: JSONObject): RadarInitializeOptions {
            val fraud = if (obj.isNull(KEY_FRAUD)) null else obj.optBoolean(KEY_FRAUD)
            val userId = if (obj.isNull(KEY_USER_ID)) null else obj.optString(KEY_USER_ID)
            val metadata = if (obj.isNull(KEY_METADATA)) null else obj.optJson(KEY_METADATA)

            return RadarInitializeOptions(fraud, userId, metadata)
        }
    }

    fun toJson(): JSONObject {
        val obj = JSONObject()

        obj.put(KEY_FRAUD, fraud)
        obj.put(KEY_USER_ID, userId)
        obj.put(KEY_METADATA, metadata)

        return obj
    }
}
