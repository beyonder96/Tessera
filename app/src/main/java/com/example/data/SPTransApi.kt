package com.example.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class SPTransParada(
    @Json(name = "cp") val cp: Int, // Código do ponto de parada
    @Json(name = "np") val np: String, // Nome da parada
    @Json(name = "ed") val ed: String?, // Endereço
    @Json(name = "py") val py: Double, // Latitude
    @Json(name = "px") val px: Double // Longitude
)

@JsonClass(generateAdapter = true)
data class SPTransPrevisaoResponse(
    @Json(name = "hr") val hr: String?, // Horário de referência da transmissão
    @Json(name = "p") val p: SPTransPrevisaoParada? // Parada com suas previsões
)

@JsonClass(generateAdapter = true)
data class SPTransPrevisaoParada(
    @Json(name = "cp") val cp: Int, // Código da parada
    @Json(name = "np") val np: String, // Nome da parada
    @Json(name = "py") val py: Double,
    @Json(name = "px") val px: Double,
    @Json(name = "l") val l: List<SPTransLinhaPrevisao>? // Linhas que passam no ponto
)

@JsonClass(generateAdapter = true)
data class SPTransLinhaPrevisao(
    @Json(name = "c") val c: String, // Letreiro completo da linha (ex: 809P-10)
    @Json(name = "cl") val cl: Int, // Código identificador da linha
    @Json(name = "sl") val sl: Int, // Sentido (1: Terminal Principal, 2: Terminal Secundário)
    @Json(name = "lt0") val lt0: String, // Letreiro de destino
    @Json(name = "lt1") val lt1: String, // Letreiro de origem
    @Json(name = "qv") val qv: Int, // Quantidade de veículos em circulação na linha
    @Json(name = "vs") val vs: List<SPTransVeiculoPrevisao>? // Lista de veículos em circulação
)

@JsonClass(generateAdapter = true)
data class SPTransVeiculoPrevisao(
    @Json(name = "p") val p: Int, // Prefixo do veículo
    @Json(name = "t") val t: String, // Horário estimado de chegada do veículo
    @Json(name = "a") val a: Boolean, // Indica se o veículo é adaptado para deficientes
    @Json(name = "ta") val ta: String?, // Horário de transmissão da localização do veículo
    @Json(name = "py") val py: Double, // Latitude da última transmissão
    @Json(name = "px") val px: Double // Longitude da última transmissão
)

@JsonClass(generateAdapter = true)
data class SPTransVeiculoPosicao(
    @Json(name = "p") val p: Int, // Prefixo do veículo
    @Json(name = "a") val a: Boolean, // Acessibilidade
    @Json(name = "ta") val ta: String?, // Horário da transmissão
    @Json(name = "py") val py: Double, // Latitude da última transmissão
    @Json(name = "px") val px: Double // Longitude da última transmissão
)

@JsonClass(generateAdapter = true)
data class SPTransPosicaoLinhaResponse(
    @Json(name = "hr") val hr: String?,
    @Json(name = "vs") val vs: List<SPTransVeiculoPosicao>?
)

@JsonClass(generateAdapter = true)
data class SPTransLinhaDetalhe(
    @Json(name = "cl") val cl: Int,
    @Json(name = "nc") val nc: String,
    @Json(name = "sl") val sl: Int,
    @Json(name = "lt0") val lt0: String,
    @Json(name = "lt1") val lt1: String
)

interface SPTransService {
    @POST("Login/Autenticar")
    suspend fun authenticate(
        @Query("token") token: String
    ): retrofit2.Response<Boolean>

    @GET("Parada/BuscarPorPosicao")
    suspend fun getParadasPorPosicao(
        @Query("lat") lat: Double,
        @Query("lng") lng: Double,
        @Query("raio") raio: Int
    ): List<SPTransParada>

    @GET("Previsao/Parada")
    suspend fun getPrevisaoParada(
        @Query("codigoParada") codigoParada: Int
    ): SPTransPrevisaoResponse

    @GET("Parada/BuscarParadasPorLinha")
    suspend fun getParadasPorLinha(
        @Query("codigoLinha") codigoLinha: Int
    ): List<SPTransParada>

    @GET("Linha/CarregarDetalhes")
    suspend fun getLinhaDetalhes(
        @Query("codigoLinha") codigoLinha: Int
    ): List<SPTransLinhaDetalhe>

    @GET("Posicao/Linha")
    suspend fun getPosicaoLinha(
        @Query("codigoLinha") codigoLinha: Int
    ): SPTransPosicaoLinhaResponse

    companion object {
        private const val BASE_URL = "https://api.olhovivo.sptrans.com.br/v2.1/"

        fun create(): SPTransService {
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }

            val okHttpClient = OkHttpClient.Builder()
                .addInterceptor(SPTransCookieInterceptor)
                .addInterceptor(logging)
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .build()

            val retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(MoshiConverterFactory.create())
                .build()

            return retrofit.create(SPTransService::class.java)
        }
    }
}

object SPTransCookieInterceptor : Interceptor {
    @Volatile
    private var cachedCookie: String? = null

    override fun intercept(chain: Interceptor.Chain): okhttp3.Response {
        val requestBuilder = chain.request().newBuilder()
        
        cachedCookie?.let {
            requestBuilder.addHeader("Cookie", it)
        }
        
        val response = chain.proceed(requestBuilder.build())
        
        val setCookies = response.headers("Set-Cookie")
        if (setCookies.isNotEmpty()) {
            val cookieValue = setCookies.firstOrNull()?.split(";")?.firstOrNull()
            if (cookieValue != null && cookieValue.startsWith("NetContextToken")) {
                cachedCookie = cookieValue
            }
        }
        
        return response
    }

    fun clearCookie() {
        cachedCookie = null
    }
}
