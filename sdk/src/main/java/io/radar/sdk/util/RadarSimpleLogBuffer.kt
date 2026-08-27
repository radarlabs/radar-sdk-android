package io.radar.sdk.util

import io.radar.sdk.Radar
import io.radar.sdk.model.RadarLog
import java.util.concurrent.LinkedBlockingDeque
import android.content.Context
import io.radar.sdk.RadarSettings
import org.json.JSONObject
import org.json.JSONException
import java.io.File
import java.lang.Integer.min
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

internal class RadarSimpleLogBuffer(override val context: Context) : RadarLogBuffer {

    private companion object {

        const val MAX_MEMORY_BUFFER_SIZE = 200
        const val MAX_PERSISTED_BUFFER_SIZE = 500
        const val PURGE_AMOUNT = 250
        const val logFileDir = "radar_logs"
        var fileCounter = 0
        const val KEY_PURGED_LOG_LINE = "----- purged oldest logs -----"

    }
    private var persistentLogFeatureFlag = false

    private val lock = Any()

    private data class LogIdentity(
        val createdAt: Long,
        val level: Radar.RadarLogLevel,
        val typeName: String?,
        val message: String
    )

    private fun deduplicate(logs: Collection<RadarLog>): List<RadarLog> {
        val seen = HashSet<LogIdentity>()
        return logs.filter { log ->
            seen.add(
                LogIdentity(
                    createdAt = log.createdAt.time,
                    level = log.level,
                    typeName = log.type?.name,
                    message = log.message
                )
            )
        }
    }

    private val timer = Executors.newScheduledThreadPool(1)

    private val logBuffer = LinkedBlockingDeque<RadarLog>()

    init {
        persistentLogFeatureFlag = RadarSettings.getSdkConfiguration(context).useLogPersistence
        val file = File(context.filesDir, logFileDir)
        if (!file.exists()) {
            file.mkdir()
        }
        timer.scheduleWithFixedDelay({ persistLogs() }, 2, 2, TimeUnit.SECONDS)
    }

    override fun setPersistentLogFeatureFlag(persistentLogFeatureFlag: Boolean) {
        this.persistentLogFeatureFlag = persistentLogFeatureFlag
    }

    override fun write(
        level: Radar.RadarLogLevel,
        type: Radar.RadarLogType?,
        message: String,
        createdAt: Date
    ) {
        synchronized(lock) {
            val radarLog = RadarLog(level, message, type, createdAt)
            logBuffer.put(radarLog)
            if (persistentLogFeatureFlag) {
                if (logBuffer.size > MAX_MEMORY_BUFFER_SIZE) {
                    persistLogs()
                }
            } else {
                if (logBuffer.size > MAX_PERSISTED_BUFFER_SIZE) {
                    purgeOldestLogs()
                }
            }
        }
    }

    override fun persistLogs() {
        synchronized(lock) {
            if (persistentLogFeatureFlag) {
                if (logBuffer.isNotEmpty()) {
                    writeToFileStorage(logBuffer)
                    logBuffer.clear()
                }
            }
        }
    }

    private fun getLogFilesInTimeOrder(): Array<File>? {
        val compareTimeStamps = Comparator<File> { file1, file2 ->
            val number1 = file1.name.replace("_", "").toLongOrNull() ?: 0L
            val number2 = file2.name.replace("_", "").toLongOrNull() ?: 0L
            number1.compareTo(number2)
        }

        return RadarFileStorage(context).sortedFilesInDirectory(logFileDir, compareTimeStamps)
    }

    private fun isValidJson(json: String): Boolean = try {
        JSONObject(json)
        true
    } catch (ex: JSONException) {
        false
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
            if (jsonString.isNullOrEmpty() || !isValidJson(jsonString)) {
                file.delete()
                continue
            }
            val log = RadarLog.fromJson(JSONObject(jsonString))
            logs.add(log)
        }
        return logs
    }

    private fun writeToFileStorage(logs: Collection<RadarLog>) {
        for (log in logs) {
            val counterString = String.format(Locale.US, "%04d", fileCounter++)
            val fileName = "${log.createdAt.time / 1000}_$counterString"
            RadarFileStorage(context).writeData(logFileDir, fileName, log.toJson().toString())
        }
    }

    override fun getFlushableLogs(): Flushable<RadarLog> {
        val logs = mutableListOf<RadarLog>()
        synchronized(lock) {
            if (persistentLogFeatureFlag) {
                persistLogs()
                purgeOldestLogs()
                val files = getLogFilesInTimeOrder()
                val storedLogs = readFromFileStorage()
                logs.addAll(deduplicate(storedLogs))
                files?.forEach { it.delete() }
            } else {
                val memoryLogs = mutableListOf<RadarLog>()
                logBuffer.drainTo(memoryLogs)
                logs.addAll(deduplicate(memoryLogs))
            }
        }
        return object : Flushable<RadarLog> {

            override fun get(): List<RadarLog> = logs

            override fun onFlush(success: Boolean) {
                // Restore the snapshot because the upload failed.
                if (!success) {
                    if (persistentLogFeatureFlag) {
                        writeToFileStorage(logs)
                        purgeOldestLogs()
                    } else {
                        logs.reverse()
                        logs.forEach {
                            if (!logBuffer.offerFirst(it)) {
                                purgeOldestLogs()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun purgeOldestLogs() {
        if (persistentLogFeatureFlag) {
            var files = getLogFilesInTimeOrder()
            if (files.isNullOrEmpty()) {
                return
            }
            var printedPurgedLogs = false
            while (files?.size ?: 0 > MAX_PERSISTED_BUFFER_SIZE) {
                val numberToPurge = min(PURGE_AMOUNT, files?.size ?: 0)
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
            logBuffer.drainTo(oldLogs, PURGE_AMOUNT)
            write(Radar.RadarLogLevel.DEBUG, null, KEY_PURGED_LOG_LINE)
        }
    }
}
