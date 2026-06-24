package com.example.data

import retrofit2.http.POST
import retrofit2.http.Query
import retrofit2.http.GET
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import okhttp3.OkHttpClient
import okhttp3.CookieJar
import okhttp3.Cookie
import okhttp3.HttpUrl
import java.util.concurrent.TimeUnit

interface SPTransApiService {
    @POST("Login/Autenticar")
    suspend fun autenticar(@Query("token") token: String): retrofit2.Response<okhttp3.ResponseBody>

    @GET("Previsao/Linha")
    suspend fun getPrevisaoLinha(@Query("codigoLinha") codigoLinha: Int): PrevisaoLinhaResponse

    @GET("Linha/Buscar")
    suspend fun buscarLinha(@Query("termosBusca") termosBusca: String): List<SPTransLinha>

    @GET("Parada/BuscarParadasPorLinha")
    suspend fun getParadasPorLinha(@Query("codigoLinha") codigoLinha: Int): List<SPTransParada>

    @GET("Previsao/Parada")
    suspend fun getPrevisaoParada(
        @Query("codigoParada") codigoParada: Int,
        @Query("codigoLinha") codigoLinha: Int
    ): PrevisaoParadaResponse
}

@JsonClass(generateAdapter = true)
data class SPTransLinha(
    @Json(name = "cl") val cl: Int,
    @Json(name = "lc") val lc: Boolean,
    @Json(name = "lt") val lt: String,
    @Json(name = "sl") val sl: Int,
    @Json(name = "tl") val tl: Int,
    @Json(name = "tp") val tp: String,
    @Json(name = "ts") val ts: String
)

@JsonClass(generateAdapter = true)
data class SPTransParada(
    @Json(name = "cp") val cp: Int,
    @Json(name = "np") val np: String,
    @Json(name = "ed") val ed: String,
    @Json(name = "py") val py: Double,
    @Json(name = "px") val px: Double
)

@JsonClass(generateAdapter = true)
data class PrevisaoParadaResponse(
    @Json(name = "hr") val hr: String?,
    @Json(name = "p") val p: PrevisaoParadaDetail?
)

@JsonClass(generateAdapter = true)
data class PrevisaoParadaDetail(
    @Json(name = "cp") val cp: Int,
    @Json(name = "np") val np: String,
    @Json(name = "py") val py: Double,
    @Json(name = "px") val px: Double,
    @Json(name = "l") val l: List<PrevisaoParadaLinha>?
)

@JsonClass(generateAdapter = true)
data class PrevisaoParadaLinha(
    @Json(name = "c") val c: String,
    @Json(name = "cl") val cl: Int,
    @Json(name = "sl") val sl: Int,
    @Json(name = "lt0") val lt0: String,
    @Json(name = "lt1") val lt1: String,
    @Json(name = "qv") val qv: Int,
    @Json(name = "vs") val vs: List<VeiculoPrevisao>?
)

@JsonClass(generateAdapter = true)
data class PrevisaoLinhaResponse(
    @Json(name = "hr") val hr: String?,
    @Json(name = "ps") val ps: List<ParadaPrevisao>?
)

@JsonClass(generateAdapter = true)
data class ParadaPrevisao(
    @Json(name = "cp") val cp: Int, // Codigo parada
    @Json(name = "np") val np: String, // Nome parada
    @Json(name = "vs") val vs: List<VeiculoPrevisao>?
)

@JsonClass(generateAdapter = true)
data class VeiculoPrevisao(
    @Json(name = "p") val p: String, // Prefixo do veículo
    @Json(name = "t") val t: String, // Horário previsto
    @Json(name = "px") val px: Double?,
    @Json(name = "py") val py: Double?
)

// UI Model for TransportScreen
data class SavedBusLine(
    val id: String,
    val lineCode: Int,
    val lineNumber: String,
    val destination: String,
    val estimatedArrivalText: String,
    val stopName: String
)

object SPTransApi {
    private const val BASE_URL = "https://api.olhovivo.sptrans.com.br/v2.1/"

    private val cookieJar = object : CookieJar {
        private val cookies = mutableListOf<Cookie>()
        override fun saveFromResponse(url: HttpUrl, newCookies: List<Cookie>) {
            // Keep existing valid cookies, add/replace new ones
            for (newCookie in newCookies) {
                cookies.removeAll { it.name == newCookie.name }
                cookies.add(newCookie)
            }
        }
        override fun loadForRequest(url: HttpUrl): List<Cookie> {
            return cookies
        }
    }

    val okHttpClient = OkHttpClient.Builder()
        .cookieJar(cookieJar)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create())
        .build()

    val service: SPTransApiService = retrofit.create(SPTransApiService::class.java)
}
