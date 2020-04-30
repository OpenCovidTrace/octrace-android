package org.opencovidtrace.octrace.api

import org.opencovidtrace.octrace.data.KeysData
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST


interface ApiEndpoint {

    @POST("keys")
    fun sendKeys(@Body keysData: KeysData): Call<String>

}