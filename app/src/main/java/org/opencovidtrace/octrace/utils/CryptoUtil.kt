package org.opencovidtrace.octrace.utils

import android.util.Base64
import at.favre.lib.crypto.HKDF
import org.opencovidtrace.octrace.storage.DataManager
import org.opencovidtrace.octrace.storage.KeyManager
import org.opencovidtrace.octrace.storage.KeysManager
import java.nio.ByteBuffer
import java.security.SecureRandom
import java.util.*

object CryptoUtil {

    private const val daySeconds = 60 * 60 * 24
    private const val BYTES = 4
    private val hkdf = HKDF.fromHmacSha256()

    private val random = SecureRandom()
    val spec = AgSpecV1_1.instance


    // MARK: - AES 128bit

    fun encodeAES(value: ByteArray, key: ByteArray) =
        AESEncryptor.encrypt(value, key) ?: byteArrayOf()

    fun decodeAES(value: ByteArray, key: ByteArray) = AESEncryptor.decryptWithAES(value, key)


    fun generateKey(size: Int): ByteArray {
        val bytes = ByteArray(size)

        random.nextBytes(bytes)

        return bytes
    }

    fun toSecretKey(key: ByteArray): String = key.base64EncodedString()

    fun getLatestSecretDailyKeys(): List<String> {
        return spec.getLatestDailyKeys().map(this::toSecretKey)
    }

    fun getRollingId(): ByteArray {
        val dailyKey = spec.getDailyKey(currentDayNumber())
        return spec.getRollingId(dailyKey, Calendar.getInstance())
    }

    fun getDayNumber(tst: Long) = (tst / 1000 / daySeconds).toInt()

    fun getDayNumber(date: Calendar) = getDayNumber(date.timeInMillis)

    fun currentDayNumber() = (getTimestamp() / daySeconds).toInt()


    private fun getTimeIntervalNumber(timestamp: Long): Int =
        ((timestamp - currentDayNumber() * daySeconds) / (60 * 10)).toInt()

    private fun getEnIntervalNumber(timestamp: Long): Int = (timestamp / (60 * 10)).toInt()


    private fun getTimestamp() = System.currentTimeMillis() / 1000

    fun getDailyKeys(dayNumber: Int) : Pair<ByteArray, ByteArray> {
        val dailyKeys = KeysManager.getDailyKeys()
        val metaKeys = KeysManager.getMetaKeys()

        dailyKeys[dayNumber]?.let {dailyKey->
            metaKeys[dayNumber]?.let {metaKey->
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


    interface CryptoSpec {
        /// Temporary Exposure Key
        fun getDailyKey(dayNumber: Int): ByteArray

        /// Rolling Proximity Identifier
        fun getRollingId(dailyKey: ByteArray, date: Calendar): ByteArray

        /// This is extension to Apple/Google spec: we use contact timestamp to match daily key
        fun match(rollingId: String, date: Calendar, dailyKey: ByteArray): Boolean

        fun getLatestDailyKeys(): List<ByteArray>
    }

    // MARK: - Apple/Google crypto spec:
    // https://www.blog.google/documents/56/Contact_Tracing_-_Cryptography_Specification.pdf

    class AgSpecV1 : CryptoSpec {

        companion object {
            val instance = AgSpecV1()
        }

        override fun getDailyKey(dayNumber: Int): ByteArray {
            var info = "CT-DTK".toByteArray()
            val dayBytes =
                ByteBuffer.allocate(BYTES).putInt(Integer.reverseBytes(dayNumber)).array()
            info += dayBytes
            val tracingKey = KeyManager.getTracingKey()
            return hkdf.extractAndExpand(byteArrayOf(), tracingKey, info, 16)
        }

        override fun getRollingId(dailyKey: ByteArray, date: Calendar): ByteArray {
            return getRollingId(dailyKey, getTimeIntervalNumber(date))
        }

        override fun match(rollingId: String, date: Calendar, dailyKey: ByteArray): Boolean {
            val timeIntervalNumber = getTimeIntervalNumber(date)

            val idExact = getRollingId(dailyKey, timeIntervalNumber).base64EncodedString()
            val idBefore = getRollingId(dailyKey, timeIntervalNumber - 1).base64EncodedString()
            val idAfter = getRollingId(dailyKey, timeIntervalNumber + 1).base64EncodedString()

            return rollingId == idExact || rollingId == idBefore || rollingId == idAfter
        }

        override fun getLatestDailyKeys(): List<ByteArray> {
            val result = arrayListOf<ByteArray>()
            val dayNumber = currentDayNumber()
            var offset = 0
            while (offset < DataManager.maxDays) {
                result.add(getDailyKey(dayNumber - offset))
                offset += 1
            }
            return result
        }

        private fun getRollingId(dailyKey: ByteArray, timeIntervalNumber: Int): ByteArray {
            var info = "CT-RPI".toByteArray()
            info += timeIntervalNumber.toByte()
            val bytes = hkdf.extract(info, dailyKey)
            return bytes.copyOfRange(0, 16)
        }

        private fun getTimeIntervalNumber(date: Calendar) =
            getTimeIntervalNumber(date.timeInMillis / 1000)
    }

    /// https://www.blog.google/documents/60/Exposure_Notification_-_Cryptography_Specification_v1.1.pdf


    class AgSpecV1_1 : CryptoSpec {

        companion object {
            val instance = AgSpecV1_1()
        }

        private val info = "EN-RPIK".toByteArray()
        private val rpiPrefix = "EN-RPI".toByteArray()

        override fun getDailyKey(dayNumber: Int): ByteArray {
            val dailyKeys = KeysManager.getDailyKeys()
            dailyKeys[dayNumber]?.let { return it }

            val dailyKey = generateKey(16)
            dailyKeys[dayNumber] = dailyKey

            KeysManager.setDailyKeys(dailyKeys)

            return dailyKey
        }

        override fun getRollingId(dailyKey: ByteArray, date: Calendar): ByteArray {
            return getRollingId(dailyKey, getEnIntervalNumber(date))
        }

        override fun match(rollingId: String, date: Calendar, dailyKey: ByteArray): Boolean {
            val enIntervalNumber = getEnIntervalNumber(date)

            // We check 3 nearest ids in case of timestamp rolling
            val idExact = getRollingId(dailyKey, enIntervalNumber).base64EncodedString()
            val idBefore = getRollingId(dailyKey, enIntervalNumber - 1).base64EncodedString()
            val idAfter = getRollingId(dailyKey, enIntervalNumber + 1).base64EncodedString()

            return rollingId == idExact || rollingId == idBefore || rollingId == idAfter
        }

        override fun getLatestDailyKeys(): List<ByteArray> {
            val lastDayNumber = currentDayNumber() - DataManager.maxDays

            return KeysManager.getDailyKeys().filterKeys { it > lastDayNumber }.values.toList()
        }

        private fun getRollingId(dailyKey: ByteArray, enIntervalNumber: Int): ByteArray {
            val rpiKey = hkdf.extractAndExpand(byteArrayOf(), dailyKey, info, 16)
            var paddedData = rpiPrefix
            for (i in 6..11) {
                paddedData += 0
            }
            val eninBytes =
                ByteBuffer.allocate(BYTES).putInt(Integer.reverseBytes(enIntervalNumber)).array()
            paddedData += eninBytes
            return encodeAES(paddedData, rpiKey)
        }

        private fun getEnIntervalNumber(date: Calendar) =
            getEnIntervalNumber(date.timeInMillis / 1000)
    }


    fun ByteArray.base64EncodedString(): String = Base64.encodeToString(this, Base64.DEFAULT)

    fun String.base64DecodeByteArray(): ByteArray = Base64.decode(this, Base64.DEFAULT)


}