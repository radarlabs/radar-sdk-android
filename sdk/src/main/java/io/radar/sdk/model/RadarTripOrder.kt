package io.radar.sdk.model

import io.radar.sdk.RadarUtils
import org.json.JSONArray
import org.json.JSONObject
import java.util.Date

/**
 * Represents a trip order.
 *
 * @see [](https://radar.com/documentation/trip-tracking)
 */
class RadarTripOrder(
    /**
     * The Radar ID of the trip order.
     */
    val _id: String,

    /**
     * The optional GUID of the trip order.
     */
    val guid: String? = null,

    /**
     * The optional handoff mode of the trip order.
     */
    val handoffMode: String? = null,

    /**
     * The status of the trip order.
     */
    val status: RadarTripOrderStatus = RadarTripOrderStatus.UNKNOWN,

    /**
     * The optional datetime when the order was fired.
     */
    val firedAt: Date? = null,

    /**
     * The optional number of fired attempts.
     */
    val firedAttempts: Int? = null,

    /**
     * The optional reason why the order was fired.
     */
    val firedReason: String? = null,

    /**
     * The datetime when the order was last updated.
     */
    val updatedAt: Date
) {

    /**
     * The statuses for trip orders.
     */
    enum class RadarTripOrderStatus {
        /** Unknown */
        UNKNOWN,
        /** `pending` */
        PENDING,
        /** `fired` */
        FIRED,
        /** `canceled` */
        CANCELED,
        /** `completed` */
        COMPLETED
    }

    internal companion object {
        private const val FIELD_ID = "id"
        private const val FIELD_GUID = "guid"
        private const val FIELD_HANDOFF_MODE = "handoffMode"
        private const val FIELD_STATUS = "status"
        private const val FIELD_FIRED_AT = "firedAt"
        private const val FIELD_FIRED_ATTEMPTS = "firedAttempts"
        private const val FIELD_FIRED_REASON = "firedReason"
        private const val FIELD_UPDATED_AT = "updatedAt"

        /**
         * Creates a [RadarTripOrder] from a JSON object.
         */
        @JvmStatic
        fun fromJson(obj: JSONObject?): RadarTripOrder? {
            if (obj == null) {
                return null
            }

            val id = obj.optString(FIELD_ID)
            if (id.isNullOrEmpty()) {
                return null
            }

            val guid: String? = obj.optString(FIELD_GUID).takeIf { it.isNotEmpty() }
            val handoffMode: String? = obj.optString(FIELD_HANDOFF_MODE).takeIf { it.isNotEmpty() }

            val status: RadarTripOrderStatus = when (obj.optString(FIELD_STATUS)) {
                "pending" -> RadarTripOrderStatus.PENDING
                "fired" -> RadarTripOrderStatus.FIRED
                "canceled" -> RadarTripOrderStatus.CANCELED
                "completed" -> RadarTripOrderStatus.COMPLETED
                else -> RadarTripOrderStatus.UNKNOWN
            }

            val firedAt: Date? = RadarUtils.isoStringToDate(obj.optString(FIELD_FIRED_AT))

            val firedAttempts: Int? = if (obj.has(FIELD_FIRED_ATTEMPTS) && !obj.isNull(FIELD_FIRED_ATTEMPTS)) {
                obj.optInt(FIELD_FIRED_ATTEMPTS)
            } else {
                null
            }

            val firedReason: String? = obj.optString(FIELD_FIRED_REASON).takeIf { it.isNotEmpty() }

            val updatedAtStr = obj.optString(FIELD_UPDATED_AT)
            val updatedAt = RadarUtils.isoStringToDate(updatedAtStr) ?: return null

            return RadarTripOrder(
                id,
                guid,
                handoffMode,
                status,
                firedAt,
                firedAttempts,
                firedReason,
                updatedAt
            )
        }

        /**
         * Creates an array of [RadarTripOrder] from a JSON array.
         */
        @JvmStatic
        fun fromJson(arr: JSONArray?): Array<RadarTripOrder>? {
            if (arr == null) {
                return null
            }

            return Array(arr.length()) { index ->
                fromJson(arr.optJSONObject(index))
            }.filterNotNull().toTypedArray()
        }

        /**
         * Returns a display string for a [RadarTripOrderStatus].
         */
        @JvmStatic
        fun stringForStatus(status: RadarTripOrderStatus): String {
            return when (status) {
                RadarTripOrderStatus.PENDING -> "pending"
                RadarTripOrderStatus.FIRED -> "fired"
                RadarTripOrderStatus.CANCELED -> "canceled"
                RadarTripOrderStatus.COMPLETED -> "completed"
                else -> "unknown"
            }
        }

        /**
         * Converts an array of [RadarTripOrder] to a JSON array.
         */
        @JvmStatic
        fun toJson(orders: Array<RadarTripOrder>?): JSONArray? {
            if (orders == null) {
                return null
            }

            val arr = JSONArray()
            orders.forEach { order ->
                arr.put(order.toJson())
            }
            return arr
        }
    }

    /**
     * Converts this [RadarTripOrder] to a JSON object.
     */
    fun toJson(): JSONObject {
        val obj = JSONObject()
        obj.putOpt(FIELD_ID, this._id)
        obj.putOpt(FIELD_GUID, this.guid)
        obj.putOpt(FIELD_HANDOFF_MODE, this.handoffMode)
        obj.putOpt(FIELD_STATUS, stringForStatus(this.status))
        obj.putOpt(FIELD_FIRED_AT, RadarUtils.dateToISOString(this.firedAt))
        obj.putOpt(FIELD_FIRED_ATTEMPTS, this.firedAttempts)
        obj.putOpt(FIELD_FIRED_REASON, this.firedReason)
        obj.putOpt(FIELD_UPDATED_AT, RadarUtils.dateToISOString(this.updatedAt))
        return obj
    }
}
