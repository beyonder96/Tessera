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
        // Garante a criação da pasta de arquivos externos do app
        try {
            context.getExternalFilesDir(null)?.mkdirs()
        } catch (e: Exception) {
            e.printStackTrace()
        }

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
        val systemPrompt = """
            Você é a Tessera AI, uma assistente pessoal inteligente, elegante, super prestativa e integrada ao aplicativo Tessera do Kenned.
            Você tem acesso em tempo real ao contexto local do usuário:
            - Patrimônio Consolidado (Finanças)
            - Compromissos e vacinas dos Pets
            - Lista de compras do Mercado
            - Medicamentos agendados e status de ingestão
            
            Use essas tabelas ativamente para responder:
            - Se o usuário perguntar ou mencionar compras, receitas, ingredientes ou mercado, analise a lista de mercado e liste os itens e quantidades pendentes ou comprados. Sugira fazer as compras dos itens pendentes.
            - Se o usuário perguntar sobre saúde, remédios, medicação ou horários de dosagem, analise os medicamentos do dia e faça alertas explícitos se houver algum pendente, ou parabenize-o se todos já foram tomados.
            - Seja sempre direta, concisa, elegante e amigável. Responda em português brasileiro.
        """.trimIndent()
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
                    // High-fidelity fallback simulated AI reading local context dynamically
                    val netWorth = regexFind(userPrompt, "\\[Contexto\\] Patrimônio consolidado: R\\$ ([\\d,.]+)") ?: "0,00"
                    val petsContext = regexFind(userPrompt, "\\[Contexto\\] Compromissos dos Pets: (.*)") ?: "Nenhum compromisso pendente hoje."
                    val marketContext = regexFind(userPrompt, "\\[Contexto\\] Lista de Compras \\(Mercado\\): (.*)") ?: "Nenhuma compra pendente."
                    val medsContext = regexFind(userPrompt, "\\[Contexto\\] Medicamentos e Remédios: (.*)") ?: "Nenhum medicamento agendado."
                    val query = regexFind(userPrompt, "Pergunta do usuário: \"(.*)\"") ?: userPrompt
                    val queryClean = query.lowercase()

                    when {
                        queryClean.contains("patrimônio") || queryClean.contains("saldo") || queryClean.contains("finança") || queryClean.contains("dinheiro") || queryClean.contains("quanto tenho") || queryClean.contains("capital") -> {
                            "Olá Kenned! Analisei suas finanças locais: seu patrimônio consolidado atual é de R$ $netWorth. Seu saldo e seus lançamentos estão sob controle no painel financeiro do app."
                        }
                        queryClean.contains("compra") || queryClean.contains("mercado") || queryClean.contains("adquirir") || queryClean.contains("comprar") || queryClean.contains("ingrediente") || queryClean.contains("receita") -> {
                            val pendingItems = marketContext.split("; ")
                                .filter { it.contains("Pendente") }
                                .map { it.substringBefore(" (Pendente)") }
                            
                            val advice = if (pendingItems.isNotEmpty()) {
                                "Vejo que você tem os seguintes itens pendentes na sua lista de compras de mercado: ${pendingItems.joinToString(", ")}. Recomendo comprá-los na sua próxima ida ao mercado!"
                            } else {
                                "Todos os itens da sua lista de mercado já foram marcados como comprados!"
                            }
                            "Olá Kenned! $advice"
                        }
                        queryClean.contains("remédio") || queryClean.contains("medicamento") || queryClean.contains("saúde") || queryClean.contains("dose") || queryClean.contains("tomar") -> {
                            val pendingMeds = medsContext.split("; ")
                                .filter { it.contains("Pendente") }
                                .map { it.substringBefore(" - Pendente") }
                            
                            val advice = if (pendingMeds.isNotEmpty()) {
                                "Atenção, Kenned! Você tem medicamentos pendentes para hoje: ${pendingMeds.joinToString(", ")}. Lembre-se de tomá-los no horário correto para manter seu tratamento em dia!"
                            } else {
                                "Parabéns, Kenned! Todos os seus medicamentos agendados para hoje já constam como tomados."
                            }
                            "Olá Kenned! $advice"
                        }
                        queryClean.contains("pet") || queryClean.contains("marie") || queryClean.contains("churchill") || queryClean.contains("vacina") || queryClean.contains("consulta") || queryClean.contains("rotina") -> {
                            "Olá Kenned! Analisando o status dos seus pets: $petsContext."
                        }
                        queryClean.contains("olá") || queryClean.contains("oi") || queryClean.contains("bom dia") || queryClean.contains("boa tarde") || queryClean.contains("boa noite") -> {
                            "Olá Kenned! Sou a Tessera AI, sua assistente pessoal de IA. Estou monitorando seu status. Patrimônio: R$ $netWorth. Pets: $petsContext. Compras pendentes: $marketContext. Saúde: $medsContext. Como posso te apoiar hoje?"
                        }
                        else -> {
                            val pendingItems = marketContext.split("; ").filter { it.contains("Pendente") }
                            val pendingMeds = medsContext.split("; ").filter { it.contains("Pendente") }
                            
                            val healthAlert = if (pendingMeds.isNotEmpty()) " (Alerte: você possui medicamentos pendentes)" else ""
                            val marketAlert = if (pendingItems.isNotEmpty()) " (Nota: você possui itens pendentes no mercado)" else ""
                            
                            "Entendido, Kenned! Como sua assistente inteligente Tessera AI, estou acompanhando seu painel. Seu patrimônio atual é de R$ $netWorth. Compromissos dos pets: $petsContext.$healthAlert$marketAlert. Se precisar de alguma ajuda específica com finanças, rotinas ou mercado, me informe!"
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