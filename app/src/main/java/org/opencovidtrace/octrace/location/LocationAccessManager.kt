package org.opencovidtrace.octrace.location

import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices

object LocationAccessManager {

    private var fusedLocationClient: FusedLocationProviderClient? = null

    @SuppressLint("MissingPermission")
    fun addConsumer(
        ctx: Context,
        locationRequest: LocationRequest,
        locationCallback: LocationCallback
    ) {
        if (fusedLocationClient == null) {
            init(ctx)
        }

        fusedLocationClient?.requestLocationUpdates(locationRequest, locationCallback, null)
    }

    fun removeConsumer(locationCallback: LocationCallback) {
            fusedLocationClient?.removeLocationUpdates(locationCallback)
    }

    @SuppressLint("MissingPermission")
    private fun init(ctx: Context) {
        LocationServices.getFusedLocationProviderClient(ctx).apply {
            fusedLocationClient=this
            lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    LocationUpdateManager.updateLocation(
                        location
                    )
                }
            }
        }
    }

    fun authorized(ctx: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            ctx,
            ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
            ctx,
            ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

}
