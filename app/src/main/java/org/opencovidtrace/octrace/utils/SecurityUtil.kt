package org.opencovidtrace.octrace.utils

import at.favre.lib.crypto.HKDF
import org.opencovidtrace.octrace.storage.KeyManager
import java.nio.ByteBuffer
import java.security.SecureRandom

object SecurityUtil {

    private const val daySeconds = 60 * 60 * 24
    private const val BYTES = java.lang.Long.SIZE / java.lang.Byte.SIZE
    private val hkdf = HKDF.fromHmacSha256()

    private val random = SecureRandom()

    // MARK: - Apple/Google crypto spec: https://www.blog.google/documents/56/Contact_Tracing_-_Cryptography_Specification.pdf

    fun generateKey(): ByteArray {
        val bytes = ByteArray(32)

        random.nextBytes(bytes)

        return bytes
    }

    fun getDailyKey(tracingKey: ByteArray): ByteArray = getDailyKey(tracingKey, currentDayNumber())


    fun getDailyKey(tracingKey: ByteArray, dayNumber: Int): ByteArray {
        var info = "CT-DTK".toByteArray()
        val dayBytes = ByteBuffer.allocate(BYTES).putInt(Integer.reverseBytes(dayNumber)).array()
        info += dayBytes
        return hkdf.extractAndExpand(byteArrayOf(), tracingKey, info, 16)
    }

    fun getSecretDailyKey(tracingKey: ByteArray, dayNumber: Int): ByteArray {
        var dailyKey = getDailyKey(tracingKey, dayNumber)
        dailyKey += tracingKey
        return hkdf.extract(byteArrayOf(), dailyKey)
    }

    fun getRollingId(): ByteArray {
        val dailyKey = KeyManager.getDailyKey(currentDayNumber())
        return getRollingId(dailyKey, getCurrentTimeIntervalNumber())
    }

    private fun getRollingId(dailyKey: ByteArray, timeIntervalNumber: Int): ByteArray {
        var info = "CT-RPI".toByteArray()
        info += timeIntervalNumber.toByte()
        val bytes = hkdf.extract(info, dailyKey)
        return bytes.copyOfRange(0, 16)
    }

    private fun currentDayNumber() = (getTimestamp() / daySeconds).toInt()

    private fun getCurrentTimeIntervalNumber() = (getTimeIntervalNumber(getTimestamp())).toInt()

    private fun getTimeIntervalNumber(timestamp: Long) =
        (timestamp - currentDayNumber() * daySeconds) / (60 * 10)

    private fun getTimestamp() = System.currentTimeMillis() / 1000

}