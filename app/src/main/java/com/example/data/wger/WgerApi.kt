package com.example.data.wger

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

interface WgerApi {

    @GET("exerciseinfo/")
    suspend fun getExerciseInfo(
        @Query("language") language: Int = 2, // 2 = English
        @Query("limit") limit: Int = 100,
        @Query("offset") offset: Int = 0,
        @Query("category") category: Int? = null
    ): WgerPaginatedResponse<WgerExerciseInfoDto>

    @GET("exercisecategory/")
    suspend fun getCategories(): WgerPaginatedResponse<WgerCategoryDto>

    @GET("muscle/")
    suspend fun getMuscles(): WgerPaginatedResponse<WgerMuscleDto>

    @GET("equipment/")
    suspend fun getEquipment(): WgerPaginatedResponse<WgerEquipmentDto>

    companion object {
        private const val BASE_URL = "https://wger.de/api/v2/"

        fun create(): WgerApi {
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            }

            val client = OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .addInterceptor(logging)
                .addInterceptor { chain ->
                    val request = chain.request().newBuilder()
                        .header("Accept", "application/json")
                        .header("User-Agent", "TesseraGym/2.0")
                        .build()
                    chain.proceed(request)
                }
                .build()

            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(MoshiConverterFactory.create())
                .build()
                .create(WgerApi::class.java)
        }
    }
}
