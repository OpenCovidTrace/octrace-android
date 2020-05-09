package org.opencovidtrace.octrace.storage

import com.google.gson.Gson
import org.opencovidtrace.octrace.data.TrackingPoint

object TrackingManager : PreferencesHolder("tracking") {

    private const val TRACKING_DATA = "trackingData"
    const val  trackingIntervalMs = 60 * 1000//60 sec

    fun getTrackingData(): List<TrackingPoint> {
        val storedHashMapString = KeyManager.getString(TRACKING_DATA)
        (Gson().fromJson(storedHashMapString) as? List<TrackingPoint>)?.let {
            return it
        } ?: kotlin.run { return arrayListOf() }
    }

    fun setTrackingData(newValue: List<TrackingPoint>) {
        val hashMapString = Gson().toJson(newValue)
        KeyManager.setString(TRACKING_DATA, hashMapString)
    }

    fun addTrackingPoint(point: TrackingPoint) {
        val newTracks = getTrackingData().toMutableList()

        newTracks.add(point)

        setTrackingData(newTracks)
    }

    fun removeOldPoints() {
        val expirationTimestamp = DataManager.expirationTimestamp()

        val newTrackingData = getTrackingData().filter { it.tst > expirationTimestamp }

        setTrackingData(newTrackingData)
    }

}