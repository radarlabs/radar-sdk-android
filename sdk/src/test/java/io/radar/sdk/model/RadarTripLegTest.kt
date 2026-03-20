package io.radar.sdk.model

import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.P])
class RadarTripLegTest {

    // region Default State

    @Test
    fun test_defaults() {
        val leg = RadarTripLeg()
        assertNull(leg._id)
        assertNull(leg.destinationGeofenceTag)
        assertNull(leg.destinationGeofenceExternalId)
        assertNull(leg.destinationGeofenceId)
        assertNull(leg.address)
        assertNull(leg.metadata)
        assertNull(leg.createdAt)
        assertNull(leg.updatedAt)
        assertNull(leg.coordinates)
        assertEquals(RadarTripLeg.RadarTripLegStatus.UNKNOWN, leg.status)
        assertEquals(RadarTripLeg.RadarTripLegDestinationType.UNKNOWN, leg.destinationType)
        assertEquals(0.0, leg.etaDuration, 0.0)
        assertEquals(0.0, leg.etaDistance, 0.0)
        assertEquals(0, leg.stopDuration)
        assertEquals(0, leg.arrivalRadius)
    }

    // endregion

    // region Factory Methods

    @Test
    fun test_forGeofence() {
        val leg = RadarTripLeg.forGeofence("store", "store-1")
        assertEquals("store", leg.destinationGeofenceTag)
        assertEquals("store-1", leg.destinationGeofenceExternalId)
        assertNull(leg.destinationGeofenceId)
        assertNull(leg.coordinates)
        assertEquals(RadarTripLeg.RadarTripLegStatus.UNKNOWN, leg.status)
        assertEquals(RadarTripLeg.RadarTripLegDestinationType.GEOFENCE, leg.destinationType)
    }

    @Test
    fun test_forGeofenceId() {
        val leg = RadarTripLeg.forGeofenceId("geofence_abc")
        assertEquals("geofence_abc", leg.destinationGeofenceId)
        assertNull(leg.destinationGeofenceTag)
        assertNull(leg.destinationGeofenceExternalId)
        assertNull(leg.coordinates)
        assertEquals(RadarTripLeg.RadarTripLegDestinationType.GEOFENCE, leg.destinationType)
    }

    @Test
    fun test_forAddress() {
        val leg = RadarTripLeg.forAddress("123 Main St, New York, NY")
        assertEquals("123 Main St, New York, NY", leg.address)
        assertNull(leg.destinationGeofenceTag)
        assertNull(leg.coordinates)
        assertEquals(RadarTripLeg.RadarTripLegDestinationType.ADDRESS, leg.destinationType)
    }

    @Test
    fun test_forCoordinates() {
        val leg = RadarTripLeg.forCoordinates(40.783825, -73.975365)
        assertNotNull(leg.coordinates)
        assertEquals(40.783825, leg.coordinates!!.latitude, 0.0001)
        assertEquals(-73.975365, leg.coordinates!!.longitude, 0.0001)
        assertEquals(0, leg.arrivalRadius)
        assertEquals(RadarTripLeg.RadarTripLegDestinationType.COORDINATES, leg.destinationType)
    }

    // endregion

    // region Status String Conversion

    @Test
    fun test_stringForStatus() {
        assertEquals("unknown", RadarTripLeg.stringForStatus(RadarTripLeg.RadarTripLegStatus.UNKNOWN))
        assertEquals("pending", RadarTripLeg.stringForStatus(RadarTripLeg.RadarTripLegStatus.PENDING))
        assertEquals("started", RadarTripLeg.stringForStatus(RadarTripLeg.RadarTripLegStatus.STARTED))
        assertEquals("approaching", RadarTripLeg.stringForStatus(RadarTripLeg.RadarTripLegStatus.APPROACHING))
        assertEquals("arrived", RadarTripLeg.stringForStatus(RadarTripLeg.RadarTripLegStatus.ARRIVED))
        assertEquals("completed", RadarTripLeg.stringForStatus(RadarTripLeg.RadarTripLegStatus.COMPLETED))
        assertEquals("canceled", RadarTripLeg.stringForStatus(RadarTripLeg.RadarTripLegStatus.CANCELED))
        assertEquals("expired", RadarTripLeg.stringForStatus(RadarTripLeg.RadarTripLegStatus.EXPIRED))
    }

    @Test
    fun test_statusForString() {
        assertEquals(RadarTripLeg.RadarTripLegStatus.PENDING, RadarTripLeg.statusForString("pending"))
        assertEquals(RadarTripLeg.RadarTripLegStatus.STARTED, RadarTripLeg.statusForString("started"))
        assertEquals(RadarTripLeg.RadarTripLegStatus.APPROACHING, RadarTripLeg.statusForString("approaching"))
        assertEquals(RadarTripLeg.RadarTripLegStatus.ARRIVED, RadarTripLeg.statusForString("arrived"))
        assertEquals(RadarTripLeg.RadarTripLegStatus.COMPLETED, RadarTripLeg.statusForString("completed"))
        assertEquals(RadarTripLeg.RadarTripLegStatus.CANCELED, RadarTripLeg.statusForString("canceled"))
        assertEquals(RadarTripLeg.RadarTripLegStatus.EXPIRED, RadarTripLeg.statusForString("expired"))
        assertEquals(RadarTripLeg.RadarTripLegStatus.UNKNOWN, RadarTripLeg.statusForString("unknown"))
        assertEquals(RadarTripLeg.RadarTripLegStatus.UNKNOWN, RadarTripLeg.statusForString("invalid_garbage"))
        assertEquals(RadarTripLeg.RadarTripLegStatus.UNKNOWN, RadarTripLeg.statusForString(""))
    }

    // endregion

    // region Destination Type String Conversion

    @Test
    fun test_stringForDestinationType() {
        assertEquals("unknown", RadarTripLeg.stringForDestinationType(RadarTripLeg.RadarTripLegDestinationType.UNKNOWN))
        assertEquals("geofence", RadarTripLeg.stringForDestinationType(RadarTripLeg.RadarTripLegDestinationType.GEOFENCE))
        assertEquals("address", RadarTripLeg.stringForDestinationType(RadarTripLeg.RadarTripLegDestinationType.ADDRESS))
        assertEquals("coordinates", RadarTripLeg.stringForDestinationType(RadarTripLeg.RadarTripLegDestinationType.COORDINATES))
    }

    @Test
    fun test_destinationTypeForString() {
        assertEquals(RadarTripLeg.RadarTripLegDestinationType.GEOFENCE, RadarTripLeg.destinationTypeForString("geofence"))
        assertEquals(RadarTripLeg.RadarTripLegDestinationType.ADDRESS, RadarTripLeg.destinationTypeForString("address"))
        assertEquals(RadarTripLeg.RadarTripLegDestinationType.COORDINATES, RadarTripLeg.destinationTypeForString("coordinates"))
        assertEquals(RadarTripLeg.RadarTripLegDestinationType.UNKNOWN, RadarTripLeg.destinationTypeForString("unknown"))
        assertEquals(RadarTripLeg.RadarTripLegDestinationType.UNKNOWN, RadarTripLeg.destinationTypeForString("invalid_garbage"))
        assertEquals(RadarTripLeg.RadarTripLegDestinationType.UNKNOWN, RadarTripLeg.destinationTypeForString(""))
    }

    // endregion

    // region fromJson - Request Format

    @Test
    fun test_fromJson_requestFormat_geofenceTagAndExternalId() {
        val json = JSONObject().apply {
            put("destination", JSONObject().apply {
                put("destinationGeofenceTag", "store")
                put("destinationGeofenceExternalId", "store-1")
            })
            put("stopDuration", 10)
            put("metadata", JSONObject().apply { put("package", "small") })
        }
        val leg = RadarTripLeg.fromJson(json)
        assertNotNull(leg)
        assertEquals("store", leg!!.destinationGeofenceTag)
        assertEquals("store-1", leg.destinationGeofenceExternalId)
        assertEquals(10, leg.stopDuration)
        assertEquals("small", leg.metadata?.optString("package"))
        assertEquals(RadarTripLeg.RadarTripLegDestinationType.GEOFENCE, leg.destinationType)
    }

    @Test
    fun test_fromJson_requestFormat_geofenceId() {
        val json = JSONObject().apply {
            put("destination", JSONObject().apply {
                put("destinationGeofenceId", "geofence_abc")
            })
        }
        val leg = RadarTripLeg.fromJson(json)
        assertNotNull(leg)
        assertEquals("geofence_abc", leg!!.destinationGeofenceId)
        assertEquals(RadarTripLeg.RadarTripLegDestinationType.GEOFENCE, leg.destinationType)
    }

    @Test
    fun test_fromJson_requestFormat_coordinates() {
        val json = JSONObject().apply {
            put("destination", JSONObject().apply {
                put("coordinates", JSONArray().apply {
                    put(-73.975365)
                    put(40.783825)
                })
                put("arrivalRadius", 200)
            })
        }
        val leg = RadarTripLeg.fromJson(json)
        assertNotNull(leg)
        assertNotNull(leg!!.coordinates)
        assertEquals(40.783825, leg.coordinates!!.latitude, 0.0001)
        assertEquals(-73.975365, leg.coordinates!!.longitude, 0.0001)
        assertEquals(200, leg.arrivalRadius)
        assertEquals(RadarTripLeg.RadarTripLegDestinationType.COORDINATES, leg.destinationType)
    }

    @Test
    fun test_fromJson_requestFormat_address() {
        val json = JSONObject().apply {
            put("destination", JSONObject().apply {
                put("address", "456 Oak Ave")
            })
        }
        val leg = RadarTripLeg.fromJson(json)
        assertNotNull(leg)
        assertEquals("456 Oak Ave", leg!!.address)
        assertEquals(RadarTripLeg.RadarTripLegDestinationType.ADDRESS, leg.destinationType)
    }

    // endregion

    // region fromJson - Server Response Format

    @Test
    fun test_fromJson_responseFormat_geofence() {
        val json = JSONObject().apply {
            put("_id", "leg_001")
            put("status", "started")
            put("eta", JSONObject().apply {
                put("duration", 5.0)
                put("distance", 2000.0)
            })
            put("destination", JSONObject().apply {
                put("type", "geofence")
                put("source", JSONObject().apply {
                    put("geofence", "geofence_aaa")
                    put("data", JSONObject().apply {
                        put("tag", "store")
                        put("externalId", "store-1")
                    })
                })
                put("location", JSONObject().apply {
                    put("coordinates", JSONArray().apply {
                        put(-73.975365)
                        put(40.783825)
                    })
                })
            })
            put("stopDuration", 10)
            put("metadata", JSONObject().apply { put("package", "small") })
        }
        val leg = RadarTripLeg.fromJson(json)
        assertNotNull(leg)
        assertEquals("leg_001", leg!!._id)
        assertEquals(RadarTripLeg.RadarTripLegStatus.STARTED, leg.status)
        assertEquals(RadarTripLeg.RadarTripLegDestinationType.GEOFENCE, leg.destinationType)
        assertEquals(5.0, leg.etaDuration, 0.0)
        assertEquals(2000.0, leg.etaDistance, 0.0)
        assertEquals("geofence_aaa", leg.destinationGeofenceId)
        assertEquals("store", leg.destinationGeofenceTag)
        assertEquals("store-1", leg.destinationGeofenceExternalId)
        assertNotNull(leg.coordinates)
        assertEquals(40.783825, leg.coordinates!!.latitude, 0.0001)
        assertEquals(-73.975365, leg.coordinates!!.longitude, 0.0001)
        assertEquals(10, leg.stopDuration)
        assertEquals("small", leg.metadata?.optString("package"))
    }

    @Test
    fun test_fromJson_responseFormat_address() {
        val json = JSONObject().apply {
            put("_id", "leg_002")
            put("status", "pending")
            put("destination", JSONObject().apply {
                put("type", "address")
                put("source", JSONObject().apply {
                    put("data", "401 Broadway, New York, NY")
                })
                put("location", JSONObject().apply {
                    put("coordinates", JSONArray().apply {
                        put(-73.9851)
                        put(40.7589)
                    })
                })
                put("arrivalRadius", 25)
            })
        }
        val leg = RadarTripLeg.fromJson(json)
        assertNotNull(leg)
        assertEquals(RadarTripLeg.RadarTripLegDestinationType.ADDRESS, leg!!.destinationType)
        assertEquals("401 Broadway, New York, NY", leg.address)
        assertNotNull(leg.coordinates)
        assertEquals(40.7589, leg.coordinates!!.latitude, 0.0001)
        assertEquals(25, leg.arrivalRadius)
    }

    @Test
    fun test_fromJson_responseFormat_coordinates() {
        val json = JSONObject().apply {
            put("_id", "leg_003")
            put("status", "pending")
            put("destination", JSONObject().apply {
                put("type", "coordinates")
                put("source", JSONObject().apply {
                    put("data", JSONArray().apply {
                        put(40.7484)
                        put(-73.9857)
                    })
                })
                put("location", JSONObject().apply {
                    put("coordinates", JSONArray().apply {
                        put(-73.9857)
                        put(40.7484)
                    })
                })
                put("arrivalRadius", 100)
            })
        }
        val leg = RadarTripLeg.fromJson(json)
        assertNotNull(leg)
        assertEquals(RadarTripLeg.RadarTripLegDestinationType.COORDINATES, leg!!.destinationType)
        assertNotNull(leg.coordinates)
        assertEquals(40.7484, leg.coordinates!!.latitude, 0.0001)
        assertEquals(-73.9857, leg.coordinates!!.longitude, 0.0001)
        assertEquals(100, leg.arrivalRadius)
    }

    // endregion

    // region fromJson - Invalid Input

    @Test
    fun test_fromJson_null() {
        assertNull(RadarTripLeg.fromJson(null as JSONObject?))
    }

    // endregion

    // region toJson

    @Test
    fun test_toJson_geofenceLeg() {
        val leg = RadarTripLeg.forGeofence("store", "store-1")
        leg.stopDuration = 10
        leg.metadata = JSONObject().apply { put("key", "value") }

        val json = leg.toJson()
        val dest = json.optJSONObject("destination")
        assertNotNull(dest)
        assertEquals("store", dest!!.optString("destinationGeofenceTag"))
        assertEquals("store-1", dest.optString("destinationGeofenceExternalId"))
        assertEquals(10, json.optInt("stopDuration"))
        assertEquals("value", json.optJSONObject("metadata")?.optString("key"))
        assertFalse(json.has("_id"))
        assertFalse(json.has("status"))
    }

    @Test
    fun test_toJson_coordinateLeg() {
        val leg = RadarTripLeg.forCoordinates(40.783825, -73.975365)
        leg.arrivalRadius = 200

        val json = leg.toJson()
        val dest = json.optJSONObject("destination")
        assertNotNull(dest)
        val coords = dest!!.optJSONArray("coordinates")
        assertNotNull(coords)
        assertEquals(-73.975365, coords!!.optDouble(0), 0.0001)
        assertEquals(40.783825, coords.optDouble(1), 0.0001)
        assertEquals(200, dest.optInt("arrivalRadius"))
    }

    @Test
    fun test_toJson_includesResponseFields() {
        val responseJson = JSONObject().apply {
            put("_id", "leg_xyz")
            put("status", "arrived")
            put("eta", JSONObject().apply {
                put("duration", 3.5)
                put("distance", 1200.0)
            })
            put("destination", JSONObject().apply {
                put("type", "geofence")
                put("source", JSONObject().apply {
                    put("geofence", "gf_1")
                    put("data", JSONObject().apply {
                        put("tag", "t")
                        put("externalId", "e")
                    })
                })
                put("location", JSONObject().apply {
                    put("coordinates", JSONArray().apply {
                        put(-74.0)
                        put(40.0)
                    })
                })
            })
        }
        val leg = RadarTripLeg.fromJson(responseJson)
        val json = leg!!.toJson()
        assertEquals("leg_xyz", json.optString("_id"))
        assertEquals("arrived", json.optString("status"))
        val eta = json.optJSONObject("eta")
        assertNotNull(eta)
        assertEquals(3.5, eta!!.optDouble("duration"), 0.0)
        assertEquals(1200.0, eta.optDouble("distance"), 0.0)
    }

    // end region

    // region Array Serialization

    @Test
    fun test_fromJson_array() {
        val arr = JSONArray().apply {
            put(JSONObject().apply {
                put("destination", JSONObject().apply {
                    put("destinationGeofenceTag", "a")
                    put("destinationGeofenceExternalId", "1")
                })
            })
            put(JSONObject().apply {
                put("destination", JSONObject().apply {
                    put("destinationGeofenceTag", "b")
                    put("destinationGeofenceExternalId", "2")
                })
            })
        }
        val legs = RadarTripLeg.fromJson(arr)
        assertNotNull(legs)
        assertEquals(2, legs!!.size)
        assertEquals("a", legs[0].destinationGeofenceTag)
        assertEquals("b", legs[1].destinationGeofenceTag)
    }

    @Test
    fun test_fromJson_array_null() {
        assertNull(RadarTripLeg.fromJson(null as JSONArray?))
    }

    @Test
    fun test_toJson_array() {
        val legs = arrayOf(
            RadarTripLeg.forGeofence("a", "1"),
            RadarTripLeg.forAddress("456 St")
        )
        val arr = RadarTripLeg.toJson(legs)
        assertNotNull(arr)
        assertEquals(2, arr!!.length())
    }

    @Test
    fun test_toJson_array_null() {
        assertNull(RadarTripLeg.toJson(null))
        assertNull(RadarTripLeg.toJson(emptyArray()))
    }

    // endregion

    // region Round-Trip Serialization

    @Test
    fun test_roundTrip_geofenceLeg() {
        val original = RadarTripLeg.forGeofence("store", "store-1")
        original.stopDuration = 15
        original.metadata = JSONObject().apply { put("key", "value" )}

        val json = original.toJson()
        val restored = RadarTripLeg.fromJson(json)
        assertEquals(original, restored)
    }

    @Test
    fun test_roundTrip_coordinateLeg() {
        val original = RadarTripLeg.forCoordinates(40.783825, -73.975365)
        original.arrivalRadius = 100
        original.stopDuration = 5

        val json = original.toJson()
        val restored = RadarTripLeg.fromJson(json)
        assertEquals(original, restored)
    }

    // endregion

    // region equals

    @Test
    fun test_equals_sameLeg() {
        val leg1 = RadarTripLeg.forGeofence("store", "store-1")
        leg1.stopDuration = 10
        leg1.metadata = JSONObject().apply { put("k", "v") }

        val leg2 = RadarTripLeg.forGeofence("store", "store-1")
        leg2.stopDuration = 10
        leg2.metadata = JSONObject().apply { put("k", "v") }

        assertEquals(leg1, leg2)
    }

    @Test
    fun test_equals_differentTag() {
        val leg1 = RadarTripLeg.forGeofence("store", "store-1")
        val leg2 = RadarTripLeg.forGeofence("warehouse", "store-1")
        assertNotEquals(leg1, leg2)
    }

    @Test
    fun test_equals_differentDestinationType() {
        val leg1 = RadarTripLeg.forAddress("123 St")
        val leg2 = RadarTripLeg.forCoordinates(40.0, -74.0)
        assertNotEquals(leg1, leg2)
    }

    @Test
    fun test_equals_differentStopDuration() {
        val leg1 = RadarTripLeg.forAddress("123 St")
        leg1.stopDuration = 10
        val leg2 = RadarTripLeg.forAddress("123 St")
        leg2.stopDuration = 20
        assertNotEquals(leg1, leg2)
    }

    @Test
    fun test_equals_null() {
        val leg = RadarTripLeg.forAddress("123 St")
        assertNotEquals(leg, null)
    }

    @Test
    fun test_equals_wrongType() {
        val leg = RadarTripLeg.forAddress("123 St")
        assertNotEquals(leg, "not a leg")
    }

    @Test
    fun test_equals_sameInstance() {
        val leg = RadarTripLeg.forAddress("123 St")
        assertEquals(leg, leg)
    }

    //endregion
}