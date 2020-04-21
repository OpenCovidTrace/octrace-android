package org.opencovidtrace.octrace.storage

import android.content.Context

object KeyManager : PreferencesHolder("key") {

    private const val TRACING_KEY = "tracingKey"

    fun getKey(ctx: Context): ByteArray? {
        return getString(ctx, TRACING_KEY)?.toByteArray()
    }

    fun setKey(ctx: Context, value: ByteArray) {
        setString(ctx, TRACING_KEY, String(value))
    }

    fun hasKey(ctx: Context): Boolean {
        return getKey(ctx) != null
    }

}
