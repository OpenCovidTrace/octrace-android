package org.opencovidtrace.octrace

import android.app.Application
import com.google.firebase.FirebaseApp
import org.bouncycastle.util.io.pem.PemReader
import org.dpppt.android.sdk.DP3T
import org.dpppt.android.sdk.backend.models.ApplicationInfo
import org.opencovidtrace.octrace.bluetooth.DeviceManager
import org.opencovidtrace.octrace.di.BluetoothManagerProvider
import org.opencovidtrace.octrace.di.ContextProvider
import org.opencovidtrace.octrace.ext.data.insertDp3tLogs
import org.opencovidtrace.octrace.utils.CryptoUtil.base64DecodeString
import java.io.StringReader
import java.security.KeyFactory
import java.security.spec.X509EncodedKeySpec

class OctraceApp : Application() {

    companion object {
        const val API_HOST = "dev.opencovidtrace.org"
    }

    init {
        ContextProvider.inject { applicationContext }
        BluetoothManagerProvider.inject { DeviceManager(applicationContext) }
    }

    override fun onCreate() {
        super.onCreate()

        FirebaseApp.initializeApp(applicationContext)

        val publicKey =
            "LS0tLS1CRUdJTiBQVUJMSUMgS0VZLS0tLS0KTUZrd0V3WUhLb1pJemowQ0FRWUlLb1pJemowREFRY0RRZ0FFUzF2ejdNdE1hejArQkppWUd6MUhGd0dLRHd6RgpuQ2psQ3R5dUhUWEtjVkE0WlBxa3JDczNadFBTZXNwUDJDVk5DdSsrcExmdGdKeEZLSzl0UTFyQW1RPT0KLS0tLS1FTkQgUFVCTElDIEtFWS0tLS0tCg==".base64DecodeString()
        val reader = StringReader(publicKey)
        val readerPem = PemReader(reader)
        val obj = readerPem.readPemObject()
        readerPem.close()
        val signaturePublicKey =
            KeyFactory.getInstance("EC").generatePublic(X509EncodedKeySpec(obj.content))

        DP3T.init(
            applicationContext,
            ApplicationInfo(
                BuildConfig.APPLICATION_ID,
                "https://dp3t.$API_HOST/",
                "https://dp3t.$API_HOST/"
            ),
            signaturePublicKey
        )

        insertDp3tLogs("Library initialized")
    }

}
