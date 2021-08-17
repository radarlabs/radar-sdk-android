package io.radar.sdk.model

import org.json.JSONArray

/**
 * Represents routes between multiple origins and destinations.
 *
 * @see [](https://radar.io/documentation/api#matrix)
 */
class RadarRouteMatrix(
    /**
     * The routes between multiple origins and destinations.
     */
    val matrix: Array<Array<RadarRoute?>?>?
) {

    internal companion object {
        @JvmStatic
        fun fromJson(arr: JSONArray?): RadarRouteMatrix? {
            if (arr == null) {
                return null
            }

            val matrix = arrayOfNulls<Array<RadarRoute?>>(arr.length())
            for (i in 0 until arr.length()) {
                val col = arr.getJSONArray(i)
                val routes = arrayOfNulls<RadarRoute>(col.length())
                for (j in 0 until col.length()) {
                    val route = RadarRoute.fromJson(col.getJSONObject(j))
                    routes[j] = route
                }
                matrix[i] = routes
            }

            return RadarRouteMatrix(matrix)
        }
    }

    fun routeBetween(originIndex: Int, destinationIndex: Int): RadarRoute? {
        if (matrix == null) {
            return null
        }

        if (originIndex >= matrix.size) {
            return null
        }

        val routes = matrix[originIndex] ?: return null

        if (destinationIndex >= routes.size) {
            return null
        }

        return routes[destinationIndex]
    }

    fun toJson(): JSONArray {
        val rows = JSONArray()
        matrix?.forEachIndexed { i, routes ->
            val col = JSONArray()
            routes?.forEachIndexed { j, route ->
                col.put(j, route?.toJson())
            }
            rows.put(i, col)
        }
        return rows
    }

}
