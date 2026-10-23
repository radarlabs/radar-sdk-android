package io.radar.sdk.util

import org.json.JSONObject

/**
 * Returns the string for [name], or null if the key is missing or JSON null.
 *
 * Unlike [JSONObject.optString], this does not turn a missing key into `""` or a JSON null into `"null"`.
 */
internal fun JSONObject.optStringOrNull(name: String): String? = if (has(name) && !isNull(name)) optString(name) else null

/**
 * Returns the double for [name], or null if the key is missing, JSON null, or not a number.
 *
 * Unlike [JSONObject.optDouble], this never returns NaN, which [JSONObject] refuses to serialize.
 */
internal fun JSONObject.optDoubleOrNull(name: String): Double? = if (has(name) && !isNull(name)) optDouble(name).takeIf { !it.isNaN() } else null

/**
 * Returns the boolean for [name], or null if the key is missing or JSON null.
 */
internal fun JSONObject.optBooleanOrNull(name: String): Boolean? = if (has(name) && !isNull(name)) optBoolean(name) else null
