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
    suspend fun autenticar(@Query("token") token: String): Boolean

    // GET /Previsao/Linha?codigoLinha={codigoLinha}
    @GET("Previsao/Linha")
    suspend fun getPrevisaoLinha(@Query("codigoLinha") codigoLinha: Int): PrevisaoLinhaResponse
}

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
