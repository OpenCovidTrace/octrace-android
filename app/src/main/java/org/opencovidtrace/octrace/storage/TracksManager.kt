package org.opencovidtrace.octrace.storage

import com.google.gson.Gson
import org.opencovidtrace.octrace.data.Track
import org.opencovidtrace.octrace.data.TracksData
import org.opencovidtrace.octrace.di.api.ApiClientProvider
import org.opencovidtrace.octrace.utils.CryptoUtil
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

object TracksManager : PreferencesHolder("tracks") {

    private const val TRACKS = "tracks"
    private const val TRACKS_LAST_UPLOAD = "tracks-last-upload"

    private val apiClient by ApiClientProvider()

    fun getTracks(): List<Track> {
        val storedHashMapString = KeyManager.getString(TRACKS)
        (Gson().fromJson(storedHashMapString) as? List<Track>)?.let {
            return it
        } ?: kotlin.run { return arrayListOf() }
    }

    fun setTracks(newValue: List<Track>) {
        val hashMapString = Gson().toJson(newValue)
        KeyManager.setString(TRACKS, hashMapString)
    }

    fun removeOldTracks() {
        val lastDay = CryptoUtil.currentDayNumber() - 14

        val newTracks = getTracks().filter { it.day > lastDay }

        setTracks(newTracks)
    }

    fun addTracks(tracks: List<Track>) {
        val newTracks: LinkedHashSet<Track> = LinkedHashSet(getTracks())

        newTracks.addAll(tracks)

        setTracks(newTracks.toList())
    }

    fun getLastUploadTimestamp(): Long {
        return getLong(TRACKS_LAST_UPLOAD)
    }

    fun setLastUploadTimestamp(value: Long) {
        setLong(TRACKS_LAST_UPLOAD, value)
    }

    fun uploadNewTracks() {
        val oldLastUploadTimestamp = getLastUploadTimestamp()
        val now = System.currentTimeMillis()

        val points = TrackingManager.getTrackingData().filter {
            it.tst > oldLastUploadTimestamp
        }

        val tracksByDay: HashMap<Int, Track> = hashMapOf()

        points.forEach { point ->
            val dayNumber = point.dayNumber()

            tracksByDay[dayNumber]?.points?.add(point) ?: kotlin.run {
                val (dailyKey, _) = CryptoUtil.getDailyKeys(dayNumber)
                val secretKey = CryptoUtil.toSecretKey(dailyKey)

                tracksByDay[dayNumber] = Track(arrayListOf(point), dayNumber, secretKey)
            }
        }

        val tracksData = TracksData(tracks = tracksByDay.values.toList())

        apiClient.sendTracks(tracksData)
            .enqueue(object : Callback<String> {

                override fun onResponse(call: Call<String>, response: Response<String>) {
                    setLastUploadTimestamp(now)
                }

                override fun onFailure(call: Call<String>, t: Throwable) {
                    println("ERROR: ${t.message}")
                }

            })
    }

}