package io.radar.sdk

import android.app.NotificationManager
import android.content.Context
import android.location.Location
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.radar.sdk.model.RadarBeacon
import io.radar.sdk.model.RadarEvent
import io.radar.sdk.model.RadarGeofence
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowNotificationManager

@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.P])
class RadarNotificationHelperTest {

    private lateinit var context: Context
    private lateinit var shadowNotificationManager: ShadowNotificationManager
    private val fmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        shadowNotificationManager = Shadows.shadowOf(notificationManager)
    }

    private fun relativeTimestamp(offsetMs: Long): String = fmt.format(Date(System.currentTimeMillis() + offsetMs))

    private fun makeGeofenceWithMetadata(metadata: JSONObject?): RadarGeofence = RadarGeofence("geo1", "Test Geofence", null, null, metadata, null, null)

    private fun makeBeaconWithMetadata(metadata: JSONObject?): RadarBeacon = RadarBeacon(uuid = "uuid", major = "1", minor = "1", metadata = metadata, type = RadarBeacon.RadarBeaconType.IBEACON)

    private fun makeGeofenceEvent(
        type: RadarEvent.RadarEventType,
        geofence: RadarGeofence
    ): RadarEvent = RadarEvent(
        _id = "event1",
        createdAt = Date(),
        actualCreatedAt = Date(),
        live = false,
        type = type,
        conversionName = null,
        geofence = geofence,
        place = null,
        region = null,
        beacon = null,
        trip = null,
        fraud = null,
        alternatePlaces = null,
        verifiedPlace = null,
        verification = RadarEvent.RadarEventVerification.ACCEPT,
        confidence = RadarEvent.RadarEventConfidence.HIGH,
        duration = 0f,
        location = Location("test"),
        replayed = false,
        metadata = null
    )

    private fun makeBeaconEvent(
        type: RadarEvent.RadarEventType,
        beacon: RadarBeacon
    ): RadarEvent = RadarEvent(
        _id = "event2",
        createdAt = Date(),
        actualCreatedAt = Date(),
        live = false,
        type = type,
        conversionName = null,
        geofence = null,
        place = null,
        region = null,
        beacon = beacon,
        trip = null,
        fraud = null,
        alternatePlaces = null,
        verifiedPlace = null,
        verification = RadarEvent.RadarEventVerification.ACCEPT,
        confidence = RadarEvent.RadarEventConfidence.HIGH,
        duration = 0f,
        location = Location("test"),
        replayed = false,
        metadata = null
    )

    private fun notificationCount(): Int = shadowNotificationManager.allNotifications.size

    // --- No scheduling window ---

    @Test
    fun `geofence entry notification shown when no scheduling window set`() {
        val metadata = JSONObject().apply { put("radar:entryNotificationText", "You entered!") }
        val event = makeGeofenceEvent(RadarEvent.RadarEventType.USER_ENTERED_GEOFENCE, makeGeofenceWithMetadata(metadata))
        RadarNotificationHelper.showNotifications(context, arrayOf(event))
        assertEquals(1, notificationCount())
    }

    // --- Within window ---

    @Test
    fun `geofence entry notification shown when within scheduling window`() {
        val metadata = JSONObject().apply {
            put("radar:entryNotificationText", "You entered!")
            put("radar:startsAt", relativeTimestamp(-60_000)) // 1 min ago
            put("radar:endsAt", relativeTimestamp(60_000)) // 1 min from now
        }
        val event = makeGeofenceEvent(RadarEvent.RadarEventType.USER_ENTERED_GEOFENCE, makeGeofenceWithMetadata(metadata))
        RadarNotificationHelper.showNotifications(context, arrayOf(event))
        assertEquals(1, notificationCount())
    }

    // --- Before window starts ---

    @Test
    fun `geofence entry notification suppressed when before startsAt`() {
        val metadata = JSONObject().apply {
            put("radar:entryNotificationText", "You entered!")
            put("radar:startsAt", relativeTimestamp(60_000)) // 1 min from now
        }
        val event = makeGeofenceEvent(RadarEvent.RadarEventType.USER_ENTERED_GEOFENCE, makeGeofenceWithMetadata(metadata))
        RadarNotificationHelper.showNotifications(context, arrayOf(event))
        assertEquals(0, notificationCount())
    }

    // --- After window ends ---

    @Test
    fun `geofence entry notification suppressed when after endsAt`() {
        val metadata = JSONObject().apply {
            put("radar:entryNotificationText", "You entered!")
            put("radar:endsAt", relativeTimestamp(-60_000)) // 1 min ago
        }
        val event = makeGeofenceEvent(RadarEvent.RadarEventType.USER_ENTERED_GEOFENCE, makeGeofenceWithMetadata(metadata))
        RadarNotificationHelper.showNotifications(context, arrayOf(event))
        assertEquals(0, notificationCount())
    }

    // --- Exit event respects window ---

    @Test
    fun `geofence exit notification suppressed when outside window`() {
        val metadata = JSONObject().apply {
            put("radar:exitNotificationText", "You exited!")
            put("radar:startsAt", relativeTimestamp(60_000)) // future
        }
        val event = makeGeofenceEvent(RadarEvent.RadarEventType.USER_EXITED_GEOFENCE, makeGeofenceWithMetadata(metadata))
        RadarNotificationHelper.showNotifications(context, arrayOf(event))
        assertEquals(0, notificationCount())
    }

    @Test
    fun `geofence exit notification shown when within window`() {
        val metadata = JSONObject().apply {
            put("radar:exitNotificationText", "You exited!")
            put("radar:startsAt", relativeTimestamp(-60_000))
            put("radar:endsAt", relativeTimestamp(60_000))
        }
        val event = makeGeofenceEvent(RadarEvent.RadarEventType.USER_EXITED_GEOFENCE, makeGeofenceWithMetadata(metadata))
        RadarNotificationHelper.showNotifications(context, arrayOf(event))
        assertEquals(1, notificationCount())
    }

    // --- Both set to distant future/past ---

    @Test
    fun `notification suppressed when both startsAt and endsAt are in the future`() {
        val metadata = JSONObject().apply {
            put("radar:entryNotificationText", "You entered!")
            put("radar:startsAt", relativeTimestamp(3_600_000)) // 1 hour from now
            put("radar:endsAt", relativeTimestamp(7_200_000)) // 2 hours from now
        }
        val event = makeGeofenceEvent(RadarEvent.RadarEventType.USER_ENTERED_GEOFENCE, makeGeofenceWithMetadata(metadata))
        RadarNotificationHelper.showNotifications(context, arrayOf(event))
        assertEquals(0, notificationCount())
    }

    @Test
    fun `notification suppressed when both startsAt and endsAt are in the past`() {
        val metadata = JSONObject().apply {
            put("radar:entryNotificationText", "You entered!")
            put("radar:startsAt", relativeTimestamp(-7_200_000)) // 2 hours ago
            put("radar:endsAt", relativeTimestamp(-3_600_000)) // 1 hour ago
        }
        val event = makeGeofenceEvent(RadarEvent.RadarEventType.USER_ENTERED_GEOFENCE, makeGeofenceWithMetadata(metadata))
        RadarNotificationHelper.showNotifications(context, arrayOf(event))
        assertEquals(0, notificationCount())
    }

    // --- Fail-open on bad date ---

    @Test
    fun `notification shown when startsAt is unparseable (fail-open)`() {
        val metadata = JSONObject().apply {
            put("radar:entryNotificationText", "You entered!")
            put("radar:startsAt", "not-a-date")
        }
        val event = makeGeofenceEvent(RadarEvent.RadarEventType.USER_ENTERED_GEOFENCE, makeGeofenceWithMetadata(metadata))
        RadarNotificationHelper.showNotifications(context, arrayOf(event))
        assertEquals(1, notificationCount())
    }

    // --- Beacon event is NOT affected by window check ---

    @Test
    fun `beacon entry notification shown regardless of scheduling window fields`() {
        val metadata = JSONObject().apply {
            put("radar:entryNotificationText", "Beacon nearby!")
            put("radar:startsAt", relativeTimestamp(60_000)) // future — should NOT suppress beacon
        }
        val event = makeBeaconEvent(RadarEvent.RadarEventType.USER_ENTERED_BEACON, makeBeaconWithMetadata(metadata))
        RadarNotificationHelper.showNotifications(context, arrayOf(event))
        assertEquals(1, notificationCount())
    }
}
