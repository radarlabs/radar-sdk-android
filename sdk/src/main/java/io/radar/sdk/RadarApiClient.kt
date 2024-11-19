package io.radar.sdk

import android.content.Context
import android.location.Location
import android.os.Build
import android.os.SystemClock
import io.radar.sdk.Radar.RadarAddressVerificationStatus
import io.radar.sdk.Radar.RadarLocationSource
import io.radar.sdk.Radar.RadarStatus
import io.radar.sdk.Radar.locationManager
import io.radar.sdk.model.RadarAddress
import io.radar.sdk.model.RadarBeacon
import io.radar.sdk.model.RadarConfig
import io.radar.sdk.model.RadarContext
import io.radar.sdk.model.RadarEvent
import io.radar.sdk.model.RadarEvent.RadarEventVerification
import io.radar.sdk.model.RadarGeofence
import io.radar.sdk.model.RadarLog
import io.radar.sdk.model.RadarPlace
import io.radar.sdk.model.RadarReplay
import io.radar.sdk.model.RadarRouteMatrix
import io.radar.sdk.model.RadarRoutes
import io.radar.sdk.model.RadarTrip
import io.radar.sdk.model.RadarUser
import io.radar.sdk.model.RadarVerifiedLocationToken
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.net.URLEncoder
import java.util.EnumSet

internal class RadarApiClient(
    private val context: Context,
    private var logger: RadarLogger,
    internal var apiHelper: RadarApiHelper = RadarApiHelper(logger)
) {
    private fun headers(publishableKey: String): Map<String, String> {
        val headers = mutableMapOf(
            "Authorization" to publishableKey,
            "Content-Type" to "application/json",
            "X-Radar-Config" to "true",
            "X-Radar-Device-Make" to RadarUtils.deviceMake,
            "X-Radar-Device-Model" to RadarUtils.deviceModel,
            "X-Radar-Device-OS" to RadarUtils.deviceOS,
            "X-Radar-Device-Type" to RadarUtils.deviceType,
            "X-Radar-SDK-Version" to RadarUtils.sdkVersion,
            "X-Radar-Mobile-Origin" to context.packageName
        )
        if (RadarSettings.isXPlatform(context)) {
            headers["X-Radar-X-Platform-SDK-Type"] = RadarSettings.getXPlatformSDKType(context)
            headers["X-Radar-X-Platform-SDK-Version"] = RadarSettings.getXPlatformSDKVersion(context)
        } else {
            headers["X-Radar-X-Platform-SDK-Type"] = "Native"
        }
        return headers
    }

    internal fun getConfig(usage: String? = null, verified: Boolean = false, callback: ((status: RadarStatus, config: RadarConfig?) -> Unit)? = null) {
        val publishableKey = RadarSettings.getPublishableKey(context)
        if (publishableKey == null) {
            callback?.invoke(RadarStatus.ERROR_PUBLISHABLE_KEY, null)
            return
        }

        val queryParams = StringBuilder()
        queryParams.append("installId=${RadarSettings.getInstallId(context)}")
        queryParams.append("&sessionId=${RadarSettings.getSessionId(context)}")
        val id = RadarSettings.getId(context);
        if (id != null) {
            queryParams.append("&id=${id}")
        }
        queryParams.append("&locationAuthorization=${RadarUtils.getLocationAuthorization(context)}")
        queryParams.append("&locationAccuracyAuthorization=${RadarUtils.getLocationAccuracyAuthorization(context)}")
        queryParams.append("&verified=$verified")
        if (usage != null) {
            queryParams.append("&usage=${usage}")
        }
        val clientSdkConfiguration = RadarSettings.getClientSdkConfiguration(context).toString()
        queryParams.append("&clientSdkConfiguration=${URLEncoder.encode(clientSdkConfiguration, "utf-8")}")

        val path = "v1/config?${queryParams}"
        val headers = headers(publishableKey)

        apiHelper.request(context, "GET", path, headers, stream = true, verified = verified)
        { status: RadarStatus, res: JSONObject? ->
            if (status == RadarStatus.SUCCESS) {
                Radar.flushLogs()
            }
            callback?.invoke(status, RadarConfig.fromJson(res))
        }
    }

    internal fun log(logs: List<RadarLog>, callback: (status: RadarStatus, res: JSONObject?) -> Unit) {
        val publishableKey = RadarSettings.getPublishableKey(context)
        if (publishableKey == null) {
            callback(RadarStatus.ERROR_PUBLISHABLE_KEY, null)
            return
        }
        val params = JSONObject()
        try {
            params.putOpt("id", RadarSettings.getId(context))
            params.putOpt("deviceId", RadarUtils.getDeviceId(context))
            params.putOpt("installId", RadarSettings.getInstallId(context))
            params.putOpt("sessionId", RadarSettings.getSessionId(context))
            val array = JSONArray()
            logs.forEach { log -> array.put(log.toJson()) }
            params.putOpt("logs", array)
        } catch (e: JSONException) {
            callback(RadarStatus.ERROR_BAD_REQUEST, null)
            return
        }
        val path = "v1/logs"
        apiHelper.request(
            context = context,
            method = "POST",
            path = path,
            headers = headers(publishableKey),
            params = params,
            stream = true,
            logPayload = false // avoid logging the logging call
        ) { status: RadarStatus, res: JSONObject? ->
            callback(status, res)
        }
    }

    internal fun replay(replays: List<RadarReplay>, callback: (status: RadarStatus, res: JSONObject?) -> Unit) {
        val publishableKey = RadarSettings.getPublishableKey(context)
        if (publishableKey == null) {
            callback.invoke(RadarStatus.ERROR_PUBLISHABLE_KEY, null)

            return
        }

        val params = JSONObject()
        val replaysList = mutableListOf<JSONObject>()
        for (replay in replays) {
            replaysList.add(replay.replayParams)
        }
        params.putOpt("replays", JSONArray(replaysList))

        val path = "v1/track/replay"
        apiHelper.request(
            context = context,
            method = "POST",
            path = path,
            headers = headers(publishableKey),
            params = params,
            extendedTimeout = true,
            logPayload = false,
        ) { status: RadarStatus, res: JSONObject? ->
            if (status != RadarStatus.SUCCESS) {
                Radar.sendError(status)
            }

            val events = res?.optJSONArray("events")?.let { eventsArr ->
                RadarEvent.fromJson(eventsArr)
            }
            val user = res?.optJSONObject("user")?.let { userObj ->
                RadarUser.fromJson(userObj)
            }

            if (events != null && events.isNotEmpty()) {
                Radar.sendEvents(events, user)
            }

            callback(status, res)
        }
    }

    internal fun track(
        location: Location, stopped: Boolean, foreground: Boolean, source: RadarLocationSource, replayed: Boolean, beacons: Array<RadarBeacon>?, verified: Boolean = false, integrityToken: String? = null, integrityException: String? = null, encrypted: Boolean? = false, expectedCountryCode: String? = null, expectedStateCode: String? = null,
        callback: (
            status: RadarStatus,
            res: JSONObject?,
            events: Array<RadarEvent>?,
            user: RadarUser?,
            nearbyGeofences: Array<RadarGeofence>?,
            config: RadarConfig?,
            token: RadarVerifiedLocationToken?) -> Unit) {
        val publishableKey = RadarSettings.getPublishableKey(context)
        if (publishableKey == null) {
            callback.invoke(RadarStatus.ERROR_PUBLISHABLE_KEY, null, null, null, null, null, null)

            return
        }

        val params = JSONObject()
        val options = Radar.getTrackingOptions()
        val tripOptions = RadarSettings.getTripOptions(context)
        val anonymous = RadarSettings.getAnonymousTrackingEnabled(context)
        try {
            params.putOpt("anonymous", anonymous)
            if (anonymous) {
                params.putOpt("deviceId", "anonymous")
                params.putOpt("geofenceIds", JSONArray(RadarState.getGeofenceIds(context)))
                params.putOpt("placeId", RadarState.getPlaceId(context))
                params.putOpt("regionIds", JSONArray(RadarState.getRegionIds(context)))
                params.putOpt("beaconIds", JSONArray(RadarState.getBeaconIds(context)))
            } else {
                params.putOpt("id", RadarSettings.getId(context))
                params.putOpt("installId", RadarSettings.getInstallId(context))
                params.putOpt("userId", RadarSettings.getUserId(context))
                params.putOpt("deviceId", RadarUtils.getDeviceId(context))
                params.putOpt("description", RadarSettings.getDescription(context))
                params.putOpt("metadata", RadarSettings.getMetadata(context))
                params.putOpt("sessionId", RadarSettings.getSessionId(context))
            }
            params.putOpt("latitude", location.latitude)
            params.putOpt("longitude", location.longitude)
            var accuracy = location.accuracy
            if (!location.hasAccuracy() || location.accuracy.isNaN() || accuracy <= 0) {
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
                if (location.hasVerticalAccuracy() && !location.verticalAccuracyMeters.isNaN()) {
                    params.putOpt("verticalAccuracy", location.verticalAccuracyMeters)
                }
                if (location.hasSpeedAccuracy() && !location.speedAccuracyMetersPerSecond.isNaN()) {
                    params.putOpt("speedAccuracy", location.speedAccuracyMetersPerSecond)
                }
                if (location.hasBearingAccuracy() && !location.bearingAccuracyDegrees.isNaN()) {
                    params.putOpt("courseAccuracy", location.bearingAccuracyDegrees)
                }
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
            params.putOpt("source", Radar.stringForSource(source))
            if (RadarSettings.isXPlatform(context)) {
                params.putOpt("xPlatformType", RadarSettings.getXPlatformSDKType(context))
                params.putOpt("xPlatformSDKVersion", RadarSettings.getXPlatformSDKVersion(context))
            } else {
                params.putOpt("xPlatformType", "Native")
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                val mocked = location.isFromMockProvider
                params.putOpt("mocked", mocked)
            }
            if (tripOptions != null) {
                val tripOptionsObj = JSONObject()
                tripOptionsObj.putOpt("version", "2")
                tripOptionsObj.putOpt("externalId", tripOptions.externalId)
                tripOptionsObj.putOpt("metadata", tripOptions.metadata)
                tripOptionsObj.putOpt("destinationGeofenceTag", tripOptions.destinationGeofenceTag)
                tripOptionsObj.putOpt("destinationGeofenceExternalId", tripOptions.destinationGeofenceExternalId)
                tripOptionsObj.putOpt("mode", Radar.stringForMode(tripOptions.mode))
                params.putOpt("tripOptions", tripOptionsObj)
            }
            if (options.syncGeofences) {
                params.putOpt("nearbyGeofences", true)
                params.putOpt("nearbyGeofencesLimit", options.syncGeofencesLimit)
            }
            if (beacons != null) {
                params.putOpt("beacons", RadarBeacon.toJson(beacons))
            }
            params.putOpt("locationAuthorization", RadarUtils.getLocationAuthorization(context))
            params.putOpt("locationAccuracyAuthorization", RadarUtils.getLocationAccuracyAuthorization(context))
            params.putOpt("trackingOptions", Radar.getTrackingOptions().toJson())
            val usingRemoteTrackingOptions = RadarSettings.getTracking(context) && RadarSettings.getRemoteTrackingOptions(context) != null
            params.putOpt("usingRemoteTrackingOptions", usingRemoteTrackingOptions)
            params.putOpt("locationServicesProvider", RadarSettings.getLocationServicesProvider(context))
            params.putOpt("verified", verified)
            if (verified) {
                params.putOpt("integrityToken", integrityToken)
                params.putOpt("integrityException", integrityException)
                params.putOpt("sharing", RadarUtils.isScreenSharing(context))
                params.putOpt("encrypted", encrypted)
                if (expectedCountryCode != null) {
                    params.putOpt("expectedCountryCode", expectedCountryCode)
                }
                if (expectedStateCode != null) {
                    params.putOpt("expectedStateCode", expectedStateCode)
                }
            }
            params.putOpt("appId", context.packageName)
            if (RadarSettings.getSdkConfiguration(context).useLocationMetadata) {
                val metadata = JSONObject()
                metadata.putOpt("motionActivityData", RadarState.getLastMotionActivity(context))
                if (location.hasSpeed() && !location.speed.isNaN()) {
                    metadata.putOpt("speed",location.speed)
                }
                if (location.hasBearing() && !location.bearing.isNaN()) {
                    metadata.putOpt("bearing", location.bearing)
                }
                params.putOpt("locationMetadata", metadata)
            }
        } catch (e: JSONException) {
            callback.invoke(RadarStatus.ERROR_BAD_REQUEST, null, null, null, null, null, null)

            return
        }

        var path = "v1/track"
        val headers = headers(publishableKey)

        if (anonymous) {
            val usage = "track"
            this.getConfig(usage)
        }

        val hasReplays = Radar.hasReplays()
        var requestParams = params
        val nowMS = System.currentTimeMillis()

        // before we track, check if replays need to sync
        val replaying = options.replay == RadarTrackingOptions.RadarTrackingOptionsReplay.ALL && hasReplays && !verified
        if (replaying) {
            Radar.flushReplays(
                replayParams = params,
                callback = object : Radar.RadarTrackCallback {
                    override fun onComplete(status: RadarStatus, location: Location?, events: Array<RadarEvent>?, user: RadarUser?) {
                        // pass through flush replay onComplete for track callback
                        callback.invoke(status, null, null, null, null, null, null)
                    }
                }
            )
            return
        }

        apiHelper.request(context, "POST", path, headers, requestParams, sleep = true, verified = verified)
        { status: RadarStatus, res: JSONObject? ->
            if (status != RadarStatus.SUCCESS || res == null) {
                if (options.replay == RadarTrackingOptions.RadarTrackingOptionsReplay.ALL) {
                    params.putOpt("replayed", true)
                    params.putOpt("updatedAtMs", nowMS)
                    params.remove("updatedAtMsDiff")
                    Radar.addReplay(params)
                } else if (options.replay == RadarTrackingOptions.RadarTrackingOptionsReplay.STOPS && stopped && !(source == RadarLocationSource.FOREGROUND_LOCATION || source == RadarLocationSource.BACKGROUND_LOCATION)) {
                    RadarState.setLastFailedStoppedLocation(context, location)
                }

                Radar.sendError(status)

                callback.invoke(status, null, null, null, null, null, null)

                return@request
            }

            RadarState.setLastFailedStoppedLocation(context, null)
            Radar.flushLogs()
            RadarSettings.updateLastTrackedTime(context)

            val config = RadarConfig.fromJson(res)

            val events = res.optJSONArray("events")?.let { eventsArr ->
                RadarEvent.fromJson(eventsArr)
            }
            val user = res.optJSONObject("user")?.let { userObj ->
                RadarUser.fromJson(userObj)
            }
            val nearbyGeofences = res.optJSONArray("nearbyGeofences")?.let { nearbyGeofencesArr ->
                RadarGeofence.fromJson(nearbyGeofencesArr)
            }
            val token = RadarVerifiedLocationToken.fromJson(res)

            if (user != null) {
                val inGeofences = user.geofences != null && user.geofences.isNotEmpty()
                val atPlace = user.place != null
                val canExit = inGeofences || atPlace
                RadarState.setCanExit(context, canExit)

                val geofenceIds = mutableSetOf<String>()
                user.geofences?.forEach { geofence -> geofenceIds.add(geofence._id) }
                RadarState.setGeofenceIds(context, geofenceIds)

                val placeId = user.place?._id
                RadarState.setPlaceId(context, placeId)

                val regionIds = mutableSetOf<String>()
                user.country?.let { country -> regionIds.add(country._id) }
                user.state?.let { state -> regionIds.add(state._id) }
                user.dma?.let { dma -> regionIds.add(dma._id) }
                user.postalCode?.let { postalCode -> regionIds.add(postalCode._id) }
                RadarState.setRegionIds(context, regionIds)

                val beaconIds = mutableSetOf<String>()
                user.beacons?.forEach { beacon -> beacon._id?.let { _id -> beaconIds.add(_id) } }
                RadarState.setBeaconIds(context, beaconIds)
            }

            if (events != null && user != null) {
                RadarSettings.setId(context, user._id)

                if (user.trip == null) {
                    // if user was on a trip that ended server-side, restore previous tracking options
                    val tripOptions = RadarSettings.getTripOptions(context)
                    if (tripOptions != null) {
                        locationManager.restartPreviousTrackingOptions()
                        RadarSettings.setTripOptions(context, null)
                    }
                }

                RadarSettings.setUserDebug(context, user.debug)

                Radar.sendLocation(location, user)

                if (events.isNotEmpty()) {
                    Radar.sendEvents(events, user)
                }

                if (token != null) {
                    Radar.sendToken(token)
                }

                callback.invoke(
                    RadarStatus.SUCCESS,
                    res,
                    events,
                    user,
                    nearbyGeofences,
                    config,
                    token
                )

                return@request
            }

            Radar.sendError(status)

            callback.invoke(RadarStatus.ERROR_SERVER, null, null, null, null, null, null)
        }
    }

    internal fun verifyEvent(eventId: String, verification: RadarEventVerification, verifiedPlaceId: String? = null) {
        val publishableKey = RadarSettings.getPublishableKey(context) ?: return

        val params = JSONObject()
        params.putOpt("verification", verification)
        params.putOpt("verifiedPlaceId", verifiedPlaceId)

        val path = "v1/events/$eventId/verification"
        val headers = headers(publishableKey)

        apiHelper.request(context, "PUT", path, headers, params)
    }

    internal fun createTrip(options: RadarTripOptions?, callback: (status: RadarStatus, res: JSONObject?, trip: RadarTrip?, events: Array<RadarEvent>?) -> Unit) {
        val publishableKey = RadarSettings.getPublishableKey(context)
        if (publishableKey == null) {
            callback.invoke(RadarStatus.ERROR_PUBLISHABLE_KEY, null, null, null)

            return
        }

        val externalId = options?.externalId
        if (externalId == null) {
            callback.invoke(RadarStatus.ERROR_BAD_REQUEST, null, null, null)

            return
        }

        val params = JSONObject()
        params.putOpt("userId", RadarSettings.getUserId(context))
        params.putOpt("externalId", externalId)
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
        params.putOpt("scheduledArrivalAt", RadarUtils.dateToISOString(options.scheduledArrivalAt))
        if (options.approachingThreshold > 0) {
            params.put("approachingThreshold", options.approachingThreshold)
        }

        val path = "v1/trips"

        val headers = headers(publishableKey)

        apiHelper.request(context, "POST", path, headers, params)
        { status: RadarStatus, res: JSONObject? ->
            if (status != RadarStatus.SUCCESS || res == null) {
                callback.invoke(status, null, null, null)

                return@request
            }

            val trip = res.optJSONObject("trip")?.let { tripObj ->
                RadarTrip.fromJson(tripObj)
            }
            val events = res.optJSONArray("events")?.let { eventsArr ->
                RadarEvent.fromJson(eventsArr)
            }

            if (events != null && events.isNotEmpty()) {
                Radar.sendEvents(events)
            }

            callback.invoke(RadarStatus.SUCCESS, res, trip, events)
        }
    }

    internal fun updateTrip(options: RadarTripOptions?, status: RadarTrip.RadarTripStatus?, callback: (status: RadarStatus, res: JSONObject?, trip: RadarTrip?, events: Array<RadarEvent>?) -> Unit) {
        val publishableKey = RadarSettings.getPublishableKey(context)
        if (publishableKey == null) {
            callback.invoke(RadarStatus.ERROR_PUBLISHABLE_KEY, null, null, null)

            return
        }

        val externalId = options?.externalId
        if (externalId == null) {
            callback.invoke(RadarStatus.ERROR_BAD_REQUEST, null, null, null)

            return
        }

        val params = JSONObject()
        params.putOpt("userId", RadarSettings.getUserId(context))
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
        params.putOpt("scheduledArrivalAt", RadarUtils.dateToISOString(options.scheduledArrivalAt))
        if (options.approachingThreshold > 0) {
            params.put("approachingThreshold", options.approachingThreshold)
        }

        val path = "v1/trips/$externalId/update"
        val headers = headers(publishableKey)

        apiHelper.request(context, "PATCH", path, headers, params)
        { status: RadarStatus, res: JSONObject? ->
            if (status != RadarStatus.SUCCESS || res == null) {
                callback.invoke(status, null, null, null)

                return@request
            }

            val trip = res.optJSONObject("trip")?.let { tripObj ->
                RadarTrip.fromJson(tripObj)
            }
            val events = res.optJSONArray("events")?.let { eventsArr ->
                RadarEvent.fromJson(eventsArr)
            }

            if (!events.isNullOrEmpty()) {
                Radar.sendEvents(events)
            }

            callback.invoke(RadarStatus.SUCCESS, res, trip, events)
        }
    }

    internal fun getContext(
        location: Location,
        callback: (status: RadarStatus, res: JSONObject?, context: RadarContext?) -> Unit
    ) {
        val publishableKey = RadarSettings.getPublishableKey(context)
        if (publishableKey == null) {
            callback.invoke(RadarStatus.ERROR_PUBLISHABLE_KEY, null, null)

            return
        }

        val queryParams = StringBuilder()
        queryParams.append("coordinates=${location.latitude},${location.longitude}")

        val path = "v1/context?${queryParams}"
        val headers = headers(publishableKey)

        apiHelper.request(context, "GET", path, headers)
        { status: RadarStatus, res: JSONObject? ->
            if (status != RadarStatus.SUCCESS || res == null) {
                callback.invoke(status, null, null)

                return@request
            }

            val context = res.optJSONObject("context")?.let { contextObj ->
                RadarContext.fromJson(contextObj)
            }
            if (context != null) {
                callback.invoke(RadarStatus.SUCCESS, res, context)

                return@request
            }

            callback.invoke(RadarStatus.ERROR_SERVER, null, null)
        }
    }

    internal fun searchPlaces(
        location: Location,
        radius: Int,
        chains: Array<String>?,
        chainMetadata: Map<String, String>?,
        categories: Array<String>?,
        groups: Array<String>?,
        limit: Int?,
        callback: (status: RadarStatus, res: JSONObject?, places: Array<RadarPlace>?) -> Unit
    ) {
        val publishableKey = RadarSettings.getPublishableKey(context)
        if (publishableKey == null) {
            callback.invoke(RadarStatus.ERROR_PUBLISHABLE_KEY, null, null)

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

        chainMetadata?.entries?.forEach {
            queryParams.append("&chainMetadata[${it.key}]=\"${it.value}\"");
        }

        val path = "v1/search/places?${queryParams}"
        val headers = headers(publishableKey)

        apiHelper.request(context, "GET", path, headers)
        { status: RadarStatus, res: JSONObject? ->
            if (status != RadarStatus.SUCCESS || res == null) {
                callback.invoke(status, null, null)

                return@request
            }

            val places = res.optJSONArray("places")?.let { placesArr ->
                RadarPlace.fromJson(placesArr)
            }
            if (places != null) {
                callback.invoke(RadarStatus.SUCCESS, res, places)

                return@request
            }

            callback.invoke(RadarStatus.ERROR_SERVER, null, null)
        }
    }

    internal fun searchGeofences(
        location: Location,
        radius: Int?,
        tags: Array<String>?,
        metadata: JSONObject?,
        limit: Int?,
        includeGeometry: Boolean?,
        callback: (status: RadarStatus, res: JSONObject?, geofences: Array<RadarGeofence>?) -> Unit
    ) {
        val publishableKey = RadarSettings.getPublishableKey(context)
        if (publishableKey == null) {
            callback.invoke(RadarStatus.ERROR_PUBLISHABLE_KEY, null, null)

            return
        }

        val queryParams = StringBuilder()
        queryParams.append("near=${location.latitude},${location.longitude}")
        if (radius != null) {
            queryParams.append("&radius=${radius}")
        }
        queryParams.append("&limit=${limit}")
        if (tags?.isNotEmpty() == true) {
            queryParams.append("&tags=${tags.joinToString(separator = ",")}")
        }
        metadata?.keys()?.forEach { key ->
            val value = metadata.get(key)
            queryParams.append("&metadata[${key}]=${value}")
        }

        if (includeGeometry != null) {
            queryParams.append("&includeGeometry=${includeGeometry}")
        }

        val path = "v1/search/geofences?${queryParams}"
        val headers = headers(publishableKey)

        apiHelper.request(context, "GET", path, headers)
        { status: RadarStatus, res: JSONObject? ->
            if (status != RadarStatus.SUCCESS || res == null) {
                callback.invoke(status, null, null)

                return@request
            }

            val geofences = res.optJSONArray("geofences")?.let { geofencesArr ->
                RadarGeofence.fromJson(geofencesArr)
            }
            if (geofences != null) {
                callback.invoke(RadarStatus.SUCCESS, res, geofences)

                return@request
            }

            callback.invoke(RadarStatus.ERROR_SERVER, null, null)
        }
    }

    internal fun searchBeacons(
        location: Location,
        radius: Int,
        limit: Int?,
        cache: Boolean,
        callback: (status: RadarStatus, res: JSONObject?, beacons: Array<RadarBeacon>?, uuids: Array<String>?, uids: Array<String>?) -> Unit
    ) {
        val publishableKey = RadarSettings.getPublishableKey(context)
        if (publishableKey == null) {
            callback.invoke(RadarStatus.ERROR_PUBLISHABLE_KEY, null, null, null, null)

            return
        }

        if (cache && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val lastBeacons = RadarState.getLastBeacons(context)
            val lastBeaconUUIDs = RadarState.getLastBeaconUUIDs(context)
            val lastBeaconUIDs = RadarState.getLastBeaconUIDs(context)

            logger.d("Using cached search beacons response | lastBeaconUUIDs = ${lastBeaconUUIDs?.joinToString(",")}; lastBeaconUIDs = ${lastBeaconUIDs?.joinToString(",")}")

            callback.invoke(RadarStatus.SUCCESS, null, lastBeacons, lastBeaconUUIDs, lastBeaconUIDs)

            return
        }

        val queryParams = StringBuilder()
        queryParams.append("near=${location.latitude},${location.longitude}")
        queryParams.append("&radius=${radius}")
        queryParams.append("&limit=${limit}")
        queryParams.append("&installId=${RadarSettings.getInstallId(context)}")

        val path = "v1/search/beacons?${queryParams}"
        val headers = headers(publishableKey)

        apiHelper.request(context, "GET", path, headers)
        { status: RadarStatus, res: JSONObject? ->
            if (status != RadarStatus.SUCCESS || res == null) {
                var lastBeacons: Array<RadarBeacon>? = null
                var lastBeaconUUIDs: Array<String>? = null
                var lastBeaconUIDs: Array<String>? = null

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    lastBeacons = RadarState.getLastBeacons(context)
                    lastBeaconUUIDs = RadarState.getLastBeaconUUIDs(context)
                    lastBeaconUIDs = RadarState.getLastBeaconUIDs(context)
                }

                callback.invoke(status, res, lastBeacons, lastBeaconUUIDs, lastBeaconUIDs)

                return@request
            }

            val beacons = res.optJSONArray("beacons")?.let { beaconsArr ->
                RadarBeacon.fromJson(beaconsArr)
            }

            val uuids =
                res.optJSONObject("meta")?.optJSONObject("settings")?.optJSONObject("beacons")
                    ?.optJSONArray("uuids")?.let { uuids ->
                    Array(uuids.length()) { index ->
                        uuids.getString(index)
                    }.filter { uuid -> uuid.isNotEmpty() }.toTypedArray()
                }

            val uids =
                res.optJSONObject("meta")?.optJSONObject("settings")?.optJSONObject("beacons")
                    ?.optJSONArray("uids")?.let { uids ->
                    Array(uids.length()) { index ->
                        uids.getString(index)
                    }.filter { uid -> uid.isNotEmpty() }.toTypedArray()
                }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                RadarState.setLastBeacons(context, beacons)
                RadarState.setLastBeaconUUIDs(context, uuids)
                RadarState.setLastBeaconUIDs(context, uids)
            }

            callback.invoke(RadarStatus.SUCCESS, res, beacons, uuids, uids)
        }
    }

    internal fun autocomplete(
        query: String,
        near: Location? = null,
        layers: Array<String>? = null,
        limit: Int? = null,
        country: String? = null,
        mailable: Boolean? = null,
        callback: (status: RadarStatus, res: JSONObject?, addresses: Array<RadarAddress>?) -> Unit
    ) {
        val publishableKey = RadarSettings.getPublishableKey(context)
        if (publishableKey == null) {
            callback(RadarStatus.ERROR_PUBLISHABLE_KEY, null, null)

            return
        }

        val queryParams = StringBuilder()
        queryParams.append("query=${query}")
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
        if (mailable != null) {
            queryParams.append("&mailable=${mailable}")
        }

        val path = "v1/search/autocomplete?${queryParams}"
        val headers = headers(publishableKey)

        apiHelper.request(context, "GET", path, headers)
        { status: RadarStatus, res: JSONObject? ->
            if (status != RadarStatus.SUCCESS || res == null) {
                callback(status, null, null)

                return@request
            }

            val addresses = res.optJSONArray("addresses")?.let { addressesArr ->
                RadarAddress.fromJson(addressesArr)
            }
            if (addresses != null) {
                callback(RadarStatus.SUCCESS, res, addresses)

                return@request
            }

            callback(RadarStatus.ERROR_SERVER, null, null)
        }
    }

    internal fun validateAddress(
        address: RadarAddress,
        callback: (status: RadarStatus, res: JSONObject?, address: RadarAddress?, verificationStatus: RadarAddressVerificationStatus?) -> Unit
    ) {
        val publishableKey = RadarSettings.getPublishableKey(context)
        if (publishableKey == null) {
            callback.invoke(RadarStatus.ERROR_PUBLISHABLE_KEY, null, null, null)

            return
        }

        val queryParams = StringBuilder()
        queryParams.append("countryCode=${address.countryCode}")
        queryParams.append("&stateCode=${address.stateCode}")
        queryParams.append("&city=${address.city}")
        queryParams.append("&postalCode=${address.postalCode}")
        if (address.number != null) {
            queryParams.append("&number=${address.number}")
        }
        if (address.street != null) {
            queryParams.append("&street=${address.street}")
        }
        if (address.unit != null) {
            queryParams.append("&unit=${address.unit}")
        }
        if (address.addressLabel != null) {
            queryParams.append("&addressLabel=${address.addressLabel}")
        }

        val path = "v1/addresses/validate?${queryParams}"

        val headers = headers(publishableKey)

        apiHelper.request(context, "GET", path, headers)
        { status: RadarStatus, res: JSONObject? ->
            if (status != RadarStatus.SUCCESS || res == null) {
                callback.invoke(status, null, null, null)

                return@request
            }

            val address = res.optJSONObject("address")?.let { address ->
                RadarAddress.fromJson(address)
            }

            val result = res.optJSONObject("result")
            val verificationStatus = when (result?.optString("verificationStatus")) {
                "verified" -> RadarAddressVerificationStatus.VERIFIED
                "partially verified" -> RadarAddressVerificationStatus.PARTIALLY_VERIFIED
                "ambiguous" -> RadarAddressVerificationStatus.AMBIGUOUS
                "unverified" -> RadarAddressVerificationStatus.UNVERIFIED
                else -> RadarAddressVerificationStatus.NONE
            }

            if (address != null) {
                callback.invoke(RadarStatus.SUCCESS, res, address, verificationStatus)

                return@request
            }

            callback.invoke(RadarStatus.ERROR_SERVER, null, null, null)
        }
    }

    internal fun geocode(
        query: String,
        layers: Array<String>? = null,
        countries: Array<String>? = null,
        callback: (status: RadarStatus, res: JSONObject?, addresses: Array<RadarAddress>?) -> Unit
    ) {
        val publishableKey = RadarSettings.getPublishableKey(context)
        if (publishableKey == null) {
            callback(RadarStatus.ERROR_PUBLISHABLE_KEY, null, null)

            return
        }

        val queryParams = StringBuilder()
        queryParams.append("query=${query}")
        if (layers?.isNotEmpty() == true) {
            queryParams.append("&layers=${layers.joinToString(separator = ",")}")
        }
        if (countries?.isNotEmpty() == true) {
            queryParams.append("&country=${countries.joinToString(separator = ",")}")
        }

        val path = "v1/geocode/forward?${queryParams}"
        val headers = headers(publishableKey)

        apiHelper.request(context, "GET", path, headers)
        { status: RadarStatus, res: JSONObject? ->
            if (status != RadarStatus.SUCCESS || res == null) {
                callback(status, null, null)

                return@request
            }

            val addresses = res.optJSONArray("addresses")?.let { addressesArr ->
                RadarAddress.fromJson(addressesArr)
            }
            if (addresses != null) {
                callback(RadarStatus.SUCCESS, res, addresses)

                return@request
            }

            callback(RadarStatus.ERROR_SERVER, null, null)
        }
    }

    internal fun reverseGeocode(
        location: Location,
        layers: Array<String>? = null,
        callback: (status: RadarStatus, res: JSONObject?, addresses: Array<RadarAddress>?) -> Unit
    ) {
        val publishableKey = RadarSettings.getPublishableKey(context)
        if (publishableKey == null) {
            callback(RadarStatus.ERROR_PUBLISHABLE_KEY, null, null)

            return
        }

        val queryParams = StringBuilder()
        queryParams.append("coordinates=${location.latitude},${location.longitude}")

        if (layers?.isNotEmpty() == true) {
            queryParams.append("&layers=${layers.joinToString(separator = ",")}")
        }

        val path = "v1/geocode/reverse?${queryParams}"
        val headers = headers(publishableKey)

        apiHelper.request(context, "GET", path, headers)
        { status: RadarStatus, res: JSONObject? ->
            if (status != RadarStatus.SUCCESS || res == null) {
                callback(status, null, null)

                return@request
            }

            val addresses = res.optJSONArray("addresses")?.let { addressesArr ->
                RadarAddress.fromJson(addressesArr)
            }
            if (addresses != null) {
                callback(RadarStatus.SUCCESS, res, addresses)

                return@request
            }

            callback(RadarStatus.ERROR_SERVER, null, null)
        }
    }

    internal fun ipGeocode(
        callback: (status: RadarStatus, res: JSONObject?, address: RadarAddress?, proxy: Boolean) -> Unit
    ) {
        val publishableKey = RadarSettings.getPublishableKey(context)
        if (publishableKey == null) {
            callback(RadarStatus.ERROR_PUBLISHABLE_KEY, null, null, false)

            return
        }

        val path = "v1/geocode/ip"
        val headers = headers(publishableKey)

        apiHelper.request(context, "GET", path, headers)
        { status: RadarStatus, res: JSONObject? ->
            if (status != RadarStatus.SUCCESS || res == null) {
                callback(status, null, null, false)

                return@request
            }

            val address: RadarAddress? = res.optJSONObject("address")?.let { addressObj ->
                RadarAddress.fromJson(addressObj)
            }
            val proxy = res.optBoolean("proxy")

            if (address != null) {
                callback(RadarStatus.SUCCESS, res, address, proxy)

                return@request
            }

            callback(RadarStatus.ERROR_SERVER, null, null, false)
        }
    }

    internal fun getDistance(
        origin: Location,
        destination: Location,
        modes: EnumSet<Radar.RadarRouteMode>,
        units: Radar.RadarRouteUnits,
        geometryPoints: Int,
        callback: (status: RadarStatus, res: JSONObject?, routes: RadarRoutes?) -> Unit
    ) {
        val publishableKey = RadarSettings.getPublishableKey(context)
        if (publishableKey == null) {
            callback(RadarStatus.ERROR_PUBLISHABLE_KEY, null, null)

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

        val path = "v1/route/distance?${queryParams}"
        val headers = headers(publishableKey)

        apiHelper.request(context, "GET", path, headers)
        { status: RadarStatus, res: JSONObject? ->
            if (status != RadarStatus.SUCCESS || res == null) {
                callback(status, null, null)

                return@request
            }

            val routes = res.optJSONObject("routes")?.let { routesObj ->
                RadarRoutes.fromJson(routesObj)
            }
            if (routes != null) {
                callback(RadarStatus.SUCCESS, res, routes)

                return@request
            }

            callback(RadarStatus.ERROR_SERVER, null, null)
        }
    }

    internal fun getMatrix(
        origins: Array<Location>,
        destinations: Array<Location>,
        mode: Radar.RadarRouteMode,
        units: Radar.RadarRouteUnits,
        callback: (status: RadarStatus, res: JSONObject?, matrix: RadarRouteMatrix?) -> Unit
    ) {
        val publishableKey = RadarSettings.getPublishableKey(context)
        if (publishableKey == null) {
            callback(RadarStatus.ERROR_PUBLISHABLE_KEY, null, null)

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
        if (mode == Radar.RadarRouteMode.FOOT) {
            queryParams.append("&mode=foot")
        } else if (mode == Radar.RadarRouteMode.BIKE) {
            queryParams.append("&mode=bike")
        } else if (mode == Radar.RadarRouteMode.CAR) {
            queryParams.append("&mode=car")
        } else if (mode == Radar.RadarRouteMode.TRUCK) {
            queryParams.append("&mode=truck")
        } else if (mode == Radar.RadarRouteMode.MOTORBIKE) {
            queryParams.append("&mode=motorbike")
        }
        if (units == Radar.RadarRouteUnits.METRIC) {
            queryParams.append("&units=metric")
        } else {
            queryParams.append("&units=imperial")
        }

        val path = "v1/route/matrix?${queryParams}"
        val headers = headers(publishableKey)

        apiHelper.request(context, "GET", path, headers)
        { status: RadarStatus, res: JSONObject? ->
            if (status != RadarStatus.SUCCESS || res == null) {
                callback(status, null, null)

                return@request
            }

            val matrix = res.optJSONArray("matrix")?.let { matrixObj ->
                RadarRouteMatrix.fromJson(matrixObj)
            }
            if (matrix != null) {
                callback(RadarStatus.SUCCESS, res, matrix)

                return@request
            }

            callback(RadarStatus.ERROR_SERVER, null, null)
        }
    }

    internal fun sendEvent(
        name: String,
        metadata: JSONObject?,
        callback: (status: RadarStatus, res: JSONObject?, event: RadarEvent?) -> Unit
    ) {
        val publishableKey = RadarSettings.getPublishableKey(context)
        if (publishableKey == null) {
            callback(RadarStatus.ERROR_PUBLISHABLE_KEY, null, null)

            return
        }

        val params = JSONObject()
        try {
            params.putOpt("id", RadarSettings.getId(context))
            params.putOpt("installId", RadarSettings.getInstallId(context))
            params.putOpt("userId", RadarSettings.getUserId(context))
            params.putOpt("deviceId", RadarUtils.getDeviceId(context))
            params.putOpt("type", name)
            params.putOpt("metadata", metadata)
        } catch (e: JSONException) {
            callback(RadarStatus.ERROR_BAD_REQUEST, null, null)

            return
        }

        val path = "v1/events"
        val headers = headers(publishableKey)

        apiHelper.request(context, "POST", path, headers, params)
        { status: RadarStatus, res: JSONObject? ->
            if (status != RadarStatus.SUCCESS || res == null) {
                callback(status, null, null)

                return@request
            }

            val conversionEvent = res.optJSONObject("event")?.let { eventObj ->
                RadarEvent.fromJson(eventObj)
            }

            if (conversionEvent == null) {
                callback(RadarStatus.ERROR_SERVER, null, null)

                return@request
            }

            callback(RadarStatus.SUCCESS, res, conversionEvent)
        }
    }

}
