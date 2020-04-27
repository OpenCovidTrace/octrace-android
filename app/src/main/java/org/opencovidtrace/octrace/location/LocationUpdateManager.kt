package org.opencovidtrace.octrace.location

import android.location.Location

object LocationUpdateManager {

    private var lastLocation: Location? = null
    private var lastLocationUpdate: Long = 0
    private val callbacks = mutableListOf<(Location) -> Unit>()

    /**
     * To be used only on UI thread
     */
    fun updateLocation(location: Location) {
        lastLocation = location
        lastLocationUpdate = System.currentTimeMillis()

        for (callback in callbacks) {
            callback(location)
        }

        callbacks.clear()
    }

    /**
     * To be used only on UI thread
     */
    fun registerCallback(callback: (Location) -> Unit) {
        if (lastLocation != null && System.currentTimeMillis() - lastLocationUpdate < 60000) {
            callback(lastLocation!!)
        } else {
            callbacks.add(callback)
        }
    }

    fun getLastLocation() = lastLocation

}
