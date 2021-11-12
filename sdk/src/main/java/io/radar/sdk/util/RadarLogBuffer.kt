package io.radar.sdk.util

import android.content.Context
import androidx.annotation.VisibleForTesting
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
    /**
     * Application Context
     */
    context: Context,
    /**
     * Executor for writing files. This is used to ensure that log entries are written to the file in the order that
     * they are received.
     */
    private val executor: ExecutorService = Executors.newSingleThreadExecutor()
) {

    private companion object {

        /**
         * Name of the directory in which the logs are stored
         */
        const val DIRECTORY = "RadarLogs"

        /**
         * Maximum number of logs in the buffer.
         */
        const val LOG_BUFFER_SIZE_LIMIT = 500
    }

    /**
     * The directory where the log files are stored
     */
    private val directory = context.getDir(DIRECTORY, Context.MODE_PRIVATE)

    /**
     * Lock for handling changes to the files within [DIRECTORY]
     */
    private val directoryLock = ReentrantLock()

    /**
     * Contains the name of the current log file. Modifications to this value should occur within a [fileOp]
     * operation.
     */
    private var currentFile: String

    /**
     * Number of log entries in the current log file. Modifications to this value should occur within a [fileOp]
     * operation.
     */
    var size: Int
        private set

    init {
        // First find current file
        val files = directory.listFiles()
        if (files?.isNotEmpty() == true) {
            // This gets the latest file that was in use if it can, otherwise it returns the first one.
            currentFile = files.maxByOrNull { getLastModified(it) }!!.name
            // This is initialized to the number of lines in the current file
            size = FileInputStream(File(directory, currentFile)).bufferedReader().readLines().size
            if (files.size > 2) {
                // This case likely means the app started and ended several times without completing a purge. To prevent
                // excessive log files, purge them now.
                purgeInactiveFiles()
            }
        } else {
            // If no such file exists yet, create one.
            currentFile = getNewFilePath()
            size = 0
        }
    }

    /**
     * Write a log to the buffer
     *
     * @param[level] log level
     * @param[message] log message
     */
    fun write(level: Radar.RadarLogLevel, message: String) {
        executor.submit {
            if (isFull()) {
                purgeInactiveFiles()
            }
            // Remove line-breaks to ensure each line in the file is a separate log event
            val radarLog = RadarLog(level, message.replace('\n', '\t')).toJson()

            // Locks file operations while writing to the currentFile
            fileOp {
                FileOutputStream(File(directory, currentFile), true).use { fileOutputStream ->
                    fileOutputStream.write("$radarLog\n".toByteArray())
                }
                size++
            }
        }
    }

    /**
     * Creates a stash of the logs currently in the buffer and returns them as a [Flushable] so that a successful
     * callback can cleanup this log buffer by deleting old log files.
     *
     * @return a [Flushable] containing all stored logs
     */
    fun getFlushableLogsStash(): Flushable<RadarLog> {
        val (files, list) = fileOp {
            val files = directory.listFiles()
            val list = mutableListOf<RadarLog>()
            // Now iterate over the old list of files
            files?.forEach { file ->
                FileInputStream(file).bufferedReader().forEachLine { line ->
                    list.add(RadarLog.fromJson(JSONObject(line)))
                }
            }
            // Create a new currentFile
            currentFile = getNewFilePath()

            // return files and logs
            files to list
        }
        return object : Flushable<RadarLog> {

            override fun get(): List<RadarLog> = list

            override fun onFlush(success: Boolean) {
                if (success) {
                    fileOp {
                        files?.forEach { it.delete() }
                        size -= list.size % LOG_BUFFER_SIZE_LIMIT
                    }
                }
                // else do nothing. These logs will be stored until a later time.
            }
        }
    }

    /**
     * Checks if the current log file (not previous log files) is at the max buffer size
     *
     * @return true if the log buffer is full, otherwise false
     */
    private fun isFull(): Boolean {
        return fileOp { size >= LOG_BUFFER_SIZE_LIMIT }
    }

    /**
     * Clears oldest logs and adds a "purged" log line
     */
    private fun purgeInactiveFiles() {
        fileOp {
            val files = directory.listFiles()
            if (files != null && files.isNotEmpty()) {
                if (files.size > 2) {
                    // Delete all previous files. By allowing 2 files, it allows a max number of logs to be at most
                    // 2 x LOG_BUFFER_SIZE_LIMIT
                    val otherFiles = files.toMutableList()
                    for (file in otherFiles) {
                        if (file.name == currentFile) {
                            otherFiles.remove(file)
                            break
                        }
                    }
                    otherFiles.forEach { it.delete() }
                }
                // Store current file for sending later, and set currentFile to a new file
                currentFile = getNewFilePath()
            }
            size = 0
        }
        write(Radar.RadarLogLevel.DEBUG, "------ purged oldest logs -----")
    }

    /**
     * Creates a new File and returns its path
     *
     * @return the path of the newly-created file
     */
    private fun getNewFilePath(): String {
        val name = "${UUID.randomUUID()}.log"
        File(directory, name).createNewFile()
        return name
    }

    /**
     * Perform a blocking operation on the file system
     *
     * @param[block] the operation to perform
     * @return the result of the [block]
     */
    private inline fun <T> fileOp(block: () -> T): T {
        directoryLock.lock()
        try {
            return block.invoke()
        } finally {
            directoryLock.unlock()
        }
    }

    /**
     * Get the last modified date, or zero if the value cannot be retrieved because of security restrictions
     *
     * @param[file] the file to query
     */
    @VisibleForTesting
    fun getLastModified(file: File): Long {
        return try {
            file.lastModified()
        } catch (ignored: SecurityException) {
            0L
        }
    }
}
