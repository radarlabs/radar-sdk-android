package io.radar.sdk.util

import io.radar.sdk.Radar
import io.radar.sdk.model.RadarLog
import java.util.concurrent.LinkedBlockingDeque
import android.content.Context

/**
 * Basic Log Buffer implementation that is backed by a field.
 */
internal class
RadarSimpleLogBuffer(override val context: Context) : RadarLogBuffer{

    private companion object {
        const val MAXIMUM_CAPACITY = 1000
        const val HALF_CAPACITY = MAXIMUM_CAPACITY / 2
        const val radarFileSystem = RadarFileSystem(context)
        const val logFilePath = "radarLogs.txt"
    }

    /**
     * Concurrency-safe list of logs with a maximum capacity of 1000
     */
    private val list = LinkedBlockingDeque<RadarLog>(MAXIMUM_CAPACITY)

    override fun write(level: Radar.RadarLogLevel, message: String, type: Radar.RadarLogType?) {
        //push into queue
        if (!list.offer(RadarLog(level, message, type))) {
            //instead of purge, flush into mem
            purgeOldestLogs()
            list.put(RadarLog(level, message, type))
        }
    }

    // flush into mem
    // get logs from disk into memory
    // add logs from memory
    // if its too long purge
    // put back into disk

    private fun flush() {
        val logs = getLogsFromDisk()
        //CHECK IF THIS IS THE RIGHT ORDERING
        logs.addAll(list)
        if (logs.size > MAXIMUM_CAPACITY) {
            purgeOldestLogs()
        }
        writeLogsToDisk(logs)
    }


    override fun getFlushableLogsStash(): Flushable<RadarLog> {
        //get logs from disk into memory


        val logs = mutableListOf<RadarLog>(getLogsFromDisk())
        list.drainTo(logs)
        return object : Flushable<RadarLog> {

            override fun get(): List<RadarLog> {
                return logs
            }

            override fun onFlush(success: Boolean) {
                // if success, clear the logs from disk
                if (success) {
                    radarFileSystem.delete(logFilePath)
                }
                // if not success, push the logs back into list and purge
                // put back into disk
                if (!success) {
                    // Reverse order to ensure the logs will purge correctly (oldest logs purged first)
                    logs.reverse()
                    logs.forEach {
                        if (!list.offerFirst(it)) {
                            purgeOldestLogs()
                        }
                    }
                    writeLogsToDisk(logs)
                }
            }

        }
    }

    /**
     * Clears oldest logs and adds a "purged" log line
     */
    private fun purgeOldestLogs() {
        val logs = mutableListOf<RadarLog>()
        list.drainTo(logs, HALF_CAPACITY)
        write(Radar.RadarLogLevel.DEBUG, "----- purged oldest logs -----", null)
    }

    /**
     * Gets logs from disk
     */
    private fun getLogsFromDisk(): LinkedBlockingDeque<RadarLog> {
        val json = radarFileSystem.read(logFilePath)
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
    private fun writeLogsToDisk(logs: LinkedBlockingDeque<RadarLog>) {
        val jsonArray = JSONArray()
        for (log in logs) {
            jsonArray.put(log.toJson())
        }
        radarFileSystem.write(logFilePath, jsonArray.toString())
    }
}
