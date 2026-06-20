package com.example.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class FootballFixturesResponse(
    @Json(name = "response") val response: List<FootballFixtureElement>?
)

@JsonClass(generateAdapter = true)
data class FootballFixtureElement(
    @Json(name = "fixture") val fixture: FootballFixture?,
    @Json(name = "league") val league: FootballLeague?,
    @Json(name = "teams") val teams: FootballTeams?,
    @Json(name = "goals") val goals: FootballGoals?
)

@JsonClass(generateAdapter = true)
data class FootballFixture(
    @Json(name = "id") val id: Int,
    @Json(name = "date") val date: String?,
    @Json(name = "status") val status: FootballFixtureStatus?
)

@JsonClass(generateAdapter = true)
data class FootballFixtureStatus(
    @Json(name = "long") val long: String?,
    @Json(name = "short") val short: String?, // ex: "FT" (Finished), "NS" (Not Started)
    @Json(name = "elapsed") val elapsed: Int?
)

@JsonClass(generateAdapter = true)
data class FootballLeague(
    @Json(name = "id") val id: Int,
    @Json(name = "name") val name: String,
    @Json(name = "logo") val logo: String?
)

@JsonClass(generateAdapter = true)
data class FootballTeams(
    @Json(name = "home") val home: FootballTeam?,
    @Json(name = "away") val away: FootballTeam?
)

@JsonClass(generateAdapter = true)
data class FootballTeam(
    @Json(name = "id") val id: Int,
    @Json(name = "name") val name: String,
    @Json(name = "logo") val logo: String?
)

@JsonClass(generateAdapter = true)
data class FootballGoals(
    @Json(name = "home") val home: Int?,
    @Json(name = "away") val away: Int?
)

interface FootballService {
    @GET("fixtures")
    suspend fun getFixtures(
        @Header("x-apisports-key") apiKey: String,
        @Query("team") teamId: Int,
        @Query("last") last: Int? = null,
        @Query("next") next: Int? = null
    ): FootballFixturesResponse

    companion object {
        private const val BASE_URL = "https://v3.football.api-sports.io/"

        fun create(): FootballService {
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.HEADERS
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

            return retrofit.create(FootballService::class.java)
        }
    }
}
