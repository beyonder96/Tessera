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
        modelName = "gemini-1.5-flash",
        apiKey = com.example.BuildConfig.GEMINI_API_KEY,
        systemInstruction = content { text("Você é Tessera, uma assistente virtual focada em produtividade, finanças e bem-estar. Seja concisa, prestativa e amigável.") }
    )
    
    private val chat = generativeModel.startChat()

    init {
        // Welcome message
        _messages.value = listOf(
            ChatMessage(
                text = "Olá! Eu sou a Tessera. Como posso ajudar no seu dia de hoje?",
                isUser = false
            )
        )
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
                _messages.value = _messages.value.dropLast(1) + ChatMessage(
                    text = "Ocorreu um erro ao conectar com o Gemini. Verifique sua chave de API e conexão.",
                    isUser = false
                )
            }
        }
    }
}
