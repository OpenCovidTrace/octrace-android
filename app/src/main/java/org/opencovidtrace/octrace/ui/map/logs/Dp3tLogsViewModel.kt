package org.opencovidtrace.octrace.ui.map.logs

import org.opencovidtrace.octrace.data.Dp3tLogTableValue
import org.opencovidtrace.octrace.utils.DoAsync

class Dp3tLogsViewModel : LogsViewModel<Dp3tLogTableValue>() {

    override val logsLiveData = database.appDao().getDp3tLogsLiveData()

    override fun removeOldContacts() {
        DoAsync { database.appDao().clearDp3tLogs() }
    }

}
