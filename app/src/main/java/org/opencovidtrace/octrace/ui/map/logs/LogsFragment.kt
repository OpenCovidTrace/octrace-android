package org.opencovidtrace.octrace.ui.map.logs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import com.andrefrsousa.superbottomsheet.SuperBottomSheetFragment
import kotlinx.android.synthetic.main.fragment_logs.*
import org.opencovidtrace.octrace.R
import org.opencovidtrace.octrace.data.LogTableValue
import org.opencovidtrace.octrace.ext.ui.confirm


abstract class LogsFragment<T : LogTableValue> : SuperBottomSheetFragment() {

    private lateinit var logsViewModel: LogsViewModel<T>
    private val logsAdapter = LogsAdapter()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        logsViewModel = getLogsViewModel()
        return inflater.inflate(R.layout.fragment_logs, container, false)
    }

    override fun getCornerRadius() = resources.getDimension(R.dimen.sheet_rounded_corner)

    override fun animateCornerRadius() = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        logRecyclerView.adapter = logsAdapter
        logsViewModel.logsLiveData.observe(this, Observer { logs ->
            logsAdapter.setItems(logs)
        })
        closeImageButton.setOnClickListener { dismiss() }
        clearImageButton.setOnClickListener {
            confirm(R.string.clear_logs) {
                logsViewModel.removeOldContacts()
            }
        }
    }

    abstract fun getLogsViewModel(): LogsViewModel<T>

}
