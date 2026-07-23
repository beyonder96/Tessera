package com.example.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.gemma.GemmaDownloadState
import com.example.data.gemma.GemmaLocalManager
import com.example.data.groq.GroqApi
import com.example.data.groq.GroqMessage
import com.example.data.groq.GroqRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val text: String,
    val isUser: Boolean,
    val isLoading: Boolean = false
)

class ChatViewModel(application: Application) : AndroidViewModel(application) {
    
    private val prefs = application.getSharedPreferences("tessera_gemma_prefs", Context.MODE_PRIVATE)

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val gemmaLocalManager = GemmaLocalManager(application)
    val downloadState: StateFlow<GemmaDownloadState> = gemmaLocalManager.downloadState

    val useLocalGemma = MutableStateFlow(prefs.getBoolean("use_local_gemma", false))
    val gemmaModelUrl = MutableStateFlow(
        prefs.getString(
            "gemma_model_url",
            "https://huggingface.co/google/gemma-2b-it-gpu-int4/resolve/main/gemma-2b-it-gpu-int4.bin"
        ) ?: "https://huggingface.co/google/gemma-2b-it-gpu-int4/resolve/main/gemma-2b-it-gpu-int4.bin"
    )
    val systemPrompt = MutableStateFlow(
        prefs.getString(
            "gemma_system_prompt",
            "Você é Tessera, uma assistente virtual exclusiva do aplicativo. Você DEVE responder APENAS a questões ligadas a Finanças, Pets, Apartamento (Apê), Saúde e Produtividade. Quando explicar sobre finanças, insira a tag [WIDGET:FINANCE]. Para pets, insira a tag [WIDGET:PETS]. Para apartamento, insira a tag [WIDGET:APARTMENT]. Para saúde, insira a tag [WIDGET:HEALTH]. Seja concisa, prestativa e amigável."
        ) ?: ""
    )
    val temperature = MutableStateFlow(prefs.getFloat("gemma_temperature", 0.7f))
    val topK = MutableStateFlow(prefs.getInt("gemma_top_k", 40))
    val maxTokens = MutableStateFlow(prefs.getInt("gemma_max_tokens", 1024))

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://api.groq.com/")
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    private val groqApi = retrofit.create(GroqApi::class.java)

    private var history = mutableListOf<GroqMessage>()

    init {
        history.add(GroqMessage(role = "system", content = systemPrompt.value))
    }

    fun setUseLocalGemma(value: Boolean) {
        useLocalGemma.value = value
        prefs.edit().putBoolean("use_local_gemma", value).apply()
    }

    fun updateModelUrl(url: String) {
        gemmaModelUrl.value = url
        prefs.edit().putString("gemma_model_url", url).apply()
    }

    fun updateSystemParameters(prompt: String, temp: Float, k: Int, maxT: Int) {
        systemPrompt.value = prompt
        temperature.value = temp
        topK.value = k
        maxTokens.value = maxT

        prefs.edit()
            .putString("gemma_system_prompt", prompt)
            .putFloat("gemma_temperature", temp)
            .putInt("gemma_top_k", k)
            .putInt("gemma_max_tokens", maxT)
            .apply()

        clearChat()
    }

    fun downloadGemmaModel() {
        viewModelScope.launch {
            gemmaLocalManager.downloadGemmaModel(gemmaModelUrl.value)
        }
    }

    fun deleteGemmaModel() {
        gemmaLocalManager.deleteModel()
    }

    fun clearChat() {
        _messages.value = emptyList()
        history.clear()
        history.add(GroqMessage(role = "system", content = systemPrompt.value))
    }

    fun sendMessage(userMessage: String) {
        if (userMessage.isBlank()) return

        val newMsg = ChatMessage(text = userMessage, isUser = true)
        val loadingMsg = ChatMessage(text = "Pensando...", isUser = false, isLoading = true)
        
        _messages.value = _messages.value + newMsg + loadingMsg
        history.add(GroqMessage(role = "user", content = userMessage))

        viewModelScope.launch {
            if (useLocalGemma.value && gemmaLocalManager.modelFile.exists()) {
                // Inferência On-Device com Gemma Local
                try {
                    val fullPrompt = "${systemPrompt.value}\n\nUsuário: $userMessage\nTessera:"
                    val responseText = gemmaLocalManager.generateLocalResponse(
                        prompt = fullPrompt,
                        temperature = temperature.value,
                        topK = topK.value,
                        maxTokens = maxTokens.value
                    )
                    history.add(GroqMessage(role = "assistant", content = responseText))
                    _messages.value = _messages.value.dropLast(1) + ChatMessage(text = responseText, isUser = false)
                } catch (e: Exception) {
                    e.printStackTrace()
                    _messages.value = _messages.value.dropLast(1) + ChatMessage(
                        text = "Erro na inferência do Gemma local: ${e.message}. Verifique o modelo baixado.",
                        isUser = false
                    )
                }
            } else {
                // Inferência Cloud (Groq Gemma 2 / Llama 3)
                try {
                    val apiKey = com.example.BuildConfig.GROQ_API_KEY
                    if (apiKey.isBlank() || apiKey == "gsk_your_groq_api_key_here") {
                        throw Exception("Configure a GROQ_API_KEY no arquivo .env ou faça o download do modelo Gemma local.")
                    }

                    val request = GroqRequest(
                        model = "gemma2-9b-it", // Modelo Gemma 2 oficial do Groq Cloud
                        messages = history.toList()
                    )

                    val response = groqApi.createChatCompletion("Bearer $apiKey", request)
                    val responseText = response.choices.firstOrNull()?.message?.content ?: "Não consegui obter a resposta do Gemma."

                    history.add(GroqMessage(role = "assistant", content = responseText))
                    _messages.value = _messages.value.dropLast(1) + ChatMessage(text = responseText, isUser = false)
                } catch (e: Exception) {
                    e.printStackTrace()
                    val errorDetails = e.message ?: e.toString()
                    history.removeLastOrNull()
                    _messages.value = _messages.value.dropLast(1) + ChatMessage(
                        text = "Ocorreu um erro ao conectar ao assistente: $errorDetails",
                        isUser = false
                    )
                }
            }
        }
    }
}
