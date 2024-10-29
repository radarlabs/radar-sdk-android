package io.radar.sdk

import android.content.Context
import android.location.Location
import io.radar.sdk.model.RadarRemoteTrackingOptions
import io.radar.sdk.model.RadarCircleGeometry
import io.radar.sdk.model.RadarConfig
import io.radar.sdk.model.RadarCoordinate
import io.radar.sdk.model.RadarPolygonGeometry
import org.json.JSONObject

class RadarOfflineManager {
    interface RadarOfflineCallback {
        fun onComplete(config: RadarConfig?)
    }
    internal fun contextualizeLocation(context: Context, location: Location, callback: RadarOfflineCallback) {
        var newGeofenceIds = mutableSetOf<String>()
        var newGeofenceTags = mutableSetOf<String>()
        val nearbyGeofences = RadarState.getNearbyGeofences(context)
        if (nearbyGeofences == null) {
            Radar.logger.d("skipping as no synced nearby geofence")
            callback.onComplete(null)
            return
        }
        for (geofence in nearbyGeofences) {
            var center: RadarCoordinate? = null
            var radius = 100.0
            if (geofence.geometry is RadarCircleGeometry) {
                center = geofence.geometry.center
                radius = geofence.geometry.radius
            } else if (geofence.geometry is RadarPolygonGeometry) {
                center = geofence.geometry.center
                radius = geofence.geometry.radius
            } else {
                Radar.logger.e("Unsupported geofence geometry type")
                continue
            }
            if (isPointInsideCircle(center, radius, location)) {
                newGeofenceIds.add(geofence._id)
                if (geofence.tag != null) {
                    newGeofenceTags.add(geofence.tag)
                }
            }
        }
        RadarState.setGeofenceIds(context,newGeofenceIds)
        val sdkConfiguration = RadarSettings.getSdkConfiguration(context)
        val rampUpGeofenceTags = RadarRemoteTrackingOptions.getGeofenceTagsWithKey(sdkConfiguration.remoteTrackingOptions, "inGeofence")
        var isRampedUp = false
        if (!rampUpGeofenceTags.isNullOrEmpty()) {
            for (tag in rampUpGeofenceTags) {
                if (newGeofenceTags.contains(tag)) {
                    isRampedUp = true
                    break
                }
            }
        }
        var newTrackingOptions: RadarTrackingOptions? = null
        if (isRampedUp) {
            // ramp up
            Radar.logger.d("Ramp up geofences with trackingOptions: $sdkConfiguration.inGeofenceTrackingOptions")
            newTrackingOptions = RadarRemoteTrackingOptions.getRemoteTrackingOptionsWithKey(sdkConfiguration.remoteTrackingOptions, "inGeofence")
        } else {
            val tripOptions = RadarSettings.getTripOptions(context)
            if (tripOptions != null && RadarRemoteTrackingOptions.getRemoteTrackingOptionsWithKey(sdkConfiguration.remoteTrackingOptions, "onTrip") != null){
                Radar.logger.d("Ramp down geofences with trackingOptions: $sdkConfiguration.6onTripTrackingOptions")
                newTrackingOptions = RadarRemoteTrackingOptions.getRemoteTrackingOptionsWithKey(sdkConfiguration.remoteTrackingOptions, "onTrip")
            } else {
                Radar.logger.d("Ramp down geofences with trackingOptions: $sdkConfiguration.defaultTrackingOptions")
                newTrackingOptions = RadarRemoteTrackingOptions.getRemoteTrackingOptionsWithKey(sdkConfiguration.remoteTrackingOptions, "default")
            }
        }
        if (newTrackingOptions != null) {
            val metaDict = JSONObject()
            metaDict.put("trackingOptions", newTrackingOptions.toJson())
            val configDict = JSONObject()
            configDict.put("meta", metaDict)
            callback.onComplete(RadarConfig.fromJson(configDict))
            return 
        }
        callback.onComplete(null)
        return
    }

    private fun isPointInsideCircle(center: RadarCoordinate, radius: Double, point: Location): Boolean {
        val centerLocation = Location("centerLocation")
        centerLocation.latitude = center.latitude
        centerLocation.longitude = center.longitude
        val distanceBetween = point.distanceTo(centerLocation)
        return distanceBetween <= radius
    }
}
