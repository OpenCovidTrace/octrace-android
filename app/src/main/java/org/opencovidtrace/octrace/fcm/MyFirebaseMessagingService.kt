package org.opencovidtrace.octrace.fcm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import org.opencovidtrace.octrace.MainActivity
import org.opencovidtrace.octrace.R
import org.opencovidtrace.octrace.storage.EncryptionKeysManager
import org.opencovidtrace.octrace.storage.QrContact
import org.opencovidtrace.octrace.storage.QrContactsManager
import org.opencovidtrace.octrace.utils.CryptoUtil
import org.opencovidtrace.octrace.utils.CryptoUtil.base64DecodeByteArray
import org.opencovidtrace.octrace.utils.CryptoUtil.base64EncodedString


class MyFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        const val UPDATES_CHANNEL_ID = "UPDATES_CHANNEL_ID"
    }

    private lateinit var notificationManager: NotificationManager

    override fun onCreate() {
        super.onCreate()
        notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel =
                NotificationChannel(
                    UPDATES_CHANNEL_ID,
                    getString(R.string.updates_channel_name),
                    NotificationManager.IMPORTANCE_HIGH
                )
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        val data = remoteMessage.data

        if (data["type"] == "MAKE_CONTACT") {
            val secret = data["secret"]!!
            val tst = data["tst"]!!.toLong()

            EncryptionKeysManager.getEncryptionKeys()[tst]?.let { key ->
                val secretData = secret.base64DecodeByteArray()

                val rollingId =
                    CryptoUtil.decodeAES(secretData.sliceArray(0 until CryptoUtil.keyLength), key)
                val meta = CryptoUtil.decodeAES(
                    secretData.sliceArray(CryptoUtil.keyLength until CryptoUtil.keyLength * 2),
                    key
                )

                val contact = QrContact(rollingId.base64EncodedString(), meta.base64EncodedString())

                QrContactsManager.addContact(contact)

                // TODO dismiss QrLinkViewController
            }
        }
    }

    private fun info(message: String) {
        val intentOpen = Intent(this, MainActivity::class.java)
        val pendingIntentOpen = PendingIntent.getActivity(
            this,
            MainActivity.REQUEST_NONE,
            intentOpen,
            PendingIntent.FLAG_UPDATE_CURRENT
        )

        val soundUri =
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val builder = NotificationCompat.Builder(
            this,
            UPDATES_CHANNEL_ID
        )
            .setAutoCancel(true)
            // TODO .setSmallIcon(R.drawable.ic_icon_24dp)
            .setLargeIcon(BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher))
            .setContentTitle(getString(R.string.app_name))
            .setContentText(message)
            .setSound(soundUri)
            .setContentIntent(pendingIntentOpen)

        notificationManager.notify(0, builder.build())
    }

}

