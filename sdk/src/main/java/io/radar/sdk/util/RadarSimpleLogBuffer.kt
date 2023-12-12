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
 * Log Buffer implementation that persist.
 */
internal class
RadarSimpleLogBuffer(override val context: Context): RadarLogBuffer{

    private companion object {

        const val MAXIMUM_MEMORY_CAPACITY = 200

        const val MAXIMUM_CAPACITY = 500
        const val HALF_CAPACITY = MAXIMUM_CAPACITY / 2
        const val logFileDir = "radar_logs"
        var counter = 0

    }
    private var featureFlag = false


    private val scheduler = Executors.newScheduledThreadPool(1)

    init {
        scheduler.scheduleAtFixedRate({ persistLogs() }, 30, 30, TimeUnit.SECONDS)
        val file = File(context.filesDir, logFileDir)
        if (!file.exists()) {
            file.mkdir()
        }
        featureFlag = RadarSettings.getFeatureSettings(context).useLogPersistence
    }

    private val list = LinkedBlockingDeque<RadarLog>(MAXIMUM_MEMORY_CAPACITY)
    private val oldList = LinkedBlockingDeque<RadarLog>(MAXIMUM_CAPACITY)

    override fun setFeatureFlag(featureFlag: Boolean){
       this.featureFlag = featureFlag
    }

    override fun write(level: Radar.RadarLogLevel, message: String, type: Radar.RadarLogType?, createdAt: Date) {
        if (!oldList.offer(RadarLog(level, message, type))) {
            oldPurgeOldestLogs()
            oldList.put(RadarLog(level, message, type))
        }
        if (featureFlag) {
            if (!list.offer(RadarLog(level, message, type, createdAt))) {
                persistLogs()
                list.put(RadarLog(level, message, type, createdAt))
            }
        }        
    }


    override fun persistLogs() {
        if (featureFlag) {
            if(list.size > 0) {
                writeLogsToDisk(list)
                list.clear()
            }
            val files = RadarFileStorage(context).allFilesInDirectory(logFileDir)

            if ((files?.size ?: 0) > MAXIMUM_CAPACITY) {
                purgeOldestLogs()
            }
        }
    }


    override fun getFlushableLogsStash(): Flushable<RadarLog> {
       
        persistLogs()
        val logs = mutableListOf<RadarLog>()
        getLogsFromDisk().drainTo(logs)
        val oldLogs = mutableListOf<RadarLog>()
        oldList.drainTo(oldLogs)
        return object : Flushable<RadarLog> {

            override fun get(): List<RadarLog> {
                if (featureFlag) {
                    return logs
                }
                return oldLogs
            }

            override fun onFlush(success: Boolean) {
                // clear the logs from disk
                if (success) {
                    val files = RadarFileStorage(context).allFilesInDirectory(logFileDir)
                    if (!files.isNullOrEmpty()) {
                        for (file in files) {
                            file.delete()
                        }
                    }
                }

                // if not success, push the logs back into list and purge
                // put back into disk
                if (!success) {
                    oldLogs.reverse()
                    oldLogs.forEach {
                        if (!oldList.offerFirst(it)) {
                            oldPurgeOldestLogs()
                        }
                    }
                    if (featureFlag) {
                        purgeOldestLogs()
                    }
                }
            }

        }
        
    }



    private fun oldPurgeOldestLogs() {
        val oldLogs = mutableListOf<RadarLog>()
        oldList.drainTo(oldLogs, HALF_CAPACITY)
       if (!featureFlag) {
            write(Radar.RadarLogLevel.DEBUG, "----- purged oldest logs -----", null)
        } 
        
    }

    /**
     * Clears oldest logs and adds a "purged" log line
     */
    private fun purgeOldestLogs() {
        var files = RadarFileStorage(context).allFilesInDirectory(logFileDir)
        if (files.isNullOrEmpty()) {
            return 
        }
        var printedPurgedLogs = false
        while(files?.size ?: 0 > MAXIMUM_CAPACITY){
            val numberToPurge = min(HALF_CAPACITY,files?.size ?: 0)
            for(i in 0 until numberToPurge){
                files?.get(i)?.delete()
            }
            if(!printedPurgedLogs){
                writeLogsToDisk(listOf(RadarLog(Radar.RadarLogLevel.DEBUG, "----- purged oldest logs -----", null)))
                printedPurgedLogs = true
            }
            files = RadarFileStorage(context).allFilesInDirectory(logFileDir)
        }
    }

    /**
     * Gets logs from disk
     */
    private fun getLogsFromDisk(): LinkedBlockingDeque<RadarLog> {

        val files = RadarFileStorage(context).allFilesInDirectory(logFileDir)
        val logs = LinkedBlockingDeque<RadarLog>()
        if (files.isNullOrEmpty()) {
            return logs
        }

        for (file in files) {
            val jsonString = RadarFileStorage(context).readFileAtPath(logFileDir, file.name)
            val log = RadarLog.fromJson(JSONObject(jsonString))
            if(log != null){
                logs.add(log)
            }
        }
        return logs
    }

    /**
    * Writes logs to disk
    */
    private fun writeLogsToDisk(logs: Collection<RadarLog>) {
        for (log in logs) {
            val counterString = String.format("%04d", counter++)
            val fileName = "${log.createdAt.time / 1000}${counterString}"
            RadarFileStorage(context).writeData(logFileDir, fileName, log.toJson().toString())
        }
    }
}
