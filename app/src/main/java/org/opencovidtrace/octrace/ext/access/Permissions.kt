package org.opencovidtrace.octrace.ext.access

import android.app.Activity
import android.content.pm.PackageManager.PERMISSION_GRANTED

import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat.checkSelfPermission
import androidx.fragment.app.Fragment
import org.opencovidtrace.octrace.di.ContextProvider

fun String.isNotGranted() = !isGranted()

fun String.isGranted(): Boolean {
    val context by ContextProvider()
    return checkSelfPermission(context, this) == PERMISSION_GRANTED
}

inline fun Activity.withPermissions(permissions: Array<String>, requestCode: Int, run: () -> Unit) {
    val notGrantedPermissions = permissions.filter { it.isNotGranted() }
    if (notGrantedPermissions.isEmpty()) run()
    else ActivityCompat.requestPermissions(this, notGrantedPermissions.toTypedArray(), requestCode)
}

inline fun Fragment.withPermissions(permissions: Array<String>, requestCode: Int, run: () -> Unit) {
    val notGrantedPermissions = permissions.filter { it.isNotGranted() }
    if (notGrantedPermissions.isEmpty()) run()
    else requestPermissions(notGrantedPermissions.toTypedArray(), requestCode)
}
