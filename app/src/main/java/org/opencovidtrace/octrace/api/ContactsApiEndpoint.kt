package org.opencovidtrace.octrace.api

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST


interface ContactsApiEndpoint {

    @POST("makeContact")
    fun sendContactRequest(@Body contactRequest: ContactRequest): Call<Void>

}


data class ContactRequest(
    val token: String,
    val platform: String,
    val secret: String,
    val tst: Long
)
