package org.opencovidtrace.octrace.storage

import android.location.Location
import com.google.android.gms.maps.model.LatLng
import com.google.gson.Gson
import org.opencovidtrace.octrace.utils.CryptoUtil

object TrackingManager : PreferencesHolder("tracking") {

    private const val TRACKING_DATA = "trackingData"
    const val  trackingIntervalMs = 60 * 1000//60 sec

    fun getTrackingData(): List<TrackingPoint> {
        val storedHashMapString = OnboardingManager.getString(TRACKING_DATA)
        (Gson().fromJson(storedHashMapString) as? List<TrackingPoint>)?.let {
            return it
        } ?: kotlin.run { return arrayListOf() }
    }

    fun setTrackingData(newValue: List<TrackingPoint>) {
        val hashMapString = Gson().toJson(newValue)
        OnboardingManager.setString(TRACKING_DATA, hashMapString)
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

data class TrackingPoint(val lat: Double, val lng: Double, val tst: Long) {

    constructor(location: Location) : this(
        location.latitude,
        location.longitude,
        System.currentTimeMillis()
    )

    constructor(latLng: LatLng) : this(
        latLng.latitude,
        latLng.longitude,
        System.currentTimeMillis()
    )

    fun coordinate(): LatLng = LatLng(lat, lng)

    fun dayNumber() = CryptoUtil.getDayNumber(tst)

}
