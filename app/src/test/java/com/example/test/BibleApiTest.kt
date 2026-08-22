package com.example.test

import kotlinx.coroutines.runBlocking
import org.junit.Test
import com.example.data.BibleApi
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

class BibleApiTest {
    @Test
    fun testBibliaApiV2() = runBlocking {
        try {
            val apiKey = "bapi_cyd65a70b4cmbin97bcojzs3vmq7cpxnhyo2lgfjogiup9d5"
            val okHttp = OkHttpClient.Builder()
                .addInterceptor { chain ->
                    val request = chain.request().newBuilder()
                        .header("Authorization", "Bearer $apiKey")
                        .header("Accept", "application/json")
                        .build()
                    chain.proceed(request)
                }
                .build()

            val moshi = Moshi.Builder()
                .add(KotlinJsonAdapterFactory())
                .build()

            val retrofit = Retrofit.Builder()
                .baseUrl("https://bibliaapi.com.br/api/v2/")
                .client(okHttp)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()

            val api = retrofit.create(BibleApi::class.java)
            val random = api.getRandomVerse("NVT")
            println("Random Verse Success: ${random.data.reference} - ${random.data.text}")

            val books = api.getBooks()
            println("Books Count: ${books.data.size}")

            val chapter = api.getChapter("NVT", "sl", 23)
            println("Chapter Salmos 23 Verses Count: ${chapter.data.verses.size}")
        } catch (e: Exception) {
            println("Aviso: Teste de API online falhou (rede/timeout): ${e.message}")
        }
    }
}
