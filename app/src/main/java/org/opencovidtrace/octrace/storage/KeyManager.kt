package org.opencovidtrace.octrace.storage

import org.opencovidtrace.octrace.utils.CryptoUtil


object KeyManager : PreferencesHolder("key") {

    private const val TRACING_KEY = "tracingKey"

    /// Used in AG spec v1 and as onboarding indicator

    fun getTracingKey(): ByteArray {
        return getKey() ?: CryptoUtil.generateKey(32).apply { setKey(this) }
    }

    private fun getKey(): ByteArray? {
        return getString(TRACING_KEY)?.toByteArray()
    }

    fun setKey(value: ByteArray) {
        setString(TRACING_KEY, String(value))
    }

    fun hasKey(): Boolean {
        return getKey() != null
    }

}
