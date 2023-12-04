package io.radar.sdk.util
import android.content.Context
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

class RadarFileSystem(private val context: Context) {
    fun writeData(filename: String, content: String) {
        val fileOutputStream: FileOutputStream
        try {
            fileOutputStream = context.openFileOutput(filename, Context.MODE_PRIVATE)
            fileOutputStream.write(content.toByteArray())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun readFileAtPath(filePath: String): String {
        var fileInputStream: FileInputStream? = null
        try {
            fileInputStream = context.openFileInput(filePath)
            val inputStreamReader = fileInputStream.reader()
            return inputStreamReader.readText()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            fileInputStream?.close()
        }
        return ""
    }

    fun deleteFileAtPath(filePath: String): Boolean {
        val file = File(context.filesDir, filePath)
        return file.delete()
    }
}