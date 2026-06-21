package com.example

import android.content.Context
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class LocalLLMManager(private val context: Context) {
    private var generativeModel: GenerativeModel? = null

    val isLocalActive: Boolean
        get() = true

    private val _diagnosticStatus = MutableStateFlow("Iniciando Gemini...")
    val diagnosticStatus: StateFlow<String> = _diagnosticStatus.asStateFlow()

    fun startInference(preferredPath: String) {
        try {
            val clazz = Class.forName("com.example.BuildConfig")
            val field = clazz.getField("GEMINI_API_KEY")
            val apiKey = field.get(null) as? String ?: ""
            
            if (apiKey.isNotBlank() && apiKey != "MY_GEMINI_API_KEY") {
                generativeModel = GenerativeModel(
                    modelName = "gemini-1.5-flash",
                    apiKey = apiKey
                )
                _diagnosticStatus.value = "Gemini carregado com sucesso!"
                println("Tessera AI: Gemini carregado com sucesso.")
            } else {
                _diagnosticStatus.value = "API Key do Gemini inválida ou não encontrada."
                println("Tessera AI: API Key ausente.")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            _diagnosticStatus.value = "Erro ao carregar Gemini: ${e.message}"
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
                if (generativeModel != null) {
                    val response = generativeModel?.generateContent(prompt)
                    response?.text ?: "Desculpe, não consegui formular uma resposta agora."
                } else {
                    "Olá, Kenned! Parece que minha conexão com o Gemini não foi estabelecida. Verifique a chave de API."
                }
            } catch (e: Exception) {
                e.printStackTrace()
                "Houve um erro na comunicação: ${e.localizedMessage}"
            }
        }
    }
}