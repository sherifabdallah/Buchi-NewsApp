package com.sherif.newsapp.data.remote

import com.sherif.newsapp.data.remote.dto.ArticlesResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface NewsApi {

    @GET("top-headlines")
    suspend fun getTopHeadlines(
        @Query("country") country: String,
        @Query("page") page: Int,
    ): ArticlesResponse

    @GET("top-headlines")
    suspend fun searchTopHeadlines(
        @Query("country") country: String,
        @Query("q") query: String,
    ): ArticlesResponse
}
