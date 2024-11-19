package io.radar.sdk

import java.util.Date

class RadarProfiler {
    private val startTimes: MutableMap<String, Long> = mutableMapOf()
    private val endTimes: MutableMap<String, Long> = mutableMapOf()
    fun start(tag: String = "") {
        startTimes[tag] = Date().time
    }

    fun end(tag: String = "") {
        if (startTimes.containsKey(tag)) {
            endTimes[tag] = Date().time
        }
    }

    fun get(tag: String = ""): Long {
        return endTimes.getOrDefault(tag, 0) - startTimes.getOrDefault(tag, 0)
    }

    fun formatted(): String {
        return endTimes.map { (tag, endTime) ->
            val diff = endTime - startTimes[tag]!!
            "${tag}: ${"%.2f".format(diff.toFloat() / 1000)}s"
        }.joinToString(", ")
    }
}