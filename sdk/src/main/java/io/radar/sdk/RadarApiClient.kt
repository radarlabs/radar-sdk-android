package io.radar.sdk

import android.content.Context
import android.location.Location
import android.net.Uri
import android.os.Build
import android.os.SystemClock
import io.radar.sdk.model.RadarEvent.RadarEventVerification
import io.radar.sdk.Radar.RadarLocationSource
import io.radar.sdk.Radar.RadarStatus
import io.radar.sdk.model.*
import org.json.JSONObject
import java.net.URL
import java.util.*

internal class RadarApiClient(
    private val context: Context,
    internal var apiHelper: RadarApiHelper = RadarApiHelper()
) {

    interface RadarTrackApiCallback {
        fun onComplete(status: RadarStatus, res: JSONObject? = null, events: Array<RadarEvent>? = null, user: RadarUser? = null)
    }

    interface RadarContextApiCallback {
        fun onComplete(status: RadarStatus, res: JSONObject? = null, context: RadarContext? = null)
    }

    interface RadarSearchPlacesApiCallback {
        fun onComplete(status: RadarStatus, res: JSONObject? = null, places: Array<RadarPlace>? = null)
    }

    interface RadarSearchGeofencesApiCallback {
        fun onComplete(status: RadarStatus, res: JSONObject? = null, geofences: Array<RadarGeofence>? = null)
    }

    interface RadarSearchPointsApiCallback {
        fun onComplete(status: RadarStatus, res: JSONObject? = null, points: Array<RadarPoint>? = null)
    }

    interface RadarGeocodeApiCallback {
        fun onComplete(status: RadarStatus, res: JSONObject? = null, addresses: Array<RadarAddress>? = null)
    }

    interface RadarIpGeocodeApiCallback {
        fun onComplete(status: RadarStatus, res: JSONObject? = null, address: RadarAddress? = null)
    }

    interface RadarDistanceApiCallback {
        fun onComplete(status: RadarStatus, res: JSONObject? = null, routes: RadarRoutes? = null)
    }

    private fun headers(publishableKey: String): Map<String, String> {
        return mapOf(
            "Authorization" to publishableKey,
            "Content-Type" to "application/json",
            "X-Radar-Config" to "true",
            "X-Radar-Device-Make" to RadarUtils.deviceMake,
            "X-Radar-Device-Model" to RadarUtils.deviceModel,
            "X-Radar-Device-OS" to RadarUtils.deviceOS,
            "X-Radar-Device-Type" to RadarUtils.deviceType,
            "X-Radar-SDK-Version" to RadarUtils.sdkVersion
        )
    }

    internal fun getConfig() {
        val publishableKey = RadarSettings.getPublishableKey(context) ?: return

        val queryParams = StringBuilder()
        queryParams.append("installId=${RadarSettings.getInstallId(context)}")
        val userId = RadarSettings.getUserId(context)
        if (userId != null) {
            queryParams.append("&userId=$userId")
        }
        val deviceId = RadarUtils.getDeviceId(context)
        if (deviceId != null) {
            queryParams.append("&deviceId=$deviceId")
        }

        val host = RadarSettings.getHost(context)
        val uri = Uri.parse(host).buildUpon()
            .appendEncodedPath("v1/config?${queryParams}")
            .build()
        val url = URL(uri.toString())

        val headers = headers(publishableKey)

        apiHelper.request(context, "GET", url, headers, null, object : RadarApiHelper.RadarApiCallback {
            override fun onComplete(status: RadarStatus, res: JSONObject?) {
                if (res != null && res.has("meta")) {
                    val meta = res.getJSONObject("meta")
                    if (meta.has("config")) {
                        val config = meta.getJSONObject("config")
                        RadarSettings.setConfig(context, config)
                    }
                }
            }
        })
    }

    internal fun track(location: Location, stopped: Boolean, foreground: Boolean, source: RadarLocationSource, replayed: Boolean, callback: RadarTrackApiCallback? = null) {
        val publishableKey = RadarSettings.getPublishableKey(context)
        if (publishableKey == null) {
            callback?.onComplete(RadarStatus.ERROR_PUBLISHABLE_KEY)

            return
        }

        val params = JSONObject()
        params.putOpt("installId", RadarSettings.getInstallId(context))
        params.putOpt("id", RadarSettings.getId(context))
        params.putOpt("userId", RadarSettings.getUserId(context))
        params.putOpt("deviceId", RadarUtils.getDeviceId(context))
        params.putOpt("description", RadarSettings.getDescription(context))
        params.putOpt("metadata", RadarSettings.getMetadata(context))
        if (RadarSettings.getAdIdEnabled(context)) {
            params.putOpt("adId", RadarUtils.getAdId(context))
        }
        params.putOpt("latitude", location.latitude)
        params.putOpt("longitude", location.longitude)
        var accuracy = location.accuracy
        if (accuracy <= 0) {
            accuracy = 1F
        }
        params.putOpt("accuracy", accuracy)
        params.putOpt("speed", location.speed)
        params.putOpt("course", location.bearing)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            params.putOpt("verticalAccuracy", location.verticalAccuracyMeters)
            params.putOpt("speedAccuracy", location.speedAccuracyMetersPerSecond)
            params.putOpt("courseAccuracy", location.bearingAccuracyDegrees)
        }
        if (!foreground && Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            val updatedAtMsDiff = (SystemClock.elapsedRealtimeNanos() - location.elapsedRealtimeNanos) / 1000000
            params.putOpt("updatedAtMsDiff", updatedAtMsDiff)
        }
        params.putOpt("foreground", foreground)
        params.putOpt("stopped", stopped)
        params.putOpt("replayed", replayed)
        params.putOpt("deviceType", "Android")
        params.putOpt("deviceMake", RadarUtils.deviceMake)
        params.putOpt("sdkVersion", RadarUtils.sdkVersion)
        params.putOpt("deviceModel", RadarUtils.deviceModel)
        params.putOpt("deviceOS", RadarUtils.deviceOS)
        params.putOpt("deviceType", RadarUtils.deviceType)
        params.putOpt("deviceMake", RadarUtils.deviceMake)
        params.putOpt("country", RadarUtils.country)
        params.putOpt("timeZoneOffset", RadarUtils.timeZoneOffset)
        params.putOpt("uaChannelId", RadarUtils.getUaChannelId())
        params.putOpt("uaNamedUserId", RadarUtils.getUaNamedUserId())
        params.putOpt("uaSessionId", RadarUtils.getUaSessionId())
        params.putOpt("source", Radar.stringForSource(source))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            val mocked = location.isFromMockProvider
            params.putOpt("mocked", mocked)
        }

        val host = RadarSettings.getHost(context)
        val uri = Uri.parse(host).buildUpon()
            .appendEncodedPath("v1/track")
            .build()
        val url = URL(uri.toString())

        val headers = headers(publishableKey)

        apiHelper.request(context, "POST", url, headers, params, object : RadarApiHelper.RadarApiCallback {
            override fun onComplete(status: RadarStatus, res: JSONObject?) {
                if (status != RadarStatus.SUCCESS || res == null) {
                    val options = RadarSettings.getTrackingOptions(context)
                    if (options.replay == RadarTrackingOptions.RadarTrackingOptionsReplay.STOPS && stopped && !(source == RadarLocationSource.FOREGROUND_LOCATION || source == RadarLocationSource.BACKGROUND_LOCATION)) {
                        RadarState.setLastFailedStoppedLocation(context, location)
                    }

                    val errorIntent = RadarReceiver.createErrorIntent(status)
                    Radar.broadcastIntent(errorIntent)

                    callback?.onComplete(status)

                    return
                }

                RadarState.setLastFailedStoppedLocation(context, null)

                if (res.has("meta")) {
                    val meta = res.getJSONObject("meta")
                    if (meta.has("config")) {
                        val config = meta.getJSONObject("config")
                        RadarSettings.setConfig(context, config)
                    }
                }

                val events = res.optJSONArray("events")?.let { eventsArr ->
                    RadarEvent.fromJson(eventsArr)
                }
                val user = res.optJSONObject("user")?.let { userObj ->
                    RadarUser.fromJson(userObj)
                }
                if (events != null && user != null) {
                    val successIntent = RadarReceiver.createSuccessIntent(res, location)
                    Radar.broadcastIntent(successIntent)

                    callback?.onComplete(RadarStatus.SUCCESS, res, events, user)

                    return
                }

                val errorIntent = RadarReceiver.createErrorIntent(status)
                Radar.broadcastIntent(errorIntent)

                callback?.onComplete(RadarStatus.ERROR_SERVER)
            }
        })
    }

    internal fun verifyEvent(eventId: String, verification: RadarEventVerification, verifiedPlaceId: String? = null) {
        val publishableKey = RadarSettings.getPublishableKey(context) ?: return

        val params = JSONObject()
        params.putOpt("verification", verification)
        params.putOpt("verifiedPlaceId", verifiedPlaceId)

        val host = RadarSettings.getHost(context)
        val uri = Uri.parse(host).buildUpon()
            .appendEncodedPath("v1/events/")
            .appendEncodedPath(eventId)
            .appendEncodedPath("/verification")
            .build()
        val url = URL(uri.toString())

        val headers = headers(publishableKey)

        apiHelper.request(context, "PUT", url, headers, params)
    }

    internal fun getContext(
        location: Location,
        callback: RadarContextApiCallback
    ) {
        val publishableKey = RadarSettings.getPublishableKey(context)
        if (publishableKey == null) {
            callback.onComplete(RadarStatus.ERROR_PUBLISHABLE_KEY)

            return
        }

        val queryParams = StringBuilder()
        queryParams.append("coordinates=${location.latitude},${location.longitude}")

        val host = RadarSettings.getHost(context)
        val uri = Uri.parse(host).buildUpon()
            .appendEncodedPath("v1/context?${queryParams}")
        val url = URL(uri.toString())

        val headers = headers(publishableKey)

        apiHelper.request(context, "GET", url, headers, null, object: RadarApiHelper.RadarApiCallback {
            override fun onComplete(status: RadarStatus, res: JSONObject?) {
                if (status != RadarStatus.SUCCESS || res == null) {
                    callback.onComplete(status)

                    return
                }

                val context = res.optJSONObject("context")?.let { contextObj ->
                    RadarContext.fromJson(contextObj)
                }
                if (context != null) {
                    callback.onComplete(RadarStatus.SUCCESS, res, context)

                    return
                }

                callback.onComplete(RadarStatus.ERROR_SERVER)
            }
        })

    }

    internal fun searchPlaces(
        location: Location,
        radius: Int,
        chains: Array<String>?,
        categories: Array<String>?,
        groups: Array<String>?,
        limit: Int?,
        callback: RadarSearchPlacesApiCallback
    ) {
        val publishableKey = RadarSettings.getPublishableKey(context)
        if (publishableKey == null) {
            callback.onComplete(RadarStatus.ERROR_PUBLISHABLE_KEY)

            return
        }

        val queryParams = StringBuilder()
        queryParams.append("near=${location.latitude},${location.longitude}")
        queryParams.append("&radius=${radius}")
        queryParams.append("&limit=${limit}")
        if (chains?.isNotEmpty() == true) {
            queryParams.append("&chains=${chains.joinToString(separator = ",")}")
        }
        if (categories?.isNotEmpty() == true) {
            queryParams.append("&categories=${categories.joinToString(separator = ",")}")
        }
        if (groups?.isNotEmpty() == true) {
            queryParams.append("&groups=${groups.joinToString(separator = ",")}")
        }

        val host = RadarSettings.getHost(context)
        val uri = Uri.parse(host).buildUpon()
            .appendEncodedPath("v1/search/places?${queryParams}")
            .build()
        val url = URL(uri.toString())

        val headers = headers(publishableKey)

        apiHelper.request(context, "GET", url, headers, null, object : RadarApiHelper.RadarApiCallback {
            override fun onComplete(status: RadarStatus, res: JSONObject?) {
                if (status != RadarStatus.SUCCESS || res == null) {
                    callback.onComplete(status)

                    return
                }

                val places = res.optJSONArray("places")?.let { placesArr ->
                    RadarPlace.fromJson(placesArr)
                }
                if (places != null) {
                    callback.onComplete(RadarStatus.SUCCESS, res, places)

                    return
                }

                callback.onComplete(RadarStatus.ERROR_SERVER)
            }
        })
    }

    internal fun searchGeofences(
        location: Location,
        radius: Int,
        tags: Array<String>?,
        limit: Int?,
        callback: RadarSearchGeofencesApiCallback
    ) {
        val publishableKey = RadarSettings.getPublishableKey(context)
        if (publishableKey == null) {
            callback.onComplete(RadarStatus.ERROR_PUBLISHABLE_KEY)

            return
        }

        val queryParams = StringBuilder()
        queryParams.append("near=${location.latitude},${location.longitude}")
        queryParams.append("&radius=${radius}")
        queryParams.append("&limit=${limit}")
        if (tags?.isNotEmpty() == true) {
            queryParams.append("&tags=${tags.joinToString(separator = ",")}")
        }

        val host = RadarSettings.getHost(context)
        val uri = Uri.parse(host).buildUpon()
            .appendEncodedPath("v1/search/geofences?${queryParams}")
            .build()
        val url = URL(uri.toString())

        val headers = headers(publishableKey)

        apiHelper.request(context, "GET", url, headers, null, object : RadarApiHelper.RadarApiCallback {
            override fun onComplete(status: RadarStatus, res: JSONObject?) {
                if (status != RadarStatus.SUCCESS || res == null) {
                    callback.onComplete(status)

                    return
                }

                val geofences = res.optJSONArray("geofences")?.let { geofencesArr ->
                    RadarGeofence.fromJson(geofencesArr)
                }
                if (geofences != null) {
                    callback.onComplete(RadarStatus.SUCCESS, res, geofences)

                    return
                }

                callback.onComplete(RadarStatus.ERROR_SERVER)
            }
        })
    }

    internal fun searchPoints(
        location: Location,
        radius: Int,
        tags: Array<String>?,
        limit: Int?,
        callback: RadarSearchPointsApiCallback
    ) {
        val publishableKey = RadarSettings.getPublishableKey(context)
        if (publishableKey == null) {
            callback.onComplete(RadarStatus.ERROR_PUBLISHABLE_KEY)

            return
        }

        val queryParams = StringBuilder()
        queryParams.append("near=${location.latitude},${location.longitude}")
        queryParams.append("&radius=${radius}")
        queryParams.append("&limit=${limit}")
        if (tags?.isNotEmpty() == true) {
            queryParams.append("&tags=${tags.joinToString(separator = ",")}")
        }

        val host = RadarSettings.getHost(context)
        val uri = Uri.parse(host).buildUpon()
            .appendEncodedPath("v1/search/points?${queryParams}")
            .build()
        val url = URL(uri.toString())

        val headers = headers(publishableKey)

        apiHelper.request(context, "GET", url, headers, null, object : RadarApiHelper.RadarApiCallback {
            override fun onComplete(status: RadarStatus, res: JSONObject?) {
                if (status != RadarStatus.SUCCESS || res == null) {
                    callback.onComplete(status)

                    return
                }

                val points = res.optJSONArray("points")?.let { pointsArr ->
                    RadarPoint.fromJson(pointsArr)
                }
                if (points != null) {
                    callback.onComplete(RadarStatus.SUCCESS, res, points)

                    return
                }

                callback.onComplete(RadarStatus.ERROR_SERVER)
            }
        })
    }
    internal fun autocomplete(
        query: String,
        near: Location,
        limit: Int,
        callback: RadarGeocodeApiCallback
    ) {
        val publishableKey = RadarSettings.getPublishableKey(context)
        if (publishableKey == null) {
            callback.onComplete(RadarStatus.ERROR_PUBLISHABLE_KEY)

            return
        }

        val queryParams = StringBuilder()
        queryParams.append("query=${query}")
        queryParams.append("&near=${near.latitude},${near.longitude}")
        queryParams.append("&limit=${limit}")

        val host = RadarSettings.getHost(context)
        val uri = Uri.parse(host).buildUpon()
            .appendEncodedPath("v1/search/autocomplete?${queryParams}")
            .build()
        val url = URL(uri.toString())

        val headers = headers(publishableKey)

        apiHelper.request(context, "GET", url, headers, null, object : RadarApiHelper.RadarApiCallback {
            override fun onComplete(status: RadarStatus, res: JSONObject?) {
                if (status != RadarStatus.SUCCESS || res == null) {
                    callback.onComplete(status)

                    return
                }

                val addresses = res.optJSONArray("addresses")?.let { addressesArr ->
                    RadarAddress.fromJson(addressesArr)
                }
                if (addresses != null) {
                    callback.onComplete(RadarStatus.SUCCESS, res, addresses)

                    return
                }

                callback.onComplete(RadarStatus.ERROR_SERVER)
            }
        })
    }

    internal fun geocode(
        query: String,
        callback: RadarGeocodeApiCallback
    ) {
        val publishableKey = RadarSettings.getPublishableKey(context)
        if (publishableKey == null) {
            callback.onComplete(RadarStatus.ERROR_PUBLISHABLE_KEY)

            return
        }

        val queryParams = StringBuilder()
        queryParams.append("query=${query}")

        val host = RadarSettings.getHost(context)
        val uri = Uri.parse(host).buildUpon()
            .appendEncodedPath("v1/geocode/forward?${queryParams}")
            .build()
        val url = URL(uri.toString())

        val headers = headers(publishableKey)

        apiHelper.request(context, "GET", url, headers, null, object: RadarApiHelper.RadarApiCallback {
            override fun onComplete(status: RadarStatus, res: JSONObject?) {
                if (status != RadarStatus.SUCCESS || res == null) {
                    callback.onComplete(status)

                    return
                }

                val addresses = res.optJSONArray("addresses")?.let { addressesArr ->
                    RadarAddress.fromJson(addressesArr)
                }
                if (addresses != null) {
                    callback.onComplete(RadarStatus.SUCCESS, res, addresses)

                    return
                }

                callback.onComplete(RadarStatus.ERROR_SERVER)
            }
        })
    }

    internal fun reverseGeocode(
        location: Location,
        callback: RadarGeocodeApiCallback
    ) {
        val publishableKey = RadarSettings.getPublishableKey(context)
        if (publishableKey == null) {
            callback.onComplete(RadarStatus.ERROR_PUBLISHABLE_KEY)

            return
        }

        val queryParams = StringBuilder()
        queryParams.append("coordinates=${location.latitude},${location.longitude}")

        val host = RadarSettings.getHost(context)
        val uri = Uri.parse(host).buildUpon()
            .appendEncodedPath("v1/geocode/reverse?${queryParams}")
            .build()
        val url = URL(uri.toString())

        val headers = headers(publishableKey)

        apiHelper.request(context, "GET", url, headers, null, object: RadarApiHelper.RadarApiCallback {
            override fun onComplete(status: RadarStatus, res: JSONObject?) {
                if (status != RadarStatus.SUCCESS || res == null) {
                    callback.onComplete(status)

                    return
                }

                val addresses = res.optJSONArray("addresses")?.let { addressesArr ->
                    RadarAddress.fromJson(addressesArr)
                }
                if (addresses != null) {
                    callback.onComplete(RadarStatus.SUCCESS, res, addresses)

                    return
                }

                callback.onComplete(RadarStatus.ERROR_SERVER)
            }
        })
    }

    internal fun ipGeocode(
        callback: RadarIpGeocodeApiCallback
    ) {
        val publishableKey = RadarSettings.getPublishableKey(context)
        if (publishableKey == null) {
            callback.onComplete(RadarStatus.ERROR_PUBLISHABLE_KEY)

            return
        }

        val host = RadarSettings.getHost(context)
        val uri = Uri.parse(host).buildUpon()
            .appendEncodedPath("v1/geocode/ip")
            .build()

        val url = URL(uri.toString())

        val headers = headers(publishableKey)

        apiHelper.request(context, "GET", url, headers, null, object: RadarApiHelper.RadarApiCallback {
            override fun onComplete(status: RadarStatus, res: JSONObject?) {
                if (status != RadarStatus.SUCCESS || res == null) {
                    callback.onComplete(status)

                    return
                }

                val address: RadarAddress? = res.optJSONObject("address")?.let { addressObj ->
                    RadarAddress.fromJson(addressObj)
                }

                if (address != null) {
                    callback.onComplete(RadarStatus.SUCCESS, res, address)

                    return
                }

                callback.onComplete(RadarStatus.ERROR_SERVER)
            }
        })
    }

    internal fun getDistance(
        origin: Location,
        destination: Location,
        modes: EnumSet<Radar.RadarRouteMode>,
        units: Radar.RadarRouteUnits,
        geometryPoints: Int,
        callback: RadarDistanceApiCallback
    ) {
        val publishableKey = RadarSettings.getPublishableKey(context)
        if (publishableKey == null) {
            callback.onComplete(RadarStatus.ERROR_PUBLISHABLE_KEY)

            return
        }

        val queryParams = StringBuilder()
        queryParams.append("origin=${origin.latitude},${origin.longitude}")
        queryParams.append("&destination=${destination.latitude},${destination.longitude}")
        val modesList = mutableListOf<String>()
        if (modes.contains(Radar.RadarRouteMode.FOOT)) {
            modesList.add("foot")
        }
        if (modes.contains(Radar.RadarRouteMode.BIKE)) {
            modesList.add("bike")
        }
        if (modes.contains(Radar.RadarRouteMode.CAR)) {
            modesList.add("car")
        }
        queryParams.append("&modes=${modesList.joinToString(",")}")
        if (units == Radar.RadarRouteUnits.METRIC) {
            queryParams.append("&units=metric")
        } else {
            queryParams.append("&units=imperial")
        }
        if (geometryPoints > 1) {
            queryParams.append("&geometryPoints=${geometryPoints}")
        }
        queryParams.append("&geometry=linestring")

        val host = RadarSettings.getHost(context)
        val uri = Uri.parse(host).buildUpon()
            .appendEncodedPath("v1/route/distance?${queryParams}")
            .build()
        val url = URL(uri.toString())

        val headers = headers(publishableKey)

        apiHelper.request(context, "GET", url, headers, null, object : RadarApiHelper.RadarApiCallback {
            override fun onComplete(status: RadarStatus, res: JSONObject?) {
                if (status != RadarStatus.SUCCESS || res == null) {
                    callback.onComplete(status)

                    return
                }

                val routes = res.optJSONObject("routes")?.let { routesObj ->
                    RadarRoutes.fromJson(routesObj)
                }
                if (routes != null) {
                    callback.onComplete(RadarStatus.SUCCESS, res, routes)

                    return
                }

                callback.onComplete(RadarStatus.ERROR_SERVER)
            }
        })
    }

}
