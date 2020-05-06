package org.opencovidtrace.octrace.ui.base

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.recyclerview.widget.RecyclerView


open class BaseViewHolder<in T>(@LayoutRes layoutRes: Int, parent: ViewGroup) :
    RecyclerView.ViewHolder(LayoutInflater.from(parent.context).inflate(layoutRes, parent, false)) {


    fun getContext(): Context {
        return itemView.context
    }

    open fun updateView(item: T) {

    }

}