package io.radar.sdk.model

/**
 * The response format selected by the server.
 */
enum class RadarResponseFormat(val value: String) {
    JWS("jws"),
    JWE("jwe"),
    ID("id");

    internal companion object {
        fun fromJson(value: Any?): RadarResponseFormat? {
            if (value === null) {
                return JWS
            }

            if (value !is String) {
                return null
            }

            return RadarResponseFormat.entries.firstOrNull { it.value == value }
        }
    }
}
