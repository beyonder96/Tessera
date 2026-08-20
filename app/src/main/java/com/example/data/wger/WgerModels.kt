package com.example.data.wger

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class WgerPaginatedResponse<T>(
    @Json(name = "count") val count: Int = 0,
    @Json(name = "next") val next: String? = null,
    @Json(name = "previous") val previous: String? = null,
    @Json(name = "results") val results: List<T> = emptyList()
)

@JsonClass(generateAdapter = true)
data class WgerCategoryDto(
    @Json(name = "id") val id: Int,
    @Json(name = "name") val name: String
)

@JsonClass(generateAdapter = true)
data class WgerMuscleDto(
    @Json(name = "id") val id: Int,
    @Json(name = "name") val name: String = "",
    @Json(name = "name_en") val nameEn: String = "",
    @Json(name = "is_front") val isFront: Boolean = true,
    @Json(name = "image_url_main") val imageUrlMain: String? = null
)

@JsonClass(generateAdapter = true)
data class WgerEquipmentDto(
    @Json(name = "id") val id: Int,
    @Json(name = "name") val name: String
)

@JsonClass(generateAdapter = true)
data class WgerExerciseImageDto(
    @Json(name = "id") val id: Int,
    @Json(name = "image") val image: String,
    @Json(name = "is_main") val isMain: Boolean = false
)

@JsonClass(generateAdapter = true)
data class WgerExerciseInfoDto(
    @Json(name = "id") val id: Int,
    @Json(name = "name") val name: String,
    @Json(name = "description") val description: String = "",
    @Json(name = "category") val category: WgerCategoryDto? = null,
    @Json(name = "muscles") val muscles: List<WgerMuscleDto> = emptyList(),
    @Json(name = "muscles_secondary") val musclesSecondary: List<WgerMuscleDto> = emptyList(),
    @Json(name = "equipment") val equipment: List<WgerEquipmentDto> = emptyList(),
    @Json(name = "images") val images: List<WgerExerciseImageDto> = emptyList()
)

/**
 * Modelo de domínio limpo para apresentação de Exercícios no Tessera.
 */
data class WgerExercise(
    val id: Int,
    val name: String,
    val categoryId: Int,
    val categoryName: String,
    val description: String,
    val primaryMuscles: List<String> = emptyList(),
    val secondaryMuscles: List<String> = emptyList(),
    val equipment: List<String> = emptyList(),
    val mainImageUrl: String? = null,
    val allImageUrls: List<String> = emptyList()
)

/**
 * Categorias consolidadas para os chips da UI com nomes em português.
 */
data class MuscleCategoryFilter(
    val id: Int?,
    val displayName: String
)
