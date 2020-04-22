package org.opencovidtrace.octrace.ext.text

import android.annotation.SuppressLint
import java.text.SimpleDateFormat
import java.util.*

const val MAIN_SERVER_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss"
const val DATE_WITH_TIME_FULL_FORMAT = "yyyy-MM-dd HH:mm:ss"
const val DATE_TIME_FORMAT = "dd MMMM HH:mm"


@SuppressLint("SimpleDateFormat")
fun Calendar.serverDateFormat(): String {
    return SimpleDateFormat(MAIN_SERVER_DATE_FORMAT).format(this.time)
}

@SuppressLint("SimpleDateFormat")
fun Calendar.dateFullFormat(): String {
    return SimpleDateFormat(DATE_WITH_TIME_FULL_FORMAT).format(this.time)
}

@SuppressLint("SimpleDateFormat")
fun Calendar.dateTimeFormat(): String {
    return SimpleDateFormat(DATE_TIME_FORMAT).format(this.time)
}
