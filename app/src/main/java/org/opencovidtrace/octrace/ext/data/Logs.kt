package org.opencovidtrace.octrace.ext.data

import org.opencovidtrace.octrace.data.LogTableValue
import org.opencovidtrace.octrace.di.DatabaseProvider
import org.opencovidtrace.octrace.utils.DoAsync

private val database by DatabaseProvider()

fun insertLogs(tag: String, text: String) {
    LogTableValue(tag, text).add()
}

fun LogTableValue.add() {
    DoAsync {
        try {
            database.appDao().insertLog(this)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}