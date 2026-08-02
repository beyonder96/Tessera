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
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import android.util.Log

@JsonClass(generateAdapter = true)
data class SportmonksResponse<T>(
    @Json(name = "data") val data: T
)

@JsonClass(generateAdapter = true)
data class SMFixture(
    @Json(name = "id") val id: Long,
    @Json(name = "name") val name: String?,
    @Json(name = "starting_at") val startingAt: String,
    @Json(name = "participants") val participants: List<SMParticipant>?,
    @Json(name = "state") val state: SMState?,
    @Json(name = "league") val league: SMLeague?,
    @Json(name = "scores") val scores: List<SMScore>?
)

@JsonClass(generateAdapter = true)
data class SMParticipant(
    @Json(name = "id") val id: Long,
    @Json(name = "name") val name: String,
    @Json(name = "image_path") val imagePath: String?,
    @Json(name = "meta") val meta: SMMeta?
)

@JsonClass(generateAdapter = true)
data class SMMeta(
    @Json(name = "location") val location: String? // "home" or "away"
)

@JsonClass(generateAdapter = true)
data class SMState(
    @Json(name = "state") val state: String?,
    @Json(name = "name") val name: String?
)

@JsonClass(generateAdapter = true)
data class SMLeague(
    @Json(name = "name") val name: String?
)

@JsonClass(generateAdapter = true)
data class SMScore(
    @Json(name = "score") val score: SMScoreDetail?,
    @Json(name = "description") val description: String?, // "CURRENT"
    @Json(name = "score_type") val scoreType: SMScoreType?
)

@JsonClass(generateAdapter = true)
data class SMScoreDetail(
    @Json(name = "goals") val goals: Int?
)

@JsonClass(generateAdapter = true)
data class SMScoreType(
    @Json(name = "name") val name: String? // "home" or "away"
)

fun SMFixture.toFixtureData(): FixtureData {
    val homeParticipant = participants?.find { it.meta?.location == "home" }
    val awayParticipant = participants?.find { it.meta?.location == "away" }

    val homeScoreObj = scores?.find { it.description == "CURRENT" && it.scoreType?.name == "home" }
    val awayScoreObj = scores?.find { it.description == "CURRENT" && it.scoreType?.name == "away" }

    return FixtureData(
        fixture = FixtureInfo(
            id = this.id,
            date = this.startingAt,
            status = StatusInfo(short = this.state?.state ?: "NS"),
            venue = null
        ),
        league = LeagueInfo(
            name = this.league?.name ?: "Desconhecida"
        ),
        teams = TeamsInfo(
            home = TeamInfo(
                id = homeParticipant?.id ?: 0,
                name = homeParticipant?.name ?: "Time da Casa",
                logo = homeParticipant?.imagePath
            ),
            away = TeamInfo(
                id = awayParticipant?.id ?: 0,
                name = awayParticipant?.name ?: "Time Visitante",
                logo = awayParticipant?.imagePath
            )
        ),
        goals = GoalsInfo(
            home = homeScoreObj?.score?.goals,
            away = awayScoreObj?.score?.goals
        ),
        events = emptyList(),
        lineups = emptyList()
    )
}

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
    @GET("fixtures/date/{date}")
    suspend fun getFixturesByDate(
        @Path("date") date: String,
        @Query("api_token") apiToken: String,
        @Query("include") include: String = "participants;league;state;scores",
        @Query("timezone") timezone: String = "America/Sao_Paulo"
    ): retrofit2.Response<SportmonksResponse<List<SMFixture>>>

    @GET("fixtures/{id}")
    suspend fun getFixtureById(
        @Path("id") id: Long,
        @Query("api_token") apiToken: String,
        @Query("include") include: String = "participants;league;state;scores;events;lineups",
        @Query("timezone") timezone: String = "America/Sao_Paulo"
    ): retrofit2.Response<SportmonksResponse<SMFixture>>
}

class ApiFootballRepository(
    private val api: ApiFootballService,
    private val apiToken: String
) {
    private val mutex = Mutex()
    private var cachedFixture: FixtureData? = null
    private var lastFetchTime = 0L
    private val cacheDurationMillis = 5 * 60 * 1000

    suspend fun getFixture(fixtureId: Long): Result<FixtureData> {
        val currentTime = System.currentTimeMillis()
        
        return mutex.withLock {
            if (cachedFixture != null && cachedFixture!!.fixture.id == fixtureId && (currentTime - lastFetchTime) < cacheDurationMillis) {
                Log.i("ApiFootballRepo", "Retornando dados do cache em memória.")
                return@withLock Result.success(cachedFixture!!)
            }

            try {
                Log.i("ApiFootballRepo", "Buscando na Sportmonks (ID: $fixtureId)...")
                val response = api.getFixtureById(apiToken = apiToken, id = fixtureId)
                
                if (response.isSuccessful) {
                    val body = response.body()
                    val smFixture = body?.data
                    if (smFixture != null) {
                        val fixtureData = smFixture.toFixtureData()
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
            val response = api.getFixturesByDate(apiToken = apiToken, date = date)
            if (response.isSuccessful) {
                val data = response.body()?.data?.map { it.toFixtureData() } ?: emptyList()
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
            .baseUrl("https://api.sportmonks.com/v3/football/")
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(ApiFootballService::class.java)
    }
    
    fun provideApiFootballRepository(api: ApiFootballService): ApiFootballRepository {
        val apiToken = "s9M1yUatdExguK89eVzkubPv15aZO0hsQoRXjzh01b7g2nUGFPy5qxjmXOqo"
        return ApiFootballRepository(api, apiToken)
    }
}
