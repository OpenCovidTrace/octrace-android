package org.opencovidtrace.octrace

import android.Manifest.permission
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentSender.SendIntentException
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Bundle
import android.os.IBinder
import android.widget.Toast
import android.widget.Toast.LENGTH_LONG
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.navigation.findNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.material.bottomnavigation.BottomNavigationView
import org.opencovidtrace.octrace.OnboardingActivity.Extra.STAGE_EXTRA
import org.opencovidtrace.octrace.data.Enums
import org.opencovidtrace.octrace.data.Enums.*
import org.opencovidtrace.octrace.di.BluetoothManagerProvider
import org.opencovidtrace.octrace.ext.access.withPermissions
import org.opencovidtrace.octrace.location.LocationAccessManager
import org.opencovidtrace.octrace.service.BleUpdatesService
import org.opencovidtrace.octrace.service.TrackingService
import org.opencovidtrace.octrace.storage.BtContactsManager
import org.opencovidtrace.octrace.storage.KeyManager

class MainActivity : AppCompatActivity() {

    companion object {
        const val REQUEST_NONE = 0
        const val REQUEST_LOCATION = 1
        const val REQUEST_CHECK_TRACKING_SETTINGS = 2
        private const val REQUEST_BLUETOOTH = 3
    }

    private var bleUpdatesService: BleUpdatesService? = null

    // Tracks the bound state of the service.
    private var bound = false
    private val deviceManager by BluetoothManagerProvider()
    private var needStartBleService = false

    // Monitors the state of the connection to the service.
    private val serviceConnection: ServiceConnection =
        object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName, service: IBinder) {
                val binder: BleUpdatesService.LocalBinder = service as BleUpdatesService.LocalBinder
                bleUpdatesService = binder.service
                bound = true
                if (needStartBleService)
                    this@MainActivity.startBleService()
            }

            override fun onServiceDisconnected(name: ComponentName) {
                bleUpdatesService = null
                bound = false
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val navView: BottomNavigationView = findViewById(R.id.nav_view)

        val navController = findNavController(R.id.nav_host_fragment)
        navView.setupWithNavController(navController)

        if (KeyManager.hasKey()) {

            BtContactsManager.removeOldContacts()

        } else {
            val intent = Intent(this, OnboardingActivity::class.java)

            intent.putExtra(STAGE_EXTRA, OnboardingStage.WELCOME)

            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()

        if (KeyManager.hasKey()) {
            requestEnableTracking()
        }
    }

    override fun onStart() {
        super.onStart()
        bindService(
            Intent(this, BleUpdatesService::class.java), serviceConnection,
            Context.BIND_AUTO_CREATE
        )
    }

    override fun onStop() {
        if (bound) {
            unbindService(serviceConnection)
            bound = false
        }
        super.onStop()
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
                    startSearchDevices()
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == REQUEST_BLUETOOTH)
                startBleService()
        }
    }

    private fun enableTracking() {
        if (LocationAccessManager.authorized(this)) {
            startTrackingService()
            if (bleUpdatesService != null)
                startBleService()
            else
                needStartBleService = true
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

    private fun startSearchDevices() =
        withPermissions(arrayOf(permission.ACCESS_COARSE_LOCATION), REQUEST_LOCATION) {
            val locationManager =
                getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            val gpsEnabled =
                locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER)
                    ?: false
            if (gpsEnabled) {
                when (checkBluetooth()) {
                    ENABLED -> bleUpdatesService?.requestBleUpdates()
                    DISABLED -> showBluetoothDisabledError()
                    NOT_FOUND -> showBluetoothNotFoundError()
                }
            }
        }

    private fun checkBluetooth(): Enums = deviceManager.checkBluetooth()

    private fun startBleService() {
        when (checkBluetooth()) {
            ENABLED -> {
                bleUpdatesService?.startAdvertising()
                startSearchDevices()
            }
            DISABLED -> showBluetoothDisabledError()
            NOT_FOUND -> showBluetoothNotFoundError()
        }
    }

    private fun showBluetoothDisabledError() {
        AlertDialog.Builder(this).apply {
            setTitle(R.string.bluetooth_turn_off)
            setMessage(R.string.bluetooth_turn_off_description)
            setCancelable(false)
            setPositiveButton(R.string.enable) { _, _ ->
                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                startActivityForResult(enableBtIntent, REQUEST_BLUETOOTH)
            }
            show()
        }
    }

    private fun showBluetoothNotFoundError() {
        AlertDialog.Builder(this).apply {
            setTitle(R.string.bluetooth_do_not_support)
            setMessage(R.string.bluetooth_do_not_support_description)
            setCancelable(false)
            setNegativeButton(R.string.done) { _, _ -> }
            show()
        }
    }


}
