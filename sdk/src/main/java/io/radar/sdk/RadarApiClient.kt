package io.radar.sdk

import android.content.Context
import android.location.Location
import android.net.Uri
import android.os.Build
import android.os.SystemClock
import io.radar.sdk.model.RadarAddress
import io.radar.sdk.model.RadarEvent
import io.radar.sdk.model.RadarEvent.RadarEventVerification
import io.radar.sdk.model.RadarGeofence
import io.radar.sdk.Radar.RadarLocationSource
import io.radar.sdk.model.RadarPlace
import io.radar.sdk.model.RadarRegion
import io.radar.sdk.Radar.RadarStatus
import io.radar.sdk.model.RadarUser
import org.json.JSONObject
import java.net.URL

internal class RadarApiClient(
    private val context: Context,
    internal var apiHelper: RadarApiHelper = RadarApiHelper()
) {

    interface RadarTrackApiCallback {
        fun onComplete(status: RadarStatus, res: JSONObject? = null, events: Array<RadarEvent>? = null, user: RadarUser? = null)
    }

    interface RadarSearchPlacesApiCallback {
        fun onComplete(status: RadarStatus, res: JSONObject? = null, places: Array<RadarPlace>? = null)
    }

    interface RadarSearchGeofencesApiCallback {
        fun onComplete(status: RadarStatus, res: JSONObject? = null, geofences: Array<RadarGeofence>? = null)
    }

    interface RadarGeocodeApiCallback {
        fun onComplete(status: RadarStatus, res: JSONObject? = null, addresses: Array<RadarAddress>? = null)
    }

    interface RadarIPGeocodeApiCallback {
        fun onComplete(status: RadarStatus, res: JSONObject? = null, region: Array<RadarRegion>? = null)
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
        queryParams.append("&deviceType=${RadarUtils.deviceType}")
        queryParams.append("&deviceMake=${RadarUtils.deviceMake}")
        queryParams.append("&sdkVersion=${RadarUtils.sdkVersion}")
        queryParams.append("&deviceModel=${RadarUtils.deviceModel}")
        queryParams.append("&deviceOS=${RadarUtils.deviceOS}")

        val host = RadarSettings.getHost(context)
        val uri = Uri.parse(host).buildUpon()
            .appendEncodedPath("v1/config?${queryParams}")
            .build()
        val url = URL(uri.toString())

        val headers = mapOf(
            "Authorization" to publishableKey,
            "Content-Type" to "application/json",
            "User-Agent" to RadarUtils.userAgent,
            "X-Radar-Config" to "true"
        )

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

    internal fun track(location: Location, stopped: Boolean, source: RadarLocationSource, replayed: Boolean, callback: RadarTrackApiCallback? = null) {
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
        params.putOpt("adId", RadarUtils.getAdId(context))
        params.putOpt("latitude", location.latitude)
        params.putOpt("longitude", location.longitude)
        params.putOpt("accuracy", location.accuracy)
        val foreground = RadarActivityLifecycleCallbacks.foreground
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
        params.putOpt("country", RadarUtils.country)
        params.putOpt("timeZoneOffset", RadarUtils.timeZoneOffset)
        params.putOpt("uaChannelId", RadarUtils.getUaChannelId())
        params.putOpt("uaNamedUserId", RadarUtils.getUaNamedUserId())
        params.putOpt("uaSessionId", RadarUtils.getUaSessionId())
        params.putOpt("source", Radar.stringForSource(source))
        params.putOpt("deviceType", RadarUtils.deviceType)
        params.putOpt("deviceMake", RadarUtils.deviceMake)
        params.putOpt("sdkVersion", RadarUtils.sdkVersion)
        params.putOpt("deviceModel", RadarUtils.deviceModel)
        params.putOpt("deviceOS", RadarUtils.deviceOS)

        val host = RadarSettings.getHost(context)
        val uri = Uri.parse(host).buildUpon()
            .appendEncodedPath("v1/track")
            .build()
        val url = URL(uri.toString())

        val headers = mapOf(
            "Authorization" to publishableKey,
            "Content-Type" to "application/json",
            "User-Agent" to RadarUtils.userAgent,
            "X-Radar-Config" to "true"
        )

        apiHelper.request(context, "POST", url, headers, params, object : RadarApiHelper.RadarApiCallback {
            override fun onComplete(status: RadarStatus, res: JSONObject?) {
                if (status != RadarStatus.SUCCESS || res == null) {
                    val options = RadarSettings.getTrackingOptions(context)
                    if (options.replay == RadarTrackingOptions.RadarTrackingOptionsReplay.REPLAY_STOPS && stopped && !(source == RadarLocationSource.FOREGROUND_LOCATION || source == RadarLocationSource.BACKGROUND_LOCATION)) {
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
                    RadarEvent.eventsFromJSONArray(eventsArr)
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
        params.putOpt("deviceType", RadarUtils.deviceType)
        params.putOpt("deviceMake", RadarUtils.deviceMake)
        params.putOpt("sdkVersion", RadarUtils.sdkVersion)
        params.putOpt("deviceModel", RadarUtils.deviceModel)
        params.putOpt("deviceOS", RadarUtils.deviceOS)

        val host = RadarSettings.getHost(context)
        val uri = Uri.parse(host).buildUpon()
            .appendEncodedPath("v1/events/")
            .appendEncodedPath(eventId)
            .appendEncodedPath("/verification")
            .build()
        val url = URL(uri.toString())

        val headers = mapOf(
            "Authorization" to publishableKey,
            "Content-Type" to "application/json",
            "User-Agent" to RadarUtils.userAgent,
            "X-Radar-Config" to "true"
        )

        apiHelper.request(context, "PUT", url, headers, params)
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
        queryParams.append("latitude=${location.latitude}")
        queryParams.append("&longitude=${location.longitude}")
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
        queryParams.append("&deviceType=${RadarUtils.deviceType}")
        queryParams.append("&deviceMake=${RadarUtils.deviceMake}")
        queryParams.append("&sdkVersion=${RadarUtils.sdkVersion}")
        queryParams.append("&deviceModel=${RadarUtils.deviceModel}")
        queryParams.append("&deviceOS=${RadarUtils.deviceOS}")

        val host = RadarSettings.getHost(context)
        val uri = Uri.parse(host).buildUpon()
            .appendEncodedPath("v1/search/places?${queryParams}")
            .build()
        val url = URL(uri.toString())

        val headers = mapOf(
            "Authorization" to publishableKey,
            "Content-Type" to "application/json",
            "User-Agent" to RadarUtils.userAgent,
            "X-Radar-Config" to "true"
        )

        apiHelper.request(context, "GET", url, headers, null, object : RadarApiHelper.RadarApiCallback {
            override fun onComplete(status: RadarStatus, res: JSONObject?) {
                if (status != RadarStatus.SUCCESS || res == null) {
                    callback.onComplete(status)

                    return
                }

                val places = res.optJSONArray("places")?.let { placesArr ->
                    RadarPlace.fromJSONArray(placesArr)
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
        queryParams.append("latitude=${location.latitude}")
        queryParams.append("&longitude=${location.longitude}")
        queryParams.append("&radius=${radius}")
        queryParams.append("&limit=${limit}")
        if (tags?.isNotEmpty() == true) {
            queryParams.append("&tags=${tags.joinToString(separator = ",")}")
        }
        queryParams.append("&deviceType=${RadarUtils.deviceType}")
        queryParams.append("&deviceMake=${RadarUtils.deviceMake}")
        queryParams.append("&sdkVersion=${RadarUtils.sdkVersion}")
        queryParams.append("&deviceModel=${RadarUtils.deviceModel}")
        queryParams.append("&deviceOS=${RadarUtils.deviceOS}")

        val host = RadarSettings.getHost(context)
        val uri = Uri.parse(host).buildUpon()
            .appendEncodedPath("v1/search/geofences?${queryParams}")
            .build()
        val url = URL(uri.toString())

        val headers = mapOf(
            "Authorization" to publishableKey,
            "Content-Type" to "application/json",
            "User-Agent" to RadarUtils.userAgent,
            "X-Radar-Config" to "true"
        )

        apiHelper.request(context, "GET", url, headers, null, object : RadarApiHelper.RadarApiCallback {
            override fun onComplete(status: RadarStatus, res: JSONObject?) {
                if (status != RadarStatus.SUCCESS || res == null) {
                    callback.onComplete(status)

                    return
                }

                val geofences = res.optJSONArray("geofences")?.let { geofencesArr ->
                    RadarGeofence.fromJSONArray(geofencesArr)
                }
                if (geofences != null) {
                    callback.onComplete(RadarStatus.SUCCESS, res, geofences)

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
        queryParams.append("text=${query}")

        val host = RadarSettings.getHost(context)
        val uri = Uri.parse(host).buildUpon()
            .appendEncodedPath("v1/geocode/forward?${queryParams}")
            .build()
        val url = URL(uri.toString())

        val headers = mapOf(
            "Authorization" to publishableKey,
            "Content-Type" to "application/json",
            "User-Agent" to RadarUtils.userAgent,
            "X-Radar-Config" to "true"
        )

        apiHelper.request(context, "GET", url, headers, null, object: RadarApiHelper.RadarApiCallback {
            override fun onComplete(status: RadarStatus, res: JSONObject?) {
                if (status != RadarStatus.SUCCESS || res == null) {
                    callback.onComplete(status)

                    return
                }

                val addresses = res.optJSONArray("addresses")?.let { addressesArr ->
                    RadarAddress.fromJSONArray(addressesArr)
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
        queryParams.append("latitude=${location.latitude}")
        queryParams.append("&longitude=${location.longitude}")

        val host = RadarSettings.getHost(context)
        val uri = Uri.parse(host).buildUpon()
            .appendEncodedPath("v1/geocode/reverse?${queryParams}")
            .build()
        val url = URL(uri.toString())

        val headers = mapOf(
            "Authorization" to publishableKey,
            "Content-Type" to "application/json",
            "User-Agent" to RadarUtils.userAgent,
            "X-Radar-Config" to "true"
        )

        apiHelper.request(context, "GET", url, headers, null, object: RadarApiHelper.RadarApiCallback {
            override fun onComplete(status: RadarStatus, res: JSONObject?) {
                if (status != RadarStatus.SUCCESS || res == null) {
                    callback.onComplete(status)

                    return
                }

                val addresses = res.optJSONArray("addresses")?.let { addressesArr ->
                    RadarAddress.fromJSONArray(addressesArr)
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
        callback: RadarIPGeocodeApiCallback
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

        val headers = mapOf(
            "Authorization" to publishableKey,
            "Content-Type" to "application/json",
            "User-Agent" to RadarUtils.userAgent,
            "X-Radar-Config" to "true"
        )

        apiHelper.request(context, "GET", url, headers, null, object: RadarApiHelper.RadarApiCallback {
            override fun onComplete(status: RadarStatus, res: JSONObject?) {
                if (status != RadarStatus.SUCCESS || res == null) {
                    callback.onComplete(status)

                    return
                }

                val regions = res.optJSONArray("regions")?.let { regionsArr ->
                    RadarRegion.fromJSONArray(regionsArr)
                }
                if (regions != null) {
                    callback.onComplete(RadarStatus.SUCCESS, res, regions)

                    return
                }

                callback.onComplete(RadarStatus.ERROR_SERVER)
            }
        })
    }
}
