package org.opencovidtrace.octrace.storage

import org.opencovidtrace.octrace.di.api.ApiClientProvider
import org.opencovidtrace.octrace.utils.CryptoUtil
import org.opencovidtrace.octrace.utils.CryptoUtil.base64EncodedString
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import kotlin.math.min


object KeysManager : PreferencesHolder("keys") {

    private const val LAST_UPLOAD_DAY_KEY = "lastUploadDay"
    private val apiClient by ApiClientProvider()

    private fun getLastUploadDay(): Int {
        return getInt(LAST_UPLOAD_DAY_KEY)
    }

    fun setLastUploadDay(value: Int) {
        setInt(LAST_UPLOAD_DAY_KEY, value)
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
            borders[dayNumber]?.let { border->
                val keyValue = CryptoUtil.spec.getDailyKey(dayNumber).base64EncodedString()
                border.secure()
                val key = Key(keyValue, dayNumber, border)

                keysData.keys.add(key)
            }

            offset += 1
        }

        apiClient.sendKeys(keysData)
            .enqueue(object: Callback<String>{

                override fun onResponse(call: Call<String>, response: Response<String>) {
                    setLastUploadDay(previousDayNumber)
                }

                override fun onFailure(call: Call<String>, t: Throwable) {
                    println("ERROR: ${t.message}")
                }

            })
    }

    class KeysData {
        var keys: MutableList<Key> = arrayListOf()
    }

    data class Key(
        val value: String,
        val day: Int,
        val border: LocationBordersManager.LocationBorder
    )
}