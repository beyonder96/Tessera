package com.example.data.apifootball

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import android.util.Log

@JsonClass(generateAdapter = true)
data class ApiFootballResponse<T>(
    @Json(name = "response") val response: List<T>
)

@JsonClass(generateAdapter = true)
data class FixtureData(
    @Json(name = "fixture") val fixture: FixtureInfo,
    @Json(name = "league") val league: LeagueInfo,
    @Json(name = "teams") val teams: TeamsInfo,
    @Json(name = "goals") val goals: GoalsInfo,
    @Json(name = "events") val events: List<EventInfo>?,
    @Json(name = "lineups") val lineups: List<LineupInfo>?
)

@JsonClass(generateAdapter = true)
data class FixtureInfo(
    @Json(name = "id") val id: Long,
    @Json(name = "date") val date: String,
    @Json(name = "status") val status: StatusInfo,
    @Json(name = "venue") val venue: VenueInfo?
)

@JsonClass(generateAdapter = true)
data class StatusInfo(
    @Json(name = "short") val short: String
)

@JsonClass(generateAdapter = true)
data class VenueInfo(
    @Json(name = "name") val name: String?
)

@JsonClass(generateAdapter = true)
data class LeagueInfo(
    @Json(name = "name") val name: String
)

@JsonClass(generateAdapter = true)
data class TeamsInfo(
    @Json(name = "home") val home: TeamInfo,
    @Json(name = "away") val away: TeamInfo
)

@JsonClass(generateAdapter = true)
data class TeamInfo(
    @Json(name = "id") val id: Long,
    @Json(name = "name") val name: String,
    @Json(name = "logo") val logo: String?
)

@JsonClass(generateAdapter = true)
data class GoalsInfo(
    @Json(name = "home") val home: Int?,
    @Json(name = "away") val away: Int?
)

@JsonClass(generateAdapter = true)
data class EventInfo(
    @Json(name = "time") val time: EventTime,
    @Json(name = "team") val team: TeamInfo,
    @Json(name = "player") val player: PlayerInfo?,
    @Json(name = "type") val type: String,
    @Json(name = "detail") val detail: String?
)

@JsonClass(generateAdapter = true)
data class EventTime(
    @Json(name = "elapsed") val elapsed: Int
)

@JsonClass(generateAdapter = true)
data class PlayerInfo(
    @Json(name = "id") val id: Long?,
    @Json(name = "name") val name: String?
)

@JsonClass(generateAdapter = true)
data class LineupInfo(
    @Json(name = "team") val team: TeamInfo,
    @Json(name = "startXI") val startXI: List<LineupPlayerWrapper>?
)

@JsonClass(generateAdapter = true)
data class LineupPlayerWrapper(
    @Json(name = "player") val player: LineupPlayer
)

@JsonClass(generateAdapter = true)
data class LineupPlayer(
    @Json(name = "id") val id: Long?,
    @Json(name = "name") val name: String,
    @Json(name = "pos") val pos: String?
)

interface ApiFootballService {
    @GET("fixtures")
    suspend fun getFixturesByDate(
        @Header("x-apisports-key") apiKey: String,
        @Query("date") date: String,
        @Query("timezone") timezone: String = "America/Sao_Paulo"
    ): retrofit2.Response<ApiFootballResponse<FixtureData>>

    @GET("fixtures")
    suspend fun getFixtureById(
        @Header("x-apisports-key") apiKey: String,
        @Query("id") id: Long,
        @Query("timezone") timezone: String = "America/Sao_Paulo"
    ): retrofit2.Response<ApiFootballResponse<FixtureData>>
}

class ApiFootballRepository(
    private val api: ApiFootballService,
    private val apiKey: String
) {
    private val mutex = Mutex()
    private var cachedFixture: FixtureData? = null
    private var lastFetchTime = 0L
    private val cacheDurationMillis = 5 * 60 * 1000 // 5 minutos

    suspend fun getFixture(fixtureId: Long): Result<FixtureData> {
        val currentTime = System.currentTimeMillis()
        
        return mutex.withLock {
            if (cachedFixture != null && cachedFixture!!.fixture.id == fixtureId && (currentTime - lastFetchTime) < cacheDurationMillis) {
                Log.i("ApiFootballRepo", "Retornando dados do cache em memória.")
                return@withLock Result.success(cachedFixture!!)
            }

            try {
                Log.i("ApiFootballRepo", "Buscando na API-Football (ID: $fixtureId)...")
                val response = api.getFixtureById(apiKey = apiKey, id = fixtureId)
                
                if (response.isSuccessful) {
                    val body = response.body()
                    val fixtureData = body?.response?.firstOrNull()
                    if (fixtureData != null) {
                        cachedFixture = fixtureData
                        lastFetchTime = currentTime
                        Result.success(fixtureData)
                    } else {
                        Result.failure(Exception("Nenhum dado retornado para a fixture $fixtureId"))
                    }
                } else {
                    Result.failure(Exception("HTTP Erro: ${response.code()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun getFixturesByDate(date: String): Result<List<FixtureData>> {
        return try {
            val response = api.getFixturesByDate(apiKey = apiKey, date = date)
            if (response.isSuccessful) {
                val data = response.body()?.response ?: emptyList()
                Result.success(data)
            } else {
                Result.failure(Exception("HTTP Erro: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

object NetworkModule {
    fun provideApiFootballService(): ApiFootballService {
        val moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
            
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.HEADERS
        }
        
        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()

        return Retrofit.Builder()
            .baseUrl("https://v3.football.api-sports.io/")
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(ApiFootballService::class.java)
    }
    
    fun provideApiFootballRepository(api: ApiFootballService): ApiFootballRepository {
        val apiKey = com.example.BuildConfig.API_FOOTBALL_KEY
        return ApiFootballRepository(api, apiKey)
    }
}
