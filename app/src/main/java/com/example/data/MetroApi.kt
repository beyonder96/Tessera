package com.example.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit
import androidx.compose.ui.graphics.Color

@JsonClass(generateAdapter = true)
data class MetroStatusResponse(
    @Json(name = "meta") val meta: MetroMeta?,
    @Json(name = "empresas") val empresas: List<MetroEmpresaStatus>?
)

@JsonClass(generateAdapter = true)
data class MetroMeta(
    @Json(name = "versao") val versao: String?,
    @Json(name = "timestamp") val timestamp: String?,
    @Json(name = "total_linhas") val totalLinhas: Int?,
    @Json(name = "total_empresas") val totalEmpresas: Int?
)

@JsonClass(generateAdapter = true)
data class MetroEmpresaStatus(
    @Json(name = "id") val id: Int,
    @Json(name = "nome") val nome: String,
    @Json(name = "fiscalizacao_artesp") val fiscalizacaoArtesp: Boolean?,
    @Json(name = "linhas") val linhas: List<MetroLinhaStatus>?
)

@JsonClass(generateAdapter = true)
data class MetroLinhaStatus(
    @Json(name = "nome") val nome: String,
    @Json(name = "codigo") val codigo: String,
    @Json(name = "ativa") val ativa: Boolean?,
    @Json(name = "status") val status: MetroLinhaStatusDetail?
)

@JsonClass(generateAdapter = true)
data class MetroLinhaStatusDetail(
    @Json(name = "situacao") val situacao: String,
    @Json(name = "classificacao") val classificacao: String?,
    @Json(name = "operacao_normal") val operacaoNormal: Boolean,
    @Json(name = "atualizado_em") val atualizadoEm: String?,
    @Json(name = "atualizado_ha") val atualizadoHa: String?
)

// Modelos para Concessionarias
@JsonClass(generateAdapter = true)
data class MetroConcessionariasResponse(
    @Json(name = "meta") val meta: MetroMetaConcessionaria?,
    @Json(name = "empresas") val empresas: List<MetroEmpresaConfig>?
)

@JsonClass(generateAdapter = true)
data class MetroMetaConcessionaria(
    @Json(name = "versao") val versao: String?,
    @Json(name = "timestamp") val timestamp: String?,
    @Json(name = "total") val total: Int?
)

@JsonClass(generateAdapter = true)
data class MetroEmpresaConfig(
    @Json(name = "id") val id: Int,
    @Json(name = "nome") val nome: String,
    @Json(name = "fiscalizacao_artesp") val fiscalizacaoArtesp: Boolean?,
    @Json(name = "linhas") val linhas: List<MetroLinhaConfig>?
)

@JsonClass(generateAdapter = true)
data class MetroLinhaConfig(
    @Json(name = "nome") val nome: String,
    @Json(name = "codigo") val codigo: String
)

interface MetroService {
    @GET("status/")
    suspend fun getStatus(
        @Query("empresa") empresaId: Int? = null,
        @Query("linha") linhaId: Int? = null,
        @Query("artesp_only") artespOnly: Boolean? = null
    ): MetroStatusResponse

    @GET("concessionarias/")
    suspend fun getConcessionarias(): MetroConcessionariasResponse

    companion object {
        private const val BASE_URL = "https://ccm.artesp.sp.gov.br/metroferroviario/api/"

        fun create(): MetroService {
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
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

            return retrofit.create(MetroService::class.java)
        }
    }
}

fun getMetroLineColor(lineName: String, lineCode: String): Color {
    val cleanName = lineName.lowercase()
    return when {
        cleanName.contains("azul") || lineCode == "1" -> Color(0xFF005CA9)
        cleanName.contains("verde") || lineCode == "2" -> Color(0xFF008940)
        cleanName.contains("vermelha") || lineCode == "3" -> Color(0xFFEE3E23)
        cleanName.contains("amarela") || lineCode == "4" -> Color(0xFFFFD100)
        cleanName.contains("lilás") || lineCode == "5" -> Color(0xFF90278E)
        cleanName.contains("rubi") || lineCode == "7" -> Color(0xFFA11732)
        cleanName.contains("diamante") || lineCode == "8" -> Color(0xFF97A0A6)
        cleanName.contains("esmeralda") || lineCode == "9" -> Color(0xFF00A78E)
        cleanName.contains("turquesa") || lineCode == "10" -> Color(0xFF00829B)
        cleanName.contains("coral") || lineCode == "11" -> Color(0xFFE04E22)
        cleanName.contains("safira") || lineCode == "12" -> Color(0xFF1F2244)
        cleanName.contains("jade") || lineCode == "13" -> Color(0xFF00B050)
        cleanName.contains("prata") || lineCode == "15" -> Color(0xFF97A0A6)
        else -> Color(0xFF808080)
    }
}
