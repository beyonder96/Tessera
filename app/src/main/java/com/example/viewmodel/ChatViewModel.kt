package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

class ChatViewModel : ViewModel() {
    
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

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

    private val systemInstruction = "Você é Tessera, uma assistente virtual exclusiva do aplicativo. Você DEVE responder APENAS a questões ligadas a Finanças, Pets, Apartamento (Apê) e Produtividade. Se o usuário perguntar sobre assuntos fora desse escopo, recuse educadamente informando que você só trata dos temas do aplicativo. Quando explicar sobre finanças, insira a tag [WIDGET:FINANCE]. Para pets, insira a tag [WIDGET:PETS]. Para apartamento, construção ou reforma, insira a tag [WIDGET:APARTMENT]. Seja concisa, prestativa e amigável."

    private var history = mutableListOf<GroqMessage>()

    init {
        // Initialize history with system message
        history.add(GroqMessage(role = "system", content = systemInstruction))
    }

    fun clearChat() {
        _messages.value = emptyList()
        history.clear()
        history.add(GroqMessage(role = "system", content = systemInstruction))
    }

    fun sendMessage(userMessage: String) {
        if (userMessage.isBlank()) return

        val newMsg = ChatMessage(text = userMessage, isUser = true)
        val loadingMsg = ChatMessage(text = "Pensando...", isUser = false, isLoading = true)
        
        _messages.value = _messages.value + newMsg + loadingMsg
        
        // Adiciona a mensagem do usuário ao histórico da API
        history.add(GroqMessage(role = "user", content = userMessage))

        viewModelScope.launch {
            try {
                val apiKey = com.example.BuildConfig.GROQ_API_KEY
                
                // Remove placeholder se não estiver configurado corretamente
                if (apiKey.isBlank() || apiKey == "gsk_your_groq_api_key_here") {
                    throw Exception("A chave da API do Groq não está configurada no .env (GROQ_API_KEY).")
                }

                val request = GroqRequest(
                    model = "llama3-8b-8192", // Modelo leve e muito rápido
                    messages = history.toList()
                )

                val response = groqApi.createChatCompletion("Bearer $apiKey", request)
                
                val responseText = response.choices.firstOrNull()?.message?.content ?: "Não consegui processar a resposta."
                
                // Adiciona a resposta do assistente ao histórico da API
                history.add(GroqMessage(role = "assistant", content = responseText))

                _messages.value = _messages.value.dropLast(1) + ChatMessage(text = responseText, isUser = false)
            } catch (e: Exception) {
                e.printStackTrace()
                val errorDetails = e.message ?: e.toString()
                
                // Se der erro, removemos a mensagem do usuário do histórico para ele poder tentar de novo
                history.removeLastOrNull()
                
                _messages.value = _messages.value.dropLast(1) + ChatMessage(
                    text = "Ocorreu um erro ao conectar com o Groq: $errorDetails",
                    isUser = false
                )
            }
        }
    }
}
