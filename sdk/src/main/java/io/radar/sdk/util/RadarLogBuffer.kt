package io.radar.sdk.util

import android.content.Context
import io.radar.sdk.Radar
import io.radar.sdk.model.RadarLog
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.locks.ReentrantLock

/**
 * Wraps the filesystem to support log caching
 */
internal class RadarLogBuffer(
    private val context: Context,
    private val executor: ExecutorService = Executors.newSingleThreadExecutor()
) {

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
    private val fileLock = ReentrantLock()

    /**
     * Lock for handling changes to the files within [DIRECTORY]
     */
    private val directoryLock = ReentrantLock()

    /**
     * Contains the name of the current log file
     */
    private var currentFile: String

    /**
     * Number of log entries in the current log file.
     */
    var size: Int
        private set

    init {
        //First find current file
        val files = getDirectory().listFiles()
        if (files != null && files.isNotEmpty()) {
            //This gets the latest file that was in use if it can, otherwise it returns the first one.
            currentFile = files.maxByOrNull { getLastModified(it) }!!.name
            //This is initialized to the number of lines in the current file
            size = FileInputStream(File(getDirectory(), currentFile)).bufferedReader().readLines().size
            if (files.size > 2) {
                //This case likely means the app started and ended several times without completing a purge. To prevent
                //excessive log files, purge them now.
                purge()
            }
        } else {
            //If no such file exists yet, create one.
            currentFile = getNewFilePath()
            size = 0
        }
    }

    /**
     * Write a log to the buffer
     */
    fun write(level: Radar.RadarLogLevel, message: String) {
        executor.submit {
            if (isFull()) {
                purge()
            }
            //Locks file operations while writing to the currentFile
            fileOp {
                FileOutputStream(File(getDirectory(), currentFile), true).use { fileOutputStream ->
                    val radarLog = RadarLog(level, message).toJson().toString()
                    fileOutputStream.write("$radarLog\n".toByteArray())
                }
            }

            size++
        }
    }

    /**
     * Get the logs from the buffer. These are given as a [Flushable] so that a successful callback can cleanup this
     * log buffer by deleting old log files.
     */
    fun getLogs(): Flushable<RadarLog> {
        val (files, list) = directoryOp {
            val files = getDirectory().listFiles()
            val list = mutableListOf<RadarLog>()
            //Now iterate over the old list of files
            files?.forEach { file ->
                FileInputStream(file).bufferedReader().forEachLine { line ->
                    list.add(RadarLog.fromJson(JSONObject(line)))
                }
            }
            files to list
        }
        fileOp {
            //Create a new currentFile
            currentFile = getNewFilePath()
        }
        return object : Flushable<RadarLog> {

            override fun get(): List<RadarLog> = list

            override fun onFlush(success: Boolean) {
                if (success) {
                    directoryOp { files?.forEach { it.delete() } }
                    size -= list.size % LOG_BUFFER_SIZE_LIMIT
                }
                //else do nothing. These logs will be stored until a later time.
            }
        }
    }

    private fun getDirectory() = context.getDir(DIRECTORY, Context.MODE_PRIVATE)

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
        directoryOp {
            fileOp {
                val files = getDirectory().listFiles()
                if (files != null && files.isNotEmpty()) {
                    if (files.size > 2) {
                        //Delete all previous files. By allowing 2 files, it allows a max number of logs to be at most
                            //2 x LOG_BUFFER_SIZE_LIMIT
                        val otherFiles = files.toMutableList()
                        otherFiles.removeIf { it.name == currentFile }
                        otherFiles.forEach { it.delete() }
                    }
                    //Store current file for sending later, and set currentFile to a new file
                    currentFile = getNewFilePath()
                }
                size = 0
            }
        }
        write(Radar.RadarLogLevel.DEBUG, "------ purged oldest logs -----")
    }

    /**
     * Get a new file path
     */
    private fun getNewFilePath(): String {
        val name = "${UUID.randomUUID()}.log"
        File(getDirectory(), name).createNewFile()
        return name
    }

    private inline fun <T> fileOp(block: () -> T): T {
        fileLock.lock()
        try {
            return block.invoke()
        } finally {
            fileLock.unlock()
        }
    }

    private inline fun <T> directoryOp(block: () -> T): T {
        directoryLock.lock()
        try {
            return block.invoke()
        } finally {
            directoryLock.unlock()
        }
    }

    /**
     * Get the last modified date, or zero if the value cannot be retrieved because of security restrictions
     */
    private fun getLastModified(file: File): Long {
        return try {
            file.lastModified()
        } catch (ignored: SecurityException) {
            0L
        }
    }

}