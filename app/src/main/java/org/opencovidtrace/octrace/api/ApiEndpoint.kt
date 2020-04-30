package org.opencovidtrace.octrace.api

import org.opencovidtrace.octrace.storage.KeysManager
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST


interface ApiEndpoint {

    @POST("keys")
    fun sendKeys(@Body keysData: KeysManager.KeysData): Call<String>

}