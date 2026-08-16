package com.example.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class TSDBTeam(
    @Json(name = "idTeam") val idTeam: String?,
    @Json(name = "strTeam") val strTeam: String?,
    @Json(name = "strBadge") val strBadge: String?,
    @Json(name = "strLogo") val strLogo: String?
)

@JsonClass(generateAdapter = true)
data class TSDBTeamsResponse(
    @Json(name = "teams") val teams: List<TSDBTeam>?
)

@JsonClass(generateAdapter = true)
data class TSDBEvent(
    @Json(name = "idEvent") val idEvent: String?,
    @Json(name = "strEvent") val strEvent: String?,
    @Json(name = "strFilename") val strFilename: String?,
    @Json(name = "strLeague") val strLeague: String?,
    @Json(name = "strHomeTeam") val strHomeTeam: String?,
    @Json(name = "strAwayTeam") val strAwayTeam: String?,
    @Json(name = "intHomeScore") val intHomeScore: String?,
    @Json(name = "intAwayScore") val intAwayScore: String?,
    @Json(name = "strHomeTeamBadge") val strHomeTeamBadge: String?,
    @Json(name = "strAwayTeamBadge") val strAwayTeamBadge: String?,
    @Json(name = "dateEvent") val dateEvent: String?,
    @Json(name = "strTime") val strTime: String?,
    @Json(name = "strTimeLocal") val strTimeLocal: String?,
    @Json(name = "strStatus") val strStatus: String?, // "FT", "NS", "1H", "2H", "HT", "POST"
    @Json(name = "strVenue") val strVenue: String?,
    @Json(name = "idHomeTeam") val idHomeTeam: String?,
    @Json(name = "idAwayTeam") val idAwayTeam: String?
)

@JsonClass(generateAdapter = true)
data class TSDBEventsResponse(
    @Json(name = "events") val events: List<TSDBEvent>?,
    @Json(name = "results") val results: List<TSDBEvent>?
)

interface TheSportsDbService {
    @GET("searchteams.php")
    suspend fun searchTeam(@Query("t") teamName: String): TSDBTeamsResponse

    @GET("eventsnext.php")
    suspend fun getNextEvents(@Query("id") teamId: String): TSDBEventsResponse

    @GET("eventslast.php")
    suspend fun getLastEvents(@Query("id") teamId: String): TSDBEventsResponse
}

object TheSportsDbApi {
    private const val BASE_URL = "https://www.thesportsdb.com/api/v1/json/3/"

    val service: TheSportsDbService by lazy {
        val moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()

        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()

        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(TheSportsDbService::class.java)
    }

    // Mapeamento oficial dos principais times brasileiros no TheSportsDB para respostas instantâneas
    val knownBrazilianTeams = mapOf(
        "flamengo" to "134287",
        "palmeiras" to "134288",
        "são paulo" to "134293",
        "sao paulo" to "134293",
        "corinthians" to "134286",
        "fluminense" to "134289",
        "vasco" to "134295",
        "botafogo" to "134283",
        "grêmio" to "134290",
        "gremio" to "134290",
        "internacional" to "134291",
        "atlético mineiro" to "134282",
        "atletico mineiro" to "134282",
        "cruzeiro" to "134285",
        "santos" to "134292",
        "bahia" to "134284",
        "fortaleza" to "134706",
        "athletico paranaense" to "134281",
        "red bull bragantino" to "136151",
        "bragantino" to "136151",
        "vitória" to "134280",
        "vitoria" to "134280",
        "juventude" to "134301",
        "criciúma" to "134300",
        "criciuma" to "134300",
        "cuiabá" to "136933",
        "cuiaba" to "136933",
        "atlético goianiense" to "134299",
        "atletico goianiense" to "134299"
    )
}
