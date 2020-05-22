package org.opencovidtrace.octrace.ext.data

import org.opencovidtrace.octrace.data.BtLogTableValue
import org.opencovidtrace.octrace.data.Dp3tLogTableValue
import org.opencovidtrace.octrace.di.DatabaseProvider
import org.opencovidtrace.octrace.utils.DoAsync

private val database by DatabaseProvider()

fun insertBtLogs(tag: String, text: String) {
    BtLogTableValue(tag, text).add()
}

fun BtLogTableValue.add() {
    DoAsync {
        try {
            database.appDao().insertBtLog(this)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}


fun insertDp3tLogs(text: String) {
    Dp3tLogTableValue(text).add()
}

fun Dp3tLogTableValue.add() {
    DoAsync {
        try {
            database.appDao().insertDp3tLog(this)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}