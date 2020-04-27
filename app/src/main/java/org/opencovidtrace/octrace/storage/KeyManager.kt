package org.opencovidtrace.octrace.storage

import org.opencovidtrace.octrace.di.ContextProvider
import org.opencovidtrace.octrace.utils.SecurityUtil

object KeyManager : PreferencesHolder("key") {

    private val context by ContextProvider()

    private const val TRACING_KEY = "tracingKey"

    private fun getKey(): ByteArray? {
        return getString(context, TRACING_KEY)?.toByteArray()
    }

    private fun getTracingKey(): ByteArray {
        return getKey() ?: SecurityUtil.generateKey().apply {
            setKey(this)
        }
    }

    fun setKey(value: ByteArray) {
        setString(context, TRACING_KEY, String(value))
    }

    fun hasKey(): Boolean {
        return getKey() != null
    }

    fun getDailyKey(dayNumber: Int): ByteArray {
        return SecurityUtil.getDailyKey(getTracingKey(), dayNumber)
    }


}
