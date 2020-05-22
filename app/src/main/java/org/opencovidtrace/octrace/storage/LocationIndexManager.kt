package org.opencovidtrace.octrace.storage


import android.location.Location
import com.google.gson.Gson
import kotlin.math.roundToInt


object LocationIndexManager : PreferencesHolder("location-index") {

    private const val KEYS_INDEX = "keysIndex"
    private const val TRACKS_INDEX = "tracksIndex"

    fun getKeysIndex(): HashMap<LocationIndex, Long> {
        val storedHashMapString = OnboardingManager.getString(KEYS_INDEX)
        (objectMapper.fromJson(storedHashMapString) as? HashMap<LocationIndex, Long>)?.let {
            return it
        } ?: kotlin.run { return hashMapOf() }
    }

    fun setKeysIndex(newValue: HashMap<LocationIndex, Long>) {
        val hashMapString = objectMapper.toJson(newValue)
        OnboardingManager.setString(KEYS_INDEX, hashMapString)
    }

    fun updateKeysIndex(index: LocationIndex) {
        val newIndex = getKeysIndex()

        newIndex[index] = System.currentTimeMillis()

        setKeysIndex(newIndex)
    }

    fun getTracksIndex(): HashMap<LocationIndex, Long> {
        val storedHashMapString = OnboardingManager.getString(TRACKS_INDEX)
        (objectMapper.fromJson(storedHashMapString) as? HashMap<LocationIndex, Long>)?.let {
            return it
        } ?: kotlin.run { return hashMapOf() }
    }

    fun setTracksIndex(newValue: HashMap<LocationIndex, Long>) {
        val hashMapString = objectMapper.toJson(newValue)
        OnboardingManager.setString(TRACKS_INDEX, hashMapString)
    }

    fun updateTracksIndex(index: LocationIndex) {
        val newIndex = getTracksIndex()

        newIndex[index] = System.currentTimeMillis()

        setTracksIndex(newIndex)
    }
}


data class LocationIndex(val latIdx: Int, val lngIdx: Int) {

    companion object {
        const val diff = 0.25 // ~ 25km
        const val precision = 10.0 // ~ 10km square side per index
    }

    constructor(location: Location) : this(
        latIdx = (location.latitude * precision).roundToInt(),
        lngIdx = (location.longitude * precision).roundToInt()
    )

    override fun toString(): String {
        return Gson().toJson(this)
    }
}
