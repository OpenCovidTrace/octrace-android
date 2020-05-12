package org.opencovidtrace.octrace.utils

import android.util.Base64
import at.favre.lib.crypto.HKDF
import org.opencovidtrace.octrace.data.ContactCoord
import org.opencovidtrace.octrace.data.ContactMetaData
import org.opencovidtrace.octrace.location.LocationUpdateManager
import org.opencovidtrace.octrace.storage.DataManager
import org.opencovidtrace.octrace.storage.KeysManager
import java.nio.ByteBuffer
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.*

object CryptoUtil {

    const val keyLength = 16

    private const val daySeconds = 60 * 60 * 24
    private const val enIntervalSeconds = 60 * 10
    private const val coordPrecision = 1e7
    private val info = "EN-RPIK".toByteArray()
    private val rpiPrefix = "EN-RPI".toByteArray()
    private val hkdf = HKDF.fromHmacSha256()
    private val digest = MessageDigest.getInstance("SHA-256")

    private val random = SecureRandom()

    // MARK: - AES 128bit

    fun encodeAES(value: ByteArray, key: ByteArray) = AESEncryptor.encrypt(value, key)!!

    fun decodeAES(value: ByteArray, key: ByteArray) = AESEncryptor.decryptWithAES(value, key)!!


    fun generateKey(size: Int): ByteArray {
        val bytes = ByteArray(size)

        random.nextBytes(bytes)

        return bytes
    }

    fun toSecretKey(key: ByteArray): String = digest.digest(key).base64EncodedString()

    fun getLatestSecretDailyKeys(): List<String> = getLatestDailyKeys().map(this::toSecretKey)

    fun getCurrentRpi(): ByteArray {
        val (rollingId, meta) = getCurrentRollingIdAndMeta()

        var data = rollingId
        data += meta

        return data
    }

    fun getCurrentRollingIdAndMeta(): Pair<ByteArray, ByteArray> {
        val date = Date()
        val dayNumber = getDayNumber(date)
        val (dailyKey, metaKey) = getDailyKeys(dayNumber)

        return Pair(getRollingId(dailyKey, date), getMetaData(date, metaKey))
    }

    fun getDayNumber(tst: Long) = ((tst / 1000) / daySeconds).toInt()

    fun getDayNumber(date: Date) = getDayNumber(date.time)

    fun currentDayNumber() = (getTimestamp() / daySeconds).toInt()

    private fun getEnIntervalNumber(date: Date) =
        getEnIntervalNumber((date.time / 1000).toInt())

    private fun getEnIntervalNumber(timeInterval: Int): Int = timeInterval / enIntervalSeconds

    private fun getTimestamp() = System.currentTimeMillis() / 1000

    fun getDailyKeys(dayNumber: Int): Pair<ByteArray, ByteArray> {
        val dailyKeys = KeysManager.getDailyKeys()
        val metaKeys = KeysManager.getMetaKeys()

        dailyKeys[dayNumber]?.let { dailyKey ->
            metaKeys[dayNumber]?.let { metaKey ->
                return Pair(dailyKey, metaKey)
            }
        }

        val dailyKey = generateKey(32)
        val metaKey = generateKey(32)

        dailyKeys[dayNumber] = dailyKey
        metaKeys[dayNumber] = metaKey

        KeysManager.setDailyKeys(dailyKeys)
        KeysManager.setMetaKeys(metaKeys)

        return Pair(dailyKey, metaKey)
    }

    fun getMetaData(date: Date, metaKey: ByteArray): ByteArray {
        val timeInterval = (date.time / 1000).toInt()

        var data = intToBytes(timeInterval)

        var latInt = Int.MAX_VALUE
        var lngInt = Int.MAX_VALUE
        var accuracy = 0

        LocationUpdateManager.getLastLocation()?.let { location ->
            latInt = coordToInt(location.latitude)
            lngInt = coordToInt(location.longitude)
            accuracy = location.accuracy.toInt()
        }

        data += intToBytes(latInt)
        data += intToBytes(lngInt)
        data += intToBytes(accuracy)

        return encodeAES(data, getEncryptionKey(metaKey))
    }

    fun decodeMetaData(encryptedData: ByteArray, metaKey: ByteArray): ContactMetaData {
        val data = decodeAES(encryptedData, getEncryptionKey(metaKey))

        val timeInterval = bytesToInt(data.sliceArray(0..7))
        val date = Date(timeInterval.toLong() * 1000)

        var coord: ContactCoord? = null

        val latInt = bytesToInt(data.sliceArray(8..15))
        if (latInt != Int.MAX_VALUE) {
            val lngInt = bytesToInt(data.sliceArray(16..23))
            val accuracy = bytesToInt(data.sliceArray(24..31))

            coord = ContactCoord(coordToDouble(latInt), coordToDouble(lngInt), accuracy)
        }

        return ContactMetaData(coord, date)
    }

    private fun coordToInt(value: Double): Int {
        return (value * coordPrecision).toInt()
    }

    private fun coordToDouble(value: Int): Double {
        return value.toDouble() / coordPrecision
    }

    private fun bytesToInt(bytes: ByteArray): Int {
        var value = 0

        bytes.forEach { byte ->
            value = value shl 8
            value = value or byte.toInt()
        }

        return value
    }

    private fun intToBytes(value: Int) =
        ByteBuffer.allocate(4).putInt(Integer.reverseBytes(value)).array()

    fun getRollingId(dailyKey: ByteArray, date: Date): ByteArray {
        return getRollingId(dailyKey, getEnIntervalNumber(date))
    }

    fun match(rollingId: String, dayNumber: Int, dailyKey: ByteArray): Boolean {
        val rpiKey = getEncryptionKey(dailyKey)

        val firstEnIntervalNumber = getEnIntervalNumber(dayNumber * daySeconds)
        val nextDayEnIntervalNumber = getEnIntervalNumber((dayNumber + 1) * daySeconds)

        for (enIntervalNumber in firstEnIntervalNumber until nextDayEnIntervalNumber) {
            if (rollingId == getRollingId(rpiKey, enIntervalNumber).base64EncodedString()) {
                return true
            }
        }

        return false
    }

    fun getLatestDailyKeys(): List<ByteArray> {
        val lastDayNumber = currentDayNumber() - DataManager.maxDays

        return KeysManager.getDailyKeys().filterKeys { it > lastDayNumber }.values.toList()
    }

    private fun getRollingId(dailyKey: ByteArray, enIntervalNumber: Int): ByteArray {
        val rpiKey = getEncryptionKey(dailyKey)
        var paddedData = rpiPrefix
        for (i in 6..11) {
            paddedData += 0
        }
        val eninBytes = intToBytes(enIntervalNumber)
        paddedData += eninBytes
        return encodeAES(paddedData, rpiKey)
    }

    private fun getEncryptionKey(key: ByteArray): ByteArray =
        hkdf.extractAndExpand(byteArrayOf(), key, info, CryptoUtil.keyLength)


    fun ByteArray.base64EncodedString(): String = Base64.encodeToString(this, Base64.DEFAULT)

    fun String.base64DecodeByteArray(): ByteArray = Base64.decode(this, Base64.DEFAULT)
}
