package org.opencovidtrace.octrace.storage

import com.google.gson.Gson
import org.opencovidtrace.octrace.di.api.ApiClientProvider
import org.opencovidtrace.octrace.utils.CryptoUtil
import org.opencovidtrace.octrace.utils.CryptoUtil.base64EncodedString
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import kotlin.math.min


object KeysManager : PreferencesHolder("keys") {

    private const val LAST_UPLOAD_DAY = "lastUploadDay"
    private const val DAILY_KEYS = "dailyKeys"
    private const val META_KEYS = "metaKeys"

    private val apiClient by ApiClientProvider()

    fun getDailyKeys(): HashMap<Int, ByteArray> {
        val storedHashMapString = OnboardingManager.getString(DAILY_KEYS)
        (Gson().fromJson(storedHashMapString) as? HashMap<Int, ByteArray>)?.let {
            return it
        } ?: kotlin.run { return hashMapOf() }
    }

    fun setDailyKeys(newValue: HashMap<Int, ByteArray>) {
        val hashMapString = Gson().toJson(newValue)
        OnboardingManager.setString(DAILY_KEYS, hashMapString)
    }

    fun getMetaKeys(): HashMap<Int, ByteArray> {
        val storedHashMapString = OnboardingManager.getString(META_KEYS)
        (Gson().fromJson(storedHashMapString) as? HashMap<Int, ByteArray>)?.let {
            return it
        } ?: kotlin.run { return hashMapOf() }
    }

    fun setMetaKeys(newValue: HashMap<Int, ByteArray>) {
        val hashMapString = Gson().toJson(newValue)
        OnboardingManager.setString(META_KEYS, hashMapString)
    }

    private fun getLastUploadDay(): Int {
        return getInt(LAST_UPLOAD_DAY)
    }

    fun setLastUploadDay(value: Int) {
        setInt(LAST_UPLOAD_DAY, value)
    }

    fun uploadNewKeys(includeToday: Boolean = false) {
        val oldLastUploadDay = getLastUploadDay()

        // Uploading after EOD to include widest borders
        val currentDayNumber = CryptoUtil.currentDayNumber()
        val previousDayNumber = CryptoUtil.currentDayNumber() - 1

        if (oldLastUploadDay == previousDayNumber) {
            return
        }

        val borders = LocationBordersManager.getLocationBorders()

        val keysData = KeysData()

        fun addKey(dayNumber: Int) {
            // We currently don't upload diagnostic keys without location data!
            borders[dayNumber]?.let { border ->
                border.secure()

                val dailyKey = getDailyKeys()[dayNumber]!!.base64EncodedString()
                val meta = if (UserSettingsManager.discloseMetaData) {
                    getMetaKeys()[dayNumber]!!.base64EncodedString()
                } else {
                    null
                }

                val key = Key(dailyKey, meta, dayNumber, border)

                keysData.keys.add(key)
            }
        }

        if (includeToday) {
            // Include key for today when reporting exposure
            // This key will be uploaded again next day with updated borders
            addKey(currentDayNumber)
        }

        val diff = min(previousDayNumber - oldLastUploadDay, DataManager.maxDays)

        var offset = 0
        while (offset < diff) {
            val dayNumber = previousDayNumber - offset

            addKey(dayNumber)

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


data class KeysData(var keys: MutableList<Key> = arrayListOf())

data class Key(
    val value: String,
    val meta: String?,
    val day: Int,
    val border: LocationBordersManager.LocationBorder
)
