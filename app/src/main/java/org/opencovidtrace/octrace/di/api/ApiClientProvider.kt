package org.opencovidtrace.octrace.di.api

import org.opencovidtrace.octrace.BuildConfig.API_BASE_URL
import org.opencovidtrace.octrace.api.ApiClient
import org.opencovidtrace.octrace.api.ApiEndpoint
import org.opencovidtrace.octrace.di.IndependentProvider
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

internal object ApiClientProvider : IndependentProvider<ApiClient>() {

    private val httpClient by HttpClientProvider()

    private const val STORAGE_ENDPOINT = "https://storage.$API_BASE_URL/"

    override fun initInstance(): ApiClient = Retrofit.Builder()
        .baseUrl(STORAGE_ENDPOINT)
        .client(httpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(ApiEndpoint::class.java)
        .run { ApiClient(this) }

}


