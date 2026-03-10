package io.radar.sdk.model

import io.radar.sdk.RadarUtils
import org.json.JSONArray
import org.json.JSONObject
import java.util.Date

/**
 *  Represents a leg of a multi-destination trip.
 *
 */

class RadarTripLeg (
    val _id: String? = null,
    val status: RadarTripLegStatus = RadarTripLegStatus.UNKNOWN,
    val destinationType: RadarTripLegDestinationType = RadarTripLegDestinationType.UNKNOWN,
    val createdAt: Date? = null,
    val updatedAt: Date? = null,
    val etaDuration: Double = 0.0,
    val etaDistance: Double = 0.0,
    var destinationGeofenceTag: String? = null,
    var destinationGeofenceExternalId: String? = null,
    var destinationGeofenceId: String? = null,
    var address: String? = null,
    var coordinates: RadarCoordinate? = null,
    var arrivalRadius: Int = 0,
    var stopDuration: Int = 0,
    var metadata: JSONObject? = null
) {
    enum class RadarTripLegStatus {
        UNKNOWN,
        PENDING,
        STARTED,
        APPROACHING,
        ARRIVED,
        COMPLETED,
        CANCELED,
        EXPIRED
    }

    enum class RadarTripLegDestinationType {
        UNKNOWN,
        GEOFENCE,
        ADDRESS,
        COORDINATES
    }

    companion object {
        private const val FIELD_ID = "_id"
        private const val FIELD_STATUS = "status"
        private const val FIELD_CREATED_AT = "createdAt"
        private const val FIELD_UPDATED_AT = "updatedAt"
        private const val FIELD_DESTINATION = "destination"
        private const val FIELD_TYPE = "type"
        private const val FIELD_SOURCE = "source"
        private const val FIELD_DATA = "data"
        private const val FIELD_GEOFENCE = "geofence"
        private const val FIELD_TAG = "tag"
        private const val FIELD_EXTERNAL_ID = "externalId"
        private const val FIELD_DESTINATION_GEOFENCE_TAG = "destinationGeofenceTag"
        private const val FIELD_DESTINATION_GEOFENCE_EXTERNAL_ID = "destinationGeofenceExternalId"
        private const val FIELD_DESTINATION_GEOFENCE_ID = "destinationGeofenceId"
        private const val FIELD_ADDRESS = "address"
        private const val FIELD_COORDINATES = "coordinates"
        private const val FIELD_LOCATION = "location"
        private const val FIELD_ARRIVAL_RADIUS = "arrivalRadius"
        private const val FIELD_STOP_DURATION = "stopDuration"
        private const val FIELD_METADATA = "metadata"
        private const val FIELD_ETA = "eta"
        private const val FIELD_DURATION = "duration"
        private const val FIELD_DISTANCE = "distance"


        @JvmStatic
        fun stringForStatus(status: RadarTripLegStatus): String = when (status) {
            RadarTripLegStatus.PENDING -> "pending"
            RadarTripLegStatus.STARTED -> "started"
            RadarTripLegStatus.APPROACHING -> "approaching"
            RadarTripLegStatus.ARRIVED -> "arrived"
            RadarTripLegStatus.COMPLETED -> "completed"
            RadarTripLegStatus.CANCELED -> "canceled"
            RadarTripLegStatus.EXPIRED -> "expired"
            else -> "unknown"
        }

        @JvmStatic
        fun statusForString(string: String): RadarTripLegStatus = when (string) {
            "pending" -> RadarTripLegStatus.PENDING
            "started" -> RadarTripLegStatus.STARTED
            "approaching" -> RadarTripLegStatus.APPROACHING
            "arrived" -> RadarTripLegStatus.ARRIVED
            "completed" -> RadarTripLegStatus.COMPLETED
            "canceled" -> RadarTripLegStatus.CANCELED
            "expired" -> RadarTripLegStatus.EXPIRED
            else -> RadarTripLegStatus.UNKNOWN
        }

        @JvmStatic
        fun stringForDestinationType(type: RadarTripLegDestinationType): String = when (type) {
            RadarTripLegDestinationType.GEOFENCE ->  "geofence"
            RadarTripLegDestinationType.ADDRESS -> "address"
            RadarTripLegDestinationType.COORDINATES -> "coordinates"
            else -> "unknown"
        }

        @JvmStatic
        fun destinationTypeForString(string: String): RadarTripLegDestinationType = when (string) {
            "geofence" -> RadarTripLegDestinationType.GEOFENCE
            "address" -> RadarTripLegDestinationType.ADDRESS
            "coordinates" -> RadarTripLegDestinationType.COORDINATES
            else -> RadarTripLegDestinationType.UNKNOWN
        }

        @JvmStatic
        fun forGeofence(tag: String?, externalId: String?): RadarTripLeg = RadarTripLeg(
            destinationType = RadarTripLegDestinationType.GEOFENCE,
            destinationGeofenceTag = tag,
            destinationGeofenceExternalId =  externalId
        )

        @JvmStatic
        fun forGeofenceId(geofenceId: String): RadarTripLeg = RadarTripLeg(
            destinationType = RadarTripLegDestinationType.GEOFENCE,
            destinationGeofenceId = geofenceId
        )

        @JvmStatic
        fun forAddress(address: String): RadarTripLeg = RadarTripLeg(
            destinationType = RadarTripLegDestinationType.ADDRESS,
            address = address
        )

        @JvmStatic
        fun forCoordinates(latitude: Double, longitude: Double): RadarTripLeg = RadarTripLeg(
            destinationType = RadarTripLegDestinationType.COORDINATES,
            coordinates = RadarCoordinate(latitude, longitude)
        )

        @JvmStatic
        fun fromJson(obj: JSONObject?): RadarTripLeg? {
            if (obj == null) return null

            val id = obj.optString(FIELD_ID).takeIf { it.isNotEmpty() }
            val status = statusForString(obj.optString(FIELD_STATUS, ""))
            val createdAt = RadarUtils.isoStringToDate(obj.optString(FIELD_CREATED_AT))
            val updatedAt = RadarUtils.isoStringToDate(obj.optString(FIELD_UPDATED_AT))

            val etaObj = obj.optJSONObject(FIELD_ETA)
            val etaDuration = etaObj?.optDouble(FIELD_DURATION, 0.0) ?: 0.0
            val etaDistance = etaObj?.optDouble(FIELD_DISTANCE, 0.0) ?: 0.0

            var destinationType = RadarTripLegDestinationType.UNKNOWN
            var geofenceTag: String? = null
            var geofenceExternalId: String? = null
            var geofenceId: String? = null
            var address: String? = null
            var coordinates: RadarCoordinate? = null
            var arrivalRadius = 0

            val destinationObj = obj.optJSONObject(FIELD_DESTINATION)
            if (destinationObj != null) {
                val typeStr = destinationObj.optString(FIELD_TYPE, "")
                if (typeStr.isNotEmpty()) {
                    destinationType = destinationTypeForString(typeStr)
                }

                val sourceObj = destinationObj.optJSONObject(FIELD_SOURCE)
                if (sourceObj != null) {
                    when (destinationType) {
                        RadarTripLegDestinationType.GEOFENCE -> {
                            geofenceId = sourceObj.optString(FIELD_GEOFENCE).takeIf { it.isNotEmpty() }
                            sourceObj.optJSONObject(FIELD_DATA)?.let { dataObj ->
                                geofenceTag = dataObj.optString(FIELD_TAG).takeIf { it.isNotEmpty() }
                                geofenceExternalId = dataObj.optString(FIELD_EXTERNAL_ID).takeIf { it.isNotEmpty() }
                            }
                        }

                        RadarTripLegDestinationType.ADDRESS -> {
                            address = sourceObj.optString(FIELD_DATA).takeIf { it.isNotEmpty() }
                        }

                        RadarTripLegDestinationType.COORDINATES -> {
                            sourceObj.optJSONArray(FIELD_DATA)?.let { coordArr ->
                                if (coordArr.length() >= 2) {
                                    coordinates = RadarCoordinate(coordArr.optDouble(1), coordArr.optDouble(0))
                                }
                            }
                        }
                        else -> { }
                    }
                } else {
                    geofenceTag = destinationObj.optString(FIELD_DESTINATION_GEOFENCE_TAG).takeIf { it.isNotEmpty() }
                    geofenceExternalId = destinationObj.optString(FIELD_DESTINATION_GEOFENCE_EXTERNAL_ID).takeIf { it.isNotEmpty() }
                    geofenceId = destinationObj.optString(FIELD_DESTINATION_GEOFENCE_ID).takeIf { it.isNotEmpty() }
                    address = destinationObj.optString(FIELD_ADDRESS).takeIf { it.isNotEmpty() }
                    destinationObj.optJSONArray(FIELD_COORDINATES)?.let { coordArr ->
                        if (coordArr.length() >= 2) {
                            coordinates = RadarCoordinate(coordArr.optDouble(1), coordArr.optDouble(0))
                        }
                    }
                }

                destinationObj.optJSONObject(FIELD_LOCATION)?.let { locationObj ->
                    locationObj.optJSONArray(FIELD_COORDINATES)?.let { coordArr ->
                        if (coordArr.length() >= 2) {
                            coordinates = RadarCoordinate(coordArr.optDouble(1), coordArr.optDouble(0))
                        }
                    }
                }

                if (destinationObj.has(FIELD_ARRIVAL_RADIUS)) {
                    arrivalRadius = destinationObj.optInt(FIELD_ARRIVAL_RADIUS)
                }

                if (destinationType == RadarTripLegDestinationType.UNKNOWN) {
                    destinationType = when {
                        (geofenceTag != null && geofenceExternalId != null) || geofenceId != null ->
                            RadarTripLegDestinationType.GEOFENCE
                        address != null -> RadarTripLegDestinationType.ADDRESS
                        coordinates != null -> RadarTripLegDestinationType.COORDINATES
                        else -> RadarTripLegDestinationType.UNKNOWN
                    }
                }
            }

            return RadarTripLeg(
                _id = id,
                status = status,
                destinationType = destinationType,
                createdAt = createdAt,
                updatedAt = updatedAt,
                etaDuration = etaDuration,
                etaDistance = etaDistance,
                destinationGeofenceTag = geofenceTag,
                destinationGeofenceExternalId = geofenceExternalId,
                destinationGeofenceId = geofenceId,
                address = address,
                coordinates = coordinates,
                arrivalRadius = arrivalRadius,
                stopDuration = obj.optInt(FIELD_STOP_DURATION, 0),
                metadata = obj.optJSONObject(FIELD_METADATA)
            )
        }

        @JvmStatic
        fun fromJson(arr: JSONArray?): Array<RadarTripLeg>? {
            if (arr == null) return null
            val legs = Array(arr.length()) { index ->
                fromJson(arr.optJSONObject(index))
            }.filterNotNull().toTypedArray()
            return legs.takeIf { it.isNotEmpty() }
        }

        @JvmStatic
        fun toJson(legs: Array<RadarTripLeg>?): JSONArray? {
            if (legs.isNullOrEmpty()) return null
            val arr = JSONArray()
            legs.forEach { arr.put(it.toJson()) }
            return arr
        }
    }

    fun toJson(): JSONObject {
        val obj = JSONObject()

        _id?.let { obj.putOpt(FIELD_ID, it) }
        if (status != RadarTripLegStatus.UNKNOWN) {
            obj.putOpt(FIELD_STATUS, stringForStatus(status))
        }

        createdAt?.let { obj.putOpt(FIELD_CREATED_AT, RadarUtils.dateToISOString(it)) }
        updatedAt?.let { obj.putOpt(FIELD_UPDATED_AT, RadarUtils.dateToISOString(it)) }

        val destination = JSONObject()
        destinationGeofenceTag?.let { destination.putOpt(FIELD_DESTINATION_GEOFENCE_TAG, it) }
        destinationGeofenceExternalId?.let { destination.putOpt(FIELD_DESTINATION_GEOFENCE_EXTERNAL_ID, it) }
        destinationGeofenceId?.let { destination.putOpt(FIELD_DESTINATION_GEOFENCE_ID, it) }
        address?.let { destination.putOpt(FIELD_ADDRESS, it) }
        coordinates?.let { coord ->
            val coordArr = JSONArray()
            coordArr.put(coord.longitude)
            coordArr.put(coord.latitude)
            destination.putOpt(FIELD_COORDINATES, coordArr)
        }
        if (arrivalRadius > 0) {
            destination.putOpt(FIELD_ARRIVAL_RADIUS, arrivalRadius)
        }

        if (destination.length() > 0) {
            obj.putOpt(FIELD_DESTINATION, destination)
        }

        if (stopDuration > 0) {
            obj.putOpt(FIELD_STOP_DURATION, stopDuration)
        }
        metadata?.let { obj.putOpt(FIELD_METADATA, it) }

        if (etaDuration > 0 || etaDistance > 0) {
            val eta = JSONObject()
            if (etaDuration > 0) eta.putOpt(FIELD_DURATION, etaDuration)
            if (etaDistance > 0) eta.putOpt(FIELD_DISTANCE, etaDistance)
            obj.putOpt(FIELD_ETA, eta)
        }

        return obj
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RadarTripLeg) return false

        if (destinationType != other.destinationType) return false
        if (status != other.status) return false
        if (stopDuration != other.stopDuration) return false
        if (arrivalRadius != other.arrivalRadius) return false
        if (_id != other._id) return false
        if (destinationGeofenceTag != other.destinationGeofenceTag) return false
        if (destinationGeofenceExternalId != other.destinationGeofenceExternalId) return false
        if (destinationGeofenceId != other.destinationGeofenceId) return false
        if (address != other.address) return false
        if (coordinates?.latitude != other.coordinates?.latitude ||
            coordinates?.longitude != other.coordinates?.longitude) return false
        if (metadata?.toString() != other.metadata?.toString()) return  false

        return true
    }

    // Override equals/hashCode to enable value-based comparison
    // for round-trip serialization testing.
    override fun hashCode(): Int {
        var result = _id?.hashCode() ?: 0
        result = 31 * result + status.hashCode()
        result = 31 * result + destinationType.hashCode()
        result = 31 * result + stopDuration
        result = 31 * result + arrivalRadius
        result = 31 * result + (destinationGeofenceTag?.hashCode() ?: 0)
        result = 31 * result + (destinationGeofenceExternalId?.hashCode() ?: 0)
        result = 31 * result + (destinationGeofenceId?.hashCode() ?: 0)
        result = 31 * result + (address?.hashCode() ?: 0)
        result = 31 * result + (coordinates?.latitude?.hashCode() ?: 0)
        result = 31 * result + (coordinates?.longitude?.hashCode() ?: 0)
        result = 31 * result + (metadata?.toString()?.hashCode() ?: 0)
        return result
    }
}