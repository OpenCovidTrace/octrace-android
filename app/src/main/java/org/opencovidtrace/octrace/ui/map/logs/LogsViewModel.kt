package org.opencovidtrace.octrace.ui.map.logs

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import org.opencovidtrace.octrace.data.LogTableValue
import org.opencovidtrace.octrace.di.DatabaseProvider

abstract class LogsViewModel<T : LogTableValue> : ViewModel() {

    protected val database by DatabaseProvider()

    abstract val logsLiveData: LiveData<List<T>>

    abstract fun removeOldContacts()

}
