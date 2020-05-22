package org.opencovidtrace.octrace.ui.map.logs

import org.opencovidtrace.octrace.data.BtLogTableValue
import org.opencovidtrace.octrace.utils.DoAsync

class BtLogsViewModel : LogsViewModel<BtLogTableValue>() {

    override val logsLiveData = database.appDao().getBtLogsLiveData()

    override fun removeOldContacts() {
        DoAsync { database.appDao().clearBtLogs() }
    }

}
