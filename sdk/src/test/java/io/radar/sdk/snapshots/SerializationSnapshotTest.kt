package io.radar.sdk.snapshots

import android.location.Location
import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.radar.sdk.Radar
import io.radar.sdk.model.RadarAddress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Serialization snapshots: parse a fixture with the SDK's normal parser, serialize it with the
 * method the cross-platform bridges use, and compare the canonical JSON to a checked-in snapshot.
 * See [Snapshots] for how to re-record.
 *
 * Pinned to API 34: `Radar.jsonForLocation` writes the accuracy fields only on API 26+ and reads
 * `isMock` on API 31+ (`isFromMockProvider` below), so the Location snapshot depends on the SDK level.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.UPSIDE_DOWN_CAKE])
class SerializationSnapshotTest {

    @Test
    fun geocode_addresses() {
        val addresses = Snapshots.fixture("geocode.json").getJSONArray("addresses")
        assertEquals(1, addresses.length())
        for (i in 0 until addresses.length()) {
            val address = RadarAddress.fromJson(addresses.getJSONObject(i))
            assertNotNull(address)
            Snapshots.assertMatches("geocode/addresses.$i", address!!.toJson())
        }
    }

    @Test
    fun full_address() {
        val address = RadarAddress.fromJson(Snapshots.fixture("full/Address.json"))
        assertNotNull(address)
        assertFullyPopulated(address!!, allowedNull = emptySet())
        Snapshots.assertMatches("full/Address/root", address.toJson())
    }

    @Test
    fun built_location() {
        val location = Location("gps").apply {
            latitude = 40.7039
            longitude = -73.9867
            altitude = 10.5
            accuracy = 5.0f
            verticalAccuracyMeters = 3.0f
            speed = 1.5f
            speedAccuracyMetersPerSecond = 0.5f
            bearing = 90.0f
            bearingAccuracyDegrees = 10.0f
            time = 1737479963000L // 2025-01-21T17:19:23Z; not read by jsonForLocation
            // Set to the non-default value so the snapshot shows it's read. jsonForLocation reads
            // isMock on API 31+ and isFromMockProvider below.
            isMock = true
        }
        Snapshots.assertMatches("built/location", Radar.jsonForLocation(location))
    }
}
