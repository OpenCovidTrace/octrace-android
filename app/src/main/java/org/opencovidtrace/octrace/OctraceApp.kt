package org.opencovidtrace.octrace

import android.app.Application
import org.opencovidtrace.octrace.bluetooth.DeviceManager
import org.opencovidtrace.octrace.di.BluetoothManagerProvider
import org.opencovidtrace.octrace.di.ContextProvider

class OctraceApp : Application() {

    init {
        ContextProvider.inject { applicationContext }
        BluetoothManagerProvider.inject { DeviceManager(applicationContext) }
    }

}
