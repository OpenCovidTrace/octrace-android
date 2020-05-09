package org.opencovidtrace.octrace.storage

import com.google.gson.Gson
import org.opencovidtrace.octrace.data.Key
import org.opencovidtrace.octrace.data.KeysData
import org.opencovidtrace.octrace.di.api.ApiClientProvider
import org.opencovidtrace.octrace.utils.CryptoUtil
import org.opencovidtrace.octrace.utils.CryptoUtil.base64EncodedString
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import kotlin.math.min


object KeysManager : PreferencesHolder("keys") {

    private const val LAST_UPLOAD_DAY = "lastUploadDay"
    private const val DISCLOSE_META_DATA = "discloseMetaData"
    private const val DAILY_KEYS = "dailyKeys"
    private const val META_KEYS = "metaKeys"

    private val apiClient by ApiClientProvider()

    fun getDailyKeys(): HashMap<Int, ByteArray> {
        val storedHashMapString = KeyManager.getString(DAILY_KEYS)
        (Gson().fromJson(storedHashMapString) as? HashMap<Int, ByteArray>)?.let {
            return it
        } ?: kotlin.run { return hashMapOf() }
    }

    fun setDailyKeys(newValue: HashMap<Int, ByteArray>) {
        val hashMapString = Gson().toJson(newValue)
        KeyManager.setString(DAILY_KEYS, hashMapString)
    }

    fun getMetaKeys(): HashMap<Int, ByteArray> {
        val storedHashMapString = KeyManager.getString(META_KEYS)
        (Gson().fromJson(storedHashMapString) as? HashMap<Int, ByteArray>)?.let {
            return it
        } ?: kotlin.run { return hashMapOf() }
    }

    fun setMetaKeys(newValue: HashMap<Int, ByteArray>) {
        val hashMapString = Gson().toJson(newValue)
        KeyManager.setString(META_KEYS, hashMapString)
    }

    private fun getLastUploadDay(): Int {
        return getInt(LAST_UPLOAD_DAY)
    }

    fun setLastUploadDay(value: Int) {
        setInt(LAST_UPLOAD_DAY, value)
    }

    private fun isDiscloseMetaData(): Boolean {
        return getBoolean(DISCLOSE_META_DATA)
    }

    fun setDiscloseMetaData(value: Boolean) {
        setBoolean(DISCLOSE_META_DATA, value)
    }

    fun uploadNewKeys() {
        val oldLastUploadDay = getLastUploadDay()

        // Uploading after EOD to include widest borders
        val previousDayNumber = CryptoUtil.currentDayNumber() - 1

        if (oldLastUploadDay == previousDayNumber) {
            return
        }

        val borders = LocationBordersManager.getLocationBorders()

        val keysData = KeysData()
        val diff = min(previousDayNumber - oldLastUploadDay, DataManager.maxDays)

        var offset = 0
        while (offset < diff) {
            val dayNumber = previousDayNumber - offset

            // We currently don't upload diagnostic keys without location data!
            borders[dayNumber]?.let { border ->
                val keyValue = CryptoUtil.spec.getDailyKey(dayNumber).base64EncodedString()
                border.secure()
                val key = Key(keyValue, dayNumber, border)

                keysData.keys.add(key)
            }

            offset += 1
        }

        apiClient.sendKeys(keysData)
            .enqueue(object : Callback<String> {

                override fun onResponse(call: Call<String>, response: Response<String>) {
                    setLastUploadDay(previousDayNumber)
                }

                override fun onFailure(call: Call<String>, t: Throwable) {
                    println("ERROR: ${t.message}")
                }

            })
    }

}