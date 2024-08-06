package io.radar.sdk

import android.content.Context
import android.location.Location
import android.net.Uri
import android.os.Build
import android.os.SystemClock
import io.radar.sdk.model.RadarEvent.RadarEventVerification
import io.radar.sdk.Radar.RadarLocationSource
import io.radar.sdk.Radar.RadarStatus
import io.radar.sdk.Radar.RadarAddressVerificationStatus
import io.radar.sdk.Radar.locationManager
import io.radar.sdk.model.*
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.util.*

internal class RadarApiClient(
    private val context: Context,
    private var logger: RadarLogger,
    internal var apiHelper: RadarApiHelper = RadarApiHelper(logger)
) {

    interface RadarTrackApiCallback {
        fun onComplete(
            status: RadarStatus,
            res: JSONObject? = null,
            events: Array<RadarEvent>? = null,
            user: RadarUser? = null,
            nearbyGeofences: Array<RadarGeofence>? = null,
            config: RadarConfig? = null,
            token: RadarVerifiedLocationToken? = null
        )
    }

    interface RadarGetConfigApiCallback {
        fun onComplete(status: RadarStatus, config: RadarConfig)
    }

    interface RadarTripApiCallback {
        fun onComplete(status: RadarStatus, res: JSONObject? = null, trip: RadarTrip? = null, events: Array<RadarEvent>? = null)
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

    interface RadarSearchBeaconsApiCallback {
        fun onComplete(status: RadarStatus, res: JSONObject? = null, beacons: Array<RadarBeacon>? = null, uuids: Array<String>? = null, uids: Array<String>? = null)
    }

    interface RadarValidateAddressAPICallback {
        fun onComplete(status: RadarStatus, res: JSONObject? = null, address: RadarAddress? = null, verificationStatus: RadarAddressVerificationStatus? = null)
    }

    interface RadarGeocodeApiCallback {
        fun onComplete(status: RadarStatus, res: JSONObject? = null, addresses: Array<RadarAddress>? = null)
    }

    interface RadarIpGeocodeApiCallback {
        fun onComplete(status: RadarStatus, res: JSONObject? = null, address: RadarAddress? = null, proxy: Boolean = false)
    }

    interface RadarDistanceApiCallback {
        fun onComplete(status: RadarStatus, res: JSONObject? = null, routes: RadarRoutes? = null)
    }

    interface RadarMatrixApiCallback {
        fun onComplete(status: RadarStatus, res: JSONObject? = null, matrix: RadarRouteMatrix? = null)
    }

    interface RadarSendEventApiCallback {
        fun onComplete(
            status: RadarStatus,
            res: JSONObject? = null,
            event: RadarEvent? = null
        )
    }

    internal interface RadarLogCallback {
        fun onComplete(status: RadarStatus, res: JSONObject? = null)
    }

    internal interface RadarReplayApiCallback {
        fun onComplete(status: RadarStatus, res: JSONObject? = null)
    }

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

    internal fun getConfig(usage: String? = null, verified: Boolean = false, callback: RadarGetConfigApiCallback? = null) {
        val publishableKey = RadarSettings.getPublishableKey(context) ?: return

        val path = Uri.Builder().path("v1/config")
        path.appendQueryParameter("installId", RadarSettings.getInstallId(context))
        path.appendQueryParameter("sessionId", RadarSettings.getSessionId(context))
        val id = RadarSettings.getId(context);
        if (id != null) {
            path.appendQueryParameter("id", id)
        }
        path.appendQueryParameter("locationAuthorization",
            RadarUtils.getLocationAuthorization(context)
        )
        path.appendQueryParameter("locationAccuracyAuthorization",
            RadarUtils.getLocationAccuracyAuthorization(context)
        )
        path.appendQueryParameter("verified", verified.toString())
        if (usage != null) {
            path.appendQueryParameter("usage",usage)
        }
        val clientSdkConfiguration = RadarSettings.getClientSdkConfiguration(context).toString()
        path.appendQueryParameter("clientSdkConfiguration", clientSdkConfiguration)

        val headers = headers(publishableKey)

        apiHelper.request(context, "GET", path.toString(), headers, null, false, object : RadarApiHelper.RadarApiCallback {
            override fun onComplete(status: RadarStatus, res: JSONObject?) {
                if (status == RadarStatus.SUCCESS) {
                    Radar.flushLogs()
                }
                callback?.onComplete(status, RadarConfig.fromJson(res))
            }
        }, extendedTimeout = false, stream = true, logPayload = verified)
    }

    internal fun log(logs: List<RadarLog>, callback: RadarLogCallback?) {
        val publishableKey = RadarSettings.getPublishableKey(context)
        if (publishableKey == null) {
            callback?.onComplete(RadarStatus.ERROR_PUBLISHABLE_KEY)
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
            callback?.onComplete(RadarStatus.ERROR_BAD_REQUEST)
            return
        }
        val path = "v1/logs"
        apiHelper.request(
            context = context,
            method = "POST",
            path = path,
            headers = headers(publishableKey),
            params = params,
            sleep = false,
            callback = object : RadarApiHelper.RadarApiCallback {
                override fun onComplete(status: RadarStatus, res: JSONObject?) {
                    callback?.onComplete(status, res)
                }
            },
            extendedTimeout = false,
            stream = true,
            logPayload = false // avoid logging the logging call
        )
    }

    internal fun replay(replays: List<RadarReplay>, callback: RadarReplayApiCallback?) {
        val publishableKey = RadarSettings.getPublishableKey(context)
        if (publishableKey == null) {
            callback?.onComplete(RadarStatus.ERROR_PUBLISHABLE_KEY)

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
            sleep = false,
            callback = object : RadarApiHelper.RadarApiCallback {
                override fun onComplete(status: RadarStatus, res: JSONObject?) {
                     if (status != RadarStatus.SUCCESS) {
                        Radar.sendError(status)
                     }

                    val events = res?.optJSONArray("events")?.let { eventsArr ->
                        RadarEvent.fromJson(eventsArr)
                    }
                    val user = res?.optJSONObject("user")?.let { userObj ->
                        RadarUser.fromJson(userObj)
                    }

                     if (!events.isNullOrEmpty()) {
                        Radar.sendEvents(events, user)
                    }

                    callback?.onComplete(status, res)
                }
            },
            extendedTimeout = true,
            stream = false,
            logPayload = false,
        )
    }

    internal fun track(location: Location, stopped: Boolean, foreground: Boolean, source: RadarLocationSource, replayed: Boolean, beacons: Array<RadarBeacon>?, verified: Boolean = false, integrityToken: String? = null, integrityException: String? = null, encrypted: Boolean? = false, callback: RadarTrackApiCallback? = null) {
        val publishableKey = RadarSettings.getPublishableKey(context)
        if (publishableKey == null) {
            callback?.onComplete(RadarStatus.ERROR_PUBLISHABLE_KEY)

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
            }
            params.putOpt("appId", context.packageName)
        } catch (e: JSONException) {
            callback?.onComplete(RadarStatus.ERROR_BAD_REQUEST)

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
                        callback?.onComplete(status)
                    }
                }
            )
            return
        }

        apiHelper.request(context, "POST", path, headers, requestParams, true, object : RadarApiHelper.RadarApiCallback {
            override fun onComplete(status: RadarStatus, res: JSONObject?) {
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

                    callback?.onComplete(status)

                    return
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
                    val inGeofences = !user.geofences.isNullOrEmpty()
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

                    callback?.onComplete(RadarStatus.SUCCESS, res, events, user, nearbyGeofences, config, token)

                    return
                }

                Radar.sendError(status)

                callback?.onComplete(RadarStatus.ERROR_SERVER)
            }
        }, replaying, false, !replaying, verified)
    }

    internal fun verifyEvent(eventId: String, verification: RadarEventVerification, verifiedPlaceId: String? = null) {
        val publishableKey = RadarSettings.getPublishableKey(context) ?: return

        val params = JSONObject()
        params.putOpt("verification", verification)
        params.putOpt("verifiedPlaceId", verifiedPlaceId)

        val path = "v1/events/$eventId/verification"
        val headers = headers(publishableKey)

        apiHelper.request(context, "PUT", path, headers, params, false)
    }

    internal fun createTrip(options: RadarTripOptions?, callback: RadarTripApiCallback?) {
        val publishableKey = RadarSettings.getPublishableKey(context)
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

        apiHelper.request(context, "POST", path, headers, params, false, object: RadarApiHelper.RadarApiCallback {
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

                if (!events.isNullOrEmpty()) {
                    Radar.sendEvents(events)
                }

                callback?.onComplete(RadarStatus.SUCCESS, res, trip, events)
            }
        })
    }

    internal fun updateTrip(options: RadarTripOptions?, status: RadarTrip.RadarTripStatus?, callback: RadarTripApiCallback?) {
        val publishableKey = RadarSettings.getPublishableKey(context)
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

        apiHelper.request(context, "PATCH", path, headers, params, false, object: RadarApiHelper.RadarApiCallback {
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

                if (!events.isNullOrEmpty()) {
                    Radar.sendEvents(events)
                }

                callback?.onComplete(RadarStatus.SUCCESS, res, trip, events)
            }
        })
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

        val path = Uri.Builder().path("v1/context")
        path.appendQueryParameter("coordinates", "${location.latitude},${location.longitude}")

        val headers = headers(publishableKey)

        apiHelper.request(context, "GET", path.toString(), headers, null, false, object: RadarApiHelper.RadarApiCallback {
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
        chainMetadata: Map<String, String>?,
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

        val path = Uri.Builder().path("v1/search/places")
        path.appendQueryParameter("near","${location.latitude},${location.longitude}")
        path.appendQueryParameter("radius", radius.toString())
        if (limit != null) {
            path.appendQueryParameter("limit", limit.toString())
        }
        if (!chains.isNullOrEmpty()) {
            path.appendQueryParameter("chains", chains.joinToString(separator = ","))
        }
        if (!categories.isNullOrEmpty()) {
            path.appendQueryParameter("categories", categories.joinToString(separator = ","))
        }
        if (!groups.isNullOrEmpty()) {
            path.appendQueryParameter("groups", groups.joinToString(separator = ","))
        }

        chainMetadata?.entries?.forEach {
            path.appendQueryParameter("chainMetadata[${it.key}]", "\"${it.value}\"");
        }

        val headers = headers(publishableKey)

        apiHelper.request(context, "GET", path.toString(), headers, null, false, object : RadarApiHelper.RadarApiCallback {
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
        radius: Int?,
        tags: Array<String>?,
        metadata: JSONObject?,
        limit: Int?,
        includeGeometry: Boolean?,
        callback: RadarSearchGeofencesApiCallback
    ) {
        val publishableKey = RadarSettings.getPublishableKey(context)
        if (publishableKey == null) {
            callback.onComplete(RadarStatus.ERROR_PUBLISHABLE_KEY)

            return
        }

        val path = Uri.Builder().path("v1/search/geofences")

        path.appendQueryParameter("near", "${location.latitude},${location.longitude}")
        if (radius != null) {
            path.appendQueryParameter("radius", radius.toString())
        }
        if (limit != null) {
            path.appendQueryParameter("limit", limit.toString())
        }
        if (!tags.isNullOrEmpty()) {
            path.appendQueryParameter("tags", tags.joinToString(","))
        }
        metadata?.keys()?.forEach { key ->
            val value = metadata.get(key)
            path.appendQueryParameter("metadata[${key}]", value.toString())
        }
        if (includeGeometry != null) {
            path.appendQueryParameter("includeGeometry", includeGeometry.toString())
        }

        val headers = headers(publishableKey)

        apiHelper.request(context, "GET", path.toString(), headers, null, false, object : RadarApiHelper.RadarApiCallback {
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

    internal fun searchBeacons(
        location: Location,
        radius: Int,
        limit: Int?,
        callback: RadarSearchBeaconsApiCallback,
        cache: Boolean
    ) {
        val publishableKey = RadarSettings.getPublishableKey(context)
        if (publishableKey == null) {
            callback.onComplete(RadarStatus.ERROR_PUBLISHABLE_KEY)

            return
        }

        if (cache && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val lastBeacons = RadarState.getLastBeacons(context)
            val lastBeaconUUIDs = RadarState.getLastBeaconUUIDs(context)
            val lastBeaconUIDs = RadarState.getLastBeaconUIDs(context)

            logger.d("Using cached search beacons response | lastBeaconUUIDs = ${lastBeaconUUIDs?.joinToString(",")}; lastBeaconUIDs = ${lastBeaconUIDs?.joinToString(",")}")

            callback.onComplete(RadarStatus.SUCCESS, null, lastBeacons, lastBeaconUUIDs, lastBeaconUIDs)

            return
        }

        val path = Uri.Builder().path("v1/search/beacons")
        path.appendQueryParameter("near", "${location.latitude},${location.longitude}")
        path.appendQueryParameter("radius", radius.toString())
        if (limit != null) {
            path.appendQueryParameter("limit", limit.toString())
        }
        path.appendQueryParameter("installId", RadarSettings.getInstallId(context))

        val headers = headers(publishableKey)

        apiHelper.request(context, "GET", path.toString(), headers, null, false, object : RadarApiHelper.RadarApiCallback {
            override fun onComplete(status: RadarStatus, res: JSONObject?) {
                if (status != RadarStatus.SUCCESS || res == null) {
                    var lastBeacons: Array<RadarBeacon>? = null
                    var lastBeaconUUIDs: Array<String>? = null
                    var lastBeaconUIDs: Array<String>? = null

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        lastBeacons = RadarState.getLastBeacons(context)
                        lastBeaconUUIDs = RadarState.getLastBeaconUUIDs(context)
                        lastBeaconUIDs = RadarState.getLastBeaconUIDs(context)
                    }

                    callback.onComplete(status, res, lastBeacons, lastBeaconUUIDs, lastBeaconUIDs)

                    return
                }

                val beacons = res.optJSONArray("beacons")?.let { beaconsArr ->
                    RadarBeacon.fromJson(beaconsArr)
                }

                val uuids = res.optJSONObject("meta")?.optJSONObject("settings")?.optJSONObject("beacons")?.optJSONArray("uuids")?.let { uuids ->
                    Array(uuids.length()) { index ->
                        uuids.getString(index)
                    }.filter { uuid -> uuid.isNotEmpty() }.toTypedArray()
                }

                val uids = res.optJSONObject("meta")?.optJSONObject("settings")?.optJSONObject("beacons")?.optJSONArray("uids")?.let { uids ->
                    Array(uids.length()) { index ->
                        uids.getString(index)
                    }.filter { uid -> uid.isNotEmpty() }.toTypedArray()
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    RadarState.setLastBeacons(context, beacons)
                    RadarState.setLastBeaconUUIDs(context, uuids)
                    RadarState.setLastBeaconUIDs(context, uids)
                }

                callback.onComplete(RadarStatus.SUCCESS, res, beacons, uuids, uids)
            }
        })
    }

    internal fun autocomplete(
        query: String,
        near: Location? = null,
        layers: Array<String>? = null,
        limit: Int? = null,
        country: String? = null,
        mailable: Boolean? = null,
        callback: RadarGeocodeApiCallback
    ) {
        val publishableKey = RadarSettings.getPublishableKey(context)
        if (publishableKey == null) {
            callback.onComplete(RadarStatus.ERROR_PUBLISHABLE_KEY)

            return
        }

        val path = Uri.Builder().path("v1/search/autocomplete")
        path.appendQueryParameter("query", query)
        if (near != null) {
            path.appendQueryParameter("near", "${near.latitude},${near.longitude}")
        }
        if (layers?.isNotEmpty() == true) {
            path.appendQueryParameter("layers", layers.joinToString(separator = ","))
        }
        if (limit != null) {
            path.appendQueryParameter("limit", limit.toString())
        }
        if (country != null) {
            path.appendQueryParameter("country", country.toString())
        }
        if (mailable != null) {
            path.appendQueryParameter("mailable", mailable.toString())
        }

        val headers = headers(publishableKey)

        apiHelper.request(context, "GET", path.toString(), headers, null, false, object : RadarApiHelper.RadarApiCallback {
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

    internal fun validateAddress(
        address: RadarAddress,
        callback: RadarValidateAddressAPICallback
    ) {
        val publishableKey = RadarSettings.getPublishableKey(context)
        if (publishableKey == null) {
            callback.onComplete(RadarStatus.ERROR_PUBLISHABLE_KEY)

            return
        }

        val path = Uri.Builder().path("v1/addresses/validate")
        path.appendQueryParameter("countryCode", "${address.countryCode}")
        path.appendQueryParameter("stateCode", "${address.stateCode}")
        path.appendQueryParameter("city", "${address.city}")
        path.appendQueryParameter("number", "${address.number}")
        path.appendQueryParameter("street", "${address.street}")
        path.appendQueryParameter("postalCode", "${address.postalCode}")

        if (address.unit != null) {
            path.appendQueryParameter("unit", address.unit)
        }

        val headers = headers(publishableKey)

        apiHelper.request(context, "GET", path.toString(), headers, null, false, object : RadarApiHelper.RadarApiCallback {
            override fun onComplete(status: RadarStatus, res: JSONObject?) {
                if (status != RadarStatus.SUCCESS || res == null) {
                    callback.onComplete(status)

                    return
                }

                val address = res.optJSONObject("address")?.let { address ->
                    RadarAddress.fromJson(address)
                }

                val verificationStatus = when(res.optString("verificationStatus")) {
                    "V" -> RadarAddressVerificationStatus.VERIFIED
                    "P" -> RadarAddressVerificationStatus.PARTIALLY_VERIFIED
                    "A" -> RadarAddressVerificationStatus.AMBIGUOUS
                    "U"-> RadarAddressVerificationStatus.UNVERIFIED
                    else -> RadarAddressVerificationStatus.NONE
                }

                if (address != null) {
                    callback.onComplete(RadarStatus.SUCCESS, res, address, verificationStatus)

                    return
                }

                callback.onComplete(RadarStatus.ERROR_SERVER)
            }
        })
    }

    internal fun geocode(
        query: String,
        layers: Array<String>? = null,
        countries: Array<String>? = null,
        callback: RadarGeocodeApiCallback
    ) {
        val publishableKey = RadarSettings.getPublishableKey(context)
        if (publishableKey == null) {
            callback.onComplete(RadarStatus.ERROR_PUBLISHABLE_KEY)

            return
        }

        val path = Uri.Builder().path("v1/geocode/forward")
        path.appendQueryParameter("query", query)
        if (!layers.isNullOrEmpty()) {
            path.appendQueryParameter("layers", layers.joinToString(separator = ","))
        }
        if (!countries.isNullOrEmpty()) {
            path.appendQueryParameter("country", countries.joinToString(separator = ","))
        }

        val headers = headers(publishableKey)

        apiHelper.request(context, "GET", path.toString(), headers, null, false, object: RadarApiHelper.RadarApiCallback {
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
        layers: Array<String>? = null,
        callback: RadarGeocodeApiCallback
    ) {
        val publishableKey = RadarSettings.getPublishableKey(context)
        if (publishableKey == null) {
            callback.onComplete(RadarStatus.ERROR_PUBLISHABLE_KEY)

            return
        }

        val path = Uri.Builder().path("v1/geocode/reverse")
        path.appendQueryParameter("coordinates", "${location.latitude},${location.longitude}")

        if (!layers.isNullOrEmpty()) {
            path.appendQueryParameter("layers", layers.joinToString(separator = ","))
        }

        val headers = headers(publishableKey)

        apiHelper.request(context, "GET", path.toString(), headers, null, false, object: RadarApiHelper.RadarApiCallback {
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

        val path = "v1/geocode/ip"
        val headers = headers(publishableKey)

        apiHelper.request(context, "GET", path, headers, null, false, object: RadarApiHelper.RadarApiCallback {
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

        val path = Uri.Builder().path("v1/route/distance")
        path.appendQueryParameter("origin", "${origin.latitude},${origin.longitude}")
        path.appendQueryParameter("destination", "${destination.latitude},${destination.longitude}")
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
        path.appendQueryParameter("modes", modesList.joinToString(","))
        if (units == Radar.RadarRouteUnits.METRIC) {
            path.appendQueryParameter("units", "metric")
        } else {
            path.appendQueryParameter("units", "imperial")
        }
        if (geometryPoints > 1) {
            path.appendQueryParameter("geometryPoints", geometryPoints.toString())
        }
        path.appendQueryParameter("geometry", "linestring")

        val headers = headers(publishableKey)

        apiHelper.request(context, "GET", path.toString(), headers, null, false, object : RadarApiHelper.RadarApiCallback {
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

    internal fun getMatrix(
        origins: Array<Location>,
        destinations: Array<Location>,
        mode: Radar.RadarRouteMode,
        units: Radar.RadarRouteUnits,
        callback: RadarMatrixApiCallback
    ) {
        val publishableKey = RadarSettings.getPublishableKey(context)
        if (publishableKey == null) {
            callback.onComplete(RadarStatus.ERROR_PUBLISHABLE_KEY)

            return
        }

        val path = Uri.Builder().path("v1/route/matrix")
        path.appendQueryParameter("origins",
            origins.joinToString("|") { "${it.latitude},${it.longitude}" })
        path.appendQueryParameter("destinations",
            destinations.joinToString("|") { "${it.latitude},${it.longitude}" })
        path.appendQueryParameter("mode", Radar.stringForMode(mode))
        path.appendQueryParameter("units", units.toString().lowercase())

        val headers = headers(publishableKey)

        apiHelper.request(context, "GET", path.toString(), headers, null, false, object : RadarApiHelper.RadarApiCallback {
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
    }

    internal fun sendEvent(
        name: String,
        metadata: JSONObject?,
        callback: RadarSendEventApiCallback
    ) {
        val publishableKey = RadarSettings.getPublishableKey(context)
        if (publishableKey == null) {
            callback.onComplete(RadarStatus.ERROR_PUBLISHABLE_KEY)

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
            callback?.onComplete(RadarStatus.ERROR_BAD_REQUEST)

            return
        }

        val path = "v1/events"
        val headers = headers(publishableKey)

        apiHelper.request(context, "POST", path, headers, params, false, object : RadarApiHelper.RadarApiCallback {
            override fun onComplete(status: RadarStatus, res: JSONObject?) {
                if (status != RadarStatus.SUCCESS || res == null) {
                    callback.onComplete(status)

                    return
                }

                val conversionEvent = res.optJSONObject("event")?.let { eventObj ->
                    RadarEvent.fromJson(eventObj)
                }

                if (conversionEvent == null) {
                    callback.onComplete(RadarStatus.ERROR_SERVER)

                    return
                }

                callback.onComplete(RadarStatus.SUCCESS, res, conversionEvent)
            }
        })
    }

}
