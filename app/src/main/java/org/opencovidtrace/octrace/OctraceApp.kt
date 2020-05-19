package org.opencovidtrace.octrace

import android.app.Application
import com.google.firebase.FirebaseApp
import org.opencovidtrace.octrace.bluetooth.DeviceManager
import org.opencovidtrace.octrace.di.BluetoothManagerProvider
import org.opencovidtrace.octrace.di.ContextProvider

class OctraceApp : Application() {

    companion object {
        const val API_HOST = "dev.openexposuretrace.org"
    }

    init {
        ContextProvider.inject { applicationContext }
        BluetoothManagerProvider.inject { DeviceManager(applicationContext) }
    }

    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(applicationContext)
    }

}
