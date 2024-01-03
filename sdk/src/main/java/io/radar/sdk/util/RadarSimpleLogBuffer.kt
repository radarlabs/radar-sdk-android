package io.radar.sdk.util

import io.radar.sdk.Radar
import io.radar.sdk.model.RadarLog
import java.util.concurrent.LinkedBlockingDeque
import android.content.Context
import io.radar.sdk.RadarSettings
import org.json.JSONObject
import java.io.File
import java.lang.Integer.min
import java.util.Date
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Log Buffer implementation that is backed by an in-memory buffer and files on disk.
 */
internal class
RadarSimpleLogBuffer(override val context: Context): RadarLogBuffer {

    private companion object {

        const val MAX_MEMORY_BUFFER_SIZE = 200
        const val MAX_PERSISTED_BUFFER_SIZE = 500
        const val PURGE_AMOUNT = 250
        const val logFileDir = "radar_logs"
        var fileCounter = 0
        const val KEY_PURGED_LOG_LINE = "----- purged oldest logs -----"

    }
    private var persistentLogFeatureFlag = false


    private val timer = Executors.newScheduledThreadPool(1)

    private val inMemoryLogBuffer = LinkedBlockingDeque<RadarLog>()

    init {
        persistentLogFeatureFlag = RadarSettings.getFeatureSettings(context).useLogPersistence
        val file = File(context.filesDir, logFileDir)
        if (!file.exists()) {
            file.mkdir()
        }
        timer.scheduleAtFixedRate({ persistLogs() }, 20, 20, TimeUnit.SECONDS)
    }

    override fun setPersistentLogFeatureFlag(persistentLogFeatureFlag: Boolean){
       this.persistentLogFeatureFlag = persistentLogFeatureFlag
    }

    override fun write(
        level: Radar.RadarLogLevel,
        type: Radar.RadarLogType?,
        message: String,
        createdAt: Date
    ) {
        val radarLog = RadarLog(level, message, type, createdAt)
        inMemoryLogBuffer.put(radarLog)
        if (persistentLogFeatureFlag) {
            if (inMemoryLogBuffer.size > MAX_MEMORY_BUFFER_SIZE) {
                persistLogs()
            }
        } else {
            if (inMemoryLogBuffer.size > MAX_PERSISTED_BUFFER_SIZE) {
                purgeOldestLogs()
            }
        }
    }


    override fun persistLogs() {
        if (persistentLogFeatureFlag) {
            if (inMemoryLogBuffer.size > 0) {
                writeToFileStorage(inMemoryLogBuffer)
                purgeOldestLogs()
                inMemoryLogBuffer.clear()
            }
        }
    }

    private fun getLogFilesInTimeOrder(): Array<File>? {
        val compareTimeStamps = Comparator<File> { file1, file2 ->
            val number1 = file1.name.replace("_","").toLongOrNull() ?: 0L
            val number2 = file2.name.replace("_","").toLongOrNull() ?: 0L
            number1.compareTo(number2)
        }

        return RadarFileStorage(context).sortedFilesInDirectory(logFileDir, compareTimeStamps)
    }

    /**
     * Gets logs from disk.
     */
    private fun readFromFileStorage(): LinkedBlockingDeque<RadarLog> {

        val files = getLogFilesInTimeOrder()
        val logs = LinkedBlockingDeque<RadarLog>()
        if (files.isNullOrEmpty()) {
            return logs
        }

        for (file in files) {
            val jsonString = RadarFileStorage(context).readFileAtPath(logFileDir, file.name)
            val log = RadarLog.fromJson(JSONObject(jsonString))
            if (log != null) {
                logs.add(log)
            }
        }
        return logs
    }

    /**
     * Writes logs to disk.
     */
    private fun writeToFileStorage(logs: Collection<RadarLog>) {
        for (log in logs) {
            val counterString = String.format("%04d", fileCounter++)
            val fileName = "${log.createdAt.time / 1000}_${counterString}"
            RadarFileStorage(context).writeData(logFileDir, fileName, log.toJson().toString())
        }
    }


    override fun flushableLogs(): Flushable<RadarLog> {

        val logs = mutableListOf<RadarLog>()
        if (persistentLogFeatureFlag) {
            persistLogs()
            readFromFileStorage().drainTo(logs)
            val files = getLogFilesInTimeOrder()
            for (i in 0 until min(logs.size,files?.size ?:0)){
                files?.get(i)?.delete()
            }
        } else {
            inMemoryLogBuffer.drainTo(logs)
        }
        return object : Flushable<RadarLog> {

            override fun get(): List<RadarLog> {
                return logs
            }

            override fun onFlush(success: Boolean) {
                // clear the logs from disk
                if (!success) {
                   if (persistentLogFeatureFlag) {
                        writeToFileStorage(logs)
                        purgeOldestLogs()
                   } else {
                       logs.reverse()
                       logs.forEach {
                           if (!inMemoryLogBuffer.offerFirst(it)) {
                               purgeOldestLogs()
                           }
                       }
                   }
                }
            }
        }
    }


    /**
     * Clears oldest logs and adds a "purged" log line.
     */
    private fun purgeOldestLogs() {
        if (persistentLogFeatureFlag) {
            var files = getLogFilesInTimeOrder()
            if (files.isNullOrEmpty()) {
                return
            }
            var printedPurgedLogs = false
            while (files?.size ?: 0 > MAX_PERSISTED_BUFFER_SIZE) {
                val numberToPurge = min(PURGE_AMOUNT,files?.size ?: 0)
                for (i in 0 until numberToPurge) {
                    files?.get(i)?.delete()
                }
                if (!printedPurgedLogs) {
                    writeToFileStorage(listOf(RadarLog(Radar.RadarLogLevel.DEBUG, KEY_PURGED_LOG_LINE, null)))
                    printedPurgedLogs = true
                }
                files = getLogFilesInTimeOrder()
            }
        } else {
            val oldLogs = mutableListOf<RadarLog>()
            inMemoryLogBuffer.drainTo(oldLogs, PURGE_AMOUNT)
            write(Radar.RadarLogLevel.DEBUG, null, KEY_PURGED_LOG_LINE)
        }
    }

}
