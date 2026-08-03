package com.example.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.http.GET

@JsonClass(generateAdapter = true)
data class BibleVerseResponse(
    @Json(name = "pk") val pk: Int?,
    @Json(name = "translation") val translation: String?,
    @Json(name = "book") val book: Int?,
    @Json(name = "chapter") val chapter: Int?,
    @Json(name = "verse") val verse: Int?,
    @Json(name = "text") val text: String?
)

interface BibleApi {
    @GET("get-random-verse/NVT/")
    suspend fun getRandomVerse(): BibleVerseResponse
}
