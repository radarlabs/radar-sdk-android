package io.radar.sdk

import android.content.Context
import org.json.JSONException
import org.json.JSONObject
import java.io.File

class RadarFileStorage {

    fun writeFile(context: Context, filename: String, content: ByteArray) {
        val root = File(context.filesDir, "RadarSDK")
        val file = File(root, filename)
        // create intermediate parent folder if it does not
        file.parentFile?.apply { mkdirs() }
        // creates the file if it doesn't exist, no-op if it does
        file.createNewFile()
        file.writeBytes(content)
    }

    fun readFile(context: Context, filename: String): ByteArray? {
        val root = File(context.filesDir, "RadarSDK")
        val file = File(root, filename)
        return if (file.exists()) {
            file.readBytes()
        } else {
            null
        }
    }

    fun writeJSON(context: Context, filename: String, json: JSONObject) {
        writeFile(context, filename, json.toString().toByteArray(Charsets.UTF_8))
    }

    fun readJSON(context: Context, filename: String): JSONObject? {
        return readFile(context, filename)?.let {
            try {
                return JSONObject(it.toString(Charsets.UTF_8))
            }
            catch (e: JSONException) {
                return null
            }
        }
    }
}