package com.example.data.gemma

import android.content.Context
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

data class GemmaDownloadState(
    val isDownloading: Boolean = false,
    val progressPercent: Int = 0,
    val bytesDownloadedMB: Double = 0.0,
    val totalBytesMB: Double = 0.0,
    val isDownloaded: Boolean = false,
    val errorMessage: String? = null
)

class GemmaLocalManager(private val context: Context) {

    private val _downloadState = MutableStateFlow(GemmaDownloadState())
    val downloadState: StateFlow<GemmaDownloadState> = _downloadState.asStateFlow()

    private var llmInference: LlmInference? = null

    val modelFile: File
        get() = File(context.filesDir, "models/gemma-2b-it.bin")

    init {
        checkModelStatus()
    }

    fun checkModelStatus() {
        val exists = modelFile.exists() && modelFile.length() > 50 * 1024 * 1024 // > 50MB
        _downloadState.value = _downloadState.value.copy(
            isDownloaded = exists,
            bytesDownloadedMB = if (exists) modelFile.length().toDouble() / (1024 * 1024) else 0.0
        )
    }

    suspend fun downloadGemmaModel(modelUrl: String) = withContext(Dispatchers.IO) {
        if (_downloadState.value.isDownloading) return@withContext

        val targetDir = File(context.filesDir, "models")
        if (!targetDir.exists()) targetDir.mkdirs()

        _downloadState.value = GemmaDownloadState(
            isDownloading = true,
            progressPercent = 0,
            errorMessage = null
        )

        val client = OkHttpClient.Builder().build()
        try {
            val request = Request.Builder().url(modelUrl).build()
            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                throw Exception("Falha ao baixar o modelo Gemma (Código HTTP: ${response.code})")
            }

            val body = response.body ?: throw Exception("Resposta vazia ao baixar Gemma")
            val totalBytes = body.contentLength()
            val totalMB = if (totalBytes > 0) totalBytes.toDouble() / (1024 * 1024) else 1350.0

            val inputStream: InputStream = body.byteStream()
            val outputFile = modelFile
            val outputStream = FileOutputStream(outputFile)

            val buffer = ByteArray(32 * 1024)
            var bytesRead: Int
            var downloadedBytes: Long = 0

            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
                downloadedBytes += bytesRead

                val downloadedMB = downloadedBytes.toDouble() / (1024 * 1024)
                val percent = if (totalBytes > 0) ((downloadedBytes * 100) / totalBytes).toInt() else ((downloadedMB / totalMB) * 100).toInt().coerceIn(0, 99)

                _downloadState.value = GemmaDownloadState(
                    isDownloading = true,
                    progressPercent = percent,
                    bytesDownloadedMB = downloadedMB,
                    totalBytesMB = totalMB,
                    isDownloaded = false
                )
            }

            outputStream.flush()
            outputStream.close()
            inputStream.close()

            _downloadState.value = GemmaDownloadState(
                isDownloading = false,
                progressPercent = 100,
                bytesDownloadedMB = downloadedBytes.toDouble() / (1024 * 1024),
                totalBytesMB = downloadedBytes.toDouble() / (1024 * 1024),
                isDownloaded = true
            )
        } catch (e: Exception) {
            e.printStackTrace()
            _downloadState.value = GemmaDownloadState(
                isDownloading = false,
                errorMessage = "Erro ao baixar modelo Gemma: ${e.message}"
            )
        }
    }

    fun deleteModel() {
        if (modelFile.exists()) {
            modelFile.delete()
        }
        llmInference = null
        checkModelStatus()
    }

    suspend fun generateLocalResponse(
        prompt: String,
        temperature: Float = 0.7f,
        topK: Int = 40,
        maxTokens: Int = 1024
    ): String = withContext(Dispatchers.IO) {
        if (!modelFile.exists()) {
            throw Exception("O modelo Gemma local não foi baixado ainda. Faça o download nas configurações do chat.")
        }

        try {
            if (llmInference == null) {
                val options = LlmInference.LlmInferenceOptions.builder()
                    .setModelPath(modelFile.absolutePath)
                    .setMaxTokens(maxTokens)
                    .setTopK(topK)
                    .setTemperature(temperature)
                    .build()

                llmInference = LlmInference.createFromOptions(context, options)
            }

            return@withContext llmInference?.generateResponse(prompt)
                ?: throw Exception("Não foi possível gerar a resposta com o Gemma local.")
        } catch (e: Exception) {
            e.printStackTrace()
            throw Exception("Erro de inferência Gemma local: ${e.message}")
        }
    }
}
