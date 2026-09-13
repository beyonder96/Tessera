import "jsr:@supabase/functions-js/edge-runtime.d.ts"

// ============================================================================
// CONFIGURAÇÕES E VARIÁVEIS DE AMBIENTE
// ============================================================================
const TELEGRAM_BOT_TOKEN = Deno.env.get("TELEGRAM_BOT_TOKEN") || ""
const TELEGRAM_ALLOWED_USER_IDS = (Deno.env.get("TELEGRAM_ALLOWED_USER_IDS") || "")
  .split(",")
  .map(id => id.trim())
  .filter(id => id.length > 0)

const GROQ_API_KEY = Deno.env.get("GROQ_API_KEY") || Deno.env.get("GEMINI_API_KEY") || ""
const SUPABASE_URL = (Deno.env.get("SUPABASE_URL") || "").replace(/\/$/, "")
const SUPABASE_SERVICE_ROLE_KEY = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") || ""

// Hub IDs opcionais fixados em ambiente
const ENV_FINANCE_HUB_ID = Deno.env.get("FINANCE_HUB_ID")
const ENV_TASKS_HUB_ID = Deno.env.get("TASKS_HUB_ID")
const ENV_WISHES_HUB_ID = Deno.env.get("WISHES_HUB_ID")
const ENV_MARKET_HUB_ID = Deno.env.get("MARKET_HUB_ID")

// Cache de idempotência em memória para evitar reprocessamento de retries do Telegram
const processedUpdates = new Set<number>()

// ============================================================================
// HELPERS TELEGRAM BOT API
// ============================================================================
async function tgCall(method: string, payload: Record<string, unknown>): Promise<any> {
  if (!TELEGRAM_BOT_TOKEN) {
    console.error("TELEGRAM_BOT_TOKEN não configurado!")
    return null
  }
  const url = `https://api.telegram.org/bot${TELEGRAM_BOT_TOKEN}/${method}`
  try {
    const res = await fetch(url, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(payload)
    })
    return await res.json()
  } catch (err) {
    console.error(`Erro ao chamar método ${method}:`, err)
    return null
  }
}

async function sendTelegramMessage(
  chatId: number | string,
  text: string,
  replyMarkup?: Record<string, unknown>
): Promise<any> {
  if (!text) return
  try {
    const res = await tgCall("sendMessage", {
      chat_id: chatId,
      text: text.slice(0, 4000),
      parse_mode: "HTML",
      disable_web_page_preview: true,
      ...(replyMarkup ? { reply_markup: replyMarkup } : {})
    })
    if (res && res.ok === false && (res.description?.includes("can't parse entities") || res.description?.includes("entity"))) {
      return await tgCall("sendMessage", {
        chat_id: chatId,
        text: text.replace(/<[^>]*>/g, "").slice(0, 4000),
        disable_web_page_preview: true,
        ...(replyMarkup ? { reply_markup: replyMarkup } : {})
      })
    }
    return res
  } catch (_err) {
    return await tgCall("sendMessage", {
      chat_id: chatId,
      text: text.replace(/<[^>]*>/g, "").slice(0, 4000),
      disable_web_page_preview: true,
      ...(replyMarkup ? { reply_markup: replyMarkup } : {})
    })
  }
}

async function editTelegramMessage(
  chatId: number | string,
  messageId: number,
  text: string,
  replyMarkup?: Record<string, unknown>
): Promise<any> {
  return await tgCall("editMessageText", {
    chat_id: chatId,
    message_id: messageId,
    text,
    parse_mode: "HTML",
    disable_web_page_preview: true,
    ...(replyMarkup ? { reply_markup: replyMarkup } : {})
  })
}

async function answerCallbackQuery(callbackQueryId: string, text?: string): Promise<any> {
  return await tgCall("answerCallbackQuery", {
    callback_query_id: callbackQueryId,
    text: text || ""
  })
}

async function sendChatAction(chatId: number | string, action = "typing"): Promise<any> {
  return await tgCall("sendChatAction", {
    chat_id: chatId,
    action
  })
}

async function sendTelegramPhoto(
  chatId: number | string,
  photoUrl: string,
  caption?: string
): Promise<any> {
  return await tgCall("sendPhoto", {
    chat_id: chatId,
    photo: photoUrl,
    caption: caption || "",
    parse_mode: "HTML"
  })
}

async function sendTelegramDocument(
  chatId: number | string,
  content: string,
  fileName: string,
  caption?: string
): Promise<any> {
  const form = new FormData()
  form.append("chat_id", String(chatId))
  const blob = new Blob(["\uFEFF" + content], { type: "text/csv;charset=utf-8" })
  form.append("document", blob, fileName)
  if (caption) {
    form.append("caption", caption)
    form.append("parse_mode", "HTML")
  }

  const url = `https://api.telegram.org/bot${TELEGRAM_BOT_TOKEN}/sendDocument`
  const res = await fetch(url, {
    method: "POST",
    body: form
  })
  return await res.json()
}

async function sendTelegramVoiceAudio(
  chatId: number | string,
  audioBuffer: ArrayBuffer,
  caption?: string
): Promise<any> {
  const form = new FormData()
  form.append("chat_id", String(chatId))
  form.append("audio", new Blob([audioBuffer], { type: "audio/mpeg" }), "resposta_tessera.mp3")
  form.append("title", "Tessera AI")
  form.append("performer", "Tessera")
  if (caption) {
    form.append("caption", caption.slice(0, 1024))
    form.append("parse_mode", "HTML")
  }

  const url = `https://api.telegram.org/bot${TELEGRAM_BOT_TOKEN}/sendAudio`
  const res = await fetch(url, {
    method: "POST",
    body: form
  })
  return await res.json()
}

async function generateSpeechAudio(text: string, voice = "pt-BR-FranciscaNeural"): Promise<ArrayBuffer | null> {
  try {
    const { EdgeTTS } = await import("npm:@andresaya/edge-tts")
    const tts = new EdgeTTS()
    let cleanText = text
      .replace(/<[^>]*>/g, "")
      .replace(/•/g, "")
      .replace(/[*_~`]/g, "")
      .replace(/\n+/g, " ")
      .trim()

    // Se o texto for longo, trunca respeitando o ponto final da última frase completa
    if (cleanText.length > 500) {
      const sub = cleanText.slice(0, 500)
      const lastPeriod = Math.max(sub.lastIndexOf("."), sub.lastIndexOf("!"), sub.lastIndexOf("?"))
      cleanText = lastPeriod > 200 ? sub.slice(0, lastPeriod + 1) : sub
    }

    await tts.synthesize(cleanText, voice)
    const buf = tts.toBuffer()
    return buf.buffer.slice(buf.byteOffset, buf.byteOffset + buf.byteLength)
  } catch (err) {
    console.error("Erro ao gerar áudio TTS:", err)
    return null
  }
}

async function maybeSendVoiceReply(chatId: number | string, text: string, shouldSend: boolean): Promise<void> {
  if (!shouldSend) return
  try {
    const audioBuf = await generateSpeechAudio(text, "pt-BR-FranciscaNeural")
    if (audioBuf) {
      await sendTelegramVoiceAudio(chatId, audioBuf)
    }
  } catch (err) {
    console.error("Erro ao enviar áudio neural:", err)
  }
}


async function downloadTelegramFile(fileId: string): Promise<{ buffer: ArrayBuffer; mimeType: string } | null> {
  try {
    const fileInfo = await tgCall("getFile", { file_id: fileId })
    if (!fileInfo || !fileInfo.ok || !fileInfo.result?.file_path) {
      console.error("Não foi possível obter caminho do arquivo no Telegram:", fileInfo)
      return null
    }

    const filePath = fileInfo.result.file_path
    const downloadUrl = `https://api.telegram.org/file/bot${TELEGRAM_BOT_TOKEN}/${filePath}`
    const fileRes = await fetch(downloadUrl)
    if (!fileRes.ok) {
      console.error(`Falha no download do arquivo: ${fileRes.statusText}`)
      return null
    }

    const buffer = await fileRes.arrayBuffer()
    let mimeType = "application/octet-stream"
    const lower = filePath.toLowerCase()
    if (lower.endsWith(".oga") || lower.endsWith(".ogg")) mimeType = "audio/ogg"
    else if (lower.endsWith(".mp3")) mimeType = "audio/mpeg"
    else if (lower.endsWith(".pdf")) mimeType = "application/pdf"
    else if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) mimeType = "image/jpeg"
    else if (lower.endsWith(".png")) mimeType = "image/png"

    return { buffer, mimeType }
  } catch (err) {
    console.error("Erro ao baixar arquivo do Telegram:", err)
    return null
  }
}

function arrayBufferToBase64(buffer: ArrayBuffer): string {
  const bytes = new Uint8Array(buffer)
  let binary = ""
  for (let i = 0; i < bytes.byteLength; i++) {
    binary += String.fromCharCode(bytes[i])
  }
  return btoa(binary)
}

function generateFinanceChartUrl(categories: Array<{ name: string; value: number }>, monthLabel = "Mês Atual"): string {
  const valid = categories
    .filter(c => Number(c.value) > 0)
    .sort((a, b) => Number(b.value) - Number(a.value))
    .slice(0, 7)

  if (valid.length === 0) {
    valid.push({ name: "Sem Despesas", value: 1 })
  }

  const palette = ["#10B981", "#3B82F6", "#F59E0B", "#EF4444", "#8B5CF6", "#EC4899", "#6366F1"]
  const chartConfig = {
    type: "doughnut",
    data: {
      labels: valid.map(c => c.name),
      datasets: [{
        data: valid.map(c => Number(c.value)),
        backgroundColor: palette.slice(0, valid.length),
        borderWidth: 2,
        borderColor: "#FFFFFF"
      }]
    },
    options: {
      plugins: {
        legend: {
          position: "bottom",
          labels: { font: { size: 13, family: "sans-serif" }, boxWidth: 15 }
        },
        title: {
          display: true,
          text: `Gastos por Categoria • Tessera (${monthLabel})`,
          font: { size: 16, weight: "bold" }
        }
      }
    }
  }

  return `https://quickchart.io/chart?c=${encodeURIComponent(JSON.stringify(chartConfig))}&w=600&h=450&bkg=white`
}

// ============================================================================
// SUPABASE REST CLIENT (UTILIZANDO SERVICE ROLE KEY PARA OPERAÇÕES DO BOT)
// ============================================================================
async function supabaseRest(path: string, options: RequestInit = {}): Promise<any> {
  const url = `${SUPABASE_URL}/rest/v1/${path}`
  const headers = {
    "apikey": SUPABASE_SERVICE_ROLE_KEY,
    "Authorization": `Bearer ${SUPABASE_SERVICE_ROLE_KEY}`,
    "Content-Type": "application/json",
    ...(options.headers || {})
  }

  const res = await fetch(url, { ...options, headers })
  if (!res.ok) {
    const errorText = await res.text()
    console.error(`Erro no Supabase REST (${path}): [${res.status}] ${errorText}`)
    throw new Error(`Supabase error ${res.status}: ${errorText}`)
  }
  const contentType = res.headers.get("content-type") || ""
  if (contentType.includes("application/json")) {
    return await res.json()
  }
  return await res.text()
}

// Finanças
async function getFinanceDoc(): Promise<{ id: string; data: any } | null> {
  if (ENV_FINANCE_HUB_ID) {
    const docs = await supabaseRest(`shared_finance_dashboards?id=eq.${ENV_FINANCE_HUB_ID}&select=*`)
    if (Array.isArray(docs) && docs.length > 0) return { id: docs[0].id, data: docs[0] }
  }
  const docs = await supabaseRest("shared_finance_dashboards?select=*&order=updated_at.desc&limit=1")
  if (Array.isArray(docs) && docs.length > 0) return { id: docs[0].id, data: docs[0] }
  return null
}

function checkBudgetAlert(category: string, newAmount: number, doc: any): string | null {
  if (!doc?.data?.categories || !Array.isArray(doc.data.categories)) return null
  const cat = doc.data.categories.find((c: any) => (c.name || "").toLowerCase() === category.toLowerCase())
  if (!cat || !cat.budget || Number(cat.budget) <= 0) return null
  const budget = Number(cat.budget)
  const current = Number(cat.value || 0) + newAmount
  const pct = Math.round((current / budget) * 100)
  if (pct >= 100) {
    return `\n🚨 <b>Limite Estourado:</b> Você atingiu ${pct}% do orçamento de <b>${cat.name}</b>!`
  } else if (pct >= 90) {
    return `\n⚠️ <b>Atenção Crítica:</b> Você já consumiu <b>${pct}%</b> do teto de <b>${cat.name}</b> (restam R$ ${(budget - current).toFixed(2).replace(".", ",")})!`
  } else if (pct >= 70) {
    return `\n🔔 <b>Alerta de Orçamento:</b> Você atingiu <b>${pct}%</b> do limite mensal de <b>${cat.name}</b>.`
  }
  return null
}

async function addFinanceTransaction(tx: {
  title: string
  amount: number
  type: "expense" | "income"
  category?: string
  accountOrCardName?: string
  date?: string
}): Promise<{ id: string; title: string; amount: number; category: string; account: string; budgetAlert?: string } | null> {
  const doc = await getFinanceDoc()
  if (!doc) {
    console.error("Nenhum dashboard de finanças encontrado no Supabase.")
    return null
  }

  const currentSuggestions = Array.isArray(doc.data.suggestions) ? doc.data.suggestions : []
  const todayStr = new Date().toISOString().split("T")[0]

  // Deduplicação: se já existe um lançamento idêntico do Telegram criado nos últimos 30 segundos
  const isDuplicate = currentSuggestions.some((s: any) =>
    s.origin === "Telegram" &&
    s.title?.trim().toLowerCase() === (tx.title || "Lançamento").trim().toLowerCase() &&
    Math.abs(Number(s.amount || 0) - Number(tx.amount || 0)) < 0.01 &&
    s.date === (tx.date || todayStr) &&
    (Date.now() - new Date(s.created_at || 0).getTime() < 30000)
  )

  if (isDuplicate) {
    console.log("Transação idêntica do Telegram detectada (< 30s). Ignorando duplicação.")
    return {
      id: "duplicate",
      title: tx.title || "Lançamento",
      amount: tx.amount,
      category: tx.category || "Geral",
      account: tx.accountOrCardName || "Geral"
    }
  }

  const newSuggestionId = crypto.randomUUID()

  const newSuggestion = {
    id: newSuggestionId,
    title: tx.title || "Lançamento",
    amount: tx.amount,
    type: tx.type || "expense",
    category: tx.category || "Geral",
    date: tx.date || todayStr,
    created_at: new Date().toISOString(),
    status: "auto_approved",
    auto_approved: true,
    action: "create",
    origin: "Telegram",
    account_or_card_name: tx.accountOrCardName || null
  }

  const updatedSuggestions = [...currentSuggestions, newSuggestion]

  await supabaseRest(`shared_finance_dashboards?id=eq.${doc.id}`, {
    method: "PATCH",
    body: JSON.stringify({
      suggestions: updatedSuggestions,
      updated_at: new Date().toISOString()
    })
  })

  const budgetAlert = tx.type === "expense" ? checkBudgetAlert(newSuggestion.category, newSuggestion.amount, doc) : null

  return {
    id: newSuggestionId,
    title: newSuggestion.title,
    amount: newSuggestion.amount,
    category: newSuggestion.category,
    account: newSuggestion.account_or_card_name || "Geral",
    budgetAlert: budgetAlert || undefined
  }
}

async function cancelFinanceTransaction(suggestionId: string): Promise<boolean> {
  const doc = await getFinanceDoc()
  if (!doc) return false

  const currentSuggestions = Array.isArray(doc.data.suggestions) ? doc.data.suggestions : []
  const updatedSuggestions = currentSuggestions.filter((s: any) => s.id !== suggestionId)

  await supabaseRest(`shared_finance_dashboards?id=eq.${doc.id}`, {
    method: "PATCH",
    body: JSON.stringify({
      suggestions: updatedSuggestions,
      updated_at: new Date().toISOString()
    })
  })
  return true
}

// Tarefas e Lembretes
async function getTasksDoc(): Promise<{ id: string; data: any } | null> {
  if (ENV_TASKS_HUB_ID) {
    const docs = await supabaseRest(`shared_tasks_hub?id=eq.${ENV_TASKS_HUB_ID}&select=*`)
    if (Array.isArray(docs) && docs.length > 0) return { id: docs[0].id, data: docs[0] }
  }
  const docs = await supabaseRest("shared_tasks_hub?select=*&order=updated_at.desc&limit=1")
  if (Array.isArray(docs) && docs.length > 0) return { id: docs[0].id, data: docs[0] }
  return null
}

async function addTaskReminder(task: {
  title: string
  description?: string
  due_date?: number
  due_time?: string
}): Promise<any> {
  let doc = await getTasksDoc()
  const currentItems = doc && Array.isArray(doc.data.items) ? doc.data.items : []

  // Deduplicação: não duplica se já existir uma tarefa pendente com o mesmo título
  const isDuplicate = currentItems.some((t: any) =>
    t.title?.trim().toLowerCase() === task.title?.trim().toLowerCase() &&
    t.status === "pending"
  )
  if (isDuplicate) {
    console.log("Tarefa idêntica pendente já existente. Ignorando duplicação.")
    return { title: task.title }
  }

  const taskId = crypto.randomUUID()

  const newItem = {
    id: taskId,
    title: task.title,
    description: task.description || null,
    type: "notice",
    target_user: "kenned",
    due_date: task.due_date || null,
    due_time: task.due_time || null,
    status: "pending",
    created_by: "Telegram",
    created_at: Date.now(),
    completed_at: null
  }

  const updatedItems = [...currentItems, newItem]

  if (doc) {
    await supabaseRest(`shared_tasks_hub?id=eq.${doc.id}`, {
      method: "PATCH",
      body: JSON.stringify({
        items: updatedItems,
        updated_at: new Date().toISOString()
      })
    })
  } else {
    const hubId = ENV_TASKS_HUB_ID || crypto.randomUUID()
    await supabaseRest("shared_tasks_hub", {
      method: "POST",
      body: JSON.stringify({
        id: hubId,
        title: "Tarefas e Lembretes",
        items: updatedItems,
        created_at: new Date().toISOString(),
        updated_at: new Date().toISOString()
      })
    })
  }

  return newItem
}

// Desejos / Wishlist
async function getWishesDoc(): Promise<{ id: string; data: any } | null> {
  if (ENV_WISHES_HUB_ID) {
    const docs = await supabaseRest(`shared_wishes_hub?id=eq.${ENV_WISHES_HUB_ID}&select=*`)
    if (Array.isArray(docs) && docs.length > 0) return { id: docs[0].id, data: docs[0] }
  }
  const docs = await supabaseRest("shared_wishes_hub?select=*&order=updated_at.desc&limit=1")
  if (Array.isArray(docs) && docs.length > 0) return { id: docs[0].id, data: docs[0] }
  return null
}

async function markWishCompleted(wishId: string): Promise<any | null> {
  const doc = await getWishesDoc()
  if (!doc) return null

  const items = Array.isArray(doc.data.items) ? doc.data.items : []
  let foundItem: any = null

  const updatedItems = items.map((item: any) => {
    if (item.id === wishId) {
      foundItem = { ...item, isBought: true, currentValue: item.targetValue || item.currentValue }
      return foundItem
    }
    return item
  })

  if (!foundItem) return null

  await supabaseRest(`shared_wishes_hub?id=eq.${doc.id}`, {
    method: "PATCH",
    body: JSON.stringify({
      items: updatedItems,
      updated_at: new Date().toISOString()
    })
  })

  return foundItem
}

async function addWishItem(wish: {
  title: string
  target_value?: number
  category?: string
}): Promise<any> {
  let doc = await getWishesDoc()
  const currentItems = doc && Array.isArray(doc.data.items) ? doc.data.items : []

  // Deduplicação: não duplica se já existir um desejo pendente com o mesmo título
  const isDuplicate = currentItems.some((w: any) =>
    w.title?.trim().toLowerCase() === wish.title?.trim().toLowerCase() &&
    !w.isBought
  )
  if (isDuplicate) {
    console.log("Desejo idêntico pendente já existente. Ignorando duplicação.")
    return { title: wish.title }
  }

  const wishId = crypto.randomUUID()

  const newItem = {
    id: wishId,
    title: wish.title,
    targetValue: wish.target_value || 0.0,
    currentValue: 0.0,
    imageUrl: "",
    buyUrl: "",
    category: wish.category || "Geral",
    priorityClassification: "Moderado",
    isBought: false,
    created_by: "Telegram"
  }

  const updatedItems = [...currentItems, newItem]

  if (doc) {
    await supabaseRest(`shared_wishes_hub?id=eq.${doc.id}`, {
      method: "PATCH",
      body: JSON.stringify({
        items: updatedItems,
        updated_at: new Date().toISOString()
      })
    })
  } else {
    const hubId = ENV_WISHES_HUB_ID || "wishes_default"
    await supabaseRest("shared_wishes_hub", {
      method: "POST",
      body: JSON.stringify({
        id: hubId,
        title: "Lista de Desejos",
        items: updatedItems,
        created_at: new Date().toISOString(),
        updated_at: new Date().toISOString()
      })
    })
  }

  return newItem
}

// ============================================================================
// LISTA DE COMPRAS (MERCADO)
// ============================================================================
async function getMarketDoc(): Promise<{ id: string; data: any } | null> {
  if (ENV_MARKET_HUB_ID) {
    const docs = await supabaseRest(`shared_market_lists?id=eq.${ENV_MARKET_HUB_ID}&select=*`)
    if (Array.isArray(docs) && docs.length > 0) return { id: docs[0].id, data: docs[0] }
  }
  const docs = await supabaseRest("shared_market_lists?select=*&order=updated_at.desc&limit=1")
  if (Array.isArray(docs) && docs.length > 0) return { id: docs[0].id, data: docs[0] }
  return null
}

async function addMarketItems(itemsToAdd: Array<{ name: string; quantity?: number; unit?: string; category?: string }>): Promise<any[]> {
  let doc = await getMarketDoc()
  const currentItems = doc && Array.isArray(doc.data.items) ? doc.data.items : []
  const newItemsCreated: any[] = []

  for (const it of itemsToAdd) {
    if (!it.name || it.name.trim().length === 0) continue
    const trimmedName = it.name.trim()

    // Deduplicação: se já existe um item com o mesmo nome na lista pendente/mercado
    const existingIndex = currentItems.findIndex((ci: any) =>
      ci.name && ci.name.trim().toLowerCase() === trimmedName.toLowerCase() && !ci.isBought
    )

    if (existingIndex >= 0) {
      const existing = currentItems[existingIndex]
      existing.quantity = (Number(existing.quantity) || 1) + (Number(it.quantity) || 1)
      existing.inMarket = true
      existing.isChecked = false
      newItemsCreated.push(existing)
    } else {
      const newItem = {
        name: trimmedName,
        quantity: it.quantity || 1.0,
        unit: it.unit || "un",
        category: it.category || "Geral",
        price: 0.0,
        isChecked: false,
        isBought: false,
        inMarket: true,
        needsApproval: false,
        created_by: "Telegram"
      }
      currentItems.push(newItem)
      newItemsCreated.push(newItem)
    }
  }

  if (doc) {
    await supabaseRest(`shared_market_lists?id=eq.${doc.id}`, {
      method: "PATCH",
      body: JSON.stringify({
        items: currentItems,
        updated_at: new Date().toISOString()
      })
    })
  } else {
    const hubId = ENV_MARKET_HUB_ID || crypto.randomUUID()
    await supabaseRest("shared_market_lists", {
      method: "POST",
      body: JSON.stringify({
        id: hubId,
        title: "Lista de Compras",
        items: currentItems,
        created_at: new Date().toISOString(),
        updated_at: new Date().toISOString()
      })
    })
  }

  return newItemsCreated
}

async function markMarketItemBought(itemName: string): Promise<boolean> {
  const doc = await getMarketDoc()
  if (!doc) return false
  const items = Array.isArray(doc.data.items) ? doc.data.items : []
  let changed = false
  const updatedItems = items.map((it: any) => {
    if (it.name.toLowerCase() === itemName.toLowerCase() || it.name.toLowerCase().includes(itemName.toLowerCase())) {
      changed = true
      return { ...it, isChecked: true, isBought: true }
    }
    return it
  })
  if (!changed) return false
  await supabaseRest(`shared_market_lists?id=eq.${doc.id}`, {
    method: "PATCH",
    body: JSON.stringify({
      items: updatedItems,
      updated_at: new Date().toISOString()
    })
  })
  return true
}

// ============================================================================
// SERVIÇOS EXTERNOS: PREVISÃO DO TEMPO & FUTEBOL
// ============================================================================
async function fetchWeatherForecast(cityQuery = "São Paulo"): Promise<string> {
  try {
    let lat = -23.5505
    let lon = -46.6333
    let cityName = "São Paulo"

    if (cityQuery.toLowerCase() !== "são paulo" && cityQuery.toLowerCase() !== "sp") {
      const geoUrl = `https://geocoding-api.open-meteo.com/v1/search?name=${encodeURIComponent(cityQuery)}&count=1&language=pt&format=json`
      const geoRes = await fetch(geoUrl)
      if (geoRes.ok) {
        const geoData = await geoRes.json()
        if (geoData.results && geoData.results.length > 0) {
          lat = geoData.results[0].latitude
          lon = geoData.results[0].longitude
          cityName = `${geoData.results[0].name}, ${geoData.results[0].admin1 || ""}`
        }
      }
    }

    const weatherUrl = `https://api.open-meteo.com/v1/forecast?latitude=${lat}&longitude=${lon}&current=temperature_2m,relative_humidity_2m,apparent_temperature,precipitation,weather_code,wind_speed_10m&daily=temperature_2m_max,temperature_2m_min,precipitation_probability_max&timezone=auto`
    const weatherRes = await fetch(weatherUrl)
    if (!weatherRes.ok) throw new Error("Erro na API Open-Meteo")

    const data = await weatherRes.json()
    const current = data.current
    const daily = data.daily

    const weatherDescriptions: Record<number, string> = {
      0: "Céu limpo ☀️",
      1: "Predominantemente limpo 🌤️",
      2: "Parcialmente nublado ⛅",
      3: "Nublado ☁️",
      45: "Nevoeiro 🌫️",
      51: "Garoa leve 🌦️",
      61: "Chuva fraca 🌧️",
      63: "Chuva moderada 🌧️",
      65: "Chuva forte ⛈️",
      80: "Pancadas de chuva 🌦️",
      95: "Tempestade com trovoadas ⚡"
    }

    const condition = weatherDescriptions[current.weather_code] || "Tempo estável ⛅"
    const maxTemp = daily?.temperature_2m_max?.[0] ?? "--"
    const minTemp = daily?.temperature_2m_min?.[0] ?? "--"
    const rainChance = daily?.precipitation_probability_max?.[0] ?? 0

    return `🌤️ <b>Previsão do Tempo • ${cityName}</b>\n\n` +
           `• <b>Condição:</b> ${condition}\n` +
           `• <b>Temperatura Atual:</b> ${Math.round(current.temperature_2m)}°C (Sensação de ${Math.round(current.apparent_temperature)}°C)\n` +
           `• <b>Mín / Máx:</b> ${Math.round(minTemp)}°C / ${Math.round(maxTemp)}°C\n` +
           `• <b>Probabilidade de Chuva:</b> ${rainChance}%\n` +
           `• <b>Umidade:</b> ${current.relative_humidity_2m}%\n` +
           `• <b>Vento:</b> ${Math.round(current.wind_speed_10m)} km/h`
  } catch (err) {
    console.error("Erro ao buscar clima:", err)
    return "❌ Não foi possível carregar a previsão do tempo no momento."
  }
}

const KNOWN_BRAZILIAN_TEAMS: Record<string, { id: string; name: string }> = {
  "flamengo": { id: "134287", name: "Flamengo" },
  "fla": { id: "134287", name: "Flamengo" },
  "mengo": { id: "134287", name: "Flamengo" },
  "mengao": { id: "134287", name: "Flamengo" },
  "mengão": { id: "134287", name: "Flamengo" },
  "rubro-negro": { id: "134287", name: "Flamengo" },
  "rubro negro": { id: "134287", name: "Flamengo" },
  "palmeiras": { id: "134465", name: "Palmeiras" },
  "verdao": { id: "134465", name: "Palmeiras" },
  "verdão": { id: "134465", name: "Palmeiras" },
  "corinthians": { id: "134284", name: "Corinthians" },
  "timao": { id: "134284", name: "Corinthians" },
  "timão": { id: "134284", name: "Corinthians" },
  "coringao": { id: "134284", name: "Corinthians" },
  "coringão": { id: "134284", name: "Corinthians" },
  "sao paulo": { id: "134291", name: "São Paulo" },
  "são paulo": { id: "134291", name: "São Paulo" },
  "spfc": { id: "134291", name: "São Paulo" },
  "tricolor": { id: "134291", name: "São Paulo" },
  "vasco": { id: "134282", name: "Vasco da Gama" },
  "vasco da gama": { id: "134282", name: "Vasco da Gama" },
  "vascao": { id: "134282", name: "Vasco da Gama" },
  "vascão": { id: "134282", name: "Vasco da Gama" },
  "botafogo": { id: "134285", name: "Botafogo" },
  "fogao": { id: "134285", name: "Botafogo" },
  "fogão": { id: "134285", name: "Botafogo" },
  "gremio": { id: "134288", name: "Grêmio" },
  "grêmio": { id: "134288", name: "Grêmio" },
  "internacional": { id: "134281", name: "Internacional" },
  "inter": { id: "134281", name: "Internacional" },
  "colorado": { id: "134281", name: "Internacional" },
  "atletico mineiro": { id: "134299", name: "Atlético Mineiro" },
  "atlético mineiro": { id: "134299", name: "Atlético Mineiro" },
  "atletico-mg": { id: "134299", name: "Atlético Mineiro" },
  "galo": { id: "134299", name: "Atlético Mineiro" },
  "cruzeiro": { id: "134294", name: "Cruzeiro" },
  "raposa": { id: "134294", name: "Cruzeiro" },
  "santos": { id: "134286", name: "Santos" },
  "peixe": { id: "134286", name: "Santos" },
  "bahia": { id: "134293", name: "Bahia" },
  "fortaleza": { id: "136186", name: "Fortaleza" },
  "fluminense": { id: "134296", name: "Fluminense" },
  "flu": { id: "134296", name: "Fluminense" },
  "athletico": { id: "134297", name: "Athletico Paranaense" },
  "athletico paranaense": { id: "134297", name: "Athletico Paranaense" },
  "athletico-pr": { id: "134297", name: "Athletico Paranaense" },
  "furacao": { id: "134297", name: "Athletico Paranaense" },
  "furacão": { id: "134297", name: "Athletico Paranaense" },
  "bragantino": { id: "134736", name: "Red Bull Bragantino" },
  "red bull bragantino": { id: "134736", name: "Red Bull Bragantino" },
  "vitoria": { id: "134280", name: "Vitória" },
  "vitória": { id: "134280", name: "Vitória" },
  "juventude": { id: "134301", name: "Juventude" },
  "criciuma": { id: "134300", name: "Criciúma" },
  "criciúma": { id: "134300", name: "Criciúma" },
  "cuiaba": { id: "136933", name: "Cuiabá" },
  "cuiabá": { id: "136933", name: "Cuiabá" },
  "mirassol": { id: "141181", name: "Mirassol" },
  "sport": { id: "134290", name: "Sport" },
  "ceara": { id: "134705", name: "Ceará" },
  "ceará": { id: "134705", name: "Ceará" },
  "coritiba": { id: "134704", name: "Coritiba" },
  "coxa": { id: "134704", name: "Coritiba" },
  "del valle": { id: "135687", name: "Independiente del Valle" },
  "independiente del valle": { id: "135687", name: "Independiente del Valle" },
  "boca": { id: "134266", name: "Boca Juniors" },
  "boca juniors": { id: "134266", name: "Boca Juniors" },
  "river": { id: "134267", name: "River Plate" },
  "river plate": { id: "134267", name: "River Plate" },
  "racing": { id: "134269", name: "Racing Club" },
  "penarol": { id: "135114", name: "Peñarol" },
  "peñarol": { id: "135114", name: "Peñarol" },
  "nacional": { id: "135115", name: "Nacional" },
  "olimpia": { id: "135649", name: "Olimpia" },
  "cerro porteno": { id: "135650", name: "Cerro Porteño" },
  "cerro porteño": { id: "135650", name: "Cerro Porteño" },
  "ldu": { id: "135688", name: "LDU" },
  "colo-colo": { id: "135249", name: "Colo-Colo" },
  "colo colo": { id: "135249", name: "Colo-Colo" },
  "real madrid": { id: "133738", name: "Real Madrid" },
  "barcelona": { id: "133739", name: "Barcelona" },
  "manchester city": { id: "133613", name: "Manchester City" },
  "manchester united": { id: "133612", name: "Manchester United" },
  "liverpool": { id: "133602", name: "Liverpool" },
  "arsenal": { id: "133604", name: "Arsenal" },
  "psg": { id: "133714", name: "Paris Saint-Germain" }
}

async function searchTeamByName(teamQuery?: string): Promise<{ id: string; name: string } | null> {
  if (!teamQuery || teamQuery.trim().length === 0) return null
  const clean = teamQuery.toLowerCase().trim()
  if (KNOWN_BRAZILIAN_TEAMS[clean]) return KNOWN_BRAZILIAN_TEAMS[clean]
  for (const [k, v] of Object.entries(KNOWN_BRAZILIAN_TEAMS)) {
    if (clean === v.id || clean.includes(k) || k.includes(clean)) return v
  }
  try {
    const res = await fetch(`https://www.thesportsdb.com/api/v1/json/3/searchteams.php?t=${encodeURIComponent(clean)}`)
    if (res.ok) {
      const data = await res.json()
      const t = data.teams?.[0]
      if (t && t.idTeam) {
        return { id: t.idTeam, name: t.strTeam }
      }
    }
  } catch (err) {
    console.error("Erro na busca de time:", err)
  }
  return null
}

function formatMatchDateTime(dateStr?: string, timeStr?: string): { formatted: string; spoken: string } {
  if (!dateStr) return { formatted: "Data a definir", spoken: "em data a definir" }
  try {
    let cleanTime = timeStr || "00:00:00"
    if (!cleanTime.includes(":")) cleanTime += ":00:00"
    else if (cleanTime.split(":").length === 2) cleanTime += ":00"

    const utcIso = `${dateStr}T${cleanTime}Z`
    const d = new Date(utcIso)

    const brDate = new Intl.DateTimeFormat("pt-BR", {
      timeZone: "America/Sao_Paulo",
      day: "2-digit",
      month: "2-digit",
      year: "numeric"
    }).format(d)

    const brTime = new Intl.DateTimeFormat("pt-BR", {
      timeZone: "America/Sao_Paulo",
      hour: "2-digit",
      minute: "2-digit",
      hour12: false
    }).format(d)

    const [hours, minutes] = brTime.split(":")
    const timeSpoken = minutes === "00" ? `${parseInt(hours, 10)} horas` : `${parseInt(hours, 10)} e ${minutes}`

    const now = new Date()
    const todayBr = new Intl.DateTimeFormat("pt-BR", { timeZone: "America/Sao_Paulo", day: "2-digit", month: "2-digit", year: "numeric" }).format(now)
    const tomorrow = new Date(now.getTime() + 86400000)
    const tomorrowBr = new Intl.DateTimeFormat("pt-BR", { timeZone: "America/Sao_Paulo", day: "2-digit", month: "2-digit", year: "numeric" }).format(tomorrow)

    if (brDate === todayBr) {
      return {
        formatted: `Hoje (${brDate.slice(0, 5)}) às ${brTime}`,
        spoken: `hoje às ${timeSpoken}`
      }
    } else if (brDate === tomorrowBr) {
      return {
        formatted: `Amanhã (${brDate.slice(0, 5)}) às ${brTime}`,
        spoken: `amanhã às ${timeSpoken}`
      }
    } else {
      const dayMonth = brDate.slice(0, 5)
      return {
        formatted: `${brDate} às ${brTime}`,
        spoken: `no dia ${dayMonth} às ${timeSpoken}`
      }
    }
  } catch (_e) {
    return { formatted: dateStr, spoken: `no dia ${dateStr}` }
  }
}

function formatGEDateTime(dateOrIsoStr?: string, timeStr?: string): { formatted: string; spoken: string } {
  if (!dateOrIsoStr) return { formatted: "Data a definir", spoken: "em data a definir" }
  try {
    let year = "", month = "", day = "", time = timeStr ? timeStr.slice(0, 5) : ""
    if (dateOrIsoStr.includes("T")) {
      const [dPart, tPart] = dateOrIsoStr.split("T")
      const parts = dPart.split("-")
      year = parts[0]
      month = parts[1]
      day = parts[2]
      if (!time && tPart) time = tPart.slice(0, 5)
    } else if (dateOrIsoStr.includes("-")) {
      const parts = dateOrIsoStr.split("-")
      year = parts[0]
      month = parts[1]
      day = parts[2]
    } else {
      return { formatted: dateOrIsoStr, spoken: `em ${dateOrIsoStr}` }
    }

    const dayMonth = `${day}/${month}`
    const formattedTime = time ? ` às ${time}` : ""

    const now = new Date()
    const todayBr = new Intl.DateTimeFormat("pt-BR", { timeZone: "America/Sao_Paulo", day: "2-digit", month: "2-digit", year: "numeric" }).format(now)
    const tomorrow = new Date(now.getTime() + 86400000)
    const tomorrowBr = new Intl.DateTimeFormat("pt-BR", { timeZone: "America/Sao_Paulo", day: "2-digit", month: "2-digit", year: "numeric" }).format(tomorrow)

    const fullDate = `${day}/${month}/${year}`
    let relative = `${dayMonth}${formattedTime}`
    let spoken = `no dia ${dayMonth}${time ? ` às ${time}` : ""}`

    if (fullDate === todayBr) {
      relative = `Hoje (${dayMonth})${formattedTime}`
      spoken = `hoje${time ? ` às ${time}` : ""}`
    } else if (fullDate === tomorrowBr) {
      relative = `Amanhã (${dayMonth})${formattedTime}`
      spoken = `amanhã${time ? ` às ${time}` : ""}`
    }

    return { formatted: relative, spoken }
  } catch (_e) {
    return { formatted: dateOrIsoStr, spoken: `em ${dateOrIsoStr}` }
  }
}

function getMatchTimestamp(m: any): number {
  if (!m.data_realizacao) return 0
  const dStr = m.data_realizacao.includes("T") ? m.data_realizacao.split("T")[0] : m.data_realizacao
  const tStr = m.hora_realizacao || (m.data_realizacao.includes("T") ? m.data_realizacao.split("T")[1].slice(0, 5) : "00:00")
  return new Date(`${dStr}T${tStr}:00`).getTime() || 0
}

async function fetchGEMultiCompetitionData(): Promise<{ table: any[]; matches: any[]; edition: string } | null> {
  const urls = [
    { url: "https://ge.globo.com/futebol/brasileirao-serie-a/", defaultLeague: "Brasileirão Série A", isSerieA: true },
    { url: "https://ge.globo.com/futebol/libertadores/", defaultLeague: "Copa Libertadores", isSerieA: false },
    { url: "https://ge.globo.com/futebol/copa-do-brasil/", defaultLeague: "Copa do Brasil", isSerieA: false }
  ]

  let table: any[] = []
  let edition = "Brasileirão Série A"
  const allMatches: any[] = []

  try {
    const results = await Promise.allSettled(urls.map(async (u) => {
      const res = await fetch(u.url, {
        headers: {
          "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
          "Accept": "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8"
        }
      })
      if (!res.ok) return { config: u, html: null }
      const html = await res.text()
      return { config: u, html }
    }))

    for (const r of results) {
      if (r.status !== "fulfilled" || !r.value.html) continue
      const { config, html } = r.value

      const classMarker = "const classificacao = "
      const classIdx = html.indexOf(classMarker)
      if (classIdx !== -1) {
        const jsonStart = classIdx + classMarker.length
        let openBraces = 0, jsonEnd = -1
        for (let i = jsonStart; i < html.length; i++) {
          if (html[i] === "{") openBraces++
          else if (html[i] === "}") {
            openBraces--
            if (openBraces === 0) { jsonEnd = i + 1; break; }
          }
        }
        if (jsonEnd !== -1) {
          try {
            const classObj = JSON.parse(html.substring(jsonStart, jsonEnd))
            if (config.isSerieA) {
              table = classObj.classificacao || []
              if (classObj.edicao?.nome) edition = classObj.edicao.nome
            }
            const leagueName = classObj.edicao?.nome || config.defaultLeague
            if (classObj.secao && Array.isArray(classObj.secao)) {
              classObj.secao.forEach((s: any) => {
                (s.chave || []).forEach((ch: any) => {
                  (ch.jogos || []).forEach((j: any) => {
                    allMatches.push({
                      ...j,
                      torneio: leagueName,
                      faseNome: ch.nome || classObj.fase?.nome || "Mata-mata"
                    })
                  })
                })
              })
            }
          } catch (_e) {}
        }
      }

      const matchMarker = "const listaJogos = "
      const matchIdx = html.indexOf(matchMarker)
      if (matchIdx !== -1) {
        const jsonStart = matchIdx + matchMarker.length
        let openBrackets = 0, jsonEnd = -1
        for (let i = jsonStart; i < html.length; i++) {
          if (html[i] === "[" || html[i] === "{") openBrackets++
          else if (html[i] === "]" || html[i] === "}") {
            openBrackets--
            if (openBrackets === 0) { jsonEnd = i + 1; break; }
          }
        }
        if (jsonEnd !== -1) {
          try {
            const games = JSON.parse(html.substring(jsonStart, jsonEnd))
            games.forEach((g: any) => {
              allMatches.push({
                ...g,
                torneio: config.defaultLeague,
                faseNome: "Rodada Atual"
              })
            })
          } catch (_e) {}
        }
      }
    }

    return { table, matches: allMatches, edition }
  } catch (err) {
    console.error("Erro ao buscar dados multi-competições do GE:", err)
    return null
  }
}

async function fetchGEBrasileiraoData(): Promise<{ table: any[]; matches: any[]; edition: string } | null> {
  return fetchGEMultiCompetitionData()
}

function formatTableMonospace(table: any[], edition: string, highlightTeamName = ""): { text: string; spokenText: string } {
  function pad(str: any, len: number, alignLeft = true) {
    const s = String(str ?? "")
    if (s.length >= len) return s.slice(0, len)
    return alignLeft ? s + " ".repeat(len - s.length) : " ".repeat(len - s.length) + s
  }

  let text = `🏆 <b>${edition} • Classificação Oficial</b>\n`
  text += `<i>Atualização em tempo real (GE)</i>\n\n`
  text += `<pre>\n`
  text += `Pos Clube         PTS  J  V  SG\n`
  text += `───────────────────────────────\n`

  // 1º ao 4º: Libertadores
  table.slice(0, 4).forEach((t: any) => {
    const pos = pad(`${t.ordem}º`, 4, true)
    const name = pad(t.nome_popular || t.nome || "Time", 13, true)
    const pts = pad(t.pontos, 4, false)
    const j = pad(t.jogos, 3, false)
    const v = pad(t.vitorias, 3, false)
    const sgVal = t.saldo_gols > 0 ? `+${t.saldo_gols}` : String(t.saldo_gols ?? 0)
    const sg = pad(sgVal, 4, false)
    text += `${pos}${name}${pts}${j}${v}${sg}\n`
  })

  text += `───────────────────────────────\n`
  // 5º e 6º: Pré-Libertadores
  table.slice(4, 6).forEach((t: any) => {
    const pos = pad(`${t.ordem}º`, 4, true)
    const name = pad(t.nome_popular || t.nome || "Time", 13, true)
    const pts = pad(t.pontos, 4, false)
    const j = pad(t.jogos, 3, false)
    const v = pad(t.vitorias, 3, false)
    const sgVal = t.saldo_gols > 0 ? `+${t.saldo_gols}` : String(t.saldo_gols ?? 0)
    const sg = pad(sgVal, 4, false)
    text += `${pos}${name}${pts}${j}${v}${sg}\n`
  })

  text += `───────────────────────────────\n`
  // 17º ao 20º: Rebaixamento
  table.slice(16, 20).forEach((t: any) => {
    const pos = pad(`${t.ordem}º`, 4, true)
    const name = pad(t.nome_popular || t.nome || "Time", 13, true)
    const pts = pad(t.pontos, 4, false)
    const j = pad(t.jogos, 3, false)
    const v = pad(t.vitorias, 3, false)
    const sgVal = t.saldo_gols > 0 ? `+${t.saldo_gols}` : String(t.saldo_gols ?? 0)
    const sg = pad(sgVal, 4, false)
    text += `${pos}${name}${pts}${j}${v}${sg}\n`
  })
  text += `</pre>\n`
  text += `🟢 <i>1º ao 4º: Libertadores (Fase de Grupos)</i>\n`
  text += `🔵 <i>5º e 6º: Pré-Libertadores</i>\n`
  text += `🔴 <i>17º ao 20º: Zona de Rebaixamento (Z-4)</i>\n`

  let spokenHighlight = ""
  if (highlightTeamName) {
    const clean = highlightTeamName.toLowerCase().trim()
    const target = table.find((t: any) => (t.nome_popular || t.nome || "").toLowerCase().includes(clean))
    if (target) {
      const formIcons = (target.ultimos_jogos || []).map((f: string) => {
        if (f.toLowerCase() === "v") return "🟢 V"
        if (f.toLowerCase() === "e") return "🟡 E"
        return "🔴 D"
      }).join(" ")

      text += `\n📌 <b>Destaque • ${target.nome_popular || target.nome}:</b>\n`
      text += `• <b>Posição:</b> ${target.ordem}º lugar\n`
      text += `• <b>Pontos:</b> ${target.pontos} pts (${target.jogos} jogos | ${target.vitorias} vitórias)\n`
      const targetSg = target.saldo_gols > 0 ? `+${target.saldo_gols}` : target.saldo_gols
      text += `• <b>Saldo de Gols:</b> ${targetSg}\n`
      text += `• <b>Aproveitamento:</b> ${target.aproveitamento}%\n`
      if (formIcons) text += `• <b>Forma Recente:</b> ${formIcons}\n`

      spokenHighlight = ` O ${target.nome_popular || target.nome} está na ${target.ordem}ª posição com ${target.pontos} pontos em ${target.jogos} jogos.`
    }
  }

  const leader = table[0]
  const spokenText = `Na tabela do Brasileirão, o líder é o ${leader.nome_popular || leader.nome} com ${leader.pontos} pontos, seguido por ${table[1]?.nome_popular || table[1]?.nome} com ${table[1]?.pontos} pontos e ${table[2]?.nome_popular || table[2]?.nome} com ${table[2]?.pontos} pontos.${spokenHighlight}`

  return { text, spokenText }
}

async function fetchSoccerInfo(
  soccer?: { team?: string; type?: "next" | "last" | "standings" | "general" },
  rawQuery = ""
): Promise<{ text: string; spokenText: string; replyMarkup?: any }> {
  const queryLower = `${soccer?.team || ""} ${rawQuery || ""}`.toLowerCase()

  // 1. Consulta de Tabela / Classificação
  const isStandingsQuery = soccer?.type === "standings" ||
    queryLower.includes("tabela") ||
    queryLower.includes("classificacao") ||
    queryLower.includes("classificação") ||
    queryLower.includes("lider") ||
    queryLower.includes("líder") ||
    queryLower.includes("g4") ||
    queryLower.includes("g-4") ||
    queryLower.includes("pontos")

  if (isStandingsQuery) {
    try {
      const geData = await fetchGEBrasileiraoData()
      if (geData && geData.table && geData.table.length > 0) {
        const formatted = formatTableMonospace(geData.table, geData.edition, soccer?.team || rawQuery)
        const replyMarkup = {
          inline_keyboard: [
            [
              { text: "🔴 Próximo do Flamengo", callback_data: "soccer_team:flamengo" },
              { text: "🟢 Próximo do Palmeiras", callback_data: "soccer_team:palmeiras" }
            ],
            [
              { text: "⚪ Próximo do Corinthians", callback_data: "soccer_team:corinthians" },
              { text: "⚫ Próximo do São Paulo", callback_data: "soccer_team:sao paulo" }
            ]
          ]
        }
        return { text: formatted.text, spokenText: formatted.spokenText, replyMarkup }
      }
    } catch (err) {
      console.error("Erro ao buscar tabela GE:", err)
    }

    // Fallback: TheSportsDB
    try {
      const currentYear = new Date().getFullYear()
      let res = await fetch(`https://www.thesportsdb.com/api/v1/json/3/lookuptable.php?l=4351&s=${currentYear}`)
      let data = await res.json()
      if (!data.table || data.table.length === 0) {
        res = await fetch(`https://www.thesportsdb.com/api/v1/json/3/lookuptable.php?l=4351&s=${currentYear - 1}`)
        data = await res.json()
      }

      const table = data.table || []
      if (table.length > 0) {
        let text = `🏆 <b>Classificação • Brasileirão Série A</b>\n\n`
        text += `🟢 <b>Zona de Libertadores (G4):</b>\n`
        table.slice(0, 4).forEach((t: any) => {
          text += `<b>${t.intRank}º</b> ${t.strTeam} — <b>${t.intPoints} pts</b> (${t.intPlayed}J | ${t.intGoalDifference}SG)\n`
        })

        if (table.length >= 6) {
          text += `\n🔵 <b>Pré-Libertadores:</b>\n`
          table.slice(4, 6).forEach((t: any) => {
            text += `<b>${t.intRank}º</b> ${t.strTeam} — <b>${t.intPoints} pts</b> (${t.intPlayed}J)\n`
          })
        }

        const leader = table[0]
        const spokenText = `Na tabela do Brasileirão, o líder é o ${leader.strTeam} com ${leader.intPoints} pontos.`
        return { text, spokenText }
      }
    } catch (err) {
      console.error("Erro fallback tabela TheSportsDB:", err)
    }
  }

  // 2. Consulta de Time (Próximo Jogo ou Último Resultado)
  const teamCandidate = soccer?.team || rawQuery
  const resolvedTeam = await searchTeamByName(teamCandidate)

  if (resolvedTeam) {
    const isLastQuery = soccer?.type === "last" ||
      queryLower.includes("ultimo") ||
      queryLower.includes("último") ||
      queryLower.includes("anterior") ||
      queryLower.includes("resultado") ||
      queryLower.includes("placar") ||
      queryLower.includes("quanto foi") ||
      queryLower.includes("quanto terminou") ||
      queryLower.includes("ganhou") ||
      queryLower.includes("perdeu")

    // Busca dados em tempo real multi-competições do GloboEsporte (Brasileirão, Libertadores, Copa do Brasil)
    let geMatches: any[] = []
    try {
      const geData = await fetchGEMultiCompetitionData()
      if (geData?.matches) {
        geMatches = geData.matches
      }
    } catch (_err) {
      // Ignora erro e usa TheSportsDB
    }

    const cleanName = resolvedTeam.name.toLowerCase()
    const teamMatches = geMatches.filter((m: any) => {
      const mand = (m.equipes?.mandante?.nome_popular || m.equipes?.mandante?.sigla || m.equipes?.mandante?.nome || "").toLowerCase()
      const visi = (m.equipes?.visitante?.nome_popular || m.equipes?.visitante?.sigla || m.equipes?.visitante?.nome || "").toLowerCase()
      return mand.includes(cleanName) || cleanName.includes(mand) || visi.includes(cleanName) || cleanName.includes(visi)
    })

    const liveMatch = teamMatches.find((m: any) => m.jogo_ja_comecou && m.transmissao?.broadcast?.id === "AO_VIVO")
    const completedMatches = teamMatches.filter((m: any) => m.jogo_ja_comecou && (m.transmissao?.broadcast?.id === "ENCERRADA" || (m.placar_oficial_mandante !== null && m.placar_oficial_visitante !== null)))
    completedMatches.sort((a, b) => getMatchTimestamp(b) - getMatchTimestamp(a))
    const upcomingMatches = teamMatches.filter((m: any) => !m.jogo_ja_comecou)
    upcomingMatches.sort((a, b) => getMatchTimestamp(a) - getMatchTimestamp(b))

    // A: Partida AO VIVO agora no GE
    if (liveMatch) {
      const h = liveMatch.equipes.mandante.nome_popular || liveMatch.equipes.mandante.nome
      const a = liveMatch.equipes.visitante.nome_popular || liveMatch.equipes.visitante.nome
      const hs = liveMatch.placar_oficial_mandante ?? 0
      const as = liveMatch.placar_oficial_visitante ?? 0
      const venue = liveMatch.sede?.nome_popular ? `🏟️ ${liveMatch.sede.nome_popular}\n` : ""
      const fase = liveMatch.faseNome ? ` • ${liveMatch.faseNome}` : ""
      const torneio = liveMatch.torneio || "Futebol"

      const text = `⚽ <b>Partida em Andamento • ${resolvedTeam.name}</b>\n\n` +
                   `🔴 <b>AO VIVO: ${h} ${hs} x ${as} ${a}</b>\n` +
                   `🏆 <b>${torneio}</b>${fase}\n` +
                   venue +
                   `\n⚡ <i>Acompanhe estatísticas e lances minuto a minuto no app Tessera!</i>`

      const spokenText = `O jogo do ${resolvedTeam.name} está acontecendo agora! O placar ao vivo é ${h} ${hs}, ${a} ${as}.`

      const replyMarkup = {
        inline_keyboard: [
          [
            { text: "🏆 Ver Tabela ao Vivo", callback_data: "soccer_table" }
          ]
        ]
      }

      return { text, spokenText, replyMarkup }
    }

    // B: Consulta de Último Jogo / Resultado Concluído
    if (isLastQuery) {
      if (completedMatches.length > 0) {
        const lastMatch = completedMatches[0]
        const h = lastMatch.equipes.mandante.nome_popular || lastMatch.equipes.mandante.nome
        const a = lastMatch.equipes.visitante.nome_popular || lastMatch.equipes.visitante.nome
        const hs = lastMatch.placar_oficial_mandante ?? 0
        const as = lastMatch.placar_oficial_visitante ?? 0
        const dt = formatGEDateTime(lastMatch.data_realizacao, lastMatch.hora_realizacao)
        const venue = lastMatch.sede?.nome_popular ? `🏟️ ${lastMatch.sede.nome_popular}\n` : ""
        const fase = lastMatch.faseNome ? ` • ${lastMatch.faseNome}` : ""
        const torneio = lastMatch.torneio || "Competição Oficial"

        let upcomingNote = ""
        let upcomingSpoken = ""
        if (upcomingMatches.length > 0) {
          const u = upcomingMatches[0]
          const uH = u.equipes.mandante.nome_popular || u.equipes.mandante.nome
          const uA = u.equipes.visitante.nome_popular || u.equipes.visitante.nome
          const uDt = formatGEDateTime(u.data_realizacao, u.hora_realizacao)
          const uOpponent = resolvedTeam.name.toLowerCase().includes(uH.toLowerCase()) ? uA : uH
          const uTorneio = u.torneio || "Futebol"
          upcomingNote = `\n🔔 <b>Atenção:</b> O ${resolvedTeam.name} entra em campo <b>${uDt.formatted}</b> contra o ${uOpponent} (${uTorneio})!\n`
          upcomingSpoken = ` E atenção: o ${resolvedTeam.name} volta a campo ${uDt.spoken} contra o ${uOpponent} pelo ${uTorneio}.`
        }

        const text = `⚽ <b>Último Confronto • ${resolvedTeam.name}</b>\n\n` +
                     `🏁 <b>${h} ${hs} x ${as} ${a}</b>\n` +
                     `🏆 <b>${torneio}</b>${fase}\n` +
                     `📅 ${dt.formatted} • <b>Encerrado</b>\n` +
                     venue +
                     upcomingNote +
                     `\n⚡ <i>Lances e estatísticas completos no app Tessera!</i>`

        const spokenText = `No último jogo pela ${torneio}, o resultado foi ${h} ${hs}, ${a} ${as}.${upcomingSpoken}`

        const replyMarkup = {
          inline_keyboard: [
            [
              { text: `📅 Próximo Jogo do ${resolvedTeam.name}`, callback_data: `soccer_team:${resolvedTeam.name.toLowerCase()}` },
              { text: "🏆 Ver Tabela", callback_data: "soccer_table" }
            ]
          ]
        }

        return { text, spokenText, replyMarkup }
      }

      // Se o jogo desta rodada ainda não ocorreu ou equipe não jogou hoje, busca último do TheSportsDB
      try {
        const res = await fetch(`https://www.thesportsdb.com/api/v1/json/3/eventslast.php?id=${resolvedTeam.id}`)
        if (res.ok) {
          const data = await res.json()
          const event = data.results?.[0] || data.events?.[0]
          if (event) {
            const h = event.strHomeTeam || "Casa"
            const a = event.strAwayTeam || "Fora"
            const hs = event.intHomeScore ?? 0
            const as = event.intAwayScore ?? 0
            const league = event.strLeague === "Brazilian Serie A" ? "Brasileirão Série A" : (event.strLeague || "Brasileirão")
            const dt = formatMatchDateTime(event.dateEvent, event.strTime)
            const venue = event.strVenue ? `🏟️ ${event.strVenue}\n` : ""

            let upcomingNote = ""
            let upcomingSpoken = ""
            if (upcomingMatches.length > 0) {
              const u = upcomingMatches[0]
              const uH = u.equipes.mandante.nome_popular || u.equipes.mandante.nome
              const uA = u.equipes.visitante.nome_popular || u.equipes.visitante.nome
              const uDt = formatGEDateTime(u.data_realizacao, u.hora_realizacao)
              const uOpponent = resolvedTeam.name.toLowerCase().includes(uH.toLowerCase()) ? uA : uH
              const uTorneio = u.torneio || "Futebol"
              upcomingNote = `\n🔔 <b>Atenção:</b> O ${resolvedTeam.name} entra em campo <b>${uDt.formatted}</b> contra o ${uOpponent} (${uTorneio})!\n`
              upcomingSpoken = ` E atenção: o ${resolvedTeam.name} volta a campo ${uDt.spoken} contra o ${uOpponent} pelo ${uTorneio}.`
            }

            const text = `⚽ <b>Último Confronto • ${resolvedTeam.name}</b>\n\n` +
                         `🏁 <b>${h} ${hs} x ${as} ${a}</b>\n` +
                         `🏆 <b>${league}</b>${event.intRound ? ` • ${event.intRound}ª Rodada` : ""}\n` +
                         `📅 ${dt.formatted} • <b>Encerrado</b>\n` +
                         venue +
                         upcomingNote +
                         `\n⚡ <i>Lances e estatísticas completos no app Tessera!</i>`

            const spokenText = `No último jogo concluído pelo ${league}, o placar foi ${h} ${hs}, ${a} ${as}.${upcomingSpoken}`

            const replyMarkup = {
              inline_keyboard: [
                [
                  { text: `📅 Ver Próximo Jogo do ${resolvedTeam.name}`, callback_data: `soccer_team:${resolvedTeam.name.toLowerCase()}` },
                  { text: "🏆 Ver Tabela", callback_data: "soccer_table" }
                ]
              ]
            };

            return { text, spokenText, replyMarkup }
          }
        }
      } catch (err) {
        console.error("Erro ao buscar último jogo TheSportsDB:", err)
      }
    }

    // C: Consulta de Próximo Jogo (Default)
    if (upcomingMatches.length > 0) {
      const nextMatch = upcomingMatches[0]
      const h = nextMatch.equipes.mandante.nome_popular || nextMatch.equipes.mandante.nome
      const a = nextMatch.equipes.visitante.nome_popular || nextMatch.equipes.visitante.nome
      const dt = formatGEDateTime(nextMatch.data_realizacao, nextMatch.hora_realizacao)
      const venue = nextMatch.sede?.nome_popular ? `🏟️ ${nextMatch.sede.nome_popular}\n` : ""
      const fase = nextMatch.faseNome ? ` • ${nextMatch.faseNome}` : ""
      const torneio = nextMatch.torneio || "Competição Oficial"
      const isHome = resolvedTeam.name.toLowerCase().includes(h.toLowerCase())
      const mandoStr = isHome ? "(Em casa)" : "(Fora de casa)"
      const opponent = isHome ? a : h

      let subsequentList = ""
      if (upcomingMatches.length > 1) {
        subsequentList = "\n🗓️ <b>Jogos Seguintes:</b>\n"
        upcomingMatches.slice(1, 3).forEach((sub: any) => {
          const sH = sub.equipes.mandante.nome_popular || sub.equipes.mandante.nome
          const sA = sub.equipes.visitante.nome_popular || sub.equipes.visitante.nome
          const sDt = formatGEDateTime(sub.data_realizacao, sub.hora_realizacao)
          subsequentList += `• <b>${sDt.formatted}</b>: ${sH} vs ${sA} (${sub.torneio})\n`
        })
      }

      const text = `⚽ <b>Próximo Jogo • ${resolvedTeam.name}</b>\n\n` +
                   `⚔️ <b>${h} vs ${a}</b> ${mandoStr}\n` +
                   `🏆 <b>${torneio}</b>${fase}\n` +
                   `📅 <b>${dt.formatted}</b> (Horário de Brasília)\n` +
                   venue +
                   subsequentList +
                   `\n⚡ <i>Acompanhe escalações e estatísticas ao vivo na aba de Futebol do seu app Tessera!</i>`

      const spokenText = `O próximo jogo do ${resolvedTeam.name} é contra o ${opponent}, ${dt.spoken}${nextMatch.sede?.nome_popular ? `, no ${nextMatch.sede.nome_popular}` : ""}, pelo ${torneio}.`

      const replyMarkup = {
        inline_keyboard: [
          [
            { text: "🏆 Tabela do Brasileirão", callback_data: "soccer_table" },
            { text: "🏁 Último Resultado", callback_data: `soccer_last:${resolvedTeam.name.toLowerCase()}` }
          ]
        ]
      }

      return { text, spokenText, replyMarkup }
    }

    // Fallback para TheSportsDB para próximos jogos
    try {
      const res = await fetch(`https://www.thesportsdb.com/api/v1/json/3/eventsnext.php?id=${resolvedTeam.id}`)
      if (res.ok) {
        const data = await res.json()
        const event = data.events?.[0]
        if (event) {
          const h = event.strHomeTeam || "Casa"
          const a = event.strAwayTeam || "Fora"
          const league = event.strLeague === "Brazilian Serie A" ? "Brasileirão Série A" : (event.strLeague || "Brasileirão")
          const dt = formatMatchDateTime(event.dateEvent, event.strTime)
          const venue = event.strVenue ? `🏟️ ${event.strVenue}\n` : ""
          const round = event.intRound ? ` • ${event.intRound}ª Rodada` : ""
          const opponent = resolvedTeam.name.toLowerCase().includes(h.toLowerCase()) ? a : h
          const isHome = resolvedTeam.name.toLowerCase().includes(h.toLowerCase())
          const mandoStr = isHome ? "(Em casa)" : "(Fora de casa)"

          const text = `⚽ <b>Próximo Jogo • ${resolvedTeam.name}</b>\n\n` +
                       `⚔️ <b>${h} vs ${a}</b> ${mandoStr}\n` +
                       `🏆 <b>${league}</b>${round}\n` +
                       `📅 <b>${dt.formatted}</b> (Horário de Brasília)\n` +
                       venue +
                       `\n⚡ <i>Acompanhe escalações e estatísticas ao vivo na aba de Futebol do seu app Tessera!</i>`

          const spokenText = `O próximo jogo do ${resolvedTeam.name} é contra o ${opponent}, ${dt.spoken}${event.strVenue ? `, no ${event.strVenue}` : ""}, pelo ${league}.`

          const replyMarkup = {
            inline_keyboard: [
              [
                { text: "🏆 Tabela do Brasileirão", callback_data: "soccer_table" },
                { text: "🏁 Último Resultado", callback_data: `soccer_last:${resolvedTeam.name.toLowerCase()}` }
              ]
            ]
          }

          return { text, spokenText, replyMarkup }
        }
      }
    } catch (err) {
      console.error("Erro ao buscar próximo jogo TheSportsDB:", err)
    }
  }

  // 3. Resposta Geral / Central de Futebol
  const text = `⚽ <b>Futebol & Brasileirão • Central de Jogos</b>\n\n` +
               `Você pode me perguntar a qualquer momento:\n` +
               `• <i>"Qual o próximo jogo do Flamengo?"</i>\n` +
               `• <i>"Quanto foi o último jogo do Palmeiras?"</i>\n` +
               `• <i>"Como tá a tabela do Brasileirão?"</i>\n` +
               `• <i>"Quando o Corinthians joga?"</i>\n\n` +
               `Toque em um dos botões abaixo para ver agora:`

  const spokenText = "Você pode me perguntar sobre os próximos jogos de qualquer time, os últimos resultados ou a classificação do Brasileirão. Sobre qual time você deseja saber?"

  const replyMarkup = {
    inline_keyboard: [
      [
        { text: "🏆 Tabela do Brasileirão", callback_data: "soccer_table" }
      ],
      [
        { text: "🔴 Próximo do Flamengo", callback_data: "soccer_team:flamengo" },
        { text: "🟢 Próximo do Palmeiras", callback_data: "soccer_team:palmeiras" }
      ],
      [
        { text: "⚪ Próximo do Corinthians", callback_data: "soccer_team:corinthians" },
        { text: "⚫ Próximo do São Paulo", callback_data: "soccer_team:sao paulo" }
      ]
    ]
  }

  return { text, spokenText, replyMarkup }
}

// ============================================================================
// MÓDULO MEU APÊ / EVOLUÇÃO DA OBRA & REFORMA
// ============================================================================
interface ApartmentState {
  progress: number
  expected_date: string
  budget_total: number
  spent_total: number
  phases: Array<{ name: string; percent: number; status: "done" | "in_progress" | "pending" }>
  recent_expenses: Array<{ title: string; amount: number; date: string }>
}

async function getApartmentDoc(): Promise<ApartmentState> {
  try {
    const docs = await supabaseRest(`telegram_bot_logs?action=eq.apartment_state&select=*&order=created_at.desc&limit=1`)
    if (Array.isArray(docs) && docs.length > 0 && docs[0].payload) {
      return docs[0].payload
    }
  } catch (err) {
    console.error("Erro ao carregar apartment_state:", err)
  }
  return {
    progress: 0.78,
    expected_date: "Dez 2026",
    budget_total: 120000,
    spent_total: 45200,
    phases: [
      { name: "Alvenaria e Demolição", percent: 100, status: "done" },
      { name: "Elétrica e Hidráulica", percent: 100, status: "done" },
      { name: "Pisos e Revestimentos", percent: 70, status: "in_progress" },
      { name: "Pintura e Gesso", percent: 40, status: "in_progress" },
      { name: "Marcenaria e Móveis", percent: 15, status: "pending" }
    ],
    recent_expenses: [
      { title: "Porcelanato e Pisos", amount: 4800, date: "10/09" },
      { title: "Argamassa e Tintas", amount: 650, date: "11/09" }
    ]
  }
}

async function saveApartmentDoc(data: ApartmentState, userId = "admin"): Promise<void> {
  try {
    await supabaseRest("telegram_bot_logs", {
      method: "POST",
      body: JSON.stringify({
        telegram_user_id: userId,
        action: "apartment_state",
        payload: data
      })
    })
  } catch (err) {
    console.error("Erro ao salvar apartment_state:", err)
  }
}

function renderProgressBar(fraction: number, length = 10): string {
  const percent = Math.min(Math.max(fraction, 0), 1)
  const filled = Math.round(percent * length)
  const empty = length - filled
  return "▓".repeat(filled) + "░".repeat(empty)
}

function formatApartmentCard(data: ApartmentState): { text: string; spokenText: string; replyMarkup: any } {
  const pct = Math.round(data.progress * 100)
  const bar = renderProgressBar(data.progress, 12)
  const spent = Number(data.spent_total || 0).toFixed(2).replace(".", ",")
  const budget = Number(data.budget_total || 0).toFixed(2).replace(".", ",")

  let text = `🏗️ <b>Evolução da Obra • Meu Apê</b>\n\n` +
             `📊 <b>Progresso:</b> <code>[${bar}] ${pct}% Concluído</code>\n` +
             `📅 <b>Previsão de Entrega:</b> ${data.expected_date || "Dez 2026"}\n` +
             `💰 <b>Total Investido na Reforma:</b> R$ ${spent}\n`
  if (data.budget_total > 0) {
    text += `💵 <b>Orçamento Estimado:</b> R$ ${budget}\n`
  }

  if (Array.isArray(data.phases) && data.phases.length > 0) {
    text += `\n🔨 <b>Etapas da Obra:</b>\n`
    data.phases.forEach((ph) => {
      const icon = ph.status === "done" ? "✅" : ph.status === "in_progress" ? "⏳" : "⚪"
      text += `${icon} <b>${ph.name}:</b> ${ph.percent}%\n`
    })
  }

  if (Array.isArray(data.recent_expenses) && data.recent_expenses.length > 0) {
    text += `\n🧾 <b>Últimos Gastos Registrados:</b>\n`
    data.recent_expenses.slice(0, 3).forEach((ex) => {
      const val = Number(ex.amount || 0).toFixed(2).replace(".", ",")
      text += `• ${ex.title}: R$ ${val} (${ex.date || "recente"})\n`
    })
  }

  text += `\n⚡ <i>Diga "atualiza a obra para 80%" ou "adicionei gasto de R$ X na obra" a qualquer momento!</i>`

  const spokenText = `A obra do seu apartamento está em ${pct}% de conclusão, com previsão de entrega para ${data.expected_date || "dezembro de 2026"}. O total investido até agora é de ${spent} reais.`

  const replyMarkup = {
    inline_keyboard: [
      [
        { text: "📱 Ver Maquete no Mini App", web_app: { url: "https://tessera-35c54.web.app" } }
      ]
    ]
  }

  return { text, spokenText, replyMarkup }
}

// ============================================================================
// MÓDULO SAÚDE & BEM-ESTAR (ÁGUA, PESO, PASSOS, SONO)
// ============================================================================
interface HealthState {
  today_water_ml: number
  water_goal_ml: number
  today_steps: number
  steps_goal: number
  latest_weight: number
  latest_sleep_hours: number
  date: string
}

async function getHealthDoc(): Promise<HealthState> {
  const todayStr = new Date().toISOString().split("T")[0]
  try {
    const docs = await supabaseRest(`telegram_bot_logs?action=eq.health_state&select=*&order=created_at.desc&limit=1`)
    if (Array.isArray(docs) && docs.length > 0 && docs[0].payload) {
      const d = docs[0].payload
      if (d.date !== todayStr) {
        return {
          ...d,
          today_water_ml: 0,
          today_steps: 0,
          date: todayStr
        }
      }
      return d
    }
  } catch (err) {
    console.error("Erro ao carregar health_state:", err)
  }
  return {
    today_water_ml: 0,
    water_goal_ml: 2500,
    today_steps: 0,
    steps_goal: 10000,
    latest_weight: 74.2,
    latest_sleep_hours: 7.5,
    date: todayStr
  }
}

async function saveHealthDoc(data: HealthState, userId = "admin"): Promise<void> {
  try {
    await supabaseRest("telegram_bot_logs", {
      method: "POST",
      body: JSON.stringify({
        telegram_user_id: userId,
        action: "health_state",
        payload: data
      })
    })
  } catch (err) {
    console.error("Erro ao salvar health_state:", err)
  }
}

function formatHealthCard(data: HealthState): { text: string; spokenText: string; replyMarkup: any } {
  const water = data.today_water_ml || 0
  const waterGoal = data.water_goal_ml || 2500
  const waterPct = Math.min(Math.round((water / waterGoal) * 100), 100)
  const waterBar = renderProgressBar(water / waterGoal, 10)

  const steps = data.today_steps || 0
  const stepsGoal = data.steps_goal || 10000
  const stepsPct = Math.min(Math.round((steps / stepsGoal) * 100), 100)

  let text = `🩺 <b>Saúde & Hábitos Diários • Tessera</b>\n\n` +
             `💧 <b>Hidratação Hoje:</b> <code>[${waterBar}] ${water}ml</code> / ${waterGoal}ml (${waterPct}%)\n` +
             `🚶 <b>Passos:</b> <b>${steps.toLocaleString("pt-BR")}</b> / ${stepsGoal.toLocaleString("pt-BR")} (${stepsPct}%)\n`
  if (data.latest_weight) {
    text += `⚖️ <b>Peso Atual:</b> <b>${data.latest_weight} kg</b>\n`
  }
  if (data.latest_sleep_hours) {
    text += `😴 <b>Último Sono:</b> <b>${data.latest_sleep_hours}h</b> registradas\n`
  }

  text += `\n💡 <i>Diga "bebi 500ml de água", "pesei 74kg" ou "dormi 8 horas" para registrar na hora!</i>`

  const spokenText = `Você já bebeu ${water} ml de água hoje, o que representa ${waterPct}% da sua meta diária. Você deu ${steps} passos e seu último peso registrado foi de ${data.latest_weight || 74} quilos.`

  const replyMarkup = {
    inline_keyboard: [
      [
        { text: "💧 +250ml Água", callback_data: "health_water:250" },
        { text: "💧 +500ml Água", callback_data: "health_water:500" }
      ]
    ]
  }

  return { text, spokenText, replyMarkup }
}

// ============================================================================
// BRIEFING MATINAL INTELIGENTE (PROATIVO & SOB DEMANDA)
// ============================================================================
async function generateMorningBriefing(userFirstName = "Kenned"): Promise<{ text: string; spokenText: string; replyMarkup: any }> {
  const now = new Date()
  const dateFormatted = new Intl.DateTimeFormat("pt-BR", {
    timeZone: "America/Sao_Paulo",
    weekday: "long",
    day: "2-digit",
    month: "long",
    year: "numeric"
  }).format(now)
  const capDate = dateFormatted.charAt(0).toUpperCase() + dateFormatted.slice(1)

  // 1. Clima de São Paulo
  let weatherText = "☀️ Clima ameno em São Paulo."
  let weatherSpoken = "O dia começa agradável."
  try {
    const res = await fetch("https://api.open-meteo.com/v1/forecast?latitude=-23.5505&longitude=-46.6333&daily=temperature_2m_max,temperature_2m_min,precipitation_probability_max,weathercode&current_weather=true&timezone=America%2FSao_Paulo")
    if (res.ok) {
      const d = await res.json()
      const cur = d.current_weather
      const daily = d.daily
      const maxT = Math.round(daily.temperature_2m_max[0])
      const minT = Math.round(daily.temperature_2m_min[0])
      const rainProb = daily.precipitation_probability_max[0] ?? 0
      const curT = Math.round(cur.temperature)
      const rainTip = rainProb > 40 ? `🌧️ Chance de chuva de ${rainProb}% (leve um guarda-chuva!)` : `☀️ Sem previsão de chuva (${rainProb}%)`
      weatherText = `🌡️ <b>${curT}°C</b> (Máx: ${maxT}°C | Mín: ${minT}°C)\n${rainTip}`
      weatherSpoken = `Em São Paulo temos temperatura de ${curT} graus, máxima de ${maxT} e ${rainProb > 40 ? "chance de chuva ao longo do dia" : "tempo estável"}.`
    }
  } catch (_e) {}

  // 2. Tarefas prioritárias
  let tasksText = "<i>Nenhuma tarefa urgente agendada para hoje! Aproveite para focar em novos projetos.</i>"
  let tasksSpoken = "Você não tem nenhuma tarefa urgente para hoje."
  try {
    const taskDoc = await getTasksDoc()
    const items = taskDoc && Array.isArray(taskDoc.data.items) ? taskDoc.data.items : []
    const pending = items.filter((it: any) => it.status === "pending")
    if (pending.length > 0) {
      tasksText = ""
      pending.slice(0, 3).forEach((it: any, i: number) => {
        tasksText += `${i + 1}. <b>${it.title}</b>${it.due_time ? ` (às ${it.due_time})` : ""}\n`
      })
      if (pending.length > 3) {
        tasksText += `<i>... e mais ${pending.length - 3} tarefas no painel.</i>\n`
      }
      tasksSpoken = `Você tem ${pending.length} tarefa${pending.length > 1 ? "s" : ""} na sua lista, começando por ${pending[0].title}.`
    }
  } catch (_e) {}

  // 3. Finanças, Cartões, Vencimentos e Receitas
  let financeDetails = ""
  let financeSpoken = ""
  try {
    const finDoc = await getFinanceDoc()
    if (finDoc) {
      const d = finDoc.data
      const spendable = Number(d.spendable_balance || 0).toFixed(2).replace(".", ",")
      const total = Number(d.total_balance || 0).toFixed(2).replace(".", ",")
      const salary = Number(d.salary_value || 0)

      financeDetails += `• <b>Saldo Livre:</b> R$ ${spendable}\n`
      financeDetails += `• <b>Saldo Total:</b> R$ ${total}\n`
      if (salary > 0) {
        financeDetails += `• <b>Receitas Previstas:</b> R$ ${salary.toFixed(2).replace(".", ",")}\n`
      }

      // Cartões com vencimento
      const currentDay = now.getDate()
      if (Array.isArray(d.cards) && d.cards.length > 0) {
        financeDetails += `\n💳 <b>Cartões de Crédito:</b>\n`
        d.cards.forEach((c: any) => {
          const used = Number(c.usedLimit || c.used_limit || 0)
          const dueDay = c.dueDay || c.due_day || 20
          const diff = dueDay - currentDay
          const dueMsg = diff === 0 ? "⚠️ Vence HOJE!" : diff > 0 && diff <= 7 ? `Vence dia ${dueDay} (em ${diff} dias)` : `Vence dia ${dueDay}`
          financeDetails += `• <b>${c.name}:</b> Fatura R$ ${used.toFixed(2).replace(".", ",")} (${dueMsg})\n`
        })
      }

      // Contas a vencer
      if (Array.isArray(d.recurrents) && d.recurrents.length > 0) {
        let upcomingBills = ""
        d.recurrents.forEach((r: any) => {
          const dueDay = r.dueDay || r.due_day || r.day
          if (dueDay) {
            const diff = dueDay - currentDay
            if (diff >= 0 && diff <= 7) {
              const val = Number(r.amount || r.value || 0).toFixed(2).replace(".", ",")
              upcomingBills += `• <b>${r.name}:</b> R$ ${val} (dia ${dueDay})\n`
            }
          }
        })
        if (upcomingBills) {
          financeDetails += `\n📅 <b>Contas a Vencer nos Próximos 7 Dias:</b>\n${upcomingBills}`
        }
      }

      financeSpoken = `Seu saldo livre está em ${spendable} reais.`
    }
  } catch (_e) {}

  // 4. Obra do Apê
  let aptSummary = ""
  try {
    const apt = await getApartmentDoc()
    const pct = Math.round(apt.progress * 100)
    const bar = renderProgressBar(apt.progress, 10)
    aptSummary = `📊 <code>[${bar}] ${pct}% Concluído</code> • Entrega: ${apt.expected_date || "Dez 2026"}\n💰 Investido: R$ ${Number(apt.spent_total || 0).toFixed(2).replace(".", ",")}`
  } catch (_e) {}

  // 5. Saúde & Hidratação
  let healthSummary = ""
  try {
    const hl = await getHealthDoc()
    healthSummary = `💧 <b>Hidratação:</b> ${hl.today_water_ml}ml / ${hl.water_goal_ml}ml • <i>Hora de tomar o 1º copo d'água!</i>`
  } catch (_e) {}

  // 6. Futebol / Mengão
  let soccerSection = ""
  let soccerSpoken = ""
  try {
    const soccerData = await fetchGEMultiCompetitionData()
    if (soccerData?.matches) {
      const todayIso = now.toISOString().split("T")[0]
      const flaToday = soccerData.matches.find((m: any) => {
        const d = (m.data_realizacao || "").split("T")[0]
        const mand = (m.equipes?.mandante?.nome_popular || "").toLowerCase()
        const visi = (m.equipes?.visitante?.nome_popular || "").toLowerCase()
        return d === todayIso && (mand.includes("flamengo") || visi.includes("flamengo"))
      })

      if (flaToday) {
        const h = flaToday.equipes.mandante.nome_popular
        const a = flaToday.equipes.visitante.nome_popular
        const time = flaToday.hora_realizacao || (flaToday.data_realizacao.includes("T") ? flaToday.data_realizacao.split("T")[1].slice(0, 5) : "17:30")
        const venue = flaToday.sede?.nome_popular ? ` no ${flaToday.sede.nome_popular}` : ""
        soccerSection = `🔥 <b>Hoje tem Mengão em campo!</b>\n<b>${h} vs ${a}</b> às <b>${time}</b>${venue} (${flaToday.torneio || "Futebol"})`
        soccerSpoken = ` E atenção: hoje tem jogo do Flamengo contra o ${h.toLowerCase().includes("flamengo") ? a : h} às ${time}!`
      } else {
        const upcomingFla = soccerData.matches.filter((m: any) => {
          const mand = (m.equipes?.mandante?.nome_popular || "").toLowerCase()
          const visi = (m.equipes?.visitante?.nome_popular || "").toLowerCase()
          return !m.jogo_ja_comecou && (mand.includes("flamengo") || visi.includes("flamengo"))
        }).sort((a: any, b: any) => getMatchTimestamp(a) - getMatchTimestamp(b))[0]

        if (upcomingFla) {
          const uH = upcomingFla.equipes.mandante.nome_popular
          const uA = upcomingFla.equipes.visitante.nome_popular
          const dt = formatGEDateTime(upcomingFla.data_realizacao, upcomingFla.hora_realizacao)
          soccerSection = `⚽ <b>Próximo Jogo do Flamengo:</b>\n${uH} vs ${uA} — <b>${dt.formatted}</b> (${upcomingFla.torneio})`
        }
      }
    }
  } catch (_e) {}

  let text = `☀️ <b>Bom dia, ${userFirstName}! • Seu Briefing Tessera</b>\n` +
             `<i>${capDate}</i>\n\n` +
             `🌤️ <b>Clima & Tempo:</b>\n${weatherText}\n\n` +
             `⏰ <b>Prioridades de Hoje:</b>\n${tasksText}\n\n`

  if (financeDetails) {
    text += `💰 <b>Termômetro Financeiro:</b>\n${financeDetails}\n`
  }

  if (aptSummary) {
    text += `🏗️ <b>Obra do Apê:</b>\n${aptSummary}\n\n`
  }

  if (healthSummary) {
    text += `🩺 <b>Saúde & Hábitos:</b>\n${healthSummary}\n\n`
  }

  if (soccerSection) {
    text += `⚽ <b>Radar do Futebol:</b>\n${soccerSection}\n\n`
  }

  text += `📖 <b>Reflexão do Dia:</b>\n` +
          `<i>"A disciplina diária é o combustível silencioso das grandes realizações."</i>\n\n` +
          `⚡ <i>Tenha um dia extraordinário e abençoado!</i>`

  const spokenText = `Bom dia, ${userFirstName}! Aqui está o seu briefing matinal para ${capDate}. ${weatherSpoken} ${tasksSpoken} ${financeSpoken}${soccerSpoken} Tenha um excelente dia!`

  const replyMarkup = {
    inline_keyboard: [
      [
        { text: "📱 Abrir Tessera Hub", web_app: { url: "https://tessera-35c54.web.app" } }
      ],
      [
        { text: "🏗️ Ver Obra", callback_data: "menu_apartment" },
        { text: "🩺 Ver Saúde", callback_data: "menu_health" },
        { text: "💰 Ver Saldo", callback_data: "menu_saldo" }
      ]
    ]
  }

  return { text, spokenText, replyMarkup }
}

// ============================================================================
// CÉREBRO MULTIMODAL GROQ (WHISPER LARGE V3 TURBO + LLAMA / QWEN VISION)
// ============================================================================
interface GroqIntentResponse {
  action: "add_transaction" | "get_finances" | "get_chart" | "export_csv" | "add_task" | "get_tasks" | "query_wishes" | "complete_wish" | "add_wish" | "add_market_items" | "get_market_items" | "get_weather" | "get_soccer" | "get_briefing" | "get_apartment" | "update_apartment" | "get_health" | "update_health" | "chat_general"
  transcription?: string
  transaction?: {
    title: string
    amount: number
    type: "expense" | "income"
    category?: string
    account_or_card_name?: string
    date?: string
  }
  task?: {
    title: string
    description?: string
    due_date_str?: string
    due_time?: string
  }
  wish?: {
    title: string
    target_value?: number
    category?: string
  }
  market_items?: Array<{
    name: string
    quantity?: number
    unit?: string
    category?: string
  }>
  soccer?: {
    team?: string
    type?: "next" | "last" | "standings" | "general"
  }
  apartment?: {
    progress?: number
    spent_amount?: number
    expense_title?: string
    phase?: string
    expected_date?: string
  }
  health?: {
    water_ml?: number
    weight?: number
    steps?: number
    sleep_hours?: number
  }
  query?: string
  location?: string
  reply_text?: string
}

async function transcribeAudioWithGroq(audioBuffer: ArrayBuffer, mimeType: string): Promise<string> {
  const audioBlob = new Blob([audioBuffer], { type: mimeType || "audio/ogg" })
  const formData = new FormData()
  formData.append("file", audioBlob, "audio.ogg")
  formData.append("model", "whisper-large-v3-turbo")
  formData.append("language", "pt")
  formData.append("temperature", "0")

  const res = await fetch("https://api.groq.com/openai/v1/audio/transcriptions", {
    method: "POST",
    headers: {
      "Authorization": `Bearer ${GROQ_API_KEY}`,
    },
    body: formData,
  })

  if (!res.ok) {
    const errText = await res.text()
    console.error("Erro na transcrição Groq Whisper:", res.status, errText)
    throw new Error(`Erro na transcrição de áudio com Groq Whisper: ${errText}`)
  }

  const data = await res.json()
  return data.text || ""
}

async function processReceiptPhotoWithGroq(imageDataUrl: string): Promise<{
  establishment: string
  amount: number
  category: string
  date: string | null
  description: string
}> {
  const prompt = `Analise com atenção a imagem deste cupom fiscal, nota fiscal ou comprovante de pagamento/PIX.
Extraia com precisão os dados financeiros e responda EXCLUSIVAMENTE em formato json_object com o schema:
{
  "establishment": "nome da empresa, estabelecimento comercial ou favorecido do PIX",
  "amount": 25.50,
  "category": "Alimentação" | "Mercado" | "Transporte" | "Saúde" | "Lazer" | "Moradia" | "Serviços" | "Geral",
  "date": "YYYY-MM-DD",
  "description": "resumo dos itens comprados ou do comprovante"
}
Se não identificar o estabelecimento, use "Despesa por Comprovante".
O valor deve ser um número float no campo amount (ex: 45.90).`

  const res = await fetch("https://api.groq.com/openai/v1/chat/completions", {
    method: "POST",
    headers: {
      "Authorization": `Bearer ${GROQ_API_KEY}`,
      "Content-Type": "application/json"
    },
    body: JSON.stringify({
      model: "qwen/qwen3.8-27b",
      messages: [
        {
          role: "user",
          content: [
            { type: "text", text: prompt },
            { type: "image_url", image_url: { url: imageDataUrl } }
          ]
        }
      ],
      temperature: 0.1,
      response_format: { type: "json_object" }
    })
  })

  if (!res.ok) {
    const errText = await res.text()
    console.error("Erro no OCR Groq Vision:", res.status, errText)
    throw new Error(`Erro na leitura óptica do comprovante: ${errText}`)
  }

  const data = await res.json()
  const content = data.choices?.[0]?.message?.content || "{}"
  try {
    return JSON.parse(content)
  } catch (err) {
    console.error("Erro ao fazer parse do OCR JSON:", content, err)
    return {
      establishment: "Comprovante",
      amount: 0.0,
      category: "Geral",
      date: new Date().toISOString().split("T")[0],
      description: "Não foi possível extrair os dados com clareza."
    }
  }
}

async function processBankStatementPdfWithGemini(pdfBase64: string): Promise<{
  bankName: string
  period?: string
  totalIncome: number
  totalExpense: number
  transactions: Array<{
    date: string
    title: string
    amount: number
    type: "expense" | "income"
    category: string
  }>
}> {
  const geminiKey = Deno.env.get("GEMINI_API_KEY") || ""
  if (!geminiKey) {
    throw new Error("GEMINI_API_KEY não configurada para análise de documentos PDF.")
  }

  const prompt = `Você é um analista contábil e de conciliação bancária sênior do aplicativo financeiro Tessera.
Analise com extrema precisão este extrato bancário em PDF (ex: Itaú, Nubank, Bradesco, Santander, Inter, etc.).
Diretrizes estritas de leitura:
1. Identifique o banco emissor (ex: "Itaú", "Nubank", etc.) e o período do extrato (ex: "01/09/2026 a 12/09/2026").
2. Identifique cada transação financeira individual do período:
   - "date": data no formato YYYY-MM-DD (use o ano do extrato).
   - "title": descrição limpa e legível do lançamento (ex: "Supermercado Pão de Açúcar", "Posto Shell", "PIX Enviado - João", "Salário", etc. - remova códigos de transação ou numerações inúteis).
   - "amount": valor numérico estritamente positivo (ex: 45.90).
   - "type": "expense" se for saída/débito/pagamento/compra; "income" se for entrada/crédito/salário/PIX recebido.
   - "category": uma categoria concisa (ex: "Mercado", "Alimentação", "Transporte", "Moradia", "Saúde", "Lazer", "Salário", "Serviços", "Geral").
3. NUNCA inclua linhas de "Saldo Anterior", "Saldo do Dia", "Saldo Final", "Total de Débitos", "Bloqueios" ou totais acumulados como transações. Apenas eventos reais de movimentação.
4. Calcule "totalIncome" (soma das entradas), "totalExpense" (soma das saídas) e liste todas as transações em "transactions".

Responda APENAS com um objeto JSON válido no formato:
{
  "bankName": "Itaú",
  "period": "01/09/2026 a 12/09/2026",
  "totalIncome": 4500.00,
  "totalExpense": 1830.45,
  "transactions": [
    { "date": "2026-09-02", "title": "Supermercado Pão de Açúcar", "amount": 342.10, "type": "expense", "category": "Mercado" }
  ]
}`

  const url = `https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=${geminiKey}`
  const body = {
    contents: [
      {
        parts: [
          {
            inlineData: {
              mimeType: "application/pdf",
              data: pdfBase64
            }
          },
          { text: prompt }
        ]
      }
    ],
    generationConfig: {
      temperature: 0.1,
      responseMimeType: "application/json"
    }
  }

  let response = await fetch(url, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(body)
  })

  // Fallback automático para gemini-1.5-flash se 2.5 não disponível
  if (!response.ok && response.status === 404) {
    const fallbackUrl = `https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=${geminiKey}`
    response = await fetch(fallbackUrl, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(body)
    })
  }

  if (!response.ok) {
    const errText = await response.text()
    console.error("Erro no Gemini PDF Processing:", response.status, errText)
    throw new Error(`Erro na análise do extrato bancário com Gemini: ${errText}`)
  }

  const json = await response.json()
  const candidate = json.candidates?.[0]?.content?.parts?.[0]?.text || "{}"
  return JSON.parse(candidate)
}

async function processWithGroq(
  input: { text?: string; audioBuffer?: ArrayBuffer; audioMimeType?: string },
  contextInfo: { todayDate: string; userFirstName: string }
): Promise<GroqIntentResponse> {
  let promptText = input.text || ""
  let audioTranscription = ""

  // 1. Transcrever áudio de voz com Groq Whisper se enviado
  if (input.audioBuffer) {
    audioTranscription = await transcribeAudioWithGroq(input.audioBuffer, input.audioMimeType || "audio/ogg")
    promptText = audioTranscription
  }

  const systemPrompt = `Você é o assistente pessoal, conselheiro e hub inteligente de IA do ecossistema Tessera para o usuário ${contextInfo.userFirstName}.
Data de hoje: ${contextInfo.todayDate}.
Você possui vasto conhecimento enciclopédico e analítico sobre história, teologia, filosofia, ciências, literatura, tecnologia, finanças pessoais e cultura geral.

Você deve analisar o texto ou comando do usuário e responder EXCLUSIVAMENTE em formato JSON (json_object) estruturado com o seguinte schema:

{
  "action": "add_transaction" | "get_finances" | "get_chart" | "export_csv" | "add_task" | "get_tasks" | "query_wishes" | "complete_wish" | "add_wish" | "add_market_items" | "get_market_items" | "get_weather" | "get_soccer" | "get_briefing" | "get_apartment" | "update_apartment" | "get_health" | "update_health" | "chat_general",
  "transcription": "${audioTranscription ? audioTranscription.replace(/"/g, "'") : ""}",
  "transaction": {
    "title": "título curto e claro da despesa ou receita (ex: Padaria, Almoço, Salário, Gasolina)",
    "amount": 10.50,
    "type": "expense" ou "income",
    "category": "Alimentação" | "Transporte" | "Saúde" | "Lazer" | "Moradia" | "Educação" | "Serviços" | "Geral",
    "account_or_card_name": "nome do banco ou cartão se citado (ex: Nubank, Inter, C6, Itaú, Débito)",
    "date": "YYYY-MM-DD"
  },
  "task": {
    "title": "título do lembrete ou tarefa",
    "description": "detalhes opcionais",
    "due_date_str": "YYYY-MM-DD",
    "due_time": "HH:MM"
  },
  "wish": {
    "title": "nome do item da lista de desejos",
    "target_value": 0.0,
    "category": "Eletrônicos" | "Vestuário" | "Casa" | "Geral"
  },
  "market_items": [
    {
      "name": "nome do item (ex: Leite, Tomate, Café)",
      "quantity": 1.0,
      "unit": "un" ou "kg" ou "cx" ou "pct",
      "category": "Laticínios" ou "Hortifruti" ou "Mercearia" ou "Limpeza" ou "Geral"
    }
  ],
  "soccer": {
    "team": "nome do time de futebol se citado (ex: Flamengo, Palmeiras, Corinthians, Real Madrid)",
    "type": "next" ou "last" ou "standings" ou "general"
  },
  "apartment": {
    "progress": 75,
    "spent_amount": 1500.0,
    "expense_title": "Pisos e porcelanato",
    "phase": "Acabamento",
    "expected_date": "2026-11-30"
  },
  "health": {
    "water_ml": 500,
    "weight": 74.5,
    "steps": 6000,
    "sleep_hours": 7.5
  },
  "query": "termo chave para busca ou time de futebol",
  "location": "nome da cidade para clima",
  "reply_text": "resposta completa, inteligente, precisa e bem fundamentada em português para o usuário"
}

Regras:
1. Ao identificar despesa ou receita (ex: 'gastei 5 reais na padaria', 'paguei 30 no almoço no cartão nubank', 'recebi pix de 100'), defina action="add_transaction".
2. Se o usuário perguntar saldo, limites de cartão, faturas ou finanças gerais, defina action="get_finances".
3. Se pedir para lembrar de algo ou criar aviso (ex: 'me lembra de pagar a luz amanhã às 9h'), defina action="add_task".
4. Se disser que comprou algo que queria ou da lista de desejos (ex: 'comprei o fone', 'consegui comprar o tênis'), defina action="complete_wish".
5. Se pedir para adicionar algo na lista de desejos, defina action="add_wish".
6. Se pedir para adicionar produtos à lista de compras do supermercado (ex: 'adiciona 2 caixas de leite e café no mercado'), defina action="add_market_items" e preencha "market_items".
7. Se perguntar o que tem para comprar na lista de compras (ex: 'o que tem no mercado?', 'o que falta comprar?'), defina action="get_market_items".
8. Se perguntar do tempo ou chuva, defina action="get_weather".
9. Se perguntar de futebol, jogos, placares, próximos confrontos ou tabela do Brasileirão, defina action="get_soccer" e preencha "soccer".
10. Se pedir gráfico visual ou como estão os gastos por categoria (ex: 'me mostra um gráfico', 'gráfico de despesas'), defina action="get_chart".
11. Se pedir para baixar ou exportar o extrato em planilha/CSV (ex: 'me envia o extrato em excel', 'quero a planilha de gastos'), defina action="export_csv".
12. Caso seja uma pergunta sobre história, teologia, filosofia, ciências, tecnologia, literatura, conselhos ou conversa geral, defina action="chat_general" e elabore uma resposta rica, didática, completa e bem formulada no campo "reply_text".
13. Se o usuário pedir um briefing, resumo do dia, panorama matinal ou disser "bom dia" / "me atualiza de tudo", defina action="get_briefing".
14. Se o usuário perguntar da obra, status do apartamento ou quanto já gastou na reforma (ex: "como tá a obra?", "quanto gastei no apê?", "reforma do apê"), defina action="get_apartment".
15. Se o usuário pedir para atualizar a obra, mudar porcentagem da reforma ou lançar gasto na obra (ex: "atualiza a obra para 80%", "gastei 1500 na obra com pisos", "avançou para acabamento"), defina action="update_apartment" e preencha "apartment".
16. Se o usuário perguntar de saúde, água ingerida, peso ou sono (ex: "como tá minha saúde hoje?", "quanta água bebi?", "meta de água"), defina action="get_health".
17. Se o usuário registrar ingestão de água, peso, passos ou sono (ex: "bebi 500ml de água", "tomei um copo de água", "pesei 74.2kg", "dormi 8 horas"), defina action="update_health" e preencha "health" (para 'um copo de água', use water_ml=250; para 'garrafa de água', use water_ml=500).`

  let response = await fetch("https://api.groq.com/openai/v1/chat/completions", {
    method: "POST",
    headers: {
      "Authorization": `Bearer ${GROQ_API_KEY}`,
      "Content-Type": "application/json",
    },
    body: JSON.stringify({
      model: "openai/gpt-oss-120b",
      messages: [
        {
          role: "system",
          content: systemPrompt
        },
        {
          role: "user",
          content: promptText
        }
      ],
      temperature: 0.2,
      response_format: { type: "json_object" }
    }),
  })

  if (!response.ok && response.status === 404) {
    response = await fetch("https://api.groq.com/openai/v1/chat/completions", {
      method: "POST",
      headers: {
        "Authorization": `Bearer ${GROQ_API_KEY}`,
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        model: "openai/gpt-oss-20b",
        messages: [
          { role: "system", content: systemPrompt },
          { role: "user", content: promptText }
        ],
        temperature: 0.2,
        response_format: { type: "json_object" }
      }),
    })
  }

  if (!response.ok) {
    const errorText = await response.text()
    console.error("Erro na API Groq:", response.status, errorText)
    throw new Error(`Groq API error: ${response.statusText}`)
  }

  const data = await response.json()
  const candidateText = data.choices?.[0]?.message?.content || "{}"

  try {
    const parsed = JSON.parse(candidateText) as GroqIntentResponse
    if (audioTranscription && !parsed.transcription) {
      parsed.transcription = audioTranscription
    }
    return parsed
  } catch (err) {
    console.error("Erro ao analisar resposta JSON da Groq:", candidateText, err)
    return {
      action: "chat_general",
      transcription: audioTranscription || undefined,
      reply_text: "Entendi sua mensagem, mas não consegui processar a ação estruturada. Poderia repetir?"
    }
  }
}

// ============================================================================
// REGISTRO DE COMANDOS NATIVOS NO TELEGRAM
// ============================================================================
async function registerTelegramBotCommands(): Promise<void> {
  await tgCall("setMyCommands", {
    commands: [
      { command: "briefing", description: "🌅 Resumo completo do seu dia (áudio + texto)" },
      { command: "obra", description: "🏗️ Status e evolução da obra do apê" },
      { command: "saude", description: "🩺 Registro de água, peso e hábitos" },
      { command: "saldo", description: "💰 Ver saldo livre, contas e faturas" },
      { command: "futebol", description: "⚽ Próximos jogos, tabela e placares" },
      { command: "tabela", description: "🏆 Classificação da Série A" },
      { command: "grafico", description: "📊 Gráfico visual de gastos por categoria" },
      { command: "extrato", description: "📄 Baixar planilha CSV do extrato do mês" },
      { command: "mercado", description: "🛒 Ver lista de compras do supermercado" },
      { command: "lembretes", description: "⏰ Ver tarefas e avisos pendentes" },
      { command: "desejos", description: "🎁 Ver lista de desejos e metas" },
      { command: "tempo", description: "🌤️ Previsão do tempo e clima" },
      { command: "app", description: "📱 Abrir Tessera Mini App" },
      { command: "ajuda", description: "❓ Guia de comandos e como usar" }
    ]
  })
  await tgCall("setChatMenuButton", {
    menu_button: {
      type: "web_app",
      text: "📱 Tessera Hub",
      web_app: {
        url: "https://tessera-35c54.web.app"
      }
    }
  })
}

// ============================================================================
// DISPARO PROATIVO DE RESUMO MATINAL E NOTURNO (SUPABASE PG_CRON)
// ============================================================================
async function handleCronBriefing(cronEvent: string): Promise<void> {
  if (cronEvent === "morning_briefing") {
    try {
      const briefing = await generateMorningBriefing("Kenned")
      for (const userId of TELEGRAM_ALLOWED_USER_IDS) {
        await sendTelegramMessage(userId, briefing.text, briefing.replyMarkup)
        if (ENABLE_VOICE_RESPONSES && briefing.spokenText) {
          try {
            const audioBuf = await synthesizeSpeechFrancisca(briefing.spokenText)
            if (audioBuf) {
              await sendTelegramVoice(userId, audioBuf)
            }
          } catch (vErr) {
            console.error("Erro ao enviar áudio no cron briefing:", vErr)
          }
        }
      }
    } catch (err) {
      console.error("Erro ao gerar morning_briefing no cron:", err)
    }
  } else if (cronEvent === "night_briefing") {
    const finDoc = await getFinanceDoc()
    const marketDoc = await getMarketDoc()
    const marketItems = marketDoc && Array.isArray(marketDoc.data.items) ? marketDoc.data.items : []
    const pendingMarket = marketItems.filter((it: any) => !it.isChecked && !it.isBought)

    let todaySpent = 0
    let todayTxCount = 0
    const todayIso = new Date().toISOString().split("T")[0]
    if (finDoc && Array.isArray(finDoc.data.suggestions)) {
      for (const s of finDoc.data.suggestions) {
        if (s.date === todayIso && s.type === "expense") {
          todaySpent += Number(s.amount || 0)
          todayTxCount++
        }
      }
    }

    const message = `🌙 <b>Boa noite, Kenned! Resumo do seu dia:</b>\n\n` +
      `💸 <b>Despesas registradas hoje:</b> R$ ${todaySpent.toFixed(2).replace(".", ",")} (${todayTxCount} lançamentos)\n` +
      `🛒 <b>Lista de compras:</b> ${pendingMarket.length} itens pendentes\n\n` +
      `<i>Esqueceu de anotar algum gasto ou cupom fiscal de hoje? Envie uma mensagem ou foto agora para mantermos tudo atualizado! 😴✨</i>`

    for (const userId of TELEGRAM_ALLOWED_USER_IDS) {
      await sendTelegramMessage(userId, message)
    }
  }
}

// ============================================================================
// HANDLER PRINCIPAL DO WEBHOOK TELEGRAM
// ============================================================================
Deno.serve(async (req: Request) => {
  if (req.method === "OPTIONS") {
    return new Response("ok", { headers: { "Access-Control-Allow-Origin": "*" } })
  }

  if (req.method !== "POST") {
    return new Response("Tessera Telegram Bot Webhook Active", { status: 200 })
  }

  let update: any = null
  try {
    update = await req.json()
  } catch {
    return new Response("Invalid JSON", { status: 400 })
  }

  // Deduplicação de Webhook do Telegram (Impede re-execução em retries por timeout)
  if (update?.update_id) {
    if (processedUpdates.has(update.update_id)) {
      console.log(`Update ${update.update_id} já processado. Ignorando retry do Telegram.`)
      return new Response("OK (already processed)", { status: 200 })
    }
    processedUpdates.add(update.update_id)
    if (processedUpdates.size > 2000) {
      const oldest = Array.from(processedUpdates).slice(0, 500)
      for (const id of oldest) processedUpdates.delete(id)
    }
  }

  // 0. Disparo de Cron Proativo (Resumo Matinal 08:00 e Noturno 21:00)
  if (update.cron_event) {
    await handleCronBriefing(update.cron_event)
    return new Response(JSON.stringify({ ok: true, event: update.cron_event }), {
      headers: { "Content-Type": "application/json" }
    })
  }

  // 1. Processar Callback Queries (Botões Interativos)
  if (update.callback_query) {
    const cq = update.callback_query
    const fromId = String(cq.from?.id || "")
    const chatId = cq.message?.chat?.id
    const messageId = cq.message?.message_id
    const data = cq.data || ""

    // Verificação de Segurança
    if (TELEGRAM_ALLOWED_USER_IDS.length > 0 && !TELEGRAM_ALLOWED_USER_IDS.includes(fromId)) {
      await answerCallbackQuery(cq.id, "Acesso não autorizado.")
      return new Response("Forbidden", { status: 200 })
    }

    // Desfazer Transação
    if (data.startsWith("undo_tx:")) {
      const txId = data.replace("undo_tx:", "")
      await cancelFinanceTransaction(txId)
      await answerCallbackQuery(cq.id, "Lançamento cancelado!")
      if (chatId && messageId) {
        await editTelegramMessage(chatId, messageId, "❌ <i>Lançamento financeiro cancelado com sucesso.</i>")
      }
      return new Response("OK", { status: 200 })
    }

    // Confirmar Desejo Comprado
    if (data.startsWith("buy_wish:")) {
      const wishId = data.replace("buy_wish:", "")
      const item = await markWishCompleted(wishId)
      await answerCallbackQuery(cq.id, "Desejo marcado como realizado!")
      if (chatId && messageId) {
        const title = item ? item.title : "Item"
        await editTelegramMessage(chatId, messageId, `🎉 <b>Parabéns pela conquista!</b>\nItem <b>${title}</b> marcado como comprado na sua lista de desejos!`)
      }
      return new Response("OK", { status: 200 })
    }

    // Confirmar Desejo Comprado E Lançar nas Finanças
    if (data.startsWith("buy_wish_tx:")) {
      const wishId = data.replace("buy_wish_tx:", "")
      const item = await markWishCompleted(wishId)
      let financeMsg = ""
      if (item && item.targetValue > 0) {
        await addFinanceTransaction({
          title: item.title,
          amount: item.targetValue,
          type: "expense",
          category: item.category || "Lazer",
          accountOrCardName: "Cartão"
        })
        financeMsg = ` e lançamento de <b>R$ ${item.targetValue.toFixed(2)}</b> adicionado nas suas despesas do Tessera!`
      }
      await answerCallbackQuery(cq.id, "Conquista marcada e despesa lançada!")
      if (chatId && messageId) {
        const title = item ? item.title : "Item"
        await editTelegramMessage(chatId, messageId, `🎉 <b>Conquista confirmada!</b>\nItem <b>${title}</b> marcado como comprado${financeMsg}`)
      }
      return new Response("OK", { status: 200 })
    }

    // Confirmar Lançamento de Comprovante / OCR
    if (data.startsWith("confirm_ocr:")) {
      try {
        const rawJson = atob(data.replace("confirm_ocr:", ""))
        const ocrData = JSON.parse(rawJson)
        await addFinanceTransaction({
          title: ocrData.title || "Comprovante",
          amount: Number(ocrData.amount || 0),
          type: "expense",
          category: ocrData.category || "Geral",
          date: ocrData.date || new Date().toISOString().split("T")[0],
          accountOrCardName: "Cartão"
        })
        await answerCallbackQuery(cq.id, "Lançamento confirmado!")
        if (chatId && messageId) {
          const formatted = `R$ ${Number(ocrData.amount || 0).toFixed(2).replace(".", ",")}`
          await editTelegramMessage(chatId, messageId, `✅ <b>Despesa Confirmada e Lançada!</b>\n\n🏪 <b>${ocrData.title}</b>: <code>${formatted}</code>\n📁 <b>Categoria:</b> ${ocrData.category}\n\n<i>Sincronizado em tempo real com o Tessera!</i>`)
        }
      } catch (err) {
        console.error("Erro ao confirmar OCR:", err)
        await answerCallbackQuery(cq.id, "Erro ao confirmar.")
      }
      return new Response("OK", { status: 200 })
    }

    // Descartar Comprovante / OCR
    if (data === "discard_ocr") {
      await answerCallbackQuery(cq.id, "Comprovante descartado.")
      if (chatId && messageId) {
        await editTelegramMessage(chatId, messageId, "🗑️ <i>Comprovante descartado. Nenhuma despesa foi lançada.</i>")
      }
      return new Response("OK", { status: 200 })
    }

    // Marcar Item de Mercado Comprado
    if (data.startsWith("buy_market:")) {
      const itemName = decodeURIComponent(data.replace("buy_market:", ""))
      await markMarketItemBought(itemName)
      await answerCallbackQuery(cq.id, `"${itemName}" marcado como comprado!`)
      if (chatId && messageId) {
        await editTelegramMessage(chatId, messageId, `✅ <b>Item Comprado!</b>\nO item <b>${itemName}</b> foi marcado na sua lista de compras!`)
      }
      return new Response("OK", { status: 200 })
    }

    // Cancelar ação de Desejo
    if (data === "cancel_wish") {
      await answerCallbackQuery(cq.id, "Operação cancelada.")
      if (chatId && messageId) {
        await editTelegramMessage(chatId, messageId, "👌 <i>Operação cancelada sem alterações.</i>")
      }
      return new Response("OK", { status: 200 })
    }

    // Confirmar Importação de Extrato Bancário PDF (Itaú, Nubank, etc.)
    if (data.startsWith("confirm_pdf:")) {
      const pendingId = data.replace("confirm_pdf:", "")
      try {
        const docs = await supabaseRest(`telegram_bot_logs?id=eq.${pendingId}&select=*`)
        if (Array.isArray(docs) && docs.length > 0) {
          const logData = docs[0]
          const txs = logData.payload?.transactions || []
          const bank = logData.payload?.bankName || "Extrato"

          for (const t of txs) {
            await addFinanceTransaction({
              title: t.title,
              amount: Number(t.amount || 0),
              type: t.type === "income" ? "income" : "expense",
              category: t.category || "Geral",
              date: t.date,
              accountOrCardName: bank
            })
          }

          // Atualiza status no log
          await supabaseRest(`telegram_bot_logs?id=eq.${pendingId}`, {
            method: "PATCH",
            body: JSON.stringify({ action: "completed_pdf_import" })
          })

          await answerCallbackQuery(cq.id, "Extrato importado com sucesso!")
          if (chatId && messageId) {
            await editTelegramMessage(chatId, messageId, `✅ <b>Extrato Bancário (${bank}) Importado!</b>\n\nForam adicionados <b>${txs.length}</b> lançamentos ao seu painel financeiro do Tessera. Sincronizado em tempo real com o app e a web!`)
          }
        } else {
          await answerCallbackQuery(cq.id, "Registro expirado.")
        }
      } catch (err) {
        console.error("Erro ao importar PDF:", err)
        await answerCallbackQuery(cq.id, "Erro ao importar extrato.")
      }
      return new Response("OK", { status: 200 })
    }

    // Descartar Importação de PDF
    if (data === "discard_pdf") {
      await answerCallbackQuery(cq.id, "Extrato descartado.")
      if (chatId && messageId) {
        await editTelegramMessage(chatId, messageId, "🗑️ <i>Importação de extrato bancário descartada. Nenhum lançamento foi registrado.</i>")
      }
      return new Response("OK", { status: 200 })
    }

    // Callbacks Interativos de Futebol
    if (data === "soccer_table") {
      await answerCallbackQuery(cq.id, "Carregando tabela...")
      const soccer = await fetchSoccerInfo({ type: "standings" })
      if (chatId && messageId) {
        await editTelegramMessage(chatId, messageId, soccer.text, soccer.replyMarkup)
      }
      return new Response("OK", { status: 200 })
    }

    if (data.startsWith("soccer_team:")) {
      const teamKey = data.replace("soccer_team:", "")
      await answerCallbackQuery(cq.id, `Buscando próximo jogo...`)
      const soccer = await fetchSoccerInfo({ team: teamKey, type: "next" })
      if (chatId && messageId) {
        await editTelegramMessage(chatId, messageId, soccer.text, soccer.replyMarkup)
      }
      return new Response("OK", { status: 200 })
    }

    if (data.startsWith("soccer_last:")) {
      const teamId = data.replace("soccer_last:", "")
      await answerCallbackQuery(cq.id, `Buscando último resultado...`)
      const soccer = await fetchSoccerInfo({ type: "last" }, teamId)
      if (chatId && messageId) {
        await editTelegramMessage(chatId, messageId, soccer.text, soccer.replyMarkup)
      }
      return new Response("OK", { status: 200 })
    }

    // Callback de Briefing Matinal
    if (data === "cmd_briefing") {
      await answerCallbackQuery(cq.id, "Gerando briefing matinal...")
      const briefing = await generateMorningBriefing(userFirstName)
      if (chatId) {
        await sendTelegramMessage(chatId, briefing.text, briefing.replyMarkup)
        if (ENABLE_VOICE_RESPONSES && briefing.spokenText) {
          try {
            const audioBuf = await synthesizeSpeechFrancisca(briefing.spokenText)
            if (audioBuf) {
              await sendTelegramVoice(chatId, audioBuf)
            }
          } catch (vErr) {
            console.error("Erro ao sintetizar áudio no callback cmd_briefing:", vErr)
          }
        }
      }
      return new Response("OK", { status: 200 })
    }

    // Callbacks do Módulo de Obra e Apartamento
    if (data === "menu_apartment") {
      await answerCallbackQuery(cq.id, "Carregando status da obra...")
      const doc = await getApartmentDoc()
      const card = formatApartmentCard(doc)
      if (chatId && messageId) {
        await editTelegramMessage(chatId, messageId, card.text, card.replyMarkup)
      }
      return new Response("OK", { status: 200 })
    }

    // Callbacks do Módulo de Saúde e Hábitos
    if (data === "menu_health") {
      await answerCallbackQuery(cq.id, "Carregando dados de saúde...")
      const doc = await getHealthDoc()
      const card = formatHealthCard(doc)
      if (chatId && messageId) {
        await editTelegramMessage(chatId, messageId, card.text, card.replyMarkup)
      }
      return new Response("OK", { status: 200 })
    }

    if (data.startsWith("health_water:")) {
      const addMl = parseInt(data.replace("health_water:", ""), 10) || 250
      const doc = await getHealthDoc()
      doc.today_water_ml = (doc.today_water_ml || 0) + addMl
      await saveHealthDoc(doc)
      await answerCallbackQuery(cq.id, `+${addMl}ml registrados! 💧`)
      const card = formatHealthCard(doc)
      if (chatId && messageId) {
        await editTelegramMessage(chatId, messageId, card.text, card.replyMarkup)
      }
      return new Response("OK", { status: 200 })
    }

    if (data === "health_log_weight") {
      await answerCallbackQuery(cq.id, "Envie seu peso por áudio ou texto (ex: 'Pesei 74.5kg')")
      return new Response("OK", { status: 200 })
    }

    // Callback de Saldo Rápido
    if (data === "menu_saldo") {
      await answerCallbackQuery(cq.id, "Consultando finanças...")
      const finDoc = await getFinanceDoc()
      const d = finDoc?.data || {}
      const spendable = Number(d.spendable_balance || 0).toFixed(2).replace(".", ",")
      const total = Number(d.total_balance || 0).toFixed(2).replace(".", ",")
      const committed = Number(d.committed_percentage || 0).toFixed(0)

      let cardsText = ""
      if (Array.isArray(d.cards) && d.cards.length > 0) {
        cardsText = "\n💳 <b>Cartões de Crédito:</b>\n"
        for (const card of d.cards) {
          const limit = Number(card.limit || 0)
          const used = Number(card.usedLimit || card.used_limit || 0)
          const available = Math.max(0, limit - used)
          cardsText += `• <b>${card.name}:</b> Fatura R$ ${used.toFixed(2).replace(".", ",")} | Disp. R$ ${available.toFixed(2).replace(".", ",")}\n`
        }
      }

      const text = `📊 <b>Resumo Financeiro • Tessera</b>\n\n` +
        `💰 <b>Saldo Livre:</b> R$ ${spendable}\n` +
        `💵 <b>Saldo Total:</b> R$ ${total}\n` +
        `📌 <b>Renda Comprometida:</b> ${committed}%\n` +
        cardsText

      const replyMarkup = {
        inline_keyboard: [
          [
            { text: "📊 Ver Detalhes no Mini App", web_app: { url: "https://tessera-35c54.web.app/finance" } }
          ],
          [
            { text: "🏗️ Ver Obra", callback_data: "menu_apartment" },
            { text: "🩺 Ver Saúde", callback_data: "menu_health" }
          ]
        ]
      }

      if (chatId && messageId) {
        await editTelegramMessage(chatId, messageId, text, replyMarkup)
      }
      return new Response("OK", { status: 200 })
    }

    await answerCallbackQuery(cq.id)
    return new Response("OK", { status: 200 })
  }

  // 2. Processar Mensagens (Texto, Áudio ou Foto)
  const message = update.message
  if (!message) {
    return new Response("No message in update", { status: 200 })
  }

  const chatId = message.chat.id
  const fromId = String(message.from?.id || "")
  const userFirstName = message.from?.first_name || "Kenned"

  // Verificação de Segurança Estrita
  if (TELEGRAM_ALLOWED_USER_IDS.length > 0 && !TELEGRAM_ALLOWED_USER_IDS.includes(fromId)) {
    await sendTelegramMessage(
      chatId,
      "🔒 <b>Acesso Restrito:</b> Este bot é o assistente pessoal privado do ecossistema Tessera. Seu Telegram ID não está autorizado."
    )
    return new Response("Forbidden", { status: 200 })
  }

  // Processamento de Foto / Imagem de Comprovante ou Cupom (OCR Vision)
  if (message.photo && Array.isArray(message.photo) && message.photo.length > 0) {
    await sendChatAction(chatId, "typing")
    const highestPhoto = message.photo[message.photo.length - 1]
    const photoFile = await downloadTelegramFile(highestPhoto.file_id)
    if (!photoFile) {
      await sendTelegramMessage(chatId, "❌ Não consegui baixar a imagem. Poderia enviar novamente?")
      return new Response("Photo download failed", { status: 200 })
    }

    const base64Img = arrayBufferToBase64(photoFile.buffer)
    const dataUrl = `data:${photoFile.mimeType};base64,${base64Img}`

    await sendTelegramMessage(chatId, "🔍 <i>Analisando comprovante fiscal com IA... Aguarde um instante.</i>")
    try {
      const ocrResult = await processReceiptPhotoWithGroq(dataUrl)
      const formattedAmount = `R$ ${Number(ocrResult.amount || 0).toFixed(2).replace(".", ",")}`
      const est = ocrResult.establishment || "Despesa por Comprovante"
      const cat = ocrResult.category || "Mercado"
      const dateStr = ocrResult.date || new Date().toISOString().split("T")[0]

      const replyText = `🧾 <b>Comprovante / Cupom Fiscal Identificado!</b>\n\n` +
        `🏪 <b>Estabelecimento:</b> ${est}\n` +
        `💵 <b>Valor Total:</b> <code>${formattedAmount}</code>\n` +
        `📁 <b>Categoria Sugerida:</b> ${cat}\n` +
        (ocrResult.description ? `📝 <i>${ocrResult.description}</i>\n` : "") +
        `📅 <b>Data:</b> ${dateStr}\n\n` +
        `<i>Deseja confirmar o lançamento desta despesa no Tessera?</i>`

      const payloadStr = btoa(JSON.stringify({
        title: est,
        amount: ocrResult.amount,
        category: cat,
        date: dateStr
      }))

      const replyMarkup = {
        inline_keyboard: [
          [
            { text: "✅ Confirmar Lançamento", callback_data: `confirm_ocr:${payloadStr}` }
          ],
          [
            { text: "🗑️ Descartar", callback_data: "discard_ocr" }
          ]
        ]
      }

      await sendTelegramMessage(chatId, replyText, replyMarkup)
      return new Response("OK", { status: 200 })
    } catch (err: any) {
      console.error("Erro ao analisar comprovante:", err)
      await sendTelegramMessage(chatId, `⚠️ Não consegui extrair os dados da foto com clareza: ${err.message}`)
      return new Response("OCR failed", { status: 200 })
    }
  }

  // Processamento de Documento PDF (Extratos Bancários Itaú, Nubank, etc.)
  if (message.document && (message.document.mime_type === "application/pdf" || message.document.file_name?.toLowerCase().endsWith(".pdf"))) {
    await sendChatAction(chatId, "typing")
    await sendTelegramMessage(chatId, "📄 <i>Extrato bancário em PDF recebido! Analisando documento com IA especializada em conciliação bancária... Aguarde um instante.</i>")

    const pdfFile = await downloadTelegramFile(message.document.file_id)
    if (!pdfFile) {
      await sendTelegramMessage(chatId, "❌ Não consegui baixar o arquivo PDF. Poderia enviar novamente?")
      return new Response("PDF download failed", { status: 200 })
    }

    try {
      const pdfBase64 = arrayBufferToBase64(pdfFile.buffer)
      const parsedBank = await processBankStatementPdfWithGemini(pdfBase64)
      const txs = parsedBank.transactions || []

      if (txs.length === 0) {
        await sendTelegramMessage(chatId, `⚠️ Analisei o extrato do <b>${parsedBank.bankName || "banco"}</b>, mas não encontrei movimentações financeiras legíveis. Verifique se o arquivo não possui senha ou se é um comprovante único.`)
        return new Response("OK", { status: 200 })
      }

      // Salva no log para recuperar com segurança na confirmação (sem risco de estourar 64 bytes do Telegram)
      const logRecord = await supabaseRest("telegram_bot_logs", {
        method: "POST",
        headers: { "Prefer": "return=representation" },
        body: JSON.stringify({
          telegram_user_id: fromId,
          action: "pending_pdf_import",
          payload: {
            bankName: parsedBank.bankName,
            period: parsedBank.period,
            transactions: txs
          }
        })
      })

      const pendingId = Array.isArray(logRecord) && logRecord[0]?.id ? logRecord[0].id : ""

      const totalInStr = Number(parsedBank.totalIncome || 0).toFixed(2).replace(".", ",")
      const totalOutStr = Number(parsedBank.totalExpense || 0).toFixed(2).replace(".", ",")

      let previewList = ""
      const previewTxs = txs.slice(0, 5)
      previewTxs.forEach((t) => {
        const sign = t.type === "income" ? "+" : "-"
        const val = Number(t.amount || 0).toFixed(2).replace(".", ",")
        const emoji = t.type === "income" ? "🟢" : "🔴"
        previewList += `${emoji} <b>${t.date}</b>: ${t.title} — <code>${sign}R$ ${val}</code> (${t.category})\n`
      })
      if (txs.length > 5) {
        previewList += `<i>... e mais ${txs.length - 5} transações identificadas.</i>\n`
      }

      const replyText = `📑 <b>Extrato Bancário Analisado (${parsedBank.bankName || "Banco"})!</b>\n\n` +
        (parsedBank.period ? `📅 <b>Período:</b> ${parsedBank.period}\n` : "") +
        `🟢 <b>Entradas Totais:</b> R$ ${totalInStr}\n` +
        `🔴 <b>Despesas Totais:</b> R$ ${totalOutStr}\n` +
        `📊 <b>Total de Transações:</b> ${txs.length}\n\n` +
        `<b>Prévia dos Lançamentos:</b>\n` +
        previewList +
        `\n<i>Deseja importar todos os <b>${txs.length}</b> lançamentos para as finanças do Tessera?</i>`

      const replyMarkup = {
        inline_keyboard: [
          [
            { text: `✅ Importar ${txs.length} Transações`, callback_data: `confirm_pdf:${pendingId}` }
          ],
          [
            { text: "🗑️ Descartar", callback_data: "discard_pdf" }
          ]
        ]
      }

      await sendTelegramMessage(chatId, replyText, replyMarkup)
      return new Response("OK", { status: 200 })
    } catch (err: any) {
      console.error("Erro ao analisar extrato bancário:", err)
      await sendTelegramMessage(chatId, `⚠️ Erro ao processar o extrato PDF: ${err.message}`)
      return new Response("PDF processing failed", { status: 200 })
    }
  }

  // Roteador de Comandos Instantâneos (< 50ms, sem custo de LLM)
  const rawText = message.text || ""
  if (rawText.startsWith("/")) {
    const cmd = rawText.split(" ")[0].toLowerCase().trim()

    if (cmd === "/start" || cmd === "/ajuda" || cmd === "/help") {
      await registerTelegramBotCommands()
      const welcomeText = `🤖 <b>Assistente Oficial Tessera</b>\n\n` +
        `Olá, ${userFirstName}! Aqui você tem controle total do seu aplicativo Tessera por voz, texto ou fotos:\n\n` +
        `<b>Comandos Rápidos no Teclado:</b>\n` +
        `• /briefing — Resumo matinal completo (áudio + texto)\n` +
        `• /obra — Status e evolução da obra do apartamento\n` +
        `• /saude — Registro de hidratação, peso e hábitos\n` +
        `• /saldo — Saldo livre, limites de cartão e faturas\n` +
        `• /futebol — Próximos jogos, tabela e placares\n` +
        `• /tabela — Classificação oficial da Série A\n` +
        `• /grafico — Gráfico visual de gastos por categoria\n` +
        `• /extrato — Baixar extrato do mês em planilha CSV\n` +
        `• /mercado — Ver lista de compras pendente\n` +
        `• /lembretes — Ver tarefas e avisos pendentes\n` +
        `• /desejos — Metas e lista de compras planejadas\n` +
        `• /tempo — Previsão do tempo e chuva\n` +
        `• /app — Abrir a central do Tessera em Mini App\n` +
        `• /ajuda — Este guia de atalhos\n\n` +
        `<b>Superpoderes Ativos:</b>\n` +
        `📱 <b>Mini App Integrado:</b> Abra o Tessera direto no Telegram pelo botão no rodapé ou /app!\n` +
        `🎙️ <b>Voz Ultra-Natural:</b> Fale por áudio e receba respostas em voz neural humana!\n` +
        `📑 <b>Leitor de Extrato PDF:</b> Arraste o PDF do Itaú ou qualquer banco para conciliação automática!\n` +
        `🛒 <b>Mercado:</b> Diga <i>"Adiciona 2 caixas de leite e café no mercado"</i>\n` +
        `📸 <b>Foto:</b> Envie foto de cupom fiscal ou comprovante PIX\n` +
        `🛍️ <b>Desejos:</b> Diga <i>"Comprei o fone bluetooth"</i> para dar baixa e lançar!`
      const welcomeMarkup = {
        inline_keyboard: [
          [
            { text: "📱 Abrir Tessera Hub", web_app: { url: "https://tessera-35c54.web.app" } }
          ],
          [
            { text: "🌅 Briefing", callback_data: "cmd_briefing" },
            { text: "🏗️ Obra", callback_data: "menu_apartment" },
            { text: "🩺 Saúde", callback_data: "menu_health" }
          ],
          [
            { text: "🛒 Mercado", web_app: { url: "https://tessera-35c54.web.app/market" } },
            { text: "📊 Finanças", web_app: { url: "https://tessera-35c54.web.app/finance" } }
          ]
        ]
      }
      await sendTelegramMessage(chatId, welcomeText, welcomeMarkup)
      return new Response("OK", { status: 200 })
    }

    if (cmd === "/briefing" || cmd === "/bomdia") {
      await sendChatAction(chatId, "typing")
      const briefing = await generateMorningBriefing(userFirstName)
      await sendTelegramMessage(chatId, briefing.text, briefing.replyMarkup)
      if (ENABLE_VOICE_RESPONSES && briefing.spokenText) {
        try {
          const audioBuf = await synthesizeSpeechFrancisca(briefing.spokenText)
          if (audioBuf) {
            await sendTelegramVoice(chatId, audioBuf)
          }
        } catch (vErr) {
          console.error("Erro ao enviar áudio do briefing:", vErr)
        }
      }
      return new Response("OK", { status: 200 })
    }

    if (cmd === "/obra" || cmd === "/ape" || cmd === "/apartamento" || cmd === "/reforma") {
      const doc = await getApartmentDoc()
      const card = formatApartmentCard(doc)
      await sendTelegramMessage(chatId, card.text, card.replyMarkup)
      return new Response("OK", { status: 200 })
    }

    if (cmd === "/saude" || cmd === "/agua" || cmd === "/peso") {
      const doc = await getHealthDoc()
      const card = formatHealthCard(doc)
      await sendTelegramMessage(chatId, card.text, card.replyMarkup)
      return new Response("OK", { status: 200 })
    }

    if (cmd === "/app" || cmd === "/menu") {
      const appCard = `📱 <b>Tessera Mini App • Central Conectada</b>\n\n` +
        `Abra qualquer módulo em tela cheia com interface interativa, sincronização em tempo real e vibração tátil direto no Telegram:`
      const appMarkup = {
        inline_keyboard: [
          [
            { text: "📱 Abrir Tessera Hub Completo", web_app: { url: "https://tessera-35c54.web.app" } }
          ],
          [
            { text: "🛒 Lista de Supermercado", web_app: { url: "https://tessera-35c54.web.app/market" } },
            { text: "📊 Dashboard Financeiro", web_app: { url: "https://tessera-35c54.web.app/finance" } }
          ],
          [
            { text: "⏰ Tarefas & Avisos", web_app: { url: "https://tessera-35c54.web.app/tasks" } },
            { text: "🎁 Mural de Desejos", web_app: { url: "https://tessera-35c54.web.app/wishes" } }
          ]
        ]
      }
      await sendTelegramMessage(chatId, appCard, appMarkup)
      return new Response("OK", { status: 200 })
    }

    if (cmd === "/saldo") {
      const finDoc = await getFinanceDoc()
      if (!finDoc) {
        await sendTelegramMessage(chatId, "⚠️ Não encontrei dados financeiros sincronizados no momento.")
        return new Response("OK", { status: 200 })
      }
      const d = finDoc.data
      const spendable = Number(d.spendable_balance || 0).toFixed(2).replace(".", ",")
      const total = Number(d.total_balance || 0).toFixed(2).replace(".", ",")
      const committed = Number(d.committed_percentage || 0).toFixed(0)

      let cardsText = ""
      if (Array.isArray(d.cards) && d.cards.length > 0) {
        cardsText = "\n💳 <b>Cartões de Crédito:</b>\n"
        for (const card of d.cards) {
          const limit = Number(card.limit || 0)
          const used = Number(card.usedLimit || card.used_limit || 0)
          const available = Math.max(0, limit - used)
          cardsText += `• <b>${card.name}:</b> Fatura R$ ${used.toFixed(2).replace(".", ",")} | Disp. R$ ${available.toFixed(2).replace(".", ",")}\n`
        }
      }

      let accountsText = ""
      if (Array.isArray(d.accounts) && d.accounts.length > 0) {
        accountsText = "\n🏦 <b>Contas Bancárias:</b>\n"
        for (const acc of d.accounts) {
          const bal = Number(acc.balance || 0).toFixed(2).replace(".", ",")
          accountsText += `• <b>${acc.name}:</b> R$ ${bal}\n`
        }
      }

      const reply = `📊 <b>Resumo Financeiro • Tessera</b>\n\n` +
        `💰 <b>Saldo Livre:</b> R$ ${spendable}\n` +
        `💵 <b>Saldo Total:</b> R$ ${total}\n` +
        `📌 <b>Renda Comprometida:</b> ${committed}%\n` +
        cardsText + accountsText

      const replyMarkup = {
        inline_keyboard: [
          [
            { text: "📊 Ver Detalhes e Faturas no Mini App", web_app: { url: "https://tessera-35c54.web.app/finance" } }
          ]
        ]
      }

      await sendTelegramMessage(chatId, reply, replyMarkup)
      return new Response("OK", { status: 200 })
    }

    if (cmd === "/mercado") {
      const marketDoc = await getMarketDoc()
      const items = marketDoc && Array.isArray(marketDoc.data.items) ? marketDoc.data.items : []
      const pending = items.filter((it: any) => !it.isChecked && !it.isBought)

      if (pending.length === 0) {
        const emptyMarkup = {
          inline_keyboard: [
            [
              { text: "🛒 Abrir Lista no Mini App", web_app: { url: "https://tessera-35c54.web.app/market" } }
            ]
          ]
        }
        await sendTelegramMessage(chatId, "🎉 Sua lista de compras está vazia! Não há nenhum item pendente.", emptyMarkup)
        return new Response("OK", { status: 200 })
      }

      let listText = `🛒 <b>Lista de Compras (${pending.length} pendentes):</b>\n\n`
      pending.forEach((it: any, idx: number) => {
        const qty = it.quantity ? `${it.quantity} ${it.unit || "un"} ` : ""
        listText += `${idx + 1}. <b>${qty}${it.name}</b> (${it.category || "Geral"})\n`
      })
      listText += `\n<i>Diga "comprei X" ou abra o Mini App para ticar com feedback tátil!</i>`

      const replyMarkup = {
        inline_keyboard: [
          [
            { text: "🛒 Abrir & Marcar Itens no Mini App", web_app: { url: "https://tessera-35c54.web.app/market" } }
          ]
        ]
      }

      await sendTelegramMessage(chatId, listText, replyMarkup)
      return new Response("OK", { status: 200 })
    }

    if (cmd === "/lembretes") {
      const taskDoc = await getTasksDoc()
      const items = taskDoc && Array.isArray(taskDoc.data.items) ? taskDoc.data.items : []
      const pending = items.filter((it: any) => it.status === "pending")

      if (pending.length === 0) {
        const emptyMarkup = {
          inline_keyboard: [
            [
              { text: "⏰ Ver Painel de Tarefas", web_app: { url: "https://tessera-35c54.web.app/tasks" } }
            ]
          ]
        }
        await sendTelegramMessage(chatId, "🎉 Você não tem nenhum lembrete ou tarefa pendente!", emptyMarkup)
        return new Response("OK", { status: 200 })
      }

      let listText = `📋 <b>Tarefas e Lembretes Pendentes (${pending.length}):</b>\n\n`
      pending.forEach((it: any, idx: number) => {
        listText += `${idx + 1}. <b>${it.title}</b>${it.due_time ? ` (às ${it.due_time})` : ""}\n`
      })

      const replyMarkup = {
        inline_keyboard: [
          [
            { text: "⏰ Gerenciar Tarefas no Mini App", web_app: { url: "https://tessera-35c54.web.app/tasks" } }
          ]
        ]
      }

      await sendTelegramMessage(chatId, listText, replyMarkup)
      return new Response("OK", { status: 200 })
    }

    if (cmd === "/desejos") {
      const wishDoc = await getWishesDoc()
      const items = wishDoc && Array.isArray(wishDoc.data.items) ? wishDoc.data.items : []
      const active = items.filter((it: any) => !it.isBought)

      if (active.length === 0) {
        const emptyMarkup = {
          inline_keyboard: [
            [
              { text: "🎁 Ver Mural de Desejos", web_app: { url: "https://tessera-35c54.web.app/wishes" } }
            ]
          ]
        }
        await sendTelegramMessage(chatId, "✨ Sua lista de desejos está em dia! Nenhuma meta pendente.", emptyMarkup)
        return new Response("OK", { status: 200 })
      }

      let listText = `🎁 <b>Lista de Desejos e Metas (${active.length}):</b>\n\n`
      active.forEach((it: any, idx: number) => {
        const val = it.targetValue ? ` — R$ ${Number(it.targetValue).toFixed(2).replace(".", ",")}` : ""
        listText += `${idx + 1}. <b>${it.title}</b>${val}\n`
      })

      const replyMarkup = {
        inline_keyboard: [
          [
            { text: "🎁 Ver Mural de Desejos no Mini App", web_app: { url: "https://tessera-35c54.web.app/wishes" } }
          ]
        ]
      }

      await sendTelegramMessage(chatId, listText, replyMarkup)
      return new Response("OK", { status: 200 })
    }

    if (cmd === "/tempo") {
      const forecast = await fetchWeatherForecast("São Paulo")
      await sendTelegramMessage(chatId, forecast)
      return new Response("OK", { status: 200 })
    }

    if (cmd === "/futebol" || cmd === "/jogos") {
      const soccer = await fetchSoccerInfo({ type: "general" })
      await sendTelegramMessage(chatId, soccer.text, soccer.replyMarkup)
      return new Response("OK", { status: 200 })
    }

    if (cmd === "/tabela") {
      const soccer = await fetchSoccerInfo({ type: "standings" })
      await sendTelegramMessage(chatId, soccer.text, soccer.replyMarkup)
      return new Response("OK", { status: 200 })
    }

    if (cmd === "/grafico") {
      const finDoc = await getFinanceDoc()
      if (!finDoc) {
        await sendTelegramMessage(chatId, "⚠️ Não encontrei dados financeiros sincronizados no momento.")
        return new Response("OK", { status: 200 })
      }
      const d = finDoc.data
      const categories = Array.isArray(d.categories) ? d.categories : []
      const month = d.month_label || "Mês Atual"
      const chartUrl = generateFinanceChartUrl(categories, month)

      let summary = `📊 <b>Distribuição de Gastos • ${month}</b>\n\n`
      const top = categories
        .filter((c: any) => Number(c.value) > 0)
        .sort((a: any, b: any) => Number(b.value) - Number(a.value))
        .slice(0, 5)

      if (top.length > 0) {
        top.forEach((c: any, i: number) => {
          summary += `${i + 1}. <b>${c.name}:</b> R$ ${Number(c.value).toFixed(2).replace(".", ",")}\n`
        })
      } else {
        summary += "<i>Nenhum gasto categorizado no mês ainda.</i>\n"
      }
      summary += `\n⚡ <i>Gerado em tempo real com base no seu Tessera!</i>`

      await sendTelegramPhoto(chatId, chartUrl, summary)
      return new Response("OK", { status: 200 })
    }

    if (cmd === "/extrato") {
      const finDoc = await getFinanceDoc()
      if (!finDoc) {
        await sendTelegramMessage(chatId, "⚠️ Não encontrei dados financeiros sincronizados no momento.")
        return new Response("OK", { status: 200 })
      }
      const d = finDoc.data
      const transactions = Array.isArray(d.suggestions) ? d.suggestions : []

      if (transactions.length === 0) {
        await sendTelegramMessage(chatId, "📄 Seu extrato financeiro não possui lançamentos registrados ainda.")
        return new Response("OK", { status: 200 })
      }

      let csv = "Data;Descrição;Categoria;Tipo;Valor (R$);Conta/Cartão;Origem\n"
      for (const t of transactions) {
        const val = Number(t.amount || 0).toFixed(2).replace(".", ",")
        const tipo = t.type === "income" ? "Receita" : "Despesa"
        const cat = t.category || "Geral"
        const conta = t.accountOrCardName || "Padrão"
        const origem = t.origin || "App"
        csv += `"${t.date || ""}";"${(t.title || "").replace(/"/g, '""')}";"${cat}";"${tipo}";"${val}";"${conta}";"${origem}"\n`
      }

      const month = (d.month_label || "extrato").toLowerCase().replace(/\s+/g, "_")
      const fileName = `tessera_extrato_${month}.csv`
      const caption = `📄 <b>Extrato Financeiro • Tessera</b>\n\n` +
        `Total de lançamentos: <b>${transactions.length}</b>\n` +
        `<i>Arquivo CSV compatível com Excel, Numbers e Google Planilhas!</i>`

      await sendTelegramDocument(chatId, csv, fileName, caption)
      return new Response("OK", { status: 200 })
    }
  }

  // Indicador de digitação imediato para mensagens em linguagem natural
  await sendChatAction(chatId, message.voice ? "record_voice" : "typing")

  try {
    let audioBuffer: ArrayBuffer | undefined
    let audioMimeType: string | undefined
    let textInput: string | undefined = message.text

    // Se for mensagem de voz
    if (message.voice) {
      const voiceFile = await downloadTelegramFile(message.voice.file_id)
      if (voiceFile) {
        audioBuffer = voiceFile.buffer
        audioMimeType = voiceFile.mimeType
      } else {
        await sendTelegramMessage(chatId, "❌ Não consegui baixar seu áudio. Poderia enviar novamente?")
        return new Response("Audio download failed", { status: 200 })
      }
    }

    if (!textInput && !audioBuffer) {
      return new Response("No text or audio found", { status: 200 })
    }

    // Processamento com Groq (Whisper + Llama 3.3 70B)
    const todayStr = new Date().toLocaleDateString("pt-BR", { timeZone: "America/Sao_Paulo" })
    const aiResult = await processWithGroq(
      { text: textInput, audioBuffer, audioMimeType },
      { todayDate: todayStr, userFirstName }
    )

    let transcriptionNote = ""
    if (aiResult.transcription) {
      transcriptionNote = `🎙️ <i>"${aiResult.transcription}"</i>\n\n`
    }

    // ------------------------------------------------------------------------
    // ROTEAMENTO DE AÇÕES
    // ------------------------------------------------------------------------

    // Ação: Adicionar Despesa / Receita
    if (aiResult.action === "add_transaction" && aiResult.transaction) {
      const tx = aiResult.transaction
      const result = await addFinanceTransaction({
        title: tx.title,
        amount: tx.amount,
        type: tx.type,
        category: tx.category,
        accountOrCardName: tx.account_or_card_name,
        date: tx.date
      })

      if (result) {
        const isExpense = tx.type === "expense"
        const emoji = isExpense ? "💸" : "💰"
        const typeLabel = isExpense ? "Despesa" : "Receita"
        const formattedAmount = `R$ ${result.amount.toFixed(2).replace(".", ",")}`
        const valStr = `${result.amount.toFixed(2).replace(".", ",")} reais`

        let alertSection = ""
        if (result.budgetAlert) {
          alertSection = `\n\n${result.budgetAlert}`
        }

        const responseText = `${transcriptionNote}✅ <b>${typeLabel} Lançada com Sucesso!</b>\n\n` +
          `${emoji} <b>${result.title}:</b> <code>${formattedAmount}</code>\n` +
          `📁 <b>Categoria:</b> ${result.category}\n` +
          `🏦 <b>Conta/Cartão:</b> ${result.account}` +
          alertSection +
          `\n\n⚡ <i>Sincronizado em tempo real com seu Tessera!</i>`

        const replyMarkup = {
          inline_keyboard: [
            [
              { text: "🗑️ Desfazer Lançamento", callback_data: `undo_tx:${result.id}` }
            ]
          ]
        }

        await sendTelegramMessage(chatId, responseText, replyMarkup)
        if (message.voice) {
          await maybeSendVoiceReply(chatId, `Lançamento de ${result.title} no valor de ${valStr} registrado com sucesso nas suas finanças.`, true)
        }
        return new Response("OK", { status: 200 })
      }
    }

    // Ação: Consultar Finanças (Saldo, Limites, Faturas)
    if (aiResult.action === "get_finances") {
      const finDoc = await getFinanceDoc()
      if (!finDoc) {
        await sendTelegramMessage(chatId, "⚠️ Não encontrei dados financeiros sincronizados no momento.")
        return new Response("OK", { status: 200 })
      }

      const d = finDoc.data
      const spendable = Number(d.spendable_balance || 0).toFixed(2).replace(".", ",")
      const total = Number(d.total_balance || 0).toFixed(2).replace(".", ",")
      const committed = Number(d.committed_percentage || 0).toFixed(0)

      let cardsText = ""
      if (Array.isArray(d.cards) && d.cards.length > 0) {
        cardsText = "\n💳 <b>Cartões de Crédito:</b>\n"
        for (const card of d.cards) {
          const limit = Number(card.limit || 0)
          const used = Number(card.usedLimit || card.used_limit || 0)
          const available = Math.max(0, limit - used)
          cardsText += `• <b>${card.name}:</b> Fatura R$ ${used.toFixed(2).replace(".", ",")} | Disp. R$ ${available.toFixed(2).replace(".", ",")}\n`
        }
      }

      let accountsText = ""
      if (Array.isArray(d.accounts) && d.accounts.length > 0) {
        accountsText = "\n🏦 <b>Contas Bancárias:</b>\n"
        for (const acc of d.accounts) {
          const bal = Number(acc.balance || 0).toFixed(2).replace(".", ",")
          accountsText += `• <b>${acc.name}:</b> R$ ${bal}\n`
        }
      }

      const reply = `${transcriptionNote}📊 <b>Resumo Financeiro • Tessera</b>\n\n` +
        `💰 <b>Saldo Livre:</b> R$ ${spendable}\n` +
        `💵 <b>Saldo Total:</b> R$ ${total}\n` +
        `📌 <b>Renda Comprometida:</b> ${committed}%\n` +
        cardsText +
        accountsText

      await sendTelegramMessage(chatId, reply)
      if (message.voice) {
        await maybeSendVoiceReply(chatId, `Seu saldo livre atual é de R$ ${spendable}. O saldo total é de R$ ${total}.`, true)
      }
      return new Response("OK", { status: 200 })
    }

    // Ação: Criar Lembrete / Tarefa
    if (aiResult.action === "add_task" && aiResult.task) {
      const t = aiResult.task
      const item = await addTaskReminder({
        title: t.title,
        description: t.description,
        due_time: t.due_time
      })

      const reply = `${transcriptionNote}⏰ <b>Lembrete Criado!</b>\n\n` +
        `📌 <b>${item.title}</b>\n` +
        (item.due_time ? `🕒 <b>Horário:</b> ${item.due_time}\n` : "") +
        (item.description ? `📝 <i>${item.description}</i>\n` : "") +
        `\n🔔 <i>Alerta prioritário sincronizado com o seu smartphone!</i>`

      await sendTelegramMessage(chatId, reply)
      if (message.voice) {
        await maybeSendVoiceReply(chatId, `Lembrete de ${item.title} agendado com sucesso!`, true)
      }
      return new Response("OK", { status: 200 })
    }

    // Ação: Consultar Lembretes Pendentes
    if (aiResult.action === "get_tasks") {
      const taskDoc = await getTasksDoc()
      const items = taskDoc && Array.isArray(taskDoc.data.items) ? taskDoc.data.items : []
      const pending = items.filter((it: any) => it.status === "pending")

      if (pending.length === 0) {
        await sendTelegramMessage(chatId, `${transcriptionNote}🎉 Você não tem nenhum lembrete ou tarefa pendente no momento!`)
        return new Response("OK", { status: 200 })
      }

      let listText = `${transcriptionNote}📋 <b>Suas Tarefas e Lembretes Pendentes:</b>\n\n`
      pending.slice(0, 10).forEach((it: any, idx: number) => {
        listText += `${idx + 1}. <b>${it.title}</b>${it.due_time ? ` (às ${it.due_time})` : ""}\n`
      })

      await sendTelegramMessage(chatId, listText)
      return new Response("OK", { status: 200 })
    }

    // Ação: Concluir Item de Desejo (Comprei X)
    if (aiResult.action === "complete_wish") {
      const query = (aiResult.query || aiResult.reply_text || "").toLowerCase()
      const wishDoc = await getWishesDoc()
      const items = wishDoc && Array.isArray(wishDoc.data.items) ? wishDoc.data.items : []
      const activeWishes = items.filter((it: any) => !it.isBought)

      // Busca semântica ou substring
      const matched = activeWishes.find((it: any) =>
        it.title.toLowerCase().includes(query) || query.includes(it.title.toLowerCase())
      ) || activeWishes[0]

      if (matched) {
        const valStr = matched.targetValue ? `R$ ${Number(matched.targetValue).toFixed(2).replace(".", ",")}` : ""
        const replyText = `${transcriptionNote}🎯 Encontrei este desejo na sua lista:\n\n` +
          `🛍️ <b>${matched.title}</b>${valStr ? ` (${valStr})` : ""}\n\n` +
          `Deseja marcar como comprado agora?`

        const replyMarkup = {
          inline_keyboard: [
            [
              { text: "✅ Marcar como Comprado", callback_data: `buy_wish:${matched.id}` }
            ],
            ...(matched.targetValue > 0 ? [[
              { text: `💳 Marcar + Lançar ${valStr}`, callback_data: `buy_wish_tx:${matched.id}` }
            ]] : []),
            [
              { text: "❌ Não era esse", callback_data: "cancel_wish" }
            ]
          ]
        }

        await sendTelegramMessage(chatId, replyText, replyMarkup)
        return new Response("OK", { status: 200 })
      } else {
        await sendTelegramMessage(chatId, `${transcriptionNote}Não encontrei nenhum desejo ativo correspondente na sua lista.`)
        return new Response("OK", { status: 200 })
      }
    }

    // Ação: Adicionar Novo Desejo
    if (aiResult.action === "add_wish" && aiResult.wish) {
      const w = aiResult.wish
      const created = await addWishItem({
        title: w.title,
        target_value: w.target_value,
        category: w.category
      })

      const valStr = created.targetValue > 0 ? ` (Meta: R$ ${created.targetValue.toFixed(2).replace(".", ",")})` : ""
      const reply = `${transcriptionNote}✨ <b>Item Adicionado à Lista de Desejos!</b>\n\n` +
        `🎁 <b>${created.title}</b>${valStr}\n` +
        `📁 <b>Categoria:</b> ${created.category}\n\n` +
        `<i>Visível agora no app Tessera e na Web!</i>`

      await sendTelegramMessage(chatId, reply)
      return new Response("OK", { status: 200 })
    }

    // Ação: Adicionar Itens à Lista de Compras (Mercado)
    if (aiResult.action === "add_market_items") {
      const itemsToAdd = aiResult.market_items || []
      if (itemsToAdd.length === 0 && aiResult.query) {
        itemsToAdd.push({ name: aiResult.query, quantity: 1, unit: "un", category: "Geral" })
      }

      const created = await addMarketItems(itemsToAdd)
      let itemsList = ""
      created.forEach(it => {
        const qty = it.quantity ? `${it.quantity} ${it.unit || "un"} ` : ""
        itemsList += `• <b>${qty}${it.name}</b> (${it.category})\n`
      })

      const reply = `${transcriptionNote}🛒 <b>Itens Adicionados à Lista de Compras!</b>\n\n` +
        itemsList +
        `\n⚡ <i>Sincronizado em tempo real com o app Tessera e a Web!</i>`

      await sendTelegramMessage(chatId, reply)
      if (message.voice) {
        await maybeSendVoiceReply(chatId, `Itens adicionados com sucesso à sua lista de compras.`, true)
      }
      return new Response("OK", { status: 200 })
    }

    // Ação: Consultar Lista de Compras (Mercado)
    if (aiResult.action === "get_market_items") {
      const marketDoc = await getMarketDoc()
      const items = marketDoc && Array.isArray(marketDoc.data.items) ? marketDoc.data.items : []
      const pending = items.filter((it: any) => !it.isChecked && !it.isBought)

      if (pending.length === 0) {
        await sendTelegramMessage(chatId, `${transcriptionNote}🛒 Sua lista de compras está vazia no momento!`)
        if (message.voice) {
          await maybeSendVoiceReply(chatId, `Sua lista de compras está vazia no momento.`, true)
        }
        return new Response("OK", { status: 200 })
      }

      let listText = `${transcriptionNote}🛒 <b>Lista de Compras (${pending.length} pendentes):</b>\n\n`
      pending.forEach((it: any, idx: number) => {
        const qty = it.quantity ? `${it.quantity} ${it.unit || "un"} ` : ""
        listText += `${idx + 1}. <b>${qty}${it.name}</b>\n`
      })
      listText += `\n<i>Diga "comprei X" ou marque no aplicativo!</i>`

      await sendTelegramMessage(chatId, listText)
      if (message.voice) {
        await maybeSendVoiceReply(chatId, `Você tem ${pending.length} itens pendentes na sua lista de compras.`, true)
      }
      return new Response("OK", { status: 200 })
    }

    // Ação: Gráfico Visual de Gastos
    if (aiResult.action === "get_chart") {
      const finDoc = await getFinanceDoc()
      const d = finDoc?.data || {}
      const categories = Array.isArray(d.categories) ? d.categories : []
      const month = d.month_label || "Mês Atual"
      const chartUrl = generateFinanceChartUrl(categories, month)
      const summary = `📊 <b>Distribuição de Gastos • ${month}</b>\n\n⚡ <i>Gerado em tempo real com base no seu Tessera!</i>`
      await sendTelegramPhoto(chatId, chartUrl, summary)
      if (message.voice) {
        await maybeSendVoiceReply(chatId, `Aqui está o gráfico das suas despesas em ${month}.`, true)
      }
      return new Response("OK", { status: 200 })
    }

    // Ação: Exportar Extrato CSV
    if (aiResult.action === "export_csv") {
      const finDoc = await getFinanceDoc()
      const d = finDoc?.data || {}
      const transactions = Array.isArray(d.suggestions) ? d.suggestions : []

      if (transactions.length === 0) {
        await sendTelegramMessage(chatId, "📄 Seu extrato financeiro não possui lançamentos registrados ainda.")
        return new Response("OK", { status: 200 })
      }

      let csv = "Data;Descrição;Categoria;Tipo;Valor (R$);Conta/Cartão;Origem\n"
      for (const t of transactions) {
        const val = Number(t.amount || 0).toFixed(2).replace(".", ",")
        const tipo = t.type === "income" ? "Receita" : "Despesa"
        const cat = t.category || "Geral"
        const conta = t.accountOrCardName || "Padrão"
        const origem = t.origin || "App"
        csv += `"${t.date || ""}";"${(t.title || "").replace(/"/g, '""')}";"${cat}";"${tipo}";"${val}";"${conta}";"${origem}"\n`
      }

      const month = (d.month_label || "extrato").toLowerCase().replace(/\s+/g, "_")
      const fileName = `tessera_extrato_${month}.csv`
      const caption = `📄 <b>Extrato Financeiro • Tessera</b>\n\nTotal de lançamentos: <b>${transactions.length}</b>\n<i>Arquivo CSV compatível com Excel, Numbers e Google Planilhas!</i>`

      await sendTelegramDocument(chatId, csv, fileName, caption)
      if (message.voice) {
        await maybeSendVoiceReply(chatId, `Acabei de enviar a planilha do seu extrato de ${month}.`, true)
      }
      return new Response("OK", { status: 200 })
    }

    // Ação: Previsão do Tempo
    if (aiResult.action === "get_weather") {
      const forecast = await fetchWeatherForecast(aiResult.location || "São Paulo")
      await sendTelegramMessage(chatId, `${transcriptionNote}${forecast}`)
      if (message.voice) {
        await maybeSendVoiceReply(chatId, forecast, true)
      }
      return new Response("OK", { status: 200 })
    }

    // Ação: Futebol
    if (aiResult.action === "get_soccer") {
      const queryStr = aiResult.query || textInput || aiResult.transcription || ""
      const soccer = await fetchSoccerInfo(aiResult.soccer, queryStr)
      await sendTelegramMessage(chatId, `${transcriptionNote}${soccer.text}`, soccer.replyMarkup)
      if (message.voice) {
        await maybeSendVoiceReply(chatId, soccer.spokenText, true)
      }
      return new Response("OK", { status: 200 })
    }

    // Ação: Briefing Matinal Completo
    if (aiResult.action === "get_briefing") {
      const briefing = await generateMorningBriefing(userFirstName)
      await sendTelegramMessage(chatId, `${transcriptionNote}${briefing.text}`, briefing.replyMarkup)
      if (ENABLE_VOICE_RESPONSES && briefing.spokenText) {
        try {
          const audioBuf = await synthesizeSpeechFrancisca(briefing.spokenText)
          if (audioBuf) {
            await sendTelegramVoice(chatId, audioBuf)
          }
        } catch (vErr) {
          console.error("Erro ao sintetizar áudio no briefing AI:", vErr)
        }
      }
      return new Response("OK", { status: 200 })
    }

    // Ação: Consultar Status da Obra / Apartamento
    if (aiResult.action === "get_apartment") {
      const doc = await getApartmentDoc()
      const card = formatApartmentCard(doc)
      await sendTelegramMessage(chatId, `${transcriptionNote}${card.text}`, card.replyMarkup)
      if (message.voice) {
        const spoken = `A obra do seu apartamento está com ${doc.progress || 0}% de conclusão na fase de ${doc.phase || "Reforma"}. O total investido até o momento é de ${Number(doc.total_spent || 0).toFixed(0)} reais.`
        await maybeSendVoiceReply(chatId, spoken, true)
      }
      return new Response("OK", { status: 200 })
    }

    // Ação: Atualizar Obra / Lançar Gasto da Obra
    if (aiResult.action === "update_apartment") {
      const doc = await getApartmentDoc()
      const ap = aiResult.apartment || {}
      let changeMsg = ""

      if (typeof ap.progress === "number" && !isNaN(ap.progress)) {
        doc.progress = Math.min(100, Math.max(0, ap.progress))
        changeMsg += `📈 Progresso atualizado para <b>${doc.progress}%</b>.\n`
      }
      if (ap.phase) {
        doc.phase = ap.phase
        changeMsg += `🏷️ Fase alterada para <b>${doc.phase}</b>.\n`
      }
      if (ap.expected_date) {
        doc.expected_completion = ap.expected_date
      }

      if (ap.spent_amount && ap.spent_amount > 0) {
        const expenseTitle = ap.expense_title || "Reforma do Apartamento"
        doc.total_spent = (doc.total_spent || 0) + ap.spent_amount
        doc.expenses = doc.expenses || []
        doc.expenses.push({
          title: expenseTitle,
          amount: ap.spent_amount,
          date: new Date().toISOString().split("T")[0]
        })

        // Sincroniza também como transação no dashboard financeiro principal
        try {
          await addFinanceTransaction({
            title: `Obra: ${expenseTitle}`,
            amount: ap.spent_amount,
            type: "expense",
            category: "Moradia",
            accountOrCardName: "Cartão / Conta"
          })
        } catch (fErr) {
          console.error("Erro ao sincronizar despesa da obra nas finanças:", fErr)
        }

        changeMsg += `💸 Lançado gasto de <b>R$ ${ap.spent_amount.toFixed(2).replace(".", ",")}</b> em <i>${expenseTitle}</i>.\n`
      }

      await saveApartmentDoc(doc)
      const card = formatApartmentCard(doc)

      const replyText = `${transcriptionNote}🏗️ <b>Obra Atualizada com Sucesso!</b>\n\n` +
        (changeMsg ? `${changeMsg}\n` : "") +
        card.text

      await sendTelegramMessage(chatId, replyText, card.replyMarkup)
      if (message.voice) {
        const spoken = `Atualizei a obra do apartamento para ${doc.progress}% de conclusão. Total investido agora é de ${Number(doc.total_spent || 0).toFixed(0)} reais.`
        await maybeSendVoiceReply(chatId, spoken, true)
      }
      return new Response("OK", { status: 200 })
    }

    // Ação: Consultar Saúde e Hábitos
    if (aiResult.action === "get_health") {
      const doc = await getHealthDoc()
      const card = formatHealthCard(doc)
      await sendTelegramMessage(chatId, `${transcriptionNote}${card.text}`, card.replyMarkup)
      if (message.voice) {
        const spoken = `Você registrou ${doc.today_water_ml || 0} ml de água hoje, de uma meta de ${doc.water_goal_ml || 2500} ml. Seu peso atual é de ${doc.weight ? doc.weight + " quilos" : "não registrado"}.`
        await maybeSendVoiceReply(chatId, spoken, true)
      }
      return new Response("OK", { status: 200 })
    }

    // Ação: Registrar Água, Peso, Sono ou Passos
    if (aiResult.action === "update_health") {
      const doc = await getHealthDoc()
      const h = aiResult.health || {}
      let logMsg = ""

      if (h.water_ml && h.water_ml > 0) {
        doc.today_water_ml = (doc.today_water_ml || 0) + h.water_ml
        logMsg += `💧 +${h.water_ml}ml de água registrados (Total hoje: ${doc.today_water_ml}ml / ${doc.water_goal_ml}ml).\n`
      }
      if (h.weight && h.weight > 0) {
        doc.weight = h.weight
        logMsg += `⚖️ Peso registrado: <b>${doc.weight} kg</b>.\n`
      }
      if (h.steps && h.steps > 0) {
        doc.today_steps = (doc.today_steps || 0) + h.steps
        logMsg += `👟 +${h.steps} passos registrados (Total hoje: ${doc.today_steps}).\n`
      }
      if (h.sleep_hours && h.sleep_hours > 0) {
        doc.sleep_hours = h.sleep_hours
        logMsg += `😴 Sono registrado: <b>${doc.sleep_hours}h</b> de descanso.\n`
      }

      await saveHealthDoc(doc)
      const card = formatHealthCard(doc)

      const replyText = `${transcriptionNote}🩺 <b>Saúde & Hábitos Atualizados!</b>\n\n` +
        (logMsg ? `${logMsg}\n` : "") +
        card.text

      await sendTelegramMessage(chatId, replyText, card.replyMarkup)
      if (message.voice) {
        const spoken = `Registrado com sucesso na sua rotina de saúde! Continue focado nas suas metas.`
        await maybeSendVoiceReply(chatId, spoken, true)
      }
      return new Response("OK", { status: 200 })
    }

    // Fallback Geral / Chat
    const generalReply = aiResult.reply_text || "Olá! Como posso ajudar você no Tessera hoje?"
    await sendTelegramMessage(chatId, `${transcriptionNote}${generalReply}`)
    if (message.voice) {
      await maybeSendVoiceReply(chatId, generalReply, true)
    }
    return new Response("OK", { status: 200 })

  } catch (err: any) {
    console.error("Erro interno no webhook:", err)
    await sendTelegramMessage(chatId, `⚠️ Desculpe, ocorreu uma oscilação temporária ao processar sua solicitação: ${err.message || err}`)
    return new Response("Internal Error", { status: 200 })
  }
})
