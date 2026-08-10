package com.example.test

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.coroutines.runBlocking
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import org.junit.Test

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

class BibleApiTest {
    @Test
    fun testBibleApi() = runBlocking {
        try {
            val bibleRetrofit = Retrofit.Builder()
                .baseUrl("https://bolls.life/")
                .addConverterFactory(MoshiConverterFactory.create())
                .build()

            val api = bibleRetrofit.create(BibleApi::class.java)
            val response = api.getRandomVerse()
            println("Success: $response")
        } catch (e: Exception) {
            println("Error: ${e.message}")
            e.printStackTrace()
            throw e
        }
    }
}
