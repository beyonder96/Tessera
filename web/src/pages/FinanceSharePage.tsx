import React, { useEffect, useState } from 'react'
import { supabase } from '../supabaseClient'
import { 
  TrendingUp, 
  TrendingDown, 
  Wallet, 
  Eye, 
  EyeOff, 
  Share2, 
  AlertCircle, 
  RefreshCw, 
  ArrowUpRight, 
  ArrowDownLeft, 
  Home, 
  Plus, 
  X, 
  Clock, 
  CheckCircle2,
  CreditCard,
  Repeat,
  Banknote,
  ChevronRight,
  ChevronLeft,
  Building2,
  Sun,
  Moon,
  Pencil,
  Download
} from 'lucide-react'

interface CategoryBreakdown {
  name: string
  amount: number
  percentage: number
}

interface TransactionItem {
  id: number | string
  title: string
  subtitle?: string
  category: string
  amount: number
  type: string
  date: string | number
  is_recurrent?: boolean
  account_or_card_name?: string
  is_realized?: boolean
  due_date?: string | number
}

export interface DebtItem {
  id: number | string
  title: string
  description?: string
  value: number
  due_date: number
  creditor_name: string
  installments_total: number
  installments_paid: number
}

import { usePwaInstall } from '../hooks/usePwaInstall'
import { PwaInstructionsModal } from '../components/PwaInstructionsModal'
import { saveRecentItem } from '../utils/recentStorage'

export interface DebtsSummary {
  count: number
  total_owed: number
  total_paid: number
  remaining_to_pay: number
  items: DebtItem[]
}

export interface InstallmentItem {
  id: number | string
  title: string
  subtitle?: string
  value: number
  category: string
  account_or_card_name?: string
  date: number
  is_current_month?: boolean
  is_realized?: boolean
}

export interface InstallmentsSummary {
  count: number
  total_month_value: number
  total_value?: number
  all_count?: number
  items: InstallmentItem[]
  all_items?: InstallmentItem[]
  accounts?: BankAccountDoc[]
  cards?: CreditCardDoc[]
}

export interface BankAccountDoc {
  id: number | string
  name: string
  type: string
  balance: number
  color_hex?: string
}

export interface CreditCardDoc {
  id: number | string
  name: string
  type: 'credit' | 'benefit'
  limit: number
  used_limit: number
  available_limit: number
  color_hex?: string
  closing_day?: number
  due_day?: number
}

export interface RecurrentItem {
  id: number | string
  title: string
  subtitle?: string
  value: number
  category: string
  account_or_card_name?: string
  recurrence_interval?: string
}

export interface RecurrentsSummary {
  count: number
  total_monthly_value: number
  items: RecurrentItem[]
}

export interface FinanceSuggestionDoc {
  id: string
  title: string
  amount: number
  type: 'expense' | 'income'
  category: string
  date: string
  created_at: string
  status: 'pending' | 'approved' | 'rejected'
  action?: 'create' | 'edit'
  target_tx_id?: number | string
  original_title?: string
  original_amount?: number
  account_or_card_name?: string
}

interface FinanceDashboardDoc {
  id: string
  title: string
  month_label: string
  total_balance: number
  spendable_balance?: number
  salary_value?: number
  committed_value?: number
  committed_percentage?: number
  monthly_income?: number
  monthly_expense?: number
  categories: CategoryBreakdown[]
  transactions: TransactionItem[]
  debts?: unknown
  installments?: unknown
  recurrents?: unknown
  accounts?: unknown
  cards?: unknown
  suggestions?: FinanceSuggestionDoc[]
  is_live: boolean
  updated_at: string
}

function normalizeDebts(raw: unknown, transactions?: TransactionItem[]): DebtsSummary {
  let debtsList: DebtItem[] = []
  if (Array.isArray(raw) && raw.length > 0) {
    debtsList = raw as DebtItem[]
  } else if (typeof raw === 'object' && raw !== null) {
    const obj = raw as Record<string, unknown>
    if (Array.isArray(obj.items) && obj.items.length > 0) {
      debtsList = obj.items as DebtItem[]
    }
  }

  // Se não houver dívidas registradas no objeto principal, deriva de transações vencidas não pagas
  if (debtsList.length === 0 && Array.isArray(transactions) && transactions.length > 0) {
    const now = Date.now()
    const synthetic = transactions
      .filter(tx => {
        const txDate = typeof tx.date === 'number' ? tx.date : parseInt(String(tx.date), 10) || 0
        const isOverdueUnrealized = tx.is_realized === false && txDate > 0 && txDate < now
        const isDebtByName = tx.title.toLowerCase().includes('dívida') ||
          tx.title.toLowerCase().includes('divida') ||
          tx.title.toLowerCase().includes('empréstimo') ||
          tx.title.toLowerCase().includes('emprestimo') ||
          tx.category.toLowerCase().includes('dívida') ||
          tx.category.toLowerCase().includes('divida')
        return tx.type === 'expense' && (isOverdueUnrealized || isDebtByName)
      })
      .map(tx => {
        const txDate = typeof tx.date === 'number' ? tx.date : parseInt(String(tx.date), 10) || 0
        return {
          id: tx.id,
          title: tx.title,
          description: tx.subtitle || 'Pendência financeira',
          value: Number(tx.amount) || 0,
          due_date: txDate,
          creditor_name: tx.account_or_card_name || 'Credor',
          installments_total: 1,
          installments_paid: tx.is_realized ? 1 : 0
        }
      })
    if (synthetic.length > 0) {
      debtsList = synthetic
    }
  }

  let totalOwed = 0
  let totalPaid = 0
  debtsList.forEach(debt => {
    const val = typeof debt.value === 'number' ? debt.value : parseFloat(String(debt.value)) || 0
    totalOwed += val
    const instTotal = debt.installments_total > 0 ? debt.installments_total : 1
    const instPaid = debt.installments_paid || 0
    totalPaid += (val / instTotal) * instPaid
  })

  return {
    count: debtsList.length,
    total_owed: totalOwed,
    total_paid: totalPaid,
    remaining_to_pay: Math.max(0, totalOwed - totalPaid),
    items: debtsList
  }
}

function normalizeInstallments(raw: unknown, transactions?: TransactionItem[]): InstallmentsSummary {
  let items: InstallmentItem[] = []
  let allItems: InstallmentItem[] = []

  if (raw && !Array.isArray(raw) && typeof raw === 'object') {
    const obj = raw as Record<string, unknown>
    if (Array.isArray(obj.items)) items = obj.items as InstallmentItem[]
    if (Array.isArray(obj.all_items)) allItems = obj.all_items as InstallmentItem[]
  } else if (Array.isArray(raw)) {
    items = raw as InstallmentItem[]
    allItems = items
  }

  // Se ambos estiverem vazios, aciona a heurística abrangente sobre transações locais
  if (items.length === 0 && allItems.length === 0) {
    const derived = (transactions || []).filter(tx => 
      tx.type === 'expense' && (
        (Boolean(tx.subtitle) && (
          tx.subtitle!.toLowerCase().includes('parcela') ||
          tx.subtitle!.toLowerCase().includes('parc.') ||
          tx.subtitle!.toLowerCase().includes('de')
        )) ||
        tx.title.toLowerCase().includes('parcela') ||
        tx.title.toLowerCase().includes('parcelado') ||
        tx.title.toLowerCase().includes('parcelamento') ||
        tx.category.toLowerCase().includes('parcelad') ||
        /\(\d+\/\d+\)/.test(tx.title) ||
        /\b\d+\/\d+\b/.test(tx.title) ||
        /\b\d+x\b/i.test(tx.title) ||
        tx.title.toLowerCase() === 'cartao' ||
        tx.title.toLowerCase() === 'cartão' ||
        Boolean(tx.account_or_card_name && (tx.account_or_card_name.toLowerCase().includes('credito') || tx.account_or_card_name.toLowerCase().includes('crédito')))
      )
    ).map(tx => ({
      id: tx.id,
      title: tx.title,
      subtitle: tx.subtitle || (Boolean(tx.account_or_card_name && tx.account_or_card_name.toLowerCase().includes('credito')) ? 'Cartão de Crédito' : 'Parcela'),
      value: Number(tx.amount) || 0,
      category: tx.category,
      account_or_card_name: tx.account_or_card_name,
      date: typeof tx.date === 'number' ? tx.date : parseInt(String(tx.date), 10) || 0,
      is_current_month: true,
      is_realized: tx.is_realized ?? true
    }))

    items = derived
    allItems = derived
  }

  const resolvedItems = items.length > 0 ? items : allItems.filter(i => i.is_current_month)
  const resolvedAllItems = allItems.length > 0 ? allItems : items

  const total_month_value = resolvedItems.reduce((acc, curr) => acc + (Number(curr.value) || 0), 0)
  const total_value = resolvedAllItems.reduce((acc, curr) => acc + (Number(curr.value) || 0), 0)

  return {
    count: resolvedItems.length,
    total_month_value,
    total_value,
    all_count: resolvedAllItems.length,
    items: resolvedItems,
    all_items: resolvedAllItems
  }
}

function normalizeAccounts(docAccounts: unknown, rawInstallments: unknown, transactions?: TransactionItem[]): BankAccountDoc[] {
  let list: BankAccountDoc[] = []
  // 1. Tenta da raiz doc.accounts
  if (Array.isArray(docAccounts) && docAccounts.length > 0) {
    list = docAccounts.map(item => {
      const obj = (typeof item === 'object' && item !== null ? item : {}) as Record<string, unknown>
      return {
        id: (obj.id as number | string) || String(Math.random()),
        name: String(obj.name || 'Conta'),
        type: String(obj.type || 'Corrente'),
        balance: typeof obj.balance === 'number' ? obj.balance : parseFloat(String(obj.balance)) || 0,
        color_hex: typeof obj.color_hex === 'string' ? obj.color_hex : undefined
      }
    })
  } else if (typeof rawInstallments === 'object' && rawInstallments !== null) {
    // 2. Tenta do fallback em installments.accounts
    const instObj = rawInstallments as Record<string, unknown>
    if (Array.isArray(instObj.accounts) && instObj.accounts.length > 0) {
      list = (instObj.accounts as Record<string, unknown>[]).map(obj => ({
        id: (obj.id as number | string) || String(Math.random()),
        name: String(obj.name || 'Conta'),
        type: String(obj.type || 'Corrente'),
        balance: typeof obj.balance === 'number' ? obj.balance : parseFloat(String(obj.balance)) || 0,
        color_hex: typeof obj.color_hex === 'string' ? obj.color_hex : undefined
      }))
    }
  }

  // 3. Fallback defensivo: se contas estiverem vazias mas há transações identificando conta
  if (list.length === 0 && Array.isArray(transactions) && transactions.length > 0) {
    const names = Array.from(new Set(transactions
      .map(t => t.account_or_card_name?.trim())
      .filter((name): name is string => Boolean(name && !name.toLowerCase().includes('credito') && !name.toLowerCase().includes('crédito') && !name.toLowerCase().includes('cartao') && !name.toLowerCase().includes('cartão')))
    ))
    if (names.length > 0) {
      list = names.map((name, idx) => ({
        id: `acc_derived_${idx}`,
        name,
        type: 'Corrente',
        balance: 0,
        color_hex: '#FF7A00'
      }))
    }
  }

  return list
}

function normalizeCards(docCards: unknown, rawInstallments: unknown, transactions?: TransactionItem[]): CreditCardDoc[] {
  let list: CreditCardDoc[] = []
  // 1. Tenta da raiz doc.cards
  if (Array.isArray(docCards) && docCards.length > 0) {
    list = docCards.map(item => {
      const obj = (typeof item === 'object' && item !== null ? item : {}) as Record<string, unknown>
      const limit = typeof obj.limit === 'number' ? obj.limit : parseFloat(String(obj.limit)) || 0
      const usedLimit = typeof obj.used_limit === 'number' ? obj.used_limit : parseFloat(String(obj.used_limit)) || 0
      const avail = typeof obj.available_limit === 'number' ? obj.available_limit : Math.max(0, limit - usedLimit)
      return {
        id: (obj.id as number | string) || String(Math.random()),
        name: String(obj.name || 'Cartão'),
        type: obj.type === 'benefit' ? 'benefit' : 'credit',
        limit,
        used_limit: usedLimit,
        available_limit: avail,
        color_hex: typeof obj.color_hex === 'string' ? obj.color_hex : undefined,
        closing_day: typeof obj.closing_day === 'number' ? obj.closing_day : undefined,
        due_day: typeof obj.due_day === 'number' ? obj.due_day : undefined
      }
    })
  } else if (typeof rawInstallments === 'object' && rawInstallments !== null) {
    // 2. Tenta do fallback em installments.cards
    const instObj = rawInstallments as Record<string, unknown>
    if (Array.isArray(instObj.cards) && instObj.cards.length > 0) {
      list = (instObj.cards as Record<string, unknown>[]).map(obj => {
        const limit = typeof obj.limit === 'number' ? obj.limit : parseFloat(String(obj.limit)) || 0
        const usedLimit = typeof obj.used_limit === 'number' ? obj.used_limit : parseFloat(String(obj.used_limit)) || 0
        const avail = typeof obj.available_limit === 'number' ? obj.available_limit : Math.max(0, limit - usedLimit)
        return {
          id: (obj.id as number | string) || String(Math.random()),
          name: String(obj.name || 'Cartão'),
          type: obj.type === 'benefit' ? 'benefit' : 'credit',
          limit,
          used_limit: usedLimit,
          available_limit: avail,
          color_hex: typeof obj.color_hex === 'string' ? obj.color_hex : undefined,
          closing_day: typeof obj.closing_day === 'number' ? obj.closing_day : undefined,
          due_day: typeof obj.due_day === 'number' ? obj.due_day : undefined
        }
      })
    }
  }

  // 3. Fallback defensivo se não houver cartões mas houver transações mencionando cartão
  if (list.length === 0 && Array.isArray(transactions) && transactions.length > 0) {
    const cardNames = Array.from(new Set(transactions
      .map(t => t.account_or_card_name?.trim())
      .filter((name): name is string => Boolean(name && (name.toLowerCase().includes('credito') || name.toLowerCase().includes('crédito') || name.toLowerCase().includes('cartao') || name.toLowerCase().includes('cartão'))))
    ))
    if (cardNames.length > 0) {
      list = cardNames.map((name, idx) => ({
        id: `card_derived_${idx}`,
        name,
        type: 'credit',
        limit: 1000,
        used_limit: 0,
        available_limit: 1000,
        color_hex: '#4A90E2'
      }))
    }
  }

  // 4. Se for cartão de crédito, ajusta a fatura usada conforme as despesas registradas
  return list.map(card => {
    if (card.type === 'credit') {
      const cardExpenses = (transactions || [])
        .filter(t => t.type === 'expense' && (
          t.account_or_card_name?.toLowerCase() === card.name.toLowerCase() ||
          t.title.toLowerCase().includes(card.name.toLowerCase()) ||
          t.title.toLowerCase() === 'cartao' ||
          t.title.toLowerCase() === 'cartão'
        ))
        .reduce((acc, t) => acc + t.amount, 0)
      const effectiveUsed = Math.max(card.used_limit, cardExpenses)
      const effectiveAvail = Math.max(0, card.limit - effectiveUsed)
      return {
        ...card,
        used_limit: effectiveUsed,
        available_limit: effectiveAvail
      }
    }
    return card
  })
}

function normalizeRecurrents(raw: unknown, transactions?: TransactionItem[]): RecurrentsSummary {
  if (raw && !Array.isArray(raw) && typeof raw === 'object') {
    const obj = raw as Record<string, unknown>
    const items = Array.isArray(obj.items) ? (obj.items as RecurrentItem[]) : []
    if (items.length > 0) {
      const count = typeof obj.count === 'number' ? obj.count : items.length
      const total_monthly_value = typeof obj.total_monthly_value === 'number'
        ? obj.total_monthly_value
        : items.reduce((acc, curr) => acc + (Number(curr.value) || 0), 0)
      return { count, total_monthly_value, items }
    }
    if (items.length === 0 && (!transactions || transactions.length === 0)) {
      return { count: 0, total_monthly_value: 0, items: [] }
    }
  }

  if (Array.isArray(raw) && raw.length > 0) {
    const items = raw as RecurrentItem[]
    const total = items.reduce((acc, curr) => acc + (Number(curr.value) || 0), 0)
    return { count: items.length, total_monthly_value: total, items }
  }

  const derived = (transactions || []).filter(tx => 
    tx.type === 'expense' && Boolean(tx.is_recurrent)
  ).map(tx => ({
    id: tx.id,
    title: tx.title,
    subtitle: tx.subtitle,
    value: Number(tx.amount) || 0,
    category: tx.category,
    account_or_card_name: tx.account_or_card_name
  }))

  return {
    count: derived.length,
    total_monthly_value: derived.reduce((acc, curr) => acc + curr.value, 0),
    items: derived
  }
}

export const FinanceSharePage: React.FC<{ dashboardId: string }> = ({ dashboardId }) => {
  const [doc, setDoc] = useState<FinanceDashboardDoc | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [isPrivacyMode, setIsPrivacyMode] = useState(false)
  const [copied, setCopied] = useState(false)

  // Tema Claro / Escuro com persistência
  const [theme, setTheme] = useState<'dark' | 'light'>(() => {
    const saved = typeof window !== 'undefined' ? localStorage.getItem('tessera_theme') : null
    if (saved === 'light' || saved === 'dark') return saved
    return typeof window !== 'undefined' && window.matchMedia && window.matchMedia('(prefers-color-scheme: light)').matches ? 'light' : 'dark'
  })

  useEffect(() => {
    document.documentElement.setAttribute('data-theme', theme)
    try {
      localStorage.setItem('tessera_theme', theme)
    } catch {
      // ignore
    }
  }, [theme])

  const toggleTheme = () => {
    setTheme(prev => (prev === 'dark' ? 'light' : 'dark'))
  }

  // PWA Install state via hook resiliente
  const { isInstalled, installApp, showHelpModal, setShowHelpModal, isIos } = usePwaInstall()

  // Seleção de filtro (Conta ou Cartão clicado, exatamente como no app principal)
  const [selectedFilter, setSelectedFilter] = useState<{
    type: 'account' | 'card'
    name: string
    balance?: number
    limit?: number
    usedLimit?: number
    cardType?: 'credit' | 'benefit'
    accountType?: string
  } | null>(null)

  // Modo de visualização no modal de parcelados ('month' = deste mês | 'all' = todas)
  const [installmentViewMode, setInstallmentViewMode] = useState<'month' | 'all'>('month')

  // Suggestion Modal state
  const [isModalOpen, setIsModalOpen] = useState(false)
  const [suggestionType, setSuggestionType] = useState<'expense' | 'income'>('expense')
  const [suggestionTitle, setSuggestionTitle] = useState('')
  const [suggestionAmount, setSuggestionAmount] = useState('')
  const [suggestionCategory, setSuggestionCategory] = useState('Alimentação')
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [successMessage, setSuccessMessage] = useState<string | null>(null)

  // Edit Transaction Modal state
  const [editingTx, setEditingTx] = useState<TransactionItem | null>(null)
  const [editTitle, setEditTitle] = useState('')
  const [editAmount, setEditAmount] = useState('')
  const [editType, setEditType] = useState<'expense' | 'income'>('expense')
  const [editCategory, setEditCategory] = useState('Outros')
  const [editAccountOrCard, setEditAccountOrCard] = useState('')
  const [isSubmittingEdit, setIsSubmittingEdit] = useState(false)

  // Summary Panels state (Carrossel dos 3 painéis: Dívidas | Parcelados | Contas Fixas)
  const [activePanelIndex, setActivePanelIndex] = useState(0)
  const [selectedPanelModal, setSelectedPanelModal] = useState<'debts' | 'installments' | 'recurrents' | null>(null)

  useEffect(() => {
    async function loadDashboard(isInitial = false) {
      if (isInitial) {
        setLoading(true)
        setError(null)
      }
      try {
        const { data, error: sbError } = await supabase
          .from('shared_finance_dashboards')
          .select('*')
          .eq('id', dashboardId)
          .single()

        if (sbError) {
          throw sbError
        }

        if (data) {
          const finDoc = data as FinanceDashboardDoc
          setDoc(finDoc)
          localStorage.setItem(`tessera_finance_${dashboardId}`, JSON.stringify(finDoc))
          saveRecentItem({ type: 'finance', id: dashboardId, title: finDoc.title || 'Resumo Financeiro' })
        }
      } catch (err: unknown) {
        console.error('Error fetching finance dashboard:', err)
        const cached = localStorage.getItem(`tessera_finance_${dashboardId}`)
        if (cached && isInitial) {
          try {
            const cachedDoc = JSON.parse(cached) as FinanceDashboardDoc
            setDoc(cachedDoc)
            saveRecentItem({ type: 'finance', id: dashboardId, title: cachedDoc.title || 'Resumo Financeiro' })
            setError(null)
            setLoading(false)
            return
          } catch {
            // ignore
          }
        }
        if (isInitial) {
          const message = err instanceof Error ? err.message : 'Resumo financeiro não encontrado ou link expirado.'
          setError(message)
        }
      } finally {
        if (isInitial) {
          setLoading(false)
        }
      }
    }

    loadDashboard(true)

    // Listener para quando o usuário volta para a aba do navegador
    const handleFocus = () => loadDashboard(false)
    const handleVisibility = () => {
      if (document.visibilityState === 'visible') {
        loadDashboard(false)
      }
    }

    window.addEventListener('focus', handleFocus)
    document.addEventListener('visibilitychange', handleVisibility)

    // Polling a cada 4 segundos como garantia extra
    const pollInterval = setInterval(() => {
      if (document.visibilityState === 'visible') {
        loadDashboard(false)
      }
    }, 4000)

    // Realtime Subscription
    const channel = supabase
      .channel(`finance-dashboard-${dashboardId}`)
      .on(
        'postgres_changes',
        {
          event: '*',
          schema: 'public',
          table: 'shared_finance_dashboards',
          filter: `id=eq.${dashboardId}`,
        },
        (payload) => {
          if (payload.new && typeof payload.new === 'object') {
            const newDoc = payload.new as unknown as FinanceDashboardDoc
            setDoc(newDoc)
            localStorage.setItem(`tessera_finance_${dashboardId}`, JSON.stringify(newDoc))
            saveRecentItem({ type: 'finance', id: dashboardId, title: newDoc.title || 'Resumo Financeiro' })
          }
        }
      )
      .subscribe()

    return () => {
      window.removeEventListener('focus', handleFocus)
      document.removeEventListener('visibilitychange', handleVisibility)
      clearInterval(pollInterval)
      supabase.removeChannel(channel)
    }
  }, [dashboardId])

  const formatCurrency = (val: unknown) => {
    if (isPrivacyMode) return 'R$ •••••'
    const num = typeof val === 'number' ? val : parseFloat(String(val))
    const safeNum = isNaN(num) ? 0 : num
    return new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(safeNum)
  }

  const formatDate = (rawDate: unknown): string => {
    if (!rawDate) return ''
    try {
      if (typeof rawDate === 'number' || (!isNaN(Number(rawDate)) && String(rawDate).length > 8)) {
        const d = new Date(Number(rawDate))
        return d.toLocaleDateString('pt-BR', { day: '2-digit', month: '2-digit' })
      }
      return String(rawDate)
    } catch {
      return String(rawDate)
    }
  }

  const handleShare = () => {
    if (navigator.clipboard) {
      navigator.clipboard.writeText(window.location.href)
      setCopied(true)
      setTimeout(() => setCopied(false), 2000)
    }
  }

  // Handle submitting a suggestion from Web
  const handleSuggestTransaction = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!doc || !suggestionTitle.trim()) return

    const amount = parseFloat(suggestionAmount.replace(',', '.'))
    if (!amount || amount <= 0) return

    setIsSubmitting(true)

    const newSuggestion: FinanceSuggestionDoc = {
      id: `sug_${Date.now()}`,
      title: suggestionTitle.trim(),
      amount,
      type: suggestionType,
      category: suggestionCategory,
      date: new Date().toLocaleDateString('pt-BR', { day: '2-digit', month: '2-digit' }),
      created_at: new Date().toISOString(),
      status: 'pending',
    }

    const currentSuggestions = doc.suggestions || []
    const updatedSuggestions = [newSuggestion, ...currentSuggestions]
    const updatedDoc: FinanceDashboardDoc = { ...doc, suggestions: updatedSuggestions, updated_at: new Date().toISOString() }

    // Optimistic local update
    setDoc(updatedDoc)
    localStorage.setItem(`tessera_finance_${dashboardId}`, JSON.stringify(updatedDoc))

    try {
      await supabase
        .from('shared_finance_dashboards')
        .update({ suggestions: updatedSuggestions, updated_at: new Date().toISOString() })
        .eq('id', dashboardId)

      setSuccessMessage(`Sugestão de ${new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(amount)} enviada! Ela aparecerá no aplicativo para aprovação.`)
      setSuggestionTitle('')
      setSuggestionAmount('')
      setIsModalOpen(false)
      setTimeout(() => setSuccessMessage(null), 5000)
    } catch (err: unknown) {
      console.error('Failed to submit suggestion:', err)
    } finally {
      setIsSubmitting(false)
    }
  }

  const handleOpenEditModal = (tx: TransactionItem) => {
    setEditingTx(tx)
    setEditTitle(tx.title)
    setEditAmount(String(tx.amount))
    setEditType(tx.type?.toLowerCase() === 'income' ? 'income' : 'expense')
    setEditCategory(tx.category || 'Outros')
    setEditAccountOrCard(tx.account_or_card_name || '')
  }

  const handleSuggestEditTransaction = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!doc || !editingTx || !editTitle.trim()) return

    const amount = parseFloat(editAmount.replace(',', '.'))
    if (!amount || amount <= 0) return

    setIsSubmittingEdit(true)

    const newSuggestion: FinanceSuggestionDoc = {
      id: `sug_edit_${Date.now()}`,
      title: editTitle.trim(),
      amount,
      type: editType,
      category: editCategory,
      date: new Date().toLocaleDateString('pt-BR', { day: '2-digit', month: '2-digit' }),
      created_at: new Date().toISOString(),
      status: 'pending',
      action: 'edit',
      target_tx_id: editingTx.id,
      original_title: editingTx.title,
      original_amount: Number(editingTx.amount) || 0,
      account_or_card_name: editAccountOrCard || editingTx.account_or_card_name
    }

    const currentSuggestions = doc.suggestions || []
    const updatedSuggestions = [newSuggestion, ...currentSuggestions]
    const updatedDoc: FinanceDashboardDoc = { ...doc, suggestions: updatedSuggestions, updated_at: new Date().toISOString() }

    setDoc(updatedDoc)
    localStorage.setItem(`tessera_finance_${dashboardId}`, JSON.stringify(updatedDoc))

    try {
      await supabase
        .from('shared_finance_dashboards')
        .update({ suggestions: updatedSuggestions, updated_at: new Date().toISOString() })
        .eq('id', dashboardId)

      setSuccessMessage(`Edição de "${editingTx.title}" enviada! A alteração será aplicada no Tessera assim que for aprovada no app.`)
      setEditingTx(null)
      setTimeout(() => setSuccessMessage(null), 6000)
    } catch (err: unknown) {
      console.error('Failed to submit edit suggestion:', err)
    } finally {
      setIsSubmittingEdit(false)
    }
  }

  if (loading) {
    return (
      <div className="container" style={{ paddingTop: 40 }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 24 }}>
          <div className="skeleton" style={{ width: 160, height: 28 }} />
          <div className="skeleton" style={{ width: 90, height: 24, borderRadius: 999 }} />
        </div>
        <div className="skeleton" style={{ width: '100%', height: 160, borderRadius: 20, marginBottom: 16 }} />
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12, marginBottom: 24 }}>
          <div className="skeleton" style={{ height: 90, borderRadius: 16 }} />
          <div className="skeleton" style={{ height: 90, borderRadius: 16 }} />
        </div>
        <div className="skeleton" style={{ width: '100%', height: 200, borderRadius: 20 }} />
      </div>
    )
  }

  if (error || !doc) {
    return (
      <div className="container" style={{ textAlign: 'center', paddingTop: 80 }}>
        <div className="card" style={{ maxWidth: 460, margin: '0 auto', display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 16 }}>
          <div style={{ width: 48, height: 48, borderRadius: '50%', background: 'var(--danger-subtle)', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
            <AlertCircle size={24} color="var(--danger)" />
          </div>
          <h2 style={{ fontSize: 18, fontWeight: 600 }}>Resumo indisponível</h2>
          <p style={{ fontSize: 13, color: 'var(--text-secondary)', lineHeight: 1.5 }}>
            O painel financeiro (<code>{dashboardId}</code>) não foi localizado no servidor.
          </p>
          <div style={{ background: 'var(--bg-surface)', padding: '12px 16px', borderRadius: 'var(--radius-sm)', width: '100%', textAlign: 'left', fontSize: 12, color: 'var(--text-muted)' }}>
            💡 <strong>Dica:</strong> Abra o aplicativo Tessera no celular, acesse a aba <em>Finanças</em> e toque em <em>Compartilhar / Gerar Novo Link</em> para atualizar os dados compartilhados.
          </div>
          <div style={{ display: 'flex', gap: 10, marginTop: 8 }}>
            <button className="btn btn-outline" onClick={() => window.location.reload()}>
              <RefreshCw size={14} /> Tentar novamente
            </button>
            <button 
              className="btn btn-outline" 
              onClick={() => {
                sessionStorage.setItem('tessera_skip_autoredirect', 'true')
                window.location.href = '/?home=true'
              }}
            >
              <Home size={14} /> Início
            </button>
          </div>
        </div>
      </div>
    )
  }

  const parseNum = (val: unknown, fallback: number = 0): number => {
    if (val === undefined || val === null) return fallback
    const n = typeof val === 'number' ? val : parseFloat(String(val))
    return isNaN(n) ? fallback : n
  }

  const salaryValue = parseNum(doc.salary_value, parseNum(doc.monthly_income, 0))
  const committedValue = parseNum(doc.committed_value, parseNum(doc.monthly_expense, 0))
  const spendableValue = parseNum(doc.spendable_balance, salaryValue - committedValue)
  const committedPercent = parseNum(doc.committed_percentage, salaryValue > 0 ? (committedValue / salaryValue) * 100 : 0)
  const isSpendableNegative = spendableValue < 0
  const pendingSuggestions = (doc.suggestions || []).filter(s => s.status === 'pending')

  // Dados dos Painéis (Dívidas, Parcelados e Contas Fixas) normalizados com resiliência
  const debtsData = normalizeDebts(doc.debts, doc.transactions)
  const installmentsData = normalizeInstallments(doc.installments, doc.transactions)
  const recurrentsData = normalizeRecurrents(doc.recurrents, doc.transactions)
  const accountsData = normalizeAccounts(doc.accounts, doc.installments, doc.transactions)
  const cardsData = normalizeCards(doc.cards, doc.installments, doc.transactions)

  const totalInstallmentsCount = installmentsData.all_count || installmentsData.all_items?.length || installmentsData.count || installmentsData.items.length

  const panels = [
    {
      key: 'debts' as const,
      tabLabel: 'Dívidas',
      title: 'Painel de Dívidas',
      subtitle: debtsData.count > 0 
        ? `${debtsData.count} dívidas ativas • ${formatCurrency(debtsData.remaining_to_pay)} a pagar` 
        : 'Acompanhe tudo que deve e precisa pagar',
      badge: debtsData.count > 0 ? `${debtsData.count} ativas` : 'Em dia',
      icon: Banknote,
      color: '#EF4444',
      bgBadge: 'rgba(239, 68, 68, 0.12)',
      borderActive: 'rgba(239, 68, 68, 0.35)',
      onClick: () => setSelectedPanelModal('debts')
    },
    {
      key: 'installments' as const,
      tabLabel: 'Parcelados',
      title: 'Painel de Parcelados',
      subtitle: installmentsData.count > 0
        ? `${installmentsData.count} parcelas no mês (${formatCurrency(installmentsData.total_month_value)})`
        : totalInstallmentsCount > 0
          ? `${totalInstallmentsCount} parcelas registradas`
          : 'Nenhuma compra parcelada registrada',
      badge: `${installmentsData.count > 0 ? `${installmentsData.count} no mês` : `${totalInstallmentsCount} ativas`}`,
      icon: CreditCard,
      color: '#4A90E2',
      bgBadge: 'rgba(74, 144, 226, 0.12)',
      borderActive: 'rgba(74, 144, 226, 0.35)',
      onClick: () => setSelectedPanelModal('installments')
    },
    {
      key: 'recurrents' as const,
      tabLabel: 'Contas Fixas',
      title: 'Painel de Contas Fixas',
      subtitle: recurrentsData.count > 0
        ? `${recurrentsData.count} contas fixas (${formatCurrency(recurrentsData.total_monthly_value)})`
        : 'Nenhum custo fixo recorrente cadastrado',
      badge: `${recurrentsData.count} fixas`,
      icon: Repeat,
      color: 'var(--accent)',
      bgBadge: 'var(--accent-subtle)',
      borderActive: 'var(--border-active)',
      onClick: () => setSelectedPanelModal('recurrents')
    }
  ]

  const currentPanel = panels[activePanelIndex] || panels[0]

  return (
    <div className="container">
      {/* Success Notification */}
      {successMessage && (
        <div style={{
          background: 'var(--accent-subtle)',
          border: '1px solid var(--border-active)',
          borderRadius: 'var(--radius-md)',
          padding: '12px 16px',
          marginBottom: 16,
          display: 'flex',
          alignItems: 'center',
          gap: 10,
          fontSize: 13,
          color: 'var(--text-primary)'
        }}>
          <CheckCircle2 size={18} color="var(--accent)" />
          <span>{successMessage}</span>
        </div>
      )}

      {/* Header */}
      <div className="animate-fade-in-up" style={{ marginBottom: 20 }}>
        {/* Top bar com Badge e Botões de Controle */}
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 10 }}>
          <div className="live-badge">
            <div className="live-dot" />
            Ao Vivo
          </div>

          <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
            {!isInstalled && (
              <button 
                type="button"
                className="btn btn-outline" 
                onClick={installApp}
                title="Instalar como aplicativo no celular ou desktop"
                style={{ padding: '6px 10px', fontSize: 11, height: 36, borderRadius: 'var(--radius-full)', gap: 6 }}
              >
                <Download size={14} color="var(--accent)" />
                <span>Instalar App</span>
              </button>
            )}
            <button 
              type="button"
              className="theme-toggle-btn"
              onClick={toggleTheme}
              title={theme === 'dark' ? 'Mudar para tema claro' : 'Mudar para tema escuro'}
              aria-label="Alternar tema claro/escuro"
            >
              {theme === 'dark' ? (
                <Sun key="sun" size={17} color="#F59E0B" className="theme-icon-enter" />
              ) : (
                <Moon key="moon" size={17} color="#4A90E2" className="theme-icon-enter" />
              )}
            </button>
            <button 
              className="btn btn-outline" 
              onClick={() => setIsPrivacyMode(!isPrivacyMode)}
              style={{ width: 36, height: 36, padding: 0, display: 'flex', alignItems: 'center', justifyContent: 'center', borderRadius: 'var(--radius-full)' }}
              title={isPrivacyMode ? 'Mostrar valores' : 'Ocultar valores'}
              aria-label="Alternar privacidade"
            >
              {isPrivacyMode ? <EyeOff size={16} /> : <Eye size={16} />}
            </button>
            <button 
              type="button"
              className="btn btn-outline" 
              onClick={() => {
                sessionStorage.setItem('tessera_skip_autoredirect', 'true')
                window.location.href = '/?home=true'
              }}
              style={{ width: 36, height: 36, padding: 0, display: 'flex', alignItems: 'center', justifyContent: 'center', borderRadius: 'var(--radius-full)' }}
              title="Início / Acessos recentes"
              aria-label="Página inicial"
            >
              <Home size={16} />
            </button>
            <button 
              className="btn btn-outline" 
              onClick={handleShare} 
              style={{ width: 36, height: 36, padding: 0, display: 'flex', alignItems: 'center', justifyContent: 'center', borderRadius: 'var(--radius-full)' }}
              title={copied ? 'Link copiado!' : 'Copiar link'}
              aria-label="Compartilhar"
            >
              {copied ? <CheckCircle2 size={16} color="var(--accent)" /> : <Share2 size={16} />}
            </button>
          </div>
        </div>

        {/* Título e Subtítulo sem quebras forçadas */}
        <div>
          <h1 className="header-title" style={{ fontSize: 24, fontWeight: 600, letterSpacing: -0.5, color: 'var(--text-primary)', marginBottom: 4 }}>
            {doc.title || 'Resumo Financeiro'}
          </h1>
          <p style={{ fontSize: 13, color: 'var(--text-secondary)' }}>
            {doc.month_label} • Atualizado em tempo real
          </p>
        </div>

        {/* Botão de Sugerir Lançamento */}
        <button 
          className="btn btn-primary"
          onClick={() => setIsModalOpen(true)}
          style={{ width: '100%', marginTop: 14, height: 42 }}
        >
          <Plus size={16} /> Sugerir Lançamento
        </button>
      </div>

      {/* 1. HERO CARD: DISPONÍVEL PARA GASTAR / LIMITE OU SALDO ATUAL */}
      <div className="card animate-fade-in-up" style={{ marginBottom: 16, padding: '24px 20px', background: 'var(--bg-card)', animationDelay: '50ms' }}>
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 10 }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
            {selectedFilter ? (
              selectedFilter.type === 'card' ? <CreditCard size={16} color="#4A90E2" /> : <Building2 size={16} color="var(--accent)" />
            ) : (
              <Wallet size={16} color="var(--accent)" />
            )}
            <span style={{ 
              fontSize: 11, 
              fontWeight: 600, 
              letterSpacing: 1.2, 
              color: selectedFilter?.type === 'card' ? '#4A90E2' : 'var(--accent)', 
              textTransform: 'uppercase' 
            }}>
              {selectedFilter 
                ? (selectedFilter.type === 'card' ? `FATURA / LIMITE • ${selectedFilter.name}` : `SALDO ATUAL • ${selectedFilter.name}`)
                : 'Disponível para Gastar'}
            </span>
          </div>
          {selectedFilter ? (
            <button
              onClick={() => setSelectedFilter(null)}
              style={{
                background: 'var(--bg-surface)',
                border: '1px solid var(--border)',
                color: 'var(--text-secondary)',
                fontSize: 11,
                cursor: 'pointer',
                display: 'flex',
                alignItems: 'center',
                gap: 4,
                padding: '3px 8px',
                borderRadius: 'var(--radius-full)',
                transition: 'all 150ms ease-out'
              }}
              title="Limpar seleção e voltar ao Disponível para Gastar"
            >
              <X size={12} /> Limpar
            </button>
          ) : (
            <span style={{ fontSize: 11, fontWeight: 500, color: 'var(--text-muted)' }}>Livre no mês</span>
          )}
        </div>

        <div style={{ 
          fontSize: 36, 
          fontWeight: 600, 
          letterSpacing: -0.8, 
          color: (selectedFilter ? false : isSpendableNegative) && !isPrivacyMode ? 'var(--danger)' : 'var(--text-primary)',
          marginBottom: 16,
          fontVariantNumeric: 'tabular-nums'
        }}>
          {selectedFilter ? (
            selectedFilter.type === 'card'
              ? formatCurrency(selectedFilter.usedLimit || 0)
              : formatCurrency(selectedFilter.balance || 0)
          ) : (
            formatCurrency(spendableValue)
          )}
        </div>

        {/* Detalhes do Hero Card */}
        {selectedFilter ? (
          selectedFilter.type === 'card' ? (
            <div>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', fontSize: 12, marginBottom: 6 }}>
                <span style={{ color: 'var(--text-secondary)', fontWeight: 500 }}>
                  Disponível: <strong style={{ color: 'var(--text-primary)' }}>{formatCurrency(Math.max(0, (selectedFilter.limit || 0) - (selectedFilter.usedLimit || 0)))}</strong>
                </span>
                <span style={{ color: 'var(--text-muted)', fontSize: 11 }}>
                  Total: {formatCurrency(selectedFilter.limit || 0)}
                </span>
              </div>
              <div style={{ width: '100%', height: 6, background: 'var(--bg-surface-hover)', borderRadius: 999, overflow: 'hidden' }}>
                <div 
                  style={{ 
                    width: `${Math.min(100, Math.max(0, ((selectedFilter.usedLimit || 0) / (selectedFilter.limit || 1)) * 100))}%`, 
                    height: '100%', 
                    background: ((selectedFilter.usedLimit || 0) / (selectedFilter.limit || 1)) > 0.9 ? 'var(--danger)' : '#4A90E2', 
                    borderRadius: 999,
                    transition: 'width 300ms ease-out' 
                  }} 
                />
              </div>
            </div>
          ) : (
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', fontSize: 12, color: 'var(--text-secondary)' }}>
              <span>Tipo de conta: <strong style={{ color: 'var(--text-primary)' }}>{selectedFilter.accountType || 'Corrente'}</strong></span>
              <span style={{ color: 'var(--accent)', fontSize: 11, fontWeight: 500 }}>Conta Ativa</span>
            </div>
          )
        ) : (
          <div>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', fontSize: 12, marginBottom: 6 }}>
              <span style={{ color: 'var(--text-secondary)', fontWeight: 500 }}>Orçamento Comprometido</span>
              <span style={{ fontWeight: 600, color: 'var(--text-primary)', fontVariantNumeric: 'tabular-nums' }}>
                {isPrivacyMode ? '•••' : `${Math.round(committedPercent)}%`}
              </span>
            </div>
            <div style={{ width: '100%', height: 6, background: 'var(--bg-surface-hover)', borderRadius: 999, overflow: 'hidden' }}>
              <div 
                className="animate-progress"
                style={{ 
                  width: `${Math.min(committedPercent, 100)}%`, 
                  height: '100%', 
                  background: committedPercent > 90 ? 'var(--danger)' : 'var(--accent)', 
                  borderRadius: 999,
                  transition: 'width 300ms ease-out' 
                }} 
              />
            </div>
          </div>
        )}
      </div>

      {/* 2. CARROSSEL DOS PAINÉIS (Dívidas | Parcelados | Contas Fixas) */}
      <div className="animate-fade-in-up" style={{ marginBottom: 16, animationDelay: '100ms' }}>
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 10, padding: '0 2px' }}>
          <span style={{ fontSize: 11, fontWeight: 600, letterSpacing: 1.2, color: 'var(--text-muted)', textTransform: 'uppercase' }}>
            Compromissos & Painéis
          </span>
          <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
            <button
              onClick={() => setActivePanelIndex((prev) => (prev > 0 ? prev - 1 : panels.length - 1))}
              className="btn btn-outline"
              style={{ width: 28, height: 28, padding: 0, display: 'flex', alignItems: 'center', justifyContent: 'center', borderRadius: '50%' }}
              aria-label="Painel anterior"
              title="Painel anterior"
            >
              <ChevronLeft size={14} />
            </button>
            <button
              onClick={() => setActivePanelIndex((prev) => (prev < panels.length - 1 ? prev + 1 : 0))}
              className="btn btn-outline"
              style={{ width: 28, height: 28, padding: 0, display: 'flex', alignItems: 'center', justifyContent: 'center', borderRadius: '50%' }}
              aria-label="Próximo painel"
              title="Próximo painel"
            >
              <ChevronRight size={14} />
            </button>
          </div>
        </div>

        {/* Tabs Rápidas dos 3 Painéis */}
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: 8, marginBottom: 10 }}>
          {panels.map((p, idx) => {
            const isActive = activePanelIndex === idx
            return (
              <button
                key={p.key}
                onClick={() => setActivePanelIndex(idx)}
                style={{
                  padding: '8px 4px',
                  borderRadius: 'var(--radius-sm)',
                  border: isActive ? `1px solid ${p.borderActive}` : '1px solid var(--border)',
                  background: isActive ? p.bgBadge : 'var(--bg-surface)',
                  color: isActive ? p.color : 'var(--text-secondary)',
                  fontSize: 11,
                  fontWeight: 600,
                  cursor: 'pointer',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  gap: 6,
                  transition: 'all 180ms ease-out'
                }}
              >
                <p.icon size={13} color={isActive ? p.color : 'currentColor'} />
                <span>{p.tabLabel}</span>
              </button>
            )
          })}
        </div>

        {/* Card do Painel Ativo */}
        <div 
          className="card interactive-card animate-fade-in-up stagger-1"
          onClick={currentPanel.onClick}
          style={{
            padding: '18px 20px',
            background: 'var(--bg-card)',
            border: '1px solid var(--border)',
            cursor: 'pointer',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            gap: 14
          }}
          onMouseEnter={(e) => {
            e.currentTarget.style.borderColor = currentPanel.borderActive
            e.currentTarget.style.transform = 'translateY(-1px)'
          }}
          onMouseLeave={(e) => {
            e.currentTarget.style.borderColor = 'var(--border)'
            e.currentTarget.style.transform = 'translateY(0)'
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: 14, minWidth: 0 }}>
            <div style={{
              width: 40,
              height: 40,
              borderRadius: '50%',
              background: currentPanel.bgBadge,
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              flexShrink: 0
            }}>
              <currentPanel.icon size={20} color={currentPanel.color} />
            </div>
            <div style={{ minWidth: 0 }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                <span style={{ fontSize: 14, fontWeight: 600, color: 'var(--text-primary)' }}>
                  {currentPanel.title}
                </span>
                <span style={{
                  fontSize: 10,
                  fontWeight: 600,
                  padding: '2px 8px',
                  borderRadius: 'var(--radius-full)',
                  background: currentPanel.bgBadge,
                  color: currentPanel.color
                }}>
                  {currentPanel.badge}
                </span>
              </div>
              <p style={{
                fontSize: 12,
                color: 'var(--text-secondary)',
                marginTop: 2,
                overflow: 'hidden',
                textOverflow: 'ellipsis',
                whiteSpace: 'nowrap'
              }}>
                {currentPanel.subtitle}
              </p>
            </div>
          </div>
          <ChevronRight size={18} color="var(--text-muted)" style={{ flexShrink: 0 }} />
        </div>

        {/* Indicadores de Página (3 Dots) */}
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 8, marginTop: 12 }}>
          {panels.map((p, idx) => (
            <button
              key={p.key}
              onClick={() => setActivePanelIndex(idx)}
              aria-label={`Ir para ${p.title}`}
              style={{
                width: activePanelIndex === idx ? 20 : 6,
                height: 6,
                borderRadius: 'var(--radius-full)',
                background: activePanelIndex === idx ? 'var(--accent)' : 'var(--border)',
                border: 'none',
                cursor: 'pointer',
                transition: 'all 180ms ease-out',
                padding: 0
              }}
            />
          ))}
        </div>
      </div>

      {/* 3. SEUS CARTÕES (Fatura e Limites) */}
      <div style={{ marginBottom: 20 }}>
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 10, padding: '0 2px' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
            <span style={{ fontSize: 11, fontWeight: 600, letterSpacing: 1.2, color: 'var(--text-muted)', textTransform: 'uppercase' }}>
              Seus Cartões
            </span>
            {cardsData.length > 0 && (
              <span style={{ fontSize: 10, fontWeight: 600, padding: '1px 6px', borderRadius: 'var(--radius-full)', background: 'rgba(74, 144, 226, 0.12)', color: '#4A90E2' }}>
                {cardsData.length}
              </span>
            )}
          </div>
          {cardsData.length > 0 && (
            <span style={{ fontSize: 11, color: 'var(--text-muted)' }}>
              Toque para filtrar saldo
            </span>
          )}
        </div>

        {cardsData.length === 0 ? (
          <div className="card" style={{ padding: '16px 20px', textAlign: 'center', color: 'var(--text-muted)', fontSize: 12 }}>
            Nenhum cartão cadastrado no aplicativo.
          </div>
        ) : (
          <div style={{ display: 'grid', gridTemplateColumns: cardsData.length === 1 ? '1fr' : 'repeat(auto-fit, minmax(260px, 1fr))', gap: 12 }}>
            {cardsData.map((card) => {
              const isSelected = selectedFilter?.type === 'card' && selectedFilter.name === card.name
              const isBenefit = card.type === 'benefit'
              const cardColor = card.color_hex || (isBenefit ? '#F59E0B' : '#4A90E2')
              const usageRatio = card.limit > 0 ? Math.min(100, (card.used_limit / card.limit) * 100) : 0

              return (
                <div
                  key={card.id}
                  className={`card interactive-card animate-fade-in-up stagger-2 ${isSelected ? 'active-card-glow' : ''}`}
                  onClick={() => {
                    if (isSelected) {
                      setSelectedFilter(null)
                    } else {
                      setSelectedFilter({
                        type: 'card',
                        name: card.name,
                        limit: card.limit,
                        usedLimit: card.used_limit,
                        cardType: card.type
                      })
                    }
                  }}
                  style={{
                    padding: '16px 18px',
                    background: 'var(--bg-card)',
                    border: isSelected ? '1px solid var(--accent)' : '1px solid var(--border)',
                    borderRadius: 'var(--radius-md)',
                    cursor: 'pointer',
                    boxShadow: isSelected ? '0 0 0 1px var(--accent)' : 'none',
                    display: 'flex',
                    flexDirection: 'column',
                    gap: 10
                  }}
                >
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                      <div style={{ width: 8, height: 8, borderRadius: '50%', background: cardColor }} />
                      <span style={{ fontSize: 13, fontWeight: 600, color: 'var(--text-primary)', letterSpacing: 0.5 }}>
                        {card.name.toUpperCase()}
                      </span>
                    </div>
                    <span style={{
                      fontSize: 10,
                      fontWeight: 600,
                      padding: '2px 8px',
                      borderRadius: 'var(--radius-full)',
                      background: isBenefit ? 'rgba(245, 158, 11, 0.12)' : 'rgba(74, 144, 226, 0.12)',
                      color: isBenefit ? '#F59E0B' : '#4A90E2'
                    }}>
                      {isBenefit ? 'Benefício' : 'Crédito'}
                    </span>
                  </div>

                  <div>
                    <div style={{ fontSize: 11, color: 'var(--text-muted)', marginBottom: 2 }}>
                      {isBenefit ? 'Saldo Atual' : 'Fatura Atual'}
                    </div>
                    <div style={{ fontSize: 20, fontWeight: 600, color: 'var(--text-primary)', fontVariantNumeric: 'tabular-nums' }}>
                      {isBenefit ? formatCurrency(card.available_limit) : formatCurrency(card.used_limit)}
                    </div>
                  </div>

                  {!isBenefit && (
                    <div>
                      <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: 11, color: 'var(--text-muted)', marginBottom: 4 }}>
                        <span>Disp: <strong style={{ color: 'var(--text-secondary)' }}>{formatCurrency(card.available_limit)}</strong></span>
                        <span>Total: {formatCurrency(card.limit)}</span>
                      </div>
                      <div style={{ width: '100%', height: 4, background: 'var(--bg-surface-hover)', borderRadius: 999, overflow: 'hidden' }}>
                        <div 
                          className="animate-progress"
                          style={{ 
                            width: `${usageRatio}%`, 
                            height: '100%', 
                            background: usageRatio > 90 ? 'var(--danger)' : cardColor, 
                            borderRadius: 999 
                          }} 
                        />
                      </div>
                    </div>
                  )}
                </div>
              )
            })}
          </div>
        )}
      </div>

      {/* 4. SUAS CONTAS (Saldo em Contas Bancárias) */}
      <div style={{ marginBottom: 20 }}>
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 10, padding: '0 2px' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
            <span style={{ fontSize: 11, fontWeight: 600, letterSpacing: 1.2, color: 'var(--text-muted)', textTransform: 'uppercase' }}>
              Suas Contas
            </span>
            {accountsData.length > 0 && (
              <span style={{ fontSize: 10, fontWeight: 600, padding: '1px 6px', borderRadius: 'var(--radius-full)', background: 'var(--accent-subtle)', color: 'var(--accent)' }}>
                {accountsData.length}
              </span>
            )}
          </div>
          {accountsData.length > 0 && (
            <span style={{ fontSize: 11, color: 'var(--text-muted)' }}>
              Toque para filtrar saldo
            </span>
          )}
        </div>

        {accountsData.length === 0 ? (
          <div className="card" style={{ padding: '16px 20px', textAlign: 'center', color: 'var(--text-muted)', fontSize: 12 }}>
            Nenhuma conta cadastrada no aplicativo.
          </div>
        ) : (
          <div style={{ display: 'grid', gridTemplateColumns: accountsData.length === 1 ? '1fr' : 'repeat(auto-fit, minmax(200px, 1fr))', gap: 12 }}>
            {accountsData.map((acc) => {
              const isSelected = selectedFilter?.type === 'account' && selectedFilter.name === acc.name
              const accColor = acc.color_hex || 'var(--accent)'

              return (
                <div
                  key={acc.id}
                  className={`card interactive-card animate-fade-in-up stagger-2 ${isSelected ? 'active-card-glow' : ''}`}
                  onClick={() => {
                    if (isSelected) {
                      setSelectedFilter(null)
                    } else {
                      setSelectedFilter({
                        type: 'account',
                        name: acc.name,
                        balance: acc.balance,
                        accountType: acc.type
                      })
                    }
                  }}
                  style={{
                    padding: '14px 16px',
                    background: 'var(--bg-card)',
                    border: isSelected ? '1px solid var(--accent)' : '1px solid var(--border)',
                    borderRadius: 'var(--radius-md)',
                    cursor: 'pointer',
                    boxShadow: isSelected ? '0 0 0 1px var(--accent)' : 'none',
                    display: 'flex',
                    alignItems: 'center',
                    gap: 12
                  }}
                >
                  <div style={{ width: 4, height: 36, borderRadius: 999, background: accColor, flexShrink: 0 }} />
                  <div style={{ flex: 1, minWidth: 0 }}>
                    <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 6 }}>
                      <span style={{ fontSize: 13, fontWeight: 600, color: 'var(--text-primary)', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                        {acc.name}
                      </span>
                      <span style={{ fontSize: 10, color: 'var(--text-muted)' }}>
                        {acc.type}
                      </span>
                    </div>
                    <div style={{ fontSize: 15, fontWeight: 600, color: 'var(--accent)', marginTop: 2, fontVariantNumeric: 'tabular-nums' }}>
                      {formatCurrency(acc.balance)}
                    </div>
                  </div>
                </div>
              )
            })}
          </div>
        )}
      </div>

      {/* Income & Expense Grid */}
      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12, marginBottom: 20 }}>
        <div className="card interactive-card animate-fade-in-up stagger-3" style={{ padding: '16px 18px' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 6, marginBottom: 6 }}>
            <TrendingUp size={14} color="var(--success)" />
            <span style={{ fontSize: 11, fontWeight: 600, color: 'var(--text-secondary)' }}>Receitas do Mês</span>
          </div>
          <div style={{ fontSize: 18, fontWeight: 600, color: 'var(--success)', fontVariantNumeric: 'tabular-nums' }}>
            {formatCurrency(salaryValue)}
          </div>
        </div>

        <div className="card interactive-card animate-fade-in-up stagger-3" style={{ padding: '16px 18px' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 6, marginBottom: 6 }}>
            <TrendingDown size={14} color="var(--danger)" />
            <span style={{ fontSize: 11, fontWeight: 600, color: 'var(--text-secondary)' }}>Despesas Comprometidas</span>
          </div>
          <div style={{ fontSize: 18, fontWeight: 600, color: 'var(--danger)', fontVariantNumeric: 'tabular-nums' }}>
            {formatCurrency(committedValue)}
          </div>
        </div>
      </div>

      {/* Pending Suggestions Waiting for Approval */}
      {pendingSuggestions.length > 0 && (
        <div className="card animate-fade-in-up" style={{ marginBottom: 20, border: '1px solid var(--border-active)', background: 'var(--bg-surface)' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 14 }}>
            <Clock size={16} color="var(--accent)" />
            <h2 style={{ fontSize: 12, fontWeight: 600, textTransform: 'uppercase', letterSpacing: 1.2, color: 'var(--accent)' }}>
              Sugestões Enviadas ({pendingSuggestions.length} aguardando aprovação no app)
            </h2>
          </div>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
            {pendingSuggestions.map(sug => {
              const isIncome = sug.type === 'income'
              const isEdit = sug.action === 'edit'
              return (
                <div 
                  key={sug.id}
                  className="interactive-card"
                  style={{
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'space-between',
                    padding: '10px 14px',
                    background: 'var(--bg-card)',
                    border: '1px solid var(--border)',
                    borderRadius: 'var(--radius-sm)',
                  }}
                >
                  <div>
                    <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                      <span style={{
                        fontSize: 9,
                        fontWeight: 600,
                        padding: '1px 6px',
                        borderRadius: 'var(--radius-sm)',
                        background: isEdit ? 'rgba(74, 144, 226, 0.15)' : 'var(--accent-subtle)',
                        color: isEdit ? '#4A90E2' : 'var(--accent)',
                        textTransform: 'uppercase'
                      }}>
                        {isEdit ? 'Edição' : 'Novo'}
                      </span>
                      <span style={{ fontSize: 13, fontWeight: 600, color: 'var(--text-primary)' }}>{sug.title}</span>
                    </div>
                    <div style={{ fontSize: 11, color: 'var(--text-muted)', marginTop: 2 }}>
                      {isEdit && sug.original_amount !== undefined ? (
                        <span>{sug.category} • De {formatCurrency(sug.original_amount)} para {formatCurrency(sug.amount)} • Aguardando aprovação</span>
                      ) : (
                        <span>{sug.category} • {sug.date} • Aguardando aprovação</span>
                      )}
                    </div>
                  </div>
                  <div style={{ fontSize: 13, fontWeight: 600, color: isIncome ? 'var(--success)' : 'var(--text-primary)', fontVariantNumeric: 'tabular-nums' }}>
                    {isIncome ? '+' : '-'} {formatCurrency(sug.amount)}
                  </div>
                </div>
              )
            })}
          </div>
        </div>
      )}

      {/* Category Breakdown */}
      {doc.categories && doc.categories.length > 0 && (
        <div className="card animate-fade-in-up stagger-4" style={{ marginBottom: 20 }}>
          <h2 style={{ fontSize: 12, fontWeight: 600, textTransform: 'uppercase', letterSpacing: 1.2, color: 'var(--text-muted)', marginBottom: 16 }}>
            Gastos por Categoria
          </h2>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>
            {doc.categories.map((cat, idx) => {
              const roundedPct = Math.round(cat.percentage || 0)
              return (
                <div key={idx}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', fontSize: 13, marginBottom: 6 }}>
                    <span style={{ fontWeight: 600, color: 'var(--text-primary)' }}>{cat.name}</span>
                    <span style={{ fontWeight: 600, color: 'var(--text-secondary)', fontVariantNumeric: 'tabular-nums' }}>
                      {formatCurrency(cat.amount)} <span style={{ color: 'var(--text-muted)', fontSize: 12, fontWeight: 500 }}>({roundedPct}%)</span>
                    </span>
                  </div>
                  <div style={{ width: '100%', height: 6, background: 'var(--bg-surface-hover)', borderRadius: 999, overflow: 'hidden' }}>
                    <div 
                      className="animate-progress"
                      style={{ 
                        width: `${Math.min(cat.percentage, 100)}%`, 
                        height: '100%', 
                        background: 'var(--accent)', 
                        borderRadius: 999 
                      }} 
                    />
                  </div>
                </div>
              )
            })}
          </div>
        </div>
      )}

      {/* Recent Transactions List */}
      {doc.transactions && doc.transactions.length > 0 && (
        <div className="animate-fade-in-up stagger-5">
          <h2 style={{ fontSize: 12, fontWeight: 600, textTransform: 'uppercase', letterSpacing: 1.2, color: 'var(--text-muted)', marginBottom: 12 }}>
            Últimas Movimentações
          </h2>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
            {doc.transactions.map((tx) => {
              const isIncome = tx.type?.toUpperCase() === 'INCOME'
              const hasPendingEdit = pendingSuggestions.some(s => s.action === 'edit' && String(s.target_tx_id) === String(tx.id))
              return (
                <div 
                  key={tx.id}
                  className="card interactive-card"
                  style={{
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'space-between',
                    gap: 12,
                    padding: '12px 14px',
                    background: 'var(--bg-card)',
                    border: '1px solid var(--border)',
                    borderLeft: isIncome ? '3px solid var(--success)' : '3px solid var(--danger)',
                    borderRadius: 'var(--radius-md)',
                  }}
                >
                  <div style={{ display: 'flex', alignItems: 'center', gap: 10, minWidth: 0, flex: 1 }}>
                    <div style={{ 
                      width: 32, 
                      height: 32, 
                      flexShrink: 0,
                      borderRadius: 'var(--radius-sm)', 
                      background: isIncome ? 'var(--success-subtle)' : 'var(--danger-subtle)',
                      border: `1px solid ${isIncome ? 'rgba(16, 185, 129, 0.2)' : 'rgba(239, 68, 68, 0.2)'}`,
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center'
                    }}>
                      {isIncome ? <ArrowDownLeft size={16} color="var(--success)" /> : <ArrowUpRight size={16} color="var(--danger)" />}
                    </div>
                    <div style={{ minWidth: 0, flex: 1 }}>
                      <div style={{ 
                        fontSize: 13, 
                        fontWeight: 600, 
                        color: 'var(--text-primary)',
                        overflow: 'hidden',
                        textOverflow: 'ellipsis',
                        whiteSpace: 'nowrap'
                      }}>
                        {tx.title}
                      </div>
                      <div style={{ 
                        fontSize: 11, 
                        color: 'var(--text-muted)', 
                        marginTop: 2,
                        overflow: 'hidden',
                        textOverflow: 'ellipsis',
                        whiteSpace: 'nowrap'
                      }}>
                        <span style={{ fontWeight: 500, color: 'var(--text-secondary)' }}>{tx.category}</span>
                        {tx.date ? ` • ${formatDate(tx.date)}` : ''}
                      </div>
                      {hasPendingEdit && (
                        <div style={{ 
                          display: 'inline-flex', 
                          alignItems: 'center', 
                          gap: 4, 
                          marginTop: 4, 
                          padding: '1px 6px', 
                          borderRadius: 'var(--radius-sm)', 
                          background: 'rgba(74, 144, 226, 0.12)', 
                          color: '#4A90E2', 
                          fontSize: 10, 
                          fontWeight: 600 
                        }}>
                          <Clock size={10} /> Alteração em aprovação no app
                        </div>
                      )}
                    </div>
                  </div>

                  <div style={{ display: 'flex', alignItems: 'center', gap: 10, flexShrink: 0 }}>
                    <div style={{ 
                      fontSize: 13, 
                      fontWeight: 600, 
                      color: isIncome ? 'var(--success)' : 'var(--danger)',
                      whiteSpace: 'nowrap',
                      textAlign: 'right',
                      fontVariantNumeric: 'tabular-nums'
                    }}>
                      {isIncome ? '+ ' : '- '}{formatCurrency(tx.amount)}
                    </div>
                    <button
                      type="button"
                      onClick={(e) => {
                        e.stopPropagation()
                        if (!hasPendingEdit) {
                          handleOpenEditModal(tx)
                        }
                      }}
                      disabled={hasPendingEdit}
                      title={hasPendingEdit ? "Já possui uma alteração aguardando aprovação" : "Sugerir alteração"}
                      aria-label="Editar lançamento"
                      style={{
                        padding: 6,
                        borderRadius: 'var(--radius-sm)',
                        border: '1px solid var(--border)',
                        background: 'var(--bg-surface)',
                        color: hasPendingEdit ? 'var(--text-muted)' : 'var(--text-secondary)',
                        cursor: hasPendingEdit ? 'not-allowed' : 'pointer',
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                        transition: 'all 150ms ease-out',
                        opacity: hasPendingEdit ? 0.4 : 1
                      }}
                    >
                      <Pencil size={13} />
                    </button>
                  </div>
                </div>
              )
            })}
          </div>
        </div>
      )}

      {/* Suggestion Modal */}
      {isModalOpen && (
        <div className="modal-overlay" onClick={() => setIsModalOpen(false)}>
          <div className="modal-content" onClick={e => e.stopPropagation()}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 20 }}>
              <h2 style={{ fontSize: 18, fontWeight: 600, color: 'var(--text-primary)' }}>Sugerir Lançamento</h2>
              <button 
                className="btn btn-outline" 
                onClick={() => setIsModalOpen(false)}
                style={{ padding: 6, borderRadius: '50%', width: 32, height: 32 }}
              >
                <X size={16} />
              </button>
            </div>

            <form onSubmit={handleSuggestTransaction} style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
              {/* Type Switcher */}
              <div>
                <label className="input-label">Tipo de Lançamento</label>
                <div className="segmented-control">
                  <button 
                    type="button"
                    className={`segmented-btn ${suggestionType === 'expense' ? 'active' : ''}`}
                    onClick={() => setSuggestionType('expense')}
                  >
                    💸 Despesa
                  </button>
                  <button 
                    type="button"
                    className={`segmented-btn ${suggestionType === 'income' ? 'active' : ''}`}
                    onClick={() => setSuggestionType('income')}
                  >
                    💰 Receita
                  </button>
                </div>
              </div>

              {/* Title */}
              <div>
                <label className="input-label">Descrição / Título</label>
                <input 
                  type="text"
                  className="input-field"
                  placeholder="Ex: Jantar, Supermercado, Freela, Farmácia..."
                  value={suggestionTitle}
                  onChange={e => setSuggestionTitle(e.target.value)}
                  autoFocus
                  required
                />
              </div>

              {/* Amount & Category */}
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
                <div>
                  <label className="input-label">Valor (R$)</label>
                  <input 
                    type="number"
                    step="0.01"
                    min="0.01"
                    placeholder="0,00"
                    className="input-field"
                    value={suggestionAmount}
                    onChange={e => setSuggestionAmount(e.target.value)}
                    required
                  />
                </div>

                <div>
                  <label className="input-label">Categoria</label>
                  <select 
                    className="input-field"
                    value={suggestionCategory}
                    onChange={e => setSuggestionCategory(e.target.value)}
                    style={{ cursor: 'pointer' }}
                  >
                    <option value="Alimentação">Alimentação</option>
                    <option value="Mercado">Mercado</option>
                    <option value="Transporte">Transporte</option>
                    <option value="Moradia">Moradia</option>
                    <option value="Lazer">Lazer</option>
                    <option value="Saúde">Saúde</option>
                    <option value="Educação">Educação</option>
                    <option value="Salário">Salário</option>
                    <option value="Outros">Outros</option>
                  </select>
                </div>
              </div>

              <div style={{ 
                background: 'var(--bg-surface)', 
                border: '1px solid var(--border)', 
                borderRadius: 'var(--radius-sm)', 
                padding: '10px 14px', 
                fontSize: 12, 
                color: 'var(--text-muted)',
                lineHeight: 1.4
              }}>
                ℹ️ Esta transação será enviada como uma sugestão para o aplicativo do Kenned e só será efetivada após aprovação no celular.
              </div>

              <div style={{ display: 'flex', gap: 10, marginTop: 4 }}>
                <button 
                  type="button" 
                  className="btn btn-outline" 
                  onClick={() => setIsModalOpen(false)}
                  style={{ flex: 1 }}
                >
                  Cancelar
                </button>
                <button 
                  type="submit" 
                  className="btn btn-primary" 
                  disabled={!suggestionTitle.trim() || !suggestionAmount || isSubmitting}
                  style={{ flex: 1 }}
                >
                  {isSubmitting ? 'Enviando...' : 'Enviar Sugestão'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Modal de Edição de Lançamento (com aprovação obrigatória) */}
      {editingTx && (
        <div className="modal-overlay" onClick={() => setEditingTx(null)}>
          <div className="modal-content animate-fade-in-up" onClick={e => e.stopPropagation()}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 20 }}>
              <div>
                <h2 style={{ fontSize: 18, fontWeight: 600, color: 'var(--text-primary)' }}>Editar Lançamento</h2>
                <p style={{ fontSize: 11, color: 'var(--text-muted)', marginTop: 2 }}>
                  Sugerir alteração para aprovação no app
                </p>
              </div>
              <button 
                type="button"
                className="btn btn-outline" 
                onClick={() => setEditingTx(null)}
                style={{ padding: 6, borderRadius: '50%', width: 32, height: 32, display: 'flex', alignItems: 'center', justifyContent: 'center' }}
                aria-label="Fechar"
              >
                <X size={16} />
              </button>
            </div>

            <form onSubmit={handleSuggestEditTransaction} style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
              {/* Type Switcher */}
              <div>
                <label className="input-label">Tipo de Lançamento</label>
                <div className="segmented-control">
                  <button 
                    type="button"
                    className={`segmented-btn ${editType === 'expense' ? 'active' : ''}`}
                    onClick={() => setEditType('expense')}
                  >
                    💸 Despesa
                  </button>
                  <button 
                    type="button"
                    className={`segmented-btn ${editType === 'income' ? 'active' : ''}`}
                    onClick={() => setEditType('income')}
                  >
                    💰 Receita
                  </button>
                </div>
              </div>

              {/* Title */}
              <div>
                <label className="input-label">Descrição / Título</label>
                <input 
                  type="text"
                  className="input-field"
                  placeholder="Título do lançamento"
                  value={editTitle}
                  onChange={e => setEditTitle(e.target.value)}
                  autoFocus
                  required
                />
              </div>

              {/* Amount & Category */}
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
                <div>
                  <label className="input-label">Valor (R$)</label>
                  <input 
                    type="number"
                    step="0.01"
                    min="0.01"
                    placeholder="0,00"
                    className="input-field"
                    value={editAmount}
                    onChange={e => setEditAmount(e.target.value)}
                    required
                  />
                </div>

                <div>
                  <label className="input-label">Categoria</label>
                  <select 
                    className="input-field"
                    value={editCategory}
                    onChange={e => setEditCategory(e.target.value)}
                    style={{ cursor: 'pointer' }}
                  >
                    <option value="Alimentação">Alimentação</option>
                    <option value="Mercado">Mercado</option>
                    <option value="Transporte">Transporte</option>
                    <option value="Moradia">Moradia</option>
                    <option value="Lazer">Lazer</option>
                    <option value="Saúde">Saúde</option>
                    <option value="Educação">Educação</option>
                    <option value="Salário">Salário</option>
                    <option value="Outros">Outros</option>
                  </select>
                </div>
              </div>

              {/* Conta / Cartão de Origem (opcional) */}
              {(accountsData.length > 0 || cardsData.length > 0) && (
                <div>
                  <label className="input-label">Conta ou Cartão</label>
                  <select
                    className="input-field"
                    value={editAccountOrCard}
                    onChange={e => setEditAccountOrCard(e.target.value)}
                    style={{ cursor: 'pointer' }}
                  >
                    <option value="">Manter original ({editingTx.account_or_card_name || 'Não informado'})</option>
                    {accountsData.map(acc => (
                      <option key={acc.id} value={acc.name}>🏦 {acc.name} ({acc.type})</option>
                    ))}
                    {cardsData.map(c => (
                      <option key={c.id} value={c.name}>💳 {c.name}</option>
                    ))}
                  </select>
                </div>
              )}

              {/* Aviso de Aprovação Obrigatória */}
              <div style={{ 
                background: 'var(--bg-surface)', 
                border: '1px solid var(--border)', 
                borderRadius: 'var(--radius-sm)', 
                padding: '10px 14px', 
                fontSize: 12, 
                color: 'var(--text-muted)',
                lineHeight: 1.4
              }}>
                🔒 <strong>Segurança:</strong> Esta alteração não será aplicada diretamente. Ela será enviada para aprovação no aplicativo Tessera do proprietário.
              </div>

              <div style={{ display: 'flex', gap: 10, marginTop: 4 }}>
                <button 
                  type="button" 
                  className="btn btn-outline" 
                  onClick={() => setEditingTx(null)}
                  style={{ flex: 1 }}
                >
                  Cancelar
                </button>
                <button 
                  type="submit" 
                  className="btn btn-primary" 
                  disabled={!editTitle.trim() || !editAmount || isSubmittingEdit}
                  style={{ flex: 1 }}
                >
                  {isSubmittingEdit ? 'Enviando...' : 'Enviar para Aprovação'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Detalhamento dos Painéis: Modal Interativo */}
      {selectedPanelModal && (
        <div className="modal-overlay" onClick={() => setSelectedPanelModal(null)}>
          <div 
            className="modal-content" 
            onClick={e => e.stopPropagation()} 
            style={{ maxWidth: 540, maxHeight: '85vh', display: 'flex', flexDirection: 'column' }}
          >
            {/* Header do Modal */}
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                <div style={{
                  width: 36,
                  height: 36,
                  borderRadius: '50%',
                  background: selectedPanelModal === 'debts' ? 'rgba(239, 68, 68, 0.12)' : selectedPanelModal === 'installments' ? 'rgba(74, 144, 226, 0.12)' : 'var(--accent-subtle)',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center'
                }}>
                  {selectedPanelModal === 'debts' && <Banknote size={18} color="#EF4444" />}
                  {selectedPanelModal === 'installments' && <CreditCard size={18} color="#4A90E2" />}
                  {selectedPanelModal === 'recurrents' && <Repeat size={18} color="var(--accent)" />}
                </div>
                <div>
                  <h2 style={{ fontSize: 16, fontWeight: 600, color: 'var(--text-primary)' }}>
                    {selectedPanelModal === 'debts' && 'Painel de Dívidas'}
                    {selectedPanelModal === 'installments' && 'Painel de Parcelados'}
                    {selectedPanelModal === 'recurrents' && 'Painel de Contas Fixas'}
                  </h2>
                  <p style={{ fontSize: 11, color: 'var(--text-muted)' }}>
                    {selectedPanelModal === 'debts' && 'Compromissos e pendências ativas'}
                    {selectedPanelModal === 'installments' && 'Compras e despesas parceladas no mês'}
                    {selectedPanelModal === 'recurrents' && 'Assinaturas e contas fixas recorrentes'}
                  </p>
                </div>
              </div>
              <button 
                className="btn btn-outline" 
                onClick={() => setSelectedPanelModal(null)}
                style={{ padding: 6, borderRadius: '50%', width: 32, height: 32, display: 'flex', alignItems: 'center', justifyContent: 'center' }}
                aria-label="Fechar"
              >
                <X size={16} />
              </button>
            </div>

            {/* Métricas do Topo do Modal */}
            {selectedPanelModal === 'debts' && (
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: 8, marginBottom: 16 }}>
                <div style={{ background: 'var(--bg-surface)', border: '1px solid var(--border)', borderRadius: 'var(--radius-sm)', padding: '10px 12px' }}>
                  <div style={{ fontSize: 10, color: 'var(--text-muted)', fontWeight: 600 }}>Total Devido</div>
                  <div style={{ fontSize: 14, fontWeight: 600, color: 'var(--text-primary)', marginTop: 2, fontVariantNumeric: 'tabular-nums' }}>
                    {formatCurrency(debtsData.total_owed)}
                  </div>
                </div>
                <div style={{ background: 'var(--bg-surface)', border: '1px solid var(--border)', borderRadius: 'var(--radius-sm)', padding: '10px 12px' }}>
                  <div style={{ fontSize: 10, color: 'var(--text-muted)', fontWeight: 600 }}>Já Quitado</div>
                  <div style={{ fontSize: 14, fontWeight: 600, color: 'var(--success)', marginTop: 2, fontVariantNumeric: 'tabular-nums' }}>
                    {formatCurrency(debtsData.total_paid)}
                  </div>
                </div>
                <div style={{ background: 'var(--bg-surface)', border: '1px solid var(--border)', borderRadius: 'var(--radius-sm)', padding: '10px 12px' }}>
                  <div style={{ fontSize: 10, color: 'var(--text-muted)', fontWeight: 600 }}>Restante</div>
                  <div style={{ fontSize: 14, fontWeight: 600, color: '#EF4444', marginTop: 2, fontVariantNumeric: 'tabular-nums' }}>
                    {formatCurrency(debtsData.remaining_to_pay)}
                  </div>
                </div>
              </div>
            )}

            {selectedPanelModal === 'installments' && (
              <div style={{ display: 'flex', flexDirection: 'column', gap: 12, marginBottom: 16 }}>
                {/* Abas Deste Mês vs Todas as Parcelas */}
                <div style={{
                  display: 'flex',
                  background: 'var(--bg-surface-hover)',
                  padding: 3,
                  borderRadius: 'var(--radius-sm)',
                  gap: 4
                }}>
                  <button
                    type="button"
                    onClick={() => setInstallmentViewMode('month')}
                    style={{
                      flex: 1,
                      padding: '6px 12px',
                      fontSize: 12,
                      fontWeight: installmentViewMode === 'month' ? 600 : 400,
                      borderRadius: 'calc(var(--radius-sm) - 2px)',
                      border: 'none',
                      background: installmentViewMode === 'month' ? 'var(--bg-surface)' : 'transparent',
                      color: installmentViewMode === 'month' ? 'var(--text-primary)' : 'var(--text-muted)',
                      cursor: 'pointer',
                      transition: 'all 150ms ease-out',
                      boxShadow: installmentViewMode === 'month' ? '0 1px 3px rgba(0,0,0,0.1)' : 'none'
                    }}
                  >
                    Deste Mês ({installmentsData.count})
                  </button>
                  <button
                    type="button"
                    onClick={() => setInstallmentViewMode('all')}
                    style={{
                      flex: 1,
                      padding: '6px 12px',
                      fontSize: 12,
                      fontWeight: installmentViewMode === 'all' ? 600 : 400,
                      borderRadius: 'calc(var(--radius-sm) - 2px)',
                      border: 'none',
                      background: installmentViewMode === 'all' ? 'var(--bg-surface)' : 'transparent',
                      color: installmentViewMode === 'all' ? 'var(--text-primary)' : 'var(--text-muted)',
                      cursor: 'pointer',
                      transition: 'all 150ms ease-out',
                      boxShadow: installmentViewMode === 'all' ? '0 1px 3px rgba(0,0,0,0.1)' : 'none'
                    }}
                  >
                    Todas as Parcelas ({totalInstallmentsCount})
                  </button>
                </div>

                {/* Métricas do modo selecionado */}
                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 10 }}>
                  <div style={{ background: 'var(--bg-surface)', border: '1px solid var(--border)', borderRadius: 'var(--radius-sm)', padding: '10px 12px' }}>
                    <div style={{ fontSize: 10, color: 'var(--text-muted)', fontWeight: 600 }}>
                      {installmentViewMode === 'month' ? 'Total no Mês' : 'Total Geral Lançado'}
                    </div>
                    <div style={{ fontSize: 15, fontWeight: 600, color: '#4A90E2', marginTop: 2, fontVariantNumeric: 'tabular-nums' }}>
                      {formatCurrency(installmentViewMode === 'month' ? installmentsData.total_month_value : (installmentsData.total_value || installmentsData.total_month_value))}
                    </div>
                  </div>
                  <div style={{ background: 'var(--bg-surface)', border: '1px solid var(--border)', borderRadius: 'var(--radius-sm)', padding: '10px 12px' }}>
                    <div style={{ fontSize: 10, color: 'var(--text-muted)', fontWeight: 600 }}>
                      {installmentViewMode === 'month' ? 'Parcelas do Mês' : 'Total Cadastradas'}
                    </div>
                    <div style={{ fontSize: 15, fontWeight: 600, color: 'var(--text-primary)', marginTop: 2 }}>
                      {installmentViewMode === 'month' ? `${installmentsData.count} parcelas` : `${totalInstallmentsCount} parcelas`}
                    </div>
                  </div>
                </div>
              </div>
            )}

            {selectedPanelModal === 'recurrents' && (
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 10, marginBottom: 16 }}>
                <div style={{ background: 'var(--bg-surface)', border: '1px solid var(--border)', borderRadius: 'var(--radius-sm)', padding: '10px 12px' }}>
                  <div style={{ fontSize: 10, color: 'var(--text-muted)', fontWeight: 600 }}>Custo Fixo Mensal</div>
                  <div style={{ fontSize: 15, fontWeight: 600, color: 'var(--accent)', marginTop: 2, fontVariantNumeric: 'tabular-nums' }}>
                    {formatCurrency(recurrentsData.total_monthly_value)}
                  </div>
                </div>
                <div style={{ background: 'var(--bg-surface)', border: '1px solid var(--border)', borderRadius: 'var(--radius-sm)', padding: '10px 12px' }}>
                  <div style={{ fontSize: 10, color: 'var(--text-muted)', fontWeight: 600 }}>Contas Cadastradas</div>
                  <div style={{ fontSize: 15, fontWeight: 600, color: 'var(--text-primary)', marginTop: 2 }}>
                    {recurrentsData.count} ativas
                  </div>
                </div>
              </div>
            )}

            {/* Lista com scroll e Empty States */}
            <div style={{ overflowY: 'auto', flex: 1, display: 'flex', flexDirection: 'column', gap: 8, paddingRight: 4 }}>
              {/* Painel de Dívidas: Lista ou Empty State */}
              {selectedPanelModal === 'debts' && (
                debtsData.items.length === 0 ? (
                  <div style={{ textAlign: 'center', padding: '36px 16px', color: 'var(--text-muted)' }}>
                    <div style={{ width: 44, height: 44, borderRadius: '50%', background: 'var(--success-subtle)', display: 'flex', alignItems: 'center', justifyContent: 'center', margin: '0 auto 12px' }}>
                      <CheckCircle2 size={22} color="var(--success)" />
                    </div>
                    <div style={{ fontSize: 14, fontWeight: 600, color: 'var(--text-primary)' }}>Nenhuma dívida pendente</div>
                    <p style={{ fontSize: 12, marginTop: 4 }}>Você está com todos os compromissos em dia!</p>
                  </div>
                ) : (
                  debtsData.items.map(debt => {
                    const instTotal = debt.installments_total > 0 ? debt.installments_total : 1
                    const instPaid = debt.installments_paid || 0
                    const remainingVal = (debt.value / instTotal) * (instTotal - instPaid)
                    const progress = Math.min((instPaid / instTotal) * 100, 100)

                    return (
                      <div 
                        key={debt.id}
                        style={{
                          padding: '12px 14px',
                          background: 'var(--bg-surface)',
                          border: '1px solid var(--border)',
                          borderRadius: 'var(--radius-sm)',
                          display: 'flex',
                          flexDirection: 'column',
                          gap: 8
                        }}
                      >
                        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
                          <div>
                            <div style={{ fontSize: 13, fontWeight: 600, color: 'var(--text-primary)' }}>{debt.title}</div>
                            <div style={{ fontSize: 11, color: 'var(--text-muted)' }}>
                              {debt.creditor_name || 'Credor'} {debt.due_date > 0 && `• Vence ${formatDate(debt.due_date)}`}
                            </div>
                          </div>
                          <div style={{ textAlign: 'right' }}>
                            <div style={{ fontSize: 13, fontWeight: 600, color: '#EF4444', fontVariantNumeric: 'tabular-nums' }}>
                              {formatCurrency(remainingVal)}
                            </div>
                            <div style={{ fontSize: 10, color: 'var(--text-muted)' }}>
                              de {formatCurrency(debt.value)}
                            </div>
                          </div>
                        </div>

                        {/* Barra de Progresso de Quitação */}
                        <div>
                          <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: 10, color: 'var(--text-muted)', marginBottom: 4 }}>
                            <span>Parcelas: {instPaid}/{instTotal}</span>
                            <span>{Math.round(progress)}%</span>
                          </div>
                          <div style={{ width: '100%', height: 4, background: 'var(--bg-surface-hover)', borderRadius: 999, overflow: 'hidden' }}>
                            <div style={{ width: `${progress}%`, height: '100%', background: '#EF4444', borderRadius: 999 }} />
                          </div>
                        </div>
                      </div>
                    )
                  })
                )
              )}

              {/* Painel de Parcelados: Lista ou Empty State */}
              {selectedPanelModal === 'installments' && (() => {
                const currentList = installmentViewMode === 'month'
                  ? installmentsData.items
                  : (installmentsData.all_items && installmentsData.all_items.length > 0 ? installmentsData.all_items : installmentsData.items)

                if (currentList.length === 0) {
                  return (
                    <div style={{ textAlign: 'center', padding: '36px 16px', color: 'var(--text-muted)' }}>
                      <div style={{ width: 44, height: 44, borderRadius: '50%', background: 'rgba(74, 144, 226, 0.1)', display: 'flex', alignItems: 'center', justifyContent: 'center', margin: '0 auto 12px' }}>
                        <CreditCard size={22} color="#4A90E2" />
                      </div>
                      <div style={{ fontSize: 14, fontWeight: 600, color: 'var(--text-primary)' }}>
                        {installmentViewMode === 'month' ? 'Nenhuma parcela este mês' : 'Nenhuma parcela cadastrada'}
                      </div>
                      <p style={{ fontSize: 12, marginTop: 4, marginBottom: totalInstallmentsCount > 0 ? 12 : 0 }}>
                        {installmentViewMode === 'month' 
                          ? (totalInstallmentsCount > 0 
                              ? `Você tem ${totalInstallmentsCount} parcela(s) lançada(s) em outros períodos.` 
                              : 'Não constam compras parceladas ativas para o período.')
                          : 'Nenhuma compra parcelada registrada no momento.'}
                      </p>
                      {installmentViewMode === 'month' && totalInstallmentsCount > 0 && (
                        <button
                          type="button"
                          className="btn btn-outline"
                          onClick={() => setInstallmentViewMode('all')}
                          style={{ fontSize: 11, padding: '6px 14px', margin: '0 auto' }}
                        >
                          Ver todas as {totalInstallmentsCount} parcelas
                        </button>
                      )}
                    </div>
                  )
                }

                return currentList.map(inst => (
                  <div 
                    key={inst.id}
                    style={{
                      padding: '12px 14px',
                      background: 'var(--bg-surface)',
                      border: '1px solid var(--border)',
                      borderRadius: 'var(--radius-sm)',
                      display: 'flex',
                      justifyContent: 'space-between',
                      alignItems: 'center',
                      gap: 12
                    }}
                  >
                    <div style={{ minWidth: 0, flex: 1 }}>
                      <div style={{ display: 'flex', alignItems: 'center', gap: 6, flexWrap: 'wrap' }}>
                        <span style={{ fontSize: 13, fontWeight: 600, color: 'var(--text-primary)' }}>{inst.title}</span>
                        {installmentViewMode === 'all' && inst.is_current_month && (
                          <span style={{
                            fontSize: 9,
                            padding: '2px 6px',
                            borderRadius: 4,
                            background: 'rgba(74, 144, 226, 0.12)',
                            color: '#4A90E2',
                            fontWeight: 600
                          }}>
                            Deste Mês
                          </span>
                        )}
                        {inst.is_realized && (
                          <span style={{
                            fontSize: 9,
                            padding: '2px 6px',
                            borderRadius: 4,
                            background: 'var(--success-subtle)',
                            color: 'var(--success)',
                            fontWeight: 600
                          }}>
                            Paga
                          </span>
                        )}
                      </div>
                      <div style={{ fontSize: 11, color: 'var(--text-muted)', marginTop: 2 }}>
                        <span style={{ color: '#4A90E2', fontWeight: 600 }}>{inst.subtitle || 'Parcela'}</span>
                        {inst.account_or_card_name && ` • ${inst.account_or_card_name}`}
                        {inst.date > 0 && ` • ${formatDate(inst.date)}`}
                      </div>
                    </div>
                    <div style={{ fontSize: 13, fontWeight: 600, color: 'var(--text-primary)', fontVariantNumeric: 'tabular-nums', flexShrink: 0 }}>
                      {formatCurrency(inst.value)}
                    </div>
                  </div>
                ))
              })()}

              {/* Painel de Contas Fixas: Lista ou Empty State */}
              {selectedPanelModal === 'recurrents' && (
                recurrentsData.items.length === 0 ? (
                  <div style={{ textAlign: 'center', padding: '36px 16px', color: 'var(--text-muted)' }}>
                    <div style={{ width: 44, height: 44, borderRadius: '50%', background: 'var(--accent-subtle)', display: 'flex', alignItems: 'center', justifyContent: 'center', margin: '0 auto 12px' }}>
                      <Repeat size={22} color="var(--accent)" />
                    </div>
                    <div style={{ fontSize: 14, fontWeight: 600, color: 'var(--text-primary)' }}>Nenhuma conta fixa</div>
                    <p style={{ fontSize: 12, marginTop: 4 }}>Nenhum custo fixo recorrente cadastrado.</p>
                  </div>
                ) : (
                  recurrentsData.items.map(rec => (
                    <div 
                      key={rec.id}
                      style={{
                        padding: '12px 14px',
                        background: 'var(--bg-surface)',
                        border: '1px solid var(--border)',
                        borderRadius: 'var(--radius-sm)',
                        display: 'flex',
                        justifyContent: 'space-between',
                        alignItems: 'center'
                      }}
                    >
                      <div>
                        <div style={{ fontSize: 13, fontWeight: 600, color: 'var(--text-primary)' }}>{rec.title}</div>
                        <div style={{ fontSize: 11, color: 'var(--text-muted)' }}>
                          <span style={{ color: 'var(--accent)', fontWeight: 600 }}>{rec.category}</span>
                          {rec.account_or_card_name && ` • ${rec.account_or_card_name}`}
                          {rec.recurrence_interval && ` • ${rec.recurrence_interval}`}
                        </div>
                      </div>
                      <div style={{ fontSize: 13, fontWeight: 600, color: 'var(--text-primary)', fontVariantNumeric: 'tabular-nums' }}>
                        {formatCurrency(rec.value)}
                      </div>
                    </div>
                  ))
                )
              )}
            </div>

            {/* Footer do Modal */}
            <div style={{ marginTop: 16, paddingTop: 12, borderTop: '1px solid var(--border)', display: 'flex', justifyContent: 'flex-end' }}>
              <button 
                type="button" 
                className="btn btn-outline" 
                onClick={() => setSelectedPanelModal(null)}
                style={{ height: 36, fontSize: 12 }}
              >
                Fechar
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Footer Branding */}
      <div style={{ textAlign: 'center', marginTop: 40, marginBottom: 20, fontSize: 11, color: 'var(--text-muted)' }}>
        Desenvolvido por <strong style={{ color: 'var(--text-primary)' }}>Tessera</strong> • Conexão Web
      </div>

      {/* Modal de Instruções de Instalação PWA */}
      <PwaInstructionsModal 
        isOpen={showHelpModal} 
        onClose={() => setShowHelpModal(false)} 
        isIos={isIos} 
      />
    </div>
  )
}

