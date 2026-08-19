package com.example.data.nutrition

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

@JsonClass(generateAdapter = true)
data class OpenFoodNutriments(
    @Json(name = "energy-kcal_100g") val energyKcal100g: Double? = null,
    @Json(name = "energy-kcal_serving") val energyKcalServing: Double? = null,
    @Json(name = "energy-kcal") val energyKcal: Double? = null,
    @Json(name = "proteins_100g") val proteins100g: Double? = null,
    @Json(name = "proteins_serving") val proteinsServing: Double? = null,
    @Json(name = "proteins") val proteins: Double? = null,
    @Json(name = "carbohydrates_100g") val carbs100g: Double? = null,
    @Json(name = "carbohydrates_serving") val carbsServing: Double? = null,
    @Json(name = "carbohydrates") val carbs: Double? = null,
    @Json(name = "fat_100g") val fat100g: Double? = null,
    @Json(name = "fat_serving") val fatServing: Double? = null,
    @Json(name = "fat") val fat: Double? = null,
    @Json(name = "fiber_100g") val fiber100g: Double? = null,
    @Json(name = "fiber_serving") val fiberServing: Double? = null,
    @Json(name = "fiber") val fiber: Double? = null,
    @Json(name = "sodium_100g") val sodium100g: Double? = null,
    @Json(name = "sodium_serving") val sodiumServing: Double? = null,
    @Json(name = "sugars_100g") val sugars100g: Double? = null
)

@JsonClass(generateAdapter = true)
data class OpenFoodProduct(
    @Json(name = "_id") val id: String? = null,
    @Json(name = "code") val code: String? = null,
    @Json(name = "product_name") val productName: String? = null,
    @Json(name = "product_name_pt") val productNamePt: String? = null,
    @Json(name = "brands") val brands: String? = null,
    @Json(name = "image_front_url") val imageFrontUrl: String? = null,
    @Json(name = "image_url") val imageUrl: String? = null,
    @Json(name = "nutriscore_grade") val nutriscoreGrade: String? = null,
    @Json(name = "nova_group") val novaGroup: Int? = null,
    @Json(name = "serving_size") val servingSize: String? = null,
    @Json(name = "nutriments") val nutriments: OpenFoodNutriments? = null,
    @Json(name = "ingredients_text_pt") val ingredientsTextPt: String? = null,
    @Json(name = "allergens_tags") val allergensTags: List<String>? = null
) {
    val displayName: String
        get() = productNamePt?.takeIf { it.isNotBlank() }
            ?: productName?.takeIf { it.isNotBlank() }
            ?: "Produto Sem Nome"

    val displayBrand: String
        get() = brands?.takeIf { it.isNotBlank() } ?: "Marca desconhecida"

    val bestImage: String?
        get() = imageFrontUrl ?: imageUrl

    val effectiveCalories: Double
        get() = nutriments?.energyKcalServing
            ?: nutriments?.energyKcal100g
            ?: nutriments?.energyKcal
            ?: 0.0

    val effectiveProtein: Double
        get() = nutriments?.proteinsServing
            ?: nutriments?.proteins100g
            ?: nutriments?.proteins
            ?: 0.0

    val effectiveCarbs: Double
        get() = nutriments?.carbsServing
            ?: nutriments?.carbs100g
            ?: nutriments?.carbs
            ?: 0.0

    val effectiveFat: Double
        get() = nutriments?.fatServing
            ?: nutriments?.fat100g
            ?: nutriments?.fat
            ?: 0.0

    val effectiveFiber: Double
        get() = nutriments?.fiberServing
            ?: nutriments?.fiber100g
            ?: nutriments?.fiber
            ?: 0.0

    val effectivePortion: String
        get() = servingSize?.takeIf { it.isNotBlank() } ?: "100g"
}

@JsonClass(generateAdapter = true)
data class OpenFoodBarcodeResponse(
    @Json(name = "status") val status: Int? = 0,
    @Json(name = "status_verbose") val statusVerbose: String? = null,
    @Json(name = "product") val product: OpenFoodProduct? = null
)

@JsonClass(generateAdapter = true)
data class OpenFoodSearchResponse(
    @Json(name = "count") val count: Int? = 0,
    @Json(name = "products") val products: List<OpenFoodProduct>? = emptyList()
)

interface OpenFoodFactsService {
    @GET("api/v2/product/{barcode}.json")
    suspend fun getProductByBarcode(
        @Path("barcode") barcode: String
    ): OpenFoodBarcodeResponse

    @GET("cgi/search.pl")
    suspend fun searchProducts(
        @Query("search_terms") terms: String,
        @Query("search_simple") simple: Int = 1,
        @Query("action") action: String = "process",
        @Query("json") json: Int = 1,
        @Query("page_size") pageSize: Int = 20,
        @Query("cc") countryCode: String = "br"
    ): OpenFoodSearchResponse
}

object OpenFoodFactsApiClient {
    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        })
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .header("User-Agent", "TesseraApp - Android - Version 2.0 - https://tessera-app.firebaseapp.com")
                .build()
            chain.proceed(request)
        }
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://world.openfoodfacts.org/")
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    val service: OpenFoodFactsService = retrofit.create(OpenFoodFactsService::class.java)
}
