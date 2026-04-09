package io.radar.sdk.util

import android.content.Context
import org.json.JSONObject
import java.io.File
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

internal class RadarTypedFileStorage<T>(
    context: Context,
    fileName: String,
    private val serializer: (T) -> JSONObject,
    private val deserializer: (JSONObject) -> T
){
    private val file: File
    private val lock = ReentrantLock()
    private var cache: T? = null
    private var cacheLoaded = false

    init {
        val dir = File(context.filesDir, "RadarSDK")
        if (!dir.exists()) dir.mkdirs()
        file = File(dir, fileName)
    }

    fun read(): T? {
        lock.withLock {
            if (cacheLoaded) return cache
            cacheLoaded = true
            cache = try {
                if (!file.exists()) null
                else deserializer(JSONObject(file.readText()))
            } catch (e: Exception) {
                null
            }
            return cache
        }
    }

    fun write(value: T) {
        lock.withLock {
            cache = value
            cacheLoaded = true
            try {
                file.writeText(serializer(value).toString())
            } catch (_: Exception) {}
        }
    }

    fun modify(transform: (T?) -> T?) {
        lock.withLock {
            if (!cacheLoaded) {
                cacheLoaded = true
                cache = try {
                    if (!file.exists()) null
                    else deserializer(JSONObject(file.readText()))
                } catch (e: Exception) {
                    null
                }
            }
            cache = transform(cache)
            try {
                val c = cache
                if (c != null) {
                    file.writeText(serializer(c).toString())
                } else {
                    file.delete()
                }
            } catch (_: Exception) {}
        }
    }

    fun clear() {
        lock.withLock {
            cache = null
            cacheLoaded = true
            try {
                file.delete()
            } catch (_: Exception) {}
        }
    }
}