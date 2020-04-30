package org.opencovidtrace.octrace.di.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.opencovidtrace.octrace.BuildConfig
import org.opencovidtrace.octrace.di.IndependentProvider
import java.util.concurrent.TimeUnit


internal object HttpClientProvider : IndependentProvider<OkHttpClient>() {

    private const val TIMEOUT = 60L

    private val loggingInterceptor: HttpLoggingInterceptor =
        HttpLoggingInterceptor { message -> println(message) }.apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }

    override fun initInstance(): OkHttpClient {
        return OkHttpClient.Builder()
            .readTimeout(TIMEOUT, TimeUnit.SECONDS)
            .writeTimeout(TIMEOUT, TimeUnit.SECONDS)
            .addNetworkInterceptor(loggingInterceptor)
            .build()
    }


}
