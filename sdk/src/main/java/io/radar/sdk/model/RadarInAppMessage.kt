package io.radar.sdk.model

import org.json.JSONArray
import org.json.JSONObject

/**
 * Data class representing the payload for an in-app message.
 * Contains all the necessary information to display a banner message.
 */
data class RadarInAppMessage(
    val title: Title,
    val body: Body,
    val button: Button? = null,
    val image: Image? = null,
    val metadata: JSONObject
) {
    
    data class Title(
        val text: String,
        val color: String
    )
    
    data class Body(
        val text: String,
        val color: String
    )
    
    data class Button(
        val text: String,
        val color: String,
        val backgroundColor: String,
        val deepLink: String? = null
    )

    data class Image(
        val name: String,
        val url: String? = null
    )
    
    companion object {
        // JSON keys for serialization
        private const val KEY_TITLE = "title"
        private const val KEY_BODY = "body"
        private const val KEY_BUTTON = "button"
        private const val KEY_TEXT = "text"
        private const val KEY_COLOR = "color"
        private const val KEY_BACKGROUND_COLOR = "backgroundColor"
        private const val KEY_DEEPLINK = "deepLink"
        private const val KEY_URL = "url"
        private const val KEY_NAME = "name"
        private const val KEY_IMAGE = "image"
        private const val KEY_METADATA = "metadata"

        
        /**
         * Creates a RadarInAppMessage from a JSON string.
         * 
         * @param jsonString The JSON string to parse
         * @return RadarInAppMessage instance or null if parsing fails
         */
        @JvmStatic
        fun fromJson(jsonString: String): RadarInAppMessage? {
            return try {
                val json = JSONObject(jsonString)

                println("HERE")
                
                val titleJson = json.getJSONObject(KEY_TITLE)
                val title = Title(
                    text = titleJson.getString(KEY_TEXT),
                    color = titleJson.getString(KEY_COLOR)
                )
                println("HERE2")
                
                val bodyJson = json.getJSONObject(KEY_BODY)
                val body = Body(
                    text = bodyJson.getString(KEY_TEXT),
                    color = bodyJson.getString(KEY_COLOR)
                )

                println("HERE3")
                val buttonJson = json.optJSONObject(KEY_BUTTON)
                var button: Button? = null
                if (buttonJson != null) {
                    button = Button(
                        text = buttonJson.getString(KEY_TEXT),
                        color = buttonJson.getString(KEY_COLOR),
                        backgroundColor = buttonJson.getString(KEY_BACKGROUND_COLOR),
                        deepLink = buttonJson.optString(KEY_DEEPLINK)
                    )
                }

                val imageJson = json.optJSONObject(KEY_IMAGE)
                var image: Image? = null
                if (imageJson != null) {
                    image = Image(
                        name = imageJson.getString(KEY_NAME),
                        url = imageJson.optString(KEY_URL)
                    )
                }
                
                val metadata = json.optJSONObject(KEY_METADATA) ?: JSONObject()

                RadarInAppMessage(title, body, button, image, metadata)
            } catch (e: Exception) {
                null
            }
        }
        
        /**
         * Creates an array of RadarInAppMessage from a JSON array.
         * 
         * @param jsonArray The JSON array to parse
         * @return Array of RadarInAppMessage instances, empty array if parsing fails
         */
        fun fromJsonArray(jsonArray: JSONArray): Array<RadarInAppMessage> {
            return try {
                val payloads = mutableListOf<RadarInAppMessage>()
                
                for (i in 0 until jsonArray.length()) {
                    val jsonObject = jsonArray.getJSONObject(i)
                    val payload = fromJson(jsonObject.toString())
                    payload?.let { payloads.add(it) }
                }
                
                payloads.toTypedArray()
            } catch (e: Exception) {
                emptyArray()
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
            put(KEY_TITLE, JSONObject().apply {
                put(KEY_TEXT, title.text)
                put(KEY_COLOR, title.color)
            })
            put(KEY_BODY, JSONObject().apply {
                put(KEY_TEXT, body.text)
                put(KEY_COLOR, body.color)
            })
            if (button != null) {
                put(KEY_BUTTON, JSONObject().apply {
                    put(KEY_TEXT, button.text)
                    put(KEY_COLOR, button.color)
                    put(KEY_BACKGROUND_COLOR, button.backgroundColor)
                    button.deepLink?.let { put(KEY_DEEPLINK, it) }
                })
            }
            if (image != null) {
                put(KEY_IMAGE, JSONObject().apply {
                    put(KEY_NAME, image.name)
                    image.url?.let { put(KEY_URL, it) }
                })
            }

            put(KEY_METADATA, metadata)

        }.toString()
    }
} 