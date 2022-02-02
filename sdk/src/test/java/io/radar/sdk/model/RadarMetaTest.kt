package io.radar.sdk.model

import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.radar.sdk.RadarTrackingOptions
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.P])
class RadarMetaTest {

    @Test
    fun testFromJson() {
        val meta = JSONObject()
        val trackingOptions = RadarTrackingOptions.RESPONSIVE
        meta.putOpt("trackingOptions", trackingOptions.toJson())
        val json = JSONObject()
        json.putOpt("meta", meta)
        val obj = RadarMeta.fromJson(json)
        assertNotNull(obj.remoteTrackingOptions)
        assertEquals(trackingOptions.desiredStoppedUpdateInterval, obj.remoteTrackingOptions!!.desiredStoppedUpdateInterval)
        assertEquals(trackingOptions.fastestStoppedUpdateInterval, obj.remoteTrackingOptions.fastestStoppedUpdateInterval)
        assertEquals(trackingOptions.desiredMovingUpdateInterval, obj.remoteTrackingOptions.desiredMovingUpdateInterval)
        assertEquals(trackingOptions.fastestMovingUpdateInterval, obj.remoteTrackingOptions.fastestMovingUpdateInterval)
        assertEquals(trackingOptions.desiredSyncInterval, obj.remoteTrackingOptions.desiredSyncInterval)
        assertEquals(trackingOptions.desiredAccuracy, obj.remoteTrackingOptions.desiredAccuracy)
        assertEquals(trackingOptions.stopDuration, obj.remoteTrackingOptions.stopDuration)
        assertEquals(trackingOptions.stopDistance, obj.remoteTrackingOptions.stopDistance)
        assertEquals(trackingOptions.startTrackingAfter, obj.remoteTrackingOptions.startTrackingAfter)
        assertEquals(trackingOptions.stopTrackingAfter, obj.remoteTrackingOptions.stopTrackingAfter)
        assertEquals(trackingOptions.replay, obj.remoteTrackingOptions.replay)
        assertEquals(trackingOptions.sync, obj.remoteTrackingOptions.sync)
        assertEquals(trackingOptions.useStoppedGeofence, obj.remoteTrackingOptions.useStoppedGeofence)
        assertEquals(trackingOptions.stoppedGeofenceRadius, obj.remoteTrackingOptions.stoppedGeofenceRadius)
        assertEquals(trackingOptions.useMovingGeofence, obj.remoteTrackingOptions.useMovingGeofence)
        assertEquals(trackingOptions.movingGeofenceRadius, obj.remoteTrackingOptions.movingGeofenceRadius)
        assertEquals(trackingOptions.syncGeofences, obj.remoteTrackingOptions.syncGeofences)
        assertEquals(trackingOptions.syncGeofencesLimit, obj.remoteTrackingOptions.syncGeofencesLimit)
        assertEquals(trackingOptions.foregroundServiceEnabled, obj.remoteTrackingOptions.foregroundServiceEnabled)
        assertEquals(trackingOptions.beacons, obj.remoteTrackingOptions.beacons)
    }
}