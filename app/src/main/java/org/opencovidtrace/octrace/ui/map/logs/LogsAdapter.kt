package org.opencovidtrace.octrace.ui.map.logs

import android.view.ViewGroup
import androidx.annotation.LayoutRes

import kotlinx.android.synthetic.main.list_item_log.view.*
import org.opencovidtrace.octrace.R
import org.opencovidtrace.octrace.data.LogTableValue
import org.opencovidtrace.octrace.ext.text.dateFullFormat
import org.opencovidtrace.octrace.ui.base.BaseAdapter
import org.opencovidtrace.octrace.ui.base.BaseViewHolder


class LogsAdapter : BaseAdapter<LogTableValue, LogsAdapter.ViewHolder>() {


    override fun getItemViewType(position: Int): Int {
        return R.layout.list_item_log
    }

    override fun newViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(viewType, parent)


    inner class ViewHolder(@LayoutRes layoutRes: Int, parent: ViewGroup) :
        BaseViewHolder<LogTableValue>(layoutRes, parent) {

        override fun updateView(item: LogTableValue) {
            with(itemView) {
                dateTimeTextView.text = item.time.dateFullFormat()
                logValueTextView.text = item.getLogValue()
            }
        }
    }


}



