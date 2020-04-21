package org.opencovidtrace.octrace.ui.status

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class StatusViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "Hello, my status!"
    }
    val text: LiveData<String> = _text
}