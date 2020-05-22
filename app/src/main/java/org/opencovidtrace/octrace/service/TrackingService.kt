package org.opencovidtrace.octrace.service

import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationSettingsRequest
import org.opencovidtrace.octrace.MainActivity
import org.opencovidtrace.octrace.R
import org.opencovidtrace.octrace.location.LocationAccessManager
import org.opencovidtrace.octrace.location.LocationUpdateManager
import org.opencovidtrace.octrace.storage.UserSettingsManager

class TrackingService : Service() {

    companion object {
        private const val BACKGROUND_CHANNEL_ID = "SILENT_CHANNEL_LOCATION"
        private const val NOTIFICATION_ID = 2

        val TRACKING_LOCATION_REQUEST = LocationRequest()

        val TRACKING_LOCATION_REQUEST_BUILDER: LocationSettingsRequest.Builder =
            LocationSettingsRequest.Builder().addLocationRequest(TRACKING_LOCATION_REQUEST)

        const val TAG = "TRACKING"

        init {
            TRACKING_LOCATION_REQUEST.maxWaitTime = 5000
            TRACKING_LOCATION_REQUEST.interval = 3000
            TRACKING_LOCATION_REQUEST.fastestInterval = 1000
            TRACKING_LOCATION_REQUEST.priority = LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY
        }
    }

    private val trackingLocationCallback: LocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            val location = locationResult.lastLocation
            if (location != null) {
                LocationUpdateManager.updateLocation(location)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i(TAG, "Start Command")

        var foreground = false

        if (UserSettingsManager.recordTrack) {
            if (LocationAccessManager.authorized(this)) {
                LocationAccessManager.addConsumer(
                    this,
                    TRACKING_LOCATION_REQUEST,
                    trackingLocationCallback
                )

                foreground = true
                Log.i(TAG, "Tracking enabled")
            } else {
                Log.w(TAG, "Failed to request tracking location updates")
            }
        } else {
            stopTrackingUpdates()

            Log.i(TAG, "Tracking is off")
        }

        if (foreground) {
            startForeground()
        } else {
            stopForeground(true)
        }

        super.onStartCommand(intent, flags, startId)

        return START_STICKY
    }

    private fun startForeground() {
        val openMainIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            MainActivity.REQUEST_NONE,
            openMainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder =
            NotificationCompat.Builder(this, BACKGROUND_CHANNEL_ID)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setContentText(getString(R.string.tracking_active))
                .setSmallIcon(R.drawable.ic_near_me_black_24dp)

        startForeground(NOTIFICATION_ID, builder.build())
    }

    override fun onCreate() {
        Log.i(TAG, "onCreate")
        super.onCreate()
    }

    override fun onDestroy() {
        Log.i(TAG, "onDestroy")
        stopTrackingUpdates()
        super.onDestroy()
    }

    private fun stopTrackingUpdates() {
        LocationAccessManager.removeConsumer(trackingLocationCallback)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

}
