package io.radar.sdk.model

import org.json.JSONArray
import org.json.JSONObject

class RadarOperatingHour(
    val hours: MutableMap<String, Any>
) {

    internal companion object {
        @JvmStatic
        fun fromJson(obj: JSONObject): RadarOperatingHour? {
            if (obj == null) {
                return null
                
            }

            val dictionary = mutableMapOf<String, Any>()

            for (key in obj.keys()) {
                val value = obj.get(key)
                // unwrap day
                if (value is JSONArray) {
                    val list = mutableListOf<Any>()
                    // unwrap pairs within the day
                    for (i in 0 until value.length()) {
                        val item = value.get(i)
                        
                        if (item is JSONArray && item.length() == 2) {
                            val innerList = mutableListOf<Any>()    
                            innerList.add(item.get(0))
                            innerList.add(item.get(1))
                            list.add(innerList)
                        }
                        
                    }
                    dictionary[key] = list
                }
            }
            return RadarOperatingHour(dictionary)
        }
    }

}