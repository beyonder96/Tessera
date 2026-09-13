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
  return await tgCall("sendMessage", {
    chat_id: chatId,
    text,
    parse_mode: "HTML",
    disable_web_page_preview: true,
    ...(replyMarkup ? { reply_markup: replyMarkup } : {})
  })
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
    const cleanText = text
      .replace(/<[^>]*>/g, "")
      .replace(/•/g, "")
      .replace(/\n+/g, " ")
      .slice(0, 350)
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

async function addFinanceTransaction(tx: {
  title: string
  amount: number
  type: "expense" | "income"
  category?: string
  accountOrCardName?: string
  date?: string
}): Promise<{ id: string; title: string; amount: number; category: string; account: string } | null> {
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

  return {
    id: newSuggestionId,
    title: newSuggestion.title,
    amount: newSuggestion.amount,
    category: newSuggestion.category,
    account: newSuggestion.account_or_card_name || "Geral"
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
  "palmeiras": { id: "134465", name: "Palmeiras" },
  "corinthians": { id: "134284", name: "Corinthians" },
  "timao": { id: "134284", name: "Corinthians" },
  "timão": { id: "134284", name: "Corinthians" },
  "sao paulo": { id: "134291", name: "São Paulo" },
  "são paulo": { id: "134291", name: "São Paulo" },
  "spfc": { id: "134291", name: "São Paulo" },
  "vasco": { id: "134282", name: "Vasco da Gama" },
  "vasco da gama": { id: "134282", name: "Vasco da Gama" },
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
    if (clean.includes(k) || k.includes(clean)) return v
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

async function fetchSoccerInfo(
  soccer?: { team?: string; type?: "next" | "last" | "standings" | "general" },
  rawQuery = ""
): Promise<{ text: string; spokenText: string; replyMarkup?: any }> {
  const queryLower = (soccer?.team || rawQuery || "").toLowerCase()

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

        let teamHighlightSpoken = ""
        if (soccer?.team) {
          const targetTeam = table.find((t: any) => t.strTeam?.toLowerCase().includes(soccer.team!.toLowerCase()))
          if (targetTeam) {
            text += `\n📌 <b>${targetTeam.strTeam}:</b> ${targetTeam.intRank}º lugar com <b>${targetTeam.intPoints} pontos</b> em ${targetTeam.intPlayed} jogos.\n`
            teamHighlightSpoken = ` O ${targetTeam.strTeam} está na ${targetTeam.intRank}ª posição com ${targetTeam.intPoints} pontos.`
          }
        }

        text += `\n⚡ <i>Tabela completa e lances em tempo real na aba de Futebol do seu Tessera!</i>`

        const leader = table[0]
        const spokenText = `Na tabela do Brasileirão, o líder é o ${leader.strTeam} com ${leader.intPoints} pontos, seguido por ${table[1]?.strTeam} com ${table[1]?.intPoints} pontos e ${table[2]?.strTeam} com ${table[2]?.intPoints} pontos.${teamHighlightSpoken}`

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

        return { text, spokenText, replyMarkup }
      }
    } catch (err) {
      console.error("Erro ao buscar tabela:", err)
    }
  }

  // 2. Consulta de Próximo Jogo ou Último Resultado de um Time
  const teamCandidate = soccer?.team || rawQuery
  const resolvedTeam = await searchTeamByName(teamCandidate)

  if (resolvedTeam) {
    const isLastQuery = soccer?.type === "last" ||
      queryLower.includes("ultimo") ||
      queryLower.includes("último") ||
      queryLower.includes("resultado") ||
      queryLower.includes("placar") ||
      queryLower.includes("quanto foi") ||
      queryLower.includes("ganhou") ||
      queryLower.includes("perdeu")

    if (isLastQuery) {
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

            const text = `⚽ <b>Último Jogo • ${resolvedTeam.name}</b>\n\n` +
                         `🏁 <b>${h} ${hs} x ${as} ${a}</b>\n` +
                         `🏆 <b>${league}</b>${event.intRound ? ` • ${event.intRound}ª Rodada` : ""}\n` +
                         `📅 ${dt.formatted} • <b>Encerrado</b>\n` +
                         venue +
                         `\n⚡ <i>Lances e estatísticas completos no app Tessera!</i>`

            const spokenText = `No último jogo pelo ${league}, o resultado foi ${h} ${hs}, ${a} ${as}.`

            const replyMarkup = {
              inline_keyboard: [
                [
                  { text: `📅 Ver Próximo Jogo do ${resolvedTeam.name}`, callback_data: `soccer_team:${resolvedTeam.name.toLowerCase()}` },
                  { text: "🏆 Ver Tabela", callback_data: "soccer_table" }
                ]
              ]
            }

            return { text, spokenText, replyMarkup }
          }
        }
      } catch (err) {
        console.error("Erro ao buscar último jogo:", err)
      }
    }

    // Busca de Próximo Jogo (Default)
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
                { text: "🏁 Último Resultado", callback_data: `soccer_last:${resolvedTeam.id}` }
              ]
            ]
          }

          return { text, spokenText, replyMarkup }
        } else {
          // Se não houver partidas agendadas confirmadas para os próximos dias, busca último jogo
          const lastRes = await fetch(`https://www.thesportsdb.com/api/v1/json/3/eventslast.php?id=${resolvedTeam.id}`)
          if (lastRes.ok) {
            const lastData = await lastRes.json()
            const lastEvent = lastData.results?.[0] || lastData.events?.[0]
            if (lastEvent) {
              const h = lastEvent.strHomeTeam
              const a = lastEvent.strAwayTeam
              const hs = lastEvent.intHomeScore ?? 0
              const as = lastEvent.intAwayScore ?? 0
              const dt = formatMatchDateTime(lastEvent.dateEvent, lastEvent.strTime)

              const text = `⚽ <b>${resolvedTeam.name}</b>\n\n` +
                           `Não há partidas confirmadas para os próximos dias.\n\n` +
                           `🏁 <b>Último Confronto Realizado:</b>\n` +
                           `<b>${h} ${hs} x ${as} ${a}</b>\n` +
                           `🏆 ${lastEvent.strLeague || "Brasileirão"} (${dt.formatted})`

              const spokenText = `Não há partidas agendadas para os próximos dias para o ${resolvedTeam.name}. No último jogo, o placar foi ${h} ${hs} a ${as} contra o ${a}.`

              return { text, spokenText }
            }
          }
        }
      }
    } catch (err) {
      console.error("Erro ao buscar próximo jogo:", err)
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
// CÉREBRO MULTIMODAL GROQ (WHISPER LARGE V3 TURBO + LLAMA / QWEN VISION)
// ============================================================================
interface GroqIntentResponse {
  action: "add_transaction" | "get_finances" | "get_chart" | "export_csv" | "add_task" | "get_tasks" | "query_wishes" | "complete_wish" | "add_wish" | "add_market_items" | "get_market_items" | "get_weather" | "get_soccer" | "chat_general"
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

  const systemPrompt = `Você é o assistente financeiro e hub inteligente do ecossistema Tessera para o usuário ${contextInfo.userFirstName}.
Data de hoje: ${contextInfo.todayDate}.

Você deve analisar o texto ou comando do usuário e responder EXCLUSIVAMENTE em formato JSON (json_object) estruturado com o seguinte schema:

{
  "action": "add_transaction" | "get_finances" | "get_chart" | "export_csv" | "add_task" | "get_tasks" | "query_wishes" | "complete_wish" | "add_wish" | "add_market_items" | "get_market_items" | "get_weather" | "get_soccer" | "chat_general",
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
  "query": "termo chave para busca ou time de futebol",
  "location": "nome da cidade para clima",
  "reply_text": "resposta amigável e concisa em português para o usuário"
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
12. Caso seja uma conversa normal, defina action="chat_general".`

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
      { command: "app", description: "📱 Abrir Tessera Mini App" },
      { command: "saldo", description: "💰 Ver saldo livre, contas e limites" },
      { command: "grafico", description: "📊 Gráfico visual de gastos por categoria" },
      { command: "extrato", description: "📄 Baixar planilha CSV do extrato do mês" },
      { command: "mercado", description: "🛒 Ver lista de compras do supermercado" },
      { command: "lembretes", description: "⏰ Ver tarefas e avisos pendentes" },
      { command: "desejos", description: "🎁 Ver lista de desejos e metas" },
      { command: "tempo", description: "🌤️ Previsão do tempo e clima" },
      { command: "futebol", description: "⚽ Próximos jogos, tabela e placares" },
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
    const weather = await fetchWeatherForecast("São Paulo")
    const taskDoc = await getTasksDoc()
    const items = taskDoc && Array.isArray(taskDoc.data.items) ? taskDoc.data.items : []
    const pendingTasks = items.filter((it: any) => it.status === "pending")

    const finDoc = await getFinanceDoc()
    const spendable = finDoc ? Number(finDoc.data.spendable_balance || 0).toFixed(2).replace(".", ",") : "0,00"

    let tasksSummary = "• Nenhuma tarefa urgente agendada para hoje! ✨\n"
    if (pendingTasks.length > 0) {
      tasksSummary = pendingTasks.slice(0, 5).map((t: any, i: number) => `• <b>${t.title}</b>${t.due_time ? ` (às ${t.due_time})` : ""}`).join("\n") + "\n"
    }

    const message = `🌅 <b>Bom dia, Kenned! Resumo matinal do Tessera:</b>\n\n` +
      `💰 <b>Saldo Livre Atual:</b> R$ ${spendable}\n\n` +
      `⏰ <b>Lembretes e Avisos (${pendingTasks.length}):</b>\n${tasksSummary}\n` +
      `${weather}\n\n` +
      `<i>Tenha um excelente dia! Para registrar gastos ou cupons, só enviar um áudio, foto ou texto. 🚀</i>`

    for (const userId of TELEGRAM_ALLOWED_USER_IDS) {
      await sendTelegramMessage(userId, message)
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
        `• /app — Abrir a central do Tessera em Mini App interativo\n` +
        `• /saldo — Saldo livre, limites de cartão e faturas\n` +
        `• /grafico — Gráfico visual de gastos por categoria\n` +
        `• /extrato — Baixar extrato do mês em planilha CSV\n` +
        `• /mercado — Ver lista de compras pendente\n` +
        `• /lembretes — Ver tarefas e avisos pendentes\n` +
        `• /desejos — Metas e lista de compras planejadas\n` +
        `• /tempo — Previsão do tempo e chuva\n` +
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
            { text: "🛒 Mercado", web_app: { url: "https://tessera-35c54.web.app/market" } },
            { text: "📊 Finanças", web_app: { url: "https://tessera-35c54.web.app/finance" } }
          ]
        ]
      }
      await sendTelegramMessage(chatId, welcomeText, welcomeMarkup)
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

        const responseText = `${transcriptionNote}✅ <b>${typeLabel} Lançada com Sucesso!</b>\n\n` +
          `${emoji} <b>${result.title}:</b> <code>${formattedAmount}</code>\n` +
          `📁 <b>Categoria:</b> ${result.category}\n` +
          `🏦 <b>Conta/Cartão:</b> ${result.account}\n\n` +
          `⚡ <i>Sincronizado em tempo real com seu Tessera!</i>`

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
      const soccer = await fetchSoccerInfo(aiResult.soccer, aiResult.query || promptText)
      await sendTelegramMessage(chatId, `${transcriptionNote}${soccer.text}`, soccer.replyMarkup)
      if (message.voice) {
        await maybeSendVoiceReply(chatId, soccer.spokenText, true)
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
