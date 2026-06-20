package com.example.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class OverpassResponse(
    @Json(name = "elements") val elements: List<OverpassElement>?
)

@JsonClass(generateAdapter = true)
data class OverpassElement(
    @Json(name = "id") val id: Long,
    @Json(name = "lat") val lat: Double,
    @Json(name = "lon") val lon: Double,
    @Json(name = "tags") val tags: OverpassTags?
)

@JsonClass(generateAdapter = true)
data class OverpassTags(
    @Json(name = "name") val name: String?,
    @Json(name = "operator") val operator: String?,
    @Json(name = "network") val network: String?,
    @Json(name = "line") val line: String?,
    @Json(name = "colour") val colour: String?
)

interface OverpassService {
    @GET("interpreter")
    suspend fun getStations(
        @Query("data") data: String
    ): OverpassResponse

    companion object {
        private const val BASE_URL = "https://overpass-api.de/api/"

        fun create(): OverpassService {
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.HEADERS
            }
            val okHttpClient = OkHttpClient.Builder()
                .addInterceptor(logging)
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build()

            val retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(MoshiConverterFactory.create())
                .build()

            return retrofit.create(OverpassService::class.java)
        }
    }
}
