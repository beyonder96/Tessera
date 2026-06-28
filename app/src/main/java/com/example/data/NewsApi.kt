package com.example.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class NewsResponse(
    @Json(name = "status") val status: String,
    @Json(name = "totalResults") val totalResults: Int?,
    @Json(name = "articles") val articles: List<NewsArticle>?
)

@JsonClass(generateAdapter = true)
data class NewsArticle(
    @Json(name = "title") val title: String?,
    @Json(name = "description") val description: String?,
    @Json(name = "url") val url: String?,
    @Json(name = "urlToImage") val urlToImage: String?,
    @Json(name = "publishedAt") val publishedAt: String?,
    @Json(name = "source") val source: NewsSource?
)

@JsonClass(generateAdapter = true)
data class NewsSource(
    @Json(name = "name") val name: String?
)

interface NewsService {
    @GET("v2/top-headlines")
    suspend fun getTopHeadlines(
        @Query("country") country: String = "br",
        @Query("apiKey") apiKey: String
    ): Response<NewsResponse>

    companion object {
        private const val BASE_URL = "https://newsapi.org/"

        fun create(): NewsService {
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            }
            val okHttpClient = OkHttpClient.Builder()
                .addInterceptor(logging)
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .build()

            val retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(MoshiConverterFactory.create())
                .build()

            return retrofit.create(NewsService::class.java)
        }
    }
}
