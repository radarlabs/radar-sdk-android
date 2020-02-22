package io.radar.sdk

import org.json.JSONObject

internal class RadarTestUtils {

    internal companion object {

        fun jsonObjectFromResource(resource: String): JSONObject? {
            val str = RadarTest::class.java.getResource(resource)!!.readText()
            return JSONObject(str)
        }

    }

}