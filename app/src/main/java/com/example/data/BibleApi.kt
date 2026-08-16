package com.example.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.http.GET
import retrofit2.http.Query

@JsonClass(generateAdapter = true)
data class ABibliaBook(
    @Json(name = "name") val name: String? = null,
    @Json(name = "version") val version: String? = "Almeida"
)

@JsonClass(generateAdapter = true)
data class BibleVerseDetail(
    @Json(name = "book_id") val bookId: String?,
    @Json(name = "book_name") val bookName: String?,
    @Json(name = "chapter") val chapter: Int?,
    @Json(name = "verse") val verse: Int?,
    @Json(name = "text") val text: String?
)

@JsonClass(generateAdapter = true)
data class BibleApiResponse(
    @Json(name = "reference") val reference: String?,
    @Json(name = "verses") val verses: List<BibleVerseDetail>?,
    @Json(name = "text") val text: String?,
    @Json(name = "translation_id") val translationId: String?,
    @Json(name = "translation_name") val translationName: String?
)

@JsonClass(generateAdapter = true)
data class BibleVerseResponse(
    @Json(name = "book") val book: ABibliaBook? = null,
    @Json(name = "chapter") val chapter: Int? = null,
    @Json(name = "number") val verse: Int? = null,
    @Json(name = "text") val text: String? = null
)

interface BibleApi {
    @GET("/")
    suspend fun getRandomVerse(
        @Query("random") random: String = "verse",
        @Query("translation") translation: String = "almeida"
    ): BibleApiResponse
}
