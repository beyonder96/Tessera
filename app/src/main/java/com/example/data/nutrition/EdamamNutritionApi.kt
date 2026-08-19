package com.example.data.nutrition

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class EdamamNutritionRequest(
    @Json(name = "title") val title: String = "Refeição",
    @Json(name = "ingr") val ingr: List<String>
)

@JsonClass(generateAdapter = true)
data class EdamamNutrientInfo(
    @Json(name = "label") val label: String? = "",
    @Json(name = "quantity") val quantity: Double? = 0.0,
    @Json(name = "unit") val unit: String? = ""
)

@JsonClass(generateAdapter = true)
data class EdamamNutritionResponse(
    @Json(name = "calories") val calories: Double? = 0.0,
    @Json(name = "totalWeight") val totalWeight: Double? = 0.0,
    @Json(name = "dietLabels") val dietLabels: List<String>? = emptyList(),
    @Json(name = "healthLabels") val healthLabels: List<String>? = emptyList(),
    @Json(name = "totalNutrients") val totalNutrients: Map<String, EdamamNutrientInfo>? = emptyMap()
) {
    val protein: Double
        get() = totalNutrients?.get("PROCNT")?.quantity ?: 0.0

    val carbs: Double
        get() = totalNutrients?.get("CHOCDF")?.quantity ?: 0.0

    val fat: Double
        get() = totalNutrients?.get("FAT")?.quantity ?: 0.0

    val fiber: Double
        get() = totalNutrients?.get("FIBTG")?.quantity ?: 0.0

    val sodium: Double
        get() = totalNutrients?.get("NA")?.quantity ?: 0.0
}

interface EdamamNutritionService {
    @POST("api/nutrition-details")
    suspend fun analyzeNutrition(
        @Query("app_id") appId: String,
        @Query("app_key") appKey: String,
        @Body request: EdamamNutritionRequest
    ): EdamamNutritionResponse

    @GET("api/nutrition-data")
    suspend fun analyzeSingleIngredient(
        @Query("app_id") appId: String,
        @Query("app_key") appKey: String,
        @Query("ingr") ingr: String
    ): EdamamNutritionResponse
}

object EdamamApiClient {
    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        })
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://api.edamam.com/")
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    val service: EdamamNutritionService = retrofit.create(EdamamNutritionService::class.java)
}

data class NutritionAnalysisResult(
    val dishName: String,
    val calories: Double,
    val protein: Double,
    val carbs: Double,
    val fat: Double,
    val fiber: Double,
    val portion: String = "1 refeição",
    val itemsSummary: List<String> = emptyList(),
    val source: String = "Edamam API"
)

object NutritionDishAnalyzer {

    // Base de dados nutricional brasileira (TACO/IBGE) para análise local instantânea ou fallback
    private data class FoodBase(
        val keywords: List<String>,
        val calPer100g: Double,
        val protPer100g: Double,
        val carbPer100g: Double,
        val fatPer100g: Double,
        val fiberPer100g: Double,
        val defaultServingGrams: Double
    )

    private val foodDatabase = listOf(
        FoodBase(listOf("arroz", "arroz branco", "rice"), 130.0, 2.5, 28.0, 0.3, 0.4, 150.0),
        FoodBase(listOf("arroz integral", "brown rice"), 112.0, 2.6, 23.5, 0.9, 1.8, 150.0),
        FoodBase(listOf("feijao", "feijão", "feijao carioca", "feijão preto", "beans"), 76.0, 4.8, 14.0, 0.5, 8.5, 100.0),
        FoodBase(listOf("frango", "peito de frango", "filé de frango", "chicken"), 165.0, 31.0, 0.0, 3.6, 0.0, 150.0),
        FoodBase(listOf("carne", "carne moida", "carne moída", "patinho", "bife", "beef"), 215.0, 26.0, 0.0, 12.0, 0.0, 150.0),
        FoodBase(listOf("ovo", "ovos", "egg", "eggs", "ovo cozido", "ovo frito", "ovos mexidos"), 143.0, 13.0, 0.7, 9.5, 0.0, 50.0),
        FoodBase(listOf("pao", "pão", "pao frances", "pão francês", "pao de forma", "bread"), 265.0, 9.0, 49.0, 3.2, 2.7, 50.0),
        FoodBase(listOf("pao integral", "pão integral", "whole wheat bread"), 240.0, 10.5, 43.0, 3.5, 6.0, 50.0),
        FoodBase(listOf("batata", "batata doce", "sweet potato"), 86.0, 1.6, 20.0, 0.1, 3.0, 150.0),
        FoodBase(listOf("batata inglesa", "potato"), 77.0, 2.0, 17.5, 0.1, 2.2, 150.0),
        FoodBase(listOf("aveia", "farinha de aveia", "oats"), 389.0, 16.9, 66.3, 6.9, 10.6, 30.0),
        FoodBase(listOf("banana", "bananas"), 89.0, 1.1, 22.8, 0.3, 2.6, 100.0),
        FoodBase(listOf("maca", "maçã", "apple"), 52.0, 0.3, 13.8, 0.2, 2.4, 130.0),
        FoodBase(listOf("leite", "leite integral", "milk"), 61.0, 3.2, 4.8, 3.3, 0.0, 200.0),
        FoodBase(listOf("leite desnatado", "skim milk"), 35.0, 3.4, 5.0, 0.2, 0.0, 200.0),
        FoodBase(listOf("iogurte", "iogurte grego", "yogurt"), 59.0, 3.5, 4.7, 3.3, 0.0, 170.0),
        FoodBase(listOf("whey", "whey protein", "proteina"), 380.0, 75.0, 8.0, 4.0, 1.0, 30.0),
        FoodBase(listOf("queijo", "queijo minas", "mussarela", "cheese"), 280.0, 22.0, 2.0, 21.0, 0.0, 30.0),
        FoodBase(listOf("azeite", "azeite de oliva", "olive oil", "oleo", "óleo"), 884.0, 0.0, 0.0, 100.0, 0.0, 13.0),
        FoodBase(listOf("tapioca", "goma de tapioca"), 240.0, 0.0, 60.0, 0.0, 0.0, 60.0),
        FoodBase(listOf("salada", "alface", "tomate", "pepino", "salad"), 20.0, 1.2, 3.5, 0.2, 1.5, 100.0),
        FoodBase(listOf("cafe", "café", "coffee"), 2.0, 0.1, 0.0, 0.0, 0.0, 100.0)
    )

    suspend fun analyzeText(
        inputText: String,
        edamamAppId: String? = null,
        edamamAppKey: String? = null
    ): NutritionAnalysisResult = withContext(Dispatchers.IO) {
        val trimmed = inputText.trim()
        if (trimmed.isBlank()) {
            return@withContext NutritionAnalysisResult(
                dishName = "Refeição",
                calories = 0.0,
                protein = 0.0,
                carbs = 0.0,
                fat = 0.0,
                fiber = 0.0
            )
        }

        // 1. Se possuir credenciais Edamam válidas, tentar chamada de API
        if (!edamamAppId.isNullOrBlank() && !edamamAppKey.isNullOrBlank()) {
            try {
                val lines = splitIngredients(trimmed)
                val response = EdamamApiClient.service.analyzeNutrition(
                    appId = edamamAppId,
                    appKey = edamamAppKey,
                    request = EdamamNutritionRequest(
                        title = trimmed.take(40),
                        ingr = lines
                    )
                )

                if (response.calories != null && response.calories > 0) {
                    return@withContext NutritionAnalysisResult(
                        dishName = trimmed.take(50),
                        calories = response.calories,
                        protein = response.protein,
                        carbs = response.carbs,
                        fat = response.fat,
                        fiber = response.fiber,
                        portion = "${response.totalWeight?.toInt() ?: 100}g",
                        itemsSummary = lines,
                        source = "Edamam Nutrition API"
                    )
                }
            } catch (e: Exception) {
                // Fallback silencioso para cálculo local inteligente
            }
        }

        // 2. Análise Local Inteligente (NLP baseado em regras e tabela TACO)
        return@withContext analyzeWithLocalDatabase(trimmed)
    }

    private fun splitIngredients(text: String): List<String> {
        val delimiters = listOf(",", "\n", ";", " e ", " com ", " + ")
        var items = listOf(text)
        for (d in delimiters) {
            items = items.flatMap { it.split(d) }
        }
        return items.map { it.trim() }.filter { it.isNotBlank() }
    }

    private fun analyzeWithLocalDatabase(text: String): NutritionAnalysisResult {
        val rawItems = splitIngredients(text)
        var totalCal = 0.0
        var totalProt = 0.0
        var totalCarb = 0.0
        var totalFat = 0.0
        var totalFiber = 0.0
        val detectedItems = mutableListOf<String>()

        for (item in rawItems) {
            val normalized = normalizeString(item)
            val quantity = extractQuantity(normalized)

            var matchedFood: FoodBase? = null
            for (food in foodDatabase) {
                if (food.keywords.any { normalized.contains(normalizeString(it)) }) {
                    matchedFood = food
                    break
                }
            }

            if (matchedFood != null) {
                val grams = calculateGrams(normalized, quantity, matchedFood.defaultServingGrams)
                val factor = grams / 100.0

                val cal = matchedFood.calPer100g * factor
                val prot = matchedFood.protPer100g * factor
                val carb = matchedFood.carbPer100g * factor
                val fat = matchedFood.fatPer100g * factor
                val fib = matchedFood.fiberPer100g * factor

                totalCal += cal
                totalProt += prot
                totalCarb += carb
                totalFat += fat
                totalFiber += fib

                detectedItems.add("${item.trim()} (~${grams.toInt()}g - ${cal.toInt()} kcal)")
            } else {
                // Estimativa genérica média para item desconhecido
                val grams = 100.0
                val cal = 150.0
                totalCal += cal
                totalProt += 5.0
                totalCarb += 20.0
                totalFat += 5.0
                detectedItems.add("${item.trim()} (~${cal.toInt()} kcal)")
            }
        }

        val dishTitle = if (rawItems.size == 1) rawItems.first().trim() else "Prato Misto (${rawItems.size} itens)"

        return NutritionAnalysisResult(
            dishName = dishTitle.take(60),
            calories = kotlin.math.round(totalCal * 10) / 10.0,
            protein = kotlin.math.round(totalProt * 10) / 10.0,
            carbs = kotlin.math.round(totalCarb * 10) / 10.0,
            fat = kotlin.math.round(totalFat * 10) / 10.0,
            fiber = kotlin.math.round(totalFiber * 10) / 10.0,
            portion = "1 prato",
            itemsSummary = detectedItems,
            source = "Análise Nutricional Inteligente"
        )
    }

    private fun extractQuantity(text: String): Double {
        val numberRegex = """(\d+(?:[.,]\d+)?)""".toRegex()
        val match = numberRegex.find(text)
        return match?.value?.replace(",", ".")?.toDoubleOrNull() ?: 1.0
    }

    private fun calculateGrams(text: String, quantity: Double, defaultServing: Double): Double {
        return when {
            text.contains("g") && !text.contains("colher") && !text.contains("copo") -> {
                val gMatch = """(\d+(?:[.,]\d+)?)\s*g""".toRegex().find(text)
                gMatch?.groupValues?.get(1)?.toDoubleOrNull() ?: (quantity * 1.0)
            }
            text.contains("kg") -> quantity * 1000.0
            text.contains("ml") -> quantity * 1.0
            text.contains("colher de sopa") || text.contains("cs") -> quantity * 15.0
            text.contains("colher de cha") || text.contains("colher de chá") -> quantity * 5.0
            text.contains("concha") -> quantity * 120.0
            text.contains("xicara") || text.contains("xícara") || text.contains("copo") -> quantity * 150.0
            text.contains("fatia") || text.contains("unidade") || text.contains("ovo") || text.contains("ovos") -> quantity * defaultServing
            else -> quantity * defaultServing
        }
    }

    private fun normalizeString(str: String): String {
        val nfdNormalizedString = java.text.Normalizer.normalize(str, java.text.Normalizer.Form.NFD)
        val pattern = "\\p{InCombiningDiacriticalMarks}+".toRegex()
        return pattern.replace(nfdNormalizedString, "").lowercase()
    }
}
