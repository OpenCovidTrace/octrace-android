package org.opencovidtrace.octrace.ext.ui

import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import org.opencovidtrace.octrace.R

fun Fragment.showInfo(@StringRes messageId: Int) {
    showInfo(getString(messageId))
}

fun Fragment.showInfo(message: String) {
    AlertDialog.Builder(requireContext()).apply {
        setTitle(R.string.app_name)
        setMessage(message)
        setCancelable(false)
        setPositiveButton(R.string.ok) { _, _ -> }
        show()
    }
}

fun Fragment.confirm(@StringRes messageId: Int, onOkClick: () -> Unit) {
    confirm(getString(messageId), onOkClick)
}

fun Fragment.confirm(message: String, onOkClick: () -> Unit) {
    AlertDialog.Builder(requireContext()).apply {
        setTitle(R.string.app_name)
        setMessage(message)
        setCancelable(false)
        setPositiveButton(R.string.ok) { _, _ -> onOkClick() }
            .setNegativeButton(R.string.cancel) { _, _ -> }
        show()
    }
}