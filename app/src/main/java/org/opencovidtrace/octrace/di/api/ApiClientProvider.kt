package org.opencovidtrace.octrace.di.api

import okhttp3.ResponseBody
import org.opencovidtrace.octrace.BuildConfig.API_BASE_URL
import org.opencovidtrace.octrace.api.ApiClient
import org.opencovidtrace.octrace.api.ApiEndpoint
import org.opencovidtrace.octrace.di.IndependentProvider
import retrofit2.Converter
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.lang.reflect.Type


internal object ApiClientProvider : IndependentProvider<ApiClient>() {

    private val httpClient by HttpClientProvider()

    private const val STORAGE_ENDPOINT = "https://storage.$API_BASE_URL"

    override fun initInstance(): ApiClient = Retrofit.Builder()
        .baseUrl(STORAGE_ENDPOINT)
        .client(httpClient)
        .addConverterFactory(NullOnEmptyConverterFactory())
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(ApiEndpoint::class.java)
        .run { ApiClient(this) }

    class NullOnEmptyConverterFactory : Converter.Factory() {
        override fun responseBodyConverter(
            type: Type,
            annotations: Array<Annotation>,
            retrofit: Retrofit
        ): Converter<ResponseBody, *> {
            val delegate: Converter<ResponseBody, *> =
                retrofit.nextResponseBodyConverter<Any>(this, type, annotations)
            return Converter<ResponseBody, Any> { body ->
                if (body.contentLength() == 0L) return@Converter null
                try {
                    return@Converter delegate.convert(body)
                } catch (e: IOException) {
                    e.printStackTrace()
                    return@Converter null
                }
            }
        }
    }

}


