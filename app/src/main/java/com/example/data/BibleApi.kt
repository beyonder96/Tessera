package com.example.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.http.GET

@JsonClass(generateAdapter = true)
data class ABibliaBook(
    @Json(name = "name") val name: String?,
    @Json(name = "version") val version: String?
)

@JsonClass(generateAdapter = true)
data class BibleVerseResponse(
    @Json(name = "book") val book: ABibliaBook?,
    @Json(name = "chapter") val chapter: Int?,
    @Json(name = "number") val verse: Int?,
    @Json(name = "text") val text: String?
)

interface BibleApi {
    @GET("verses/nvt/random")
    suspend fun getRandomVerse(): BibleVerseResponse
}
