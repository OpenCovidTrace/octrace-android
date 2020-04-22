package org.opencovidtrace.octrace

import android.Manifest.permission
import android.content.Intent
import android.content.IntentSender.SendIntentException
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import android.widget.Toast.LENGTH_LONG
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.navigation.findNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.material.bottomnavigation.BottomNavigationView
import org.opencovidtrace.octrace.OnboardingActivity.Extra.STAGE_EXTRA
import org.opencovidtrace.octrace.storage.KeyManager


class MainActivity : AppCompatActivity() {

    companion object {
        const val REQUEST_NONE = 0
        const val REQUEST_LOCATION = 1
        const val REQUEST_CHECK_TRACKING_SETTINGS = 2
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val navView: BottomNavigationView = findViewById(R.id.nav_view)

        val navController = findNavController(R.id.nav_host_fragment)
        navView.setupWithNavController(navController)

        if (KeyManager.hasKey(this)) {
            // TODO start bluetooth service here as well
        } else {
            val intent = Intent(this, OnboardingActivity::class.java)

            intent.putExtra(STAGE_EXTRA, OnboardingStage.WELCOME)

            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()

        if (KeyManager.hasKey(this)) {
            requestEnableTracking()
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        stopTrackingService()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String?>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            REQUEST_LOCATION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startTrackingService()
                }
            }
        }
    }

    private fun enableTracking() {
        if (LocationAccessManager.authorized(this)) {
            startTrackingService()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(permission.ACCESS_FINE_LOCATION, permission.ACCESS_BACKGROUND_LOCATION),
                REQUEST_LOCATION
            )
        }
    }

    private fun requestEnableTracking() {
        checkLocationSettings(
            TrackingService.TRACKING_LOCATION_REQUEST_BUILDER,
            REQUEST_CHECK_TRACKING_SETTINGS,
            Runnable { this.enableTracking() },
            Runnable {
                Toast.makeText(this, R.string.location_disabled, LENGTH_LONG).show()
            }
        )
    }

    /**
     * This method is about location usage device-wide, not permission of the app!
     */
    private fun checkLocationSettings(
        requestBuilder: LocationSettingsRequest.Builder,
        request: Int,
        onSuccess: Runnable,
        onFailure: Runnable?
    ) {
        val client = LocationServices.getSettingsClient(this)
        val task =
            client.checkLocationSettings(requestBuilder.build())
        task.addOnSuccessListener(this) { onSuccess.run() }
        task.addOnFailureListener(this) { e ->
            if (e is ResolvableApiException) {
                // StaticLocation settings are not satisfied, but this can be fixed
                // by showing the user a dialog.
                try {
                    // Show the dialog by calling startResolutionForResult(),
                    // and check the result in onActivityResult().
                    e.startResolutionForResult(this@MainActivity, request)
                } catch (sendEx: SendIntentException) {
                    // Ignore the error.
                }
            } else {
                onFailure?.run()
            }
        }
    }

    private fun startTrackingService() {
        startService(Intent(this, TrackingService::class.java))
    }

    private fun stopTrackingService() {
        stopService(Intent(this, TrackingService::class.java))
    }

}
