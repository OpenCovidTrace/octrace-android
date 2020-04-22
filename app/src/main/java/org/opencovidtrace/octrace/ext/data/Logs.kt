package org.opencovidtrace.octrace.ext.data

import org.opencovidtrace.octrace.data.LogTableValue
import org.opencovidtrace.octrace.di.DatabaseProvider
import org.opencovidtrace.octrace.utils.DoAsync
import java.lang.Exception

private val database by DatabaseProvider()

fun insertLogs(event: String, additionalInfo: String) {
    LogTableValue(event, additionalInfo).add()
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