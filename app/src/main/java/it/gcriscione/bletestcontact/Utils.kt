package it.gcriscione.bletestcontact

import java.util.*

object Utils {
    fun getEddystoneUid(record: ByteArray): String? {
        val sign =
            byteArrayOf(0x03, 0x03, 0xAA.toByte(), 0xFE.toByte())
        val shortRecord = record.copyOfRange(0, 4)
        val longRecord = record.copyOfRange(3, 7)
        val isShort = shortRecord.contentEquals(sign)
        val frameIndex = if (isShort) 8 else 11
        val isEddystoneUid = (isShort || longRecord.contentEquals(sign)) && (record[frameIndex].toInt() == 0x00)


        if (isEddystoneUid) {
            val uuid =
                Arrays.copyOfRange(record, frameIndex + 2, frameIndex + 18)
            return toHexString(uuid)
        }
        return null
    }

    fun toHexString(a: ByteArray): String {
        val sb = StringBuilder(a.size * 2)
        for (b in a) {
            sb.append(String.format("%02x", b))
        }
        return sb.toString()
    }

    fun fromHexString(s: String): ByteArray {
        val len = s.length
        val data = ByteArray(len / 2)
        var i = 0
        while (i < len) {
            data[i / 2] = ((Character.digit(
                s[i],
                16
            ) shl 4) + Character.digit(s[i + 1], 16)).toByte()
            i += 2
        }
        return data
    }
}