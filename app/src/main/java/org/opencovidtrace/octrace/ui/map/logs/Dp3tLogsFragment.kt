package org.opencovidtrace.octrace.ui.map.logs

import androidx.lifecycle.ViewModelProvider
import org.opencovidtrace.octrace.data.Dp3tLogTableValue


class Dp3tLogsFragment : LogsFragment<Dp3tLogTableValue>() {

    override fun getLogsViewModel(): LogsViewModel<Dp3tLogTableValue> {
        return ViewModelProvider(this).get(Dp3tLogsViewModel::class.java)
    }

}
