package org.opencovidtrace.octrace.ext.text

import android.annotation.SuppressLint
import android.provider.Settings
import org.opencovidtrace.octrace.di.ContextProvider


private val context by ContextProvider()

@SuppressLint("HardwareIds")
fun getAndroidId(): String = Settings.Secure.getString(
    context.contentResolver,
    Settings.Secure.ANDROID_ID
)

fun ByteArray?.toStringUTF(): String = this?.toString(charset("UTF-8")) ?: ""

fun String.toByteArrayUTF(): ByteArray = toByteArray(charset("UTF-8"))