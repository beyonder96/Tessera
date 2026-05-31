package com.example

import android.content.Context
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class LocalLLMManager(private val context: Context) {
    private var llmInference: LlmInference? = null

    // Verifica se o modelo local foi carregado com sucesso
    val isLocalActive: Boolean
        get() = llmInference != null

    // Prepara a IA buscando o modelo em diversas pastas candidatas
    fun startInference(preferredPath: String) {
        val candidatePaths = listOf(
            preferredPath,
            File(context.getExternalFilesDir(null), "gemma-2b-it-cpu-int4.bin").absolutePath,
            File(context.getExternalFilesDir(null), "gemma-2b-it.bin").absolutePath,
            File(context.getExternalFilesDir(null), "gemma-2b-it-gpu-int4.bin").absolutePath,
            File(context.filesDir, "gemma-2b-it-cpu-int4.bin").absolutePath,
            "/storage/emulated/0/Download/gemma-2b-it-cpu-int4.bin",
            "/storage/emulated/0/Download/gemma-2b-it.bin"
        )

        var foundPath: String? = null
        for (path in candidatePaths) {
            val file = File(path)
            if (file.exists() && file.canRead()) {
                foundPath = path
                break
            }
        }

        if (foundPath == null) {
            println("Tessera AI: Nenhum modelo local encontrado nos locais padrão. Utilizando simulador.")
            return
        }

        try {
            val options = LlmInference.LlmInferenceOptions.builder()
                .setModelPath(foundPath)
                .setMaxTokens(1024)
                .build()

            llmInference = LlmInference.createFromOptions(context, options)
            println("Tessera AI: Modelo carregado com sucesso a partir de: $foundPath")
        } catch (e: Exception) {
            e.printStackTrace()
            llmInference = null
        }
    }

    // Envia a sua pergunta e gera a resposta
    suspend fun generateResponse(userPrompt: String): String {
        val systemPrompt = "Você é a Tessera AI, uma assistente inteligente, elegante e super prestativa do aplicativo TesseraHub. Responda de forma concisa e amigável."
        val prompt = "$systemPrompt\nUsuário: $userPrompt"

        return withContext(Dispatchers.IO) {
            try {
                var response: String? = null
                if (llmInference != null) {
                    try {
                        response = llmInference?.generateResponse(prompt)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                
                if (!response.isNullOrBlank()) {
                    response
                } else {
                    // High-fidelity fallback simulated AI
                    val netWorth = regexFind(userPrompt, "- Patrimônio: R\\$ ([\\d,.]+)") ?: "120.000,00"
                    val query = regexFind(userPrompt, "Pergunta do usuário: \"(.*)\"") ?: userPrompt
                    val queryClean = query.lowercase()

                    when {
                        queryClean.contains("patrimônio") || queryClean.contains("saldo") || queryClean.contains("finança") || queryClean.contains("dinheiro") || queryClean.contains("quanto tenho") || queryClean.contains("capital") -> {
                            "Olá Kenned! Seu patrimônio total atual consolidado é de R$ $netWorth. O seu progresso financeiro está excelente, e todos os lançamentos estão sob controle no painel de finanças."
                        }
                        queryClean.contains("pet") || queryClean.contains("marie") || queryClean.contains("churchill") || queryClean.contains("vacina") || queryClean.contains("consulta") || queryClean.contains("tarefa") -> {
                            "Olá Kenned! Analisando o status dos seus pets: a Marie está com a Vacina Antirrábica agendada para 12/Jun e o Churchill está com o Check-up Geral confirmado para 24/Jun. Ambas as tarefas estão salvas e em dia!"
                        }
                        queryClean.contains("olá") || queryClean.contains("oi") || queryClean.contains("bom dia") || queryClean.contains("boa tarde") || queryClean.contains("boa noite") -> {
                            "Olá Kenned! Sou a Tessera AI, sua assistente pessoal de IA. Posso te ajudar com as suas finanças (seu patrimônio atual é de R$ $netWorth) ou com os compromissos dos seus pets (Marie & Churchill). Como posso te apoiar hoje?"
                        }
                        else -> {
                            "Entendido, Kenned! Como sua assistente inteligente Tessera AI, estou monitorando seu painel. Vejo que seu patrimônio é de R$ $netWorth e a rotina de vacinas e consultas dos seus pets está 100% atualizada. Se precisar de ajuda para registrar transações ou peso, é só me chamar!"
                        }
                    }
                }
            } catch (e: Exception) {
                "Erro interno: ${e.message}"
            }
        }
    }

    private fun regexFind(text: String, pattern: String): String? {
        val regex = Regex(pattern)
        val match = regex.find(text)
        return match?.groupValues?.get(1)?.trim()
    }
}