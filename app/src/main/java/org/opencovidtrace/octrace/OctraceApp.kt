package org.opencovidtrace.octrace

import android.app.Application
import com.google.firebase.FirebaseApp
import org.dpppt.android.sdk.DP3T
import org.dpppt.android.sdk.backend.models.ApplicationInfo
import org.opencovidtrace.octrace.bluetooth.DeviceManager
import org.opencovidtrace.octrace.di.BluetoothManagerProvider
import org.opencovidtrace.octrace.di.ContextProvider
import org.opencovidtrace.octrace.ext.data.insertDp3tLogs

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

        DP3T.init(
            applicationContext,
            ApplicationInfo(
                BuildConfig.APPLICATION_ID,
                "https://demo.dpppt.org/",
                "https://demo.dpppt.org/"
            ),
            null
        )

        insertDp3tLogs("Library initialized")
    }

}
