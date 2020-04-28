package org.opencovidtrace.octrace.storage

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.opencovidtrace.octrace.di.ContextProvider
import org.opencovidtrace.octrace.utils.CryptoUtil


object KeyManager : PreferencesHolder("key") {

    private val context by ContextProvider()

    private const val TRACING_KEY = "tracingKey"
    private const val DAILY_KEYS = "dailyKeys"

    /// Used in AG spec v1 and as onboarding indicator

    fun getTracingKey(): ByteArray {
        return getKey() ?: CryptoUtil.generateKey(32).apply { setKey(this) }
    }

    private fun getKey(): ByteArray? {
        return getString(context, TRACING_KEY)?.toByteArray()
    }

    fun setKey(value: ByteArray) {
        setString(context, TRACING_KEY, String(value))
    }

    fun hasKey(): Boolean {
        return getKey() != null
    }

    /// Used in AG spec v1.1
    ////////////////////

    fun getDailyKeys(): HashMap<Int, ByteArray> {
        val storedHashMapString = getString(context, DAILY_KEYS)
        (Gson().fromJson(storedHashMapString) as? HashMap<Int, ByteArray>)?.let {
            return it
        }?: kotlin.run { return hashMapOf() }
    }

    fun setDailyKeys(newValue: HashMap<Int, ByteArray>) {
        val hashMapString = Gson().toJson(newValue)
        setString(context, DAILY_KEYS, hashMapString)
    }

    inline fun <reified T> Gson.fromJson(json: String?) =
        this.fromJson<T>(json, object : TypeToken<T>() {}.type)

}
