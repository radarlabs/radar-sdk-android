package io.radar.sdk

import android.content.Context
import android.location.Location
import android.net.Uri
import android.os.Build
import android.os.SystemClock
import androidx.annotation.RequiresApi
import io.radar.sdk.Radar.RadarLocationSource
import io.radar.sdk.Radar.RadarStatus
import io.radar.sdk.model.RadarAddress
import io.radar.sdk.model.RadarBeacon
import io.radar.sdk.model.RadarContext
import io.radar.sdk.model.RadarEvent
import io.radar.sdk.model.RadarEvent.RadarEventVerification
import io.radar.sdk.model.RadarGeofence
import io.radar.sdk.model.RadarLog
import io.radar.sdk.model.RadarPlace
import io.radar.sdk.model.RadarRouteMatrix
import io.radar.sdk.model.RadarRoutes
import io.radar.sdk.model.RadarTrip
import io.radar.sdk.model.RadarUser
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.net.URL
import java.util.*

@Suppress("LongParameterList", "TooManyFunctions", "LargeClass")
internal class RadarApiClient(
    private val app: RadarApplication,
    private val apiHelper: RadarApiHelper
) {

    interface RadarTrackApiCallback {
        fun onComplete(
            status: RadarStatus,
            res: JSONObject? = null,
            events: Array<RadarEvent>? = null,
            user: RadarUser? = null,
            nearbyGeofences: Array<RadarGeofence>? = null
        )
    }

    interface RadarTripApiCallback {
        fun onComplete(
            status: RadarStatus,
            res: JSONObject? = null,
            trip: RadarTrip? = null,
            events: Array<RadarEvent>? = null
        )
    }

    interface RadarContextApiCallback {
        fun onComplete(
            status: RadarStatus,
            res: JSONObject? = null,
            context: RadarContext? = null
        )
    }

    interface RadarSearchPlacesApiCallback {
        fun onComplete(
            status: RadarStatus,
            res: JSONObject? = null,
            places: Array<RadarPlace>? = null
        )
    }

    interface RadarSearchGeofencesApiCallback {
        fun onComplete(
            status: RadarStatus,
            res: JSONObject? = null,
            geofences: Array<RadarGeofence>? = null
        )
    }

    interface RadarSearchBeaconsApiCallback {
        fun onComplete(
            status: RadarStatus,
            res: JSONObject? = null,
            beacons: Array<RadarBeacon>? = null
        )
    }

    interface RadarGeocodeApiCallback {
        fun onComplete(
            status: RadarStatus,
            res: JSONObject? = null,
            addresses: Array<RadarAddress>? = null
        )
    }

    interface RadarIpGeocodeApiCallback {
        fun onComplete(
            status: RadarStatus,
            res: JSONObject? = null,
            address: RadarAddress? = null,
            proxy: Boolean = false
        )
    }

    interface RadarDistanceApiCallback {
        fun onComplete(
            status: RadarStatus,
            res: JSONObject? = null,
            routes: RadarRoutes? = null
        )
    }

    interface RadarMatrixApiCallback {
        fun onComplete(
            status: RadarStatus,
            res: JSONObject? = null,
            matrix: RadarRouteMatrix? = null
        )
    }

    internal interface RadarLogCallback {
        fun onComplete(
            status: RadarStatus,
            res: JSONObject? = null
        )
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
        val publishableKey = app.settings.getPublishableKey() ?: return

        val queryParams = StringBuilder()
        queryParams.append("installId=${app.settings.getInstallId()}")
        queryParams.append("&sessionId=${app.settings.getSessionId()}")
        queryParams.append("&locationAuthorization=${RadarUtils.getLocationAuthorization(app)}")
        queryParams.append("&locationAccuracyAuthorization=${RadarUtils.getLocationAccuracyAuthorization(app)}")

        val host = app.settings.getHost()
        val uri = Uri.parse(host).buildUpon()
            .appendEncodedPath("v1/config?${queryParams}")
            .build()
        val url = URL(uri.toString())

        val headers = headers(publishableKey)

        apiHelper.request(
            RadarApiRequest.get(url)
                .headers(headers)
                .callback(object : RadarApiHelper.RadarApiCallback {
                    override fun onComplete(status: RadarStatus, res: JSONObject?) {
                        val onComplete = {
                            if (res != null && res.has("meta")) {
                                val meta = res.getJSONObject("meta")
                                if (meta.has("config")) {
                                    val config = meta.getJSONObject("config")
                                    app.settings.setConfig(config)
                                }
                            }
                        }
                        if (status == RadarStatus.SUCCESS) {
                            app.flushLogs(onComplete)
                        } else {
                            onComplete.invoke()
                        }
                    }
                })
                .build()
        )
    }

    @Throws(JSONException::class)
    private fun addUserMetadata(params: JSONObject) {
        params.putOpt("id", app.settings.getId())
        params.putOpt("installId", app.settings.getInstallId())
        params.putOpt("userId", app.settings.getUserId())
        params.putOpt("deviceId", RadarUtils.getDeviceId(app))
        params.putOpt("description", app.settings.getDescription())
        params.putOpt("metadata", app.settings.getMetadata())
        if (app.settings.getAdIdEnabled()) {
            params.putOpt("adId", RadarUtils.getAdId(app))
        }
    }

    @Throws(JSONException::class)
    private fun addDeviceMetadata(params: JSONObject) {
        params.putOpt("deviceType", "Android")
        params.putOpt("deviceMake", RadarUtils.deviceMake)
        params.putOpt("sdkVersion", RadarUtils.sdkVersion)
        params.putOpt("deviceModel", RadarUtils.deviceModel)
        params.putOpt("deviceOS", RadarUtils.deviceOS)
        params.putOpt("deviceType", RadarUtils.deviceType)
        params.putOpt("deviceMake", RadarUtils.deviceMake)
        params.putOpt("country", RadarUtils.country)
        params.putOpt("timeZoneOffset", RadarUtils.timeZoneOffset)
    }

    private fun Location.isAccurate(): Boolean {
        return hasAccuracy() && !accuracy.isNaN() && accuracy > 0
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun Location.getVerticalAccuracy(): Float? {
        return if (hasVerticalAccuracy() && !verticalAccuracyMeters.isNaN()) {
            verticalAccuracyMeters
        } else {
            null
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun Location.getSpeedAccuracy(): Float? {
        return if (hasSpeedAccuracy() && !speedAccuracyMetersPerSecond.isNaN()) {
            speedAccuracyMetersPerSecond
        } else {
            null
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun Location.getBearingAccuracy(): Float? {
        return if (hasBearingAccuracy() && !bearingAccuracyDegrees.isNaN()) {
            bearingAccuracyDegrees
        } else {
            null
        }
    }

    @Throws(JSONException::class)
    private fun addLocation(location: Location, params: JSONObject) {
        params.putOpt("latitude", location.latitude)
        params.putOpt("longitude", location.longitude)
        var accuracy = location.accuracy
        if (!location.isAccurate()) {
            accuracy = 1F
        }
        params.putOpt("accuracy", accuracy)
        if (location.hasSpeed() && !location.speed.isNaN()) {
            params.putOpt("speed", location.speed)
        }
        if (location.hasBearing() && !location.bearing.isNaN()) {
            params.putOpt("course", location.bearing)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            params.putOpt("verticalAccuracy", location.getVerticalAccuracy())
            params.putOpt("speedAccuracy", location.getSpeedAccuracy())
            params.putOpt("courseAccuracy", location.getBearingAccuracy())
        }
    }

    @Throws(JSONException::class)
    private fun addTripOptions(params: JSONObject) {
        val tripOptions = app.settings.getTripOptions()
        if (tripOptions != null) {
            val tripOptionsObj = JSONObject()
            tripOptionsObj.putOpt("externalId", tripOptions.externalId)
            tripOptionsObj.putOpt("metadata", tripOptions.metadata)
            tripOptionsObj.putOpt("destinationGeofenceTag", tripOptions.destinationGeofenceTag)
            tripOptionsObj.putOpt(
                "destinationGeofenceExternalId",
                tripOptions.destinationGeofenceExternalId
            )
            tripOptionsObj.putOpt("mode", Radar.stringForMode(tripOptions.mode))
            params.putOpt("tripOptions", tripOptionsObj)
        }
    }

    internal fun log(logs: List<RadarLog>, callback: RadarLogCallback?) {
        val publishableKey = app.settings.getPublishableKey()
        if (publishableKey == null) {
            callback?.onComplete(RadarStatus.ERROR_PUBLISHABLE_KEY)
            return
        }
        val params = JSONObject()
        try {
            params.putOpt("id", app.settings.getId())
            params.putOpt("deviceId", RadarUtils.getDeviceId(app))
            params.putOpt("installId", app.settings.getInstallId())
            params.putOpt("sessionId", app.settings.getSessionId())
            val array = JSONArray()
            logs.forEach { log -> array.put(log.toJson()) }
            params.putOpt("logs", array)
        } catch (e: JSONException) {
            callback?.onComplete(RadarStatus.ERROR_BAD_REQUEST)
            return
        }
        val host = app.settings.getHost()
        val uri = Uri.parse(host).buildUpon()
            .appendEncodedPath("v1/logs")
            .build()
        apiHelper.request(
            RadarApiRequest.Builder("POST", URL(uri.toString()), false)
                .headers(headers(publishableKey))
                .params(params)
                .stream(true)
                // Do not log the saved log events. If the logs themselves were logged it would create a redundancy and
                // eventually lead to a crash when creating a downstream log request, since these will log to memory as
                // a single log entry. Then each time after, this log entry would contain more and more logs, eventually
                // causing an out of memory exception.
                .logPayload(false)
                .callback(object : RadarApiHelper.RadarApiCallback {
                    override fun onComplete(status: RadarStatus, res: JSONObject?) {
                        callback?.onComplete(status, res)
                    }
                })
                .build()
        )
    }

    internal fun track(
        location: Location,
        stopped: Boolean,
        foreground: Boolean,
        source: RadarLocationSource,
        replayed: Boolean,
        nearbyBeacons: Array<String>?,
        callback: RadarTrackApiCallback? = null
    ) {
        val publishableKey = app.settings.getPublishableKey()
        if (publishableKey == null) {
            callback?.onComplete(RadarStatus.ERROR_PUBLISHABLE_KEY)

            return
        }

        val params = JSONObject()
        val options = app.settings.getTrackingOptions()
        try {
            addUserMetadata(params)
            addLocation(location, params)
            if (!foreground && Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                val updatedAtMsDiff = (SystemClock.elapsedRealtimeNanos() - location.elapsedRealtimeNanos) / 1000000
                params.putOpt("updatedAtMsDiff", updatedAtMsDiff)
            }
            params.putOpt("foreground", foreground)
            params.putOpt("stopped", stopped)
            params.putOpt("replayed", replayed)
            addDeviceMetadata(params)
            params.putOpt("source", Radar.stringForSource(source))
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                params.putOpt("mocked", location.isMock)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                params.putOpt("mocked", location.isFromMockProvider)
            }
            addTripOptions(params)
            if (options.syncGeofences) {
                params.putOpt("nearbyGeofences", true)
                params.putOpt("nearbyGeofencesLimit", options.syncGeofencesLimit)
            }
            if (nearbyBeacons != null) {
                val nearbyBeaconsArr = JSONArray()
                for (nearbyBeacon in nearbyBeacons) {
                    nearbyBeaconsArr.put(nearbyBeacon)
                }
                params.putOpt("nearbyBeacons", nearbyBeaconsArr)
            }
            params.putOpt("locationAuthorization", RadarUtils.getLocationAuthorization(app))
            params.putOpt(
                "locationAccuracyAuthorization",
                RadarUtils.getLocationAccuracyAuthorization(app)
            )
            params.putOpt("sessionId", app.settings.getSessionId())
        } catch (e: JSONException) {
            callback?.onComplete(RadarStatus.ERROR_BAD_REQUEST)

            return
        }

        val host = app.settings.getHost()
        val uri = Uri.parse(host).buildUpon()
            .appendEncodedPath("v1/track")
            .build()
        apiHelper.request(
            RadarApiRequest.Builder("POST", URL(uri.toString()), true)
                .headers(headers(publishableKey))
                .params(params)
                .callback(getTrackCallback(location, stopped, options, source, callback))
                .build()
        )
    }

    private fun getTrackCallback(
        location: Location,
        stopped: Boolean,
        options: RadarTrackingOptions,
        source: RadarLocationSource,
        callback: RadarTrackApiCallback?
    ): RadarApiHelper.RadarApiCallback {
        return object : RadarApiHelper.RadarApiCallback {
            override fun onComplete(status: RadarStatus, res: JSONObject?) {
                if (status != RadarStatus.SUCCESS || res == null) {
                    if (options.replay == RadarTrackingOptions.RadarTrackingOptionsReplay.STOPS
                        && stopped
                        && source !in listOf(
                            RadarLocationSource.BACKGROUND_LOCATION, RadarLocationSource.FOREGROUND_LOCATION
                        )
                    ) {
                        app.state.setLastFailedStoppedLocation(location)
                    }
                    app.sendError(status)
                    callback?.onComplete(status)
                    return
                }
                app.state.setLastFailedStoppedLocation(null)

                if (res.has("meta")) {
                    val meta = res.getJSONObject("meta")
                    if (meta.has("config")) {
                        val config = meta.getJSONObject("config")
                        app.settings.setConfig(config)
                    }
                }

                val events = res.optJSONArray("events")?.let { eventsArr ->
                    RadarEvent.fromJson(eventsArr)
                }
                val user = res.optJSONObject("user")?.let { userObj ->
                    RadarUser.fromJson(userObj)
                }
                val nearbyGeofences =
                    res.optJSONArray("nearbyGeofences")?.let { nearbyGeofencesArr ->
                        RadarGeofence.fromJson(nearbyGeofencesArr)
                    }
                if (events != null && user != null) {
                    app.settings.setId(user._id)

                    if (user.trip == null) {
                        app.settings.setTripOptions(null)
                    }

                    app.sendLocation(location, user)

                    if (events.isNotEmpty()) {
                        app.sendEvents(events, user)
                    }

                    app.flushLogs {
                        callback?.onComplete(RadarStatus.SUCCESS, res, events, user, nearbyGeofences)
                    }

                    return
                }

                app.sendError(status)

                callback?.onComplete(RadarStatus.ERROR_SERVER)
            }
        }
    }

    internal fun verifyEvent(
        eventId: String,
        verification: RadarEventVerification,
        verifiedPlaceId: String? = null
    ) {
        val publishableKey = app.settings.getPublishableKey() ?: return

        val params = JSONObject()
        params.putOpt("verification", verification)
        params.putOpt("verifiedPlaceId", verifiedPlaceId)

        val host = app.settings.getHost()
        val uri = Uri.parse(host).buildUpon()
            .appendEncodedPath("v1/events/")
            .appendPath(eventId)
            .appendEncodedPath("/verification")
            .build()
        val url = URL(uri.toString())

        val headers = headers(publishableKey)

        apiHelper.request(RadarApiRequest.Builder("PUT", url).headers(headers).params(params).build())
    }

    internal fun updateTrip(
        options: RadarTripOptions?,
        status: RadarTrip.RadarTripStatus?,
        callback: RadarTripApiCallback?
    ) {
        val publishableKey = app.settings.getPublishableKey()
        if (publishableKey == null) {
            callback?.onComplete(RadarStatus.ERROR_PUBLISHABLE_KEY)

            return
        }

        val externalId = options?.externalId
        if (externalId == null) {
            callback?.onComplete(RadarStatus.ERROR_BAD_REQUEST)

            return
        }

        val params = JSONObject()
        if (status != null && status != RadarTrip.RadarTripStatus.UNKNOWN) {
            params.putOpt("status", Radar.stringForTripStatus(status))
        }
        if (options.metadata != null) {
            params.putOpt("metadata", options.metadata)
        }
        if (options.destinationGeofenceTag != null) {
            params.putOpt("destinationGeofenceTag", options.destinationGeofenceTag)
        }
        if (options.destinationGeofenceExternalId != null) {
            params.putOpt("destinationGeofenceExternalId", options.destinationGeofenceExternalId)
        }
        params.putOpt("mode", Radar.stringForMode(options.mode))

        val host = app.settings.getHost()
        val uri = Uri.parse(host).buildUpon()
            .appendEncodedPath("v1/trips/")
            .appendEncodedPath(externalId)
            .build()
        val url = URL(uri.toString())

        val headers = headers(publishableKey)

        apiHelper.request(
            RadarApiRequest.Builder("PATCH", url)
                .headers(headers)
                .params(params)
                .callback(object : RadarApiHelper.RadarApiCallback {
                    override fun onComplete(status: RadarStatus, res: JSONObject?) {
                        if (status != RadarStatus.SUCCESS || res == null) {
                            callback?.onComplete(status)
                            return
                        }

                        val trip = res.optJSONObject("trip")?.let { tripObj ->
                            RadarTrip.fromJson(tripObj)
                        }
                        val events = res.optJSONArray("events")?.let { eventsArr ->
                            RadarEvent.fromJson(eventsArr)
                        }

                        if (events != null && events.isNotEmpty()) {
                            app.sendEvents(events)
                        }

                        callback?.onComplete(RadarStatus.SUCCESS, res, trip, events)
                    }
                })
                .build()
        )
    }

    internal fun getContext(
        location: Location,
        callback: RadarContextApiCallback
    ) {
        val publishableKey = app.settings.getPublishableKey()
        if (publishableKey == null) {
            callback.onComplete(RadarStatus.ERROR_PUBLISHABLE_KEY)

            return
        }

        val queryParams = StringBuilder()
        queryParams.append("coordinates=${location.latitude},${location.longitude}")

        val host = app.settings.getHost()
        val uri = Uri.parse(host).buildUpon()
            .appendEncodedPath("v1/context?${queryParams}")
        val url = URL(uri.toString())

        val headers = headers(publishableKey)

        apiHelper.request(
            RadarApiRequest.get(url)
                .headers(headers)
                .callback(object : RadarApiHelper.RadarApiCallback {
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
                .build()
        )

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
        val publishableKey = app.settings.getPublishableKey()
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

        val host = app.settings.getHost()
        val uri = Uri.parse(host).buildUpon()
            .appendEncodedPath("v1/search/places?${queryParams}")
            .build()
        val url = URL(uri.toString())

        val headers = headers(publishableKey)

        apiHelper.request(
            RadarApiRequest.get(url)
                .headers(headers)
                .callback(object : RadarApiHelper.RadarApiCallback {
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
                .build()
        )
    }

    internal fun searchGeofences(
        location: Location,
        radius: Int,
        tags: Array<String>?,
        metadata: JSONObject?,
        limit: Int?,
        callback: RadarSearchGeofencesApiCallback
    ) {
        val publishableKey = app.settings.getPublishableKey()
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
        metadata?.keys()?.forEach { key ->
            val value = metadata.get(key)
            queryParams.append("&metadata[${key}]=${value}")
        }

        val host = app.settings.getHost()
        val uri = Uri.parse(host).buildUpon()
            .appendEncodedPath("v1/search/geofences?${queryParams}")
            .build()
        val url = URL(uri.toString())

        val headers = headers(publishableKey)

        apiHelper.request(
            RadarApiRequest.get(url)
                .headers(headers)
                .callback(object : RadarApiHelper.RadarApiCallback {
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
                .build()
        )
    }

    internal fun searchBeacons(
        location: Location,
        radius: Int,
        limit: Int?,
        callback: RadarSearchBeaconsApiCallback
    ) {
        val publishableKey = app.settings.getPublishableKey()
        if (publishableKey == null) {
            callback.onComplete(RadarStatus.ERROR_PUBLISHABLE_KEY)

            return
        }

        val queryParams = StringBuilder()
        queryParams.append("near=${location.latitude},${location.longitude}")
        queryParams.append("&radius=${radius}")
        queryParams.append("&limit=${limit}")

        val host = app.settings.getHost()
        val uri = Uri.parse(host).buildUpon()
            .appendEncodedPath("v1/search/beacons?${queryParams}")
            .build()
        val url = URL(uri.toString())

        val headers = headers(publishableKey)

        apiHelper.request(
            RadarApiRequest.get(url)
                .headers(headers)
                .callback(object : RadarApiHelper.RadarApiCallback {
                    override fun onComplete(status: RadarStatus, res: JSONObject?) {
                        if (status != RadarStatus.SUCCESS || res == null) {
                            callback.onComplete(status)
                            return
                        }

                        val beacons = res.optJSONArray("beacons")?.let { beaconsArr ->
                            RadarBeacon.fromJson(beaconsArr)
                        }
                        if (beacons != null) {
                            callback.onComplete(RadarStatus.SUCCESS, res, beacons)
                            return
                        }
                        callback.onComplete(RadarStatus.ERROR_SERVER)
                    }
                })
                .build()
        )
    }

    internal fun autocomplete(
        query: String,
        near: Location? = null,
        layers: Array<String>? = null,
        limit: Int? = null,
        country: String? = null,
        callback: RadarGeocodeApiCallback
    ) {
        val publishableKey = app.settings.getPublishableKey()
        if (publishableKey == null) {
            callback.onComplete(RadarStatus.ERROR_PUBLISHABLE_KEY)

            return
        }

        val queryParams = StringBuilder()
        queryParams.append("query=${Uri.encode(query)}")
        if (near != null) {
            queryParams.append("&near=${near.latitude},${near.longitude}")
        }
        if (layers?.isNotEmpty() == true) {
            queryParams.append("&layers=${layers.joinToString(separator = ",")}")
        }
        queryParams.append("&limit=${limit}")
        if (country != null) {
            queryParams.append("&country=${country}")
        }

        val host = app.settings.getHost()
        val uri = Uri.parse(host).buildUpon()
            .appendEncodedPath("v1/search/autocomplete?${queryParams}")
            .build()
        val url = URL(uri.toString())

        val headers = headers(publishableKey)

        apiHelper.request(
            RadarApiRequest.get(url)
                .headers(headers)
                .callback(object : RadarApiHelper.RadarApiCallback {
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
                }).build()
        )
    }

    internal fun geocode(
        query: String,
        callback: RadarGeocodeApiCallback
    ) {
        val publishableKey = app.settings.getPublishableKey()
        if (publishableKey == null) {
            callback.onComplete(RadarStatus.ERROR_PUBLISHABLE_KEY)

            return
        }

        val queryParams = StringBuilder()
        queryParams.append("query=${Uri.encode(query)}")

        val host = app.settings.getHost()
        val uri = Uri.parse(host).buildUpon()
            .appendEncodedPath("v1/geocode/forward?${queryParams}")
            .build()
        val url = URL(uri.toString())

        val headers = headers(publishableKey)

        apiHelper.request(
            RadarApiRequest.get(url)
                .headers(headers)
                .callback(object : RadarApiHelper.RadarApiCallback {
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
                .build()
        )
    }

    internal fun reverseGeocode(
        location: Location,
        callback: RadarGeocodeApiCallback
    ) {
        val publishableKey = app.settings.getPublishableKey()
        if (publishableKey == null) {
            callback.onComplete(RadarStatus.ERROR_PUBLISHABLE_KEY)

            return
        }

        val queryParams = StringBuilder()
        queryParams.append("coordinates=${location.latitude},${location.longitude}")

        val host = app.settings.getHost()
        val uri = Uri.parse(host).buildUpon()
            .appendEncodedPath("v1/geocode/reverse?${queryParams}")
            .build()
        val url = URL(uri.toString())

        val headers = headers(publishableKey)

        apiHelper.request(
            RadarApiRequest.get(url)
                .headers(headers)
                .callback(object : RadarApiHelper.RadarApiCallback {
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
                .build()
        )
    }

    internal fun ipGeocode(
        callback: RadarIpGeocodeApiCallback
    ) {
        val publishableKey = app.settings.getPublishableKey()
        if (publishableKey == null) {
            callback.onComplete(RadarStatus.ERROR_PUBLISHABLE_KEY)

            return
        }

        val host = app.settings.getHost()
        val uri = Uri.parse(host).buildUpon()
            .appendEncodedPath("v1/geocode/ip")
            .build()

        val url = URL(uri.toString())

        val headers = headers(publishableKey)

        apiHelper.request(
            RadarApiRequest.get(url)
                .headers(headers)
                .callback(object : RadarApiHelper.RadarApiCallback {
                    override fun onComplete(status: RadarStatus, res: JSONObject?) {
                        if (status != RadarStatus.SUCCESS || res == null) {
                            callback.onComplete(status)
                            return
                        }

                        val address: RadarAddress? = res.optJSONObject("address")?.let { addressObj ->
                            RadarAddress.fromJson(addressObj)
                        }
                        val proxy = res.optBoolean("proxy")

                        if (address != null) {
                            callback.onComplete(RadarStatus.SUCCESS, res, address, proxy)
                            return
                        }

                        callback.onComplete(RadarStatus.ERROR_SERVER)
                    }
                })
                .build()
        )
    }

    internal fun getDistance(
        origin: Location,
        destination: Location,
        modes: EnumSet<Radar.RadarRouteMode>,
        units: Radar.RadarRouteUnits,
        geometryPoints: Int,
        callback: RadarDistanceApiCallback
    ) {
        val publishableKey = app.settings.getPublishableKey()
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
        if (modes.contains(Radar.RadarRouteMode.TRUCK)) {
            modesList.add("truck")
        }
        if (modes.contains(Radar.RadarRouteMode.MOTORBIKE)) {
            modesList.add("motorbike")
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

        val host = app.settings.getHost()
        val uri = Uri.parse(host).buildUpon()
            .appendEncodedPath("v1/route/distance?${queryParams}")
            .build()
        val url = URL(uri.toString())

        val headers = headers(publishableKey)

        apiHelper.request(
            RadarApiRequest.get(url)
                .headers(headers)
                .callback(object : RadarApiHelper.RadarApiCallback {
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
                .build()
        )
    }

    internal fun getMatrix(
        origins: Array<Location>,
        destinations: Array<Location>,
        mode: Radar.RadarRouteMode,
        units: Radar.RadarRouteUnits,
        callback: RadarMatrixApiCallback
    ) {
        val publishableKey = app.settings.getPublishableKey()
        if (publishableKey == null) {
            callback.onComplete(RadarStatus.ERROR_PUBLISHABLE_KEY)

            return
        }

        val queryParams = StringBuilder()
        queryParams.append("origins=")
        for (i in origins.indices) {
            queryParams.append("${origins[i].latitude},${origins[i].longitude}")
            if (i < origins.size - 1) {
                queryParams.append("|")
            }
        }
        queryParams.append("&destinations=")
        for (i in destinations.indices) {
            queryParams.append("${destinations[i].latitude},${destinations[i].longitude}")
            if (i < destinations.size - 1) {
                queryParams.append("|")
            }
        }
        queryParams.append("&mode=${mode.modeString}")
        if (units == Radar.RadarRouteUnits.METRIC) {
            queryParams.append("&units=metric")
        } else {
            queryParams.append("&units=imperial")
        }

        val host = app.settings.getHost()
        val uri = Uri.parse(host).buildUpon()
            .appendEncodedPath("v1/route/matrix?${queryParams}")
            .build()
        val url = URL(uri.toString())

        val headers = headers(publishableKey)

        apiHelper.request(
            RadarApiRequest.get(url)
                .headers(headers)
                .callback(object : RadarApiHelper.RadarApiCallback {
                    override fun onComplete(status: RadarStatus, res: JSONObject?) {
                        if (status != RadarStatus.SUCCESS || res == null) {
                            callback.onComplete(status)
                            return
                        }

                        val matrix = res.optJSONArray("matrix")?.let { matrixObj ->
                            RadarRouteMatrix.fromJson(matrixObj)
                        }
                        if (matrix != null) {
                            callback.onComplete(RadarStatus.SUCCESS, res, matrix)
                            return
                        }

                        callback.onComplete(RadarStatus.ERROR_SERVER)
                    }
                })
                .build()
        )
    }

}
