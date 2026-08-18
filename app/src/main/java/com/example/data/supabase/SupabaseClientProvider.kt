package com.example.data.supabase

import android.content.Context
import android.util.Log
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

object SupabaseClientProvider {
    private const val TAG = "SupabaseClient"

    // Default Supabase configuration (Pode ser substituído em runtime ou por BuildConfig)
    private var customUrl: String? = null
    private var customKey: String? = null

    val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    fun init(context: Context) {
        val prefs = context.getSharedPreferences("tessera_supabase_prefs", Context.MODE_PRIVATE)
        customUrl = prefs.getString("supabase_url", null)
        customKey = prefs.getString("supabase_anon_key", null)
    }

    fun getSupabaseUrl(): String {
        return customUrl ?: com.example.BuildConfig.SUPABASE_URL.takeIf { it.isNotBlank() && !it.contains("MY_") } ?: "https://hyoveowiisbigcpxzoro.supabase.co"
    }

    fun getSupabaseAnonKey(): String {
        return customKey ?: com.example.BuildConfig.SUPABASE_ANON_KEY.takeIf { it.isNotBlank() && !it.contains("MY_") } ?: "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Imh5b3Zlb3dpaXNiaWdjcHh6b3JvIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODY5MTg2OTcsImV4cCI6MjEwMjQ5NDY5N30.FFHC_1r0d2buwOgElxp99RP10NrmjrOiE74F0zTW1T4"
    }

    fun getWebBaseUrl(): String {
        return "https://tessera-35c54.web.app"
    }

    fun updateCredentials(context: Context, url: String, key: String) {
        customUrl = url.trim().trimEnd('/')
        customKey = key.trim()
        val prefs = context.getSharedPreferences("tessera_supabase_prefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putString("supabase_url", customUrl)
            .putString("supabase_anon_key", customKey)
            .apply()
    }

    fun isConfigured(): Boolean {
        val url = getSupabaseUrl()
        val key = getSupabaseAnonKey()
        return url.isNotBlank() && !url.contains("placeholder") && key.isNotBlank() && !key.contains("placeholder")
    }

    suspend fun postOrUpdate(table: String, jsonBody: String): Result<String> {
        return try {
            val url = "${getSupabaseUrl()}/rest/v1/$table?on_conflict=id"
            val body = jsonBody.toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", getSupabaseAnonKey())
                .addHeader("Authorization", "Bearer ${getSupabaseAnonKey()}")
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "resolution=merge-duplicates,return=representation")
                .post(body)
                .build()

            val response = client.newCall(request).execute()
            val responseString = response.body?.string() ?: ""
            if (response.isSuccessful) {
                Result.success(responseString)
            } else {
                Log.w(TAG, "Supabase HTTP error ${response.code}: $responseString")
                Result.failure(Exception("HTTP ${response.code}: $responseString"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error contacting Supabase: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun getDocument(table: String, id: String): Result<String> {
        return try {
            val url = "${getSupabaseUrl()}/rest/v1/$table?id=eq.$id&select=*"
            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", getSupabaseAnonKey())
                .addHeader("Authorization", "Bearer ${getSupabaseAnonKey()}")
                .get()
                .build()

            val response = client.newCall(request).execute()
            val responseString = response.body?.string() ?: ""
            if (response.isSuccessful) {
                Result.success(responseString)
            } else {
                Result.failure(Exception("HTTP ${response.code}: $responseString"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
