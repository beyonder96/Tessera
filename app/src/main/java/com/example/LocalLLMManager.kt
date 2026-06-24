package com.example

import android.content.Context
import com.google.ai.client.generativeai.GenerativeModel
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

class LocalLLMManager(private val context: Context) {
    private var generativeModel: GenerativeModel? = null
    private var llmInference: LlmInference? = null
    private var isGemmaLoaded = false

    val isLocalActive: Boolean
        get() = isGemmaLoaded

    private val _diagnosticStatus = MutableStateFlow("Iniciando Tessera AI...")
    val diagnosticStatus: StateFlow<String> = _diagnosticStatus.asStateFlow()

    fun startInference(preferredPath: String) {
        // 1. Tentar inicializar o Gemma local (MediaPipe LlmInference)
        try {
            val modelFile = File(preferredPath)
            if (modelFile.exists()) {
                val options = LlmInference.LlmInferenceOptions.builder()
                    .setModelPath(preferredPath)
                    .setMaxTokens(512)
                    .setTemperature(0.7f)
                    .build()
                
                llmInference = LlmInference.createFromOptions(context, options)
                isGemmaLoaded = true
                _diagnosticStatus.value = "Gemma 2B Local carregado com sucesso!"
                println("Tessera AI: Gemma Local inicializado com o arquivo $preferredPath")
                return // Gemma carregado com sucesso, não inicializa Gemini online
            } else {
                println("Tessera AI: Arquivo do Gemma local não encontrado em: $preferredPath. Usando fallback.")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            println("Tessera AI: Falha ao carregar Gemma local: ${e.message}")
        }

        // 2. Fallback para Gemini online
        try {
            val clazz = Class.forName("com.example.BuildConfig")
            val field = clazz.getField("GEMINI_API_KEY")
            val apiKey = field.get(null) as? String ?: ""
            
            if (apiKey.isNotBlank() && apiKey != "MY_GEMINI_API_KEY") {
                generativeModel = GenerativeModel(
                    modelName = "gemini-1.5-flash",
                    apiKey = apiKey
                )
                _diagnosticStatus.value = "Conectado ao Gemini (Fallback Online)"
                println("Tessera AI: Gemini (Fallback) carregado com sucesso.")
            } else {
                _diagnosticStatus.value = "Gemma Local ausente (arquivo não encontrado) e sem Chave do Gemini."
                println("Tessera AI: Sem modelo local ou chave do Gemini.")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            _diagnosticStatus.value = "Erro no carregamento: ${e.message}"
        }
    }

    suspend fun generateResponse(userPrompt: String): String {
        val systemPrompt = """
            Você é a Tessera AI, uma companheira de conversação versátil, inteligente, elegante, natural e amigável integrada ao aplicativo Tessera do Kenned.
            Você é capaz de conversar de forma fluida sobre qualquer assunto casual, responder curiosidades e debater temas gerais com naturalidade e flexibilidade.
            Você tem acesso ao contexto local do usuário, mas deve consultar e cruzar discretamente esses dados locais APENAS quando o assunto da conversa for relevante a eles.
            Evite listar ou detalhar esses dados locais de forma intrusiva se o usuário estiver apenas jogando conversa fora.
            Seja sempre direta, concisa, fluida, natural e amigável. Responda sempre em português brasileiro.
        """.trimIndent()
        
        val prompt = "$systemPrompt\n\n$userPrompt"

        return withContext(Dispatchers.IO) {
            try {
                // Tenta usar Gemma local se estiver ativo e carregado
                val localEngine = llmInference
                if (isGemmaLoaded && localEngine != null) {
                    val response = localEngine.generateResponse(prompt)
                    return@withContext response ?: "Desculpe, não consegui formular uma resposta local agora."
                }

                // Senão tenta usar Gemini como fallback
                if (generativeModel != null) {
                    val response = generativeModel?.generateContent(prompt)
                    response?.text ?: "Desculpe, não consegui formular uma resposta agora."
                } else {
                    "Olá! Parece que não consegui encontrar o arquivo do Gemma 2B no seu celular para rodar localmente. \n\n" +
                    "Para o Android permitir que eu leia o modelo offline, por favor, mova o arquivo '.bin' do Gemma para a pasta do aplicativo:\n" +
                    "📂 Android/data/com.example/files/\n\n" +
                    "Ou, se preferir usar a nuvem (Gemini), insira sua chave da API nas Configurações do app."
                }
            } catch (e: Exception) {
                e.printStackTrace()
                "Houve um erro na comunicação: ${e.localizedMessage}"
            }
        }
    }
}