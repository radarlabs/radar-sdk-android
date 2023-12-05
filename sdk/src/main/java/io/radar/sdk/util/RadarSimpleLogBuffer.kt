package io.radar.sdk.util

import io.radar.sdk.Radar
import io.radar.sdk.model.RadarLog
import java.util.concurrent.LinkedBlockingDeque
import android.content.Context
import org.json.JSONArray
import java.util.Date
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Log Buffer implementation that persist.
 */
internal class
RadarSimpleLogBuffer(override val context: Context): RadarLogBuffer{

    private companion object {

        const val MAXIMUM_MEMORY_CAPACITY = 200

        const val MAXIMUM_CAPACITY = 500
        const val HALF_CAPACITY = MAXIMUM_CAPACITY / 2
        const val logFilePath = "radarLogs.txt"
    }

    private val scheduler = Executors.newScheduledThreadPool(1)

    init {
        scheduler.scheduleAtFixedRate({ persist() }, 30, 30, TimeUnit.SECONDS)
    }

    private val list = LinkedBlockingDeque<RadarLog>(MAXIMUM_MEMORY_CAPACITY)

    override fun write(level: Radar.RadarLogLevel, message: String, type: Radar.RadarLogType?, createdAt: Date) {
        if (!list.offer(RadarLog(level, message, type, createdAt))) {
            persist()
            list.put(RadarLog(level, message, type, createdAt))
        }
    }


    override fun persist() {
        if(list.size >0) {
            val logs = getLogsFromDisk()

            if (logs.size+list.size > MAXIMUM_CAPACITY) {
                purgeOldestLogs(logs)
            }
            list.drainTo(logs)
            writeLogsToDisk(logs)
        }
    }


    override fun getFlushableLogsStash(): Flushable<RadarLog> {
        persist()
        val logs = mutableListOf<RadarLog>()
        getLogsFromDisk().drainTo(logs)
        return object : Flushable<RadarLog> {

            override fun get(): List<RadarLog> {
                return logs
            }

            override fun onFlush(success: Boolean) {
                // if success, clear the logs from disk
                if (success) {
                    RadarFileSystem(context).deleteFileAtPath(logFilePath)
                }
                // if not success, push the logs back into list and purge
                // put back into disk
                if (!success) {
                    //Reverse order to ensure the logs will purge correctly (oldest logs purged first)
                    val purgedLogs = LinkedBlockingDeque<RadarLog>()
                    logs.reverse()
                    logs.forEach {
                        if (!purgedLogs.offerFirst(it)) {
                            purgeOldestLogs(purgedLogs)
                        }
                    }
                    writeLogsToDisk(purgedLogs)
                }
            }

        }
    }

    /**
     * Clears oldest logs and adds a "purged" log line
     */
    private fun purgeOldestLogs(logs: LinkedBlockingDeque<RadarLog>) {
        val logsToDiscard = mutableListOf<RadarLog>()
        logs.drainTo(logsToDiscard, HALF_CAPACITY)
        logs.put(RadarLog(Radar.RadarLogLevel.DEBUG, "----- purged oldest logs -----", null))
    }

    /**
     * Gets logs from disk
     */
    private fun getLogsFromDisk(): LinkedBlockingDeque<RadarLog> {
        val json = RadarFileSystem(context).readFileAtPath(logFilePath)
        val logs = LinkedBlockingDeque<RadarLog>()
        if (json == ""){
            return logs
        }
        val jsonArray = JSONArray(json)
        for (i in 0 until jsonArray.length()) {
            val jsonObject = jsonArray.getJSONObject(i)
            val log = RadarLog.fromJson(jsonObject)
            logs.add(log)
        }
        return logs
    }

    /**
    * Writes logs to disk
    */
    private fun writeLogsToDisk(logs: Collection<RadarLog>) {
        val jsonArray = JSONArray()
        for (log in logs) {
            jsonArray.put(log.toJson())
        }
        RadarFileSystem(context).writeData(logFilePath, jsonArray.toString())
    }
}
