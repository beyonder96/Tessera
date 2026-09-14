import "jsr:@supabase/functions-js/edge-runtime.d.ts"
import { extractText } from "npm:unpdf"

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
const ENABLE_VOICE_RESPONSES = Deno.env.get("ENABLE_VOICE_RESPONSES") === "true"

// Cache de idempotência em memória para evitar reprocessamento de retries do Telegram
const processedUpdates = new Set<number>()
const processedFileIds = new Set<string>()

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
  const res = await tgCall("editMessageText", {
    chat_id: chatId,
    message_id: messageId,
    text: text.slice(0, 4000),
    parse_mode: "HTML",
    disable_web_page_preview: true,
    ...(replyMarkup ? { reply_markup: replyMarkup } : {})
  })

  // Se a edição falhar por erro de parser HTML ou restrição de edição, faz fallback para envio de nova mensagem
  if (res && res.ok === false) {
    if (res.description?.includes("message is not modified")) {
      return res
    }
    console.warn("editTelegramMessage falhou, enviando mensagem direta como fallback:", res.description)
    return await sendTelegramMessage(chatId, text, replyMarkup)
  }
  return res
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

async function synthesizeSpeechFrancisca(text: string): Promise<ArrayBuffer | null> {
  return await generateSpeechAudio(text, "pt-BR-FranciscaNeural")
}

async function sendTelegramVoice(chatId: number | string, audioBuffer: ArrayBuffer): Promise<any> {
  return await sendTelegramVoiceAudio(chatId, audioBuffer)
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
  client_portal_url: string
  updated_at: string
  expected_date?: string
  notes?: string
}

async function getApartmentDoc(): Promise<ApartmentState> {
  const defaultPortal = "https://relacionamento.planoeplano.app/painel/home"
  try {
    const docs = await supabaseRest(`telegram_bot_logs?action=eq.apartment_state&select=*&order=created_at.desc&limit=1`)
    if (Array.isArray(docs) && docs.length > 0 && docs[0].payload) {
      const p = docs[0].payload
      return {
        progress: typeof p.progress === "number" ? p.progress : 75,
        client_portal_url: p.client_portal_url || defaultPortal,
        updated_at: p.updated_at || docs[0].created_at || new Date().toISOString(),
        expected_date: p.expected_date || "Dez 2026",
        notes: p.notes
      }
    }
  } catch (err) {
    console.error("Erro ao carregar apartment_state:", err)
  }
  return {
    progress: 75,
    client_portal_url: defaultPortal,
    expected_date: "Dez 2026",
    updated_at: new Date().toISOString()
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
  const rawProg = typeof data.progress === "number" ? data.progress : 75
  const pct = Math.min(100, Math.max(0, Math.round(rawProg <= 1 ? rawProg * 100 : rawProg)))
  const bar = renderProgressBar(pct / 100, 12)
  const portalUrl = data.client_portal_url || "https://relacionamento.planoeplano.app/painel/home"
  const expectedDate = data.expected_date || "Dez 2026"

  let dateFormatted = "Hoje"
  if (data.updated_at) {
    try {
      dateFormatted = new Intl.DateTimeFormat("pt-BR", {
        timeZone: "America/Sao_Paulo",
        day: "2-digit",
        month: "2-digit",
        year: "numeric",
        hour: "2-digit",
        minute: "2-digit"
      }).format(new Date(data.updated_at))
    } catch {
      dateFormatted = "Recente"
    }
  }

  const text = `🏗️ <b>Evolução da Obra • Meu Apê</b>\n\n` +
               `📊 <b>Progresso Atual:</b> <code>[${bar}] ${pct}% Concluído</code>\n` +
               `📅 <b>Previsão de Entrega:</b> ${expectedDate}\n` +
               `🕒 <b>Última Atualização:</b> ${dateFormatted}\n\n` +
               `⚡ <i>Toque nos botões rápidos abaixo ou diga: "atualiza a obra para 80%".</i>`

  const spokenText = `A obra do seu apartamento está com ${pct}% de conclusão.`

  const replyMarkup = {
    inline_keyboard: [
      [
        { text: "➖ 1%", callback_data: "apt_step:-1" },
        { text: "➕ 1%", callback_data: "apt_step:1" },
        { text: "🔄 Atualizar Status", callback_data: "menu_apartment_refresh" }
      ],
      [
        { text: "75%", callback_data: "apt_set:75" },
        { text: "80%", callback_data: "apt_set:80" },
        { text: "85%", callback_data: "apt_set:85" },
        { text: "90%", callback_data: "apt_set:90" }
      ],
      [
        { text: "🌐 Acessar Portal do Cliente (Plano&Plano)", url: portalUrl }
      ],
      [
        { text: "🔙 Menu Principal", callback_data: "cmd_menu" }
      ]
    ]
  }

  return { text, spokenText, replyMarkup }
}

// ============================================================================
// ============================================================================
// MÓDULO 1: SAÚDE & BEM-ESTAR (ÁGUA, PESO, PASSOS, SONO, REMÉDIOS)
// ============================================================================
interface HealthState {
  today_water_ml: number
  water_goal_ml: number
  today_steps: number
  steps_goal: number
  latest_weight: number
  latest_sleep_hours: number
  date: string
  weight?: number
  sleep_hours?: number
  water_records?: Array<{ amount_ml: number; timestamp: number; time: string }>
  medications?: Array<{ id: string; name: string; dosage?: string; time?: string; taken: boolean }>
}

async function getHealthDoc(): Promise<HealthState> {
  const todayStr = new Date().toISOString().split("T")[0]
  try {
    const hubDocs = await supabaseRest(`shared_health_hub?id=eq.health_default&select=*`)
    if (Array.isArray(hubDocs) && hubDocs.length > 0 && hubDocs[0].data) {
      const d = hubDocs[0].data
      if (d.date !== todayStr) {
        return {
          ...d,
          today_water_ml: 0,
          today_steps: 0,
          date: todayStr,
          water_records: []
        }
      }
      return d
    }
    const docs = await supabaseRest(`telegram_bot_logs?action=eq.health_state&select=*&order=created_at.desc&limit=1`)
    if (Array.isArray(docs) && docs.length > 0 && docs[0].payload) {
      const d = docs[0].payload
      if (d.date !== todayStr) {
        return { ...d, today_water_ml: 0, today_steps: 0, date: todayStr, water_records: [] }
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
    date: todayStr,
    water_records: []
  }
}

async function saveHealthDoc(data: HealthState, userId = "admin"): Promise<void> {
  try {
    await supabaseRest("shared_health_hub?on_conflict=id", {
      method: "POST",
      headers: { "Prefer": "resolution=merge-duplicates,return=representation" },
      body: JSON.stringify({
        id: "health_default",
        title: "Saúde & Bem-Estar",
        data: data,
        updated_at: new Date().toISOString()
      })
    })
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

  if (Array.isArray(data.medications) && data.medications.length > 0) {
    text += `\n💊 <b>Medicamentos de Hoje:</b>\n`
    data.medications.forEach(m => {
      const statusEmoji = m.taken ? "✅" : "⬜"
      const timeStr = m.time ? ` às ${m.time}` : ""
      text += `• ${statusEmoji} <b>${m.name}</b>${m.dosage ? ` (${m.dosage})` : ""}${timeStr}\n`
    })
  }

  text += `\n💡 <i>Diga "bebi 300ml de água", "pesei 74kg" ou "dormi 8 horas" para registrar!</i>`

  const spokenText = `Você já bebeu ${water} ml de água hoje, o que representa ${waterPct}% da sua meta diária. Você deu ${steps} passos e seu último peso registrado foi de ${data.latest_weight || 74} quilos.`

  const replyMarkup = {
    inline_keyboard: [
      [
        { text: "💧 +250ml", callback_data: "health_water:250" },
        { text: "💧 +500ml", callback_data: "health_water:500" },
        { text: "💧 +1000ml", callback_data: "health_water:1000" }
      ],
      [
        { text: "🔄 Atualizar Status", callback_data: "menu_health" },
        { text: "🔙 Menu Principal", callback_data: "cmd_menu" }
      ]
    ]
  }

  return { text, spokenText, replyMarkup }
}

// ============================================================================
// MÓDULO 2: ROTINAS & HÁBITOS (CHECKLIST DIÁRIO & STREAKS)
// ============================================================================
interface RoutineHabit {
  id: string
  name: string
  icon?: string
  streak: number
  is_completed_today: boolean
  category?: string
}

interface RoutineItem {
  id: string
  title: string
  steps: string[]
}

interface RoutinesState {
  habits: RoutineHabit[]
  routines: RoutineItem[]
  date: string
  updated_at: string
}

async function getRoutinesDoc(): Promise<RoutinesState> {
  const todayStr = new Date().toISOString().split("T")[0]
  try {
    const docs = await supabaseRest(`shared_routines_hub?id=eq.routines_default&select=*`)
    if (Array.isArray(docs) && docs.length > 0 && docs[0].data) {
      const d = docs[0].data
      if (d.date !== todayStr) {
        const updatedHabits = (d.habits || []).map((h: RoutineHabit) => ({
          ...h,
          is_completed_today: false
        }))
        const refreshed = { ...d, habits: updatedHabits, date: todayStr }
        await saveRoutinesDoc(refreshed)
        return refreshed
      }
      return d
    }
  } catch (err) {
    console.error("Erro ao carregar routines_hub:", err)
  }
  return {
    habits: [
      { id: "h1", name: "Hidratação (3L)", icon: "💧", streak: 12, is_completed_today: false, category: "Saúde" },
      { id: "h2", name: "Leitura Profunda", icon: "📖", streak: 5, is_completed_today: false, category: "Mente" },
      { id: "h3", name: "Mindfulness", icon: "🧘", streak: 21, is_completed_today: false, category: "Espiritual" },
      { id: "h4", name: "Treino / Atividade", icon: "🏋️", streak: 8, is_completed_today: false, category: "Corpo" }
    ],
    routines: [
      { id: "r1", title: "Rotina Matinal", steps: ["Beber Água", "Meditação", "Alongamento"] },
      { id: "r2", title: "Rotina Noturna", steps: ["Desconectar Telas", "Higiene do Sono", "Leitura"] }
    ],
    date: todayStr,
    updated_at: new Date().toISOString()
  }
}

async function saveRoutinesDoc(data: RoutinesState): Promise<void> {
  try {
    await supabaseRest("shared_routines_hub?on_conflict=id", {
      method: "POST",
      headers: { "Prefer": "resolution=merge-duplicates,return=representation" },
      body: JSON.stringify({
        id: "routines_default",
        title: "Rotinas & Hábitos",
        data: data,
        updated_at: new Date().toISOString()
      })
    })
  } catch (err) {
    console.error("Erro ao salvar routines_hub:", err)
  }
}

async function toggleHabitInDoc(habitIdOrName: string): Promise<{ doc: RoutinesState; toggledHabit?: RoutineHabit }> {
  const doc = await getRoutinesDoc()
  const habits = doc.habits || []
  const h = habits.find(it => it.id === habitIdOrName || it.name.toLowerCase().includes(habitIdOrName.toLowerCase()))
  if (h) {
    h.is_completed_today = !h.is_completed_today
    if (h.is_completed_today) {
      h.streak = (h.streak || 0) + 1
    } else {
      h.streak = Math.max(0, (h.streak || 1) - 1)
    }
    doc.updated_at = new Date().toISOString()
    await saveRoutinesDoc(doc)
    return { doc, toggledHabit: h }
  }
  return { doc }
}

function formatRoutinesCard(doc: RoutinesState): { text: string; spokenText: string; replyMarkup: any } {
  const habits = doc.habits || []
  const total = habits.length
  const completed = habits.filter(h => h.is_completed_today).length
  const pct = total > 0 ? Math.round((completed / total) * 100) : 0
  const bar = renderProgressBar(total > 0 ? completed / total : 0, 8)

  let text = `🔄 <b>Hábitos & Rotinas Diárias • Tessera</b>\n\n` +
    `Progresso Hoje: <code>[${bar}] ${completed}/${total} (${pct}%)</code>\n\n`

  const keyboard: any[][] = []

  habits.forEach((h, idx) => {
    const statusEmoji = h.is_completed_today ? "✅" : "⬜"
    const icon = h.icon || "📌"
    text += `${idx + 1}. ${statusEmoji} <b>${icon} ${h.name}</b> — 🔥 <b>${h.streak} dias</b>\n`
    keyboard.push([
      {
        text: `${statusEmoji} ${h.name} (${h.streak}🔥)`,
        callback_data: `habit_toggle:${h.id}`
      }
    ])
  })

  keyboard.push([
    { text: "🔄 Atualizar", callback_data: "menu_routines" },
    { text: "🔙 Menu Principal", callback_data: "cmd_menu" }
  ])

  text += `\n💡 <i>Toque em um hábito acima para marcar como feito ou mande um áudio (ex: "Fiz a leitura de hoje")!</i>`

  const spokenText = `Você completou ${completed} de ${total} hábitos hoje. ${total - completed > 0 ? `Ainda restam ${total - completed} hábitos pendentes.` : "Todos os hábitos foram concluídos!"}`

  return { text, spokenText, replyMarkup: { inline_keyboard: keyboard } }
}

// ============================================================================
// MÓDULO 3: PETS (CENTRAL PETZ - THOR, VACINAS & CUIDADOS)
// ============================================================================
interface PetItem {
  id: string
  name: string
  species: string
  breed: string
  weight?: number
  avatar_url?: string
}

interface PetEventItem {
  id: string
  pet_name: string
  title: string
  event_type: "vaccine" | "vet" | "bath" | "medication"
  date: string
  status: "scheduled" | "completed"
}

interface PetsState {
  pets: PetItem[]
  events: PetEventItem[]
  daily_care: {
    fed_morning: boolean
    fed_night: boolean
    walked: boolean
    fresh_water: boolean
    date: string
  }
  updated_at: string
}

async function getPetsDoc(): Promise<PetsState> {
  const todayStr = new Date().toISOString().split("T")[0]
  try {
    const docs = await supabaseRest(`shared_pets_hub?id=eq.pets_default&select=*`)
    if (Array.isArray(docs) && docs.length > 0 && docs[0].data) {
      const d = docs[0].data
      if (d.daily_care && d.daily_care.date !== todayStr) {
        d.daily_care = {
          fed_morning: false,
          fed_night: false,
          walked: false,
          fresh_water: false,
          date: todayStr
        }
        await savePetsDoc(d)
      }
      return d
    }
  } catch (err) {
    console.error("Erro ao carregar pets_hub:", err)
  }
  return {
    pets: [
      { id: "pet1", name: "Thor", species: "Cachorro", breed: "Golden Retriever", weight: 28.5 }
    ],
    events: [
      { id: "e1", pet_name: "Thor", title: "Vacina V10 (Anual)", event_type: "vaccine", date: "2026-11-20", status: "scheduled" },
      { id: "e2", pet_name: "Thor", title: "Antipulgas / Vermífugo", event_type: "medication", date: "2026-10-15", status: "scheduled" }
    ],
    daily_care: {
      fed_morning: false,
      fed_night: false,
      walked: false,
      fresh_water: true,
      date: todayStr
    },
    updated_at: new Date().toISOString()
  }
}

async function savePetsDoc(data: PetsState): Promise<void> {
  try {
    await supabaseRest("shared_pets_hub?on_conflict=id", {
      method: "POST",
      headers: { "Prefer": "resolution=merge-duplicates,return=representation" },
      body: JSON.stringify({
        id: "pets_default",
        title: "Central Petz",
        data: data,
        updated_at: new Date().toISOString()
      })
    })
  } catch (err) {
    console.error("Erro ao salvar pets_hub:", err)
  }
}

async function logPetCareInDoc(action: "fed_morning" | "fed_night" | "walked" | "fresh_water"): Promise<PetsState> {
  const doc = await getPetsDoc()
  if (!doc.daily_care) {
    doc.daily_care = {
      fed_morning: false,
      fed_night: false,
      walked: false,
      fresh_water: false,
      date: new Date().toISOString().split("T")[0]
    }
  }
  doc.daily_care[action] = !doc.daily_care[action]
  doc.updated_at = new Date().toISOString()
  await savePetsDoc(doc)
  return doc
}

function formatPetsCard(doc: PetsState): { text: string; spokenText: string; replyMarkup: any } {
  const pets = doc.pets || []
  const care = doc.daily_care || { fed_morning: false, fed_night: false, walked: false, fresh_water: false }

  let text = `🐾 <b>Central Petz • Tessera</b>\n\n`
  if (pets.length > 0) {
    pets.forEach(p => {
      text += `🐶 <b>${p.name}</b> (${p.species} • ${p.breed})${p.weight ? ` — <b>${p.weight}kg</b>` : ""}\n`
    })
    text += "\n"
  }

  text += `📋 <b>Cuidados de Hoje:</b>\n`
  text += `• Ração Manhã: ${care.fed_morning ? "✅ Alimentado" : "⬜ Pendente"}\n`
  text += `• Ração Noite: ${care.fed_night ? "✅ Alimentado" : "⬜ Pendente"}\n`
  text += `• Água Fresca: ${care.fresh_water ? "✅ Trocada" : "⬜ Pendente"}\n`
  text += `• Passeio Diário: ${care.walked ? "✅ Realizado" : "⬜ Pendente"}\n\n`

  const upcomingEvents = (doc.events || []).filter(e => e.status === "scheduled")
  if (upcomingEvents.length > 0) {
    text += `💉 <b>Próximos Eventos & Vacinas:</b>\n`
    upcomingEvents.slice(0, 3).forEach(e => {
      const typeEmoji = e.event_type === "vaccine" ? "💉" : e.event_type === "vet" ? "🩺" : "🛁"
      text += `• ${typeEmoji} <b>${e.pet_name}:</b> ${e.title} (${e.date})\n`
    })
    text += "\n"
  }

  text += `<i>Diga "dei comida pro pet" ou "passei com o cachorro" para registrar!</i>`

  const replyMarkup = {
    inline_keyboard: [
      [
        { text: care.fed_morning ? "✅ Ração Manhã" : "🍖 Dar Ração Manhã", callback_data: "pet_care:fed_morning" },
        { text: care.fed_night ? "✅ Ração Noite" : "🍖 Dar Ração Noite", callback_data: "pet_care:fed_night" }
      ],
      [
        { text: care.walked ? "✅ Passeio Feito" : "🦮 Registrar Passeio", callback_data: "pet_care:walked" },
        { text: care.fresh_water ? "✅ Água Trocada" : "💧 Trocar Água", callback_data: "pet_care:fresh_water" }
      ],
      [
        { text: "🔄 Atualizar", callback_data: "menu_pets" },
        { text: "🔙 Menu Principal", callback_data: "cmd_menu" }
      ]
    ]
  }

  const petName = pets[0]?.name || "seu pet"
  const spokenText = `Central Petz: ${care.fed_morning ? "Ração da manhã já servida para " + petName + "." : "Lembrete: Ração da manhã pendente para " + petName + "."} ${upcomingEvents.length > 0 ? "Próxima vacina é " + upcomingEvents[0].title + " em " + upcomingEvents[0].date + "." : ""}`

  return { text, spokenText, replyMarkup }
}

// ============================================================================
// MÓDULO 4: TRANSPORTE & MOBILIDADE (METRÔ, TREM CPTM E SPTRANS)
// ============================================================================
interface TransportState {
  monitored_metro_lines: string[]
  saved_bus_lines: Array<{ line_code: string; sign: string; direction: string }>
  updated_at: string
}

async function getTransportDoc(): Promise<TransportState> {
  try {
    const docs = await supabaseRest(`shared_transport_hub?id=eq.transport_default&select=*`)
    if (Array.isArray(docs) && docs.length > 0 && docs[0].data) {
      return docs[0].data
    }
  } catch (err) {
    console.error("Erro ao carregar transport_hub:", err)
  }
  return {
    monitored_metro_lines: ["1", "2", "3", "4", "9"],
    saved_bus_lines: [],
    updated_at: new Date().toISOString()
  }
}

async function saveTransportDoc(data: TransportState): Promise<void> {
  try {
    await supabaseRest("shared_transport_hub?on_conflict=id", {
      method: "POST",
      headers: { "Prefer": "resolution=merge-duplicates,return=representation" },
      body: JSON.stringify({
        id: "transport_default",
        title: "Transporte & Mobilidade",
        data: data,
        updated_at: new Date().toISOString()
      })
    })
  } catch (err) {
    console.error("Erro ao salvar transport_hub:", err)
  }
}

interface MetroLineStatus {
  codigo: string
  nome: string
  status: string
  descricao?: string
  tipo: "metro" | "cptm"
}

async function fetchLiveMetroStatus(): Promise<MetroLineStatus[]> {
  const defaultLines: MetroLineStatus[] = [
    { codigo: "1", nome: "Azul", status: "Operação Normal", tipo: "metro" },
    { codigo: "2", nome: "Verde", status: "Operação Normal", tipo: "metro" },
    { codigo: "3", nome: "Vermelha", status: "Operação Normal", tipo: "metro" },
    { codigo: "4", nome: "Amarela", status: "Operação Normal", tipo: "metro" },
    { codigo: "5", nome: "Lilás", status: "Operação Normal", tipo: "metro" },
    { codigo: "15", nome: "Prata", status: "Operação Normal", tipo: "metro" },
    { codigo: "7", nome: "Rubi", status: "Operação Normal", tipo: "cptm" },
    { codigo: "8", nome: "Diamante", status: "Operação Normal", tipo: "cptm" },
    { codigo: "9", nome: "Esmeralda", status: "Operação Normal", tipo: "cptm" },
    { codigo: "10", nome: "Turquesa", status: "Operação Normal", tipo: "cptm" },
    { codigo: "11", nome: "Coral", status: "Operação Normal", tipo: "cptm" },
    { codigo: "12", nome: "Safira", status: "Operação Normal", tipo: "cptm" },
    { codigo: "13", nome: "Jade", status: "Operação Normal", tipo: "cptm" }
  ]

  try {
    const cptmRes = await fetch("https://api.cptm.sp.gov.br/AppCPTM/v1/Linhas/ObterStatus", {
      headers: { "Accept": "application/json" },
      signal: AbortSignal.timeout(4000)
    })
    if (cptmRes.ok) {
      const data = await cptmRes.json()
      if (Array.isArray(data)) {
        data.forEach((item: any) => {
          const num = String(item.LinhaId || item.NumeroLinha || "")
          const line = defaultLines.find(l => l.codigo === num)
          if (line) {
            line.status = item.StatusLinha || item.DescricaoStatus || line.status
            line.descricao = item.Descricao || ""
          }
        })
      }
    }
  } catch (_err) {
    // Mantém linhas com status de operação padrão
  }

  return defaultLines
}

function formatTransportCard(lines: MetroLineStatus[], monitoredCodes: string[]): { text: string; spokenText: string; replyMarkup: any } {
  let text = `🚇 <b>Situação do Metrô & Trens • São Paulo</b>\n\n`
  let issuesCount = 0
  let issuesText = ""

  lines.forEach(l => {
    const isMonitored = monitoredCodes.includes(l.codigo)
    const isNormal = l.status.toLowerCase().includes("normal")
    const emoji = isNormal ? "🟢" : "🟡"
    if (!isNormal) {
      issuesCount++
      issuesText += `Linha ${l.codigo}-${l.nome} com ${l.status}. `
    }
    const star = isMonitored ? " ⭐" : ""
    text += `${emoji} <b>Linha ${l.codigo} - ${l.nome}:</b> ${l.status}${star}\n`
  })

  text += `\n⭐ <i>Linhas com estrela são suas favoritas configuradas no Tessera!</i>`

  const replyMarkup = {
    inline_keyboard: [
      [
        { text: "🔄 Atualizar Status", callback_data: "transport_refresh" },
        { text: "⭐ Minhas Linhas", callback_data: "transport_monitored" }
      ],
      [
        { text: "🔙 Menu Principal", callback_data: "cmd_menu" }
      ]
    ]
  }

  const spokenText = issuesCount === 0
    ? "Todas as linhas de metrô e trens de São Paulo estão operando normalmente neste momento."
    : `Atenção no transporte: ${issuesText}`

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
    const rawProg = typeof apt.progress === "number" ? apt.progress : 78
    const pct = Math.min(100, Math.max(0, Math.round(rawProg <= 1 ? rawProg * 100 : rawProg)))
    const bar = renderProgressBar(pct / 100, 10)
    aptSummary = `📊 <code>[${bar}] ${pct}% Concluído</code> • Construtora Plano&Plano`
  } catch (_e) {}

  // 5. Saúde & Hidratação
  let healthSummary = ""
  try {
    const hl = await getHealthDoc()
    healthSummary = `💧 <b>Hidratação:</b> ${hl.today_water_ml}ml / ${hl.water_goal_ml}ml`
    if (hl.latest_sleep_hours) {
      healthSummary += ` • 😴 <b>Sono:</b> ${hl.latest_sleep_hours}h registradas`
    }
  } catch (_e) {}

  // 6. Hábitos & Rotinas do Dia
  let routinesSummary = ""
  try {
    const rt = await getRoutinesDoc()
    const habits = rt.habits || []
    const pendingHabits = habits.filter(h => !h.is_completed_today)
    if (habits.length > 0) {
      routinesSummary = `🎯 <b>Hábitos de Hoje:</b> ${habits.length - pendingHabits.length}/${habits.length} concluídos (${pendingHabits.length} pendentes)`
    }
  } catch (_e) {}

  // 7. Pets & Cuidados
  let petsSummary = ""
  try {
    const pt = await getPetsDoc()
    const care = pt.daily_care
    const fed = care?.fed_morning ? "✅ Alimentado" : "⬜ Ração matinal pendente"
    const petName = pt.pets?.[0]?.name || "Thor"
    petsSummary = `🐶 <b>${petName}:</b> ${fed}`
  } catch (_e) {}

  // 8. Transporte & Mobilidade (Linhas Monitoradas)
  let transportSummary = ""
  try {
    const tr = await getTransportDoc()
    const metroStatus = await fetchLiveMetroStatus()
    const mon = tr.monitored_metro_lines || ["1", "2", "3", "4", "9"]
    const abnormal = metroStatus.filter(l => mon.includes(l.codigo) && !l.status.toLowerCase().includes("normal"))
    if (abnormal.length > 0) {
      transportSummary = `🚨 <b>Atenção no Metrô:</b> ` + abnormal.map(l => `Linha ${l.codigo}-${l.nome} (${l.status})`).join(", ")
    } else {
      transportSummary = `🚇 <b>Metrô & Trens:</b> Operação normal nas suas linhas favoritas.`
    }
  } catch (_e) {}

  // 9. Futebol / Mengão
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
    text += `🩺 <b>Saúde:</b>\n${healthSummary}\n\n`
  }

  if (routinesSummary) {
    text += `🔄 <b>Rotinas:</b>\n${routinesSummary}\n\n`
  }

  if (petsSummary) {
    text += `🐾 <b>Pets:</b>\n${petsSummary}\n\n`
  }

  if (transportSummary) {
    text += `${transportSummary}\n\n`
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
        { text: "🏗️ Obra", callback_data: "menu_apartment" },
        { text: "🩺 Saúde", callback_data: "menu_health" },
        { text: "🔄 Hábitos", callback_data: "menu_routines" }
      ],
      [
        { text: "🐾 Pets", callback_data: "menu_pets" },
        { text: "🚇 Metrô", callback_data: "transport_refresh" },
        { text: "💰 Saldo", callback_data: "menu_saldo" }
      ]
    ]
  }

  return { text, spokenText, replyMarkup }
}

// ============================================================================
// CÉREBRO MULTIMODAL GROQ (WHISPER LARGE V3 TURBO + LLAMA / QWEN VISION)
// ============================================================================
interface GroqIntentResponse {
  action: "add_transaction" | "get_finances" | "get_chart" | "export_csv" | "add_task" | "get_tasks" | "query_wishes" | "complete_wish" | "add_wish" | "add_market_items" | "get_market_items" | "get_weather" | "get_soccer" | "get_briefing" | "get_apartment" | "update_apartment" | "get_health" | "update_health" | "get_routines" | "toggle_habit" | "get_pets" | "log_pet_care" | "get_transport" | "chat_general"
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
  habit?: {
    name?: string
    completed?: boolean
  }
  pet?: {
    action?: "fed_morning" | "fed_night" | "walked" | "fresh_water"
    name?: string
  }
  transport?: {
    line?: string
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

// ============================================================================
// PARSER DE EXTRATO BANCÁRIO (UNPDF + DETERMINÍSTICO + GROQ LLAMA 3.3 70B)
// ============================================================================
function cleanStatementTitle(desc: string): string {
  let title = desc.trim()
  if (title === "REMUNERACAO/SALARIO") return "Salário / Remuneração"
  if (title === "REND PAGO APLIC AUT MAIS") return "Rendimento Conta Automática"
  if (title.startsWith("ADIANT.DEPOSITANTE")) return "Tarifa Adiantamento Depositante"
  if (title.startsWith("JUROS SALDO DEVEDOR")) return "Juros Saldo Devedor"
  if (title === "IOF") return "IOF Bancário"

  title = title
    .replace(/^RSHOP\s+/i, "")
    .replace(/^PIX\s+TRANSF\s+/i, "Pix: ")
    .replace(/^PIX\s+QRS\s+/i, "Pix QR: ")
    .replace(/^PIX\s+AUT\s+/i, "Pix: ")
    .replace(/^EST\s+ON\s+/i, "Estorno: ")
    .replace(/^ON\s+/i, "")
    .replace(/^ESTORNO\s+/i, "Estorno: ")
    .replace(/^PAY\s+DL\s*/i, "Pagamento Pay DL ")
    .trim()

  title = title
    .replace(/\s+\d{2}\/\d{2}$/, "")
    .replace(/\b\d{4}$/, "")
    .trim()

  if (/uber/i.test(title)) return title.toLowerCase().includes("estorno") ? "Estorno: Uber" : "Uber"
  if (/spotify/i.test(title)) return "Spotify"
  if (/99food/i.test(title)) return "99Food"
  if (/ifd|ifood/i.test(title)) return "iFood"
  if (/carrefour/i.test(title)) return "Carrefour"
  if (/cacau\s*show/i.test(title)) return "Cacau Show"
  if (/emporiopdoce/i.test(title)) return "Empório Pdoce"
  if (/primusburgue/i.test(title)) return "Primus Burger"
  if (/paes\s*e\s*doces/i.test(title)) return "Pães e Doces"
  if (/alemaoba/i.test(title)) return "Bar do Alemão"
  if (/prodata/i.test(title)) return "Prodata"
  if (/google/i.test(title)) return "Google Brasil"
  if (/nakata/i.test(title)) return "Nakata Café"
  return title
}

function categorizeStatementTx(desc: string, type: "expense" | "income"): string {
  const d = desc.toLowerCase()
  if (type === "income" && (d.includes("salario") || d.includes("remuneracao"))) return "Salário"
  if (type === "income" && (d.includes("rend") || d.includes("aplic"))) return "Investimentos"
  if (d.includes("uber") || (d.includes("99") && !d.includes("food")) || d.includes("posto")) return "Transporte"
  if (d.includes("food") || d.includes("ifd") || d.includes("ifood") || d.includes("burgue") || d.includes("boteco") || d.includes("cafe") || d.includes("restaurante") || d.includes("doces") || d.includes("alemaoba")) return "Alimentação"
  if (d.includes("carrefour") || d.includes("emporio") || d.includes("merc") || d.includes("paes")) return "Mercado"
  if (d.includes("spotify") || d.includes("google") || d.includes("netflix")) return "Assinaturas"
  if (d.includes("cacau show")) return "Lazer"
  if (d.includes("iof") || d.includes("juros") || d.includes("adiant.depositante")) return "Tarifas & Encargos"
  if (d.includes("pix")) return "Transferências"
  return "Geral"
}

function parseBrazilianBankStatementText(rawText: string): {
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
} | null {
  if (!rawText || rawText.trim().length < 20) return null

  // Identificação do banco
  let bankName = "Banco"
  if (/itaú|itau/i.test(rawText)) bankName = "Itaú"
  else if (/nubank/i.test(rawText)) bankName = "Nubank"
  else if (/bradesco/i.test(rawText)) bankName = "Bradesco"
  else if (/santander/i.test(rawText)) bankName = "Santander"
  else if (/inter/i.test(rawText)) bankName = "Banco Inter"

  // Período
  let period: string | undefined
  const periodMatch = rawText.match(/per[íi]odo(?: de visualiza[çc][ãa]o)?:\s*(\d{2}\/\d{2}\/\d{4})\s*(?:at[ée]|a)\s*(\d{2}\/\d{2}\/\d{4})/i)
  if (periodMatch) {
    period = `${periodMatch[1]} a ${periodMatch[2]}`
  }

  const lines = rawText.split(/\r?\n/)
  const transactions: Array<{
    date: string
    title: string
    amount: number
    type: "expense" | "income"
    category: string
  }> = []

  let totalIncome = 0
  let totalExpense = 0

  for (const line of lines) {
    const trimmed = line.trim()
    if (!trimmed) continue
    if (/saldo (?:do dia|anterior|final|bloqueado|projetado)/i.test(trimmed)) continue
    if (/total de (?:d[ée]bitos|cr[ée]ditos|lan[çc]amentos)/i.test(trimmed)) continue

    // Regex de padrão bancário: DD/MM/YYYY DESCRIÇÃO VALOR [SALDO]
    const match = trimmed.match(/^(\d{2}\/\d{2}\/\d{4})\s+(.+?)\s+(-?[\d\.]+,\d{2})(?:\s+(-?[\d\.]+,\d{2}))?$/)
    if (match) {
      const rawDate = match[1]
      const desc = match[2].trim()
      const valStr = match[3].replace(/\./g, "").replace(",", ".")
      const valNum = parseFloat(valStr)
      if (isNaN(valNum) || valNum === 0) continue

      const parts = rawDate.split("/")
      const dateFormatted = `${parts[2]}-${parts[1]}-${parts[0]}`

      const isIncome = valNum > 0
      const amount = Math.round(Math.abs(valNum) * 100) / 100
      const type: "expense" | "income" = isIncome ? "income" : "expense"

      if (isIncome) totalIncome += amount
      else totalExpense += amount

      transactions.push({
        date: dateFormatted,
        title: cleanStatementTitle(desc),
        amount,
        type,
        category: categorizeStatementTx(desc, type)
      })
    }
  }

  if (transactions.length >= 2) {
    return {
      bankName,
      period,
      totalIncome: Math.round(totalIncome * 100) / 100,
      totalExpense: Math.round(totalExpense * 100) / 100,
      transactions
    }
  }

  return null
}

async function extractTextFromPdf(pdfBuffer: ArrayBuffer): Promise<string> {
  // 1. Extração de alta performance via unpdf (Edge Runtime)
  try {
    const res = await extractText(new Uint8Array(pdfBuffer))
    const text = Array.isArray(res.text) ? res.text.join("\n") : String(res.text || "")
    if (text && text.trim().length > 30) {
      console.log(`unpdf extraiu ${text.length} caracteres do PDF com sucesso!`)
      return text.trim()
    }
  } catch (uErr) {
    console.warn("Aviso ao extrair texto com unpdf:", uErr)
  }

  // 2. Fallback de extração leve em memória
  try {
    const bytes = new Uint8Array(pdfBuffer)
    const raw = new TextDecoder("latin1").decode(bytes)
    let extracted = ""
    const directTjRegex = new RegExp("\\(([^()\\\\]*(?:\\\\.[^()\\\\]*)*)\\)\\s*(?:Tj|['\"])", "g")
    let tjMatch: RegExpExecArray | null
    while ((tjMatch = directTjRegex.exec(raw)) !== null) {
      const cleaned = tjMatch[1].replace(/\\([()\\])/g, "$1")
      if (cleaned.trim()) extracted += cleaned + " "
    }
    return extracted.trim()
  } catch (err) {
    console.warn("Aviso no fallback de extração:", err)
    return ""
  }
}

async function processBankStatementPdfWithGroq(pdfBuffer: ArrayBuffer, pdfBase64: string): Promise<{
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
  // 1. Extração nativa de texto do PDF via unpdf
  const text = await extractTextFromPdf(pdfBuffer)

  // 2. Parser Determinístico Ultrarrápido (< 5ms) para Itaú e bancos brasileiros
  if (text && text.length > 20) {
    const fastParsed = parseBrazilianBankStatementText(text)
    if (fastParsed && fastParsed.transactions.length >= 2) {
      console.log(`Parser determinístico extraiu ${fastParsed.transactions.length} transações do ${fastParsed.bankName}!`)
      return fastParsed
    }
  }

  // 3. Análise Contábil com Groq Llama 3.3 70B (se houver texto extraído)
  if (text && text.length > 30 && GROQ_API_KEY) {
    console.log(`Texto extraído do PDF (${text.length} caracteres). Analisando com Groq Llama 3.3 70B...`)
    const groqPrompt = `Você é um analista contábil e de conciliação bancária sênior do aplicativo financeiro Tessera.
Analise com extrema precisão este extrato bancário em formato texto (ex: Itaú, Nubank, Bradesco, Santander, Inter, etc.):

Diretrizes estritas de conciliação:
1. Identifique o banco emissor (ex: "Itaú", "Nubank", "Bradesco", etc.) e o período do extrato.
2. Identifique cada transação financeira individual do período:
   - "date": data no formato YYYY-MM-DD.
   - "title": descrição limpa e legível (ex: "Supermercado Pão de Açúcar", "Posto Shell", "Pix - João", "Salário", etc. - remova códigos numéricos inúteis).
   - "amount": valor numérico estritamente positivo (ex: 45.90).
   - "type": "expense" se for saída/débito/pagamento/compra; "income" se for entrada/crédito/salário/PIX recebido.
   - "category": uma categoria concisa ("Mercado", "Alimentação", "Transporte", "Moradia", "Saúde", "Lazer", "Salário", "Serviços", "Geral").
3. NUNCA inclua linhas de "Saldo Anterior", "Saldo do Dia", "Saldo Final", "Total de Débitos", "Bloqueios" ou totais acumulados como transações.
4. Calcule "totalIncome" (soma das entradas), "totalExpense" (soma das saídas) e liste todas as transações em "transactions".

Texto bruto do extrato:
"""
${text.slice(0, 45000)}
"""

Responda EXCLUSIVAMENTE com um JSON no formato:
{
  "bankName": "Itaú",
  "period": "14/08/2026 a 13/09/2026",
  "totalIncome": 3140.20,
  "totalExpense": 3203.33,
  "transactions": [
    { "date": "2026-09-11", "title": "Pix: NICOLI", "amount": 10.00, "type": "income", "category": "Transferências" }
  ]
}`

    try {
      const controller = new AbortController()
      const timeoutId = setTimeout(() => controller.abort(), 20000)

      const gRes = await fetch("https://api.groq.com/openai/v1/chat/completions", {
        method: "POST",
        headers: {
          "Authorization": `Bearer ${GROQ_API_KEY}`,
          "Content-Type": "application/json"
        },
        body: JSON.stringify({
          model: "llama-3.3-70b-versatile",
          messages: [
            { role: "system", content: "Você é um extrator e auditor contábil JSON estrito. Responda apenas JSON válido." },
            { role: "user", content: groqPrompt }
          ],
          temperature: 0.1,
          response_format: { type: "json_object" }
        }),
        signal: controller.signal
      })
      clearTimeout(timeoutId)

      if (gRes.ok) {
        const gData = await gRes.json()
        const content = gData.choices?.[0]?.message?.content || "{}"
        const parsed = JSON.parse(content)
        if (Array.isArray(parsed.transactions) && parsed.transactions.length > 0) {
          return parsed
        }
      } else {
        console.warn("Groq retornou status não-OK:", gRes.status, await gRes.text())
      }
    } catch (gErr) {
      console.error("Erro na análise via Groq:", gErr)
    }
  }

  // 4. Fallback inteligente para Gemini apenas para PDFs digitalizados via foto/scanner
  return await processBankStatementPdfWithGemini(pdfBase64)
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
    throw new Error("Não foi possível extrair texto legível do extrato e GEMINI_API_KEY não está configurada.")
  }

  let targetModel = "gemini-2.5-flash"
  try {
    const listResp = await fetch(`https://generativelanguage.googleapis.com/v1beta/models?key=${geminiKey}`, {
      signal: AbortSignal.timeout(5000)
    })
    if (listResp.ok) {
      const listData = await listResp.json() as { models?: Array<{ name: string; supportedGenerationMethods?: string[] }> }
      const availableModels = (listData.models || [])
        .filter((m) => m.supportedGenerationMethods?.includes("generateContent"))
        .map((m) => m.name.replace("models/", ""))

      const candidate = availableModels.find((m) => m.includes("2.5-flash"))
        || availableModels.find((m) => m.includes("1.5-flash"))
        || availableModels.find((m) => m.includes("flash"))
        || availableModels[0]
      if (candidate) targetModel = candidate
    }
  } catch (lErr) {
    console.warn("Aviso ao consultar modelos Gemini disponíveis:", lErr)
  }

  const prompt = `Você é um analista contábil e de conciliação bancária sênior do aplicativo financeiro Tessera.
Analise com extrema precisão este extrato bancário em PDF (ex: Itaú, Nubank, Bradesco, Santander, Inter, etc.).
Diretrizes estritas de leitura:
1. Identifique o banco emissor (ex: "Itaú", "Nubank", etc.) e o período do extrato.
2. Identifique cada transação financeira individual do período:
   - "date": data no formato YYYY-MM-DD.
   - "title": descrição limpa e legível.
   - "amount": valor numérico estritamente positivo (ex: 45.90).
   - "type": "expense" se for saída/débito/pagamento/compra; "income" se for entrada/crédito/salário/PIX recebido.
   - "category": categoria concisa ("Mercado", "Alimentação", "Transporte", "Moradia", "Saúde", "Lazer", "Salário", "Serviços", "Geral").
3. NUNCA inclua linhas de "Saldo Anterior", "Saldo do Dia", "Saldo Final" ou totais acumulados como transações.
4. Calcule "totalIncome", "totalExpense" e liste todas as transações em "transactions".

Responda APENAS com um objeto JSON válido no formato:
{
  "bankName": "Itaú",
  "period": "14/08/2026 a 13/09/2026",
  "totalIncome": 3140.20,
  "totalExpense": 3203.33,
  "transactions": [
    { "date": "2026-09-11", "title": "Pix: NICOLI", "amount": 10.00, "type": "income", "category": "Transferências" }
  ]
}`

  const url = `https://generativelanguage.googleapis.com/v1beta/models/${targetModel}:generateContent?key=${geminiKey}`
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

  const response = await fetch(url, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(body),
    signal: AbortSignal.timeout(20000)
  })

  if (!response.ok) {
    const errText = await response.text()
    console.error("Erro no Gemini PDF Processing:", response.status, errText)
    throw new Error(`Erro na análise do extrato bancário com Gemini (${targetModel}): ${errText}`)
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
  "action": "add_transaction" | "get_finances" | "get_chart" | "export_csv" | "add_task" | "get_tasks" | "query_wishes" | "complete_wish" | "add_wish" | "add_market_items" | "get_market_items" | "get_weather" | "get_soccer" | "get_briefing" | "get_apartment" | "update_apartment" | "get_health" | "update_health" | "get_routines" | "toggle_habit" | "get_pets" | "log_pet_care" | "get_transport" | "chat_general",
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
  "habit": {
    "name": "nome do hábito (ex: Leitura, Hidratação, Treino, Meditação)",
    "completed": true
  },
  "pet": {
    "action": "fed_morning" | "fed_night" | "walked" | "fresh_water",
    "name": "Thor"
  },
  "transport": {
    "line": "número ou nome da linha (ex: Linha 1, Azul, Linha 4, Amarela, Linha 9)"
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
6. Se o usuário perguntar da lista de desejos, painel de desejos ou compras planejadas (ex: 'painel de desejos', 'o que tem na lista de desejos?', 'meus desejos', 'ver desejos'), defina action="query_wishes".
7. Se pedir para adicionar produtos à lista de compras do supermercado (ex: 'adiciona 2 caixas de leite e café no mercado'), defina action="add_market_items" e preencha "market_items".
8. Se perguntar o que tem para comprar na lista de compras (ex: 'o que tem no mercado?', 'o que falta comprar?'), defina action="get_market_items".
9. Se perguntar do tempo ou chuva, defina action="get_weather".
10. Se perguntar de futebol, jogos, placares, próximos confrontos ou tabela do Brasileirão, defina action="get_soccer" e preencha "soccer".
11. Se pedir gráfico visual ou como estão os gastos por categoria (ex: 'me mostra um gráfico', 'gráfico de despesas'), defina action="get_chart".
12. Se pedir para baixar ou exportar o extrato em planilha/CSV (ex: 'me envia o extrato em excel', 'quero a planilha de gastos'), defina action="export_csv".
13. Caso seja uma pergunta sobre história, teologia, filosofia, ciências, tecnologia, literatura, conselhos ou conversa geral, defina action="chat_general" e elabore uma resposta rica, didática, completa e bem formulada no campo "reply_text".
14. Se o usuário pedir um briefing, resumo do dia, panorama matinal ou disser "bom dia" / "me atualiza de tudo", defina action="get_briefing".
15. Se o usuário perguntar da obra, status do apartamento, apê, reforma ou portal do cliente (ex: "como tá a obra?", "obra do apê", "status do apê", "portal do cliente"), defina action="get_apartment".
16. Se o usuário pedir para atualizar a porcentagem da obra ou informar novo progresso (ex: "atualiza a obra para 80%", "apê em 85%", "obra 82%"), defina action="update_apartment" e preencha "apartment.progress" com o número inteiro (ex: 80). Se enviar uma URL, coloque em "apartment.portal_url".
17. Se o usuário perguntar de saúde, água ingerida, peso ou sono (ex: "como tá minha saúde hoje?", "quanta água bebi?", "meta de água"), defina action="get_health".
18. Se o usuário registrar ingestão de água, peso, passos ou sono (ex: "bebi 500ml de água", "tomei um copo de água", "pesei 74.2kg", "dormi 8 horas"), defina action="update_health" e preencha "health" (para 'um copo de água', use water_ml=250; para 'garrafa de água', use water_ml=500).
19. Se o usuário perguntar de hábitos diários, rotinas ou streaks (ex: "quais hábitos faltam hoje?", "minha rotina", "ver hábitos", "hábitos"), defina action="get_routines".
20. Se o usuário disser que realizou ou completou um hábito (ex: "fiz a leitura de hoje", "já meditei", "fiz o treino", "tomei água da meta"), defina action="toggle_habit" e preencha "habit.name".
21. Se o usuário perguntar dos pets, cuidados ou vacinas (ex: "como tá o Thor?", "vacinas do cachorro", "painel pet", "pets"), defina action="get_pets".
22. Se o usuário disser que deu comida, passeou ou trocou água do pet (ex: "dei ração pro Thor", "passei com o cachorro", "troquei a água do Thor"), defina action="log_pet_care" e preencha "pet.action" ("fed_morning" ou "fed_night" ou "walked" ou "fresh_water").
23. Se o usuário perguntar da situação do metrô, trem, CPTM, trânsito ou linhas de SP (ex: "como tá o metrô?", "linha amarela tá funcionando?", "tem problema na linha 9?", "trânsito metrô"), defina action="get_transport".`

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

  // Endpoints Diretos de Sincronização para o App Android (Tessera Mobile)
  if (update?.action === "sync_apartment") {
    const rawProgress = typeof update.progress === "number" ? update.progress : 75
    const progress = rawProgress <= 1 ? Math.round(rawProgress * 100) : Math.round(rawProgress)
    const expectedDate = update.expected_date || "Dez 2026"
    const portalUrl = update.portal_url || "https://relacionamento.planoeplano.app/painel/home"
    const doc: ApartmentState = {
      progress: Math.min(100, Math.max(0, progress)),
      client_portal_url: portalUrl,
      updated_at: new Date().toISOString(),
      expected_date: expectedDate
    }
    await saveApartmentDoc(doc, "mobile_app")
    return new Response(JSON.stringify({ ok: true, data: doc }), {
      headers: { "Content-Type": "application/json", "Access-Control-Allow-Origin": "*" }
    })
  }

  if (update?.action === "get_apartment") {
    const doc = await getApartmentDoc()
    return new Response(JSON.stringify({ ok: true, data: doc }), {
      headers: { "Content-Type": "application/json", "Access-Control-Allow-Origin": "*" }
    })
  }

  // Endpoints Diretos: Saúde
  if (update?.action === "sync_health") {
    const current = await getHealthDoc()
    const updated: HealthState = {
      ...current,
      today_water_ml: typeof update.today_water_ml === "number" ? update.today_water_ml : current.today_water_ml,
      water_goal_ml: typeof update.water_goal_ml === "number" ? update.water_goal_ml : current.water_goal_ml,
      today_steps: typeof update.today_steps === "number" ? update.today_steps : current.today_steps,
      steps_goal: typeof update.steps_goal === "number" ? update.steps_goal : current.steps_goal,
      latest_weight: typeof update.latest_weight === "number" ? update.latest_weight : current.latest_weight,
      latest_sleep_hours: typeof update.latest_sleep_hours === "number" ? update.latest_sleep_hours : current.latest_sleep_hours,
      medications: Array.isArray(update.medications) ? update.medications : current.medications,
      water_records: Array.isArray(update.water_records) ? update.water_records : current.water_records
    }
    await saveHealthDoc(updated, "mobile_app")
    return new Response(JSON.stringify({ ok: true, data: updated }), {
      headers: { "Content-Type": "application/json", "Access-Control-Allow-Origin": "*" }
    })
  }

  if (update?.action === "get_health") {
    const doc = await getHealthDoc()
    return new Response(JSON.stringify({ ok: true, data: doc }), {
      headers: { "Content-Type": "application/json", "Access-Control-Allow-Origin": "*" }
    })
  }

  // Endpoints Diretos: Rotinas & Hábitos
  if (update?.action === "sync_routines") {
    const doc = await getRoutinesDoc()
    if (Array.isArray(update.habits)) doc.habits = update.habits
    if (Array.isArray(update.routines)) doc.routines = update.routines
    doc.updated_at = new Date().toISOString()
    await saveRoutinesDoc(doc)
    return new Response(JSON.stringify({ ok: true, data: doc }), {
      headers: { "Content-Type": "application/json", "Access-Control-Allow-Origin": "*" }
    })
  }

  if (update?.action === "get_routines") {
    const doc = await getRoutinesDoc()
    return new Response(JSON.stringify({ ok: true, data: doc }), {
      headers: { "Content-Type": "application/json", "Access-Control-Allow-Origin": "*" }
    })
  }

  // Endpoints Diretos: Pets
  if (update?.action === "sync_pets") {
    const doc = await getPetsDoc()
    if (Array.isArray(update.pets)) doc.pets = update.pets
    if (Array.isArray(update.events)) doc.events = update.events
    if (update.daily_care) doc.daily_care = { ...doc.daily_care, ...update.daily_care }
    doc.updated_at = new Date().toISOString()
    await savePetsDoc(doc)
    return new Response(JSON.stringify({ ok: true, data: doc }), {
      headers: { "Content-Type": "application/json", "Access-Control-Allow-Origin": "*" }
    })
  }

  if (update?.action === "get_pets") {
    const doc = await getPetsDoc()
    return new Response(JSON.stringify({ ok: true, data: doc }), {
      headers: { "Content-Type": "application/json", "Access-Control-Allow-Origin": "*" }
    })
  }

  // Endpoints Diretos: Transporte
  if (update?.action === "sync_transport") {
    const doc = await getTransportDoc()
    if (Array.isArray(update.monitored_metro_lines)) doc.monitored_metro_lines = update.monitored_metro_lines
    if (Array.isArray(update.saved_bus_lines)) doc.saved_bus_lines = update.saved_bus_lines
    doc.updated_at = new Date().toISOString()
    await saveTransportDoc(doc)
    return new Response(JSON.stringify({ ok: true, data: doc }), {
      headers: { "Content-Type": "application/json", "Access-Control-Allow-Origin": "*" }
    })
  }

  if (update?.action === "get_transport") {
    const doc = await getTransportDoc()
    const metroStatus = await fetchLiveMetroStatus()
    return new Response(JSON.stringify({ ok: true, data: doc, metro_status: metroStatus }), {
      headers: { "Content-Type": "application/json", "Access-Control-Allow-Origin": "*" }
    })
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
    const userFirstName = cq.from?.first_name || "Kenned"

    // Verificação de Segurança
    if (TELEGRAM_ALLOWED_USER_IDS.length > 0 && !TELEGRAM_ALLOWED_USER_IDS.includes(fromId)) {
      await answerCallbackQuery(cq.id, "Acesso não autorizado.")
      return new Response("Forbidden", { status: 200 })
    }

    try {

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
    if (data === "menu_apartment" || data === "menu_apartment_refresh") {
      const doc = await getApartmentDoc()
      const card = formatApartmentCard(doc)
      await answerCallbackQuery(cq.id, `Status da obra verificado: ${doc.progress}% concluído! ✅`)
      if (chatId && messageId) {
        await editTelegramMessage(chatId, messageId, card.text, card.replyMarkup)
      }
      return new Response("OK", { status: 200 })
    }

    if (data.startsWith("apt_step:")) {
      const delta = parseInt(data.replace("apt_step:", ""), 10) || 0
      const doc = await getApartmentDoc()
      const current = typeof doc.progress === "number" ? doc.progress : 75
      const newPct = Math.min(100, Math.max(0, current + delta))
      doc.progress = newPct
      doc.updated_at = new Date().toISOString()
      await saveApartmentDoc(doc, fromId)
      await answerCallbackQuery(cq.id, `Obra ajustada: ${newPct}%! 🏗️`)
      const card = formatApartmentCard(doc)
      if (chatId && messageId) {
        await editTelegramMessage(chatId, messageId, card.text, card.replyMarkup)
      }
      return new Response("OK", { status: 200 })
    }

    if (data.startsWith("apt_set:")) {
      const target = parseInt(data.replace("apt_set:", ""), 10) || 75
      const doc = await getApartmentDoc()
      doc.progress = Math.min(100, Math.max(0, target))
      doc.updated_at = new Date().toISOString()
      await saveApartmentDoc(doc, fromId)
      await answerCallbackQuery(cq.id, `Obra atualizada para ${target}%! 🏗️`)
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

    // Callbacks de Rotinas & Hábitos
    if (data === "menu_routines") {
      await answerCallbackQuery(cq.id, "Carregando hábitos...")
      const doc = await getRoutinesDoc()
      const card = formatRoutinesCard(doc)
      if (chatId && messageId) {
        await editTelegramMessage(chatId, messageId, card.text, card.replyMarkup)
      }
      return new Response("OK", { status: 200 })
    }

    if (data.startsWith("habit_toggle:")) {
      const habitId = data.replace("habit_toggle:", "")
      const { doc, toggledHabit } = await toggleHabitInDoc(habitId)
      const label = toggledHabit ? toggledHabit.name : "Hábito"
      const statusStr = toggledHabit?.is_completed_today ? "concluído! 🔥" : "desmarcado!"
      await answerCallbackQuery(cq.id, `${label} ${statusStr}`)
      const card = formatRoutinesCard(doc)
      if (chatId && messageId) {
        await editTelegramMessage(chatId, messageId, card.text, card.replyMarkup)
      }
      return new Response("OK", { status: 200 })
    }

    // Callbacks de Pets
    if (data === "menu_pets") {
      await answerCallbackQuery(cq.id, "Carregando central pet...")
      const doc = await getPetsDoc()
      const card = formatPetsCard(doc)
      if (chatId && messageId) {
        await editTelegramMessage(chatId, messageId, card.text, card.replyMarkup)
      }
      return new Response("OK", { status: 200 })
    }

    if (data.startsWith("pet_care:")) {
      const action = data.replace("pet_care:", "") as "fed_morning" | "fed_night" | "walked" | "fresh_water"
      const doc = await logPetCareInDoc(action)
      const actionLabels: Record<string, string> = {
        fed_morning: "Ração matinal registrada! 🍖",
        fed_night: "Ração noturna registrada! 🍖",
        walked: "Passeio registrado! 🦮",
        fresh_water: "Água fresca trocada! 💧"
      }
      await answerCallbackQuery(cq.id, actionLabels[action] || "Cuidado com pet registrado! 🐾")
      const card = formatPetsCard(doc)
      if (chatId && messageId) {
        await editTelegramMessage(chatId, messageId, card.text, card.replyMarkup)
      }
      return new Response("OK", { status: 200 })
    }

    // Callbacks de Transporte & Metrô
    if (data === "menu_transport" || data === "transport_refresh") {
      await answerCallbackQuery(cq.id, "Consultando linhas de metrô e trem...")
      const trDoc = await getTransportDoc()
      const lines = await fetchLiveMetroStatus()
      const card = formatTransportCard(lines, trDoc.monitored_metro_lines || [])
      if (chatId && messageId) {
        await editTelegramMessage(chatId, messageId, card.text, card.replyMarkup)
      }
      return new Response("OK", { status: 200 })
    }

    if (data === "transport_monitored") {
      await answerCallbackQuery(cq.id, "Filtrando suas linhas favoritas...")
      const trDoc = await getTransportDoc()
      const lines = await fetchLiveMetroStatus()
      const monCodes = trDoc.monitored_metro_lines || []
      const filtered = lines.filter(l => monCodes.includes(l.codigo))
      const card = formatTransportCard(filtered.length > 0 ? filtered : lines, monCodes)
      if (chatId && messageId) {
        await editTelegramMessage(chatId, messageId, card.text, card.replyMarkup)
      }
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

    // Callback para Voltar ao Menu Principal
    if (data === "cmd_menu") {
      await answerCallbackQuery(cq.id)
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
      const welcomeText = `🤖 <b>Assistente Oficial Tessera</b>\n\n` +
        `Olá, ${userFirstName}! Escolha uma das opções rápidas abaixo ou me envie áudio/texto a qualquer momento:`
      if (chatId && messageId) {
        await editTelegramMessage(chatId, messageId, welcomeText, welcomeMarkup)
      } else if (chatId) {
        await sendTelegramMessage(chatId, welcomeText, welcomeMarkup)
      }
      return new Response("OK", { status: 200 })
    }

    await answerCallbackQuery(cq.id)
    return new Response("OK", { status: 200 })
  } catch (cqErr: any) {
    console.error("Erro no callback query:", cqErr)
    await answerCallbackQuery(cq.id, "Erro ao processar.")
    if (chatId) {
      await sendTelegramMessage(chatId, `⚠️ Erro ao processar o botão: ${cqErr?.message || cqErr}`)
    }
    return new Response("OK", { status: 200 })
  }
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
    const fileId = message.document.file_id
    if (fileId && processedFileIds.has(fileId)) {
      console.log(`Documento PDF ${fileId} já está sendo processado. Ignorando retry do Telegram.`)
      return new Response("OK (already processing)", { status: 200 })
    }
    if (fileId) {
      processedFileIds.add(fileId)
      setTimeout(() => processedFileIds.delete(fileId), 5 * 60 * 1000)
    }

    await sendChatAction(chatId, "typing")
    await sendTelegramMessage(chatId, "📄 <i>Extrato bancário em PDF recebido! Analisando documento com IA especializada em conciliação bancária... Aguarde um instante.</i>")

    const pdfFile = await downloadTelegramFile(message.document.file_id)
    if (!pdfFile) {
      await sendTelegramMessage(chatId, "❌ Não consegui baixar o arquivo PDF. Poderia enviar novamente?")
      return new Response("PDF download failed", { status: 200 })
    }

    try {
      const pdfBase64 = arrayBufferToBase64(pdfFile.buffer)
      const parsedBank = await processBankStatementPdfWithGroq(pdfFile.buffer, pdfBase64)
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
        `• /habitos — Checklist de hábitos diários e streaks\n` +
        `• /pets — Central Petz, alimentação e vacinas\n` +
        `• /metro — Situação em tempo real do Metrô e CPTM\n` +
        `• /saude — Registro de hidratação, peso e hábitos\n` +
        `• /obra — Status e evolução da obra do apartamento\n` +
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
            { text: "🔄 Hábitos", callback_data: "menu_routines" },
            { text: "🐾 Pets", callback_data: "menu_pets" },
            { text: "🚇 Metrô", callback_data: "transport_refresh" }
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

    if (cmd === "/habitos" || cmd === "/rotina" || cmd === "/habito") {
      const doc = await getRoutinesDoc()
      const card = formatRoutinesCard(doc)
      await sendTelegramMessage(chatId, card.text, card.replyMarkup)
      return new Response("OK", { status: 200 })
    }

    if (cmd === "/pets" || cmd === "/pet") {
      const doc = await getPetsDoc()
      const card = formatPetsCard(doc)
      await sendTelegramMessage(chatId, card.text, card.replyMarkup)
      return new Response("OK", { status: 200 })
    }

    if (cmd === "/metro" || cmd === "/transporte" || cmd === "/trem" || cmd === "/cptm") {
      const trDoc = await getTransportDoc()
      const lines = await fetchLiveMetroStatus()
      const card = formatTransportCard(lines, trDoc.monitored_metro_lines || [])
      await sendTelegramMessage(chatId, card.text, card.replyMarkup)
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
    const rawText = (textInput || aiResult.transcription || "").trim()

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

    // Ação: Consultar Lista / Painel de Desejos
    if (aiResult.action === "query_wishes") {
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
        await sendTelegramMessage(chatId, `${transcriptionNote}✨ Sua lista de desejos está em dia! Nenhuma meta pendente.`, emptyMarkup)
        return new Response("OK", { status: 200 })
      }

      let listText = `${transcriptionNote}🎁 <b>Lista de Desejos e Metas (${active.length}):</b>\n\n`
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
      if (message.voice) {
        await maybeSendVoiceReply(chatId, `Você tem ${active.length} item${active.length > 1 ? "s" : ""} na sua lista de desejos.`, true)
      }
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
        const spoken = card.spokenText
        await maybeSendVoiceReply(chatId, spoken, true)
      }
      return new Response("OK", { status: 200 })
    }

    // Ação: Atualizar Obra / Apartamento (Progresso e Portal)
    if (aiResult.action === "update_apartment") {
      const doc = await getApartmentDoc()
      const ap = aiResult.apartment || {}
      let changeMsg = ""

      // Tenta extrair número de porcentagem da fala/texto se a IA não mapeou direto
      let parsedProgress = typeof ap.progress === "number" && !isNaN(ap.progress) ? ap.progress : null
      if (parsedProgress === null) {
        const numMatch = rawText.match(/(\d{1,3})\s*%/i) || rawText.match(/(?:para|em|obra|apê|ape)\s*(\d{1,3})/i)
        if (numMatch) parsedProgress = parseInt(numMatch[1], 10)
      }

      if (parsedProgress !== null) {
        doc.progress = Math.min(100, Math.max(0, parsedProgress))
        changeMsg += `📈 Progresso atualizado para <b>${doc.progress}%</b>.\n`
      }

      // Se o usuário passou link do portal
      const urlMatch = rawText.match(/https?:\/\/[^\s]+/i)
      if (urlMatch) {
        doc.client_portal_url = urlMatch[0]
        changeMsg += `🌐 Link do Portal do Cliente atualizado.\n`
      }

      doc.updated_at = new Date().toISOString()
      await saveApartmentDoc(doc, fromId)
      const card = formatApartmentCard(doc)

      const replyText = `${transcriptionNote}🏗️ <b>Obra do Apê Atualizada!</b>\n\n` +
        (changeMsg ? `${changeMsg}\n` : "") +
        card.text

      await sendTelegramMessage(chatId, replyText, card.replyMarkup)
      if (message.voice) {
        const spoken = `Atualizei o progresso da obra para ${doc.progress} por cento.`
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

    // Ação: Consultar Hábitos & Rotinas
    if (aiResult.action === "get_routines") {
      const doc = await getRoutinesDoc()
      const card = formatRoutinesCard(doc)
      await sendTelegramMessage(chatId, `${transcriptionNote}${card.text}`, card.replyMarkup)
      if (message.voice) {
        await maybeSendVoiceReply(chatId, card.spokenText, true)
      }
      return new Response("OK", { status: 200 })
    }

    // Ação: Concluir ou Alternar Hábito
    if (aiResult.action === "toggle_habit") {
      const hName = aiResult.habit?.name || aiResult.query || "Hábito"
      const { doc, toggledHabit } = await toggleHabitInDoc(hName)
      const label = toggledHabit ? toggledHabit.name : hName
      const statusStr = toggledHabit?.is_completed_today ? `marcado como concluído! Parabéns pelos ${toggledHabit.streak} dias de sequência! 🔥` : "desmarcado."
      const card = formatRoutinesCard(doc)
      const reply = `${transcriptionNote}✅ <b>${label}</b> ${statusStr}\n\n${card.text}`
      await sendTelegramMessage(chatId, reply, card.replyMarkup)
      if (message.voice) {
        await maybeSendVoiceReply(chatId, `Hábito de ${label} ${statusStr}`, true)
      }
      return new Response("OK", { status: 200 })
    }

    // Ação: Consultar Pets
    if (aiResult.action === "get_pets") {
      const doc = await getPetsDoc()
      const card = formatPetsCard(doc)
      await sendTelegramMessage(chatId, `${transcriptionNote}${card.text}`, card.replyMarkup)
      if (message.voice) {
        await maybeSendVoiceReply(chatId, card.spokenText, true)
      }
      return new Response("OK", { status: 200 })
    }

    // Ação: Registrar Cuidado com Pet (Ração, Passeio, Água)
    if (aiResult.action === "log_pet_care") {
      const act = (aiResult.pet?.action || "fed_morning") as "fed_morning" | "fed_night" | "walked" | "fresh_water"
      const doc = await logPetCareInDoc(act)
      const actionLabels: Record<string, string> = {
        fed_morning: "Ração matinal registrada com sucesso! 🍖",
        fed_night: "Ração noturna registrada com sucesso! 🍖",
        walked: "Passeio registrado! Parabéns pela dedicação. 🦮",
        fresh_water: "Água fresca trocada! 💧"
      }
      const card = formatPetsCard(doc)
      const reply = `${transcriptionNote}🐾 <b>${actionLabels[act] || "Cuidado registrado!"}</b>\n\n${card.text}`
      await sendTelegramMessage(chatId, reply, card.replyMarkup)
      if (message.voice) {
        await maybeSendVoiceReply(chatId, actionLabels[act] || "Cuidado com o pet registrado.", true)
      }
      return new Response("OK", { status: 200 })
    }

    // Ação: Consultar Situação do Metrô, Trem e Transporte
    if (aiResult.action === "get_transport") {
      const trDoc = await getTransportDoc()
      const lines = await fetchLiveMetroStatus()
      const card = formatTransportCard(lines, trDoc.monitored_metro_lines || [])
      await sendTelegramMessage(chatId, `${transcriptionNote}${card.text}`, card.replyMarkup)
      if (message.voice) {
        await maybeSendVoiceReply(chatId, card.spokenText, true)
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
