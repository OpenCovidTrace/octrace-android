package org.opencovidtrace.octrace.di.api

import org.opencovidtrace.octrace.BuildConfig.API_BASE_URL
import org.opencovidtrace.octrace.api.ContactsApiClient
import org.opencovidtrace.octrace.api.ContactsApiEndpoint
import org.opencovidtrace.octrace.di.IndependentProvider
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

internal object ContactsApiClientProvider : IndependentProvider<ContactsApiClient>() {

    private val httpClient by HttpClientProvider()

    const val CONTACTS_ENDPOINT = "https://contact.$API_BASE_URL"

    override fun initInstance(): ContactsApiClient = Retrofit.Builder()
        .baseUrl(CONTACTS_ENDPOINT)
        .client(httpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(ContactsApiEndpoint::class.java)
        .run { ContactsApiClient(this) }

}


