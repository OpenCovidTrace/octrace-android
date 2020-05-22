package org.opencovidtrace.octrace.ui.map.logs

import androidx.lifecycle.ViewModelProvider
import org.opencovidtrace.octrace.data.BtLogTableValue


class BtLogsFragment : LogsFragment<BtLogTableValue>() {

    override fun getLogsViewModel(): LogsViewModel<BtLogTableValue> {
        return ViewModelProvider(this).get(BtLogsViewModel::class.java)
    }

}
