package org.opencovidtrace.octrace.storage

import com.google.gson.Gson
import org.opencovidtrace.octrace.data.LocationIndex

object LocationIndexManager : PreferencesHolder("location-index") {

    private const val KEYS_INDEX = "keysIndex"
    private const val TRACKS_INDEX = "tracksIndex"

    fun getKeysIndex(): HashMap<LocationIndex, Long> {
        val storedHashMapString = KeyManager.getString(KEYS_INDEX)
        (Gson().fromJson(storedHashMapString) as? HashMap<LocationIndex, Long>)?.let {
            return it
        } ?: kotlin.run { return hashMapOf() }
    }

    fun setKeysIndex(newValue: HashMap<LocationIndex, Long>) {
        val hashMapString = Gson().toJson(newValue)
        KeyManager.setString(KEYS_INDEX, hashMapString)
    }

    fun updateKeysIndex(index: LocationIndex) {
        val newIndex = getKeysIndex()

        newIndex[index] = System.currentTimeMillis()

        setKeysIndex(newIndex)
    }

    fun getTracksIndex(): HashMap<LocationIndex, Long> {
        val storedHashMapString = KeyManager.getString(TRACKS_INDEX)
        (Gson().fromJson(storedHashMapString) as? HashMap<LocationIndex, Long>)?.let {
            return it
        } ?: kotlin.run { return hashMapOf() }
    }

    fun setTracksIndex(newValue: HashMap<LocationIndex, Long>) {
        val hashMapString = Gson().toJson(newValue)
        KeyManager.setString(TRACKS_INDEX, hashMapString)
    }

    fun updateTracksIndex(index: LocationIndex) {
        val newIndex = getTracksIndex()

        newIndex[index] = System.currentTimeMillis()

        setTracksIndex(newIndex)
    }
}