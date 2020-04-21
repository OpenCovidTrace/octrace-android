package org.opencovidtrace.octrace

import android.Manifest
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.navigation.findNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.lang.Boolean
import java.util.*


class MainActivity : AppCompatActivity() {

    companion object Requests {
        val REQUEST_LOCATION = 1
    }

    private var mapLocationRequest: LocationRequest? = null
    private var mapLocationSettingsRequestBuilder: LocationSettingsRequest.Builder? = null
    private val mapLocationCallback: LocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            LocationUpdateManager.updateLocation(locationResult.lastLocation)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val navView: BottomNavigationView = findViewById(R.id.nav_view)

        val navController = findNavController(R.id.nav_host_fragment)
        navView.setupWithNavController(navController)

        mapLocationRequest = LocationRequest()
        mapLocationRequest!!.interval = 3000
        mapLocationRequest!!.fastestInterval = 1000
        mapLocationRequest!!.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        mapLocationSettingsRequestBuilder =
            LocationSettingsRequest.Builder().addLocationRequest(mapLocationRequest!!)

        if (LocationAccessManager.authorized(this)) {
            startMapUpdates()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                REQUEST_LOCATION
            )
        }
    }

    private fun startMapUpdates() {
        LocationAccessManager.addConsumer(
            this,
            mapLocationRequest!!,
            mapLocationCallback
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String?>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            REQUEST_LOCATION -> {
                startMapUpdates()
            }
        }
    }

}
