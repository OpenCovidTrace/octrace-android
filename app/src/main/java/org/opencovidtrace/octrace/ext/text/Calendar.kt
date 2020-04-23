package org.opencovidtrace.octrace.ext.text

import android.annotation.SuppressLint
import java.text.SimpleDateFormat
import java.util.*


const val DATE_TIME_FORMAT = "dd MMMM HH:mm"

@SuppressLint("SimpleDateFormat")
fun Calendar.dateTimeFormat(): String {
    return SimpleDateFormat(DATE_TIME_FORMAT).format(this.time)
}
