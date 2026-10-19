package io.radar.sdk

import org.json.JSONArray
import org.json.JSONObject

/**
 * OTel span kind. Values are the OTLP enum values. The SDK only creates INTERNAL and CLIENT spans; the
 * others are 0 (UNSPECIFIED), 2 (SERVER, e.g. the Radar API's span for a request), 4 (PRODUCER) and 5 (CONSUMER).
 */
internal enum class RadarSpanKind(val otlpValue: Int) {
    /** Work inside the SDK, such as getting a location. */
    INTERNAL(1),

    /** An outgoing request to the Radar API. The server's span for that request is its child. */
    CLIENT(3)
}

/** An ended span's data, serialized with [toJson] as an OTLP span for storage and upload. */
internal data class RadarCompletedSpan(
    val name: String,
    val traceId: String,
    val spanId: String,
    val parentSpanId: String?,
    val kind: RadarSpanKind,
    val startUnixNanos: Long,
    val endUnixNanos: Long,
    val radarStatus: Radar.RadarStatus,
    val attributes: Map<String, Any>
) {
    fun toJson(): JSONObject {
        val attrs = JSONArray()
        attributes.forEach { (key, value) ->
            val attributeValue = JSONObject()
            when (value) {
                is Double, is Float -> attributeValue.put("doubleValue", (value as Number).toDouble())
                is Number -> attributeValue.put("intValue", (value as Number).toLong().toString())
                is Boolean -> attributeValue.put("boolValue", value)
                else -> attributeValue.put("stringValue", value.toString())
            }
            attrs.put(JSONObject().put("key", key).put("value", attributeValue))
        }
        if (radarStatus != Radar.RadarStatus.SUCCESS) {
            attrs.put(JSONObject().put("key", "error.type").put("value", JSONObject().put("stringValue", radarStatus.name)))
        }
        val json = JSONObject()
            .put("name", name)
            .put("traceId", traceId)
            .put("spanId", spanId)
            .put("kind", kind.otlpValue)
            .put("flags", 1)
            .put("startTimeUnixNano", startUnixNanos.toString())
            .put("endTimeUnixNano", endUnixNanos.toString())
            .put("attributes", attrs)
        if (parentSpanId != null) {
            json.put("parentSpanId", parentSpanId)
        }
        if (radarStatus != Radar.RadarStatus.SUCCESS) {
            json.put("status", JSONObject().put("code", 2))
        }
        return json
    }
}
