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
        var fileCounter = 0

    }
    private var persistentLogFeatureFlag = false


    private val scheduler = Executors.newScheduledThreadPool(1)

    init {
        scheduler.scheduleAtFixedRate({ persistLogs() }, 30, 30, TimeUnit.SECONDS)
        val file = File(context.filesDir, logFileDir)
        if (!file.exists()) {
            file.mkdir()
        }
        persistentLogFeatureFlag = RadarSettings.getFeatureSettings(context).useLogPersistence
    }

    private val list = LinkedBlockingDeque<RadarLog>(MAXIMUM_CAPACITY)

    override fun setPersistentLogFeatureFlag(persistentLogFeatureFlag: Boolean){
       this.persistentLogFeatureFlag = persistentLogFeatureFlag
    }

    override fun write(level: Radar.RadarLogLevel, message: String, type: Radar.RadarLogType?, createdAt: Date) {
        if (persistentLogFeatureFlag) {
            if (!list.offer(RadarLog(level, message, type, createdAt))) {
                persistLogs()
                list.put(RadarLog(level, message, type, createdAt))
            // The size of the buffer is larger than the max allowed number of logs that should be
            // allowed on the buffer in the new implementation to not mess with the old implementation.
            // TODO: Remove once throttling is done.
            } else {
                if (list.size > MAXIMUM_MEMORY_CAPACITY) {
                    persistLogs()
                }
            }
        } else {
            if (!list.offer(RadarLog(level, message, type, createdAt))) {
                oldPurgeOldestLogs()
                list.put(RadarLog(level, message, type, createdAt))
            }
        }
    }


    override fun persistLogs() {
        if (persistentLogFeatureFlag) {
            if(list.size > 0) {
                writeLogsToDisk(list)
                list.clear()
            }

            purgeOldestLogs()
            
        }
    }


    override fun getFlushableLogsStash(): Flushable<RadarLog> {
       
        persistLogs()
        val logs = mutableListOf<RadarLog>()
        if(persistentLogFeatureFlag){
            getLogsFromDisk().drainTo(logs)
            val files = RadarFileStorage(context).allFilesInDirectory(logFileDir, comparator)
            for (i in 0 until min(logs.size,files?.size ?:0)){
                files?.get(i)?.delete()
            }
        } else {
            list.drainTo(logs)
        }
        return object : Flushable<RadarLog> {

            override fun get(): List<RadarLog> {
                return logs
            }

            override fun onFlush(success: Boolean) {
                // clear the logs from disk
                if (!success) {
                   if (persistentLogFeatureFlag) {
                        writeLogsToDisk(logs)
                        purgeOldestLogs()
                    } else {
                        logs.reverse()
                        logs.forEach {
                            if (!list.offerFirst(it)) {
                                oldPurgeOldestLogs()
                            }
                        }
                    }
                }
            }

        }
        
    }



    private fun oldPurgeOldestLogs() {
        val oldLogs = mutableListOf<RadarLog>()
        list.drainTo(oldLogs, HALF_CAPACITY)
       if (!persistentLogFeatureFlag) {
           write(Radar.RadarLogLevel.DEBUG, "----- purged oldest logs -----", null)
       }
    }

    /**
     * Clears oldest logs and adds a "purged" log line
     */
    private fun purgeOldestLogs() {
        var files = RadarFileStorage(context).allFilesInDirectory(logFileDir, comparator)
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
            files = RadarFileStorage(context).allFilesInDirectory(logFileDir, comparator)
        }
    }

    /**
     * Gets logs from disk
     */
    private fun getLogsFromDisk(): LinkedBlockingDeque<RadarLog> {

        val files = RadarFileStorage(context).allFilesInDirectory(logFileDir, comparator)
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
            val counterString = String.format("%04d", fileCounter++)
            val fileName = "${log.createdAt.time / 1000}_${counterString}"
            RadarFileStorage(context).writeData(logFileDir, fileName, log.toJson().toString())
        }
    }

    private val comparator = Comparator<File> { file1, file2 ->
            val number1 = file1.name.replace("_","").toLongOrNull() ?: 0L
            val number2 = file2.name.replace("_","").toLongOrNull() ?: 0L
            number1.compareTo(number2)
        }
}
