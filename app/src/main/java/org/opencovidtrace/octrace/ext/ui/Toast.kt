package org.opencovidtrace.octrace.ext.ui

import android.content.Context

import android.widget.Toast.*
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment

fun Context.toast(@StringRes messageRes: Int) = toast(getString(messageRes))

fun Context.toast(message: String?) {
    if (message.isNullOrBlank()) return
    makeText(this, message, if (message.length > 50) LENGTH_LONG else LENGTH_SHORT).show()
}

fun Fragment.toast(@StringRes messageRes: Int) {
    context?.toast(messageRes)
}

fun Fragment.toast(message: String?) {
    context?.toast(message)
}
