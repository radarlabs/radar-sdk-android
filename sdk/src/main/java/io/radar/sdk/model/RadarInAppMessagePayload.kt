package io.radar.sdk.model

import org.json.JSONObject

/**
 * Data class representing the payload for an in-app message.
 * Contains all the necessary information to display a banner message.
 */
data class RadarInAppMessagePayload(
    val title: String,
    val message: String,
    val buttonText: String
) {
    
    companion object {
        // JSON keys for serialization
        private const val KEY_TITLE = "title"
        private const val KEY_MESSAGE = "message"
        private const val KEY_BUTTON_TEXT = "buttonText"
        
        /**
         * Creates a RadarInAppMessagePayload from a JSON string.
         * 
         * @param jsonString The JSON string to parse
         * @return RadarInAppMessagePayload instance or null if parsing fails
         */
        fun fromJson(jsonString: String): RadarInAppMessagePayload? {
            return try {
                val json = JSONObject(jsonString)
                RadarInAppMessagePayload(
                    title = json.getString(KEY_TITLE),
                    message = json.getString(KEY_MESSAGE),
                    buttonText = json.getString(KEY_BUTTON_TEXT)
                )
            } catch (e: Exception) {
                null
            }
        }
    }
    
    /**
     * Converts the payload to a JSON string for storage.
     * 
     * @return JSON string representation of the payload
     */
    fun toJson(): String {
        return JSONObject().apply {
            put(Companion.KEY_TITLE, title)
            put(Companion.KEY_MESSAGE, message)
            put(Companion.KEY_BUTTON_TEXT, buttonText)
        }.toString()
    }
} 