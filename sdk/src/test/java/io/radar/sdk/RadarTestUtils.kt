package io.radar.sdk

import org.json.JSONObject

internal object RadarTestUtils {

    internal fun jsonObjectFromResource(resource: String): JSONObject {
        val str = RadarTest::class.java.getResource(resource)!!.readText()
        return JSONObject(str)
    }

}