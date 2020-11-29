package io.radar.sdk

import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanRecord
import android.os.Build
import androidx.annotation.RequiresApi
import io.radar.sdk.model.RadarBeacon
import java.nio.ByteBuffer
import java.util.*

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
class RadarBeaconUtils {

    internal companion object {

        private const val MANUFACTURER_ID = 76

        fun getScanFilter(beacon: RadarBeacon): ScanFilter? {
            val manufacturerData = byteArrayOf(
                0, 0,

                // uuid
                0, 0, 0, 0,
                0, 0,
                0, 0,
                0, 0, 0, 0, 0, 0, 0, 0,

                // major
                0, 0,

                // minor
                0, 0,

                0
            )

            val manufacturerDataMask = byteArrayOf(
                0, 0,

                // uuid
                1, 1, 1, 1,
                1, 1,
                1, 1,
                1, 1, 1, 1, 1, 1, 1, 1,

                // major
                1, 1,

                // minor
                1, 1,

                0
            )

            System.arraycopy(uuidToByteArray(UUID.fromString(beacon.uuid)), 0, manufacturerData, 2, 16)
            System.arraycopy(intToByteArray(beacon.major.toInt()), 0, manufacturerData, 18, 2)
            System.arraycopy(intToByteArray(beacon.minor.toInt()), 0, manufacturerData, 20, 2)

           return ScanFilter.Builder()
                .setManufacturerData(MANUFACTURER_ID, manufacturerData, manufacturerDataMask)
                .build()
        }

        fun getBeacon(beacons: Array<RadarBeacon>, scanRecord: ScanRecord): RadarBeacon? {
            val bytes = scanRecord.bytes

            var startByte = 2
            var iBeacon = false
            while (startByte <= 5) {
                if ((bytes[startByte + 2].toInt() and 0xff) == 0x02 &&
                    (bytes[startByte + 3].toInt() and 0xff) == 0x15) {
                    iBeacon = true
                    break
                }
                startByte++
            }

            if (!iBeacon) {
                return null
            }

            val uuid = byteArrayToUUID(Arrays.copyOfRange(bytes, startByte + 4, startByte + 20)).toString()
            val major = byteArrayToInt(Arrays.copyOfRange(bytes, startByte + 20, startByte + 22)).toString()
            val minor = byteArrayToInt(Arrays.copyOfRange(bytes, startByte + 22, startByte + 24)).toString()

            return beacons.find { it.uuid == uuid && it.major == major && it.minor == minor }
        }

        private fun uuidToByteArray(uuid: UUID): ByteArray {
            val bb = ByteBuffer.wrap(ByteArray(16))
            bb.putLong(uuid.mostSignificantBits)
            bb.putLong(uuid.leastSignificantBits)
            return bb.array()
        }

        private fun intToByteArray(int: Int): ByteArray {
            val bb = ByteBuffer.wrap(ByteArray(2))
            bb.putInt(int)
            return bb.array()
        }

        private fun byteArrayToUUID(bytes: ByteArray): UUID {
            val bb = ByteBuffer.wrap(bytes)
            val mostSignificantBits = bb.long
            val leastSignificantBits = bb.long
            return UUID(mostSignificantBits, leastSignificantBits)
        }

        private fun byteArrayToInt(bytes: ByteArray): Int {
            val bb = ByteBuffer.wrap(bytes)
            return bb.int
        }

    }

}