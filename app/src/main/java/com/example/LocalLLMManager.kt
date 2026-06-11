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
            File(context.getExternalFilesDir(null), "gemma-4-e2b-it-qat.bin").absolutePath,
            File(context.getExternalFilesDir(null), "gemma-2b-it-cpu-int4.bin").absolutePath,
            File(context.getExternalFilesDir(null), "gemma-2b-it.bin").absolutePath,
            File(context.getExternalFilesDir(null), "gemma-2b-it-gpu-int4.bin").absolutePath,
            File(context.filesDir, "gemma-4-e2b-it-qat.bin").absolutePath,
            File(context.filesDir, "gemma-2b-it-cpu-int4.bin").absolutePath,
            "/storage/emulated/0/Download/gemma-4-e2b-it-qat.bin",
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
            Você é a Tessera AI, uma companheira de conversação versátil, inteligente, elegante, natural e amigável integrada ao aplicativo Tessera do Kenned.
            Você é capaz de conversar de forma fluida sobre qualquer assunto casual, responder curiosidades e debater temas gerais com naturalidade e flexibilidade.
            Você tem acesso ao contexto local do usuário (Finanças/Patrimônio, Saúde/Medicamentos, Pets e Compras de Mercado), mas deve consultar e cruzar discretamente esses dados locais APENAS quando o assunto da conversa for relevante a eles.
            Evite listar ou detalhar esses dados locais de forma intrusiva se o usuário estiver apenas jogando conversa fora, cumprimentando ou tratando de temas não relacionados.
            Seja sempre direta, concisa, fluida, natural e amigável. Responda em português brasileiro.
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
                            "Olá Kenned! Analisei suas finanças locais: seu *patrimônio* consolidado atual é de R$ $netWorth. Manter o equilíbrio financeiro ajuda no seu *energy rhythm* geral."
                        }
                        queryClean.contains("compra") || queryClean.contains("mercado") || queryClean.contains("adquirir") || queryClean.contains("comprar") || queryClean.contains("ingrediente") || queryClean.contains("receita") -> {
                            val pendingItems = marketContext.split("; ")
                                .filter { it.contains("Pendente") }
                                .map { it.substringBefore(" (Pendente)") }
                            
                            val advice = if (pendingItems.isNotEmpty()) {
                                "Vejo que você tem itens pendentes na sua lista de mercado: ${pendingItems.joinToString(", ")}. Abastecer o corpo no momento certo faz parte da sua *predictable biology*."
                            } else {
                                "Todos os itens da sua lista de mercado foram comprados! Isso mantém seu *energy rhythm* abastecido e saudável."
                            }
                            "Olá Kenned! $advice"
                        }
                        queryClean.contains("remédio") || queryClean.contains("medicamento") || queryClean.contains("saúde") || queryClean.contains("dose") || queryClean.contains("tomar") -> {
                            val pendingMeds = medsContext.split("; ")
                                .filter { it.contains("Pendente") }
                                .map { it.substringBefore(" - Pendente") }
                            
                            val advice = if (pendingMeds.isNotEmpty()) {
                                "Atenção Kenned: você tem medicamentos pendentes hoje: ${pendingMeds.joinToString(", ")}. Tomá-los é vital para regular sua *predictable biology*."
                            } else {
                                "Parabéns, Kenned! Todos os seus medicamentos diários constam como tomados, alinhando sua *biology* interna com sucesso."
                            }
                            "Olá Kenned! $advice"
                        }
                        queryClean.contains("pet") || queryClean.contains("marie") || queryClean.contains("churchill") || queryClean.contains("vacina") || queryClean.contains("consulta") || queryClean.contains("rotina") -> {
                            "Olá Kenned! Analisando o status dos seus pets: $petsContext. Cuidar de quem amamos traz harmonia e paz ao seu *energy rhythm* diário."
                        }
                        queryClean.contains("olá") || queryClean.contains("oi") || queryClean.contains("bom dia") || queryClean.contains("boa tarde") || queryClean.contains("boa noite") -> {
                            "Olá Kenned! Sou a Tessera AI. Sabia que cada pessoa possui seu próprio *energy rhythm* único? E isso não é aleatório, é uma *predictable biology*!"
                        }
                        else -> {
                            "Com certeza, Kenned! Como sua assistente Tessera AI, posso conversar sobre qualquer assunto. Se quiser podemos alinhar seu *energy rhythm* e examinar sua *predictable biology* hoje!"
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