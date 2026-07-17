package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val text: String,
    val isUser: Boolean,
    val isLoading: Boolean = false
)

class ChatViewModel : ViewModel() {
    
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val generativeModel = GenerativeModel(
        modelName = "gemini-2.5-flash",
        apiKey = com.example.BuildConfig.GEMINI_API_KEY,
        systemInstruction = content { text("Você é Tessera, uma assistente virtual exclusiva do aplicativo. Você DEVE responder APENAS a questões ligadas a Finanças, Pets, Apartamento (Apê) e Produtividade. Se o usuário perguntar sobre assuntos fora desse escopo, recuse educadamente informando que você só trata dos temas do aplicativo. Quando explicar sobre finanças, insira a tag [WIDGET:FINANCE]. Para pets, insira a tag [WIDGET:PETS]. Para apartamento, construção ou reforma, insira a tag [WIDGET:APARTMENT]. Seja concisa, prestativa e amigável.") }
    )
    
    private val chat = generativeModel.startChat()

    init {
        // Welcome message removida para mostrar o WelcomeScreen customizado inicial
    }

    fun clearChat() {
        _messages.value = emptyList()
        // O ideal seria resetar a sessão do Gemini também, mas manteremos o StateFlow limpo para fins de UI
    }

    fun sendMessage(userMessage: String) {
        if (userMessage.isBlank()) return

        val newMsg = ChatMessage(text = userMessage, isUser = true)
        val loadingMsg = ChatMessage(text = "Pensando...", isUser = false, isLoading = true)
        
        _messages.value = _messages.value + newMsg + loadingMsg

        viewModelScope.launch {
            try {
                val response = chat.sendMessage(userMessage)
                val responseText = response.text ?: "Não consegui processar a resposta."
                
                _messages.value = _messages.value.dropLast(1) + ChatMessage(text = responseText, isUser = false)
            } catch (e: Exception) {
                e.printStackTrace()
                val errorDetails = e.message ?: e.toString()
                _messages.value = _messages.value.dropLast(1) + ChatMessage(
                    text = "Ocorreu um erro ao conectar com o Gemini: $errorDetails",
                    isUser = false
                )
            }
        }
    }
}
