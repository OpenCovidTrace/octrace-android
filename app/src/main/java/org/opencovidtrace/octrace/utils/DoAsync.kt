package org.opencovidtrace.octrace.utils

import android.os.AsyncTask

class DoAsync(val handler: () -> Unit) : AsyncTask<Void, Void, Void>() {
    init {
        execute()
    }
    override fun doInBackground(vararg params: Void?): Void? {
        handler()
        return null
    }
}