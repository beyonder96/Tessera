package com.example

import android.content.Context
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class LocalLLMManager(private val context: Context) {
    private var llmInference: LlmInference? = null

    // Prepara a IA carregando o arquivo do modelo
    fun startInference(modelPath: String) {
        val file = File(modelPath)
        if (!file.exists()) {
            println("Erro: Modelo não encontrado.")
            return
        }

        val options = LlmInference.LlmInferenceOptions.builder()
            .setModelPath(modelPath)
            .setMaxTokens(1024)
            .build()

        llmInference = LlmInference.createFromOptions(context, options)
    }

    // Envia a sua pergunta e gera a resposta
    suspend fun generateResponse(prompt: String): String {
        return withContext(Dispatchers.IO) {
            try {
                llmInference?.generateResponse(prompt) ?: "Erro: IA não iniciada."
            } catch (e: Exception) {
                "Erro: ${e.message}"
            }
        }
    }
}