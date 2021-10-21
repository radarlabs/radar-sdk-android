package io.radar.sdk.util

internal interface Flushable<T> {

    fun get(): List<T>

    fun onFlush(success: Boolean)
}