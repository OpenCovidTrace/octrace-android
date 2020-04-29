package org.opencovidtrace.octrace.ui.map.logs

import androidx.lifecycle.ViewModel
import org.opencovidtrace.octrace.di.DatabaseProvider
import org.opencovidtrace.octrace.utils.DoAsync

class LogsViewModel : ViewModel() {

    private val database by DatabaseProvider()

    val logsLiveData = database.appDao().getLogsLiveData()

    fun removeOldContacts() {
        DoAsync { database.appDao().clearLogs() }
    }
}