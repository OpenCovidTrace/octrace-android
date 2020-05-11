package org.opencovidtrace.octrace.ext.text

import android.annotation.SuppressLint
import java.text.SimpleDateFormat
import java.util.*


const val DATE_WITH_TIME_FULL_FORMAT = "[dd MMM yyyy, HH:mm:ss]"

@SuppressLint("SimpleDateFormat")
fun Date.dateFullFormat(): String {
    return SimpleDateFormat(DATE_WITH_TIME_FULL_FORMAT).format(this.time)
}
