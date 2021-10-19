package io.radar.sdk.util

import android.content.Context
import io.radar.sdk.Radar
import io.radar.sdk.model.RadarLog
import org.json.JSONObject
import java.io.File
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.locks.ReentrantLock

/**
 * Wraps the filesystem to support log caching
 */
internal class RadarLogBuffer(private val context: Context) {

    private companion object {
        const val DIRECTORY = "RadarLogs"
        /**
         * Maximum number of logs in the buffer.
         */
        const val LOG_BUFFER_SIZE_LIMIT = 500
    }

    /**
     * Lock for handling changes to [currentFile]
     */
    private val fileSelector = ReentrantLock()

    /**
     * Used to manage incoming log writes synchronously
     */
    private val executor = Executors.newSingleThreadExecutor()

    /**
     * Contains the name of the current log file
     */
    private lateinit var currentFile: String

    /**
     * Number of log entries in the current log file
     */
    private var size = getInitialSize()

    /**
     * Write a log to the buffer
     */
    fun write(level: Radar.RadarLogLevel, message: String) {
        val file = getFile()
        executor.submit {
            if (isFull()) {
                purge()
            }
            context.openFileOutput(file, Context.MODE_PRIVATE and Context.MODE_APPEND).use { fileOutputStream ->
                val radarLog = RadarLog(level, message).toJson().toString()
                fileOutputStream.write("$radarLog\n".toByteArray())
            }
            size++
        }
    }

    /**
     * Get the logs from the buffer. These are given as a [Flushable] so that a successful callback can cleanup this
     * log buffer by deleting old log files.
     */
    fun getLogs(): Flushable<RadarLog> {
        fileSelector.lock()
        try {
            val files = context.getDir(DIRECTORY, Context.MODE_PRIVATE).listFiles()
            val list = mutableListOf<RadarLog>()
            files?.forEach { file ->
                context.openFileInput("${DIRECTORY}/${file.name}").bufferedReader().forEachLine { line ->
                    list.add(RadarLog.fromJson(JSONObject(line)))
                }
            }
            //Create a new currentFile
            val name = "${UUID.randomUUID()}.log"
            File(context.getDir(DIRECTORY, Context.MODE_PRIVATE), name).createNewFile()
            currentFile = "$DIRECTORY/$name"
            return object : Flushable<RadarLog> {

                override fun get(): List<RadarLog> = list

                override fun onFlush(success: Boolean) {
                    if (success) {
                        files?.forEach { file ->
                            context.deleteFile("${DIRECTORY}/${file.name}")
                        }
                    }
                    //else do nothing. These logs will be stored until a later time.
                }
            }
        } finally {
            fileSelector.unlock()
        }

    }

    private fun getFile(): String {
        fileSelector.lock()
        try {
            if (!this::currentFile.isInitialized) {
                //First find current file
                val files = context.getDir(DIRECTORY, Context.MODE_PRIVATE).listFiles()
                currentFile = if (files != null && files.isNotEmpty()) {
                    //This gets the latest file that was in use.
                    "$DIRECTORY/${files.minByOrNull { it.lastModified() }!!.name}"
                } else {
                    //If no such file exists yet, create one.
                    val name = "${UUID.randomUUID()}.log"
                    File(context.getDir(DIRECTORY, Context.MODE_PRIVATE), name).createNewFile()
                    "$DIRECTORY/$name"
                }
            }
            return currentFile
        } finally {
            fileSelector.unlock()
        }
    }

    /**
     * Checks if the current log file (not previous log files) is at the max buffer size
     */
    private fun isFull(): Boolean {
        return size >= LOG_BUFFER_SIZE_LIMIT
    }

    /**
     * Clears oldest logs and adds a "purged" log line
     */
    private fun purge() {
        fileSelector.lock()
        try {
            val files = context.getDir(DIRECTORY, Context.MODE_PRIVATE).listFiles()
            if (files != null && files.isNotEmpty()) {
                if (files.size > 1) {
                    //Delete all previous files
                    val sorted = files.sortedBy { it.lastModified() }
                    sorted.subList(1, sorted.size).forEach { it.delete() }
                }
                //Store current file for sending later, and set currentFile to a new file
                val name = "${UUID.randomUUID()}.log"
                File(context.getDir(DIRECTORY, Context.MODE_PRIVATE), name).createNewFile()
                currentFile = "$DIRECTORY/$name"
            }
            size = 0
        } finally {
            fileSelector.unlock()
        }
        write(Radar.RadarLogLevel.DEBUG, "------ purged oldest logs -----")
    }

    /**
     * Gets the initial size of the current file. Ignores the size of old files. These will get cleaned up during
     * a purge.
     */
    private fun getInitialSize(): Int {
        return context.openFileInput(getFile()).bufferedReader().readLines().size
    }

}