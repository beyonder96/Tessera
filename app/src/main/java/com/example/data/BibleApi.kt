package com.example.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

// ==========================================
// 1. Versões Bíblicas
// ==========================================
@JsonClass(generateAdapter = true)
data class BibliaVersionItem(
    @Json(name = "code") val code: String,
    @Json(name = "copyright") val copyright: String? = null,
    @Json(name = "permissions") val permissions: String? = null,
    @Json(name = "language") val language: String? = "pt-BR"
)

@JsonClass(generateAdapter = true)
data class BibliaVersionsResponse(
    @Json(name = "data") val data: List<BibliaVersionItem> = emptyList()
)

// ==========================================
// 2. Livros Bíblicos
// ==========================================
@JsonClass(generateAdapter = true)
data class BibliaBookItem(
    @Json(name = "id") val id: Int,
    @Json(name = "name") val name: String,
    @Json(name = "abbrev") val abbrev: String,
    @Json(name = "testament") val testament: String // "VT" ou "NT"
)

@JsonClass(generateAdapter = true)
data class BibliaBooksResponse(
    @Json(name = "data") val data: List<BibliaBookItem> = emptyList()
)

// ==========================================
// 3. Versículos e Capítulos
// ==========================================
@JsonClass(generateAdapter = true)
data class BibliaVerseItem(
    @Json(name = "number") val number: Int,
    @Json(name = "text") val text: String
)

@JsonClass(generateAdapter = true)
data class BibliaChapterInfo(
    @Json(name = "number") val number: Int,
    @Json(name = "verses") val totalVerses: Int? = null
)

@JsonClass(generateAdapter = true)
data class BibliaChapterData(
    @Json(name = "reference") val reference: String? = null,
    @Json(name = "version") val version: String? = null,
    @Json(name = "book") val book: BibliaBookItem? = null,
    @Json(name = "chapter") val chapter: BibliaChapterInfo? = null,
    @Json(name = "verses") val verses: List<BibliaVerseItem> = emptyList()
)

@JsonClass(generateAdapter = true)
data class BibliaChapterResponse(
    @Json(name = "data") val data: BibliaChapterData
)

// ==========================================
// 4. Versículo Aleatório / Versículo do Dia
// ==========================================
@JsonClass(generateAdapter = true)
data class BibliaRandomVerseData(
    @Json(name = "reference") val reference: String? = null,
    @Json(name = "version") val version: String? = null,
    @Json(name = "book") val book: BibliaBookItem? = null,
    @Json(name = "chapter") val chapter: Int? = null,
    @Json(name = "verse") val verse: Int? = null,
    @Json(name = "text") val text: String? = null
)

@JsonClass(generateAdapter = true)
data class BibliaRandomVerseResponse(
    @Json(name = "data") val data: BibliaRandomVerseData
)

// ==========================================
// 5. Modelo Legado Compatível para UI existente
// ==========================================
@JsonClass(generateAdapter = true)
data class ABibliaBook(
    @Json(name = "name") val name: String? = null,
    @Json(name = "version") val version: String? = "Almeida"
)

@JsonClass(generateAdapter = true)
data class BibleVerseResponse(
    @Json(name = "book") val book: ABibliaBook? = null,
    @Json(name = "chapter") val chapter: Int? = null,
    @Json(name = "number") val verse: Int? = null,
    @Json(name = "text") val text: String? = null,
    @Json(name = "bookAbbrev") val bookAbbrev: String? = null,
    @Json(name = "versionCode") val versionCode: String? = "NVT"
)

// ==========================================
// 6. Retrofit API Interface (BibliaApi BR)
// ==========================================
interface BibleApi {
    @GET("versions")
    suspend fun getVersions(): BibliaVersionsResponse

    @GET("books")
    suspend fun getBooks(): BibliaBooksResponse

    @GET("versions/{version}/random")
    suspend fun getRandomVerse(
        @Path("version") version: String
    ): BibliaRandomVerseResponse

    @GET("versions/{version}/books/{book}/chapters/{chapter}")
    suspend fun getChapter(
        @Path("version") version: String,
        @Path("book") book: String,
        @Path("chapter") chapter: Int
    ): BibliaChapterResponse

    @GET("versions/{version}/search")
    suspend fun search(
        @Path("version") version: String,
        @Query("q") query: String
    ): BibliaChapterResponse
}

// ==========================================
// 7. Open Bible API (bible-api.com - Sem Token / Resiliente)
// ==========================================
@JsonClass(generateAdapter = true)
data class OpenBibleVerseItem(
    @Json(name = "book_id") val bookId: String? = null,
    @Json(name = "book_name") val bookName: String? = null,
    @Json(name = "chapter") val chapter: Int? = 1,
    @Json(name = "verse") val verse: Int = 1,
    @Json(name = "text") val text: String = ""
)

@JsonClass(generateAdapter = true)
data class OpenBibleResponse(
    @Json(name = "reference") val reference: String? = null,
    @Json(name = "verses") val verses: List<OpenBibleVerseItem> = emptyList(),
    @Json(name = "text") val text: String? = null,
    @Json(name = "translation_id") val translationId: String? = null,
    @Json(name = "translation_name") val translationName: String? = null
)

interface OpenBibleApi {
    @GET("{passage}")
    suspend fun getPassage(
        @Path("passage") passage: String,
        @Query("translation") translation: String = "almeida"
    ): OpenBibleResponse
}

