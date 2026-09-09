import "jsr:@supabase/functions-js/edge-runtime.d.ts"

const corsHeaders: Record<string, string> = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
}

interface ChatMessage {
  role: "user" | "model" | "assistant"
  content: string
}

interface SummaryContext {
  userName?: string
  date?: string
  sleepText?: string
  sleepEfficiency?: number
  completedHabits?: number
  totalHabits?: number
  todaySteps?: number
  expensesToday?: number
  pendingTasksCount?: number
  pendingMedsCount?: number
  petRoutinesPending?: number
  activeReminders?: string[]
}

interface RequestPayload {
  mode?: "summary" | "chat"
  prompt?: string
  systemInstruction?: string
  messages?: ChatMessage[]
  context?: SummaryContext
}

interface GeminiPart {
  text: string
}

interface GeminiContent {
  role: "user" | "model"
  parts: GeminiPart[]
}

interface GeminiResponseCandidate {
  content?: {
    parts?: GeminiPart[]
  }
}

interface GeminiResponse {
  candidates?: GeminiResponseCandidate[]
  error?: {
    code: number
    message: string
    status: string
  }
}

const TESSERA_SYSTEM_INSTRUCTION = `Você é a Tessera AI, a inteligência artificial central do app Tessera (ecossistema pessoal de finanças, saúde, rotinas e bem-estar).
Suas diretrizes:
- Responda sempre em português do Brasil com clareza, elegância e objetividade.
- Adote um tom moderno, focado e minimalista. Evite enrolação, saudações longas e excesso de emojis.
- Responda de forma direta e acionável.`

async function callGemini(
  apiKey: string,
  contents: GeminiContent[],
  systemInstructionText: string,
  modelName = "gemini-3.6-flash"
): Promise<string> {
  const url = `https://generativelanguage.googleapis.com/v1beta/models/${modelName}:generateContent?key=${apiKey}`

  const requestBody = {
    contents,
    systemInstruction: {
      parts: [{ text: systemInstructionText }]
    },
    generationConfig: {
      temperature: 0.7,
      maxOutputTokens: 1024,
    }
  }

  const response = await fetch(url, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(requestBody)
  })

  if (response.status === 404) {
    try {
      const listResp = await fetch(`https://generativelanguage.googleapis.com/v1beta/models?key=${apiKey}`)
      if (listResp.ok) {
        const listData = await listResp.json() as { models?: Array<{ name: string; supportedGenerationMethods?: string[] }> }
        const availableModels = (listData.models || [])
          .filter((m) => m.supportedGenerationMethods?.includes("generateContent"))
          .map((m) => m.name.replace("models/", ""))

        console.log("Modelos disponíveis para esta chave:", availableModels)

        const candidateModel = availableModels.find((m) => m.includes("3.6-flash"))
          || availableModels.find((m) => m.includes("flash"))
          || availableModels[0]

        if (candidateModel && candidateModel !== modelName) {
          console.log(`Fallback automático para modelo disponível: ${candidateModel}`)
          return callGemini(apiKey, contents, systemInstructionText, candidateModel)
        }
        throw new Error(`Nenhum modelo compatível encontrado. Disponíveis: ${availableModels.join(", ")}`)
      }
    } catch (listErr: unknown) {
      const errMsg = listErr instanceof Error ? listErr.message : String(listErr)
      console.error("Erro ao consultar ModelService.ListModels:", errMsg)
    }
  }

  if (!response.ok) {
    const errText = await response.text()
    throw new Error(`Erro Gemini HTTP ${response.status}: ${errText}`)
  }

  const json = (await response.json()) as GeminiResponse
  if (json.error) {
    throw new Error(`Erro API Gemini: ${json.error.message}`)
  }

  const candidate = json.candidates?.[0]?.content?.parts?.[0]?.text
  if (!candidate) {
    throw new Error("Gemini não retornou nenhum texto de resposta válido.")
  }

  return candidate.trim()
}

function buildSummaryPrompt(ctx: SummaryContext): string {
  const user = ctx.userName || "Usuário"
  const details: string[] = []

  if (ctx.sleepText) details.push(`- Sono: ${ctx.sleepText}${ctx.sleepEfficiency ? ` (${ctx.sleepEfficiency}% de eficiência)` : ""}`)
  if (ctx.todaySteps && ctx.todaySteps > 0) details.push(`- Passos de hoje: ${ctx.todaySteps}`)
  if (ctx.totalHabits !== undefined && ctx.totalHabits > 0) {
    details.push(`- Hábitos/Rituais concluídos: ${ctx.completedHabits ?? 0} de ${ctx.totalHabits}`)
  }
  if (ctx.expensesToday !== undefined && ctx.expensesToday > 0) {
    details.push(`- Despesas registradas hoje: R$ ${ctx.expensesToday.toFixed(2)}`)
  }
  if (ctx.pendingMedsCount !== undefined && ctx.pendingMedsCount > 0) {
    details.push(`- Medicamentos pendentes: ${ctx.pendingMedsCount}`)
  }
  if (ctx.petRoutinesPending !== undefined && ctx.petRoutinesPending > 0) {
    details.push(`- Cuidados pendentes com Pets: ${ctx.petRoutinesPending}`)
  }
  if (ctx.pendingTasksCount !== undefined && ctx.pendingTasksCount > 0) {
    details.push(`- Tarefas pendentes: ${ctx.pendingTasksCount}`)
  }

  return `Gere um resumo executivo diário personalizado para ${user}.
Dados do dia:
${details.length > 0 ? details.join("\n") : "Início do dia, nenhum registro acumulado ainda."}

Instrução estrita de formato:
- Escreva no máximo 2 a 3 frases curtas e fluidas.
- Seja inspirador, elegante e conciso. Não use marcadores, listas ou múltiplos parágrafos.
- Destaque o equilíbrio entre saúde, produtividade e compromissos.`
}

Deno.serve(async (req: Request) => {
  // Tratamento de preflight CORS
  if (req.method === "OPTIONS") {
    return new Response("ok", { headers: corsHeaders })
  }

  try {
    // Validação de autenticação obrigatória do cliente Supabase (impede chamadas públicas anônimas não autorizadas)
    const authHeader = req.headers.get("authorization") || req.headers.get("Authorization")
    const apikeyHeader = req.headers.get("apikey")
    if (!authHeader && !apikeyHeader) {
      return new Response(
        JSON.stringify({ success: false, error: "Acesso não autorizado. Forneça cabeçalho de autenticação ou apikey." }),
        { status: 401, headers: { ...corsHeaders, "Content-Type": "application/json" } }
      )
    }

    const apiKey = Deno.env.get("GEMINI_API_KEY")
    if (!apiKey) {
      return new Response(
        JSON.stringify({ success: false, error: "GEMINI_API_KEY não configurada nas variáveis de ambiente do Supabase." }),
        { status: 500, headers: { ...corsHeaders, "Content-Type": "application/json" } }
      )
    }

    let payload: RequestPayload
    try {
      payload = await req.json()
    } catch {
      return new Response(
        JSON.stringify({ success: false, error: "Corpo da requisição inválido. Esperado JSON." }),
        { status: 400, headers: { ...corsHeaders, "Content-Type": "application/json" } }
      )
    }

    const mode = payload.mode || (payload.messages ? "chat" : "summary")

    if (mode === "models") {
      const listResp = await fetch(`https://generativelanguage.googleapis.com/v1beta/models?key=${apiKey}`)
      const listJson = await listResp.json()
      return new Response(
        JSON.stringify({ success: listResp.ok, data: listJson }),
        { status: listResp.status, headers: { ...corsHeaders, "Content-Type": "application/json" } }
      )
    }

    let contents: GeminiContent[] = []
    const systemInstruction = payload.systemInstruction || TESSERA_SYSTEM_INSTRUCTION

    if (mode === "summary") {
      const summaryPrompt = payload.context
        ? buildSummaryPrompt(payload.context)
        : (payload.prompt || "Gere um briefing matinal curto para o dia de hoje.")
      
      contents = [
        {
          role: "user",
          parts: [{ text: summaryPrompt }]
        }
      ]
    } else {
      // Modo Chat
      if (payload.messages && payload.messages.length > 0) {
        contents = payload.messages.map((m) => ({
          role: m.role === "assistant" || m.role === "model" ? "model" : "user",
          parts: [{ text: m.content }]
        }))
      } else if (payload.prompt) {
        contents = [
          {
            role: "user",
            parts: [{ text: payload.prompt }]
          }
        ]
      } else {
        return new Response(
          JSON.stringify({ success: false, error: "No modo chat, envie 'messages' ou 'prompt'." }),
          { status: 400, headers: { ...corsHeaders, "Content-Type": "application/json" } }
        )
      }
    }

    const aiText = await callGemini(apiKey, contents, systemInstruction)

    return new Response(
      JSON.stringify({
        success: true,
        mode,
        text: aiText
      }),
      { status: 200, headers: { ...corsHeaders, "Content-Type": "application/json" } }
    )
  } catch (error: unknown) {
    const msg = error instanceof Error ? error.message : String(error)
    console.error("Erro na Edge Function tessera-ai:", msg)
    return new Response(
      JSON.stringify({ success: false, error: "Erro interno ao processar a solicitação com o serviço de IA." }),
      { status: 500, headers: { ...corsHeaders, "Content-Type": "application/json" } }
    )
  }
})
