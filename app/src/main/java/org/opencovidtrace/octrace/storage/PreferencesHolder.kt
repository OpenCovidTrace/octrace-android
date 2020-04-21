package org.opencovidtrace.octrace.storage

import android.content.Context
import android.content.SharedPreferences

open class PreferencesHolder(private val preferencesName: String) {

    private var preferences: SharedPreferences? = null

    private fun initPreferences(ctx: Context) {
        if (preferences == null) {
            preferences =
                ctx.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
        }
    }

    fun getString(ctx: Context, key: String): String? {
        initPreferences(ctx)

        return preferences!!.getString(key, null)
    }

    fun setString(
        ctx: Context,
        key: String,
        value: String?
    ) {
        withEditor(ctx) { editor -> editor.putString(key, value) }
    }

    fun setInt(ctx: Context, key: String, value: Int) {
        withEditor(ctx) { editor -> editor.putInt(key, value) }
    }

    fun setBoolean(
        ctx: Context,
        key: String,
        value: Boolean
    ) {
        withEditor(ctx) { editor -> editor.putBoolean(key, value) }
    }

    fun remove(ctx: Context, key: String) {
        withEditor(ctx) { editor -> editor.remove(key) }
    }

    private fun withEditor(
        ctx: Context,
        handler: (SharedPreferences.Editor) -> Unit
    ) {
        initPreferences(ctx)
        val editor = preferences!!.edit()
        handler(editor)
        editor.apply()
    }

}
