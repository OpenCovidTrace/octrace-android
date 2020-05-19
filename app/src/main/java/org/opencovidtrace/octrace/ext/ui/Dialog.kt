package org.opencovidtrace.octrace.ext.ui

import android.app.Activity
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import org.opencovidtrace.octrace.R

fun Fragment.showInfo(@StringRes messageId: Int) {
    activity?.showInfo(getString(messageId))
}

fun Activity.showInfo(@StringRes messageId: Int) {
    showInfo(getString(messageId))
}

fun Activity.showInfo(message: String) {
    AlertDialog.Builder(this).apply {
        setTitle(R.string.app_name)
        setMessage(message)
        setCancelable(false)
        setPositiveButton(R.string.ok) { _, _ -> }
        show()
    }
}

fun Fragment.confirm(@StringRes messageId: Int, onOkClick: () -> Unit) {
    activity?.confirm(getString(messageId), onOkClick)
}

fun Activity.confirm(message: String, onOkClick: () -> Unit) {
    AlertDialog.Builder(this).apply {
        setTitle(R.string.please_confirm)
        setMessage(message)
        setCancelable(false)
        setPositiveButton(R.string.ok) { _, _ -> onOkClick() }
            .setNegativeButton(R.string.cancel) { _, _ -> }
        show()
    }
}

fun Fragment.choose(@StringRes messageId: Int, onYesClick: () -> Unit, onNoClick: () -> Unit) {
    activity?.choose(messageId, onYesClick, onNoClick)
}

fun Activity.choose(@StringRes messageId: Int, onYesClick: () -> Unit, onNoClick: () -> Unit) {
    AlertDialog.Builder(this).apply {
        setTitle(R.string.make_choice)
        setMessage(getString(messageId))
        setCancelable(false)
        setPositiveButton(R.string.yes) { _, _ -> onYesClick() }
            .setNegativeButton(R.string.no) { _, _ -> onNoClick() }
        show()
    }
}

fun Fragment.showError(@StringRes messageId: Int) {
    activity?.showError(getString(messageId))
}

fun Activity.showError(@StringRes messageId: Int) {
    showError(getString(messageId))
}

fun Activity.showError(message: String) {
    AlertDialog.Builder(this).apply {
        setTitle(R.string.error)
        setMessage(message)
        setCancelable(false)
        setPositiveButton(R.string.ok) { _, _ -> }
        show()
    }
}