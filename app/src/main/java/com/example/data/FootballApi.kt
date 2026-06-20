package com.example.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class FDTeamsResponse(
    @Json(name = "teams") val teams: List<FDTeam>?
)

@JsonClass(generateAdapter = true)
data class FDTeam(
    @Json(name = "id") val id: Int,
    @Json(name = "name") val name: String,
    @Json(name = "shortName") val shortName: String?,
    @Json(name = "tla") val tla: String?,
    @Json(name = "crest") val crest: String?
)

@JsonClass(generateAdapter = true)
data class FDMatchesResponse(
    @Json(name = "matches") val matches: List<FDMatch>?
)

@JsonClass(generateAdapter = true)
data class FDMatch(
    @Json(name = "id") val id: Int,
    @Json(name = "utcDate") val utcDate: String?,
    @Json(name = "status") val status: String?, // "FINISHED", "SCHEDULED", "LIVE", "IN_PLAY", etc.
    @Json(name = "competition") val competition: FDMatchCompetition?,
    @Json(name = "homeTeam") val homeTeam: FDMatchTeam?,
    @Json(name = "awayTeam") val awayTeam: FDMatchTeam?,
    @Json(name = "score") val score: FDMatchScore?
)

@JsonClass(generateAdapter = true)
data class FDMatchCompetition(
    @Json(name = "id") val id: Int,
    @Json(name = "name") val name: String,
    @Json(name = "code") val code: String?
)

@JsonClass(generateAdapter = true)
data class FDMatchTeam(
    @Json(name = "id") val id: Int,
    @Json(name = "name") val name: String,
    @Json(name = "shortName") val shortName: String?,
    @Json(name = "crest") val crest: String?
)

@JsonClass(generateAdapter = true)
data class FDMatchScore(
    @Json(name = "winner") val winner: String?,
    @Json(name = "fullTime") val fullTime: FDScoreTime?
)

@JsonClass(generateAdapter = true)
data class FDScoreTime(
    @Json(name = "home") val home: Int?,
    @Json(name = "away") val away: Int?
)

interface FootballService {
    @GET("competitions/{competition}/teams")
    suspend fun getCompetitionTeams(
        @Header("X-Auth-Token") apiKey: String,
        @Path("competition") competitionCode: String
    ): FDTeamsResponse

    @GET("teams/{team}/matches")
    suspend fun getTeamMatches(
        @Header("X-Auth-Token") apiKey: String,
        @Path("team") teamId: Int,
        @Query("status") status: String? = null
    ): FDMatchesResponse

    companion object {
        private const val BASE_URL = "https://api.football-data.org/v4/"

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
