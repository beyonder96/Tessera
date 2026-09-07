package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ai.AiChatMessage
import com.example.data.ai.TesseraAiRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ChatUiState(
    val messages: List<AiChatMessage> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class ChatViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    fun sendMessage(content: String) {
        val trimmed = content.trim()
        if (trimmed.isBlank() || _uiState.value.isLoading) return

        val userMessage = AiChatMessage(role = "user", content = trimmed)
        val currentList = _uiState.value.messages + userMessage

        _uiState.value = _uiState.value.copy(
            messages = currentList,
            isLoading = true,
            error = null
        )

        viewModelScope.launch {
            val result = TesseraAiRepository.sendMessage(currentList)
            result.onSuccess { replyText ->
                val assistantMessage = AiChatMessage(role = "model", content = replyText)
                _uiState.value = _uiState.value.copy(
                    messages = currentList + assistantMessage,
                    isLoading = false,
                    error = null
                )
            }.onFailure { exception ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = exception.message ?: "Não foi possível obter resposta da Tessera AI."
                )
            }
        }
    }

    fun retryLastMessage() {
        val state = _uiState.value
        val lastUserMessage = state.messages.lastOrNull { it.role == "user" } ?: return
        val listWithoutTrailingModel = if (state.messages.lastOrNull()?.role == "model") {
            state.messages.dropLast(1)
        } else {
            state.messages
        }

        _uiState.value = state.copy(
            messages = listWithoutTrailingModel,
            isLoading = true,
            error = null
        )

        viewModelScope.launch {
            val result = TesseraAiRepository.sendMessage(listWithoutTrailingModel)
            result.onSuccess { replyText ->
                val assistantMessage = AiChatMessage(role = "model", content = replyText)
                _uiState.value = _uiState.value.copy(
                    messages = listWithoutTrailingModel + assistantMessage,
                    isLoading = false,
                    error = null
                )
            }.onFailure { exception ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = exception.message ?: "Erro ao tentar novamente."
                )
            }
        }
    }

    fun clearChat() {
        _uiState.value = ChatUiState()
    }
}
