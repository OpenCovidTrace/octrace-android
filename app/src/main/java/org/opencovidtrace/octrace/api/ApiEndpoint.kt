package org.opencovidtrace.octrace.api

import org.opencovidtrace.octrace.storage.KeysData
import org.opencovidtrace.octrace.storage.TracksData
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query


interface ApiEndpoint {

    @POST("keys")
    fun sendKeys(@Body keysData: KeysData): Call<String>

    @GET("keys")
    fun fetchKeys(
        @Query("lastUpdateTimestamp") lastUpdateTimestamp: Long,
        @Query("minLat") minLat: Double,
        @Query("maxLat") maxLat: Double,
        @Query("minLng") minLng: Double,
        @Query("maxLng") maxLng: Double
    ): Call<KeysData>

    @POST("tracks")
    fun sendTracks(@Body tracksData: TracksData): Call<String>

    @GET("tracks")
    fun fetchTracks(
        @Query("lastUpdateTimestamp") lastUpdateTimestamp: Long,
        @Query("minLat") minLat: Double,
        @Query("maxLat") maxLat: Double,
        @Query("minLng") minLng: Double,
        @Query("maxLng") maxLng: Double
    ): Call<TracksData>

}
