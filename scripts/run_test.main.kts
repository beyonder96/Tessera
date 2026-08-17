@file:Repository("https://repo1.maven.org/maven2/")
@file:DependsOn("com.squareup.retrofit2:retrofit:2.9.0")
@file:DependsOn("com.squareup.retrofit2:converter-moshi:2.9.0")
@file:DependsOn("com.squareup.moshi:moshi-kotlin:1.15.0")
@file:DependsOn("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")

import com.squareup.moshi.Json
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.runBlocking
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET

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

runBlocking {
    try {
        val bibleMoshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
            
        val bibleRetrofit = Retrofit.Builder()
            .baseUrl("https://bolls.life/")
            .addConverterFactory(MoshiConverterFactory.create(bibleMoshi))
            .build()

        val api = bibleRetrofit.create(BibleApi::class.java)
        val response = api.getRandomVerse()
        println("SUCCESS! $response")
    } catch(e: Exception) {
        println("FAILED!")
        e.printStackTrace()
    }
}
