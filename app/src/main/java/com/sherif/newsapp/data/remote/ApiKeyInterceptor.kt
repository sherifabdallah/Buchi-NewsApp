package com.sherif.newsapp.data.remote

import okhttp3.Interceptor
import okhttp3.Response

class ApiKeyInterceptor(private val apiKey: String) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request().newBuilder()
            .header(HEADER_NAME, apiKey)
            .build()
        return chain.proceed(request)
    }

    companion object {
        const val HEADER_NAME = "x-api-key"
    }
}
