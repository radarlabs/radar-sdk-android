package io.radar.sdk.util
import android.content.Context
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream

class RadarFileStorage(private val context: Context) {
    fun writeData(subDir: String = "",  filename: String, content: String) {
        var fileOutputStream: FileOutputStream
        try {
            val directory = if (subDir.isNotEmpty()) File(context.filesDir, subDir) else context.filesDir
            directory.mkdirs()
            val file = File(directory, filename)
            fileOutputStream = FileOutputStream(file)
            fileOutputStream.use { it.write(content.toByteArray()) }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun readFileAtPath(subDir: String = "", filePath: String): String {
        var fileInputStream: FileInputStream? = null
        try {
            val directory = if (subDir.isNotEmpty()) File(context.filesDir, subDir) else context.filesDir
            val file = File(directory, filePath)
            fileInputStream = FileInputStream(file)
            val inputStreamReader = fileInputStream?.reader()
            return inputStreamReader?.readText() ?: ""
        } catch (e: FileNotFoundException) {
            return ""
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            fileInputStream?.close()
        }
        return ""
    }

    fun deleteFileAtPath(subDir: String = "", filePath: String): Boolean {
        try {
            val directory = if (subDir.isNotEmpty()) File(context.filesDir, subDir) else context.filesDir
            val file = File(directory, filePath)
            return file.delete()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return false
    }

    fun sortedFilesInDirectory(directoryPath: String, comparator: Comparator<File>): Array<File>? {
        try {
            val directory = File(context.filesDir, directoryPath)
            var files = directory.listFiles()
            files?.sortWith(comparator)
            return files
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }
}