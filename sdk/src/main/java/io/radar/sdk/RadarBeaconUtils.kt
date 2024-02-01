package io.radar.sdk

import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanRecord
import android.bluetooth.le.ScanResult
import android.os.Build
import android.os.ParcelUuid
import androidx.annotation.RequiresApi
import io.radar.sdk.model.RadarBeacon
import org.json.JSONObject
import java.nio.ByteBuffer
import java.util.*

internal object RadarBeaconUtils {

    private const val IBEACON_MANUFACTURER_ID = 76
    private val EDDYSTONE_SERVICE_UUID = ParcelUuid.fromString("0000FEAA-0000-1000-8000-00805F9B34FB")
    private val HEX = "0123456789abcdef".toCharArray()

    fun beaconsForScanResults(scanResults: ArrayList<ScanResult>?): Array<RadarBeacon> {
        if (scanResults.isNullOrEmpty()) {
            return arrayOf()
        }

        val beacons = mutableListOf<RadarBeacon>()

        scanResults.forEach { result ->
            result.scanRecord?.let { scanRecord -> getBeacon(result, scanRecord) }?.let { beacon ->
                beacons.add(beacon)
            }
        }

        return beacons.toTypedArray()
    }

    fun stringSetForBeacons(beacons: Array<RadarBeacon>?): Set<String>? {
        if (beacons == null) {
            return null
        }

        val arr = stringArrayForBeacons(beacons)

        return arr.toSet()
    }

    fun stringArrayForBeacons(beacons: Array<RadarBeacon>): Array<String> {
        val arr = mutableListOf<String>()

        beacons.forEach { beacon ->
            arr.add(beacon.toJson().toString())
        }

        return arr.toTypedArray()
    }

    fun beaconsForStringSet(set: Set<String>?): Array<RadarBeacon>? {
        if (set == null) {
            return null
        }

        val arr = set.toTypedArray()

        return beaconsForStringArray(arr)
    }

    fun beaconsForStringArray(arr: Array<String>?): Array<RadarBeacon>? {
        if (arr == null) {
            return null
        }

        val beacons = mutableListOf<RadarBeacon>()

        arr.forEach { str ->
            val beacon = RadarBeacon.fromJson(JSONObject(str))
            if (beacon != null) {
                beacons.add(beacon)
            }
        }

        return beacons.toTypedArray()
    }

    fun getScanFilterForBeacon(beacon: RadarBeacon): ScanFilter? {
        if (beacon.type == RadarBeacon.RadarBeaconType.EDDYSTONE) {
            val uid = beacon.uuid
            val identifier = beacon.major

            val serviceData = ByteBuffer.allocate(18)
                .put(ByteArray(2) { 0x00.toByte() })
                .put(this.toByteArray(uid, 10))
                .put(this.toByteArray(identifier, 6))
                .array()

            val serviceDataMask = ByteBuffer.allocate(18)
                .put(ByteArray(1) { 0xFF.toByte() } )
                .put(ByteArray(1) { 0x00.toByte() } )
                .put(ByteArray(16) { 0xFF.toByte() } )
                .array()

            return ScanFilter.Builder()
                .setServiceUuid(EDDYSTONE_SERVICE_UUID)
                .setServiceData(EDDYSTONE_SERVICE_UUID, serviceData, serviceDataMask)
                .build()
        } else if (beacon.type == RadarBeacon.RadarBeaconType.IBEACON) {
            val uuid = UUID.fromString(beacon.uuid)
            val major = beacon.major.toInt()
            val minor = beacon.minor.toInt()

            val manufacturerData = ByteBuffer.allocate(23)
                .put(ByteArray(2) { 0x00.toByte() })
                .putLong(uuid.mostSignificantBits)
                .putLong(uuid.leastSignificantBits)
                .put((major / 256).toByte())
                .put((major % 256).toByte())
                .put((minor / 256).toByte())
                .put((minor % 256).toByte())
                .put(ByteArray(1) { 0x00.toByte() })
                .array()

            val manufacturerDataMask = ByteBuffer.allocate(23)
                .put(ByteArray(2) { 0x00.toByte() })
                .put(ByteArray(20) { 0xFF.toByte() })
                .put(ByteArray(1) { 0x00.toByte() })
                .array()

            return ScanFilter.Builder()
                .setManufacturerData(IBEACON_MANUFACTURER_ID, manufacturerData, manufacturerDataMask)
                .build()
        }

        return null
    }

    fun getScanFilterForBeacon(beaconUUID: String): ScanFilter? {
        val uuid = UUID.fromString(beaconUUID.lowercase())

        val manufacturerData = ByteBuffer.allocate(23)
            .put(ByteArray(2) { 0x00.toByte() })
            .putLong(uuid.mostSignificantBits)
            .putLong(uuid.leastSignificantBits)
            .put(ByteArray(5) { 0x00.toByte() })
            .array()

        val manufacturerDataMask = ByteBuffer.allocate(23)
            .put(ByteArray(2) { 0x00.toByte() })
            .put(ByteArray(16) { 0xFF.toByte() })
            .put(ByteArray(5) { 0x00.toByte() })
            .array()

        return ScanFilter.Builder()
            .setManufacturerData(IBEACON_MANUFACTURER_ID, manufacturerData, manufacturerDataMask)
            .build()
    }

    fun getScanFilterForBeaconUID(beaconUID: String): ScanFilter? {
        val serviceData = ByteBuffer.allocate(18)
            .put(ByteArray(2) { 0x00.toByte() })
            .put(this.toByteArray(beaconUID, 10))
            .put(ByteArray(6) { 0x00.toByte() })
            .array()

        val serviceDataMask = ByteBuffer.allocate(18)
            .put(ByteArray(1) { 0xFF.toByte() } )
            .put(ByteArray(1) { 0x00.toByte() } )
            .put(ByteArray(10) { 0xFF.toByte() } )
            .put(ByteArray(6) { 0x00.toByte() } )
            .array()

        return ScanFilter.Builder()
            .setServiceUuid(EDDYSTONE_SERVICE_UUID)
            .setServiceData(EDDYSTONE_SERVICE_UUID, serviceData, serviceDataMask)
            .build()
    }

    fun getBeacon(result: ScanResult, scanRecord: ScanRecord): RadarBeacon? {
        val bytes = scanRecord.bytes
        val serviceUuids = scanRecord.serviceUuids
        val eddystone = serviceUuids != null && serviceUuids.contains(EDDYSTONE_SERVICE_UUID)

        if (eddystone) {
            val hex = this.toHex(bytes)
            val startByte = 26

            val uid = hex.substring(startByte until startByte + 20)
            val identifier = hex.substring(startByte + 20 until startByte + 32)

            return RadarBeacon(
                uuid = uid,
                major = identifier,
                minor = "",
                rssi = result.rssi,
                type = RadarBeacon.RadarBeaconType.EDDYSTONE
            )
        } else {
            var startByte = 2
            var iBeacon = false
            while (startByte <= 5) {
                if ((bytes[startByte + 2].toInt() and 0xFF) == 0x02 &&
                    (bytes[startByte + 3].toInt() and 0xFF) == 0x15
                ) {
                    iBeacon = true
                    break
                }
                startByte++
            }

            if (!iBeacon) {
                return null
            }

            val buf = ByteBuffer.wrap(bytes, startByte + 4, 20)
            val uuid = UUID(buf.long, buf.long)
            val major = ((buf.get().toInt() and 0xFF) * 0x100 + (buf.get().toInt() and 0xFF)).toString()
            val minor = ((buf.get().toInt() and 0xFF) * 0x100 + (buf.get().toInt() and 0xFF)).toString()

            return RadarBeacon(
                uuid = uuid.toString(),
                major = major,
                minor = minor,
                rssi = result.rssi,
                type = RadarBeacon.RadarBeaconType.IBEACON
            )
        }
    }

    private fun toHex(bytes: ByteArray): String {
        val hex = CharArray(2 * bytes.size)
        bytes.forEachIndexed { i, byte ->
            val unsigned = 0xff and byte.toInt()
            hex[2 * i] = HEX[unsigned / 16]
            hex[2 * i + 1] = HEX[unsigned % 16]
        }
        return hex.joinToString("")
    }

    private fun toByteArray(hex: String, max: Int): ByteArray {
        return hex.chunked(2)
            .map { it.toInt(16).toByte() }
            .toByteArray()
            .sliceArray(0 until max)
    }

}