package io.radar.sdk

import android.content.Context
import org.json.JSONObject
import java.net.URL

internal class RadarApiRequest private constructor(
    val method: String,
    val url: URL,
    val headers: Map<String, String>?,
    val params: JSONObject?,
    val sleep: Boolean,
    val callback: RadarApiHelper.RadarApiCallback? = null
) {
    companion object {
        fun get(url: URL, sleep: Boolean = false) = Builder("GET", url, sleep)
    }

    class Builder(
        private val method: String,
        private val url: URL,
        private val sleep: Boolean = false
    ) {

        private var headers: Map<String, String>? = null
        private var params: JSONObject? = null
        private var callback: RadarApiHelper.RadarApiCallback? = null

        fun headers(headers: Map<String, String>?) = apply { this.headers = headers }
        fun params(params: JSONObject?) = apply { this.params = params }
        fun callback(callback: RadarApiHelper.RadarApiCallback?) = apply { this.callback = callback }
        fun build() = RadarApiRequest(method, url, headers, params, sleep, callback)
    }
}