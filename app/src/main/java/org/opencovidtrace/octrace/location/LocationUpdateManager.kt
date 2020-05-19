package org.opencovidtrace.octrace.location

import android.location.Location
import org.greenrobot.eventbus.EventBus
import org.opencovidtrace.octrace.data.UpdateLocationAccuracyEvent
import org.opencovidtrace.octrace.data.UpdateUserTracksEvent
import org.opencovidtrace.octrace.storage.*
import org.opencovidtrace.octrace.storage.TrackingManager.trackingIntervalMs

object LocationUpdateManager {

    private var lastLocation: Location? = null
    private var lastTrackingUpdate: Long = TrackingManager.getTrackingData().lastOrNull()?.tst ?: 0
    private val callbacks = mutableListOf<(Location) -> Unit>()

    fun updateLocation(location: Location) {
        lastLocation = location

        LocationBordersManager.updateLocationBorders(location)

        callbacks.forEach { callback -> callback(location) }
        callbacks.clear()

        if (EventBus.getDefault().hasSubscriberForEvent(UpdateLocationAccuracyEvent::class.java)) {
            EventBus.getDefault().post(UpdateLocationAccuracyEvent(location.accuracy.toInt()))
        }

        val now = System.currentTimeMillis()
        if (
            now - lastTrackingUpdate > trackingIntervalMs &&
            location.accuracy > 0 && location.accuracy < 30 &&
            UserSettingsManager.recordTrack
        ) {
            println("Updating tracking location")

            val point = TrackingPoint(location)

            TrackingManager.addTrackingPoint(point)

            if (EventBus.getDefault().hasSubscriberForEvent(UpdateUserTracksEvent::class.java)) {
                EventBus.getDefault().post(UpdateUserTracksEvent())
            }

            lastTrackingUpdate = now

            if (UserSettingsManager.uploadTrack) {
                TracksManager.uploadNewTracks()
            }
        }
    }

    fun registerCallback(callback: (Location) -> Unit) {
        if (lastLocation != null && System.currentTimeMillis() - lastTrackingUpdate < trackingIntervalMs) {
            callback(lastLocation!!)
        } else {
            callbacks.add(callback)
        }
    }

    fun getLastLocation() = lastLocation

}
