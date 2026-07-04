package com.example.data.sportmonks

import android.util.Log
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

// --- Models ---

@JsonClass(generateAdapter = true)
data class FixtureResponse(
    @Json(name = "data") val data: FixtureDto
)

@JsonClass(generateAdapter = true)
data class FixturesListResponse(
    @Json(name = "data") val data: List<FixtureDto>
)

@JsonClass(generateAdapter = true)
data class FixtureDto(
    @Json(name = "id") val id: Long,
    @Json(name = "name") val name: String,
    @Json(name = "starting_at") val startingAt: String,
    @Json(name = "participants") val participants: List<ParticipantDto>? = null,
    @Json(name = "league") val league: LeagueDto? = null,
    @Json(name = "state") val state: StateDto? = null,
    @Json(name = "scores") val scores: List<ScoreDto>? = null,
    @Json(name = "events") val events: List<EventDto>? = null,
    @Json(name = "lineups") val lineups: List<LineupDto>? = null,
    @Json(name = "venue") val venue: VenueDto? = null
)

@JsonClass(generateAdapter = true)
data class ParticipantDto(
    @Json(name = "id") val id: Long,
    @Json(name = "name") val name: String,
    @Json(name = "image_path") val imagePath: String? = null,
    @Json(name = "meta") val meta: ParticipantMetaDto? = null
)

@JsonClass(generateAdapter = true)
data class ParticipantMetaDto(
    @Json(name = "location") val location: String? = null
)

@JsonClass(generateAdapter = true)
data class LeagueDto(
    @Json(name = "id") val id: Long,
    @Json(name = "name") val name: String
)

@JsonClass(generateAdapter = true)
data class StateDto(
    @Json(name = "id") val id: Long,
    @Json(name = "state") val state: String,
    @Json(name = "name") val name: String
)

@JsonClass(generateAdapter = true)
data class ScoreDto(
    @Json(name = "id") val id: Long,
    @Json(name = "score") val score: ParticipantScoreDto,
    @Json(name = "description") val description: String? = null
)

@JsonClass(generateAdapter = true)
data class ParticipantScoreDto(
    @Json(name = "goals") val goals: Int,
    @Json(name = "participant") val participant: String
)

// --- Event & Lineup Models ---

@JsonClass(generateAdapter = true)
data class EventDto(
    @Json(name = "id") val id: Long,
    @Json(name = "fixture_id") val fixtureId: Long,
    @Json(name = "type_id") val typeId: Long,
    @Json(name = "participant_id") val participantId: Long,
    @Json(name = "player_id") val playerId: Long?,
    @Json(name = "minute") val minute: Int,
    @Json(name = "extra_minute") val extraMinute: Int?,
    @Json(name = "type") val type: TypeDto?,
    @Json(name = "player") val player: PlayerDto?
)

@JsonClass(generateAdapter = true)
data class LineupDto(
    @Json(name = "id") val id: Long,
    @Json(name = "fixture_id") val fixtureId: Long,
    @Json(name = "player_id") val playerId: Long,
    @Json(name = "participant_id") val participantId: Long,
    @Json(name = "formation_position") val formationPosition: Int?,
    @Json(name = "player") val player: PlayerDto?,
    @Json(name = "details") val details: List<LineupDetailDto>? = null
)

@JsonClass(generateAdapter = true)
data class LineupDetailDto(
    @Json(name = "id") val id: Long,
    @Json(name = "type_id") val typeId: Long,
    @Json(name = "type") val type: TypeDto?
)

@JsonClass(generateAdapter = true)
data class TypeDto(
    @Json(name = "id") val id: Long,
    @Json(name = "name") val name: String,
    @Json(name = "code") val code: String?
)

@JsonClass(generateAdapter = true)
data class PlayerDto(
    @Json(name = "id") val id: Long,
    @Json(name = "name") val name: String,
    @Json(name = "display_name") val displayName: String?,
    @Json(name = "image_path") val imagePath: String?
)

@JsonClass(generateAdapter = true)
data class VenueDto(
    @Json(name = "id") val id: Long,
    @Json(name = "name") val name: String,
    @Json(name = "city_name") val cityName: String?
)

// --- API ---

interface SportmonksApi {
    @GET("v3/football/fixtures/{id}")
    suspend fun getFixture(
        @Path("id") fixtureId: Long,
        @Query("api_token") apiToken: String,
        @Query("include") include: String = "participants;league;venue;state;scores;events.type;events.period;events.player;xGFixture.type;lineups.player;lineups.xGlineup.type;lineups.details.type"
    ): retrofit2.Response<FixtureResponse>

    @GET("v3/football/fixtures")
    suspend fun getFixtures(
        @Query("api_token") apiToken: String,
        @Query("include") include: String = "participants;league;state;scores",
        @Query("per_page") perPage: Int = 50
    ): retrofit2.Response<FixturesListResponse>
}

// --- Interceptor ---

class RateLimitInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)

        val remaining = response.header("X-RateLimit-Remaining")
        if (remaining != null) {
            Log.i("SportmonksAPI", "Rate Limit restante: $remaining")
            if (remaining.toIntOrNull() == 0) {
                Log.w("SportmonksAPI", "ALERTA: O limite de requisições foi atingido!")
            }
        }

        val retryAfter = response.header("Retry-After")
        if (retryAfter != null) {
            Log.w("SportmonksAPI", "Tempo de espera exigido (Retry-After): $retryAfter segundos")
        }

        return response
    }
}

// --- Repository ---

class FixtureRepository(
    private val api: SportmonksApi,
    private val apiToken: String
) {
    private var cachedFixture: FixtureResponse? = null
    private var lastFetchTime: Long = 0
    private val cacheDurationMillis = 5 * 60 * 1000L

    private val mutex = Mutex()

    suspend fun getFixture(fixtureId: Long): Result<FixtureResponse> {
        return mutex.withLock {
            val currentTime = System.currentTimeMillis()
            
            if (cachedFixture != null && (currentTime - lastFetchTime) < cacheDurationMillis) {
                Log.i("FixtureRepository", "Retornando dados do cache em memória (Poupando Rate Limit).")
                return@withLock Result.success(cachedFixture!!)
            }

            Log.i("FixtureRepository", "Cache expirado. Buscando na API Sportmonks...")

            try {
                val response = api.getFixture(fixtureId = fixtureId, apiToken = apiToken)

                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
                        cachedFixture = body
                        lastFetchTime = currentTime
                        Result.success(body)
                    } else {
                        Result.failure(Exception("Resposta vazia da API."))
                    }
                } else {
                    Result.failure(Exception("Erro da API: HTTP ${response.code()} - ${response.message()}"))
                }
            } catch (e: Exception) {
                Log.e("FixtureRepository", "Falha na conexão com a API: ${e.message}")
                Result.failure(e)
            }
        }
    }

    suspend fun getLatestFixtures(): Result<FixturesListResponse> {
        return mutex.withLock {
            try {
                val response = api.getFixtures(apiToken = apiToken)
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
                        Result.success(body)
                    } else {
                        Result.failure(Exception("Resposta vazia da API."))
                    }
                } else {
                    Result.failure(Exception("Erro da API: HTTP ${response.code()} - ${response.message()}"))
                }
            } catch (e: Exception) {
                Log.e("FixtureRepository", "Falha na conexão com a API: ${e.message}")
                Result.failure(e)
            }
        }
    }
}

// --- DI Module ---

object NetworkModule {
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(RateLimitInterceptor())
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    fun provideMoshi(): Moshi {
        return Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
    }

    fun provideSportmonksApi(client: OkHttpClient, moshi: Moshi): SportmonksApi {
        return Retrofit.Builder()
            .baseUrl("https://api.sportmonks.com/")
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(SportmonksApi::class.java)
    }
    
    fun provideFixtureRepository(api: SportmonksApi): FixtureRepository {
        // Lembre-se de colocar sua chave aqui, via BuildConfig ou Secrets!
        val apiToken = com.example.BuildConfig.SPORTMONKS_API_TOKEN
        return FixtureRepository(api, apiToken)
    }
}
